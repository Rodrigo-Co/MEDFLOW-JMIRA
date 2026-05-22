package com.medflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.medflow.config.AppConfig;
import com.medflow.dto.CreateRecordRequest;
import com.medflow.dto.EditRecordRequest;
import com.medflow.http.HttpException;
import com.medflow.http.RequestContext;
import com.medflow.model.MedicalRecord;
import com.medflow.model.RecordEditHistory;
import com.medflow.model.User;
import com.medflow.repository.MedicalRecordRepository;
import com.medflow.repository.UserRepository;
import com.medflow.util.Jsons;
import com.medflow.util.Validation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecordService {

    private final MedicalRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final AuditService audit;
    private final AppConfig config;
    private final HttpClient httpClient;

    public RecordService(MedicalRecordRepository recordRepo,
                         UserRepository userRepo,
                         AuditService audit,
                         AppConfig config) {
        this.recordRepo = recordRepo;
        this.userRepo = userRepo;
        this.audit = audit;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<MedicalRecord> findAll(String q) {
        if (q == null || q.isBlank()) {
            return recordRepo.findAll();
        }
        return recordRepo.search(q.trim());
    }

    public List<MedicalRecord> findByPatient(String patientId) {
        return recordRepo.findByPatientIdOrderByRecordDateDesc(patientId);
    }

    public List<MedicalRecord> findByDoctor(String doctorId) {
        return recordRepo.findByDoctorIdOrderByRecordDateDesc(doctorId);
    }

    public MedicalRecord findById(String id) {
        return recordRepo.findById(id)
                .orElseThrow(() -> new HttpException(404, "Prontuario nao encontrado"));
    }

    public MedicalRecord create(CreateRecordRequest req) {
        Validation.notBlank(req.getPatientId(), "patientId obrigatorio");
        Validation.notBlank(req.getType(), "type obrigatorio");
        Validation.notBlank(req.getRawNotes(), "rawNotes obrigatorio");
        Validation.notBlank(req.getFormattedNotes(), "formattedNotes obrigatorio");
        Validation.notBlank(req.getDiagnosis(), "diagnosis obrigatorio");

        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new HttpException(401, "Medico nao autenticado"));
        User patient = userRepo.findById(req.getPatientId())
                .orElseThrow(() -> new HttpException(404, "Paciente nao encontrado"));

        MedicalRecord record = MedicalRecord.builder()
                .id("r" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .patientId(patient.getId())
                .patientName(patient.getName())
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .recordDate(LocalDate.now())
                .type(req.getType())
                .rawNotes(req.getRawNotes())
                .formattedNotes(req.getFormattedNotes())
                .diagnosis(req.getDiagnosis())
                .createdAt(OffsetDateTime.now())
                .build();

        MedicalRecord saved = recordRepo.save(record);
        audit.log("RECORD_CREATE", "info",
                "Prontuario criado para " + patient.getName(),
                doctor.getId(), doctor.getName(), doctor.getRole());
        return saved;
    }

    public MedicalRecord edit(String recordId, EditRecordRequest req) {
        Validation.notBlank(req.getType(), "type obrigatorio");
        Validation.notBlank(req.getRawNotes(), "rawNotes obrigatorio");
        Validation.notBlank(req.getFormattedNotes(), "formattedNotes obrigatorio");
        Validation.notBlank(req.getDiagnosis(), "diagnosis obrigatorio");

        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new HttpException(401, "Medico nao autenticado"));
        MedicalRecord record = findById(recordId);

        if (!doctorId.equals(record.getDoctorId())) {
            throw new HttpException(403, "Voce so pode editar seus proprios prontuarios");
        }

        if (record.getEditHistory() == null) {
            record.setEditHistory(new java.util.ArrayList<>());
        }
        record.getEditHistory().add(RecordEditHistory.builder()
                .id("eh" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .editedBy(doctor.getId())
                .editedByName(doctor.getName())
                .editTimestamp(OffsetDateTime.now())
                .changes("Prontuario editado.")
                .build());

        record.setType(req.getType());
        record.setRawNotes(req.getRawNotes());
        record.setFormattedNotes(req.getFormattedNotes());
        record.setDiagnosis(req.getDiagnosis());

        MedicalRecord saved = recordRepo.save(record);
        audit.log("RECORD_EDIT", "warning",
                "Prontuario de " + record.getPatientName() + " editado",
                doctor.getId(), doctor.getName(), doctor.getRole());
        return saved;
    }

    public String formatNotes(String raw, String type) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = formalizeWithAi(raw, type);
        if (text == null || text.isBlank()) {
            text = formalizeClinicalText(raw);
        }

        String typeLabel = "cirurgia".equalsIgnoreCase(type) ? "RELATÓRIO CIRÚRGICO" : "RELATÓRIO DE CONSULTA";
        String conduta = "cirurgia".equalsIgnoreCase(type)
                ? "Conduta pós-operatória: manter monitorização clínica, acompanhar evolução e registrar intercorrências."
                : "Conduta: manter acompanhamento clínico, reavaliar evolução e registrar plano terapêutico conforme critério médico.";

        return typeLabel + " - " + LocalDate.now()
                + "\n\nDescrição clínica revisada:\n" + text
                + "\n\n" + conduta;
    }

    private String formalizeWithAi(String raw, String type) {
        String apiKey = config.get("openai.api-key", "");
        if (apiKey.isBlank()) {
            System.err.println("OpenAI API key ausente. Usando fallback local para formatar prontuario.");
            return null;
        }

        String model = config.get("openai.model", "gpt-4.1-mini");
        int timeoutSeconds = Math.max(5, config.getInt("openai.timeout-seconds", 20));

        String instructions = """
                Voce e um assistente de redacao clinica para prontuarios medicos em portugues do Brasil.
                Reescreva as notas brutas com linguagem formal, tecnica e objetiva.
                Corrija ortografia, acentuacao, concordancia, pontuacao e erros de digitacao.
                Substitua termos leigos por termos clinicos quando for seguro, por exemplo:
                "dor no peito" -> "dor toracica", "falta de ar" -> "dispneia",
                "sono/sonolencia" -> "sonolencia", "remedio" -> "medicamento".
                Preserve estritamente os fatos informados. Nao invente sintomas, diagnosticos, exames,
                valores, condutas, doses, alergias ou historico que nao estejam no texto original.
                Se uma informacao estiver ambigua, mantenha a ambiguidade de forma profissional.
                Retorne somente o texto reescrito, sem titulo, sem markdown, sem lista e sem conduta final.
                """;

        String input = "Tipo de prontuario: " + (type == null ? "consulta" : type)
                + "\nNotas brutas:\n" + raw.trim();

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "instructions", instructions,
                    "input", input,
                    "max_output_tokens", 700,
                    "text", Map.of("format", Map.of("type", "text"))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Jsons.write(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("OpenAI retornou status " + response.statusCode()
                        + " ao formatar prontuario: " + response.body());
                return null;
            }

            String output = extractResponseText(response.body());
            return output == null ? null : ensureFinalPeriod(output.trim());
        } catch (Exception e) {
            System.err.println("Falha ao chamar OpenAI para formatar prontuario: " + e.getMessage());
            return null;
        }
    }

    private String extractResponseText(String json) throws Exception {
        JsonNode root = Jsons.MAPPER.readTree(json);
        JsonNode outputText = root.get("output_text");
        if (outputText != null && outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        List<String> parts = new ArrayList<>();
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && text.isTextual() && !text.asText().isBlank()) {
                        parts.add(text.asText());
                    }
                }
            }
        }

        return parts.isEmpty() ? null : String.join("\n", parts);
    }

    private String formalizeClinicalText(String raw) {
        String text = normalizeFreeText(raw);
        text = correctCommonWritingIssues(text);
        text = formalizeInformalClinicalTerms(text);
        text = cleanupClinicalDuplications(text);
        text = sentenceCase(text);
        return ensureFinalPeriod(text);
    }

    private String normalizeFreeText(String value) {
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.;:!?])", "$1")
                .replaceAll("([,.;:!?])([^\\s])", "$1 $2")
                .replaceAll("[.]{2,}", ".")
                .replaceAll("[,]{2,}", ",")
                .trim();
    }

    private String correctCommonWritingIssues(String text) {
        String[][] replacements = {
                {"(?i)\\bnaum\\b|\\bnao\\b", "nao"},
                {"(?i)\\bvc\\b|\\bvoce\\b", "voce"},
                {"(?i)\\bpressa[oã]\\b|\\bpresao\\b|\\bpressao\\b|\\bpress[aã]o\\b", "pressão arterial"},
                {"(?i)\\bansiedade\\b|\\bansidad[e]?\\b|\\banciedade\\b", "ansiedade"},
                {"(?i)\\brecomendano\\b|\\brecomendadno\\b", "recomendando"},
                {"(?i)\\bsonolencia\\b|\\bsonolensia\\b", "sonolência"},
                {"(?i)\\bpeqena\\b|\\bpeqenta\\b|\\bpequena\\b", "pequena"},
                {"(?i)\\bquantidad[e]?\\b|\\bqtd\\b", "quantidade"},
                {"(?i)\\bderrepente\\b|\\bde\\s+repende\\b", "de repente"},
                {"(?i)\\bpaciente\\s+ta\\b|\\bpaciente\\s+tah\\b|\\bpaciente\\s+esta\\b", "paciente apresenta"},
                {"(?i)\\bta\\s+com\\b|\\btah\\s+com\\b|\\besta\\s+com\\b", "apresenta"},
                {"(?i)\\bmto\\b|\\bmt\\b", "muito"},
                {"(?i)\\bpq\\b|\\bporque\\b", "pois"},
                {"(?i)\\bhj\\b", "hoje"},
                {"(?i)\\bontem\\b", "no dia anterior"},
                {"(?i)\\bcora[cç][aã]o\\b", "coração"},
                {"(?i)\\bcardiaco\\b", "cardíaco"},
                {"(?i)\\bcardiaca\\b", "cardíaca"},
                {"(?i)\\bcabeca\\b|\\bcabe[cç]a\\b", "cabeça"},
                {"(?i)\\bfebriu\\b|\\bfebril\\b", "febril"},
                {"(?i)\\benjoo\\b|\\benj[oô]o\\b", "náusea"},
                {"(?i)\\bvomito\\b|\\bv[oô]mito\\b", "vômito"},
                {"(?i)\\bdiarreia\\b|\\bdiarr[eé]ia\\b", "diarreia"},
                {"(?i)\\bcansaco\\b|\\bcansa[cç]o\\b", "fadiga"},
                {"(?i)\\binchaco\\b|\\bincha[cç]o\\b", "edema"},
                {"(?i)\\bdesmaio\\b", "sincope"},
                {"(?i)\\bbatimento[s]?\\s+acelerado[s]?\\b", "palpitações"},
                {"(?i)\\bremedio[s]?\\b|\\bremedinho[s]?\\b", "medicamento"},
                {"(?i)\\bexame\\s+de\\s+sangue\\b", "hemograma completo"},
                {"(?i)\\b9\\s+por\\s+6\\b", "90/60 mmHg"},
                {"(?i)\\b12\\s+por\\s+8\\b", "120/80 mmHg"},
                {"(?i)\\b14\\s+por\\s+9\\b", "140/90 mmHg"},
                {"(?i)\\b16\\s+por\\s+10\\b", "160/100 mmHg"}
        };
        return applyReplacements(text, replacements);
    }

    private String formalizeInformalClinicalTerms(String text) {
        String[][] replacements = {
                {"(?i)\\bdor\\s+no\\s+peito\\b|\\bdor\\s+peito\\b", "dor toracica (precordialgia)"},
                {"(?i)\\bfalta\\s+de\\s+ar\\b", "dispneia"},
                {"(?i)\\bdor\\s+de\\s+cabeca\\b|\\bdor\\s+na\\s+cabeca\\b", "cefaleia"},
                {"(?i)\\bdor\\s+na\\s+barriga\\b|\\bdor\\s+abdominal\\b", "dor abdominal"},
                {"(?i)\\bbarriga\\b", "abdome"},
                {"(?i)\\bdisse\\s+que\\b", "refere que"},
                {"(?i)\\bfalou\\s+que\\b", "relata que"},
                {"(?i)\\bqueixa\\s+de\\b", "relata"},
                {"(?i)\\bvem\\s+sentindo\\b", "refere"},
                {"(?i)\\bsente\\b", "refere"},
                {"(?i)\\bchega\\s+a\\b", "atinge"},
                {"(?i)\\bem\\s+picos\\s+de\\s+ansiedade\\b", "durante episódios de ansiedade"},
                {"(?i)\\brecomendando\\s+consumir\\s+sal\\b", "com recomendação de ingestão de sal"},
                {"(?i)\\bao\\s+sentir\\s+sonol[eê]ncia\\b", "ao apresentar sonolência"},
                {"(?i)\\bmelhorou\\b", "evoluiu com melhora clínica"},
                {"(?i)\\bpiorou\\b", "evoluiu com piora clínica"},
                {"(?i)\\bnormal\\b", "dentro dos parâmetros de normalidade"},
                {"(?i)\\brapido\\b", "acelerado"},
                {"(?i)\\bdevagar\\b", "lentificado"}
        };
        return applyReplacements(text, replacements);
    }

    private String cleanupClinicalDuplications(String text) {
        return text
                .replaceAll("(?i)press[aã]o arterial\\s+arterial", "pressão arterial")
                .replaceAll("(?i)press[aã]o arterial\\s+atinge", "pressão arterial atinge")
                .replaceAll("(?i)dentro dos par[aâ]metros de normalidade\\s+dos par[aâ]metros de normalidade",
                        "dentro dos parâmetros de normalidade")
                .replaceAll("(?i)paciente apresenta\\s+apresenta", "paciente apresenta")
                .replaceAll("(?i)relata\\s+relata", "relata")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String applyReplacements(String text, String[][] replacements) {
        String result = text;
        for (String[] replacement : replacements) {
            result = result.replaceAll(replacement[0], replacement[1]);
        }
        return result;
    }

    private String sentenceCase(String text) {
        String lower = text.toLowerCase();
        Matcher matcher = Pattern.compile("(^|[.!?]\\s+)([a-z])").matcher(lower);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2).toUpperCase());
        }
        matcher.appendTail(sb);
        String result = sb.toString();

        String[][] fixedTerms = {
                {"\\blgpd\\b", "LGPD"},
                {"\\bpa\\b", "PA"},
                {"\\bfc\\b", "FC"},
                {"\\becg\\b", "ECG"},
                {"\\buti\\b", "UTI"},
                {"\\bbpm\\b", "bpm"},
                {"\\bmmhg\\b", "mmHg"},
                {"\\bcovid\\b", "COVID"}
        };
        return applyReplacements(result, fixedTerms);
    }

    private String ensureFinalPeriod(String text) {
        if (text.isBlank()) {
            return "";
        }
        return text.matches(".*[.!?]$") ? text : text + ".";
    }

    private String currentUserId() {
        String userId = RequestContext.currentUserId();
        if (userId == null) {
            throw new HttpException(401, "Usuario nao autenticado");
        }
        return userId;
    }
}

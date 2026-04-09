package com.medflow.service;

import com.medflow.dto.CreateRecordRequest;
import com.medflow.dto.EditRecordRequest;
import com.medflow.model.MedicalRecord;
import com.medflow.model.RecordEditHistory;
import com.medflow.model.User;
import com.medflow.repository.MedicalRecordRepository;
import com.medflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecordService {

    private final MedicalRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public RecordService(MedicalRecordRepository recordRepo,
                         UserRepository userRepo,
                         AuditService audit) {
        this.recordRepo = recordRepo;
        this.userRepo   = userRepo;
        this.audit      = audit;
    }

    public List<MedicalRecord> findAll(String q) {
        if (q == null || q.isBlank()) return recordRepo.findAll();
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prontuário não encontrado"));
    }

    @Transactional
    public MedicalRecord create(CreateRecordRequest req) {
        String doctorId = currentUserId();
        User doctor  = userRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        User patient = userRepo.findById(req.getPatientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado"));

        MedicalRecord rec = MedicalRecord.builder()
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
                .build();

        MedicalRecord saved = recordRepo.save(rec);
        audit.log("RECORD_CREATE", "info",
                "Prontuário criado para " + patient.getName(),
                doctor.getId(), doctor.getName(), doctor.getRole());
        return saved;
    }

    @Transactional
    public MedicalRecord edit(String recordId, EditRecordRequest req) {
        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        MedicalRecord rec = findById(recordId);

        if (!rec.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode editar seus próprios prontuários");
        }

        RecordEditHistory hist = RecordEditHistory.builder()
                .id("eh" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .record(rec)
                .editedBy(doctor.getId())
                .editedByName(doctor.getName())
                .changes("Prontuário editado.")
                .build();
        rec.getEditHistory().add(hist);

        rec.setType(req.getType());
        rec.setRawNotes(req.getRawNotes());
        rec.setFormattedNotes(req.getFormattedNotes());
        rec.setDiagnosis(req.getDiagnosis());

        MedicalRecord saved = recordRepo.save(rec);
        audit.log("RECORD_EDIT", "warning",
                "Prontuário de " + rec.getPatientName() + " editado",
                doctor.getId(), doctor.getName(), doctor.getRole());
        return saved;
    }

    /** Formata notas (equivalente ao simulateAIFormatting do frontend) */
    public String formatNotes(String raw, String type) {
        if (raw == null || raw.isBlank()) return "";
        String text = raw.trim();

        // Capitaliza a primeira letra de cada frase usando Matcher (compatível com Java 8+)
        Matcher matcher = Pattern.compile("(^|[.!?]\\s+)([a-z])").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2).toUpperCase());
        }
        matcher.appendTail(sb);
        text = sb.toString();

        // Garante maiúscula na primeira letra
        if (!text.isEmpty()) {
            text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }

        // Substituições terminológicas
        text = text.replaceAll("(?i)pressao",        "pressão arterial");
        text = text.replaceAll("(?i)dor no peito",   "dor torácica (precordialgia)");
        text = text.replaceAll("(?i)remedio",         "fármaco");
        text = text.replaceAll("(?i)melhorou",        "apresentou melhora clínica significativa");
        text = text.replaceAll("(?i)piorou",          "apresentou piora do quadro clínico");
        text = text.replaceAll("(?i)exame de sangue", "hemograma completo");

        if (!text.endsWith(".")) text += ".";

        String typeLabel = "cirurgia".equals(type) ? "RELATÓRIO CIRÚRGICO" : "RELATÓRIO DE CONSULTA";
        String conduta   = "cirurgia".equals(type)
                ? "Conduta pós-operatória: monitorar sinais vitais, avaliar evolução e agendar retorno."
                : "Conduta: avaliar evolução do quadro e reavaliar em próxima consulta.";

        return typeLabel + " - " + LocalDate.now() + "\n\n" + text + "\n\n" + conduta;
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

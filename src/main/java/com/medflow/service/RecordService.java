package com.medflow.service;

import com.medflow.dto.CreateRecordRequest;
import com.medflow.dto.EditRecordRequest;
import com.medflow.http.HttpException;
import com.medflow.http.RequestContext;
import com.medflow.model.MedicalRecord;
import com.medflow.model.RecordEditHistory;
import com.medflow.model.User;
import com.medflow.repository.MedicalRecordRepository;
import com.medflow.repository.UserRepository;
import com.medflow.util.Validation;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecordService {

    private final MedicalRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public RecordService(MedicalRecordRepository recordRepo,
                         UserRepository userRepo,
                         AuditService audit) {
        this.recordRepo = recordRepo;
        this.userRepo = userRepo;
        this.audit = audit;
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
        String text = raw.trim();

        Matcher matcher = Pattern.compile("(^|[.!?]\\s+)([a-z])").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2).toUpperCase());
        }
        matcher.appendTail(sb);
        text = sb.toString();

        if (!text.isEmpty()) {
            text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }

        text = text.replaceAll("(?i)pressao", "pressao arterial");
        text = text.replaceAll("(?i)dor no peito", "dor toracica (precordialgia)");
        text = text.replaceAll("(?i)remedio", "farmaco");
        text = text.replaceAll("(?i)melhorou", "apresentou melhora clinica significativa");
        text = text.replaceAll("(?i)piorou", "apresentou piora do quadro clinico");
        text = text.replaceAll("(?i)exame de sangue", "hemograma completo");

        if (!text.endsWith(".")) {
            text += ".";
        }

        String typeLabel = "cirurgia".equals(type) ? "RELATORIO CIRURGICO" : "RELATORIO DE CONSULTA";
        String conduta = "cirurgia".equals(type)
                ? "Conduta pos-operatoria: monitorar sinais vitais, avaliar evolucao e agendar retorno."
                : "Conduta: avaliar evolucao do quadro e reavaliar em proxima consulta.";

        return typeLabel + " - " + LocalDate.now() + "\n\n" + text + "\n\n" + conduta;
    }

    private String currentUserId() {
        String userId = RequestContext.currentUserId();
        if (userId == null) {
            throw new HttpException(401, "Usuario nao autenticado");
        }
        return userId;
    }
}

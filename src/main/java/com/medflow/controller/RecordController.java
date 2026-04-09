package com.medflow.controller;

import com.medflow.dto.CreateRecordRequest;
import com.medflow.dto.EditRecordRequest;
import com.medflow.dto.FormatNotesRequest;
import com.medflow.model.MedicalRecord;
import com.medflow.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * GET /api/records?q=busca
     * Médico/Admin vê todos; filtro opcional por texto
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<MedicalRecord>> list(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(recordService.findAll(q));
    }

    /**
     * GET /api/records/my  – prontuários do médico logado
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MedicalRecord>> myRecords(Authentication auth) {
        return ResponseEntity.ok(recordService.findByDoctor((String) auth.getPrincipal()));
    }

    /**
     * GET /api/records/patient/{patientId}  – prontuários do paciente
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecord>> byPatient(
            @PathVariable String patientId, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        // Paciente só vê os seus próprios; médico/admin vê qualquer um
        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient && !userId.equals(patientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(recordService.findByPatient(patientId));
    }

    /**
     * GET /api/records/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> get(@PathVariable String id) {
        return ResponseEntity.ok(recordService.findById(id));
    }

    /**
     * POST /api/records  – criar prontuário (médico)
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecord> create(@Valid @RequestBody CreateRecordRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.create(req));
    }

    /**
     * PUT /api/records/{id}  – editar prontuário (apenas o médico dono)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecord> edit(
            @PathVariable String id,
            @Valid @RequestBody EditRecordRequest req) {
        return ResponseEntity.ok(recordService.edit(id, req));
    }

    /**
     * POST /api/records/format  – formata notas brutas (IA simulada)
     */
    @PostMapping("/format")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, String>> format(@Valid @RequestBody FormatNotesRequest req) {
        String formatted = recordService.formatNotes(req.getRawNotes(), req.getType());
        return ResponseEntity.ok(Map.of("formattedNotes", formatted));
    }
}

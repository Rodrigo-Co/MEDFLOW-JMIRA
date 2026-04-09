package com.medflow.controller;

import com.medflow.dto.*;
import com.medflow.model.User;
import com.medflow.service.UserService;
import com.medflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── Patients ──────────────────────────────────────────────

    /**
     * GET /api/patients  – médico/admin lista pacientes
     */
    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<UserDto>> listPatients() {
        return ResponseEntity.ok(
            userService.allPatients().stream().map(AuthService::toDto).toList()
        );
    }

    /**
     * GET /api/patients/{id}
     */
    @GetMapping("/patients/{id}")
    public ResponseEntity<UserDto> getPatient(@PathVariable String id, Authentication auth) {
        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient && !auth.getPrincipal().equals(id))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(AuthService.toDto(userService.getPatient(id)));
    }

    /**
     * POST /api/patients  – médico cadastra paciente
     */
    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<UserDto> createPatient(@Valid @RequestBody CreatePatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthService.toDto(userService.createPatient(req)));
    }

    /**
     * PUT /api/patients/{id}/profile  – paciente atualiza próprio perfil
     */
    @PutMapping("/patients/{id}/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        if (!auth.getPrincipal().equals(id))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        User updated = userService.updatePatientProfile(
                id, body.get("name"), body.get("email"), body.get("phone"));
        return ResponseEntity.ok(AuthService.toDto(updated));
    }

    // ── Doctors ───────────────────────────────────────────────

    /**
     * GET /api/admin/doctors
     */
    @GetMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> listDoctors() {
        return ResponseEntity.ok(
            userService.allDoctors().stream().map(AuthService::toDto).toList()
        );
    }

    /**
     * POST /api/admin/doctors
     */
    @PostMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createDoctor(@Valid @RequestBody CreateDoctorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthService.toDto(userService.createDoctor(req)));
    }

    /**
     * PUT /api/admin/doctors/{id}
     */
    @PutMapping("/admin/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateDoctor(
            @PathVariable String id,
            @RequestBody UpdateDoctorRequest req) {
        return ResponseEntity.ok(AuthService.toDto(userService.updateDoctor(id, req)));
    }
}

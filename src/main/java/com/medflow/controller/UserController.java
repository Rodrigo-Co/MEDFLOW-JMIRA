package com.medflow.controller;

import com.medflow.dto.ChangePasswordRequest;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.CreatePatientRequest;
import com.medflow.dto.UpdateDoctorRequest;
import com.medflow.dto.UserDto;
import com.medflow.model.User;
import com.medflow.service.AuthService;
import com.medflow.service.UserService;
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

    // ── Patients ──────────────────────────────────────────────────────────────

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<UserDto>> listPatients() {
        return ResponseEntity.ok(
            userService.allPatients().stream().map(AuthService::toDto).toList()
        );
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<UserDto> getPatient(@PathVariable String id, Authentication auth) {
        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient && !auth.getPrincipal().equals(id))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(AuthService.toDto(userService.getPatient(id)));
    }

    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<UserDto> createPatient(@Valid @RequestBody CreatePatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthService.toDto(userService.createPatient(req)));
    }

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

    // ── Doctors ───────────────────────────────────────────────────────────────

    @GetMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> listDoctors() {
        return ResponseEntity.ok(
            userService.allDoctors().stream().map(AuthService::toDto).toList()
        );
    }

    @PostMapping("/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createDoctor(@Valid @RequestBody CreateDoctorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthService.toDto(userService.createDoctor(req)));
    }

    @PutMapping("/admin/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateDoctor(
            @PathVariable String id,
            @RequestBody UpdateDoctorRequest req) {
        return ResponseEntity.ok(AuthService.toDto(userService.updateDoctor(id, req)));
    }

    // ── Troca de senha (qualquer usuario autenticado) ─────────────────────────

    @PostMapping("/users/change-password")
    public ResponseEntity<Map<String,String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication auth) {
        String userId = (String) auth.getPrincipal();
        userService.changePassword(userId, req);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }
}

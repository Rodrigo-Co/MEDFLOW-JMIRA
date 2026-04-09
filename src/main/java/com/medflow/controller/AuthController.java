package com.medflow.controller;

import com.medflow.dto.LoginResponse;
import com.medflow.dto.PatientLoginRequest;
import com.medflow.dto.StaffLoginRequest;
import com.medflow.dto.UserDto;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.security.JwtUtil;
import com.medflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          UserRepository userRepo,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.userRepo    = userRepo;
        this.jwtUtil     = jwtUtil;
    }

    /**
     * POST /api/auth/patient/login
     * Body: { "patientId": "p1" }
     */
    @PostMapping("/patient/login")
    public ResponseEntity<LoginResponse> loginPatient(@Valid @RequestBody PatientLoginRequest req) {
        return ResponseEntity.ok(authService.loginPatient(req.getPatientId()));
    }

    /**
     * POST /api/auth/staff/login
     * Body: { "username": "ricardo.mendes", "role": "doctor" }
     */
    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponse> loginStaff(@Valid @RequestBody StaffLoginRequest req) {
        return ResponseEntity.ok(authService.loginStaff(req.getUsername(), req.getRole()));
    }

    /**
     * GET /api/auth/me  – retorna o usuário autenticado
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(AuthService.toDto(user));
    }

    /**
     * GET /api/auth/patients  – lista pacientes para tela de seleção
     */
    @GetMapping("/patients")
    public ResponseEntity<List<UserDto>> listPatients() {
        return ResponseEntity.ok(
            userRepo.findByRole("patient").stream().map(AuthService::toDto).toList()
        );
    }

    /**
     * GET /api/auth/staff?role=doctor|admin
     */
    @GetMapping("/staff")
    public ResponseEntity<List<UserDto>> listStaff(@RequestParam String role) {
        if (!List.of("doctor", "admin").contains(role))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role inválida");
        return ResponseEntity.ok(
            userRepo.findByRole(role).stream().map(AuthService::toDto).toList()
        );
    }
}

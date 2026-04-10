package com.medflow.controller;

import com.medflow.dto.CreatePatientRequest;
import com.medflow.dto.LoginResponse;
import com.medflow.dto.PatientLoginRequest;
import com.medflow.dto.Pre2FAResponse;
import com.medflow.dto.StaffLoginRequest;
import com.medflow.dto.UserDto;
import com.medflow.dto.Verify2FARequest;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.service.AuthService;
import com.medflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService    authService;
    private final UserRepository userRepo;
    private final UserService    userService;

    public AuthController(AuthService authService, UserRepository userRepo, UserService userService) {
        this.authService = authService;
        this.userRepo    = userRepo;
        this.userService = userService;
    }

    /** PASSO 1A: Paciente envia email+senha -> recebe codigo 2FA */
    @PostMapping("/patient/login")
    public ResponseEntity<Pre2FAResponse> patientLogin(@Valid @RequestBody PatientLoginRequest req) {
        return ResponseEntity.ok(authService.initPatientLogin(req.getEmail(), req.getPassword()));
    }

    /** AUTO-CADASTRO: Paciente se cadastra publicamente (sem autenticacao) */
    @PostMapping("/patient/register")
    public ResponseEntity<UserDto> patientSelfRegister(@Valid @RequestBody CreatePatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthService.toDto(userService.createPatientSelf(req)));
    }

    /** PASSO 1B: Medico envia credenciais -> recebe codigo 2FA no email */
    @PostMapping("/doctor/login")
    public ResponseEntity<Pre2FAResponse> doctorLogin(@Valid @RequestBody StaffLoginRequest req) {
        return ResponseEntity.ok(authService.initDoctorLogin(req.getUsername(), req.getPassword()));
    }

    /** PASSO 1C: Admin entra diretamente, SEM 2FA */
    @PostMapping("/admin/login")
    public ResponseEntity<LoginResponse> adminLogin(@Valid @RequestBody StaffLoginRequest req) {
        return ResponseEntity.ok(authService.loginAdmin(req.getUsername(), req.getPassword()));
    }

    /** PASSO 2: Verifica o codigo 2FA e emite o JWT definitivo */
    @PostMapping("/verify-2fa")
    public ResponseEntity<LoginResponse> verify2FA(@Valid @RequestBody Verify2FARequest req) {
        try {
            return ResponseEntity.ok(authService.verify2FA(req.getSessionToken(), req.getCode()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /** Reenvia o codigo 2FA */
    @PostMapping("/resend-2fa")
    public ResponseEntity<Map<String, String>> resend2FA(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String username = body.get("username");
        String password = body.get("password");
        Pre2FAResponse resp = (email != null)
                ? authService.initPatientLogin(email, password)
                : authService.initDoctorLogin(username, password);
        return ResponseEntity.ok(Map.of(
                "sessionToken", resp.getSessionToken(),
                "maskedEmail",  resp.getMaskedEmail()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(AuthService.toDto(user));
    }

    @GetMapping("/patients")
    public ResponseEntity<List<UserDto>> listPatients() {
        return ResponseEntity.ok(userRepo.findByRole("patient").stream().map(AuthService::toDto).toList());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<UserDto>> listStaff(@RequestParam String role) {
        if (!List.of("doctor", "admin").contains(role))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role invalida");
        return ResponseEntity.ok(userRepo.findByRole(role).stream().map(AuthService::toDto).toList());
    }
}

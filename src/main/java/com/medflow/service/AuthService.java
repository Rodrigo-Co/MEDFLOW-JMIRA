package com.medflow.service;

import com.medflow.dto.LoginResponse;
import com.medflow.dto.Pre2FAResponse;
import com.medflow.dto.UserDto;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository   userRepo;
    private final JwtUtil          jwtUtil;
    private final AuditService     audit;
    private final PasswordEncoder  passwordEncoder;
    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepo, JwtUtil jwtUtil,
                       AuditService audit, PasswordEncoder passwordEncoder,
                       TwoFactorService twoFactorService) {
        this.userRepo         = userRepo;
        this.jwtUtil          = jwtUtil;
        this.audit            = audit;
        this.passwordEncoder  = passwordEncoder;
        this.twoFactorService = twoFactorService;
    }

    /** Passo 1 - Paciente: valida email+senha, envia codigo 2FA por email */
    public Pre2FAResponse initPatientLogin(String email, String rawPassword) {
        User user = userRepo.findByEmailAndRole(email, "patient")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos"));
        checkPassword(rawPassword, user);
        String token = twoFactorService.generateAndSend(user.getId(), user.getName(), user.getEmail());
        audit.log("LOGIN_2FA_SENT", "info", "Codigo 2FA enviado: " + TwoFactorService.maskEmail(user.getEmail()),
                user.getId(), user.getName(), user.getRole());
        return new Pre2FAResponse(token, TwoFactorService.maskEmail(user.getEmail()));
    }

    /** Passo 1 - Medico: valida senha, envia codigo 2FA por email */
    public Pre2FAResponse initDoctorLogin(String username, String rawPassword) {
        User user = userRepo.findByUsernameAndRole(username, "doctor")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
        checkPassword(rawPassword, user);
        String token = twoFactorService.generateAndSend(user.getId(), user.getName(), user.getEmail());
        audit.log("LOGIN_2FA_SENT", "info", "Codigo 2FA enviado: " + TwoFactorService.maskEmail(user.getEmail()),
                user.getId(), user.getName(), user.getRole());
        return new Pre2FAResponse(token, TwoFactorService.maskEmail(user.getEmail()));
    }

    /** Admin: login direto, SEM 2FA */
    public LoginResponse loginAdmin(String username, String rawPassword) {
        User user = userRepo.findByUsernameAndRole(username, "admin")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin nao encontrado"));
        checkPassword(rawPassword, user);
        String jwt = jwtUtil.generate(user.getId(), user.getRole());
        audit.log("LOGIN", "info", "Login admin realizado", user.getId(), user.getName(), user.getRole());
        return new LoginResponse(jwt, toDto(user));
    }

    /** Passo 2 - Verifica codigo 2FA e emite JWT definitivo */
    public LoginResponse verify2FA(String sessionToken, String code) {
        String userId = twoFactorService.verify(sessionToken, code);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String jwt = jwtUtil.generate(user.getId(), user.getRole());
        audit.log("LOGIN", "info", "Login 2FA concluido", user.getId(), user.getName(), user.getRole());
        return new LoginResponse(jwt, toDto(user));
    }

    private void checkPassword(String raw, User user) {
        if (user.getPasswordHash() == null || !passwordEncoder.matches(raw, user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
    }

    public static UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());           dto.setName(u.getName());
        dto.setEmail(u.getEmail());     dto.setPhone(u.getPhone());
        dto.setCpf(u.getCpf());         dto.setRole(u.getRole());
        dto.setTitle(u.getTitle());     dto.setSpecialty(u.getSpecialty());
        dto.setCrm(u.getCrm());         dto.setUsername(u.getUsername());
        dto.setAttendances(u.getAttendances());
        dto.setSurgeries(u.getSurgeries());
        dto.setExams(u.getExams());
        return dto;
    }
}

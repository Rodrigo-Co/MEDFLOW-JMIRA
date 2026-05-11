package com.medflow.service;

import com.medflow.dto.LoginResponse;
import com.medflow.dto.Pre2FAResponse;
import com.medflow.dto.UserDto;
import com.medflow.http.HttpException;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.security.JwtUtil;
import com.medflow.util.PasswordHasher;
import com.medflow.util.Validation;

public class AuthService {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final AuditService audit;
    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepo, JwtUtil jwtUtil,
                       AuditService audit, TwoFactorService twoFactorService) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.audit = audit;
        this.twoFactorService = twoFactorService;
    }

    public Pre2FAResponse initPatientLogin(String email, String rawPassword) {
        Validation.notBlank(email, "E-mail obrigatorio");
        Validation.notBlank(rawPassword, "Senha obrigatoria");
        User user = userRepo.findByEmailAndRole(email, "patient")
                .orElseThrow(() -> new HttpException(401, "E-mail ou senha incorretos"));
        checkPassword(rawPassword, user, "E-mail ou senha incorretos");
        String token = twoFactorService.generateAndSend(user.getId(), user.getName(), user.getEmail());
        audit.log("LOGIN_2FA_SENT", "info", "Codigo 2FA enviado: " + TwoFactorService.maskEmail(user.getEmail()),
                user.getId(), user.getName(), user.getRole());
        return new Pre2FAResponse(token, TwoFactorService.maskEmail(user.getEmail()));
    }

    public Pre2FAResponse initDoctorLogin(String username, String rawPassword) {
        Validation.notBlank(username, "Usuario obrigatorio");
        Validation.notBlank(rawPassword, "Senha obrigatoria");
        User user = userRepo.findByUsernameAndRole(username, "doctor")
                .orElseThrow(() -> new HttpException(401, "Usuario nao encontrado"));
        checkPassword(rawPassword, user, "Usuario ou senha incorretos");
        String token = twoFactorService.generateAndSend(user.getId(), user.getName(), user.getEmail());
        audit.log("LOGIN_2FA_SENT", "info", "Codigo 2FA enviado: " + TwoFactorService.maskEmail(user.getEmail()),
                user.getId(), user.getName(), user.getRole());
        return new Pre2FAResponse(token, TwoFactorService.maskEmail(user.getEmail()));
    }

    public LoginResponse loginAdmin(String username, String rawPassword) {
        Validation.notBlank(username, "Usuario obrigatorio");
        Validation.notBlank(rawPassword, "Senha obrigatoria");
        User user = userRepo.findByUsernameAndRole(username, "admin")
                .orElseThrow(() -> new HttpException(401, "Admin nao encontrado"));
        checkPassword(rawPassword, user, "Usuario ou senha incorretos");
        String jwt = jwtUtil.generate(user.getId(), user.getRole());
        audit.log("LOGIN", "info", "Login admin realizado", user.getId(), user.getName(), user.getRole());
        return new LoginResponse(jwt, toDto(user));
    }

    public LoginResponse verify2FA(String sessionToken, String code) {
        Validation.notBlank(sessionToken, "sessionToken obrigatorio");
        Validation.notBlank(code, "Codigo obrigatorio");
        String userId = twoFactorService.verify(sessionToken, code);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new HttpException(401, "Usuario nao encontrado"));
        String jwt = jwtUtil.generate(user.getId(), user.getRole());
        audit.log("LOGIN", "info", "Login 2FA concluido", user.getId(), user.getName(), user.getRole());
        return new LoginResponse(jwt, toDto(user));
    }

    private void checkPassword(String raw, User user, String message) {
        if (!PasswordHasher.matches(raw, user.getPasswordHash())) {
            throw new HttpException(401, message);
        }
    }

    public static UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setCpf(u.getCpf());
        dto.setRole(u.getRole());
        dto.setTitle(u.getTitle());
        dto.setSpecialty(u.getSpecialty());
        dto.setCrm(u.getCrm());
        dto.setUsername(u.getUsername());
        dto.setAttendances(u.getAttendances());
        dto.setSurgeries(u.getSurgeries());
        dto.setExams(u.getExams());
        return dto;
    }
}

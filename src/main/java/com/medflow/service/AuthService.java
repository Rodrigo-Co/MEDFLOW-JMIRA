package com.medflow.service;

import com.medflow.dto.*;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final AuditService audit;

    public AuthService(UserRepository userRepo, JwtUtil jwtUtil, AuditService audit) {
        this.userRepo = userRepo;
        this.jwtUtil  = jwtUtil;
        this.audit    = audit;
    }

    /** Login para paciente – selecionado por ID (sem senha no frontend demo) */
    public LoginResponse loginPatient(String patientId) {
        User user = userRepo.findById(patientId)
                .filter(u -> "patient".equals(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Paciente não encontrado"));

        String token = jwtUtil.generate(user.getId(), user.getRole());
        audit.log("LOGIN", "info", "Login realizado", user.getId(), user.getName(), user.getRole());
        return new LoginResponse(token, toDto(user));
    }

    /** Login para médico/admin – por username */
    public LoginResponse loginStaff(String username, String role) {
        User user = userRepo.findByUsernameAndRole(username, role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        String token = jwtUtil.generate(user.getId(), user.getRole());
        audit.log("LOGIN", "info", "Login realizado", user.getId(), user.getName(), user.getRole());
        return new LoginResponse(token, toDto(user));
    }

    public static UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId()); dto.setName(u.getName());
        dto.setEmail(u.getEmail()); dto.setPhone(u.getPhone());
        dto.setCpf(u.getCpf()); dto.setRole(u.getRole());
        dto.setTitle(u.getTitle()); dto.setSpecialty(u.getSpecialty());
        dto.setCrm(u.getCrm()); dto.setUsername(u.getUsername());
        dto.setAttendances(u.getAttendances());
        dto.setSurgeries(u.getSurgeries());
        dto.setExams(u.getExams());
        return dto;
    }
}

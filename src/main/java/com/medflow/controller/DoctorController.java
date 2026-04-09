package com.medflow.controller;

import com.medflow.dto.CreateChangeRequest;
import com.medflow.dto.UserDto;
import com.medflow.model.DataChangeRequest;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.service.AuthService;
import com.medflow.service.ChangeRequestService;
import com.medflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final UserRepository       userRepo;
    private final UserService          userService;
    private final ChangeRequestService changeRequestService;

    public DoctorController(UserRepository userRepo,
                             UserService userService,
                             ChangeRequestService changeRequestService) {
        this.userRepo              = userRepo;
        this.userService           = userService;
        this.changeRequestService  = changeRequestService;
    }

    /**
     * GET /api/doctor/profile  – perfil do médico logado
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDto> profile(Authentication auth) {
        String id = (String) auth.getPrincipal();
        User doctor = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(AuthService.toDto(doctor));
    }

    /**
     * PUT /api/doctor/profile  – atualiza nome e telefone (sem aprovação)
     * Alteração de e-mail gera change request automático
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String doctorId = (String) auth.getPrincipal();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        String newEmail = body.get("email");
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(doctor.getEmail())) {
            // Envia change request para o admin aprovar
            CreateChangeRequest cr = new CreateChangeRequest();
            cr.setFieldName("email");
            cr.setOldValue(doctor.getEmail());
            cr.setNewValue(newEmail);
            changeRequestService.submit(cr);
        }

        if (body.containsKey("name")  && !body.get("name").isBlank())  doctor.setName(body.get("name"));
        if (body.containsKey("phone"))                                   doctor.setPhone(body.get("phone"));
        User saved = userRepo.save(doctor);
        return ResponseEntity.ok(AuthService.toDto(saved));
    }

    /**
     * POST /api/doctor/change-request  – solicita alteração de campo
     */
    @PostMapping("/change-request")
    public ResponseEntity<DataChangeRequest> submitChangeRequest(
            @Valid @RequestBody CreateChangeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(changeRequestService.submit(req));
    }
}

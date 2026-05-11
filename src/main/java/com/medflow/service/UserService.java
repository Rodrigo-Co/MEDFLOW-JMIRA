package com.medflow.service;

import com.medflow.dto.ChangePasswordRequest;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.CreatePatientRequest;
import com.medflow.dto.UpdateDoctorRequest;
import com.medflow.http.HttpException;
import com.medflow.http.RequestContext;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import com.medflow.util.PasswordHasher;
import com.medflow.util.Validation;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class UserService {

    private final UserRepository userRepo;
    private final AuditService audit;

    public UserService(UserRepository userRepo, AuditService audit) {
        this.userRepo = userRepo;
        this.audit = audit;
    }

    public List<User> allPatients() {
        return userRepo.findByRole("patient");
    }

    public User getPatient(String id) {
        return userRepo.findById(id)
                .filter(user -> "patient".equals(user.getRole()))
                .orElseThrow(() -> new HttpException(404, "Paciente nao encontrado"));
    }

    public User createPatient(CreatePatientRequest req) {
        validatePatientRequest(req);

        if (userRepo.existsByCpf(req.getCpf())) {
            throw new HttpException(409, "CPF ja cadastrado");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new HttpException(409, "E-mail ja cadastrado");
        }

        String callerId = currentUserId();
        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new HttpException(401, "Usuario nao autenticado"));

        User patient = User.builder()
                .id("p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .cpf(req.getCpf())
                .role("patient")
                .passwordHash(PasswordHasher.hash(req.getPassword()))
                .createdAt(OffsetDateTime.now())
                .build();

        User saved = userRepo.save(patient);
        audit.log("PATIENT_CREATE", "info",
                "Paciente " + req.getName() + " cadastrado",
                caller.getId(), caller.getName(), caller.getRole());
        return saved;
    }

    public User createPatientSelf(CreatePatientRequest req) {
        validatePatientRequest(req);

        if (userRepo.existsByCpf(req.getCpf())) {
            throw new HttpException(409, "CPF ja cadastrado");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new HttpException(409, "E-mail ja cadastrado");
        }

        User patient = User.builder()
                .id("p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .cpf(req.getCpf())
                .role("patient")
                .passwordHash(PasswordHasher.hash(req.getPassword()))
                .createdAt(OffsetDateTime.now())
                .build();

        User saved = userRepo.save(patient);
        audit.log("PATIENT_SELF_REGISTER", "info",
                "Auto-cadastro: " + req.getName(), saved.getId(), saved.getName(), saved.getRole());
        return saved;
    }

    public User updatePatientProfile(String patientId, String name, String email, String phone) {
        User patient = getPatient(patientId);
        if (name != null && !name.isBlank()) {
            patient.setName(name);
        }
        if (phone != null) {
            patient.setPhone(phone);
        }
        if (email != null && !email.isBlank() && !email.equals(patient.getEmail())) {
            patient.setEmail(email);
        }
        return userRepo.save(patient);
    }

    public List<User> allDoctors() {
        return userRepo.findByRole("doctor");
    }

    public User getDoctor(String id) {
        return userRepo.findById(id)
                .filter(user -> "doctor".equals(user.getRole()))
                .orElseThrow(() -> new HttpException(404, "Medico nao encontrado"));
    }

    public User createDoctor(CreateDoctorRequest req) {
        Validation.notBlank(req.getName(), "name obrigatorio");
        Validation.notBlank(req.getEmail(), "email obrigatorio");
        Validation.notBlank(req.getCpf(), "cpf obrigatorio");
        Validation.notBlank(req.getSpecialty(), "specialty obrigatorio");
        Validation.notBlank(req.getCrm(), "crm obrigatorio");
        Validation.notBlank(req.getPassword(), "password obrigatorio");
        Validation.minLength(req.getPassword(), 6, "Senha deve ter ao menos 6 caracteres");

        if (userRepo.existsByCpf(req.getCpf())) {
            throw new HttpException(409, "CPF ja cadastrado");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new HttpException(409, "E-mail ja cadastrado");
        }

        String callerId = currentUserId();
        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new HttpException(401, "Usuario nao autenticado"));

        User doctor = User.builder()
                .id("d" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .cpf(req.getCpf())
                .role("doctor")
                .specialty(req.getSpecialty())
                .crm(req.getCrm())
                .username(buildUsername(req.getName()))
                .attendances(0)
                .surgeries(0)
                .exams(0)
                .passwordHash(PasswordHasher.hash(req.getPassword()))
                .createdAt(OffsetDateTime.now())
                .build();

        User saved = userRepo.save(doctor);
        audit.log("DOCTOR_CREATE", "info",
                "Medico " + req.getName() + " cadastrado",
                caller.getId(), caller.getName(), caller.getRole());
        return saved;
    }

    public User updateDoctor(String id, UpdateDoctorRequest req) {
        User doctor = getDoctor(id);
        String callerId = currentUserId();
        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new HttpException(401, "Usuario nao autenticado"));

        if (req.getName() != null && !req.getName().isBlank()) {
            doctor.setName(req.getName());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            doctor.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            doctor.setPhone(req.getPhone());
        }
        if (req.getSpecialty() != null && !req.getSpecialty().isBlank()) {
            doctor.setSpecialty(req.getSpecialty());
        }
        if (req.getCrm() != null && !req.getCrm().isBlank()) {
            doctor.setCrm(req.getCrm());
        }

        User saved = userRepo.save(doctor);
        audit.log("DOCTOR_UPDATE", "warning",
                "Dados do medico " + doctor.getName() + " atualizados",
                caller.getId(), caller.getName(), caller.getRole());
        return saved;
    }

    public void changePassword(String userId, ChangePasswordRequest req) {
        Validation.notBlank(req.getCurrentPassword(), "Senha atual obrigatoria");
        Validation.notBlank(req.getNewPassword(), "Nova senha obrigatoria");
        Validation.minLength(req.getNewPassword(), 6, "Nova senha deve ter ao menos 6 caracteres");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new HttpException(404, "Usuario nao encontrado"));

        if (!PasswordHasher.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new HttpException(401, "Senha atual incorreta");
        }

        user.setPasswordHash(PasswordHasher.hash(req.getNewPassword()));
        userRepo.save(user);

        audit.log("PASSWORD_CHANGE", "warning",
                "Senha alterada pelo usuario",
                user.getId(), user.getName(), user.getRole());
    }

    private void validatePatientRequest(CreatePatientRequest req) {
        Validation.notBlank(req.getName(), "name obrigatorio");
        Validation.notBlank(req.getEmail(), "email obrigatorio");
        Validation.notBlank(req.getCpf(), "cpf obrigatorio");
        Validation.notBlank(req.getPassword(), "password obrigatorio");
        Validation.minLength(req.getPassword(), 6, "Senha deve ter ao menos 6 caracteres");
    }

    private String buildUsername(String fullName) {
        String clean = fullName.replaceAll("(?i)^(Dr\\.|Dra\\.)\\s*", "").trim();
        clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z\\s]", "");
        String[] parts = clean.trim().split("\\s+");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : parts[0];
    }

    private String currentUserId() {
        String userId = RequestContext.currentUserId();
        if (userId == null) {
            throw new HttpException(401, "Usuario nao autenticado");
        }
        return userId;
    }
}

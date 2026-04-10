package com.medflow.service;

import com.medflow.dto.ChangePasswordRequest;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.CreatePatientRequest;
import com.medflow.dto.UpdateDoctorRequest;
import com.medflow.model.User;
import com.medflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository  userRepo;
    private final AuditService    audit;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, AuditService audit, PasswordEncoder passwordEncoder) {
        this.userRepo        = userRepo;
        this.audit           = audit;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Patients ──────────────────────────────────────────────────────────────

    public List<User> allPatients() { return userRepo.findByRole("patient"); }

    public User getPatient(String id) {
        return userRepo.findById(id)
                .filter(u -> "patient".equals(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado"));
    }

    @Transactional
    public User createPatient(CreatePatientRequest req) {
        if (userRepo.existsByCpf(req.getCpf()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        if (userRepo.existsByEmail(req.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");

        String callerId = currentUserId();
        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // Senha é hashada com BCrypt antes de persistir
        String hash = passwordEncoder.encode(req.getPassword());

        User p = User.builder()
                .id("p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .cpf(req.getCpf())
                .role("patient")
                .passwordHash(hash)
                .build();

        User saved = userRepo.save(p);
        audit.log("PATIENT_CREATE", "info",
                "Paciente " + req.getName() + " cadastrado",
                caller.getId(), caller.getName(), caller.getRole());
        return saved;
    }


    /** Auto-cadastro publico: paciente se registra sem autenticacao */
    @Transactional
    public User createPatientSelf(CreatePatientRequest req) {
        if (userRepo.existsByCpf(req.getCpf()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF ja cadastrado");
        if (userRepo.existsByEmail(req.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado");

        String hash = passwordEncoder.encode(req.getPassword());
        User p = User.builder()
                .id("p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .cpf(req.getCpf())
                .role("patient")
                .passwordHash(hash)
                .build();

        User saved = userRepo.save(p);
        audit.log("PATIENT_SELF_REGISTER", "info",
                "Auto-cadastro: " + req.getName(), saved.getId(), saved.getName(), "patient");
        return saved;
    }

    @Transactional
    public User updatePatientProfile(String patientId, String name, String email, String phone) {
        User p = getPatient(patientId);
        if (name  != null && !name.isBlank())  p.setName(name);
        if (phone != null)                      p.setPhone(phone);
        if (email != null && !email.isBlank() && !email.equals(p.getEmail()))
            p.setEmail(email);
        return userRepo.save(p);
    }

    // ── Doctors ───────────────────────────────────────────────────────────────

    public List<User> allDoctors() { return userRepo.findByRole("doctor"); }

    public User getDoctor(String id) {
        return userRepo.findById(id)
                .filter(u -> "doctor".equals(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico não encontrado"));
    }

    @Transactional
    public User createDoctor(CreateDoctorRequest req) {
        if (userRepo.existsByCpf(req.getCpf()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        if (userRepo.existsByEmail(req.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");

        String username = buildUsername(req.getName());
        String callerId = currentUserId();
        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // Senha é hashada com BCrypt antes de persistir
        String hash = passwordEncoder.encode(req.getPassword());

        User d = User.builder()
                .id("d" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .cpf(req.getCpf())
                .role("doctor")
                .specialty(req.getSpecialty())
                .crm(req.getCrm())
                .username(username)
                .attendances(0).surgeries(0).exams(0)
                .passwordHash(hash)
                .build();

        User saved = userRepo.save(d);
        audit.log("DOCTOR_CREATE", "info",
                "Médico " + req.getName() + " cadastrado",
                caller.getId(), caller.getName(), caller.getRole());
        return saved;
    }

    @Transactional
    public User updateDoctor(String id, UpdateDoctorRequest req) {
        User d = getDoctor(id);
        String callerId = currentUserId();
        User caller = userRepo.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (req.getName()      != null && !req.getName().isBlank())      d.setName(req.getName());
        if (req.getEmail()     != null && !req.getEmail().isBlank())     d.setEmail(req.getEmail());
        if (req.getPhone()     != null)                                  d.setPhone(req.getPhone());
        if (req.getSpecialty() != null && !req.getSpecialty().isBlank()) d.setSpecialty(req.getSpecialty());
        if (req.getCrm()       != null && !req.getCrm().isBlank())       d.setCrm(req.getCrm());

        User saved = userRepo.save(d);
        audit.log("DOCTOR_UPDATE", "warning",
                "Dados do médico " + d.getName() + " atualizados",
                caller.getId(), caller.getName(), caller.getRole());
        return saved;
    }

    // ── Troca de senha ────────────────────────────────────────────────────────

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Verifica senha atual antes de trocar
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha atual incorreta");

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);

        audit.log("PASSWORD_CHANGE", "warning",
                "Senha alterada pelo usuário",
                user.getId(), user.getName(), user.getRole());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

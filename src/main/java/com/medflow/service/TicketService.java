package com.medflow.service;

import com.medflow.dto.CreateTicketRequest;
import com.medflow.model.Ticket;
import com.medflow.model.User;
import com.medflow.repository.TicketRepository;
import com.medflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepo;
    private final UserRepository   userRepo;
    private final AuditService     audit;

    public TicketService(TicketRepository ticketRepo,
                         UserRepository userRepo,
                         AuditService audit) {
        this.ticketRepo = ticketRepo;
        this.userRepo   = userRepo;
        this.audit      = audit;
    }

    public List<Ticket> findAll()                     { return ticketRepo.findAllByOrderByCreatedAtDesc(); }
    public List<Ticket> findByStatus(String status)   { return ticketRepo.findByStatus(status); }
    public List<Ticket> findByPatient(String pid)     { return ticketRepo.findByPatientIdOrderByCreatedAtDesc(pid); }

    public Ticket findById(String id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    @Transactional
    public Ticket create(CreateTicketRequest req) {
        String patientId = currentUserId();
        User patient = userRepo.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Ticket t = Ticket.builder()
                .id("t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .patientId(patient.getId())
                .patientName(patient.getName())
                .type(req.getType())
                .specialty(req.getSpecialty())
                .description(req.getDescription())
                .status("aberto")
                .build();

        Ticket saved = ticketRepo.save(t);
        audit.log("TICKET_CREATE", "info",
                "Chamado criado por " + patient.getName(),
                patient.getId(), patient.getName(), "patient");
        return saved;
    }

    @Transactional
    public Ticket accept(String id) {
        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Ticket t = findById(id);

        if (!"aberto".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chamado não está aberto");
        }
        t.setStatus("em_andamento");
        t.setDoctorId(doctor.getId());
        t.setDoctorName(doctor.getName());
        t.setUpdatedAt(LocalDate.now());

        Ticket saved = ticketRepo.save(t);
        audit.log("TICKET_UPDATE", "info",
                "Chamado " + id + " aceito pelo médico",
                doctor.getId(), doctor.getName(), "doctor");
        return saved;
    }

    @Transactional
    public Ticket complete(String id) {
        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Ticket t = findById(id);

        if (!"em_andamento".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chamado não está em andamento");
        }
        t.setStatus("concluido");
        t.setResult("Avaliação realizada com sucesso");
        t.setUpdatedAt(LocalDate.now());

        Ticket saved = ticketRepo.save(t);
        audit.log("TICKET_UPDATE", "info",
                "Chamado " + id + " concluído",
                doctor.getId(), doctor.getName(), "doctor");
        return saved;
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

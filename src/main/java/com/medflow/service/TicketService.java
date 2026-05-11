package com.medflow.service;

import com.medflow.dto.CreateTicketRequest;
import com.medflow.http.HttpException;
import com.medflow.http.RequestContext;
import com.medflow.model.Ticket;
import com.medflow.model.User;
import com.medflow.repository.TicketRepository;
import com.medflow.repository.UserRepository;
import com.medflow.util.Validation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TicketService {

    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public TicketService(TicketRepository ticketRepo,
                         UserRepository userRepo,
                         AuditService audit) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.audit = audit;
    }

    public List<Ticket> findAll() {
        return ticketRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<Ticket> findByStatus(String status) {
        return ticketRepo.findByStatus(status);
    }

    public List<Ticket> findByPatient(String patientId) {
        return ticketRepo.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Ticket findById(String id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new HttpException(404, "Chamado nao encontrado"));
    }

    public Ticket create(CreateTicketRequest req) {
        Validation.notBlank(req.getType(), "type obrigatorio");
        Validation.notBlank(req.getSpecialty(), "specialty obrigatorio");
        Validation.notBlank(req.getDescription(), "description obrigatorio");

        String patientId = currentUserId();
        User patient = userRepo.findById(patientId)
                .orElseThrow(() -> new HttpException(401, "Paciente nao autenticado"));

        Ticket ticket = Ticket.builder()
                .id("t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .patientId(patient.getId())
                .patientName(patient.getName())
                .type(req.getType())
                .specialty(req.getSpecialty())
                .description(req.getDescription())
                .status("aberto")
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .build();

        Ticket saved = ticketRepo.save(ticket);
        audit.log("TICKET_CREATE", "info",
                "Chamado criado por " + patient.getName(),
                patient.getId(), patient.getName(), patient.getRole());
        return saved;
    }

    public Ticket accept(String id) {
        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new HttpException(401, "Medico nao autenticado"));
        Ticket ticket = findById(id);

        if (!"aberto".equals(ticket.getStatus())) {
            throw new HttpException(400, "Chamado nao esta aberto");
        }

        ticket.setStatus("em_andamento");
        ticket.setDoctorId(doctor.getId());
        ticket.setDoctorName(doctor.getName());
        ticket.setUpdatedAt(LocalDate.now());

        Ticket saved = ticketRepo.save(ticket);
        audit.log("TICKET_UPDATE", "info",
                "Chamado " + id + " aceito pelo medico",
                doctor.getId(), doctor.getName(), doctor.getRole());
        return saved;
    }

    public Ticket complete(String id) {
        String doctorId = currentUserId();
        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new HttpException(401, "Medico nao autenticado"));
        Ticket ticket = findById(id);

        if (!"em_andamento".equals(ticket.getStatus())) {
            throw new HttpException(400, "Chamado nao esta em andamento");
        }

        ticket.setStatus("concluido");
        ticket.setResult("Avaliacao realizada com sucesso");
        ticket.setUpdatedAt(LocalDate.now());

        Ticket saved = ticketRepo.save(ticket);
        audit.log("TICKET_UPDATE", "info",
                "Chamado " + id + " concluido",
                doctor.getId(), doctor.getName(), doctor.getRole());
        return saved;
    }

    private String currentUserId() {
        String userId = RequestContext.currentUserId();
        if (userId == null) {
            throw new HttpException(401, "Usuario nao autenticado");
        }
        return userId;
    }
}

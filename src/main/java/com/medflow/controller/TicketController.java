package com.medflow.controller;

import com.medflow.dto.CreateTicketRequest;
import com.medflow.model.Ticket;
import com.medflow.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * GET /api/tickets?status=aberto|em_andamento|concluido
     * Médico/Admin vê todos
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<Ticket>> list(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank())
            return ResponseEntity.ok(ticketService.findByStatus(status));
        return ResponseEntity.ok(ticketService.findAll());
    }

    /**
     * GET /api/tickets/my  – chamados do paciente logado
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<Ticket>> myTickets(Authentication auth) {
        return ResponseEntity.ok(ticketService.findByPatient((String) auth.getPrincipal()));
    }

    /**
     * GET /api/tickets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> get(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    /**
     * POST /api/tickets  – paciente cria chamado
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Ticket> create(@Valid @RequestBody CreateTicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(req));
    }

    /**
     * PATCH /api/tickets/{id}/accept  – médico aceita chamado
     */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Ticket> accept(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.accept(id));
    }

    /**
     * PATCH /api/tickets/{id}/complete  – médico conclui chamado
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Ticket> complete(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.complete(id));
    }
}

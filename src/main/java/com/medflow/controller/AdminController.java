package com.medflow.controller;

import com.medflow.dto.*;
import com.medflow.model.AuditLog;
import com.medflow.model.DataChangeRequest;
import com.medflow.model.User;
import com.medflow.repository.AuditLogRepository;
import com.medflow.repository.TicketRepository;
import com.medflow.service.ChangeRequestService;
import com.medflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ChangeRequestService changeRequestService;
    private final UserService          userService;
    private final AuditLogRepository   auditRepo;
    private final TicketRepository     ticketRepo;

    public AdminController(ChangeRequestService changeRequestService,
                           UserService userService,
                           AuditLogRepository auditRepo,
                           TicketRepository ticketRepo) {
        this.changeRequestService = changeRequestService;
        this.userService          = userService;
        this.auditRepo            = auditRepo;
        this.ticketRepo           = ticketRepo;
    }

    // ── Dashboard ─────────────────────────────────────────────

    /**
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> dashboard() {
        List<User> doctors = userService.allDoctors();
        DashboardStats stats = new DashboardStats();
        stats.setTotalDoctors(doctors.size());
        stats.setTicketsOpen(ticketRepo.countByStatus("aberto"));
        stats.setTicketsInProgress(ticketRepo.countByStatus("em_andamento"));
        stats.setTicketsConcluded(ticketRepo.countByStatus("concluido"));
        stats.setPendingRequests(changeRequestService.countPending());
        stats.setDoctors(doctors.stream().map(d -> {
            DoctorStat ds = new DoctorStat();
            ds.setId(d.getId()); ds.setName(d.getName());
            ds.setSpecialty(d.getSpecialty()); ds.setCrm(d.getCrm());
            ds.setAttendances(d.getAttendances() != null ? d.getAttendances() : 0);
            ds.setSurgeries(d.getSurgeries()     != null ? d.getSurgeries()   : 0);
            ds.setExams(d.getExams()             != null ? d.getExams()       : 0);
            return ds;
        }).toList());
        return ResponseEntity.ok(stats);
    }

    // ── Change Requests ───────────────────────────────────────

    /**
     * GET /api/admin/requests?status=pendente
     */
    @GetMapping("/requests")
    public ResponseEntity<List<DataChangeRequest>> listRequests(
            @RequestParam(required = false) String status) {
        if ("pendente".equals(status))
            return ResponseEntity.ok(changeRequestService.findPending());
        return ResponseEntity.ok(changeRequestService.findAll());
    }

    /**
     * PATCH /api/admin/requests/{id}/resolve
     * Body: { "approved": true }
     */
    @PatchMapping("/requests/{id}/resolve")
    public ResponseEntity<DataChangeRequest> resolveRequest(
            @PathVariable String id,
            @Valid @RequestBody ResolveChangeRequest req) {
        return ResponseEntity.ok(changeRequestService.resolve(id, req.getApproved()));
    }

    // ── Audit Log ─────────────────────────────────────────────

    /**
     * GET /api/admin/audit?severity=info|warning|error&role=doctor|admin|patient
     */
    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> auditLog(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String role) {

        if (severity != null && !severity.isBlank() && role != null && !role.isBlank())
            return ResponseEntity.ok(auditRepo.findBySeverityAndUserRoleOrderByLogTimestampDesc(severity, role));
        if (severity != null && !severity.isBlank())
            return ResponseEntity.ok(auditRepo.findBySeverityOrderByLogTimestampDesc(severity));
        if (role != null && !role.isBlank())
            return ResponseEntity.ok(auditRepo.findByUserRoleOrderByLogTimestampDesc(role));
        return ResponseEntity.ok(auditRepo.findAllByOrderByLogTimestampDesc());
    }
}

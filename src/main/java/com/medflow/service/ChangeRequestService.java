package com.medflow.service;

import com.medflow.dto.CreateChangeRequest;
import com.medflow.model.DataChangeRequest;
import com.medflow.model.User;
import com.medflow.repository.DataChangeRequestRepository;
import com.medflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeRequestService {

    private final DataChangeRequestRepository repo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public ChangeRequestService(DataChangeRequestRepository repo,
                                UserRepository userRepo,
                                AuditService audit) {
        this.repo     = repo;
        this.userRepo = userRepo;
        this.audit    = audit;
    }

    public List<DataChangeRequest> findAll()              { return repo.findAllByOrderByCreatedAtDesc(); }
    public List<DataChangeRequest> findPending()          { return repo.findByStatusOrderByCreatedAtDesc("pendente"); }
    public long countPending()                            { return repo.countByStatus("pendente"); }

    @Transactional
    public DataChangeRequest submit(CreateChangeRequest req) {
        String userId = currentUserId();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        DataChangeRequest dcr = DataChangeRequest.builder()
                .id("dcr" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .requesterId(user.getId())
                .requesterName(user.getName())
                .requesterRole(user.getRole())
                .fieldName(req.getFieldName())
                .oldValue(req.getOldValue())
                .newValue(req.getNewValue())
                .status("pendente")
                .build();

        DataChangeRequest saved = repo.save(dcr);
        audit.log("REQUEST_CREATE", "info",
                "Solicitação de alteração de " + req.getFieldName() + " enviada",
                user.getId(), user.getName(), user.getRole());
        return saved;
    }

    @Transactional
    public DataChangeRequest resolve(String id, boolean approved) {
        String adminId = currentUserId();
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        DataChangeRequest dcr = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));

        if (!"pendente".equals(dcr.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solicitação já resolvida");

        dcr.setStatus(approved ? "aprovado" : "rejeitado");
        dcr.setResolvedAt(OffsetDateTime.now());
        dcr.setResolvedBy(admin.getId());
        dcr.setResolvedByName(admin.getName());

        DataChangeRequest saved = repo.save(dcr);
        audit.log("REQUEST_RESOLVE", "warning",
                (approved ? "Aprovou" : "Rejeitou") + " solicitação de " + dcr.getRequesterName(),
                admin.getId(), admin.getName(), "admin");
        return saved;
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

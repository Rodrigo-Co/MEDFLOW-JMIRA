package com.medflow.service;

import com.medflow.dto.CreateChangeRequest;
import com.medflow.http.HttpException;
import com.medflow.http.RequestContext;
import com.medflow.model.DataChangeRequest;
import com.medflow.model.User;
import com.medflow.repository.DataChangeRequestRepository;
import com.medflow.repository.UserRepository;
import com.medflow.util.Validation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ChangeRequestService {

    private final DataChangeRequestRepository repo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public ChangeRequestService(DataChangeRequestRepository repo,
                                UserRepository userRepo,
                                AuditService audit) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.audit = audit;
    }

    public List<DataChangeRequest> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<DataChangeRequest> findPending() {
        return repo.findByStatusOrderByCreatedAtDesc("pendente");
    }

    public long countPending() {
        return repo.countByStatus("pendente");
    }

    public DataChangeRequest submit(CreateChangeRequest req) {
        Validation.notBlank(req.getFieldName(), "fieldName obrigatorio");
        Validation.notBlank(req.getOldValue(), "oldValue obrigatorio");
        Validation.notBlank(req.getNewValue(), "newValue obrigatorio");

        String userId = currentUserId();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new HttpException(401, "Usuario nao autenticado"));

        DataChangeRequest dcr = DataChangeRequest.builder()
                .id("dcr" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .requesterId(user.getId())
                .requesterName(user.getName())
                .requesterRole(user.getRole())
                .fieldName(req.getFieldName())
                .oldValue(req.getOldValue())
                .newValue(req.getNewValue())
                .status("pendente")
                .createdAt(OffsetDateTime.now())
                .build();

        DataChangeRequest saved = repo.save(dcr);
        audit.log("REQUEST_CREATE", "info",
                "Solicitacao de alteracao de " + req.getFieldName() + " enviada",
                user.getId(), user.getName(), user.getRole());
        return saved;
    }

    public DataChangeRequest resolve(String id, boolean approved) {
        String adminId = currentUserId();
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new HttpException(401, "Usuario nao autenticado"));

        DataChangeRequest dcr = repo.findById(id)
                .orElseThrow(() -> new HttpException(404, "Solicitacao nao encontrada"));

        if (!"pendente".equals(dcr.getStatus())) {
            throw new HttpException(400, "Solicitacao ja resolvida");
        }

        dcr.setStatus(approved ? "aprovado" : "rejeitado");
        dcr.setResolvedAt(OffsetDateTime.now());
        dcr.setResolvedBy(admin.getId());
        dcr.setResolvedByName(admin.getName());

        DataChangeRequest saved = repo.save(dcr);
        audit.log("REQUEST_RESOLVE", "warning",
                (approved ? "Aprovou" : "Rejeitou") + " solicitacao de " + dcr.getRequesterName(),
                admin.getId(), admin.getName(), admin.getRole());
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

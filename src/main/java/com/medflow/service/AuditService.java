package com.medflow.service;

import com.medflow.http.RequestContext;
import com.medflow.model.AuditLog;
import com.medflow.repository.AuditLogRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String severity, String details) {
        String userId = RequestContext.currentUserId();
        repository.save(AuditLog.builder()
                .id("al" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .action(action)
                .severity(severity)
                .details(details)
                .userId(userId == null ? "system" : userId)
                .userName("-")
                .userRole("-")
                .logTimestamp(OffsetDateTime.now())
                .build());
    }

    public void log(String action, String severity, String details,
                    String userId, String userName, String userRole) {
        repository.save(AuditLog.builder()
                .id("al" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .action(action)
                .severity(severity)
                .details(details)
                .userId(userId)
                .userName(userName)
                .userRole(userRole)
                .logTimestamp(OffsetDateTime.now())
                .build());
    }
}

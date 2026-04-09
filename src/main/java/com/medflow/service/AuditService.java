package com.medflow.service;

import com.medflow.model.AuditLog;
import com.medflow.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String action, String severity, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? (String) auth.getPrincipal() : "system";

        AuditLog entry = AuditLog.builder()
                .id("al" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .action(action)
                .severity(severity)
                .details(details)
                .userId(userId)
                .userName("—")
                .userRole("—")
                .build();
        repo.save(entry);
    }

    public void log(String action, String severity, String details,
                    String userId, String userName, String userRole) {
        AuditLog entry = AuditLog.builder()
                .id("al" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .action(action)
                .severity(severity)
                .details(details)
                .userId(userId)
                .userName(userName)
                .userRole(userRole)
                .build();
        repo.save(entry);
    }
}

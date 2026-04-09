package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    private String id;

    @Column(nullable = false) private String action;
    @Column(nullable = false) private String severity; // info | warning | error

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "user_id")   private String userId;
    @Column(name = "user_name") private String userName;
    @Column(name = "user_role") private String userRole;

    @Column(name = "log_timestamp")
    private OffsetDateTime logTimestamp;

    @PrePersist
    public void prePersist() {
        if (logTimestamp == null) logTimestamp = OffsetDateTime.now();
    }
}

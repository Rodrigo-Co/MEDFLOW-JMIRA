package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// ── Solicitação de Alteração de Dados ─────────────────────────
@Entity
@Table(name = "data_change_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataChangeRequest {

    @Id
    private String id;

    @Column(name = "requester_id")   private String requesterId;
    @Column(name = "requester_name") private String requesterName;
    @Column(name = "requester_role") private String requesterRole;

    @Column(name = "field_name") private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    private String status; // pendente | aprovado | rejeitado

    @Column(name = "created_at")      private OffsetDateTime createdAt;
    @Column(name = "resolved_at")     private OffsetDateTime resolvedAt;
    @Column(name = "resolved_by")     private String resolvedBy;
    @Column(name = "resolved_by_name") private String resolvedByName;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null)   status = "pendente";
    }
}

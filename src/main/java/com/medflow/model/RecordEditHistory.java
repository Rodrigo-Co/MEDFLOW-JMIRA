package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "record_edit_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecordEditHistory {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord record;

    @Column(name = "edited_by")      private String editedBy;
    @Column(name = "edited_by_name") private String editedByName;

    @Column(name = "edit_timestamp")
    private OffsetDateTime editTimestamp;

    private String changes;

    @PrePersist
    public void prePersist() {
        if (editTimestamp == null) editTimestamp = OffsetDateTime.now();
    }
}

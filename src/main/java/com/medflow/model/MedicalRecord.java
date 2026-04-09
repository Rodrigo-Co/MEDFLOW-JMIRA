package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalRecord {

    @Id
    private String id;

    @Column(name = "patient_id")  private String patientId;
    @Column(name = "patient_name") private String patientName;
    @Column(name = "doctor_id")   private String doctorId;
    @Column(name = "doctor_name") private String doctorName;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(nullable = false)
    private String type; // consulta | cirurgia | exame

    @Column(name = "raw_notes", columnDefinition = "TEXT")
    private String rawNotes;

    @Column(name = "formatted_notes", columnDefinition = "TEXT")
    private String formattedNotes;

    private String diagnosis;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("editTimestamp DESC")
    private List<RecordEditHistory> editHistory = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (recordDate == null) recordDate = LocalDate.now();
    }
}

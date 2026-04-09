package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    private String id;

    @Column(name = "patient_id")   private String patientId;
    @Column(name = "patient_name") private String patientName;

    @Column(nullable = false)
    private String type; // consulta | exame

    private String specialty;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status; // aberto | em_andamento | concluido

    @Column(name = "doctor_id")   private String doctorId;
    @Column(name = "doctor_name") private String doctorName;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDate.now();
        if (updatedAt == null) updatedAt = LocalDate.now();
        if (status == null)   status = "aberto";
    }
}

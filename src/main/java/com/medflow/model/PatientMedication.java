package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patient_medications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    private String medication;
}

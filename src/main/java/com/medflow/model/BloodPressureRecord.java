package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blood_pressure_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BloodPressureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "date_label")
    private String date;

    private Integer systolic;
    private Integer diastolic;
}

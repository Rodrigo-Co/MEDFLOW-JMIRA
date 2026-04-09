package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patient_conditions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "condition_name")
    private String conditionName;
}

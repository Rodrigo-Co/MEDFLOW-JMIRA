package com.medflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "heart_rate_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HeartRateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "date_label")
    private String date;

    private Integer rate;
}

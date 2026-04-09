package com.medflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

// BloodPressureRecord, HeartRateRecord, PatientMedication, PatientCondition
// are in the same package (com.medflow.model) — no explicit import needed.

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String cpf;

    @Column(nullable = false)
    private String role; // admin | doctor | patient

    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    // Admin
    private String title;

    // Doctor
    private String specialty;
    private String crm;
    private String username;
    private Integer attendances;
    private Integer surgeries;
    private Integer exams;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Patient relations
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BloodPressureRecord> bloodPressureHistory = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HeartRateRecord> heartRateHistory = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientMedication> medications = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientCondition> conditions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (attendances == null) attendances = 0;
        if (surgeries == null) surgeries = 0;
        if (exams == null) exams = 0;
    }
}

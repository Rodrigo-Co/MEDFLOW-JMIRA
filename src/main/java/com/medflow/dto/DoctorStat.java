package com.medflow.dto;

import lombok.Data;

@Data
public class DoctorStat {
    private String id, name, specialty, crm;
    private int attendances, surgeries, exams;
}

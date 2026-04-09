package com.medflow.dto;

import lombok.Data;

@Data
public class UpdateDoctorRequest {
    private String name, email, phone, specialty, crm;
}

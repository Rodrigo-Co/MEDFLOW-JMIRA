package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientLoginRequest {
    @NotBlank
    private String patientId;
}

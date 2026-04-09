package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRecordRequest {
    @NotBlank private String patientId;
    @NotBlank private String type;
    @NotBlank private String rawNotes;
    @NotBlank private String formattedNotes;
    @NotBlank private String diagnosis;
}

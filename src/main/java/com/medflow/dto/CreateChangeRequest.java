package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChangeRequest {
    @NotBlank private String fieldName;
    @NotBlank private String oldValue;
    @NotBlank private String newValue;
}

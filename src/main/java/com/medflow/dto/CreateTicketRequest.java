package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTicketRequest {
    @NotBlank private String type;
    @NotBlank private String specialty;
    @NotBlank private String description;
}

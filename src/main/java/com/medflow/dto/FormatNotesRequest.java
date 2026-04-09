package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FormatNotesRequest {
    @NotBlank private String rawNotes;
    @NotBlank private String type;
}

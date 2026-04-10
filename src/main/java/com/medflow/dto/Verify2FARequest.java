package com.medflow.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class Verify2FARequest {
    @NotBlank private String sessionToken;
    @NotBlank private String code;
}

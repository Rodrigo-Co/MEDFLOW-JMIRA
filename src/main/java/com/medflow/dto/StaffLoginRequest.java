package com.medflow.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class StaffLoginRequest {
    @NotBlank private String username;
    @NotBlank private String role;
    @NotBlank private String password;
}

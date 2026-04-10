package com.medflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePatientRequest {
    @NotBlank                          private String name;
    @NotBlank @Email                   private String email;
                                       private String phone;
    @NotBlank                          private String cpf;
    @NotBlank @Size(min = 6, max = 72) private String password;
}

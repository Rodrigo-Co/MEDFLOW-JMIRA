package com.medflow.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id, name, email, phone, cpf, role;
    private String title, specialty, crm, username;
    private Integer attendances, surgeries, exams;
}

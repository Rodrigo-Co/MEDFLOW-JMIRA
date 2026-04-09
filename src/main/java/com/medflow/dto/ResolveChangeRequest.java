package com.medflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveChangeRequest {
    @NotNull
    private Boolean approved;
}

package com.medflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientHealthUpdateHistory {
    private String id;
    private String patientId;
    private String patientName;
    private String recordId;
    private String doctorId;
    private String doctorName;
    private OffsetDateTime updateTimestamp;
    private String changes;
}

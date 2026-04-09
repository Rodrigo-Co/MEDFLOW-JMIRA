package com.medflow.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardStats {
    private long totalDoctors;
    private long ticketsOpen;
    private long ticketsInProgress;
    private long ticketsConcluded;
    private long pendingRequests;
    private List<DoctorStat> doctors;
}

package com.medflow.repository;

import com.medflow.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<Ticket> findByStatus(String status);
    List<Ticket> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}

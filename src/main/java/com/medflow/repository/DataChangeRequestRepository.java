package com.medflow.repository;

import com.medflow.model.DataChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataChangeRequestRepository extends JpaRepository<DataChangeRequest, String> {
    List<DataChangeRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<DataChangeRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}

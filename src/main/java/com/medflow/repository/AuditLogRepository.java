package com.medflow.repository;

import com.medflow.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findAllByOrderByLogTimestampDesc();
    List<AuditLog> findBySeverityOrderByLogTimestampDesc(String severity);
    List<AuditLog> findByUserRoleOrderByLogTimestampDesc(String role);
    List<AuditLog> findBySeverityAndUserRoleOrderByLogTimestampDesc(String severity, String role);
}

package com.medflow.repository;

import com.medflow.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
    List<MedicalRecord> findByPatientIdOrderByRecordDateDesc(String patientId);
    List<MedicalRecord> findByDoctorIdOrderByRecordDateDesc(String doctorId);

    @Query("SELECT r FROM MedicalRecord r WHERE " +
           "LOWER(r.patientName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(r.diagnosis)   LIKE LOWER(CONCAT('%', :q, '%'))")
    List<MedicalRecord> search(String q);
}

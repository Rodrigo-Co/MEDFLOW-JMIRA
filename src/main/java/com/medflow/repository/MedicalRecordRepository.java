package com.medflow.repository;

import com.medflow.model.MedicalRecord;
import com.medflow.model.RecordEditHistory;
import com.medflow.persistence.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedicalRecordRepository {

    private final Database database;

    public MedicalRecordRepository(Database database) {
        this.database = database;
    }

    public List<MedicalRecord> findAll() {
        return queryRecords("select * from medical_records order by record_date desc, created_at desc");
    }

    public Optional<MedicalRecord> findById(String id) {
        return queryRecords("select * from medical_records where id = ?", id).stream().findFirst();
    }

    public List<MedicalRecord> findByPatientIdOrderByRecordDateDesc(String patientId) {
        return queryRecords("select * from medical_records where patient_id = ? order by record_date desc, created_at desc", patientId);
    }

    public List<MedicalRecord> findByDoctorIdOrderByRecordDateDesc(String doctorId) {
        return queryRecords("select * from medical_records where doctor_id = ? order by record_date desc, created_at desc", doctorId);
    }

    public List<MedicalRecord> search(String q) {
        String sql = """
                select * from medical_records
                 where lower(patient_name) like lower(?) or lower(coalesce(diagnosis, '')) like lower(?)
                 order by record_date desc, created_at desc
                """;
        String term = "%" + q + "%";
        return queryRecords(sql, term, term);
    }

    public MedicalRecord save(MedicalRecord record) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, record.getId())) {
                    updateRecord(connection, record);
                } else {
                    insertRecord(connection, record);
                }
                syncEditHistory(connection, record);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return findById(record.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar prontuario", e);
        }
    }

    private boolean exists(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from medical_records where id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertRecord(Connection connection, MedicalRecord record) throws SQLException {
        String sql = """
                insert into medical_records (
                    id, patient_id, patient_name, doctor_id, doctor_name, record_date,
                    type, raw_notes, formatted_notes, diagnosis, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRecord(statement, record, false);
            statement.executeUpdate();
        }
    }

    private void updateRecord(Connection connection, MedicalRecord record) throws SQLException {
        String sql = """
                update medical_records
                   set patient_id = ?, patient_name = ?, doctor_id = ?, doctor_name = ?, record_date = ?,
                       type = ?, raw_notes = ?, formatted_notes = ?, diagnosis = ?, created_at = ?
                 where id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRecord(statement, record, true);
            statement.executeUpdate();
        }
    }

    private void bindRecord(PreparedStatement statement, MedicalRecord record, boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, record.getId());
        }
        statement.setString(index++, record.getPatientId());
        statement.setString(index++, record.getPatientName());
        statement.setString(index++, record.getDoctorId());
        statement.setString(index++, record.getDoctorName());
        statement.setDate(index++, record.getRecordDate() == null ? null : Date.valueOf(record.getRecordDate()));
        statement.setString(index++, record.getType());
        statement.setString(index++, record.getRawNotes());
        statement.setString(index++, record.getFormattedNotes());
        statement.setString(index++, record.getDiagnosis());
        statement.setTimestamp(index++, record.getCreatedAt() == null ? null : Timestamp.from(record.getCreatedAt().toInstant()));
        if (update) {
            statement.setString(index, record.getId());
        }
    }

    private void syncEditHistory(Connection connection, MedicalRecord record) throws SQLException {
        if (record.getEditHistory() == null) {
            return;
        }
        for (RecordEditHistory history : record.getEditHistory()) {
            if (!historyExists(connection, history.getId())) {
                insertHistory(connection, record.getId(), history);
            }
        }
    }

    private boolean historyExists(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from record_edit_history where id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertHistory(Connection connection, String recordId, RecordEditHistory history) throws SQLException {
        String sql = """
                insert into record_edit_history (id, record_id, edited_by, edited_by_name, edit_timestamp, changes)
                values (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, history.getId());
            statement.setString(2, recordId);
            statement.setString(3, history.getEditedBy());
            statement.setString(4, history.getEditedByName());
            statement.setTimestamp(5, history.getEditTimestamp() == null ? null : Timestamp.from(history.getEditTimestamp().toInstant()));
            statement.setString(6, history.getChanges());
            statement.executeUpdate();
        }
    }

    private List<MedicalRecord> queryRecords(String sql, String... args) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<MedicalRecord> records = new ArrayList<>();
                while (rs.next()) {
                    MedicalRecord record = MedicalRecord.builder()
                            .id(rs.getString("id"))
                            .patientId(rs.getString("patient_id"))
                            .patientName(rs.getString("patient_name"))
                            .doctorId(rs.getString("doctor_id"))
                            .doctorName(rs.getString("doctor_name"))
                            .recordDate(rs.getDate("record_date") == null ? null : rs.getDate("record_date").toLocalDate())
                            .type(rs.getString("type"))
                            .rawNotes(rs.getString("raw_notes"))
                            .formattedNotes(rs.getString("formatted_notes"))
                            .diagnosis(rs.getString("diagnosis"))
                            .createdAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC))
                            .build();
                    record.setEditHistory(loadHistory(connection, record.getId()));
                    records.add(record);
                }
                return records;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar prontuarios", e);
        }
    }

    private List<RecordEditHistory> loadHistory(Connection connection, String recordId) throws SQLException {
        String sql = "select * from record_edit_history where record_id = ? order by edit_timestamp desc";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, recordId);
            try (ResultSet rs = statement.executeQuery()) {
                List<RecordEditHistory> history = new ArrayList<>();
                while (rs.next()) {
                    history.add(RecordEditHistory.builder()
                            .id(rs.getString("id"))
                            .editedBy(rs.getString("edited_by"))
                            .editedByName(rs.getString("edited_by_name"))
                            .editTimestamp(rs.getTimestamp("edit_timestamp") == null ? null : rs.getTimestamp("edit_timestamp").toInstant().atOffset(ZoneOffset.UTC))
                            .changes(rs.getString("changes"))
                            .build());
                }
                return history;
            }
        }
    }
}

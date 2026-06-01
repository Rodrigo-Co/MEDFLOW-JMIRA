package com.medflow.repository;

import com.medflow.model.BloodPressureRecord;
import com.medflow.model.HeartRateRecord;
import com.medflow.model.PatientHealthUpdateHistory;
import com.medflow.model.PatientCondition;
import com.medflow.model.PatientMedication;
import com.medflow.model.User;
import com.medflow.persistence.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public Optional<User> findById(String id) {
        String sql = "select * from users where id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                User user = mapUser(rs);
                if ("patient".equals(user.getRole())) {
                    loadPatientDetails(connection, user);
                }
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar usuario por id", e);
        }
    }

    public Optional<User> findByEmailAndRole(String email, String role) {
        return findSingle("select * from users where email = ? and role = ?", email, role);
    }

    public Optional<User> findByUsernameAndRole(String username, String role) {
        return findSingle("select * from users where username = ? and role = ?", username, role);
    }

    public Optional<User> findByUsername(String username) {
        return findSingle("select * from users where username = ?", username);
    }

    public List<User> findByRole(String role) {
        String sql = "select * from users where role = ? order by name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role);
            try (ResultSet rs = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar usuarios por role", e);
        }
    }

    public boolean existsByCpf(String cpf) {
        return exists("select 1 from users where cpf = ?", cpf);
    }

    public boolean existsByEmail(String email) {
        return exists("select 1 from users where email = ?", email);
    }

    public User save(User user) {
        try (Connection connection = database.getConnection()) {
            if (existsById(connection, user.getId())) {
                update(connection, user);
            } else {
                insert(connection, user);
            }
            return findById(user.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar usuario", e);
        }
    }

    public void addBloodPressure(String patientId, String label, int systolic, int diastolic) {
        String sql = """
                insert into blood_pressure_history (patient_id, date_label, systolic, diastolic)
                values (?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            statement.setString(2, label);
            statement.setInt(3, systolic);
            statement.setInt(4, diastolic);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar pressao arterial", e);
        }
    }

    public void addHeartRate(String patientId, String label, int rate) {
        String sql = """
                insert into heart_rate_history (patient_id, date_label, rate)
                values (?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            statement.setString(2, label);
            statement.setInt(3, rate);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar frequencia cardiaca", e);
        }
    }

    public void replaceMedications(String patientId, List<String> medications) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement("delete from patient_medications where patient_id = ?")) {
                    delete.setString(1, patientId);
                    delete.executeUpdate();
                }
                String sql = "insert into patient_medications (patient_id, medication) values (?, ?)";
                try (PreparedStatement insert = connection.prepareStatement(sql)) {
                    for (String medication : medications) {
                        if (medication == null || medication.isBlank()) {
                            continue;
                        }
                        insert.setString(1, patientId);
                        insert.setString(2, medication.trim());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar medicacoes", e);
        }
    }

    public void insertHealthUpdateHistory(PatientHealthUpdateHistory history) {
        String sql = """
                insert into patient_health_update_history (
                    id, patient_id, patient_name, record_id, doctor_id, doctor_name, update_timestamp, changes
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, history.getId());
            statement.setString(2, history.getPatientId());
            statement.setString(3, history.getPatientName());
            statement.setString(4, history.getRecordId());
            statement.setString(5, history.getDoctorId());
            statement.setString(6, history.getDoctorName());
            statement.setTimestamp(7, history.getUpdateTimestamp() == null ? null : Timestamp.from(history.getUpdateTimestamp().toInstant()));
            statement.setString(8, history.getChanges());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar historico clinico do paciente", e);
        }
    }

    private Optional<User> findSingle(String sql, String... args) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                User user = mapUser(rs);
                if ("patient".equals(user.getRole())) {
                    loadPatientDetails(connection, user);
                }
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar usuario", e);
        }
    }

    private boolean exists(String sql, String value) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao verificar existencia de usuario", e);
        }
    }

    private boolean existsById(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from users where id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insert(Connection connection, User user) throws SQLException {
        String sql = """
                insert into users (
                    id, name, email, phone, cpf, role, password_hash, title,
                    specialty, crm, username, attendances, surgeries, exams, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindBaseUser(statement, user);
            statement.executeUpdate();
        }
    }

    private void update(Connection connection, User user) throws SQLException {
        String sql = """
                update users
                   set name = ?, email = ?, phone = ?, cpf = ?, role = ?, password_hash = ?, title = ?,
                       specialty = ?, crm = ?, username = ?, attendances = ?, surgeries = ?, exams = ?
                 where id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setString(4, user.getCpf());
            statement.setString(5, user.getRole());
            statement.setString(6, user.getPasswordHash());
            statement.setString(7, user.getTitle());
            statement.setString(8, user.getSpecialty());
            statement.setString(9, user.getCrm());
            statement.setString(10, user.getUsername());
            bindInteger(statement, 11, user.getAttendances());
            bindInteger(statement, 12, user.getSurgeries());
            bindInteger(statement, 13, user.getExams());
            statement.setString(14, user.getId());
            statement.executeUpdate();
        }
    }

    private void bindBaseUser(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getId());
        statement.setString(2, user.getName());
        statement.setString(3, user.getEmail());
        statement.setString(4, user.getPhone());
        statement.setString(5, user.getCpf());
        statement.setString(6, user.getRole());
        statement.setString(7, user.getPasswordHash());
        statement.setString(8, user.getTitle());
        statement.setString(9, user.getSpecialty());
        statement.setString(10, user.getCrm());
        statement.setString(11, user.getUsername());
        bindInteger(statement, 12, user.getAttendances());
        bindInteger(statement, 13, user.getSurgeries());
        bindInteger(statement, 14, user.getExams());
        if (user.getCreatedAt() == null) {
            statement.setObject(15, null);
        } else {
            statement.setTimestamp(15, Timestamp.from(user.getCreatedAt().toInstant()));
        }
    }

    private void loadPatientDetails(Connection connection, User user) throws SQLException {
        user.setBloodPressureHistory(loadBloodPressure(connection, user.getId()));
        user.setHeartRateHistory(loadHeartRate(connection, user.getId()));
        user.setMedications(loadMedications(connection, user.getId()));
        user.setConditions(loadConditions(connection, user.getId()));
    }

    private List<BloodPressureRecord> loadBloodPressure(Connection connection, String patientId) throws SQLException {
        String sql = "select id, date_label, systolic, diastolic from blood_pressure_history where patient_id = ? order by id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                List<BloodPressureRecord> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(BloodPressureRecord.builder()
                            .id(rs.getLong("id"))
                            .date(rs.getString("date_label"))
                            .systolic(rs.getInt("systolic"))
                            .diastolic(rs.getInt("diastolic"))
                            .build());
                }
                return items;
            }
        }
    }

    private List<HeartRateRecord> loadHeartRate(Connection connection, String patientId) throws SQLException {
        String sql = "select id, date_label, rate from heart_rate_history where patient_id = ? order by id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                List<HeartRateRecord> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(HeartRateRecord.builder()
                            .id(rs.getLong("id"))
                            .date(rs.getString("date_label"))
                            .rate(rs.getInt("rate"))
                            .build());
                }
                return items;
            }
        }
    }

    private List<PatientMedication> loadMedications(Connection connection, String patientId) throws SQLException {
        String sql = "select id, medication from patient_medications where patient_id = ? order by id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                List<PatientMedication> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(PatientMedication.builder()
                            .id(rs.getLong("id"))
                            .medication(rs.getString("medication"))
                            .build());
                }
                return items;
            }
        }
    }

    private List<PatientCondition> loadConditions(Connection connection, String patientId) throws SQLException {
        String sql = "select id, condition_name from patient_conditions where patient_id = ? order by id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                List<PatientCondition> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(PatientCondition.builder()
                            .id(rs.getLong("id"))
                            .conditionName(rs.getString("condition_name"))
                            .build());
                }
                return items;
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return User.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .cpf(rs.getString("cpf"))
                .role(rs.getString("role"))
                .passwordHash(rs.getString("password_hash"))
                .title(rs.getString("title"))
                .specialty(rs.getString("specialty"))
                .crm(rs.getString("crm"))
                .username(rs.getString("username"))
                .attendances((Integer) rs.getObject("attendances"))
                .surgeries((Integer) rs.getObject("surgeries"))
                .exams((Integer) rs.getObject("exams"))
                .createdAt(createdAt == null ? null : createdAt.toInstant().atOffset(OffsetDateTime.now().getOffset()))
                .build();
    }

    private void bindInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setInt(index, value);
        }
    }
}

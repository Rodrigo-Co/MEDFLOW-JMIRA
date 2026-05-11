package com.medflow.repository;

import com.medflow.model.AuditLog;
import com.medflow.persistence.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogRepository {

    private final Database database;

    public AuditLogRepository(Database database) {
        this.database = database;
    }

    public AuditLog save(AuditLog entry) {
        String sql = """
                insert into audit_log (id, action, severity, details, user_id, user_name, user_role, log_timestamp)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.getId());
            statement.setString(2, entry.getAction());
            statement.setString(3, entry.getSeverity());
            statement.setString(4, entry.getDetails());
            statement.setString(5, entry.getUserId());
            statement.setString(6, entry.getUserName());
            statement.setString(7, entry.getUserRole());
            statement.setTimestamp(8, Timestamp.from(entry.getLogTimestamp().toInstant()));
            statement.executeUpdate();
            return entry;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar auditoria", e);
        }
    }

    public Optional<AuditLog> findById(String id) {
        return findAll("select * from audit_log where id = ?", id).stream().findFirst();
    }

    public List<AuditLog> findAllByOrderByLogTimestampDesc() {
        return findAll("select * from audit_log order by log_timestamp desc");
    }

    public List<AuditLog> findBySeverityOrderByLogTimestampDesc(String severity) {
        return findAll("select * from audit_log where severity = ? order by log_timestamp desc", severity);
    }

    public List<AuditLog> findByUserRoleOrderByLogTimestampDesc(String role) {
        return findAll("select * from audit_log where user_role = ? order by log_timestamp desc", role);
    }

    public List<AuditLog> findBySeverityAndUserRoleOrderByLogTimestampDesc(String severity, String role) {
        return findAll("select * from audit_log where severity = ? and user_role = ? order by log_timestamp desc", severity, role);
    }

    private List<AuditLog> findAll(String sql, String... args) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<AuditLog> items = new ArrayList<>();
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("log_timestamp");
                    items.add(AuditLog.builder()
                            .id(rs.getString("id"))
                            .action(rs.getString("action"))
                            .severity(rs.getString("severity"))
                            .details(rs.getString("details"))
                            .userId(rs.getString("user_id"))
                            .userName(rs.getString("user_name"))
                            .userRole(rs.getString("user_role"))
                            .logTimestamp(timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC))
                            .build());
                }
                return items;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar auditoria", e);
        }
    }
}

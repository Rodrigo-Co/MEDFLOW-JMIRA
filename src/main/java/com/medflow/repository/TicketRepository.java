package com.medflow.repository;

import com.medflow.model.Ticket;
import com.medflow.persistence.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketRepository {

    private final Database database;

    public TicketRepository(Database database) {
        this.database = database;
    }

    public Ticket save(Ticket ticket) {
        try (Connection connection = database.getConnection()) {
            if (exists(connection, ticket.getId())) {
                update(connection, ticket);
            } else {
                insert(connection, ticket);
            }
            return findById(ticket.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar ticket", e);
        }
    }

    public Optional<Ticket> findById(String id) {
        return query("select * from tickets where id = ?", id).stream().findFirst();
    }

    public List<Ticket> findByPatientIdOrderByCreatedAtDesc(String patientId) {
        return query("select * from tickets where patient_id = ? order by created_at desc", patientId);
    }

    public List<Ticket> findByStatus(String status) {
        return query("select * from tickets where status = ? order by created_at desc", status);
    }

    public List<Ticket> findAllByOrderByCreatedAtDesc() {
        return query("select * from tickets order by created_at desc");
    }

    public long countByStatus(String status) {
        String sql = "select count(*) from tickets where status = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao contar tickets", e);
        }
    }

    private boolean exists(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from tickets where id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insert(Connection connection, Ticket ticket) throws SQLException {
        String sql = """
                insert into tickets (
                    id, patient_id, patient_name, type, specialty, description, status,
                    doctor_id, doctor_name, result, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, ticket, false);
            statement.executeUpdate();
        }
    }

    private void update(Connection connection, Ticket ticket) throws SQLException {
        String sql = """
                update tickets
                   set patient_id = ?, patient_name = ?, type = ?, specialty = ?, description = ?, status = ?,
                       doctor_id = ?, doctor_name = ?, result = ?, created_at = ?, updated_at = ?
                 where id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, ticket, true);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Ticket ticket, boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, ticket.getId());
        }
        statement.setString(index++, ticket.getPatientId());
        statement.setString(index++, ticket.getPatientName());
        statement.setString(index++, ticket.getType());
        statement.setString(index++, ticket.getSpecialty());
        statement.setString(index++, ticket.getDescription());
        statement.setString(index++, ticket.getStatus());
        statement.setString(index++, ticket.getDoctorId());
        statement.setString(index++, ticket.getDoctorName());
        statement.setString(index++, ticket.getResult());
        statement.setDate(index++, ticket.getCreatedAt() == null ? null : Date.valueOf(ticket.getCreatedAt()));
        statement.setDate(index++, ticket.getUpdatedAt() == null ? null : Date.valueOf(ticket.getUpdatedAt()));
        if (update) {
            statement.setString(index, ticket.getId());
        }
    }

    private List<Ticket> query(String sql, String... args) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Ticket> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(Ticket.builder()
                            .id(rs.getString("id"))
                            .patientId(rs.getString("patient_id"))
                            .patientName(rs.getString("patient_name"))
                            .type(rs.getString("type"))
                            .specialty(rs.getString("specialty"))
                            .description(rs.getString("description"))
                            .status(rs.getString("status"))
                            .doctorId(rs.getString("doctor_id"))
                            .doctorName(rs.getString("doctor_name"))
                            .result(rs.getString("result"))
                            .createdAt(rs.getDate("created_at") == null ? null : rs.getDate("created_at").toLocalDate())
                            .updatedAt(rs.getDate("updated_at") == null ? null : rs.getDate("updated_at").toLocalDate())
                            .build());
                }
                return items;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar tickets", e);
        }
    }
}

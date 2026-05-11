package com.medflow.repository;

import com.medflow.model.DataChangeRequest;
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

public class DataChangeRequestRepository {

    private final Database database;

    public DataChangeRequestRepository(Database database) {
        this.database = database;
    }

    public DataChangeRequest save(DataChangeRequest request) {
        try (Connection connection = database.getConnection()) {
            if (exists(connection, request.getId())) {
                update(connection, request);
            } else {
                insert(connection, request);
            }
            return findById(request.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar solicitacao de alteracao", e);
        }
    }

    public Optional<DataChangeRequest> findById(String id) {
        return query("select * from data_change_requests where id = ?", id).stream().findFirst();
    }

    public List<DataChangeRequest> findByStatusOrderByCreatedAtDesc(String status) {
        return query("select * from data_change_requests where status = ? order by created_at desc", status);
    }

    public List<DataChangeRequest> findAllByOrderByCreatedAtDesc() {
        return query("select * from data_change_requests order by created_at desc");
    }

    public long countByStatus(String status) {
        String sql = "select count(*) from data_change_requests where status = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao contar solicitacoes de alteracao", e);
        }
    }

    private boolean exists(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from data_change_requests where id = ?")) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insert(Connection connection, DataChangeRequest request) throws SQLException {
        String sql = """
                insert into data_change_requests (
                    id, requester_id, requester_name, requester_role, field_name,
                    old_value, new_value, status, created_at, resolved_at, resolved_by, resolved_by_name
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, request, false);
            statement.executeUpdate();
        }
    }

    private void update(Connection connection, DataChangeRequest request) throws SQLException {
        String sql = """
                update data_change_requests
                   set requester_id = ?, requester_name = ?, requester_role = ?, field_name = ?,
                       old_value = ?, new_value = ?, status = ?, created_at = ?, resolved_at = ?,
                       resolved_by = ?, resolved_by_name = ?
                 where id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, request, true);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, DataChangeRequest request, boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            statement.setString(index++, request.getId());
        }
        statement.setString(index++, request.getRequesterId());
        statement.setString(index++, request.getRequesterName());
        statement.setString(index++, request.getRequesterRole());
        statement.setString(index++, request.getFieldName());
        statement.setString(index++, request.getOldValue());
        statement.setString(index++, request.getNewValue());
        statement.setString(index++, request.getStatus());
        statement.setTimestamp(index++, Timestamp.from(request.getCreatedAt().toInstant()));
        if (request.getResolvedAt() == null) {
            statement.setObject(index++, null);
        } else {
            statement.setTimestamp(index++, Timestamp.from(request.getResolvedAt().toInstant()));
        }
        statement.setString(index++, request.getResolvedBy());
        statement.setString(index++, request.getResolvedByName());
        if (update) {
            statement.setString(index, request.getId());
        }
    }

    private List<DataChangeRequest> query(String sql, String... args) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setString(i + 1, args[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<DataChangeRequest> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(map(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar solicitacao de alteracao", e);
        }
    }

    private DataChangeRequest map(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        return DataChangeRequest.builder()
                .id(rs.getString("id"))
                .requesterId(rs.getString("requester_id"))
                .requesterName(rs.getString("requester_name"))
                .requesterRole(rs.getString("requester_role"))
                .fieldName(rs.getString("field_name"))
                .oldValue(rs.getString("old_value"))
                .newValue(rs.getString("new_value"))
                .status(rs.getString("status"))
                .createdAt(createdAt == null ? null : createdAt.toInstant().atOffset(ZoneOffset.UTC))
                .resolvedAt(resolvedAt == null ? null : resolvedAt.toInstant().atOffset(ZoneOffset.UTC))
                .resolvedBy(rs.getString("resolved_by"))
                .resolvedByName(rs.getString("resolved_by_name"))
                .build();
    }
}

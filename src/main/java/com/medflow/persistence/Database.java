package com.medflow.persistence;

import com.medflow.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public Database(AppConfig config) {
        this.jdbcUrl = config.getRequired("app.datasource.url");
        this.username = config.getRequired("app.datasource.username");
        this.password = config.getRequired("app.datasource.password");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}

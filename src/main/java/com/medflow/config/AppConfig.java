package com.medflow.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class AppConfig {

    private final Properties properties = new Properties();

    public AppConfig() {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("Arquivo application.properties nao encontrado");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel carregar application.properties", e);
        }
    }

    public String getRequired(String key) {
        String value = getRaw(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatoria ausente: " + key);
        }
        return value.trim();
    }

    public String get(String key, String defaultValue) {
        String value = getRaw(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public int getInt(String key, int defaultValue) {
        String value = getRaw(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = getRaw(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }

    public List<String> getCsv(String key) {
        String value = get(key, "");
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String getRaw(String key) {
        String envValue = System.getenv(envName(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        if ("server.port".equals(key)) {
            envValue = System.getenv("PORT");
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
        }
        return properties.getProperty(key);
    }

    private String envName(String key) {
        return key.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }
}

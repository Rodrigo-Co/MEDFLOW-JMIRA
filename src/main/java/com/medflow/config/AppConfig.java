package com.medflow.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class AppConfig {

    private final Properties properties = new Properties();
    private final Properties dotenv = new Properties();

    public AppConfig() {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("Arquivo application.properties nao encontrado");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel carregar application.properties", e);
        }
        loadDotenv();
    }

    public String getRequired(String key) {
        String value = getRaw(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatoria ausente: " + key
                    + " (configure " + envName(key) + " no ambiente ou em um arquivo .env)");
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
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String envValue = System.getenv(envName(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String dotenvValue = dotenv.getProperty(envName(key));
        if (dotenvValue != null && !dotenvValue.isBlank()) {
            return dotenvValue;
        }
        if ("server.port".equals(key)) {
            envValue = System.getenv("PORT");
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            dotenvValue = dotenv.getProperty("PORT");
            if (dotenvValue != null && !dotenvValue.isBlank()) {
                return dotenvValue;
            }
        }
        return properties.getProperty(key);
    }

    private String envName(String key) {
        return key.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }

    private void loadDotenv() {
        Path envPath = Path.of(".env");
        if (!Files.isRegularFile(envPath)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                dotenv.setProperty(key, unquote(value));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel carregar .env", e);
        }
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}

package com.siritel.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Pengelola koneksi JDBC PostgreSQL menggunakan konfigurasi eksternal
 * (environment variable atau file {@code db.properties}).
 */
public class DatabaseConnection {
    private static final String CONFIG_FILE = "db.properties";
    private static final String ENV_URL = "DB_URL";
    private static final String ENV_USER = "DB_USER";
    private static final String ENV_PASSWORD = "DB_PASSWORD";
    private static final String PROP_URL = "db.url";
    private static final String PROP_USER = "db.user";
    private static final String PROP_PASSWORD = "db.password";

    private static final String CONNECT_TIMEOUT_SEC = "10";
    private static final String SOCKET_TIMEOUT_SEC = "15";

    private static volatile DbConfig cachedConfig;

    private DatabaseConnection() {
        // Utility class, mencegah instansiasi langsung
    }

    /**
     * Membuka koneksi baru ke database PostgreSQL berdasarkan konfigurasi aktif
     * dengan batas waktu koneksi dan pembacaan agar UI tidak hang tanpa batas.
     */
    public static Connection getConnection() throws SQLException {
        DbConfig config = getOrLoadConfig();
        Properties connProps = new Properties();
        connProps.setProperty("user", config.user());
        connProps.setProperty("password", config.password());
        connProps.setProperty("connectTimeout", CONNECT_TIMEOUT_SEC);
        connProps.setProperty("socketTimeout", SOCKET_TIMEOUT_SEC);
        return DriverManager.getConnection(config.url(), connProps);
    }

    private static DbConfig getOrLoadConfig() throws SQLException {
        DbConfig config = cachedConfig;
        if (config != null) {
            return config;
        }
        synchronized (DatabaseConnection.class) {
            if (cachedConfig == null) {
                cachedConfig = loadConfig();
            }
            return cachedConfig;
        }
    }

    private static DbConfig loadConfig() throws SQLException {
        Properties props = loadPropertiesFile();

        String url = resolveConfigValue(ENV_URL, props, PROP_URL, false);
        String user = resolveConfigValue(ENV_USER, props, PROP_USER, false);
        String password = resolveConfigValue(ENV_PASSWORD, props, PROP_PASSWORD, true);

        if (url == null || user == null || password == null) {
            throw new SQLException(
                "Konfigurasi database belum lengkap. Siapkan file db.properties (db.url, db.user, db.password) "
                    + "atau set environment variable DB_URL, DB_USER, dan DB_PASSWORD."
            );
        }

        return new DbConfig(url, user, password);
    }

    private static Properties loadPropertiesFile() throws SQLException {
        Properties props = new Properties();
        Path localPath = Path.of(CONFIG_FILE);

        if (Files.isRegularFile(localPath)) {
            try (Reader reader = Files.newBufferedReader(localPath, StandardCharsets.UTF_8)) {
                props.load(reader);
                return props;
            } catch (IOException e) {
                throw new SQLException("Gagal membaca file konfigurasi " + CONFIG_FILE + ": " + e.getMessage(), e);
            }
        }

        try (InputStream resourceStream = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (resourceStream != null) {
                try (Reader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
        } catch (IOException e) {
            throw new SQLException("Gagal memuat resource " + CONFIG_FILE + ": " + e.getMessage(), e);
        }

        return props;
    }

    // Prioritaskan environment variable sebelum fallback ke file db.properties
    private static String resolveConfigValue(String envKey, Properties props, String propKey, boolean allowWhitespace) {
        String envVal = System.getenv(envKey);
        if (hasValue(envVal, allowWhitespace)) {
            return allowWhitespace ? envVal : envVal.trim();
        }
        String propVal = props.getProperty(propKey);
        if (hasValue(propVal, allowWhitespace)) {
            return allowWhitespace ? propVal : propVal.trim();
        }
        return null;
    }

    private static boolean hasValue(String value, boolean allowWhitespace) {
        if (value == null) {
            return false;
        }
        return allowWhitespace ? !value.isEmpty() : !value.isBlank();
    }

    private record DbConfig(String url, String user, String password) {
    }
}

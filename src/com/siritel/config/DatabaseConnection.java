package com.siritel.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // URL database PostgreSQL lokal
    private static final String URL = "jdbc:postgresql://localhost:5432/si_ritel";

    // User dan password default - ganti sesuai lokal, JANGAN commit password asli
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    private DatabaseConnection() {
        // Utility class, mencegah instansiasi langsung
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

package com.siritel.dao;

import com.siritel.config.DatabaseConnection;
import com.siritel.model.Pengguna;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk entitas Pengguna.
 * Satu-satunya tempat penulisan query SQL terkait pengguna.
 */
public class PenggunaDAO {

    public Pengguna login(String username, String passwordPlain) throws SQLException {
        String sql = "SELECT id, username, nama_lengkap, role " +
                     "FROM pengguna " +
                     "WHERE username = ? AND password = crypt(?, password)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordPlain);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Pengguna(
                            rs.getInt("id"),
                            rs.getString("username"),
                            null,
                            rs.getString("nama_lengkap"),
                            rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    public Pengguna findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, nama_lengkap, role " +
                     "FROM pengguna " +
                     "WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Pengguna(
                            rs.getInt("id"),
                            rs.getString("username"),
                            null,
                            rs.getString("nama_lengkap"),
                            rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    public List<Pengguna> getAll() throws SQLException {
        List<Pengguna> list = new ArrayList<>();
        String sql = "SELECT id, username, nama_lengkap, role " +
                     "FROM pengguna " +
                     "ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Pengguna(
                        rs.getInt("id"),
                        rs.getString("username"),
                        null,
                        rs.getString("nama_lengkap"),
                        rs.getString("role")
                ));
            }
        }
        return list;
    }

    public void insert(Pengguna p) throws SQLException {
        String sql = "INSERT INTO pengguna (username, password, nama_lengkap, role) " +
                     "VALUES (?, crypt(?, gen_salt('bf')), ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getUsername());
            ps.setString(2, p.getPassword());
            ps.setString(3, p.getNamaLengkap());
            ps.setString(4, p.getRole());
            ps.executeUpdate();
        }
    }

    public void update(Pengguna p, boolean gantiPassword) throws SQLException {
        if (gantiPassword) {
            String sql = "UPDATE pengguna " +
                         "SET username = ?, password = crypt(?, gen_salt('bf')), nama_lengkap = ?, role = ? " +
                         "WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUsername());
                ps.setString(2, p.getPassword());
                ps.setString(3, p.getNamaLengkap());
                ps.setString(4, p.getRole());
                ps.setInt(5, p.getId());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE pengguna " +
                         "SET username = ?, nama_lengkap = ?, role = ? " +
                         "WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUsername());
                ps.setString(2, p.getNamaLengkap());
                ps.setString(3, p.getRole());
                ps.setInt(4, p.getId());
                ps.executeUpdate();
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM pengguna WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

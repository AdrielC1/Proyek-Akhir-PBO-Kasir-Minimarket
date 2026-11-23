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
                     "WHERE username = ? AND password = crypt(?, password) AND is_active = TRUE";
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

    public Pengguna getById(int id) throws SQLException {
        String sql = "SELECT id, username, nama_lengkap, role " +
                     "FROM pengguna " +
                     "WHERE id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
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

    public int countActiveAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM pengguna WHERE role = 'Admin' AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<Pengguna> getAll() throws SQLException {
        List<Pengguna> list = new ArrayList<>();
        String sql = "SELECT id, username, nama_lengkap, role " +
                     "FROM pengguna " +
                     "WHERE is_active = TRUE " +
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
        int affected;
        if (gantiPassword) {
            String sql = "UPDATE pengguna " +
                         "SET username = ?, password = crypt(?, gen_salt('bf')), nama_lengkap = ?, role = ? " +
                         "WHERE id = ? AND is_active = TRUE";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUsername());
                ps.setString(2, p.getPassword());
                ps.setString(3, p.getNamaLengkap());
                ps.setString(4, p.getRole());
                ps.setInt(5, p.getId());
                affected = ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE pengguna " +
                         "SET username = ?, nama_lengkap = ?, role = ? " +
                         "WHERE id = ? AND is_active = TRUE";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUsername());
                ps.setString(2, p.getNamaLengkap());
                ps.setString(3, p.getRole());
                ps.setInt(4, p.getId());
                affected = ps.executeUpdate();
            }
        }
        if (affected == 0) {
            throw new IllegalArgumentException("Pengguna tidak ditemukan atau sudah dihapus.");
        }
    }

    /**
     * Menghapus pengguna dengan strategi hybrid soft-delete:
     * - Jika sudah memiliki histori transaksi, akun dinonaktifkan (is_active = FALSE)
     *   agar nama kasir pada riwayat transaksi & laporan tetap terjaga.
     * - Jika belum pernah melayani transaksi, baris dihapus permanen.
     *
     * @return {@code true} jika akun diarsipkan (soft delete), {@code false} jika dihapus permanen.
     */
    public boolean delete(int id) throws SQLException {
        String sqlLock = "SELECT id FROM pengguna WHERE id = ? AND is_active = TRUE FOR UPDATE";
        String sqlCekTransaksi = "SELECT 1 FROM transaksi WHERE kasir_id = ? LIMIT 1";
        String sqlSoftDelete = "UPDATE pengguna SET is_active = FALSE WHERE id = ? AND is_active = TRUE";
        String sqlHardDelete = "DELETE FROM pengguna WHERE id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psLock = conn.prepareStatement(sqlLock)) {
                    psLock.setInt(1, id);
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Pengguna tidak ditemukan atau sudah dihapus.");
                        }
                    }
                }

                boolean pernahTransaksi;
                try (PreparedStatement psCek = conn.prepareStatement(sqlCekTransaksi)) {
                    psCek.setInt(1, id);
                    try (ResultSet rs = psCek.executeQuery()) {
                        pernahTransaksi = rs.next();
                    }
                }

                if (pernahTransaksi) {
                    try (PreparedStatement psSoft = conn.prepareStatement(sqlSoftDelete)) {
                        psSoft.setInt(1, id);
                        psSoft.executeUpdate();
                    }
                    conn.commit();
                    return true;
                }

                try (PreparedStatement psHard = conn.prepareStatement(sqlHardDelete)) {
                    psHard.setInt(1, id);
                    psHard.executeUpdate();
                }
                conn.commit();
                return false;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackErr) {
                    e.addSuppressed(rollbackErr);
                }
                throw e;
            }
        }
    }
}

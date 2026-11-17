package com.siritel.controller;

import com.siritel.dao.PenggunaDAO;
import com.siritel.model.Pengguna;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller untuk mengelola autentikasi dan manajemen pengguna.
 * Dilengkapi validasi aturan bisnis dan pemetaan kode SQLState PostgreSQL.
 */
public class AuthController {
    private static Pengguna penggunaAktif;
    private final PenggunaDAO penggunaDAO;

    public AuthController() {
        this.penggunaDAO = new PenggunaDAO();
    }

    public AuthController(PenggunaDAO penggunaDAO) {
        this.penggunaDAO = penggunaDAO;
    }

    public static Pengguna getPenggunaAktif() {
        return penggunaAktif;
    }

    public static void setPenggunaAktif(Pengguna pengguna) {
        penggunaAktif = pengguna;
    }

    public static void logout() {
        penggunaAktif = null;
    }

    public Pengguna login(String username, String passwordPlain) throws SQLException {
        if (username == null || username.isBlank() || passwordPlain == null || passwordPlain.isBlank()) {
            throw new IllegalArgumentException("Username atau password salah.");
        }

        Pengguna pengguna = penggunaDAO.login(username.trim(), passwordPlain);
        if (pengguna == null) {
            throw new IllegalArgumentException("Username atau password salah.");
        }

        penggunaAktif = pengguna;
        return pengguna;
    }

    public List<Pengguna> daftarPengguna() throws SQLException {
        return penggunaDAO.getAll();
    }

    public void tambahPengguna(Pengguna p) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Data pengguna tidak boleh null.");
        }
        penggunaDAO.insert(p);
    }

    public void ubahPengguna(Pengguna p, boolean gantiPassword) throws SQLException {
        if (p == null) {
            throw new IllegalArgumentException("Data pengguna tidak boleh null.");
        }
        penggunaDAO.update(p, gantiPassword);
    }

    public void hapusPengguna(int id) throws SQLException {
        if (penggunaAktif != null && penggunaAktif.getId() == id) {
            throw new IllegalArgumentException("Tidak dapat menghapus akun yang sedang digunakan.");
        }
        penggunaDAO.delete(id);
    }

    /**
     * Memetakan kode SQLState PostgreSQL ke pesan berbahasa Indonesia yang ramah pengguna.
     */
    public static String pesanSqlError(SQLException e) {
        if (e == null) {
            return "Terjadi kesalahan database tidak diketahui.";
        }
        String state = e.getSQLState();
        if (state != null) {
            if ("23505".equals(state)) {
                return "Username sudah digunakan.";
            }
            if ("23503".equals(state)) {
                return "Pengguna masih terkait data transaksi sehingga tidak dapat dihapus.";
            }
            if (state.startsWith("08")) {
                return "Tidak dapat terhubung ke database. Pastikan PostgreSQL berjalan dan konfigurasi koneksi benar.";
            }
            if (state.startsWith("28")) {
                return "Autentikasi database gagal. Periksa user dan password di DatabaseConnection.";
            }
        }
        return "Terjadi kesalahan database: " + e.getMessage();
    }
}

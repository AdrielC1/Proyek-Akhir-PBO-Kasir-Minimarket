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

    public static Pengguna getPenggunaAktif() {
        return penggunaAktif;
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
        if (gantiPassword && (p.getPassword() == null || p.getPassword().isBlank())) {
            throw new IllegalArgumentException("Password baru tidak boleh kosong.");
        }

        Pengguna lama = penggunaDAO.getById(p.getId());
        if (lama == null) {
            throw new IllegalArgumentException("Pengguna tidak ditemukan atau sudah dihapus.");
        }

        boolean turunDariAdmin = "Admin".equalsIgnoreCase(lama.getRole())
                && !"Admin".equalsIgnoreCase(p.getRole());

        if (penggunaAktif != null && penggunaAktif.getId() == p.getId() && turunDariAdmin) {
            throw new IllegalArgumentException("Tidak dapat menurunkan role akun yang sedang digunakan.");
        }
        if (turunDariAdmin && penggunaDAO.countActiveAdmins() <= 1) {
            throw new IllegalArgumentException("Sistem harus memiliki minimal satu akun Admin aktif.");
        }

        penggunaDAO.update(p, gantiPassword);

        if (penggunaAktif != null && penggunaAktif.getId() == p.getId()) {
            penggunaAktif = new Pengguna(p.getId(), p.getUsername(), null, p.getNamaLengkap(), p.getRole());
        }
    }

    /**
     * Menghapus pengguna dari daftar aktif.
     *
     * @return {@code true} jika akun dinonaktifkan (soft delete) karena terkait transaksi,
     *         {@code false} jika dihapus permanen.
     */
    public boolean hapusPengguna(int id) throws SQLException {
        if (penggunaAktif != null && penggunaAktif.getId() == id) {
            throw new IllegalArgumentException("Tidak dapat menghapus akun yang sedang digunakan.");
        }
        Pengguna target = penggunaDAO.getById(id);
        if (target == null) {
            throw new IllegalArgumentException("Pengguna tidak ditemukan atau sudah dihapus.");
        }
        if ("Admin".equalsIgnoreCase(target.getRole()) && penggunaDAO.countActiveAdmins() <= 1) {
            throw new IllegalArgumentException("Tidak dapat menghapus satu-satunya akun Admin aktif.");
        }
        return penggunaDAO.delete(id);
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
                return "Username sudah digunakan oleh akun aktif lain.";
            }
            if ("23503".equals(state)) {
                return "Pengguna masih terkait data transaksi di database.";
            }
            if (state.startsWith("08")) {
                return "Tidak dapat terhubung ke database. Pastikan PostgreSQL berjalan dan konfigurasi koneksi benar.";
            }
            if (state.startsWith("28")) {
                return "Autentikasi database gagal. Periksa konfigurasi di db.properties atau environment variable.";
            }
        }
        return "Terjadi kesalahan database: " + e.getMessage();
    }
}

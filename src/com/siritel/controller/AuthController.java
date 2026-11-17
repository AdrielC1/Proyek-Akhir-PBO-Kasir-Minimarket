package com.siritel.controller;

import com.siritel.dao.PenggunaDAO;
import com.siritel.model.Pengguna;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller untuk mengelola autentikasi dan manajemen pengguna.
 * Tidak mengandung query SQL dan tidak bergantung pada komponen Swing.
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
        // FASE-1: tanpa penanganan eksepsi
        Pengguna pengguna = penggunaDAO.login(username, passwordPlain);
        // FASE-1: tanpa penanganan eksepsi
        penggunaAktif = pengguna;
        return pengguna;
    }

    public List<Pengguna> daftarPengguna() throws SQLException {
        return penggunaDAO.getAll();
    }

    public void tambahPengguna(Pengguna p) throws SQLException {
        // FASE-1: tanpa penanganan eksepsi
        penggunaDAO.insert(p);
    }

    public void ubahPengguna(Pengguna p, boolean gantiPassword) throws SQLException {
        // FASE-1: tanpa penanganan eksepsi
        penggunaDAO.update(p, gantiPassword);
    }

    public void hapusPengguna(int id) throws SQLException {
        // FASE-1: tanpa penanganan eksepsi
        penggunaDAO.delete(id);
    }
}

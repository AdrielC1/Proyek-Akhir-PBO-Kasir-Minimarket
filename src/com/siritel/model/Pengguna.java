package com.siritel.model;

/**
 * Entitas pengguna sistem SI-RITEL (Admin / Kasir).
 * Menerapkan enkapsulasi penuh dengan validasi di setiap setter.
 */
public class Pengguna {
    private int id;
    private String username;
    private String password;
    private String namaLengkap;
    private String role;

    public Pengguna(int id, String username, String password, String namaLengkap, String role) {
        this.id = id;
        setUsername(username);
        setNamaLengkap(namaLengkap);
        setRole(role);
        if (password != null) {
            setPassword(password);
        } else {
            this.password = null;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username tidak boleh kosong.");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("Username maksimal 50 karakter.");
        }
        this.username = username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password tidak boleh kosong.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password minimal 6 karakter.");
        }
        this.password = password;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        if (namaLengkap == null || namaLengkap.isBlank()) {
            throw new IllegalArgumentException("Nama lengkap tidak boleh kosong.");
        }
        if (namaLengkap.length() > 100) {
            throw new IllegalArgumentException("Nama lengkap maksimal 100 karakter.");
        }
        this.namaLengkap = namaLengkap.trim();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role == null || (!role.equals("Admin") && !role.equals("Kasir"))) {
            throw new IllegalArgumentException("Role harus bernilai 'Admin' atau 'Kasir'.");
        }
        this.role = role;
    }
}

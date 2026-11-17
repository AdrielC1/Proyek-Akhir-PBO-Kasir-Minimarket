package com.siritel.model;

/**
 * Entitas pengguna sistem SI-RITEL (Admin / Kasir).
 */
public class Pengguna {
    private int id;
    private String username;
    private String password;
    private String namaLengkap;
    private String role;

    public Pengguna(int id, String username, String password, String namaLengkap, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.namaLengkap = namaLengkap;
        this.role = role;
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
        // FASE-1: tanpa penanganan eksepsi
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        // FASE-1: tanpa penanganan eksepsi
        this.password = password;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        // FASE-1: tanpa penanganan eksepsi
        this.namaLengkap = namaLengkap;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        // FASE-1: tanpa penanganan eksepsi
        this.role = role;
    }
}

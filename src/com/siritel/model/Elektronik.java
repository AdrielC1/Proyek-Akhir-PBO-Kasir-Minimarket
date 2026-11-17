package com.siritel.model;

/**
 * Subclass produk kategori Elektronik dengan atribut garansi (bulan) dan pajak 15% (atau 17% jika garansi > 12 bulan).
 */
public class Elektronik extends Produk {
    private int bulanGaransi;

    public Elektronik(int id, String kode, String nama, double harga, int bulanGaransi) {
        super(id, kode, nama, harga);
        setBulanGaransi(bulanGaransi);
    }

    @Override
    public double hitungPajak() {
        double rate = (bulanGaransi > 12) ? 0.17 : 0.15;
        return harga * rate;
    }

    @Override
    public String getInfoSpesifik() {
        return "Garansi " + bulanGaransi + " bln";
    }

    @Override
    public String getJenisProduk() {
        return "ELEKTRONIK";
    }

    public int getBulanGaransi() {
        return bulanGaransi;
    }

    public void setBulanGaransi(int bulanGaransi) {
        if (bulanGaransi < 0) {
            throw new IllegalArgumentException("Lama garansi (bulan) tidak boleh negatif.");
        }
        this.bulanGaransi = bulanGaransi;
    }
}

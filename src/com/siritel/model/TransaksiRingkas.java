package com.siritel.model;

import java.time.LocalDateTime;

public class TransaksiRingkas {

    private final String idTransaksi;
    private final LocalDateTime tanggal;
    private final String namaKasir;
    private final int jumlahItem;
    private final double grandTotal;

    public TransaksiRingkas(
            String idTransaksi,
            LocalDateTime tanggal,
            String namaKasir,
            int jumlahItem,
            double grandTotal) {

        if (idTransaksi == null || idTransaksi.isBlank()) {
            throw new IllegalArgumentException(
                "ID transaksi tidak boleh kosong."
            );
        }

        if (tanggal == null) {
            throw new IllegalArgumentException(
                "Tanggal transaksi tidak boleh null."
            );
        }

        if (namaKasir == null || namaKasir.isBlank()) {
            throw new IllegalArgumentException(
                "Nama kasir tidak boleh kosong."
            );
        }

        if (jumlahItem < 0) {
            throw new IllegalArgumentException(
                "Jumlah item tidak boleh negatif."
            );
        }

        if (!Double.isFinite(grandTotal) || grandTotal < 0) {
            throw new IllegalArgumentException(
                "Grand total tidak valid."
            );
        }

        this.idTransaksi = idTransaksi;
        this.tanggal = tanggal;
        this.namaKasir = namaKasir;
        this.jumlahItem = jumlahItem;
        this.grandTotal = grandTotal;
    }

    public String getIdTransaksi() {
        return idTransaksi;
    }

    public LocalDateTime getTanggal() {
        return tanggal;
    }

    public String getNamaKasir() {
        return namaKasir;
    }

    public int getJumlahItem() {
        return jumlahItem;
    }

    public double getGrandTotal() {
        return grandTotal;
    }
}
package com.siritel.model;

import java.time.LocalDate;

/**
 * Entitas batch stok masuk dan tanggal kedaluwarsa per produk (mendukung FEFO).
 */
public class BatchProduk {
    private int id;
    private int produkId;
    private LocalDate tglMasuk;
    private LocalDate tglKadaluarsa;
    private int stokBatch;

    public BatchProduk(int id, int produkId, LocalDate tglMasuk, LocalDate tglKadaluarsa, int stokBatch) {
        this.id = id;
        setProdukId(produkId);
        setTglMasuk(tglMasuk != null ? tglMasuk : LocalDate.now());
        setTglKadaluarsa(tglKadaluarsa);
        setStokBatch(stokBatch);
    }

    public BatchProduk(int produkId, LocalDate tglMasuk, LocalDate tglKadaluarsa, int stokBatch) {
        this(0, produkId, tglMasuk, tglKadaluarsa, stokBatch);
    }

    public boolean isKadaluarsa() {
        return tglKadaluarsa != null && tglKadaluarsa.isBefore(LocalDate.now());
    }

    public boolean isHampirKadaluarsa(int hariBatas) {
        if (tglKadaluarsa == null || isKadaluarsa()) {
            return false;
        }
        return !tglKadaluarsa.isAfter(LocalDate.now().plusDays(Math.max(0, hariBatas)));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProdukId() {
        return produkId;
    }

    public void setProdukId(int produkId) {
        if (produkId < 0) {
            throw new IllegalArgumentException("ID produk pada batch tidak boleh negatif.");
        }
        this.produkId = produkId;
    }

    public LocalDate getTglMasuk() {
        return tglMasuk;
    }

    public void setTglMasuk(LocalDate tglMasuk) {
        if (tglMasuk == null) {
            throw new IllegalArgumentException("Tanggal masuk batch tidak boleh kosong.");
        }
        this.tglMasuk = tglMasuk;
    }

    public LocalDate getTglKadaluarsa() {
        return tglKadaluarsa;
    }

    public void setTglKadaluarsa(LocalDate tglKadaluarsa) {
        this.tglKadaluarsa = tglKadaluarsa;
    }

    public int getStokBatch() {
        return stokBatch;
    }

    public void setStokBatch(int stokBatch) {
        if (stokBatch < 0) {
            throw new IllegalArgumentException("Stok batch tidak boleh negatif.");
        }
        this.stokBatch = stokBatch;
    }
}

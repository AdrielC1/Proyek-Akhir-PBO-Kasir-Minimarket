package com.siritel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kelas abstrak induk seluruh katalog produk SI-RITEL.
 * Menerapkan enkapsulasi pada atribut dasar serta abstraksi perhitungan pajak dan info spesifik.
 */
public abstract class Produk {
    protected int id;
    protected String kode;
    protected String nama;
    protected double harga;
    protected List<BatchProduk> daftarBatch;

    public Produk(int id, String kode, String nama, double harga) {
        this.id = id;
        setKode(kode);
        setNama(nama);
        setHarga(harga);
        this.daftarBatch = new ArrayList<>();
    }

    /**
     * Menghitung nominal pajak satuan produk berdasarkan kebijakan masing-masing kategori.
     */
    public abstract double hitungPajak();

    /**
     * Mengembalikan ringkasan atribut khusus kategori produk (berat, volume, atau garansi).
     */
    public abstract String getInfoSpesifik();

    /**
     * Mengembalikan kode jenis produk sesuai skema database (MAKANAN, MINUMAN, ELEKTRONIK).
     */
    public abstract String getJenisProduk();

    public int getStok() {
        int total = 0;
        for (BatchProduk batch : daftarBatch) {
            total += batch.getStokBatch();
        }
        return total;
    }

    public void tambahBatch(BatchProduk batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch produk tidak boleh null.");
        }
        daftarBatch.add(batch);
    }

    public List<BatchProduk> getDaftarBatch() {
        return Collections.unmodifiableList(daftarBatch);
    }

    public void setDaftarBatch(List<BatchProduk> daftarBatch) {
        if (daftarBatch == null) {
            this.daftarBatch = new ArrayList<>();
        } else {
            this.daftarBatch = new ArrayList<>(daftarBatch);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        if (kode == null || kode.isBlank()) {
            throw new IllegalArgumentException("Kode produk tidak boleh kosong.");
        }
        String trimmed = kode.trim();
        if (trimmed.length() > 30) {
            throw new IllegalArgumentException("Kode produk maksimal 30 karakter.");
        }
        this.kode = trimmed;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama produk tidak boleh kosong.");
        }
        String trimmed = nama.trim();
        if (trimmed.length() > 150) {
            throw new IllegalArgumentException("Nama produk maksimal 150 karakter.");
        }
        this.nama = trimmed;
    }

    public double getHarga() {
        return harga;
    }

    public void setHarga(double harga) {
        if (!Double.isFinite(harga) || harga < 0) {
            throw new IllegalArgumentException("Harga produk tidak boleh negatif.");
        }
        this.harga = harga;
    }
}

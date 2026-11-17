package com.siritel.model;

/**
 * Subclass produk kategori Makanan dengan atribut berat bersih (gram) dan tarif pajak pangan 5%.
 */
public class Makanan extends Produk {
    private int beratGram;

    public Makanan(int id, String kode, String nama, double harga, int beratGram) {
        super(id, kode, nama, harga);
        setBeratGram(beratGram);
    }

    @Override
    public double hitungPajak() {
        return harga * 0.05;
    }

    @Override
    public String getInfoSpesifik() {
        return beratGram + " gram";
    }

    @Override
    public String getJenisProduk() {
        return "MAKANAN";
    }

    public int getBeratGram() {
        return beratGram;
    }

    public void setBeratGram(int beratGram) {
        if (beratGram <= 0) {
            throw new IllegalArgumentException("Berat makanan (gram) harus lebih dari 0.");
        }
        this.beratGram = beratGram;
    }
}

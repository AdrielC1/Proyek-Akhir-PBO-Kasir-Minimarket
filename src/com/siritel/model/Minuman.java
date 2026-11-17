package com.siritel.model;

/**
 * Subclass produk kategori Minuman dengan atribut volume (ml), status dingin, dan PPN 11% + Rp 500 jika dingin.
 */
public class Minuman extends Produk {
    private int volumeMl;
    private boolean isDingin;

    public Minuman(int id, String kode, String nama, double harga, int volumeMl, boolean isDingin) {
        super(id, kode, nama, harga);
        setVolumeMl(volumeMl);
        setDingin(isDingin);
    }

    @Override
    public double hitungPajak() {
        return (harga * 0.11) + (isDingin ? 500.0 : 0.0);
    }

    @Override
    public String getInfoSpesifik() {
        return volumeMl + " ml" + (isDingin ? " (Dingin)" : "");
    }

    @Override
    public String getJenisProduk() {
        return "MINUMAN";
    }

    public int getVolumeMl() {
        return volumeMl;
    }

    public void setVolumeMl(int volumeMl) {
        if (volumeMl <= 0) {
            throw new IllegalArgumentException("Volume minuman (ml) harus lebih dari 0.");
        }
        this.volumeMl = volumeMl;
    }

    public boolean isDingin() {
        return isDingin;
    }

    public void setDingin(boolean dingin) {
        this.isDingin = dingin;
    }
}

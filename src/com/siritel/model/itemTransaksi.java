
package com.siritel.model;

public class itemTransaksi {

    private final produk produk;
    private int kuantitas;
    private final double hargaSatuan;
    private final double pajakSatuan;
    private double subtotal;
    
    // Constructor
    public itemTransaksi(produk produk, int kuantitas) {

        if (produk == null) {
            throw new IllegalArgumentException(
                "produk tidak boleh null."
            );
        }

        if (kuantitas <= 0) {
            throw new IllegalArgumentException(
                "Kuantitas harus lebih dari 0."
            );
        }

        this.produk = produk;
        this.kuantitas = kuantitas;

        // Menyimpan snapshot harga dan pajak produk
        this.hargaSatuan = produk.getHarga();
        this.pajakSatuan = produk.hitungPajak();

        if (!Double.isFinite(hargaSatuan) || hargaSatuan < 0) {
            throw new IllegalArgumentException(
                "Harga satuan tidak valid."
            );
        }

        if (!Double.isFinite(pajakSatuan) || pajakSatuan < 0) {
            throw new IllegalArgumentException(
                "Pajak satuan tidak valid."
            );
        }

        hitungSubtotal();
    }

    // Menghitung subtotal barang sebelum pajak
    public double hitungSubtotal() {

        this.subtotal = hargaSatuan * kuantitas;

        return subtotal;
    }

    // Menghitung total pajak berdasarkan kuantitas
    public double hitungPajakItem() {

        return pajakSatuan * kuantitas;
    }

    // Getter
    public produk getproduk() {
        return produk;
    }

    public int getKuantitas() {
        return kuantitas;
    }

    // Setter kuantitas dengan validasi
    public void setKuantitas(int kuantitas) {

        if (kuantitas <= 0) {
            throw new IllegalArgumentException(
                "Kuantitas harus lebih dari 0."
            );
        }

        this.kuantitas = kuantitas;

        // Perbarui subtotal ketika kuantitas berubah
        hitungSubtotal();
    }

    // Getter tambahan untuk kebutuhan DAO
    public double getHargaSatuan() {
        return hargaSatuan;
    }

    public double getPajakSatuan() {
        return pajakSatuan;
    }

    public double getSubtotal() {
        return subtotal;
    }
}

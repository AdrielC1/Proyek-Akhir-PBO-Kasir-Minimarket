
package com.siritel.model;

public class ItemTransaksi {

    private final Produk produk;
    private int kuantitas;
    private final double hargaSatuan;
    private final double pajakSatuan;
    private double subtotal;

    // Constructor
    public ItemTransaksi(Produk produk, int kuantitas) {

        if (produk == null) {
            throw new IllegalArgumentException(
                    "Produk tidak boleh null.");
        }

        if (kuantitas <= 0) {
            throw new IllegalArgumentException(
                    "Kuantitas harus lebih dari 0.");
        }

        this.produk = produk;
        this.kuantitas = kuantitas;

        // Menyimpan snapshot harga dan pajak produk
        this.hargaSatuan = produk.getHarga();
        this.pajakSatuan = produk.hitungPajak();

        if (!Double.isFinite(hargaSatuan) || hargaSatuan < 0) {
            throw new IllegalArgumentException(
                    "Harga satuan tidak valid.");
        }

        if (!Double.isFinite(pajakSatuan) || pajakSatuan < 0) {
            throw new IllegalArgumentException(
                    "Pajak satuan tidak valid.");
        }

        hitungSubtotal();
    }

    public ItemTransaksi(
            Produk produk,
            int kuantitas,
            double hargaSatuan,
            double pajakSatuan,
            double subtotal) {

        if (produk == null) {
            throw new IllegalArgumentException(
                    "Produk tidak boleh null.");
        }

        if (kuantitas <= 0) {
            throw new IllegalArgumentException(
                    "Kuantitas harus lebih dari 0.");
        }

        if (!Double.isFinite(hargaSatuan) || hargaSatuan < 0 ||
                !Double.isFinite(pajakSatuan) || pajakSatuan < 0 ||
                !Double.isFinite(subtotal) || subtotal < 0) {

            throw new IllegalArgumentException(
                    "Data nominal transaksi tidak valid.");
        }

        this.produk = produk;
        this.kuantitas = kuantitas;
        this.hargaSatuan = hargaSatuan;
        this.pajakSatuan = pajakSatuan;
        this.subtotal = subtotal;
    }

    // Menghitung subtotal barang sebelum pajak
    public double hitungSubtotal() {
        subtotal = hargaSatuan * kuantitas;
        return subtotal;
    }

    // Menghitung total pajak berdasarkan kuantitas
    public double hitungPajakItem() {
        return pajakSatuan * kuantitas;
    }

    // Getter
    public Produk getProduk() {
        return produk;
    }

    public int getKuantitas() {
        return kuantitas;
    }

    public double getHargaSatuan() {
        return hargaSatuan;
    }

    public double getPajakSatuan() {
        return pajakSatuan;
    }

    public double getSubtotal() {
        return subtotal;
    }

    // Setter kuantitas dengan validasi
    public void setKuantitas(int kuantitas) {

        if (kuantitas <= 0) {
            throw new IllegalArgumentException(
                    "Kuantitas harus lebih dari 0.");
        }

        this.kuantitas = kuantitas;

        // Perbarui subtotal ketika kuantitas berubah
        hitungSubtotal();
    }
}

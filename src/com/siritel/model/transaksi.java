
package com.siritel.model;

import com.siritel.exception.StokTidakCukupException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Transaksi implements printable {

    private String idTransaksi;
    private LocalDateTime tanggal;
    private final Pengguna kasir;
    private final List<ItemTransaksi> listKeranjang;

    private double subtotalBarang;
    private double totalPajak;
    private double grandTotal;
    private double bayar;
    private double kembalian;

    // Constructor
    public Transaksi(String idTransaksi, Pengguna kasir) {

        if (idTransaksi == null || idTransaksi.isBlank()) {
            throw new IllegalArgumentException(
                "ID transaksi tidak boleh kosong."
            );
        }

        if (kasir == null) {
            throw new IllegalArgumentException(
                "Kasir tidak boleh null."
            );
        }

        this.idTransaksi = idTransaksi;
        this.tanggal = LocalDateTime.now();
        this.kasir = kasir;

        this.listKeranjang = new ArrayList<>();

        this.subtotalBarang = 0;
        this.totalPajak = 0;
        this.grandTotal = 0;
        this.bayar = 0;
        this.kembalian = 0;
    }

    // Menambahkan produk ke keranjang
    public void tambahItem(Produk produk, int qty)
            throws StokTidakCukupException {

        if (produk == null) {
            throw new IllegalArgumentException(
                "Produk tidak boleh null."
            );
        }

        if (qty <= 0) {
            throw new IllegalArgumentException(
                "Kuantitas harus lebih dari 0."
            );
        }

        int stokTersedia = produk.getStok();
        ItemTransaksi itemDitemukan = null;
        int kuantitasDiKeranjang = 0;

        // Memeriksa apakah produk sudah ada di keranjang
        for (ItemTransaksi item : listKeranjang) {

            boolean produkSama =
                item.getProduk() == produk ||
                (produk.getId() > 0 &&
                 item.getProduk().getId() == produk.getId());

            if (produkSama) {
                itemDitemukan = item;
                kuantitasDiKeranjang = item.getKuantitas();
                break;
            }
        }

        // Menghitung total kuantitas setelah penambahan
        long kuantitasBaru =
            (long) kuantitasDiKeranjang + qty;

        // Validasi stok
        if (kuantitasBaru > stokTersedia) {

            int sisaStok = Math.max(
                0, stokTersedia - kuantitasDiKeranjang
            );

            throw new StokTidakCukupException(
                "Stok produk tidak mencukupi. " +
                "Sisa stok yang dapat ditambahkan: " + sisaStok,
                sisaStok
            );
        }

        // Jika produk sudah ada, tambahkan kuantitasnya
        if (itemDitemukan != null) {

            itemDitemukan.setKuantitas(
                (int) kuantitasBaru
            );

        } else {

            // Jika belum ada, buat item transaksi baru
            listKeranjang.add(
                new ItemTransaksi(produk, qty)
            );
        }

        // Memperbarui total dan mereset pembayaran
        perbaruiTotal();
    }

    // Mencari item berdasarkan objek atau ID produk
    private ItemTransaksi cariItem(Produk produk) {

        for (ItemTransaksi item : listKeranjang) {

            boolean sama =
                item.getProduk() == produk ||
                (produk.getId() > 0 &&
                 item.getProduk().getId() == produk.getId());

            if (sama) {
                return item;
            }
        }

        return null;
    }

    // Mengubah kuantitas item
    public void ubahKuantitas(Produk produk, int qty)
            throws StokTidakCukupException {

        if (produk == null) {
            throw new IllegalArgumentException(
                "Produk tidak boleh null."
            );
        }

        if (qty <= 0) {
            throw new IllegalArgumentException(
                "Kuantitas harus lebih dari 0."
            );
        }

        ItemTransaksi item = cariItem(produk);

        if (item == null) {
            throw new IllegalArgumentException(
                "Produk tidak ditemukan di keranjang."
            );
        }

        int stokTersedia = produk.getStok();

        if (qty > stokTersedia) {
            throw new StokTidakCukupException(
                "Stok produk tidak mencukupi.",
                stokTersedia
            );
        }

        item.setKuantitas(qty);

        // Memperbarui total dan mereset pembayaran
        perbaruiTotal();
    }

    // Menghapus item dari keranjang
    public void hapusItem(Produk produk) {

        if (produk == null) {
            throw new IllegalArgumentException(
                "Produk tidak boleh null."
            );
        }

        ItemTransaksi item = cariItem(produk);

        if (item == null) {
            throw new IllegalArgumentException(
                "Produk tidak ditemukan di keranjang."
            );
        }

        listKeranjang.remove(item);

        // Memperbarui total dan mereset pembayaran
        perbaruiTotal();
    }

    // Mengosongkan seluruh keranjang
    public void bersihkanKeranjang() {

        listKeranjang.clear();

        // Mengatur ulang seluruh perhitungan transaksi
        perbaruiTotal();
    }

    // Memperbarui seluruh perhitungan transaksi
    private void perbaruiTotal() {

        hitungSubtotalBarang();
        hitungTotalPajak();
        hitungGrandTotal();

        // Pembayaran sebelumnya tidak berlaku lagi
        resetPembayaran();
    }

    // Mereset nominal pembayaran dan kembalian
    private void resetPembayaran() {

        this.bayar = 0;
        this.kembalian = 0;
    }

    // Menghitung subtotal seluruh barang
    public double hitungSubtotalBarang() {

        subtotalBarang = 0;

        for (ItemTransaksi item : listKeranjang) {
            subtotalBarang += item.hitungSubtotal();
        }

        return subtotalBarang;
    }

    // Menghitung total pajak seluruh barang
    public double hitungTotalPajak() {

        totalPajak = 0;

        for (ItemTransaksi item : listKeranjang) {
            totalPajak += item.hitungPajakItem();
        }

        return totalPajak;
    }

    // Menghitung grand total
    public double hitungGrandTotal() {

        grandTotal =
            hitungSubtotalBarang() + hitungTotalPajak();

        return grandTotal;
    }

    // Memproses pembayaran
    public void prosesBayar(double nominal) {

        if (!Double.isFinite(nominal) || nominal < 0) {
            throw new IllegalArgumentException(
                "Nominal pembayaran tidak valid."
            );
        }

        if (listKeranjang.isEmpty()) {
            throw new IllegalArgumentException(
                "Keranjang transaksi masih kosong."
            );
        }

        double total = hitungGrandTotal();

        if (nominal < total) {
            throw new IllegalArgumentException(
                "Uang pembayaran tidak mencukupi."
            );
        }

        this.bayar = nominal;
        this.kembalian = nominal - total;
    }

    // Getter
    public String getIdTransaksi() {
        return idTransaksi;
    }

    public LocalDateTime getTanggal() {
        return tanggal;
    }

    public Pengguna getKasir() {
        return kasir;
    }

    public List<ItemTransaksi> getListKeranjang() {
        return Collections.unmodifiableList(listKeranjang);
    }

    public double getSubtotalBarang() {
        return subtotalBarang;
    }

    public double getTotalPajak() {
        return totalPajak;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public double getBayar() {
        return bayar;
    }

    public double getKembalian() {
        return kembalian;
    }

    // Implementasi interface printable
    @Override
    public String cetak() {

        NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(
                new Locale("id", "ID")
            );

        DateTimeFormatter formatTanggal =
            DateTimeFormatter.ofPattern(
                "dd-MM-yyyy HH:mm:ss"
            );

        StringBuilder nota = new StringBuilder();

        nota.append("====================================\n");
        nota.append("             SI-RITEL\n");
        nota.append("          NOTA PENJUALAN\n");
        nota.append("====================================\n");

        nota.append("ID Transaksi : ")
            .append(idTransaksi).append("\n");

        nota.append("Tanggal      : ")
            .append(tanggal.format(formatTanggal)).append("\n");

        nota.append("Kasir        : ")
            .append(kasir.getNamaLengkap()).append("\n");

        nota.append("------------------------------------\n");

        for (ItemTransaksi item : listKeranjang) {

            nota.append(item.getProduk().getNama())
                .append("\n");

            nota.append(item.getKuantitas())
                .append(" x ")
                .append(rupiah.format(item.getHargaSatuan()))
                .append(" = ")
                .append(rupiah.format(item.getSubtotal()))
                .append("\n");

            nota.append("Pajak: ")
                .append(rupiah.format(item.hitungPajakItem()))
                .append("\n");
        }

        nota.append("------------------------------------\n");

        nota.append("Subtotal : ")
            .append(rupiah.format(subtotalBarang)).append("\n");

        nota.append("Pajak    : ")
            .append(rupiah.format(totalPajak)).append("\n");

        nota.append("Total    : ")
            .append(rupiah.format(grandTotal)).append("\n");

        nota.append("Bayar    : ")
            .append(rupiah.format(bayar)).append("\n");

        nota.append("Kembali  : ")
            .append(rupiah.format(kembalian)).append("\n");

        nota.append("====================================\n");
        nota.append("        Terima kasih!\n");
        nota.append("====================================\n");

        return nota.toString();
    }
}

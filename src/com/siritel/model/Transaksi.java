
package com.siritel.model;

import com.siritel.exception.StokTidakCukupException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Transaksi implements Printable {

    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");

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

    // Constructor untuk memuat transaksi historis dari database
    public Transaksi(
        String idTransaksi,
        LocalDateTime tanggal,
        Pengguna kasir,
        List<ItemTransaksi> listKeranjang,
        double subtotalBarang,
        double totalPajak,
        double grandTotal,
        double bayar,
        double kembalian) {

        if (idTransaksi == null || idTransaksi.isBlank()) {
            throw new IllegalArgumentException("ID transaksi tidak valid.");
        }

        if (tanggal == null || kasir == null || listKeranjang == null) {
            throw new IllegalArgumentException("Data transaksi tidak lengkap.");
        }

        if (listKeranjang.contains(null)) {
            throw new IllegalArgumentException("Daftar item transaksi tidak boleh berisi null.");
        }

        if (!isValidMoney(subtotalBarang) || !isValidMoney(totalPajak)
                || !isValidMoney(grandTotal) || !isValidMoney(bayar) || !isValidMoney(kembalian)) {
            throw new IllegalArgumentException("Data nominal transaksi tidak valid.");
        }

        this.idTransaksi = idTransaksi;
        this.tanggal = tanggal;
        this.kasir = kasir;
        this.listKeranjang = new ArrayList<>(listKeranjang);

        this.subtotalBarang = roundMoney(subtotalBarang);
        this.totalPajak = roundMoney(totalPajak);
        this.grandTotal = roundMoney(grandTotal);
        this.bayar = roundMoney(bayar);
        this.kembalian = roundMoney(kembalian);
    }

    // Memperbarui ID dan timestamp tepat saat checkout dilakukan
    public void siapkanCheckout(String idTransaksiBaru, LocalDateTime waktuCheckout) {
        if (idTransaksiBaru == null || idTransaksiBaru.isBlank() || idTransaksiBaru.length() > 30) {
            throw new IllegalArgumentException("ID transaksi checkout tidak valid.");
        }
        if (waktuCheckout == null) {
            throw new IllegalArgumentException("Waktu checkout tidak boleh null.");
        }
        this.idTransaksi = idTransaksiBaru.trim();
        this.tanggal = waktuCheckout;
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
        ItemTransaksi itemDitemukan = cariItem(produk);
        int kuantitasDiKeranjang = (itemDitemukan != null) ? itemDitemukan.getKuantitas() : 0;

        // Menghitung total kuantitas setelah penambahan
        long kuantitasBaru = (long) kuantitasDiKeranjang + qty;

        // Validasi stok
        if (kuantitasBaru > stokTersedia) {

            int sisaStok = Math.max(0, stokTersedia - kuantitasDiKeranjang);

            throw new StokTidakCukupException(
                "Stok produk tidak mencukupi. " +
                "Sisa stok yang dapat ditambahkan: " + sisaStok,
                sisaStok
            );
        }

        // Jika produk sudah ada, tambahkan kuantitasnya
        if (itemDitemukan != null) {
            itemDitemukan.setKuantitas((int) kuantitasBaru);
        } else {
            // Jika belum ada, buat item transaksi baru
            listKeranjang.add(new ItemTransaksi(produk, qty));
        }

        // Memperbarui total dan mereset pembayaran
        perbaruiTotal();
    }

    // Menyinkronkan keranjang aktif dengan data katalog terbaru dari database
    public List<String> sinkronkanDenganKatalog(Map<Integer, Produk> katalogTerbaru) {
        List<String> perubahan = new ArrayList<>();
        if (katalogTerbaru == null || listKeranjang.isEmpty()) {
            return perubahan;
        }

        List<ItemTransaksi> hasilBaru = new ArrayList<>();
        boolean perluHitungUlang = false;

        for (ItemTransaksi itemLama : listKeranjang) {
            Produk produkLama = itemLama.getProduk();
            Produk terbaru = katalogTerbaru.get(produkLama.getId());

            if (terbaru == null) {
                perubahan.add("'" + produkLama.getNama() + "' dihapus dari keranjang karena sudah tidak tersedia.");
                perluHitungUlang = true;
                continue;
            }

            int stokAktif = terbaru.getStok();
            if (stokAktif <= 0) {
                perubahan.add("'" + terbaru.getNama() + "' dihapus dari keranjang karena stok aktif habis.");
                perluHitungUlang = true;
                continue;
            }

            int qtyBaru = Math.min(itemLama.getKuantitas(), stokAktif);
            if (qtyBaru < itemLama.getKuantitas()) {
                perubahan.add("Jumlah '" + terbaru.getNama() + "' disesuaikan menjadi " + qtyBaru + " sesuai stok tersedia.");
                perluHitungUlang = true;
            }

            ItemTransaksi itemBaru = new ItemTransaksi(terbaru, qtyBaru);
            if (Double.compare(itemBaru.getHargaSatuan(), itemLama.getHargaSatuan()) != 0
                    || Double.compare(itemBaru.getPajakSatuan(), itemLama.getPajakSatuan()) != 0) {
                perubahan.add("Harga/pajak '" + terbaru.getNama() + "' diperbarui mengikuti katalog terbaru.");
                perluHitungUlang = true;
            }

            hasilBaru.add(itemBaru);
        }

        listKeranjang.clear();
        listKeranjang.addAll(hasilBaru);

        if (perluHitungUlang) {
            perbaruiTotal();
        }
        return perubahan;
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
                "Stok produk tidak mencukupi. Stok aktif tersedia: " + stokTersedia,
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

    // Memperbarui seluruh perhitungan transaksi dalam satu kali iterasi
    private void perbaruiTotal() {
        double sub = hitungSubtotalBarang();
        double pjk = hitungTotalPajak();
        this.grandTotal = roundMoney(sub + pjk);

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
        BigDecimal total = BigDecimal.ZERO;
        for (ItemTransaksi item : listKeranjang) {
            total = total.add(toMoneyDecimal(item.hitungSubtotal()));
        }
        subtotalBarang = total.doubleValue();
        return subtotalBarang;
    }

    // Menghitung total pajak seluruh barang
    public double hitungTotalPajak() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemTransaksi item : listKeranjang) {
            total = total.add(toMoneyDecimal(item.hitungPajakItem()));
        }
        totalPajak = total.doubleValue();
        return totalPajak;
    }

    // Menghitung grand total
    public double hitungGrandTotal() {
        BigDecimal sub = toMoneyDecimal(hitungSubtotalBarang());
        BigDecimal pjk = toMoneyDecimal(hitungTotalPajak());
        grandTotal = sub.add(pjk).doubleValue();
        return grandTotal;
    }

    // Memproses pembayaran dengan perbandingan presisi BigDecimal (2 desimal)
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

        BigDecimal totalBd = toMoneyDecimal(hitungGrandTotal());
        BigDecimal nominalBd = toMoneyDecimal(nominal);

        if (nominalBd.compareTo(totalBd) < 0) {
            throw new IllegalArgumentException(
                "Uang pembayaran tidak mencukupi."
            );
        }

        this.bayar = nominalBd.doubleValue();
        this.kembalian = nominalBd.subtract(totalBd).doubleValue();
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

    // Implementasi interface Printable
    @Override
    public String cetak() {

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
                .append(formatRupiah(item.getHargaSatuan()))
                .append(" = ")
                .append(formatRupiah(item.getSubtotal()))
                .append("\n");

            nota.append("Pajak: ")
                .append(formatRupiah(item.hitungPajakItem()))
                .append("\n");
        }

        nota.append("------------------------------------\n");

        nota.append("Subtotal : ")
            .append(formatRupiah(subtotalBarang)).append("\n");

        nota.append("Pajak    : ")
            .append(formatRupiah(totalPajak)).append("\n");

        nota.append("Total    : ")
            .append(formatRupiah(grandTotal)).append("\n");

        nota.append("Bayar    : ")
            .append(formatRupiah(bayar)).append("\n");

        nota.append("Kembali  : ")
            .append(formatRupiah(kembalian)).append("\n");

        nota.append("====================================\n");
        nota.append("        Terima kasih!\n");
        nota.append("====================================\n");

        return nota.toString();
    }

    private static boolean isValidMoney(double nominal) {
        return Double.isFinite(nominal) && nominal >= 0;
    }

    private static BigDecimal toMoneyDecimal(double nominal) {
        return BigDecimal.valueOf(nominal).setScale(2, RoundingMode.HALF_UP);
    }

    private static double roundMoney(double nominal) {
        return toMoneyDecimal(nominal).doubleValue();
    }

    private static String formatRupiah(double nominal) {
        BigDecimal bd = toMoneyDecimal(nominal);
        boolean adaSen = bd.stripTrailingZeros().scale() > 0;
        NumberFormat nf = NumberFormat.getNumberInstance(ID_LOCALE);
        nf.setMinimumFractionDigits(adaSen ? 2 : 0);
        nf.setMaximumFractionDigits(adaSen ? 2 : 0);
        return "Rp " + nf.format(bd);
    }
}

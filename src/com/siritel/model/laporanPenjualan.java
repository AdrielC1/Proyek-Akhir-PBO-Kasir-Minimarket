package com.siritel.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LaporanPenjualan implements printable {

    private final LocalDate periodeAwal;
    private final LocalDate periodeAkhir;
    private final List<Transaksi> daftarTransaksi;

    // Constructor
    public LaporanPenjualan(
            LocalDate awal,
            LocalDate akhir,
            List<Transaksi> list) {

        if (awal == null || akhir == null) {
            throw new IllegalArgumentException(
                "Periode laporan tidak boleh kosong."
            );
        }

        if (awal.isAfter(akhir)) {
            throw new IllegalArgumentException(
                "Tanggal awal tidak boleh melebihi tanggal akhir."
            );
        }

        if (list == null) {
            throw new IllegalArgumentException(
                "Daftar transaksi tidak boleh null."
            );
        }

        if (list.contains(null)) {
            throw new IllegalArgumentException(
                "Daftar transaksi tidak boleh berisi null."
            );
        }

        this.periodeAwal = awal;
        this.periodeAkhir = akhir;

        // Menyalin daftar transaksi
        this.daftarTransaksi = new ArrayList<>(list);
    }

    // Menghitung total omzet dari seluruh transaksi
    public double hitungTotalOmzet() {

        double totalOmzet = 0;

        for (Transaksi transaksi : daftarTransaksi) {
            totalOmzet += transaksi.getGrandTotal();
        }

        return totalOmzet;
    }

    // Menghitung total pajak dari seluruh transaksi
    public double hitungTotalPajakTerkumpul() {

        double totalPajak = 0;

        for (Transaksi transaksi : daftarTransaksi) {
            totalPajak += transaksi.getTotalPajak();
        }

        return totalPajak;
    }

    // Getter
    public LocalDate getPeriodeAwal() {
        return periodeAwal;
    }

    public LocalDate getPeriodeAkhir() {
        return periodeAkhir;
    }

    public List<Transaksi> getDaftarTransaksi() {
        return Collections.unmodifiableList(daftarTransaksi);
    }

    // Implementasi interface Printable
    @Override
    public String cetak() {

        NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(
                new Locale("id", "ID")
            );

        DateTimeFormatter formatTanggal =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

        StringBuilder laporan = new StringBuilder();

        laporan.append("========================================\n");
        laporan.append("          SI-RITEL\n");
        laporan.append("       LAPORAN PENJUALAN\n");
        laporan.append("========================================\n");

        laporan.append("Periode Awal  : ")
            .append(periodeAwal.format(formatTanggal))
            .append("\n");

        laporan.append("Periode Akhir : ")
            .append(periodeAkhir.format(formatTanggal))
            .append("\n");

        laporan.append("----------------------------------------\n");

        laporan.append("Jumlah Transaksi : ")
            .append(daftarTransaksi.size())
            .append("\n");

        laporan.append("Total Omzet      : ")
            .append(rupiah.format(hitungTotalOmzet()))
            .append("\n");

        laporan.append("Total Pajak      : ")
            .append(rupiah.format(hitungTotalPajakTerkumpul()))
            .append("\n");

        laporan.append("========================================\n");

        return laporan.toString();
    }
}

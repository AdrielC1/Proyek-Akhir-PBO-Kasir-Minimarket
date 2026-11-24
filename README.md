# SI-RITEL (Sistem Informasi Kasir & Inventaris Ritel)

Aplikasi desktop kasir (*Point of Sale*) dan manajemen inventaris ritel berbasis **Java Swing** dan **PostgreSQL** untuk Tugas Proyek Pemrograman Berorientasi Objek (PBO) dengan arsitektur **MVC + DAO**.

---

## Teknologi

- **Bahasa:** Java (JDK 17+)
- **Antarmuka Grafis:** Java Swing (`javax.swing.*`, `java.awt.*`)
- **Database:** PostgreSQL
- **Driver JDBC:** `pgJDBC` (`lib/postgresql-42.7.14.jar`)
- **Arsitektur:** MVC + DAO (`config`, `exception`, `model`, `dao`, `controller`, `view`)

---

## Struktur Proyek

```text
Proyek PBO SI-RITEL/
├── README.md
├── docs/
│   ├── AGENTS.md                          # Aturan arsitektur, OOP, UI, & pembagian modul
│   ├── Database_Schema.md                 # Skema DDL PostgreSQL, migrasi, & panduan query DAO
│   └── Java_Class_Schema_Architecture.md  # Rancangan paket, class diagram, & spesifikasi kelas
├── lib/
│   └── postgresql-42.7.14.jar             # Driver JDBC PostgreSQL
└── src/
    └── com/
        └── siritel/
            ├── Main.java                  # Entry point aplikasi
            ├── config/
            │   └── DatabaseConnection.java
            ├── exception/
            │   └── StokTidakCukupException.java
            ├── model/                     # Entitas OOP, pewarisan Produk, interface Printable
            ├── dao/                       # Akses JDBC PostgreSQL (Pengguna, Produk, Transaksi)
            ├── controller/                # Logika bisnis & pemetaan SQLState
            └── view/                      # Halaman Swing + paket komponen terpusat view/ui/
```

> **Status saat ini:** Seluruh Modul 1 (Autentikasi & Manajemen Pengguna), Modul 2 (Katalog Produk, Polimorfisme Pajak & Batch FEFO), dan Modul 3 (Kasir, Riwayat Transaksi, Laporan Penjualan & Cetak A4/Struk) sudah terimplementasi penuh dengan komponen UI terpusat `com.siritel.view.ui` dan dukungan *soft delete* untuk menjaga keutuhan riwayat transaksi.

---

## Fitur & Rancangan OOP

Mengacu pada [docs/AGENTS.md](docs/AGENTS.md) dan [docs/Java_Class_Schema_Architecture.md](docs/Java_Class_Schema_Architecture.md), sistem ini dirancang mencakup:

- **Enkapsulasi & Validasi:** Atribut model bersifat `private`/`protected` dengan validasi nilai pada setter (melempar `IllegalArgumentException` jika tidak valid).
- **Pewarisan & Abstraksi:** Abstract class `Produk` dengan subclass `Makanan`, `Minuman`, dan `Elektronik`, serta interface `Printable` pada `Transaksi` dan `LaporanPenjualan`.
- **Polimorfisme:**
  - *Overriding* `hitungPajak()` sesuai jenis produk.
  - *Overloading* `cariProduk(...)` pada `ProdukController`.
- **Custom Exception:** `StokTidakCukupException` untuk validasi stok saat transaksi.
- **Inventaris Multi-Batch (FEFO):** Pemotongan stok otomatis berdasarkan tanggal kedaluwarsa terdekat (*First Expired, First Out*) dalam transaksi database atomik.
- **5 Halaman Utama + Kontrol Akses:**
  - `Kasir`
  - `Manajemen Produk` (produk & batch stok)
  - `Manajemen Pengguna` (Admin & Kasir)
  - `Riwayat Transaksi`
  - `Laporan Penjualan`

### Pembagian Modul Tim

- **Modul 1 (Fondasi, Auth & User):** `DatabaseConnection`, `Main`, `Pengguna`, `PenggunaDAO`, `AuthController`, `LoginView`, `MainFrame`, `ManajemenPenggunaView`
- **Modul 2 (Katalog Produk, Polimorfisme & Batch):** `Produk`, `Makanan`, `Minuman`, `Elektronik`, `BatchProduk`, `ProdukDAO`, `ProdukController`, `ManajemenProdukView`
- **Modul 3 (Kasir, Riwayat & Laporan):** `Printable`, `StokTidakCukupException`, `ItemTransaksi`, `Transaksi`, `TransaksiRingkas`, `LaporanPenjualan`, `TransaksiDAO`, `TransaksiController`, `KasirView`, `RiwayatTransaksiView`, `LaporanPenjualanView`

---

## Persiapan Database

1. Siapkan database PostgreSQL dan jalankan skrip DDL serta *seed data* yang ada di [docs/Database_Schema.md](docs/Database_Schema.md) (mengaktifkan ekstensi `pgcrypto` dan membuat tabel `pengguna`, `produk`, `batch_produk`, `transaksi`, dan `item_transaksi`).
2. Buat file `db.properties` di folder root proyek berisi `db.url`, `db.user`, dan `db.password` sesuai server PostgreSQL yang digunakan (atau set environment variable `DB_URL`, `DB_USER`, dan `DB_PASSWORD`). File `db.properties` sudah diabaikan oleh `.gitignore` agar kredensial tidak ter-commit.

---

## Cara Menjalankan Aplikasi

### 1. Lewat Terminal (PowerShell)

Jalankan perintah berikut dari folder root proyek:

```powershell
# Compile seluruh source Java ke folder out
New-Item -ItemType Directory -Force -Path out | Out-Null
$files = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -d out -cp "lib\postgresql-42.7.14.jar" $files

# Jalankan aplikasi
java -cp "out;lib\postgresql-42.7.14.jar" com.siritel.Main
```

### 2. Lewat IDE

- **Eclipse:** Pastikan `lib/postgresql-42.7.14.jar` sudah ditambahkan ke *Java Build Path* → klik kanan `Main.java` → **Run As** → **Java Application**.
- **VS Code / Antigravity IDE:** Pastikan ekstensi Java aktif dan `lib/postgresql-42.7.14.jar` terdaftar di *Referenced Libraries*, buka `src/com/siritel/Main.java`, lalu klik tombol **Run**.

---

## Dokumentasi Terkait

- [Panduan Pengembangan & Aturan Kode (AGENTS.md)](docs/AGENTS.md)
- [Skema Database PostgreSQL](docs/Database_Schema.md)
- [Arsitektur & Class Diagram Java](docs/Java_Class_Schema_Architecture.md)

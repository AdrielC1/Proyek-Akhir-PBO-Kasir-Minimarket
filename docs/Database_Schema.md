# Database Schema — SI-RITEL (PostgreSQL)

Dokumen ini mendefinisikan struktur basis data relasional untuk aplikasi **SI-RITEL** menggunakan pendekatan **Single Table Inheritance** untuk hierarki produk dan **Multi-Batch Inventory (FEFO)** untuk pelacakan stok serta tanggal kedaluwarsa.

---

## 1. Ringkasan Entitas & Relasi (5 Tabel)

1. **`pengguna`**: Menyimpan akun pegawai (`Admin` dan `Kasir`) dengan kata sandi terenkripsi menggunakan algoritma **Bcrypt** (`pgcrypto`).
2. **`produk`**: Menyimpan master data barang (`MAKANAN`, `MINUMAN`, `ELEKTRONIK`) beserta atribut khusus masing-masing subclass.
3. **`batch_produk`**: Menyimpan riwayat stok masuk dan tanggal kedaluwarsa per *batch* pengiriman (`1 produk` memiliki `banyak batch`).
4. **`transaksi`**: Menyimpan header nota penjualan (waktu transaksi, kasir yang melayani, subtotal, total pajak, grand total, bayar, kembalian).
5. **`item_transaksi`**: Menyimpan detail barang yang dibeli pada setiap transaksi (`1 transaksi` memiliki `banyak item_transaksi`).

---

## 2. Skrip DDL & Seed Data (PostgreSQL)

Jalankan skrip berikut pada database PostgreSQL (misalnya database `si_ritel`):

```sql
-- ==========================================================
-- SKEMA DATABASE POSTGRESQL - SI-RITEL
-- ==========================================================

-- 0. Aktifkan ekstensi pgcrypto untuk hashing password (Bcrypt)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Bersihkan tabel jika sudah ada (urut berdasarkan ketergantungan FK)
DROP TABLE IF EXISTS item_transaksi CASCADE;
DROP TABLE IF EXISTS transaksi CASCADE;
DROP TABLE IF EXISTS batch_produk CASCADE;
DROP TABLE IF EXISTS produk CASCADE;
DROP TABLE IF EXISTS pengguna CASCADE;

-- 1. Tabel Pengguna
CREATE TABLE pengguna (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nama_lengkap VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('Admin', 'Kasir')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Tabel Master Produk (Single Table Inheritance)
CREATE TABLE produk (
    id SERIAL PRIMARY KEY,
    kode VARCHAR(30) NOT NULL,
    nama VARCHAR(150) NOT NULL,
    harga NUMERIC(12, 2) NOT NULL CHECK (harga >= 0),
    jenis_produk VARCHAR(20) NOT NULL CHECK (jenis_produk IN ('MAKANAN', 'MINUMAN', 'ELEKTRONIK')),

    -- Atribut khusus subclass Makanan
    berat_gram INT CHECK (berat_gram IS NULL OR berat_gram > 0),

    -- Atribut khusus subclass Minuman
    volume_ml INT CHECK (volume_ml IS NULL OR volume_ml > 0),
    is_dingin BOOLEAN,

    -- Atribut khusus subclass Elektronik
    bulan_garansi INT CHECK (bulan_garansi IS NULL OR bulan_garansi >= 0),

    -- Status aktif untuk soft delete produk yang sudah memiliki histori transaksi
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Memastikan atribut spesifik hanya terisi sesuai jenis_produk
    CONSTRAINT chk_atribut_jenis_produk CHECK (
        (jenis_produk = 'MAKANAN' AND berat_gram IS NOT NULL AND volume_ml IS NULL AND is_dingin IS NULL AND bulan_garansi IS NULL) OR
        (jenis_produk = 'MINUMAN' AND volume_ml IS NOT NULL AND is_dingin IS NOT NULL AND berat_gram IS NULL AND bulan_garansi IS NULL) OR
        (jenis_produk = 'ELEKTRONIK' AND bulan_garansi IS NOT NULL AND berat_gram IS NULL AND volume_ml IS NULL AND is_dingin IS NULL)
    )
);

-- Indeks unik parsial agar kode/username bisa dipakai ulang setelah data dinonaktifkan (soft delete)
CREATE UNIQUE INDEX uq_pengguna_username_aktif ON pengguna(username) WHERE is_active = TRUE;
CREATE UNIQUE INDEX uq_produk_kode_aktif ON produk(kode) WHERE is_active = TRUE;

-- 3. Tabel Batch Produk (Stok Masuk & Kedaluwarsa per Batch)
CREATE TABLE batch_produk (
    id SERIAL PRIMARY KEY,
    produk_id INT NOT NULL REFERENCES produk(id) ON DELETE CASCADE,
    tgl_masuk DATE NOT NULL DEFAULT CURRENT_DATE,
    tgl_kadaluarsa DATE, -- Wajib diisi untuk MAKANAN & MINUMAN, NULL untuk ELEKTRONIK
    stok_batch INT NOT NULL CHECK (stok_batch >= 0)
);

-- 4. Tabel Transaksi (Header Nota)
CREATE TABLE transaksi (
    id_transaksi VARCHAR(30) PRIMARY KEY,
    tanggal TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kasir_id INT NOT NULL REFERENCES pengguna(id) ON DELETE RESTRICT,
    subtotal_barang NUMERIC(12, 2) NOT NULL CHECK (subtotal_barang >= 0),
    total_pajak NUMERIC(12, 2) NOT NULL CHECK (total_pajak >= 0),
    grand_total NUMERIC(12, 2) NOT NULL CHECK (grand_total >= 0),
    bayar NUMERIC(12, 2) NOT NULL CHECK (bayar >= 0),
    kembalian NUMERIC(12, 2) NOT NULL CHECK (kembalian >= 0)
);

-- 5. Tabel Item Transaksi (Detail Keranjang Belanja)
CREATE TABLE item_transaksi (
    id SERIAL PRIMARY KEY,
    transaksi_id VARCHAR(30) NOT NULL REFERENCES transaksi(id_transaksi) ON DELETE CASCADE,
    produk_id INT NOT NULL REFERENCES produk(id) ON DELETE RESTRICT,
    kuantitas INT NOT NULL CHECK (kuantitas > 0),
    harga_satuan NUMERIC(12, 2) NOT NULL CHECK (harga_satuan >= 0),
    pajak_satuan NUMERIC(12, 2) NOT NULL CHECK (pajak_satuan >= 0),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0)
);

-- Indeks untuk optimasi pencarian produk, FEFO, dan laporan periode
CREATE INDEX idx_produk_nama ON produk(nama);
CREATE INDEX idx_produk_jenis ON produk(jenis_produk);
CREATE INDEX idx_batch_fefo ON batch_produk(produk_id, tgl_kadaluarsa ASC);
CREATE INDEX idx_transaksi_tanggal ON transaksi(tanggal);
CREATE INDEX idx_item_transaksi_id ON item_transaksi(transaksi_id);

-- ==========================================================
-- SEED DATA AWAL
-- ==========================================================

INSERT INTO pengguna (username, password, nama_lengkap, role) VALUES
('admin', crypt('admin123', gen_salt('bf')), 'Budi Santoso', 'Admin'),
('kasir1', crypt('kasir123', gen_salt('bf')), 'Siti Aminah', 'Kasir');

INSERT INTO produk (kode, nama, harga, jenis_produk, berat_gram, volume_ml, is_dingin, bulan_garansi) VALUES
('MKN-001', 'Roti Tawar Kupas', 15000, 'MAKANAN', 400, NULL, NULL, NULL),
('MKN-002', 'Biskuit Cokelat Kaleng', 32000, 'MAKANAN', 350, NULL, NULL, NULL),
('MNM-001', 'Air Mineral Botol', 4000, 'MINUMAN', NULL, 600, TRUE, NULL),
('MNM-002', 'Teh Melati Kotak', 6500, 'MINUMAN', NULL, 250, FALSE, NULL),
('ELK-001', 'Baterai Alkaline AA (Isi 2)', 18000, 'ELEKTRONIK', NULL, NULL, NULL, 6),
('ELK-002', 'Kabel Data USB-C Fast Charge', 45000, 'ELEKTRONIK', NULL, NULL, NULL, 12);

-- Contoh Multi-Batch untuk pengujian FEFO (Batch Januari & Batch Februari)
INSERT INTO batch_produk (produk_id, tgl_masuk, tgl_kadaluarsa, stok_batch) VALUES
(1, '2026-11-01', '2027-01-15', 10), -- Batch 1: Kedaluwarsa Januari
(1, '2026-11-15', '2027-02-20', 15), -- Batch 2: Kedaluwarsa Februari
(2, '2026-11-10', '2027-06-30', 20),
(3, '2026-11-12', '2027-08-10', 30),
(4, '2026-11-14', '2027-04-05', 25),
(5, '2026-11-05', NULL, 18),
(6, '2026-11-08', NULL, 12);
```

---

## 3. Panduan Query Penting untuk Lapisan DAO (JDBC)

### A. Autentikasi & Manajemen Password (`PenggunaDAO`)
* **Login:**
  ```sql
  SELECT id, username, nama_lengkap, role
  FROM pengguna
  WHERE username = ? AND password = crypt(?, password) AND is_active = TRUE;
  ```
* **Insert Pengguna Baru:**
  ```sql
  INSERT INTO pengguna (username, password, nama_lengkap, role)
  VALUES (?, crypt(?, gen_salt('bf')), ?, ?);
  ```
* **Update Pengguna (dengan ganti password):**
  ```sql
  UPDATE pengguna
  SET username = ?, password = crypt(?, gen_salt('bf')), nama_lengkap = ?, role = ?
  WHERE id = ? AND is_active = TRUE;
  ```

### B. Pemotongan Stok FEFO saat Checkout (`TransaksiDAO`)
* Ambil batch aktif yang **belum kedaluwarsa** berurutan dari tanggal kedaluwarsa terdekat (`NULLS LAST` untuk elektronik) dengan kunci baris `FOR UPDATE` di dalam blok `conn.setAutoCommit(false)`:
  ```sql
  SELECT id, stok_batch
  FROM batch_produk
  WHERE produk_id = ? AND stok_batch > 0
    AND (tgl_kadaluarsa IS NULL OR tgl_kadaluarsa >= CURRENT_DATE)
  ORDER BY tgl_kadaluarsa ASC NULLS LAST, id ASC
  FOR UPDATE;
  ```

### C. Filter Laporan Penjualan (`TransaksiDAO`)
* Mengambil transaksi dalam rentang tanggal inklusif:
  ```sql
  SELECT t.*, p.nama_lengkap AS nama_kasir
  FROM transaksi t
  JOIN pengguna p ON t.kasir_id = p.id
  WHERE t.tanggal >= ? AND t.tanggal < ?
  ORDER BY t.tanggal DESC;
  ```

---

## 4. Skrip Migrasi Database Eksisting (Soft Delete)

Jika database sudah dibuat sebelum penambahan kolom `is_active`, jalankan migrasi berikut:

```sql
ALTER TABLE produk ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE produk DROP CONSTRAINT IF EXISTS produk_kode_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_produk_kode_aktif ON produk(kode) WHERE is_active = TRUE;

ALTER TABLE pengguna ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE pengguna DROP CONSTRAINT IF EXISTS pengguna_username_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_pengguna_username_aktif ON pengguna(username) WHERE is_active = TRUE;
```
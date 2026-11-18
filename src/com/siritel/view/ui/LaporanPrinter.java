package com.siritel.view.ui;

import com.siritel.model.LaporanPenjualan;
import com.siritel.model.Transaksi;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Renderer dokumen cetak & pratinjau PDF untuk Laporan Penjualan SI-RITEL (format A4 rapi).
 */
public final class LaporanPrinter {
    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter TGL_LONG =
            DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE);
    private static final DateTimeFormatter TGL_WAKTU =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", ID_LOCALE);
    private static final DateTimeFormatter TGL_ROW =
            DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE);

    private LaporanPrinter() {
    }

    public static void showPrintPreviewAndPrint(Component host, LaporanPenjualan laporan, ToastManager toast) {
        if (laporan == null) {
            ModernModal.error(host, "Laporan Kosong", "Belum ada data laporan untuk dicetak.");
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(host);
        if (owner == null) {
            return;
        }

        LaporanPrintable printable = new LaporanPrintable(laporan);
        PageFormat a4Preview = createDefaultA4PageFormat();
        int totalPages = printable.getTotalPages(a4Preview);
        final int[] currentPage = {0};

        JDialog dialog = ModernModal.createDialog(owner, "Pratinjau Cetak Laporan Penjualan");
        JPanel shell = new RoundedCardPanel(new BorderLayout(0, 12), UITheme.CARD_RADIUS);
        shell.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Header modal pratinjau
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        badgeWrap.setOpaque(false);
        badgeWrap.add(ModernModal.createStatusIconBadge("INFO", UITheme.PRIMARY_SOFT_BG, UITheme.PRIMARY_BLUE, 36));
        header.add(badgeWrap, BorderLayout.WEST);

        JPanel titleCol = new JPanel(new GridBagLayout());
        titleCol.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        JLabel lblTitle = new JLabel("Pratinjau Laporan Penjualan");
        lblTitle.setFont(UITheme.font(Font.BOLD, 16f));
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        titleRow.add(lblTitle, BorderLayout.CENTER);

        RoundedButton btnClose = new RoundedButton("\u00D7", ButtonVariant.GHOST, UITheme.SMALL_RADIUS);
        btnClose.setFont(UITheme.font(Font.BOLD, 16f));
        btnClose.setPreferredSize(new Dimension(26, 26));
        btnClose.addActionListener(e -> dialog.dispose());
        titleRow.add(btnClose, BorderLayout.EAST);

        gbc.gridy = 0;
        titleCol.add(titleRow, gbc);

        JLabel lblSub = new JLabel("Periode " + laporan.getPeriodeAwal().format(TGL_LONG)
                + " s/d " + laporan.getPeriodeAkhir().format(TGL_LONG)
                + " \u2022 Format Dokumen A4 (PDF / Printer)");
        lblSub.setFont(UITheme.font(Font.PLAIN, 12f));
        lblSub.setForeground(UITheme.TEXT_MUTED);
        gbc.gridy = 1;
        titleCol.add(lblSub, gbc);

        header.add(titleCol, BorderLayout.CENTER);
        shell.add(header, BorderLayout.NORTH);

        // Kanvas pratinjau halaman A4
        JPanel previewHolder = new JPanel(new GridBagLayout());
        previewHolder.setBackground(UITheme.BG_PAGE);
        previewHolder.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 12, new Insets(14, 14, 14, 14)));

        JPanel paperCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int pw = getWidth();
                int ph = getHeight();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, pw, ph);

                double sx = pw / a4Preview.getWidth();
                double sy = ph / a4Preview.getHeight();
                double scale = Math.min(sx, sy);
                g2.scale(scale, scale);

                printable.print(g2, a4Preview, currentPage[0]);

                g2.dispose();
                g.setColor(UITheme.BORDER_INPUT);
                g.drawRect(0, 0, pw - 1, ph - 1);
            }
        };
        paperCanvas.setPreferredSize(new Dimension(380, 538));
        previewHolder.add(paperCanvas);
        shell.add(previewHolder, BorderLayout.CENTER);

        // Baris footer navigasi halaman + tombol cetak
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);

        JPanel pageNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pageNav.setOpaque(false);
        RoundedButton btnPrev = new RoundedButton("\u2039 Sebelumnya", ButtonVariant.OUTLINE, UITheme.SMALL_RADIUS);
        btnPrev.setPreferredSize(new Dimension(102, 34));
        RoundedButton btnNext = new RoundedButton("Berikutnya \u203A", ButtonVariant.OUTLINE, UITheme.SMALL_RADIUS);
        btnNext.setPreferredSize(new Dimension(102, 34));
        JLabel lblPageInfo = new JLabel();
        lblPageInfo.setFont(UITheme.font(Font.PLAIN, 12f));
        lblPageInfo.setForeground(UITheme.TEXT_MUTED);

        Runnable syncPageState = () -> {
            lblPageInfo.setText("Halaman " + (currentPage[0] + 1) + " dari " + totalPages);
            btnPrev.setEnabled(currentPage[0] > 0);
            btnNext.setEnabled(currentPage[0] < totalPages - 1);
            paperCanvas.repaint();
        };

        btnPrev.addActionListener(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                syncPageState.run();
            }
        });
        btnNext.addActionListener(e -> {
            if (currentPage[0] < totalPages - 1) {
                currentPage[0]++;
                syncPageState.run();
            }
        });
        syncPageState.run();

        if (totalPages > 1) {
            pageNav.add(btnPrev);
            pageNav.add(lblPageInfo);
            pageNav.add(btnNext);
        } else {
            pageNav.add(lblPageInfo);
        }

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        RoundedButton btnTutup = new RoundedButton("Tutup", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);
        btnTutup.setPreferredSize(new Dimension(84, 36));
        btnTutup.addActionListener(e -> dialog.dispose());

        RoundedButton btnPrint = new RoundedButton("Cetak / Simpan PDF", ButtonVariant.TEAL, UITheme.BUTTON_RADIUS);
        btnPrint.setPreferredSize(new Dimension(150, 36));
        btnPrint.addActionListener(e -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Laporan-Penjualan-" + laporan.getPeriodeAwal() + "-sd-" + laporan.getPeriodeAkhir());
            job.setPrintable(printable, a4Preview);
            if (job.printDialog()) {
                try {
                    job.print();
                    dialog.dispose();
                    if (toast != null) {
                        toast.show(
                                "Laporan Dikirim",
                                "Dokumen laporan penjualan berhasil dikirim ke printer / PDF.",
                                ToastType.SUCCESS
                        );
                    }
                } catch (PrinterException ex) {
                    ModernModal.error(dialog, "Kesalahan Cetak", "Gagal mencetak laporan:\n" + ex.getMessage());
                }
            }
        });

        rightActions.add(btnTutup);
        rightActions.add(btnPrint);
        footer.add(pageNav, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        shell.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(shell);
        dialog.getRootPane().setDefaultButton(btnPrint);
        ModernModal.showWithBackdrop(owner, dialog, 580, 640);
    }

    private static PageFormat createDefaultA4PageFormat() {
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        double width = 595.0;
        double height = 842.0;
        paper.setSize(width, height);
        paper.setImageableArea(36.0, 36.0, width - 72.0, height - 72.0);
        pf.setPaper(paper);
        return pf;
    }

    private record BarisLaporan(String tanggal, int jumlahTransaksi, double omzet, double pajak) {
    }

    private static final class LaporanPrintable implements Printable {
        private final LaporanPenjualan laporan;
        private final List<BarisLaporan> rows = new ArrayList<>();
        private final LocalDateTime waktuCetak = LocalDateTime.now();

        LaporanPrintable(LaporanPenjualan laporan) {
            this.laporan = laporan;
            for (Map.Entry<LocalDate, List<Transaksi>> entry : laporan.getDaftarHarian().entrySet()) {
                List<Transaksi> list = entry.getValue();
                rows.add(new BarisLaporan(
                        entry.getKey().format(TGL_ROW),
                        list.size(),
                        laporan.hitungOmzetHarian(list),
                        laporan.hitungPajakHarian(list)
                ));
            }
        }

        int getTotalPages(PageFormat pf) {
            double h = pf.getImageableHeight();
            double firstPageTableH = h - 180;
            double contPageTableH = h - 68;
            int rowH = 20;
            int headerAndTotalH = 48;

            int firstCap = Math.max(1, (int) ((firstPageTableH - headerAndTotalH) / rowH));
            if (rows.size() <= firstCap) {
                return 1;
            }
            int remaining = rows.size() - firstCap;
            int contCap = Math.max(1, (int) ((contPageTableH - headerAndTotalH) / rowH));
            return 1 + (int) Math.ceil((double) remaining / contCap);
        }

        @Override
        public int print(Graphics graphics, PageFormat pf, int pageIndex) {
            int totalPages = getTotalPages(pf);
            if (pageIndex >= totalPages) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double x = pf.getImageableX();
            double y = pf.getImageableY();
            double w = pf.getImageableWidth();
            double h = pf.getImageableHeight();
            int ix = (int) Math.round(x);
            int iw = (int) Math.round(w);

            int curY = (int) Math.round(y);
            int rowH = 20;

            int firstCap = Math.max(1, (int) ((h - 180 - 48) / rowH));
            int contCap = Math.max(1, (int) ((h - 68 - 48) / rowH));

            int startRow;
            int endRow;
            if (pageIndex == 0) {
                startRow = 0;
                endRow = Math.min(rows.size(), firstCap);
                curY = drawFirstPageHeader(g2, ix, curY, iw);
            } else {
                startRow = firstCap + (pageIndex - 1) * contCap;
                endRow = Math.min(rows.size(), startRow + contCap);
                curY = drawContinuationHeader(g2, ix, curY, iw);
            }

            curY = drawTableSection(g2, ix, curY, iw, startRow, endRow, pageIndex == totalPages - 1, rowH);
            drawFooter(g2, ix, (int) Math.round(y + h) - 8, iw, pageIndex + 1, totalPages);

            g2.dispose();
            return Printable.PAGE_EXISTS;
        }

        private int drawFirstPageHeader(Graphics2D g2, int x, int y, int w) {
            // Judul & identitas laporan
            g2.setColor(UITheme.PRIMARY_BLUE);
            g2.fillRoundRect(x, y, 4, 34, 4, 4);

            g2.setColor(UITheme.TEXT_MAIN);
            g2.setFont(UITheme.font(Font.BOLD, 16f));
            g2.drawString("SI-RITEL \u2014 LAPORAN PENJUALAN", x + 12, y + 14);

            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.font(Font.PLAIN, 9.5f));
            String periodeStr = "Periode: " + laporan.getPeriodeAwal().format(TGL_LONG)
                    + " s/d " + laporan.getPeriodeAkhir().format(TGL_LONG);
            g2.drawString(periodeStr, x + 12, y + 30);

            String cetakStr = "Dicetak: " + waktuCetak.format(TGL_WAKTU);
            FontMetrics fmSub = g2.getFontMetrics();
            g2.drawString(cetakStr, x + w - fmSub.stringWidth(cetakStr), y + 30);

            y += 46;
            g2.setColor(UITheme.BORDER_DEFAULT);
            g2.drawLine(x, y, x + w, y);
            y += 14;

            // 3 kartu ringkasan KPI
            int gap = 10;
            int cardW = (w - 2 * gap) / 3;
            int cardH = 58;

            drawKpiBox(g2, x, y, cardW, cardH,
                    "Jumlah Transaksi",
                    String.valueOf(laporan.hitungJumlahTransaksi()),
                    UITheme.PRIMARY_BLUE);
            drawKpiBox(g2, x + cardW + gap, y, cardW, cardH,
                    "Total Omzet",
                    formatRupiah(laporan.hitungTotalOmzet()),
                    UITheme.SUCCESS);
            drawKpiBox(g2, x + 2 * (cardW + gap), y, cardW, cardH,
                    "Total Pajak",
                    formatRupiah(laporan.hitungTotalPajakTerkumpul()),
                    UITheme.BADGE_AMBER_FG);

            y += cardH + 18;
            g2.setColor(UITheme.TEXT_MAIN);
            g2.setFont(UITheme.font(Font.BOLD, 11.5f));
            g2.drawString("Rincian Penjualan Harian", x, y);
            return y + 10;
        }

        private int drawContinuationHeader(Graphics2D g2, int x, int y, int w) {
            g2.setColor(UITheme.TEXT_MAIN);
            g2.setFont(UITheme.font(Font.BOLD, 12f));
            g2.drawString("SI-RITEL \u2014 Rincian Penjualan Harian (Lanjutan)", x, y + 12);

            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.font(Font.PLAIN, 9f));
            String periodeStr = laporan.getPeriodeAwal().format(TGL_LONG)
                    + " s/d " + laporan.getPeriodeAkhir().format(TGL_LONG);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(periodeStr, x + w - fm.stringWidth(periodeStr), y + 12);

            y += 22;
            g2.setColor(UITheme.BORDER_DEFAULT);
            g2.drawLine(x, y, x + w, y);
            return y + 12;
        }

        private void drawKpiBox(Graphics2D g2, int x, int y, int w, int h,
                                String label, String value, Color accent) {
            g2.setColor(UITheme.BG_PAGE);
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(UITheme.BORDER_DEFAULT);
            g2.drawRoundRect(x, y, w, h, 8, 8);

            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.font(Font.BOLD, 8.5f));
            g2.drawString(label, x + 10, y + 18);

            g2.setColor(accent);
            g2.setFont(UITheme.font(Font.BOLD, 13.5f));
            g2.drawString(value, x + 10, y + 40);
        }

        private int drawTableSection(Graphics2D g2, int x, int y, int w,
                                     int startRow, int endRow, boolean isLastPage, int rowH) {
            int col1End = x + (int) (w * 0.27);
            int col2End = x + (int) (w * 0.47);
            int col3End = x + (int) (w * 0.74);
            int col4End = x + w;

            // Header tabel
            int headerH = 22;
            g2.setColor(UITheme.BG_HOVER);
            g2.fillRect(x, y, w, headerH);
            g2.setColor(UITheme.BORDER_INPUT);
            g2.drawRect(x, y, w, headerH);

            g2.setFont(UITheme.font(Font.BOLD, 9f));
            g2.setColor(UITheme.TEXT_LABEL);
            int hy = y + 14;
            g2.drawString("Tanggal", x + 10, hy);
            drawRightAligned(g2, "Transaksi", col2End - 12, hy);
            drawRightAligned(g2, "Total Omzet", col3End - 12, hy);
            drawRightAligned(g2, "Total Pajak", col4End - 10, hy);
            y += headerH;

            // Baris data
            for (int i = startRow; i < endRow; i++) {
                BarisLaporan r = rows.get(i);
                if (i % 2 == 1) {
                    g2.setColor(new Color(249, 250, 251));
                    g2.fillRect(x, y, w, rowH);
                }
                g2.setColor(UITheme.BORDER_DEFAULT);
                g2.drawLine(x, y + rowH, x + w, y + rowH);

                boolean activeDay = r.jumlahTransaksi() > 0;
                g2.setFont(UITheme.font(activeDay ? Font.BOLD : Font.PLAIN, 9f));
                g2.setColor(activeDay ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED);

                int ry = y + 13;
                g2.drawString(r.tanggal(), x + 10, ry);
                drawRightAligned(g2, String.valueOf(r.jumlahTransaksi()), col2End - 12, ry);
                drawRightAligned(g2, formatRupiah(r.omzet()), col3End - 12, ry);
                drawRightAligned(g2, formatRupiah(r.pajak()), col4End - 10, ry);

                y += rowH;
            }

            // Garis tepi vertikal luar tabel
            int tableTop = y - (endRow - startRow) * rowH;
            g2.setColor(UITheme.BORDER_DEFAULT);
            g2.drawLine(x, tableTop, x, y);
            g2.drawLine(x + w, tableTop, x + w, y);

            // Baris TOTAL di akhir
            if (isLastPage) {
                int totalH = 24;
                g2.setColor(UITheme.PRIMARY_SOFT_BG);
                g2.fillRect(x, y, w, totalH);
                g2.setColor(UITheme.PRIMARY_SOFT_BORDER);
                g2.drawRect(x, y, w, totalH);

                g2.setFont(UITheme.font(Font.BOLD, 9.5f));
                g2.setColor(UITheme.PRIMARY_BLUE);
                int ty = y + 15;
                g2.drawString("TOTAL PERIODE", x + 10, ty);
                drawRightAligned(g2, String.valueOf(laporan.hitungJumlahTransaksi()), col2End - 12, ty);
                drawRightAligned(g2, formatRupiah(laporan.hitungTotalOmzet()), col3End - 12, ty);
                drawRightAligned(g2, formatRupiah(laporan.hitungTotalPajakTerkumpul()), col4End - 10, ty);
                y += totalH;
            }

            return y;
        }

        private void drawFooter(Graphics2D g2, int x, int y, int w, int page, int totalPages) {
            g2.setStroke(new BasicStroke(0.75f));
            g2.setColor(UITheme.BORDER_DEFAULT);
            g2.drawLine(x, y - 12, x + w, y - 12);

            g2.setFont(UITheme.font(Font.PLAIN, 8.5f));
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString("SI-RITEL \u2022 Sistem Informasi Kasir & Inventaris Ritel", x, y);
            String pg = "Halaman " + page + " dari " + totalPages;
            drawRightAligned(g2, pg, x + w, y);
        }

        private static void drawRightAligned(Graphics2D g2, String text, int rightX, int y) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, rightX - fm.stringWidth(text), y);
        }
    }

    private static String formatRupiah(double nominal) {
        NumberFormat format = NumberFormat.getCurrencyInstance(ID_LOCALE);
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(nominal).replace('\u00A0', ' ');
    }
}

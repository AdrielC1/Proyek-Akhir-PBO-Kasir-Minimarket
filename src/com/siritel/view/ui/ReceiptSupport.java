package com.siritel.view.ui;

import com.siritel.model.Transaksi;

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
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Helper terpusat untuk dialog pratinjau struk setelah pembayaran berhasil
 * dan pengiriman pencetakan struk ke printer (termasuk Microsoft Print to PDF
 * maupun printer thermal yang terpasang di Windows Spooler).
 */
public final class ReceiptSupport {
    private static final DateTimeFormatter HEADER_DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));

    private ReceiptSupport() {
    }

    public static void showSuccessModal(Component host, Transaksi transaksi, ToastManager toast) {
        if (transaksi == null) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(host);
        if (owner == null) {
            return;
        }

        JDialog dialog = ModernModal.createDialog(owner, "Transaksi Berhasil - " + transaksi.getIdTransaksi());
        JPanel card = new RoundedCardPanel(new BorderLayout(0, 14), UITheme.CARD_RADIUS);
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        // Header status sukses
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        badgeWrap.setOpaque(false);
        badgeWrap.add(ModernModal.createStatusIconBadge("CHECK", UITheme.SUCCESS_SOFT_BG, UITheme.SUCCESS, 38));
        header.add(badgeWrap, BorderLayout.WEST);

        JPanel titleCol = new JPanel(new GridBagLayout());
        titleCol.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        JLabel lblTitle = new JLabel("Transaksi Berhasil");
        lblTitle.setFont(UITheme.font(Font.BOLD, 16f));
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        titleRow.add(lblTitle, BorderLayout.CENTER);

        RoundedButton btnClose = new RoundedButton("\u00D7", ButtonVariant.GHOST, UITheme.SMALL_RADIUS);
        btnClose.setFont(UITheme.font(Font.BOLD, 16f));
        btnClose.setPreferredSize(new Dimension(26, 26));
        btnClose.addActionListener(e -> dialog.dispose());
        titleRow.add(btnClose, BorderLayout.EAST);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 2, 0);
        titleCol.add(titleRow, gbc);

        String subText = transaksi.getIdTransaksi()
                + " \u2022 " + transaksi.getTanggal().format(HEADER_DATE_FMT)
                + " \u2022 " + transaksi.getKasir().getNamaLengkap();
        JLabel lblSub = new JLabel(subText);
        lblSub.setFont(UITheme.font(Font.PLAIN, 12f));
        lblSub.setForeground(UITheme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        titleCol.add(lblSub, gbc);

        header.add(titleCol, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        // Kontainer pratinjau struk
        JTextArea areaNota = new JTextArea(transaksi.cetak());
        areaNota.setEditable(false);
        areaNota.setFocusable(false);
        areaNota.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaNota.setForeground(UITheme.TEXT_MAIN);
        areaNota.setBackground(UITheme.BG_PAGE);
        areaNota.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        areaNota.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(areaNota);
        scroll.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 10, new Insets(1, 1, 1, 1)));
        scroll.getViewport().setBackground(UITheme.BG_PAGE);
        scroll.setPreferredSize(new Dimension(390, 320));
        card.add(scroll, BorderLayout.CENTER);

        // Baris tombol aksi
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        RoundedButton btnCetak = new RoundedButton("Cetak Struk", ButtonVariant.OUTLINE, UITheme.BUTTON_RADIUS);
        btnCetak.setFont(UITheme.font(Font.BOLD, 12f));
        btnCetak.setPreferredSize(new Dimension(112, 36));
        btnCetak.addActionListener(e -> printStruk(dialog, transaksi, toast));

        RoundedButton btnSelesai = new RoundedButton("Selesai", ButtonVariant.PRIMARY, UITheme.BUTTON_RADIUS);
        btnSelesai.setFont(UITheme.font(Font.BOLD, 12f));
        btnSelesai.setPreferredSize(new Dimension(92, 36));
        btnSelesai.addActionListener(e -> dialog.dispose());

        actions.add(btnCetak);
        actions.add(btnSelesai);
        card.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.getRootPane().setDefaultButton(btnSelesai);
        ModernModal.showWithBackdrop(owner, dialog, 440, 480);
    }

    public static void printStruk(Component host, Transaksi transaksi, ToastManager toast) {
        if (transaksi == null) {
            return;
        }
        String[] lines = transaksi.cetak().split("\\R");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Struk-" + transaksi.getIdTransaksi());
        job.setPrintable((graphics, pageFormat, pageIndex) -> printReceiptPage(graphics, pageFormat, pageIndex, lines));

        if (!job.printDialog()) {
            return;
        }

        try {
            job.print();
            if (toast != null) {
                toast.show(
                        "Struk Dikirim",
                        "Struk " + transaksi.getIdTransaksi() + " berhasil dikirim ke printer.",
                        ToastType.SUCCESS
                );
            }
        } catch (PrinterException ex) {
            ModernModal.error(host, "Kesalahan Cetak Struk", "Gagal mencetak struk:\n" + ex.getMessage());
        }
    }

    private static int printReceiptPage(Graphics graphics, PageFormat pf, int pageIndex, String[] lines) {
        Graphics2D g2 = (Graphics2D) graphics;
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 9);
        g2.setFont(font);
        g2.setColor(Color.BLACK);

        FontMetrics fm = g2.getFontMetrics(font);
        int lineHeight = Math.max(11, fm.getHeight() + 1);
        double usableHeight = Math.max(100, pf.getImageableHeight() - 16);
        int linesPerPage = Math.max(1, (int) (usableHeight / lineHeight));
        int totalPages = Math.max(1, (int) Math.ceil((double) lines.length / linesPerPage));

        if (pageIndex >= totalPages) {
            return Printable.NO_SUCH_PAGE;
        }

        int start = pageIndex * linesPerPage;
        int end = Math.min(lines.length, start + linesPerPage);
        int x = (int) pf.getImageableX() + 6;
        int y = (int) pf.getImageableY() + fm.getAscent() + 6;

        for (int i = start; i < end; i++) {
            g2.drawString(lines[i], x, y);
            y += lineHeight;
        }
        return Printable.PAGE_EXISTS;
    }
}

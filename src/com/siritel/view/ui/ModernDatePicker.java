package com.siritel.view.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Komponen pemilih tanggal modern standar SI-RITEL dengan ikon kalender dan popup kalender bulanan interaktif.
 * Dapat digunakan ulang pada manajemen batch produk maupun filter periode laporan penjualan.
 */
public class ModernDatePicker extends JPanel {
    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE_DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE);

    private LocalDate selectedDate;
    private final boolean showExpiryPresets;
    private boolean hovered = false;
    private final JPopupMenu popup = new JPopupMenu();
    private final CalendarPopupPanel calendarPanel;

    public ModernDatePicker(LocalDate initialDate, boolean showExpiryPresets) {
        this.selectedDate = (initialDate != null) ? initialDate : LocalDate.now();
        this.showExpiryPresets = showExpiryPresets;
        setOpaque(false);
        setPreferredSize(new Dimension(0, 34));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        calendarPanel = new CalendarPopupPanel();
        popup.setBorder(new RoundedBorder(UITheme.BORDER_DEFAULT, 12, new Insets(8, 8, 8, 8)));
        popup.setBackground(UITheme.CARD_BG);
        popup.add(calendarPanel);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                repaint();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                repaint();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) {
                    return;
                }
                if (popup.isVisible()) {
                    popup.setVisible(false);
                } else {
                    calendarPanel.tampilkanBulan(selectedDate);
                    int popupW = calendarPanel.getPreferredSize().width + 16;
                    int x = Math.min(0, getWidth() - popupW);
                    popup.show(ModernDatePicker.this, x, getHeight() + 4);
                }
            }
        });
    }

    public LocalDate getDate() {
        return selectedDate;
    }

    public void setDate(LocalDate date) {
        if (date != null) {
            this.selectedDate = date;
            repaint();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled && popup.isVisible()) {
            popup.setVisible(false);
        }
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        boolean enabled = isEnabled();
        boolean active = popup.isVisible();

        g2.setColor(enabled ? UITheme.CARD_BG : UITheme.BG_PAGE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, UITheme.SMALL_RADIUS, UITheme.SMALL_RADIUS);

        Color border = !enabled ? UITheme.BORDER_DEFAULT
                : (active ? UITheme.PRIMARY_BLUE : (hovered ? UITheme.BORDER_HOVER : UITheme.BORDER_INPUT));
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.SMALL_RADIUS, UITheme.SMALL_RADIUS);

        g2.setFont(UITheme.font(Font.PLAIN, 12f));
        g2.setColor(enabled ? UITheme.TEXT_MAIN : UITheme.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        String text = DATE_DISPLAY_FMT.format(selectedDate);
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, 10, ty);

        int ix = w - 24;
        int iy = (h - 14) / 2;
        g2.setColor(enabled ? (active ? UITheme.PRIMARY_BLUE : UITheme.TEXT_MUTED) : UITheme.TEXT_HINT);
        g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawRoundRect(ix, iy + 2, 13, 11, 3, 3);
        g2.drawLine(ix, iy + 6, ix + 13, iy + 6);
        g2.drawLine(ix + 4, iy, ix + 4, iy + 3);
        g2.drawLine(ix + 9, iy, ix + 9, iy + 3);

        g2.dispose();
    }

    private class CalendarPopupPanel extends JPanel {
        private YearMonth currentMonth = YearMonth.from(selectedDate);
        private final JLabel lblMonthYear = new JLabel("", SwingConstants.CENTER);
        private final JPanel daysGrid = new JPanel(new GridLayout(6, 7, 3, 3));

        CalendarPopupPanel() {
            setLayout(new BorderLayout(0, 8));
            setBackground(UITheme.CARD_BG);
            setPreferredSize(new Dimension(264, showExpiryPresets ? 292 : 264));

            JPanel navBar = new JPanel(new BorderLayout(4, 0));
            navBar.setOpaque(false);

            JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            leftNav.setOpaque(false);
            RoundedButton btnPrevYear = createNavBtn("\u00AB", "Tahun sebelumnya");
            RoundedButton btnPrevMonth = createNavBtn("\u2039", "Bulan sebelumnya");
            btnPrevYear.addActionListener(e -> gantiBulan(currentMonth.minusYears(1)));
            btnPrevMonth.addActionListener(e -> gantiBulan(currentMonth.minusMonths(1)));
            leftNav.add(btnPrevYear);
            leftNav.add(btnPrevMonth);

            JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            rightNav.setOpaque(false);
            RoundedButton btnNextMonth = createNavBtn("\u203A", "Bulan berikutnya");
            RoundedButton btnNextYear = createNavBtn("\u00BB", "Tahun berikutnya");
            btnNextMonth.addActionListener(e -> gantiBulan(currentMonth.plusMonths(1)));
            btnNextYear.addActionListener(e -> gantiBulan(currentMonth.plusYears(1)));
            rightNav.add(btnNextMonth);
            rightNav.add(btnNextYear);

            lblMonthYear.setFont(UITheme.font(Font.BOLD, 13f));
            lblMonthYear.setForeground(UITheme.TEXT_MAIN);

            navBar.add(leftNav, BorderLayout.WEST);
            navBar.add(lblMonthYear, BorderLayout.CENTER);
            navBar.add(rightNav, BorderLayout.EAST);

            JPanel weekHeader = new JPanel(new GridLayout(1, 7, 3, 0));
            weekHeader.setOpaque(false);
            String[] hari = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
            for (String hName : hari) {
                JLabel lblDay = new JLabel(hName, SwingConstants.CENTER);
                lblDay.setFont(UITheme.font(Font.BOLD, 11f));
                lblDay.setForeground(UITheme.TEXT_MUTED);
                weekHeader.add(lblDay);
            }

            JPanel northWrap = new JPanel(new BorderLayout(0, 8));
            northWrap.setOpaque(false);
            northWrap.add(navBar, BorderLayout.NORTH);
            northWrap.add(weekHeader, BorderLayout.SOUTH);
            add(northWrap, BorderLayout.NORTH);

            daysGrid.setOpaque(false);
            add(daysGrid, BorderLayout.CENTER);

            JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            quickBar.setOpaque(false);
            quickBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_DEFAULT),
                    BorderFactory.createEmptyBorder(8, 0, 0, 0)
            ));

            quickBar.add(createPresetBtn("Hari Ini", LocalDate::now));
            if (showExpiryPresets) {
                quickBar.add(createPresetBtn("+1 Bln", () -> LocalDate.now().plusMonths(1)));
                quickBar.add(createPresetBtn("+3 Bln", () -> LocalDate.now().plusMonths(3)));
                quickBar.add(createPresetBtn("+6 Bln", () -> LocalDate.now().plusMonths(6)));
                quickBar.add(createPresetBtn("+1 Thn", () -> LocalDate.now().plusYears(1)));
            }
            add(quickBar, BorderLayout.SOUTH);

            renderGrid();
        }

        void tampilkanBulan(LocalDate target) {
            currentMonth = YearMonth.from(target != null ? target : LocalDate.now());
            renderGrid();
        }

        private void gantiBulan(YearMonth target) {
            currentMonth = target;
            renderGrid();
        }

        private void renderGrid() {
            String namaBulan = currentMonth.getMonth().getDisplayName(TextStyle.FULL, ID_LOCALE);
            String capitalized = namaBulan.substring(0, 1).toUpperCase(ID_LOCALE) + namaBulan.substring(1);
            lblMonthYear.setText(capitalized + " " + currentMonth.getYear());

            daysGrid.removeAll();
            LocalDate firstOfMonth = currentMonth.atDay(1);
            int offset = firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            LocalDate cellDate = firstOfMonth.minusDays(offset);
            LocalDate today = LocalDate.now();

            for (int i = 0; i < 42; i++) {
                LocalDate d = cellDate.plusDays(i);
                boolean inCurrentMonth = d.getMonth() == currentMonth.getMonth();
                boolean isSelected = d.equals(selectedDate);
                boolean isToday = d.equals(today);

                daysGrid.add(new DayCellButton(d, inCurrentMonth, isSelected, isToday));
            }
            daysGrid.revalidate();
            daysGrid.repaint();
        }

        private RoundedButton createNavBtn(String text, String tip) {
            RoundedButton btn = new RoundedButton(text, ButtonVariant.GHOST, UITheme.SMALL_RADIUS);
            btn.setFont(UITheme.font(Font.BOLD, 14f));
            btn.setPreferredSize(new Dimension(26, 26));
            btn.setToolTipText(tip);
            return btn;
        }

        private RoundedButton createPresetBtn(String label, Supplier<LocalDate> dateSupplier) {
            RoundedButton btn = new RoundedButton(label, ButtonVariant.OUTLINE, UITheme.SMALL_RADIUS);
            btn.setFont(UITheme.font(Font.BOLD, 10f));
            btn.setPreferredSize(new Dimension(showExpiryPresets ? 48 : 80, 24));
            btn.addActionListener(e -> {
                setDate(dateSupplier.get());
                popup.setVisible(false);
            });
            return btn;
        }

        private class DayCellButton extends JButton {
            private final boolean inCurrentMonth;
            private final boolean isSelected;
            private final boolean isToday;
            private boolean cellHovered = false;

            DayCellButton(LocalDate date, boolean inCurrentMonth, boolean isSelected, boolean isToday) {
                super(String.valueOf(date.getDayOfMonth()));
                this.inCurrentMonth = inCurrentMonth;
                this.isSelected = isSelected;
                this.isToday = isToday;
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setFont(UITheme.font(isSelected || isToday ? Font.BOLD : Font.PLAIN, 12f));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        cellHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        cellHovered = false;
                        repaint();
                    }
                });

                addActionListener(e -> {
                    setDate(date);
                    popup.setVisible(false);
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (isSelected) {
                    g2.setColor(UITheme.PRIMARY_BLUE);
                    g2.fillRoundRect(1, 1, w - 3, h - 3, UITheme.SMALL_RADIUS, UITheme.SMALL_RADIUS);
                } else if (cellHovered) {
                    g2.setColor(UITheme.PRIMARY_SOFT_BG);
                    g2.fillRoundRect(1, 1, w - 3, h - 3, UITheme.SMALL_RADIUS, UITheme.SMALL_RADIUS);
                }

                if (isToday && !isSelected) {
                    g2.setColor(UITheme.PRIMARY_BLUE);
                    g2.drawRoundRect(1, 1, w - 3, h - 3, UITheme.SMALL_RADIUS, UITheme.SMALL_RADIUS);
                }

                Color fg = isSelected ? Color.WHITE
                        : (isToday ? UITheme.PRIMARY_BLUE
                        : (inCurrentMonth ? UITheme.TEXT_MAIN : new Color(186, 196, 209)));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                int tx = (w - fm.stringWidth(txt)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(fg);
                g2.drawString(txt, tx, ty);

                g2.dispose();
            }
        }
    }
}

package com.siritel.view;

import com.siritel.controller.AuthController;
import com.siritel.model.Pengguna;
import com.siritel.view.ui.InlineAlertPanel;
import com.siritel.view.ui.RoundedButton;
import com.siritel.view.ui.RoundedPasswordField;
import com.siritel.view.ui.RoundedTextField;
import com.siritel.view.ui.UITheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Jendela Login SI-RITEL dengan desain kartu modern sesuai target login.png.
 * Menggunakan kartu rounded dengan soft shadow berlapis, notifikasi error inline yang halus,
 * retensi kredensial saat login gagal, dan animasi shake berbasis translasi Java2D.
 */
public class LoginView extends JFrame {

    private final RoundedTextField txtUsername = new RoundedTextField();
    private final RoundedPasswordField txtPassword = new RoundedPasswordField();
    private final InlineAlertPanel alertPanel = new InlineAlertPanel();
    private final RoundedButton btnLogin = new RoundedButton("Masuk");

    private final AuthController authController = new AuthController();
    private JPanel cardPanel;
    private int cardShakeOffset = 0;
    private Timer shakeTimer;

    public LoginView() {
        setTitle("SI-RITEL — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 600);
        setMinimumSize(new Dimension(440, 560));
        setLocationRelativeTo(null);

        // 1. Root Panel dengan Gradien Vertikal (Biru Sangat Muda -> Putih Keabuan)
        JPanel rootPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.BG_GRADIENT_TOP,
                        0, getHeight(), UITheme.BG_GRADIENT_BOTTOM
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        // 2. Kartu Putih Rounded (Lebar 400, Radius 16, Padding 32, Drop Shadow Berlapis)
        cardPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Terapkan offset shake kartu via translate Java2D tanpa merusak GridBagLayout
                g2.translate(cardShakeOffset, 0);

                int w = getWidth();
                int h = getHeight();

                // Multi-pass Soft Drop Shadow halus berlapis
                for (int i = 6; i >= 1; i--) {
                    int alpha = (int) (14 / (float) i);
                    g2.setColor(new Color(15, 23, 42, Math.min(255, alpha)));
                    g2.drawRoundRect(8 - i, 8 - i + 2, w - 16 + (i * 2), h - 16 + (i * 2),
                            UITheme.CARD_RADIUS + i, UITheme.CARD_RADIUS + i);
                }

                // Badan Kartu Putih Solid
                g2.setColor(UITheme.CARD_BG);
                g2.fillRoundRect(8, 8, w - 16, h - 16, UITheme.CARD_RADIUS, UITheme.CARD_RADIUS);

                // Garis Border Kartu 1 px
                g2.setColor(UITheme.CARD_BORDER);
                g2.drawRoundRect(8, 8, w - 17, h - 17, UITheme.CARD_RADIUS, UITheme.CARD_RADIUS);

                g2.dispose();
            }

            @Override
            protected void paintChildren(Graphics g) {
                // Seluruh elemen anak ikut bergoyang bersama translasi visual tanpa merusak koordinat layout
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(cardShakeOffset, 0);
                super.paintChildren(g2);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                return new Dimension(UITheme.CARD_WIDTH, Math.max(516, pref.height));
            }
        };
        cardPanel.setOpaque(false);
        // Padding kartu: atas 32, kiri 32, kanan 32, bawah 28
        cardPanel.setBorder(BorderFactory.createEmptyBorder(
                UITheme.CARD_PADDING, UITheme.CARD_PADDING,
                UITheme.PADDING_BOTTOM, UITheme.CARD_PADDING
        ));

        // 3. Bangun Elemen Form Sesuai Tata Letak & Jarak login.png
        buildCardContent();

        // 4. Pasang Card ke Root GridBag (selalu terpusat di layar termasuk saat maximize)
        GridBagConstraints rootGbc = new GridBagConstraints();
        rootGbc.gridx = 0;
        rootGbc.gridy = 0;
        rootGbc.anchor = GridBagConstraints.CENTER;
        rootPanel.add(cardPanel, rootGbc);

        setContentPane(rootPanel);

        // 5. Inisialisasi Listener & Interaksi
        initInteractions();

        // Tombol Enter otomatis memicu tombol "Masuk"
        getRootPane().setDefaultButton(btnLogin);
    }

    private void buildCardContent() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        // 1. Tile Ikon Tas 48x48 radius 12 (terpusat, ukuran tetap)
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);
        cardPanel.add(createBrandBadge(), gbc);

        // 2. Judul "SI-RITEL" Bold 24 pt (jarak tile->judul 16 px)
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(UITheme.GAP_TILE_TO_TITLE, 0, 0, 0);
        JLabel lblTitle = new JLabel("SI-RITEL", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.titleFont());
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        cardPanel.add(lblTitle, gbc);

        // 3. Subtitle 13 pt abu (jarak judul->subtitle 4 px)
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(UITheme.GAP_TITLE_TO_SUBTITLE, 0, 0, 0);
        JLabel lblSubtitle = new JLabel("Sistem Informasi Kasir Ritel", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.subtitleFont());
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);
        cardPanel.add(lblSubtitle, gbc);

        // 4. Label "Username" 13 pt (jarak subtitle->label 28 px)
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UITheme.GAP_SUBTITLE_TO_LABEL, 0, 0, 0);
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(UITheme.labelFont());
        lblUsername.setForeground(UITheme.TEXT_LABEL);
        cardPanel.add(lblUsername, gbc);

        // 5. Field Username 40 px (jarak label->field 8 px)
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(UITheme.GAP_LABEL_TO_FIELD, 0, 0, 0);
        cardPanel.add(txtUsername, gbc);

        // 6. Label "Password" 13 pt (jarak field->label 16 px)
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UITheme.GAP_FIELD_TO_FIELD, 0, 0, 0);
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(UITheme.labelFont());
        lblPassword.setForeground(UITheme.TEXT_LABEL);
        cardPanel.add(lblPassword, gbc);

        // 7. Field Password 40 px (jarak label->field 8 px)
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(UITheme.GAP_LABEL_TO_FIELD, 0, 0, 0);
        cardPanel.add(txtPassword, gbc);

        // 8. Banner Error Inline 40 px (jarak field->banner 12 px)
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(UITheme.GAP_FIELD_TO_BANNER, 0, 0, 0);
        cardPanel.add(alertPanel, gbc);

        // 9. Tombol "Masuk" 40 px (jarak banner->tombol 16 px)
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(UITheme.GAP_BANNER_TO_BUTTON, 0, 0, 0);
        cardPanel.add(btnLogin, gbc);

        // 10. Hint Keycap "Tekan [Enter] untuk masuk" (jarak tombol->hint 20 px)
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(UITheme.GAP_BUTTON_TO_HINT, 0, 0, 0);
        cardPanel.add(createKeycapHintPanel(), gbc);
    }

    private JPanel createBrandBadge() {
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Latar belakang kotak rounded biru sangat muda (48x48 radius 12)
                g2.setColor(UITheme.BRAND_ICON_BG);
                g2.fillRoundRect(0, 0, w, h, UITheme.TILE_RADIUS, UITheme.TILE_RADIUS);
                g2.setColor(UITheme.BRAND_ICON_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.TILE_RADIUS, UITheme.TILE_RADIUS);

                // Ikon tas kasir Java2D warna biru (#2563EB)
                g2.setColor(UITheme.PRIMARY_BLUE);
                int cx = w / 2;
                int cy = h / 2;
                // Badan tas rounded
                g2.drawRoundRect(cx - 9, cy - 5, 18, 15, 3, 3);
                // Gagang atas tas
                g2.drawArc(cx - 5, cy - 10, 10, 10, 0, 180);
                // Aksen saku tas
                g2.drawArc(cx - 5, cy - 4, 10, 6, 180, 180);

                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(UITheme.TILE_SIZE, UITheme.TILE_SIZE));
        badge.setMaximumSize(new Dimension(UITheme.TILE_SIZE, UITheme.TILE_SIZE));
        badge.setMinimumSize(new Dimension(UITheme.TILE_SIZE, UITheme.TILE_SIZE));
        badge.setOpaque(false);
        return badge;
    }

    private JPanel createKeycapHintPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        panel.setOpaque(false);

        JLabel lblPre = new JLabel("Tekan ");
        lblPre.setFont(UITheme.hintFont());
        lblPre.setForeground(UITheme.TEXT_HINT);

        // Keycap "Enter" rounded
        JPanel keycap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.KEYCAP_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.setColor(UITheme.KEYCAP_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.dispose();
            }
        };
        keycap.setOpaque(false);
        keycap.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        JLabel lblKey = new JLabel("Enter");
        lblKey.setFont(UITheme.keycapFont());
        lblKey.setForeground(UITheme.KEYCAP_TEXT);
        keycap.add(lblKey, BorderLayout.CENTER);

        JLabel lblPost = new JLabel(" untuk masuk");
        lblPost.setFont(UITheme.hintFont());
        lblPost.setForeground(UITheme.TEXT_HINT);

        panel.add(lblPre);
        panel.add(keycap);
        panel.add(lblPost);
        return panel;
    }

    private void initInteractions() {
        DocumentListener clearErrorListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearErrorState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearErrorState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearErrorState();
            }
        };

        txtUsername.getDocument().addDocumentListener(clearErrorListener);
        txtPassword.getInternalField().getDocument().addDocumentListener(clearErrorListener);

        // Aksi Tombol Masuk via SwingWorker
        btnLogin.addActionListener(e -> executeLogin());
    }

    private void clearErrorState() {
        if (alertPanel.isAlertVisible() || txtUsername.isError() || txtPassword.isError()) {
            alertPanel.hideAlert();
            txtUsername.setError(false);
            txtPassword.setError(false);
        }
    }

    private void executeLogin() {
        String username = txtUsername.getText().trim();
        char[] passwordChars = txtPassword.getPassword();
        String password = new String(passwordChars);

        // 1. Validasi Input Kosong Inline (Fase 3 poin 5)
        if (username.isEmpty()) {
            showError("Username wajib diisi", true, false);
            txtUsername.requestFocusInWindow();
            Arrays.fill(passwordChars, '\0');
            return;
        }

        if (password.isEmpty()) {
            showError("Password wajib diisi", false, true);
            txtPassword.requestFocusInWindow();
            Arrays.fill(passwordChars, '\0');
            return;
        }

        // 2. Jalankan Autentikasi Asinkron via SwingWorker agar EDT tidak freeze
        btnLogin.setEnabled(false);
        btnLogin.setText("Memproses...");

        SwingWorker<Pengguna, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Pengguna doInBackground() throws Exception {
                try {
                    return authController.login(username, password);
                } catch (IllegalArgumentException ex) {
                    errorMessage = ex.getMessage();
                    return null;
                } catch (SQLException ex) {
                    errorMessage = AuthController.pesanSqlError(ex);
                    return null;
                } finally {
                    Arrays.fill(passwordChars, '\0');
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Masuk");

                try {
                    Pengguna user = get();
                    if (user != null) {
                        dispose();
                        new MainFrame(user).setVisible(true);
                    } else {
                        // Kredensial salah atau error database
                        String msg = (errorMessage != null && !errorMessage.isEmpty())
                                ? errorMessage
                                : "Username atau password salah";

                        showError(msg, true, true);

                        // Pertahankan input username & password lalu sorot seluruh teks password agar mudah ditimpa
                        txtPassword.requestFocusInWindow();
                        txtPassword.selectAll();
                    }
                } catch (Exception ex) {
                    showError("Terjadi kesalahan sistem: " + ex.getMessage(), true, true);
                    txtPassword.requestFocusInWindow();
                }
            }
        };

        worker.execute();
    }

    private void showError(String message, boolean errorUser, boolean errorPass) {
        txtUsername.setError(errorUser);
        txtPassword.setError(errorPass);
        // Teks di-set sebelum banner membesar dan tidak pernah dihapus selama banner visible
        alertPanel.showAlert(message);
        triggerCardShake();
    }

    private void triggerCardShake() {
        if (shakeTimer != null && shakeTimer.isRunning()) {
            shakeTimer.stop();
        }

        // Amplitudo maks ±6 px, durasi 10 langkah * 25 ms = 250 ms (di bawah batas 300 ms)
        final int[] shakeOffsets = {0, -6, 6, -5, 5, -3, 3, -1, 1, 0};
        final int[] index = {0};

        shakeTimer = new Timer(25, e -> {
            if (index[0] < shakeOffsets.length) {
                cardShakeOffset = shakeOffsets[index[0]];
                cardPanel.repaint();
                index[0]++;
            } else {
                cardShakeOffset = 0;
                cardPanel.repaint();
                ((Timer) e.getSource()).stop();
            }
        });
        shakeTimer.start();
    }
}

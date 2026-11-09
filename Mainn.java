package com.basizon;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Mainn {

    // ===================== BRANDING =====================
    static final String APP_NAME = "BAGARA";
    static final String APP_SUB = "HEALTHCARE SYSTEM";
    static final String SLOGAN_1 = "Beyond the Click,";
    static final String SLOGAN_2 = "Healthcare Made Quick.";
    static final String[] POWERED_BY = {"DR. BAMAN", "DR. GARCIA", "DR. RAMOS"};

    // ===================== PALETTE =====================
    static final Color SIDEBAR = new Color(11, 18, 32);
    static final Color SIDEBAR_SOFT = new Color(24, 34, 54);
    static final Color PRIMARY = new Color(79, 70, 229);
    static final Color ACCENT = new Color(6, 182, 212);
    static final Color BG = new Color(241, 245, 249);
    static final Color WHITE = Color.WHITE;
    static final Color TEXT = new Color(15, 23, 42);
    static final Color MUTED = new Color(100, 116, 139);
    static final Color BORDER = new Color(226, 232, 240);
    static final Color SUCCESS = new Color(16, 185, 129);
    static final Color WARN = new Color(245, 158, 11);
    static final Color DANGER = new Color(239, 68, 68);
    static final Color NEON = new Color(103, 232, 249);
    static final Color VIOLET = new Color(167, 139, 250);

    static String pickFont(String... names) {
        Set<String> have = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String n : names) if (have.contains(n)) return n;
        return Font.SANS_SERIF;
    }

    static final String FONT = pickFont("Poppins", "Segoe UI Variable Text", "Segoe UI", "Inter", "Helvetica Neue", "Arial");
    static final String DISPLAY = pickFont("Montserrat", "Poppins", "Segoe UI Variable Display", "Segoe UI", "Arial");

    static Font f(int style, int size) {
        boolean heading = size >= 20 && (style & Font.BOLD) != 0;
        return new Font(heading ? DISPLAY : FONT, style, size);
    }

    // ===================== DATA =====================
    private static final Map<String, Account> accounts = new LinkedHashMap<>();
    private static final List<Patient> patients = new ArrayList<>();
    private static final List<Appointment> appointments = new ArrayList<>();
    private static final List<Admission> admissions = new ArrayList<>();
    private static final List<Prescription> prescriptions = new ArrayList<>();
    private static final List<Medicine> medicines = new ArrayList<>();
    private static final List<MedRecord> records = new ArrayList<>();
    private static final List<Invoice> invoices = new ArrayList<>();

    private static final Set<String> BOOKABLE_DOCTORS =
            new LinkedHashSet<>(Arrays.asList("ramos", "garcia", "baman"));

    private static int apptNo, admNo, rxNo, recNo, invNo;
    private static Account current;
    private static final OtpService otp = new OtpService();
    private static final SecureRandom RNG = new SecureRandom();

    private JFrame frame;
    private JLabel pageTitle, identity, sideName;
    private String currentPage = "Dashboard";
    private final Map<String, NavButton> navs = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    enum Role { ADMIN, DOCTOR, RECEPTIONIST, PATIENT }

    static class Account {
        String username, displayName, salt, hash;
        String specialty = "";
        String email = "", phone = "";
        Role role;
        boolean active = true;

        Account(String username, String password, String displayName, Role role) {
            this.username = username;
            this.displayName = displayName;
            this.role = role;
            setPassword(password);
        }

        void setPassword(String p) {
            byte[] s = new byte[8];
            RNG.nextBytes(s);
            salt = HexFormat.of().formatHex(s);
            hash = sha(salt + p);
        }

        boolean check(String p) {
            return hash.equals(sha(salt + p));
        }

        private static String sha(String s) {
            try {
                return HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static class Patient {
        String id, name, gender, contact, address, blood, username;
        String allergies = "", emergency = "";
        int age;

        Patient(String id, String name, int age, String gender,
                String contact, String address, String blood, String username) {
            this.id = id; this.name = name; this.age = age; this.gender = gender;
            this.contact = contact; this.address = address; this.blood = blood;
            this.username = username;
        }
    }

    static class Appointment {
        String id, patient, doctor, date, time, reason, status;

        Appointment(String id, String patient, String doctor, String date,
                    String time, String reason, String status) {
            this.id = id; this.patient = patient; this.doctor = doctor; this.date = date;
            this.time = time; this.reason = reason; this.status = status;
        }
    }

    static class Admission {
        String id, patient, roomType, room, bed, admitDate, status;
        String dischargeDate = "—";

        Admission(String id, String patient, String roomType, String room,
                  String bed, String admitDate, String status) {
            this.id = id; this.patient = patient; this.roomType = roomType; this.room = room;
            this.bed = bed; this.admitDate = admitDate; this.status = status;
        }
    }

    static class Prescription {
        String id, patient, doctor, medicine, dosage, frequency, date, instructions, status;
        int qty;

        Prescription(String id, String patient, String doctor, String medicine, String dosage,
                     String frequency, int qty, String instructions, String date) {
            this.id = id; this.patient = patient; this.doctor = doctor; this.medicine = medicine;
            this.dosage = dosage; this.frequency = frequency; this.qty = qty;
            this.instructions = instructions; this.date = date; this.status = "Pending";
        }
    }

    static class Medicine {
        String name;
        int stock;

        Medicine(String name, int stock) {
            this.name = name;
            this.stock = stock;
        }
    }

    static class MedRecord {
        String id, patient, doctor, date, diagnosis, notes;

        MedRecord(String id, String patient, String doctor, String date, String diagnosis, String notes) {
            this.id = id; this.patient = patient; this.doctor = doctor;
            this.date = date; this.diagnosis = diagnosis; this.notes = notes;
        }
    }

    static class Invoice {
        String id, patient, date, cashier;
        double subtotal, discount, total, paid, change;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("OptionPane.messageFont", f(Font.PLAIN, 14));
            UIManager.put("OptionPane.buttonFont", f(Font.BOLD, 13));
            UIManager.put("OptionPane.background", WHITE);
            UIManager.put("Panel.background", WHITE);
            UIManager.put("OptionPane.messageForeground", TEXT);
            UIManager.put("ToolTip.font", f(Font.PLAIN, 12));
            initializeData();
            new Mainn().showLogin();
        });
    }

    private static void addDoctor(String username, String displayName, String specialty) {
        Account a = new Account(username, username + "123", displayName, Role.DOCTOR);
        a.specialty = specialty;
        accounts.put(username, a);
    }

    private static void initializeData() {
        accounts.clear();
        accounts.put("admin", new Account("admin", "admin123", "Admin", Role.ADMIN));
        addDoctor("ramos", "Ramos", "General Medicine");
        addDoctor("garcia", "Garcia", "General Medicine");
        addDoctor("baman", "Baman", "General Medicine");
        accounts.put("sibonga", new Account("sibonga", "sibonga123", "Sibonga", Role.RECEPTIONIST));

        medicines.clear();
        medicines.add(new Medicine("Paracetamol 500mg", 240));
        medicines.add(new Medicine("Amoxicillin 500mg", 120));
        medicines.add(new Medicine("Cetirizine 10mg", 80));
        medicines.add(new Medicine("Losartan 50mg", 25));
        medicines.add(new Medicine("Metformin 500mg", 0));
    }

    // ===================== CUSTOM COMPONENTS =====================

    static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static class RoundPanel extends JPanel {
        private final Color bg;
        private final int r;
        private final boolean elevated;

        RoundPanel(Color bg, int r, int pad, boolean elevated) {
            this.bg = bg; this.r = r; this.elevated = elevated;
            setOpaque(false);
            setBorder(new EmptyBorder(pad, pad, pad + (elevated ? 4 : 0), pad));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight() - (elevated ? 4 : 0);
            if (elevated) {
                for (int i = 1; i <= 4; i++) {
                    g2.setColor(new Color(15, 23, 42, 7));
                    g2.fillRoundRect(0, i, w, h, r, r);
                }
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, r, r);
            if (elevated) {
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    enum Kind { PRIMARY, SOFT, DANGER, GHOST_DANGER }

    static class RoundButton extends JButton {
        private Kind kind;
        private boolean hover;

        RoundButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(f(Font.BOLD, 13));
            setBorder(new EmptyBorder(11, 18, 11, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            applyFg();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setKind(Kind k) {
            kind = k;
            applyFg();
            repaint();
        }

        private void applyFg() {
            switch (kind) {
                case PRIMARY -> setForeground(WHITE);
                case SOFT -> setForeground(PRIMARY);
                case DANGER -> setForeground(DANGER);
                default -> setForeground(new Color(252, 165, 165));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            if (!isEnabled()) g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            switch (kind) {
                case PRIMARY -> {
                    g2.setColor(new Color(79, 70, 229, hover ? 70 : 38));
                    g2.fillRoundRect(2, 4, w - 4, h - 3, 14, 14);
                    g2.setPaint(new GradientPaint(0, 0, hover ? PRIMARY.darker() : PRIMARY,
                            w, h, hover ? ACCENT.darker() : ACCENT));
                }
                case SOFT -> g2.setColor(hover ? new Color(224, 231, 255) : new Color(238, 242, 255));
                case DANGER -> g2.setColor(hover ? new Color(254, 202, 202) : new Color(254, 226, 226));
                default -> g2.setColor(hover ? new Color(127, 29, 29, 170) : new Color(127, 29, 29, 100));
            }
            g2.fillRoundRect(0, 0, w, h - (kind == Kind.PRIMARY ? 2 : 0), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static void paintField(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        aa(g2);
        g2.setColor(c.isEnabled() ? new Color(248, 250, 252) : new Color(241, 245, 249));
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
        if (c.hasFocus()) {
            g2.setColor(new Color(79, 70, 229, 36));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 12, 12);
        }
        g2.setColor(c.hasFocus() ? PRIMARY : BORDER);
        g2.setStroke(new BasicStroke(c.hasFocus() ? 1.8f : 1f));
        g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
        g2.dispose();
    }

    private static void styleInput(JTextField t) {
        t.setOpaque(false);
        t.setFont(f(Font.PLAIN, 14));
        t.setForeground(TEXT);
        t.setCaretColor(PRIMARY);
        t.setBorder(new EmptyBorder(9, 14, 9, 14));
        t.setPreferredSize(new Dimension(240, 42));
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { t.repaint(); }
            @Override public void focusLost(FocusEvent e) { t.repaint(); }
        });
    }

    static class RoundField extends JTextField {
        RoundField(String text) {
            super(text);
            styleInput(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintField(g, this);
            super.paintComponent(g);
        }
    }

    static class RoundPassword extends JPasswordField {
        RoundPassword() {
            styleInput(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintField(g, this);
            super.paintComponent(g);
        }
    }

    static class Avatar extends JComponent {
        private final String initial;

        Avatar(String name, int size) {
            initial = (name == null || name.isBlank()) ? "?" : name.trim().substring(0, 1).toUpperCase();
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int s = Math.min(getWidth(), getHeight());
            g2.setPaint(new GradientPaint(0, 0, PRIMARY, s, s, ACCENT));
            g2.fillOval(0, 0, s, s);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(1, 1, s - 3, s - 3);
            g2.setColor(WHITE);
            g2.setFont(f(Font.BOLD, s / 2));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, (s - fm.stringWidth(initial)) / 2, (s - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ===================== BRAND COMPONENTS =====================

    static class TextLine extends JComponent {
        private final TextLayout tl;
        private final Color c1, c2;

        TextLine(String text, Font base, float tracking, Color c1, Color c2) {
            this.c1 = c1;
            this.c2 = c2;
            Map<TextAttribute, Object> m = new HashMap<>();
            m.put(TextAttribute.TRACKING, tracking);
            Font font = base.deriveFont(m);
            tl = new TextLayout(text, font, new FontRenderContext(null, true, true));
            int w = (int) Math.ceil(tl.getAdvance()) + 4;
            int h = (int) Math.ceil(tl.getAscent() + tl.getDescent() + tl.getLeading()) + 4;
            setPreferredSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            if (c2 == null) g2.setColor(c1);
            else g2.setPaint(new GradientPaint(0, 0, c1, getWidth(), 0, c2));
            tl.draw(g2, 1, tl.getAscent() + 2);
            g2.dispose();
        }
    }

    static class LogoMark extends JComponent {
        LogoMark(int size) {
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int s = Math.min(getWidth(), getHeight());
            int in = Math.max(3, s / 10);
            g2.setColor(new Color(34, 211, 238, 55));
            g2.fillRoundRect(0, 0, s, s, s / 3, s / 3);
            g2.setPaint(new GradientPaint(0, 0, PRIMARY, s, s, ACCENT));
            g2.fillRoundRect(in, in, s - 2 * in, s - 2 * in, s / 3, s / 3);
            g2.setColor(WHITE);
            int cw = Math.max(4, (s - 2 * in) / 5), len = (s - 2 * in) * 3 / 5, c = s / 2;
            g2.fillRoundRect(c - cw / 2, c - len / 2, cw, len, cw / 2, cw / 2);
            g2.fillRoundRect(c - len / 2, c - cw / 2, len, cw, cw / 2, cw / 2);
            g2.dispose();
        }
    }

    static class AccentBar extends JComponent {
        AccentBar() {
            setPreferredSize(new Dimension(64, 5));
            setMaximumSize(new Dimension(64, 5));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setPaint(new GradientPaint(0, 0, ACCENT, getWidth(), 0, VIOLET));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
            g2.dispose();
        }
    }

    static class Pill extends JComponent {
        private final TextLayout tl;

        Pill(String text) {
            Map<TextAttribute, Object> m = new HashMap<>();
            m.put(TextAttribute.TRACKING, 0.08f);
            Font font = f(Font.BOLD, 11).deriveFont(m);
            tl = new TextLayout(text, font, new FontRenderContext(null, true, true));
            Dimension d = new Dimension((int) Math.ceil(tl.getAdvance()) + 40, 34);
            setPreferredSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(255, 255, 255, 24));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(new Color(103, 232, 249, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(new Color(34, 211, 238, 70));
            g2.fillOval(10, h / 2 - 6, 12, 12);
            g2.setColor(new Color(34, 211, 238));
            g2.fillOval(13, h / 2 - 3, 6, 6);
            g2.setColor(WHITE);
            tl.draw(g2, 28, h / 2f + (tl.getAscent() - tl.getDescent()) / 2f);
            g2.dispose();
        }
    }

    static JPanel brandRow(int mark, int size) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 0, 12);
        p.add(new LogoMark(mark), gc);
        gc.insets = new Insets(0, 0, 0, 0);
        gc.weightx = 1;
        p.add(new TextLine(APP_NAME, f(Font.BOLD, size), 0.06f, WHITE, null), gc);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    static JPanel indent(JComponent c, int left, int bottom) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, left, bottom, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c, BorderLayout.WEST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    // ===================== TECH / ANIMATED COMPONENTS =====================

    private static final Random VFX = new Random();

    static javax.swing.Timer startTicker(JComponent c, int ms, Runnable step) {
        javax.swing.Timer t = new javax.swing.Timer(ms, null);
        t.addActionListener(e -> {
            if (!c.isDisplayable()) { t.stop(); return; }
            step.run();
            c.repaint();
        });
        t.start();
        return t;
    }

    static double ecgWave(double x) {
        x = x - Math.floor(x);
        double v = 0;
        v += 0.12 * Math.exp(-Math.pow((x - 0.18) / 0.035, 2));
        v -= 0.14 * Math.exp(-Math.pow((x - 0.36) / 0.012, 2));
        v += 1.00 * Math.exp(-Math.pow((x - 0.40) / 0.014, 2));
        v -= 0.28 * Math.exp(-Math.pow((x - 0.44) / 0.014, 2));
        v += 0.25 * Math.exp(-Math.pow((x - 0.68) / 0.055, 2));
        return v;
    }

    static void drawEcg(Graphics2D g2, int x0, int w, int baseY, int amp, double phase,
                        Color c, float stroke, double alphaScale) {
        final int beat = 230;
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int lastX = x0, lastY = baseY;
        for (int x = x0; x <= x0 + w; x += 2) {
            double t = (double) (x - x0) / beat + phase;
            int y = baseY - (int) Math.round(ecgWave(t) * amp);
            if (x > x0) {
                double fade = (double) (x - x0) / w;
                int alpha = (int) Math.min(255, (40 + 215 * Math.pow(fade, 1.6)) * alphaScale);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, alpha)));
                g2.drawLine(lastX, lastY, x, y);
            }
            lastX = x;
            lastY = y;
        }
    }

    static String navIcon(String page) {
        return switch (page) {
            case "Dashboard" -> "◈";
            case "Patients", "My Patients", "Patient Directory" -> "☺";
            case "Register Patient" -> "＋";
            case "Doctors & Staff" -> "⚕";
            case "Appointments", "My Appointments", "Book Appointment" -> "◷";
            case "Current Queue" -> "◉";
            case "Admissions" -> "▤";
            case "Medical Records" -> "☰";
            case "Billing", "My Billing" -> "₱";
            case "Pharmacy" -> "✚";
            case "Prescriptions", "My Prescriptions" -> "℞";
            case "Accounts" -> "⚙";
            case "My Profile" -> "☻";
            default -> "•";
        };
    }

    static String statGlyph(String title) {
        String t = title.toLowerCase();
        if (t.contains("prescription")) return "℞";
        if (t.contains("record")) return "☰";
        if (t.contains("paid") || t.contains("revenue")) return "₱";
        if (t.contains("queue") || t.contains("waiting")) return "◉";
        if (t.contains("patient")) return "☺";
        if (t.contains("admitted")) return "▤";
        if (t.contains("appointment") || t.contains("upcoming")) return "◷";
        return "✚";
    }

    static class TechBackdrop extends JPanel {
        private static final int N = 38;
        private final Color a, b;
        private final float[] px = new float[N], py = new float[N], vx = new float[N], vy = new float[N];
        private double phase;

        TechBackdrop(Color a, Color b) {
            this.a = a;
            this.b = b;
            setOpaque(false);
            for (int i = 0; i < N; i++) {
                px[i] = VFX.nextFloat();
                py[i] = VFX.nextFloat();
                vx[i] = (VFX.nextFloat() - 0.5f) * 0.0016f;
                vy[i] = (VFX.nextFloat() - 0.5f) * 0.0016f;
            }
        }

        @Override
        public void addNotify() {
            super.addNotify();
            startTicker(this, 33, () -> {
                phase += 0.010;
                for (int i = 0; i < N; i++) {
                    px[i] += vx[i];
                    py[i] += vy[i];
                    if (px[i] < 0 || px[i] > 1) vx[i] = -vx[i];
                    if (py[i] < 0 || py[i] > 1) vy[i] = -vy[i];
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(w * 0.85f, h * 0.12f, 320f, new float[]{0f, 1f},
                    new Color[]{new Color(34, 211, 238, 90), new Color(34, 211, 238, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(w * 0.08f, h * 0.92f, 360f, new float[]{0f, 1f},
                    new Color[]{new Color(99, 102, 241, 105), new Color(99, 102, 241, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(w * 0.5f, h * 0.45f, 260f, new float[]{0f, 1f},
                    new Color[]{new Color(167, 139, 250, 38), new Color(167, 139, 250, 0)}));
            g2.fillRect(0, 0, w, h);

            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, 10));
            for (int x = 0; x < w; x += 40) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);

            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    double dx = (px[i] - px[j]) * w, dy = (py[i] - py[j]) * h;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d < 130) {
                        g2.setColor(new Color(165, 243, 252, (int) (70 * (1 - d / 130))));
                        g2.drawLine((int) (px[i] * w), (int) (py[i] * h), (int) (px[j] * w), (int) (py[j] * h));
                    }
                }
            }
            for (int i = 0; i < N; i++) {
                int x = (int) (px[i] * w), y = (int) (py[i] * h);
                g2.setColor(new Color(34, 211, 238, 40));
                g2.fillOval(x - 8, y - 8, 16, 16);
                g2.setColor(new Color(207, 250, 254, 210));
                g2.fillOval(x - 2, y - 2, 5, 5);
            }

            int base = (int) (h * 0.84);
            drawEcg(g2, 0, w, base, 40, phase, new Color(34, 211, 238), 7f, 0.16);
            drawEcg(g2, 0, w, base, 40, phase, NEON, 2.4f, 1.0);
            g2.dispose();
        }
    }

    static class PulseBanner extends JPanel {
        private final Color a, b;
        private final int r;
        private double phase;

        PulseBanner(Color a, Color b, int r) {
            this.a = a;
            this.b = b;
            this.r = r;
            setOpaque(false);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            startTicker(this, 33, () -> phase += 0.012);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, r, r));
            g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
            g2.fillRect(0, 0, w, h);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, 9));
            for (int x = 0; x < w; x += 28) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 28) g2.drawLine(0, y, w, y);
            g2.setColor(new Color(255, 255, 255, 26));
            g2.fillOval(w - 240, -90, 340, 340);
            g2.fillOval(w - 110, h - 120, 220, 220);
            g2.fillOval(-70, h - 70, 160, 160);
            drawEcg(g2, 0, w, h - 22, 15, phase, Color.WHITE, 2f, 0.6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class EcgStrip extends JComponent {
        private double phase;

        EcgStrip() {
            setPreferredSize(new Dimension(218, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            startTicker(this, 33, () -> phase += 0.014);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(6, 182, 212, 18));
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            drawEcg(g2, 6, Math.max(10, w - 12), h / 2 + 5, 12, phase, new Color(34, 211, 238), 6f, 0.18);
            drawEcg(g2, 6, Math.max(10, w - 12), h / 2 + 5, 12, phase, NEON, 2f, 1.0);
            g2.dispose();
        }
    }

    static class StatusChip extends JComponent {
        private double phase;

        StatusChip() {
            setPreferredSize(new Dimension(142, 30));
        }

        @Override
        public void addNotify() {
            super.addNotify();
            startTicker(this, 40, () -> phase += 0.12);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(220, 252, 231));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(new Color(134, 239, 172));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            double pulse = 0.5 + 0.5 * Math.sin(phase);
            int cx = 17, cy = h / 2, hr = (int) (6 + 4 * pulse);
            g2.setColor(new Color(16, 185, 129, (int) (40 + 80 * pulse)));
            g2.fillOval(cx - hr, cy - hr, hr * 2, hr * 2);
            g2.setColor(SUCCESS);
            g2.fillOval(cx - 4, cy - 4, 8, 8);
            g2.setColor(new Color(21, 128, 61));
            g2.setFont(f(Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("SYSTEM ONLINE", 32, cy + (fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    static class FadeIn extends JPanel {
        private float alpha = 0f;

        FadeIn(JComponent inner) {
            super(new BorderLayout());
            setOpaque(false);
            add(inner, BorderLayout.CENTER);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            alpha = 0f;
            javax.swing.Timer t = new javax.swing.Timer(16, null);
            t.addActionListener(e -> {
                if (!isDisplayable()) { t.stop(); return; }
                alpha = Math.min(1f, alpha + 0.09f);
                repaint();
                if (alpha >= 1f) t.stop();
            });
            t.start();
        }

        @Override
        public void paint(Graphics g) {
            if (alpha >= 1f) { super.paint(g); return; }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            super.paint(g2);
            g2.dispose();
        }
    }

    static class StatIcon extends JComponent {
        private final String glyph;
        private final Color c;

        StatIcon(String glyph, Color c) {
            this.glyph = glyph;
            this.c = c;
            setPreferredSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int s = Math.min(getWidth(), getHeight());
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            g2.fillOval(0, 0, s, s);
            g2.setPaint(new GradientPaint(0, 0, c, s, s, c.darker()));
            g2.fillOval(5, 5, s - 10, s - 10);
            g2.setColor(WHITE);
            g2.setFont(f(Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(glyph, (s - fm.stringWidth(glyph)) / 2, (s - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static abstract class Animated extends JComponent {
        protected float prog = 0f;

        @Override
        public void addNotify() {
            super.addNotify();
            prog = 0f;
            javax.swing.Timer t = new javax.swing.Timer(16, null);
            t.addActionListener(e -> {
                if (!isDisplayable()) { t.stop(); return; }
                prog = Math.min(1f, prog + 0.04f);
                repaint();
                if (prog >= 1f) t.stop();
            });
            t.start();
        }

        protected float ease() {
            float x = 1f - prog;
            return 1f - x * x * x;
        }
    }

    static class DonutChart extends Animated {
        private final LinkedHashMap<String, Integer> data;

        DonutChart(LinkedHashMap<String, Integer> data) {
            this.data = data;
            setPreferredSize(new Dimension(300, 140));
        }

        private static Color colorOf(String s) {
            switch (s) {
                case "Pending": return WARN;
                case "Confirmed": return ACCENT;
                case "In Consultation": return PRIMARY;
                case "Completed": return SUCCESS;
                default: return DANGER;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int h = getHeight();
            int total = 0;
            for (int v : data.values()) total += v;
            int d = Math.max(70, Math.min(h - 10, 130));
            int x = 6, y = (h - d) / 2;
            float sw = 18f;
            double in = sw / 2;
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.setColor(new Color(241, 245, 249));
            g2.draw(new Arc2D.Double(x + in, y + in, d - sw, d - sw, 0, 360, Arc2D.OPEN));
            if (total > 0) {
                double start = 90;
                for (Map.Entry<String, Integer> en : data.entrySet()) {
                    if (en.getValue() == 0) continue;
                    double extent = -360.0 * en.getValue() / total * ease();
                    g2.setColor(colorOf(en.getKey()));
                    g2.draw(new Arc2D.Double(x + in, y + in, d - sw, d - sw, start, extent, Arc2D.OPEN));
                    start += extent;
                }
            }
            g2.setColor(TEXT);
            g2.setFont(f(Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            String big = String.valueOf(total);
            g2.drawString(big, x + (d - fm.stringWidth(big)) / 2, y + d / 2 + 4);
            g2.setColor(MUTED);
            g2.setFont(f(Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            g2.drawString("Total", x + (d - fm.stringWidth("Total")) / 2, y + d / 2 + 20);

            int lx = x + d + 22, rowH = 22;
            int ly = (h - rowH * data.size()) / 2 + 14;
            for (Map.Entry<String, Integer> en : data.entrySet()) {
                g2.setColor(colorOf(en.getKey()));
                g2.fillOval(lx, ly - 9, 10, 10);
                g2.setColor(TEXT);
                g2.setFont(f(Font.PLAIN, 12));
                g2.drawString(en.getKey(), lx + 16, ly);
                g2.setFont(f(Font.BOLD, 12));
                g2.drawString(String.valueOf(en.getValue()), lx + 120, ly);
                ly += rowH;
            }
            g2.dispose();
        }
    }

    static class BarChart extends Animated {
        private final String[] labels;
        private final int[] vals;

        BarChart(String[] labels, int[] vals) {
            this.labels = labels;
            this.vals = vals;
            setPreferredSize(new Dimension(300, 140));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            int left = 8, right = 8, top = 18, bottom = 24;
            int max = 1, sum = 0;
            for (int v : vals) { max = Math.max(max, v); sum += v; }
            int n = labels.length;
            double slot = (w - left - right) / (double) n;
            int barW = (int) (slot * 0.52);
            int chartH = h - top - bottom;
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(left, h - bottom, w - right, h - bottom);
            for (int i = 0; i < n; i++) {
                int bh = (int) (chartH * (vals[i] / (double) max) * ease());
                int x = (int) (left + slot * i + (slot - barW) / 2);
                int y = h - bottom - bh;
                if (vals[i] > 0) {
                    g2.setPaint(new GradientPaint(0, y, ACCENT, 0, y + Math.max(bh, 1), PRIMARY));
                    g2.fillRoundRect(x, y, barW, Math.max(bh, 4), 8, 8);
                    g2.setColor(TEXT);
                    g2.setFont(f(Font.BOLD, 11));
                    FontMetrics fm = g2.getFontMetrics();
                    String s = String.valueOf(vals[i]);
                    g2.drawString(s, x + (barW - fm.stringWidth(s)) / 2, y - 4);
                } else {
                    g2.setColor(new Color(226, 232, 240));
                    g2.fillRoundRect(x, h - bottom - 4, barW, 4, 4, 4);
                }
                g2.setColor(i == 0 ? PRIMARY : MUTED);
                g2.setFont(f(i == 0 ? Font.BOLD : Font.PLAIN, 11));
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString(labels[i], x + (barW - fm2.stringWidth(labels[i])) / 2, h - 7);
            }
            if (sum == 0) {
                g2.setColor(MUTED);
                g2.setFont(f(Font.PLAIN, 12));
                String s = "No appointments scheduled yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, (w - fm.stringWidth(s)) / 2, top + chartH / 2 - 4);
            }
            g2.dispose();
        }
    }

    static class NavButton extends JButton {
        private boolean active, hover;

        NavButton(String text) {
            super(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(f(Font.PLAIN, 14));
            setForeground(new Color(203, 213, 225));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(11, 18, 11, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean a) {
            active = a;
            setForeground(a ? WHITE : new Color(203, 213, 225));
            setFont(f(a ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            if (active) {
                g2.setColor(new Color(79, 70, 229, 60));
                g2.fillRoundRect(0, 2, getWidth(), getHeight(), 14, 14);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, getWidth(), 0, new Color(37, 99, 235)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 1, 14, 14);
                g2.setColor(new Color(34, 211, 238, 60));
                g2.fillRoundRect(0, 4, 10, getHeight() - 8, 8, 8);
                g2.setColor(NEON);
                g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
            } else if (hover) {
                g2.setColor(SIDEBAR_SOFT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===================== STYLED DIALOGS =====================

    static class Shell extends JPanel {
        Shell() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 16, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth() - 20, h = getHeight() - 26;
            for (int i = 1; i <= 7; i++) {
                g2.setColor(new Color(15, 23, 42, 9));
                g2.fillRoundRect(10 - i, 10 - i + 5, w + 2 * i, h + 2 * i, 22 + i, 22 + i);
            }
            g2.setColor(WHITE);
            g2.fillRoundRect(10, 10, w, h, 22, 22);
            g2.dispose();
        }
    }

    static JPanel dlgHeader(JDialog d, String title, int type, boolean closeButton) {
        Color c1, c2;
        String glyph;
        switch (type) {
            case JOptionPane.ERROR_MESSAGE -> { c1 = DANGER; c2 = new Color(190, 24, 93); glyph = "✕"; }
            case JOptionPane.WARNING_MESSAGE -> { c1 = WARN; c2 = new Color(249, 115, 22); glyph = "!"; }
            case JOptionPane.QUESTION_MESSAGE -> { c1 = PRIMARY; c2 = ACCENT; glyph = "?"; }
            default -> { c1 = PRIMARY; c2 = ACCENT; glyph = "i"; }
        }
        final Color fc1 = c1, fc2 = c2;
        final String fg = glyph;

        JPanel header = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() + 30, 22, 22));
                g2.setPaint(new GradientPaint(0, 0, fc1, getWidth(), getHeight(), fc2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 34));
                g2.fillOval(getWidth() - 100, -60, 170, 170);
                g2.fillOval(getWidth() - 210, 25, 90, 90);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 22, 16, 14));

        JComponent badge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setColor(new Color(255, 255, 255, 70));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(WHITE);
                g2.setFont(f(Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fg, (getWidth() - fm.stringWidth(fg)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(36, 36));

        JLabel t = new JLabel(title);
        t.setFont(f(Font.BOLD, 17));
        t.setForeground(WHITE);

        header.add(badge, BorderLayout.WEST);
        header.add(t, BorderLayout.CENTER);

        if (closeButton) {
            JButton x = new JButton("✕");
            x.setContentAreaFilled(false);
            x.setBorderPainted(false);
            x.setFocusPainted(false);
            x.setForeground(WHITE);
            x.setFont(f(Font.BOLD, 15));
            x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            x.addActionListener(e -> d.dispose());
            header.add(x, BorderLayout.EAST);
        }

        final Point[] start = new Point[1];
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { start[0] = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (start[0] == null) return;
                Point p = d.getLocation();
                d.setLocation(p.x + e.getX() - start[0].x, p.y + e.getY() - start[0].y);
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        return header;
    }

    private static Window ownerOf(Component c) {
        if (c == null) return null;
        if (c instanceof Window) return (Window) c;
        return SwingUtilities.getWindowAncestor(c);
    }

    private static JTextField firstField(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JTextField) return (JTextField) k;
            if (k instanceof Container) {
                JTextField r = firstField((Container) k);
                if (r != null) return r;
            }
        }
        return null;
    }

    static class Dlg extends JDialog {
        boolean ok;

        Dlg(Component parent, String title, int type, JComponent body, String okText, String cancelText) {
            super(ownerOf(parent), Dialog.ModalityType.APPLICATION_MODAL);
            setUndecorated(true);
            setBackground(new Color(0, 0, 0, 0));

            Shell shell = new Shell();
            shell.add(dlgHeader(this, title, type, false), BorderLayout.NORTH);

            JPanel bodyWrap = new JPanel(new BorderLayout());
            bodyWrap.setOpaque(false);
            bodyWrap.setBorder(new EmptyBorder(20, 24, 8, 24));
            bodyWrap.add(body, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttons.setOpaque(false);
            buttons.setBorder(new EmptyBorder(10, 20, 18, 20));
            RoundButton okBtn = new RoundButton(okText, Kind.PRIMARY);
            okBtn.addActionListener(e -> { ok = true; dispose(); });
            if (cancelText != null) {
                RoundButton cancelBtn = new RoundButton(cancelText, Kind.SOFT);
                cancelBtn.addActionListener(e -> { ok = false; dispose(); });
                buttons.add(cancelBtn);
            }
            buttons.add(okBtn);

            shell.add(bodyWrap, BorderLayout.CENTER);
            shell.add(buttons, BorderLayout.SOUTH);
            setContentPane(shell);
            getRootPane().setDefaultButton(okBtn);
            getRootPane().registerKeyboardAction(e -> { ok = false; dispose(); },
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    JTextField tf = firstField(body);
                    if (tf != null) tf.requestFocusInWindow();
                }
            });
            pack();
            Dimension sc = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension sz = getSize();
            setSize(Math.min(sz.width, sc.width - 20), Math.min(sz.height, sc.height - 40));
            setLocationRelativeTo(parent);
        }
    }

    static boolean showDlg(Component parent, String title, int type, JComponent body, String ok, String cancel) {
        Dlg d = new Dlg(parent, title, type, body, ok, cancel);
        d.setVisible(true);
        return d.ok;
    }

    private static JComponent textBody(String text) {
        JTextArea ta = new JTextArea(text);
        boolean mono = text.contains("OFFICIAL RECEIPT") || text.startsWith("Insufficient payment");
        ta.setFont(mono ? new Font(Font.MONOSPACED, Font.PLAIN, 13) : f(Font.PLAIN, 14));
        ta.setForeground(TEXT);
        ta.setOpaque(false);
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(null);
        ta.setSize(new Dimension(380, Short.MAX_VALUE));
        ta.setPreferredSize(new Dimension(380, ta.getPreferredSize().height));
        return ta;
    }

    static boolean ask(Component parent, String title, String text, int type) {
        return showDlg(parent, title, type, textBody(text), "Yes", "No");
    }

    static String prompt(Component parent, String title, String question, String initial) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(question, Font.PLAIN, 14, TEXT));
        p.add(Box.createVerticalStrut(10));
        RoundField field = new RoundField(initial);
        field.setPreferredSize(new Dimension(380, 42));
        p.add(field);
        return showDlg(parent, title, JOptionPane.QUESTION_MESSAGE, p, "OK", "Cancel") ? field.getText() : null;
    }

    static String choose(Component parent, String title, String question, String[] options, String selected) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(question, Font.PLAIN, 14, TEXT));
        p.add(Box.createVerticalStrut(10));
        JComboBox<String> cb = new JComboBox<>(options);
        styleCombo(cb);
        cb.setSelectedItem(selected);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setPreferredSize(new Dimension(380, 40));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(cb);
        return showDlg(parent, title, JOptionPane.QUESTION_MESSAGE, p, "OK", "Cancel")
                ? cb.getSelectedItem().toString() : null;
    }

    // ===================== SMALL HELPERS =====================

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(f(style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static void msg(Component parent, String title, String text, int type) {
        showDlg(parent, title, type, textBody(text), "OK", null);
    }

    private void info(String text) { msg(frame, APP_NAME, text, JOptionPane.INFORMATION_MESSAGE); }

    private void bad(String text) { msg(frame, "Please check", text, JOptionPane.WARNING_MESSAGE); }

    private static String peso(double v) { return String.format(Locale.US, "₱%,.2f", v); }

    private static String today() { return LocalDate.now().toString(); }

    private static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    private static void styleCombo(JComboBox<?> c) {
        c.setFont(f(Font.PLAIN, 14));
        c.setBackground(WHITE);
        c.setPreferredSize(new Dimension(240, 40));
    }

    private JPanel form(Object... pairs) {
        int rows = pairs.length / 2;
        JPanel p = new JPanel(new GridLayout(rows, 2, 14, 10));
        p.setBackground(WHITE);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        for (int i = 0; i < pairs.length; i += 2) {
            JLabel l = new JLabel((String) pairs[i]);
            l.setFont(f(Font.BOLD, 13));
            l.setForeground(TEXT);
            p.add(l);
            Component c = (Component) pairs[i + 1];
            if (c instanceof JComboBox) styleCombo((JComboBox<?>) c);
            p.add(c);
        }
        p.setPreferredSize(new Dimension(500, rows * 52));
        return p;
    }

    private boolean confirm(String title, JComponent content) {
        return showDlg(frame, title, JOptionPane.INFORMATION_MESSAGE, content, "Save", "Cancel");
    }

    private static String patientName(String id) {
        Patient p = findPatient(id);
        return p == null || p.name.isBlank() ? id : p.name;
    }

    private static Patient findPatient(String id) {
        for (Patient p : patients) if (p.id.equalsIgnoreCase(id)) return p;
        return null;
    }

    private static Patient findPatientByUsername(String username) {
        if (username == null || username.isEmpty()) return null;
        for (Patient p : patients) if (p.username.equalsIgnoreCase(username)) return p;
        return null;
    }

    private static boolean patientOwns(String patientId) {
        Patient p = findPatient(patientId);
        return p != null && !p.username.isEmpty() && p.username.equalsIgnoreCase(current.username);
    }

    private static Medicine findMedicine(String name) {
        for (Medicine m : medicines) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    private static int nextPatientNumber() {
        int highest = 0;
        for (Patient p : patients) {
            try { highest = Math.max(highest, Integer.parseInt(p.id.substring(1))); }
            catch (NumberFormatException | StringIndexOutOfBoundsException ignored) { }
        }
        return highest + 1;
    }

    private static Set<String> myPatientIds() {
        Set<String> s = new HashSet<>();
        for (Appointment a : appointments) {
            if (a.doctor.equalsIgnoreCase(current.displayName) && !a.status.equals("Cancelled")) s.add(a.patient);
        }
        return s;
    }

    private static String[] patientChoices() {
        List<String> out = new ArrayList<>();
        for (Patient p : patients) if (!p.name.isBlank()) out.add(p.id + " - " + p.name);
        return out.toArray(new String[0]);
    }

    private static String[] patientChoicesForCurrent() {
        if (current.role != Role.DOCTOR) return patientChoices();
        Set<String> mine = myPatientIds();
        List<String> out = new ArrayList<>();
        for (Patient p : patients) if (!p.name.isBlank() && mine.contains(p.id)) out.add(p.id + " - " + p.name);
        return out.toArray(new String[0]);
    }

    private static List<Account> activeDoctors() {
        List<Account> out = new ArrayList<>();
        for (String u : BOOKABLE_DOCTORS) {
            Account a = accounts.get(u);
            if (a != null && a.active) out.add(a);
        }
        return out;
    }

    private static Account findAccountByLogin(String in) {
        String k = in.trim();
        Account a = accounts.get(k.toLowerCase());
        if (a != null) return a;
        String phone = OtpService.normalizePhone(k);
        for (Account x : accounts.values()) {
            if (!x.email.isEmpty() && x.email.equalsIgnoreCase(k)) return x;
            if (phone != null && !x.phone.isEmpty() && x.phone.equals(phone)) return x;
        }
        Patient p = findPatient(k);
        if (p != null && !p.username.isEmpty()) return accounts.get(p.username.toLowerCase());
        return null;
    }

    private static boolean contactTaken(String email, String phone) {
        for (Account x : accounts.values()) {
            if (email != null && email.equalsIgnoreCase(x.email)) return true;
            if (phone != null && phone.equals(x.phone)) return true;
        }
        return false;
    }

    private static boolean ensureDisplayName(Component parent, Account acc) {
        while (acc.displayName == null || acc.displayName.isBlank()) {
            String in = prompt(parent, "Set your display name",
                    "What name would you like to appear in the system?", "");
            if (in == null) return false;
            String n = in.trim();
            if (n.length() >= 2 && n.length() <= 40) {
                acc.displayName = n;
                Patient p = findPatientByUsername(acc.username);
                if (p != null) p.name = n;
            } else {
                msg(parent, "Display name", "Please enter 2 to 40 characters.", JOptionPane.WARNING_MESSAGE);
            }
        }
        return true;
    }

    // ===================== LOGIN =====================

    private void showLogin() {
        JFrame login = new JFrame(APP_NAME + " | " + "Healthcare System");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension sc = Toolkit.getDefaultToolkit().getScreenSize();
        login.setMinimumSize(new Dimension(900, 620));
        login.setSize(Math.min(1060, sc.width - 40), Math.min(680, sc.height - 60));
        login.setLocationRelativeTo(null);
        login.setResizable(true);

        JPanel root = new JPanel(new GridLayout(1, 2));

        TechBackdrop left = new TechBackdrop(new Color(6, 11, 32), new Color(8, 80, 125));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(52, 44, 32, 30));
        left.add(brandRow(54, 44));
        left.add(Box.createVerticalStrut(12));
        left.add(new TextLine(APP_SUB, f(Font.BOLD, 14), 0.30f, new Color(165, 243, 252), null));
        left.add(Box.createVerticalStrut(30));
        left.add(new AccentBar());
        left.add(Box.createVerticalStrut(18));
        left.add(new TextLine(SLOGAN_1, f(Font.BOLD, 27), 0f, WHITE, null));
        left.add(Box.createVerticalStrut(2));
        left.add(new TextLine(SLOGAN_2, f(Font.BOLD, 27), 0f, NEON, VIOLET));
        left.add(Box.createVerticalStrut(26));
        for (String s : new String[]{"●  Book appointments online",
                "●  View prescriptions & medical records",
                "●  Track your bills and payments",
                "●  Secure sign-up with a one-time code"}) {
            left.add(label(s, Font.PLAIN, 14, new Color(224, 242, 254)));
            left.add(Box.createVerticalStrut(10));
        }
        left.add(Box.createVerticalGlue());
        left.add(new TextLine("POWERED BY", f(Font.BOLD, 11), 0.32f, new Color(165, 243, 252), null));
        left.add(Box.createVerticalStrut(10));
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pills.setOpaque(false);
        pills.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String d : POWERED_BY) pills.add(new Pill(d));
        pills.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        left.add(pills);

        JPanel right = new JPanel();
        right.setBackground(WHITE);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(70, 54, 36, 54));

        RoundField user = new RoundField("");
        RoundPassword pass = new RoundPassword();
        RoundButton signIn = new RoundButton("SIGN IN", Kind.PRIMARY);
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        right.add(label("Welcome back", Font.BOLD, 32, TEXT));
        right.add(Box.createVerticalStrut(6));
        right.add(label("Sign in to your " + APP_NAME + " account", Font.PLAIN, 14, MUTED));
        right.add(Box.createVerticalStrut(30));
        right.add(label("Email, mobile number, username or Patient ID", Font.BOLD, 13, TEXT));
        right.add(Box.createVerticalStrut(7));
        right.add(user);
        right.add(Box.createVerticalStrut(18));
        right.add(label("Password", Font.BOLD, 13, TEXT));
        right.add(Box.createVerticalStrut(7));
        right.add(pass);
        right.add(Box.createVerticalStrut(26));
        right.add(signIn);
        right.add(Box.createVerticalStrut(22));

        JLabel signup = label("<html>New patient? <b style='color:#4F46E5'>Create an account</b></html>",
                Font.PLAIN, 14, MUTED);
        signup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        right.add(signup);
        right.add(Box.createVerticalStrut(14));
        right.add(label("Forgot your password? Please contact the receptionist.", Font.PLAIN, 12, MUTED));
        right.add(Box.createVerticalGlue());
        right.add(label(SLOGAN_1 + " " + SLOGAN_2, Font.BOLD, 12, PRIMARY));

        signIn.addActionListener(e -> {
            String input = user.getText().trim();
            String password = new String(pass.getPassword());
            if (input.isEmpty() || password.isEmpty()) {
                msg(login, "Missing information", "Please enter your login and password.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Account account = findAccountByLogin(input);
            if (account == null || !account.active || !account.check(password)) {
                msg(login, "Login failed", "Invalid login details or inactive account.", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ensureDisplayName(login, account)) return;
            current = account;
            login.dispose();
            showMain();
        });
        pass.addActionListener(e -> signIn.doClick());
        user.addActionListener(e -> pass.requestFocusInWindow());

        signup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Account created = runSignUp(login);
                if (created != null) {
                    current = created;
                    login.dispose();
                    showMain();
                }
            }
        });

        root.add(left);
        root.add(right);
        login.setContentPane(root);
        login.setVisible(true);
    }

    // ===================== PATIENT SIGN-UP =====================

    private JPanel step(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(title, Font.BOLD, 24, TEXT));
        p.add(Box.createVerticalStrut(6));
        p.add(label("<html><div style='width:340px'>" + subtitle + "</div></html>", Font.PLAIN, 13, MUTED));
        p.add(Box.createVerticalStrut(22));
        return p;
    }

    private void sendOtp(Component dlg, JButton btn, String shownText, String target,
                         boolean email, java.util.function.Consumer<String> onSuccess) {
        final String oldText = btn.getText();
        btn.setEnabled(false);
        btn.setText("Sending...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return otp.send(target, email);
            }

            @Override
            protected void done() {
                btn.setEnabled(true);
                btn.setText(oldText);
                try {
                    String demo = get();
                    onSuccess.accept(demo);
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    msg(dlg, "Could not send code", c.getMessage(), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private Account runSignUp(JFrame owner) {
        JDialog d = new JDialog(owner, true);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));
        Dimension sc = Toolkit.getDefaultToolkit().getScreenSize();
        d.setSize(540, Math.min(650, sc.height - 50));
        d.setLocationRelativeTo(owner);
        d.setResizable(false);

        CardLayout cl = new CardLayout();
        JPanel cards = new JPanel(cl);
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(22, 40, 20, 40));

        final boolean[] useEmail = {true};
        final String[] target = {null};
        final Account[] created = {null};
        final boolean[] finished = {false};

        JPanel s1 = step("Create your account",
                "Sign up with your Gmail or mobile number. We will send a one-time code to verify it.");
        RoundButton optEmail = new RoundButton("Gmail", Kind.PRIMARY);
        RoundButton optPhone = new RoundButton("Mobile number", Kind.SOFT);
        JPanel toggle = new JPanel(new GridLayout(1, 2, 10, 0));
        toggle.setOpaque(false);
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        toggle.add(optEmail);
        toggle.add(optPhone);
        JLabel contactLabel = label("Gmail address", Font.BOLD, 13, TEXT);
        RoundField contact = new RoundField("");
        RoundButton send = new RoundButton("Send verification code", Kind.PRIMARY);
        send.setAlignmentX(Component.LEFT_ALIGNMENT);
        send.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        s1.add(toggle);
        s1.add(Box.createVerticalStrut(22));
        s1.add(contactLabel);
        s1.add(Box.createVerticalStrut(7));
        s1.add(contact);
        s1.add(Box.createVerticalStrut(24));
        s1.add(send);
        optEmail.addActionListener(e -> {
            useEmail[0] = true;
            optEmail.setKind(Kind.PRIMARY);
            optPhone.setKind(Kind.SOFT);
            contactLabel.setText("Gmail address");
            contact.setText("");
        });
        optPhone.addActionListener(e -> {
            useEmail[0] = false;
            optPhone.setKind(Kind.PRIMARY);
            optEmail.setKind(Kind.SOFT);
            contactLabel.setText("Mobile number (09XX XXX XXXX)");
            contact.setText("");
        });

        JPanel s2 = step("Enter the code", "");
        JLabel otpInfo = (JLabel) s2.getComponent(2);
        RoundField code = new RoundField("");
        code.setFont(f(Font.BOLD, 22));
        RoundButton verify = new RoundButton("Verify", Kind.PRIMARY);
        verify.setAlignmentX(Component.LEFT_ALIGNMENT);
        verify.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        RoundButton resend = new RoundButton("Resend code", Kind.SOFT);
        resend.setAlignmentX(Component.LEFT_ALIGNMENT);
        resend.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        RoundPanel demoBox = new RoundPanel(new Color(254, 243, 199), 14, 12, false);
        demoBox.setLayout(new BorderLayout());
        demoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        demoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel demoLbl = new JLabel();
        demoLbl.setFont(f(Font.PLAIN, 12));
        demoLbl.setForeground(new Color(120, 53, 15));
        demoBox.add(demoLbl, BorderLayout.CENTER);
        demoBox.setVisible(false);
        s2.add(demoBox);
        s2.add(Box.createVerticalStrut(16));
        final java.util.function.Consumer<String> showDemo = demo -> {
            if (demo == null) {
                demoBox.setVisible(false);
            } else {
                demoLbl.setText("<html>DEMO MODE (SMS/email not configured)<br>Your code: "
                        + "<b style='font-size:22px;letter-spacing:4px'>" + demo + "</b></html>");
                demoBox.setVisible(true);
            }
        };
        s2.add(label("6-digit code", Font.BOLD, 13, TEXT));
        s2.add(Box.createVerticalStrut(7));
        s2.add(code);
        s2.add(Box.createVerticalStrut(22));
        s2.add(verify);
        s2.add(Box.createVerticalStrut(10));
        s2.add(resend);

        JPanel s3 = step("Secure your account",
                "Verified! Create a password you will use to sign in next time (at least 8 characters with letters and numbers).");
        RoundPassword p1 = new RoundPassword();
        RoundPassword p2 = new RoundPassword();
        RoundButton create = new RoundButton("Create account", Kind.PRIMARY);
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        s3.add(label("Password", Font.BOLD, 13, TEXT));
        s3.add(Box.createVerticalStrut(7));
        s3.add(p1);
        s3.add(Box.createVerticalStrut(16));
        s3.add(label("Confirm password", Font.BOLD, 13, TEXT));
        s3.add(Box.createVerticalStrut(7));
        s3.add(p2);
        s3.add(Box.createVerticalStrut(24));
        s3.add(create);

        JPanel s4 = step("What should we call you?",
                "Choose the display name or screen name that will appear in the system when you sign in. You can change it later in My Profile.");
        RoundField nameField = new RoundField("");
        RoundButton finish = new RoundButton("Finish & enter portal", Kind.PRIMARY);
        finish.setAlignmentX(Component.LEFT_ALIGNMENT);
        finish.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        s4.add(label("Display name", Font.BOLD, 13, TEXT));
        s4.add(Box.createVerticalStrut(7));
        s4.add(nameField);
        s4.add(Box.createVerticalStrut(24));
        s4.add(finish);

        cards.add(s1, "1");
        cards.add(s2, "2");
        cards.add(s3, "3");
        cards.add(s4, "4");
        Shell shell = new Shell();
        shell.add(dlgHeader(d, "Create your " + APP_NAME + " account", JOptionPane.INFORMATION_MESSAGE, true), BorderLayout.NORTH);
        shell.add(cards, BorderLayout.CENTER);
        d.setContentPane(shell);

        send.addActionListener(e -> {
            String raw = contact.getText().trim();
            String key;
            if (useEmail[0]) {
                if (!Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE).matcher(raw).matches()) {
                    msg(d, "Invalid Gmail", "Please enter a valid Gmail address (example@gmail.com).", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                key = raw.toLowerCase();
                if (contactTaken(key, null)) {
                    msg(d, "Already registered", "This Gmail is already registered. Please sign in instead.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                key = OtpService.normalizePhone(raw);
                if (key == null) {
                    msg(d, "Invalid number", "Please enter a valid PH mobile number (09XXXXXXXXX or +639XXXXXXXXX).", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (contactTaken(null, key)) {
                    msg(d, "Already registered", "This number is already registered. Please sign in instead.", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            sendOtp(d, send, raw, key, useEmail[0], demo -> {
                target[0] = key;
                showDemo.accept(demo);
                otpInfo.setText("<html><div style='width:340px'>We sent a 6-digit code to <b>" + key
                        + "</b>. It expires in 5 minutes.</div></html>");
                code.setText("");
                cl.show(cards, "2");
                code.requestFocusInWindow();
            });
        });

        resend.addActionListener(e -> sendOtp(d, resend, target[0], target[0], useEmail[0],
                demo -> {
            showDemo.accept(demo);
            if (demo == null) msg(d, "Code sent", "A new code was sent to " + target[0] + ".", JOptionPane.INFORMATION_MESSAGE);
        }));

        verify.addActionListener(e -> {
            String err = otp.verify(target[0], code.getText());
            if (err != null) {
                msg(d, "Verification", err, JOptionPane.WARNING_MESSAGE);
                return;
            }
            cl.show(cards, "3");
        });
        code.addActionListener(e -> verify.doClick());

        create.addActionListener(e -> {
            String a = new String(p1.getPassword());
            String b = new String(p2.getPassword());
            if (a.length() < 8 || !a.matches(".*[A-Za-z].*") || !a.matches(".*\\d.*")) {
                msg(d, "Weak password", "Use at least 8 characters with letters and numbers.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!a.equals(b)) {
                msg(d, "Passwords do not match", "Please re-enter your password.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = "P" + String.format("%04d", nextPatientNumber());
            String key = id.toLowerCase();
            Account acc = new Account(key, a, "", Role.PATIENT);
            if (useEmail[0]) acc.email = target[0]; else acc.phone = target[0];
            accounts.put(key, acc);
            patients.add(new Patient(id, "", 0, "", target[0], "", "", key));
            created[0] = acc;
            cl.show(cards, "4");
            nameField.requestFocusInWindow();
        });

        finish.addActionListener(e -> {
            String n = nameField.getText().trim();
            if (n.length() < 2 || n.length() > 40) {
                msg(d, "Display name", "Please enter 2 to 40 characters.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            created[0].displayName = n;
            Patient p = findPatientByUsername(created[0].username);
            if (p != null) p.name = n;
            finished[0] = true;
            d.dispose();
        });
        nameField.addActionListener(e -> finish.doClick());

        d.setVisible(true);
        return finished[0] ? created[0] : null;
    }

    // ===================== MAIN WINDOW =====================

    private void showMain() {
        frame = new JFrame(APP_NAME + " | Healthcare System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension sc = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setMinimumSize(new Dimension(Math.min(980, sc.width - 20), Math.min(640, sc.height - 40)));
        frame.setSize(Math.min(1320, sc.width - 40), Math.min(820, sc.height - 60));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        navs.clear();

        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setPaint(new GradientPaint(0, 0, new Color(6, 11, 28), 0, getHeight(), new Color(14, 30, 66)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new RadialGradientPaint(getWidth() / 2f, getHeight() + 30f, 280f, new float[]{0f, 1f},
                        new Color[]{new Color(6, 182, 212, 80), new Color(6, 182, 212, 0)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(6, 182, 212, 45));
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(24, 16, 16, 16));

        sidebar.add(brandRow(36, 24));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(indent(new TextLine(APP_SUB, f(Font.BOLD, 10), 0.24f, NEON, null), 2, 10));
        sidebar.add(new EcgStrip());
        sidebar.add(Box.createVerticalStrut(14));

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        for (String item : menuFor(current.role)) {
            NavButton b = new NavButton(navIcon(item) + "   " + item);
            b.addActionListener(e -> showPage(item));
            navs.put(item, b);
            navPanel.add(b);
            navPanel.add(Box.createVerticalStrut(4));
        }
        JScrollPane navScroll = new JScrollPane(navPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setBorder(null);
        navScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        navScroll.getVerticalScrollBar().setUnitIncrement(16);
        navScroll.setMinimumSize(new Dimension(50, 60));
        sidebar.add(navScroll);
        sidebar.add(Box.createVerticalStrut(8));

        JPanel powered = new JPanel();
        powered.setOpaque(false);
        powered.setLayout(new BoxLayout(powered, BoxLayout.Y_AXIS));
        powered.setAlignmentX(Component.LEFT_ALIGNMENT);
        powered.setBorder(new EmptyBorder(0, 6, 10, 0));
        powered.add(new TextLine("POWERED BY", f(Font.BOLD, 9), 0.28f, NEON, null));
        powered.add(Box.createVerticalStrut(3));
        powered.add(label("<html>" + String.join("<br>", POWERED_BY) + "</html>", Font.BOLD, 10, new Color(148, 163, 184)));
        sidebar.add(powered);

        RoundPanel userCard = new RoundPanel(SIDEBAR_SOFT, 16, 12, false);
        userCard.setLayout(new BorderLayout(10, 0));
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        sideName = label(current.displayName, Font.BOLD, 13, WHITE);
        who.add(sideName);
        who.add(label(roleTitle(current.role), Font.PLAIN, 11, new Color(148, 163, 184)));
        userCard.add(new Avatar(current.displayName, 38), BorderLayout.WEST);
        userCard.add(who, BorderLayout.CENTER);
        sidebar.add(userCard);
        sidebar.add(Box.createVerticalStrut(10));

        RoundButton logout = new RoundButton("↪  Log out", Kind.GHOST_DANGER);
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        logout.addActionListener(e -> {
            boolean yes = ask(frame, "Confirm Logout",
                    "Are you sure you want to log out?\n\nYou will need to sign in again to access the system.",
                    JOptionPane.QUESTION_MESSAGE);
            if (yes) {
                frame.dispose();
                current = null;
                showLogin();
            }
        });
        sidebar.add(logout);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG);

        JPanel top = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, getWidth(), 0, ACCENT));
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        top.setBackground(WHITE);
        top.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(12, 26, 12, 26)));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        pageTitle = label("Dashboard", Font.BOLD, 22, TEXT);
        titles.add(pageTitle);
        titles.add(label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                Font.PLAIN, 12, MUTED));

        JPanel who2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        who2.setOpaque(false);
        identity = new JLabel("Welcome " + roleTitle(current.role) + " " + current.displayName + "!");
        identity.setFont(f(Font.BOLD, 13));
        identity.setForeground(PRIMARY);
        JLabel clock = new JLabel();
        clock.setFont(f(Font.BOLD, 13));
        clock.setForeground(MUTED);
        final DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern("hh:mm:ss a");
        clock.setText(LocalDateTime.now().format(clockFmt));
        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, null);
        clockTimer.addActionListener(e -> {
            if (!clock.isDisplayable()) { clockTimer.stop(); return; }
            clock.setText(LocalDateTime.now().format(clockFmt));
        });
        clockTimer.start();
        StatusChip chip = new StatusChip();
        who2.add(chip);
        who2.add(clock);
        who2.add(identity);
        who2.add(new Avatar(current.displayName, 38));

        top.add(titles, BorderLayout.WEST);
        top.add(who2, BorderLayout.EAST);

        cardPanel.setBackground(BG);
        right.add(top, BorderLayout.NORTH);
        right.add(cardPanel, BorderLayout.CENTER);

        right.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = right.getWidth();
                chip.setVisible(w >= 1000);
                clock.setVisible(w >= 880);
                identity.setVisible(w >= 740);
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int sw = frame.getWidth() < 1150 ? 214 : 250;
                if (sidebar.getPreferredSize().width != sw) {
                    sidebar.setPreferredSize(new Dimension(sw, 0));
                    sidebar.revalidate();
                }
            }
        });

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(right, BorderLayout.CENTER);

        showPage("Dashboard");
        frame.setVisible(true);
    }

    private String[] menuFor(Role role) {
        return switch (role) {
            case ADMIN -> new String[]{"Dashboard", "Patients", "Doctors & Staff", "Appointments",
                    "Admissions", "Medical Records", "Prescriptions", "Billing", "Pharmacy", "Accounts"};
            case DOCTOR -> new String[]{"Dashboard", "My Patients", "Appointments", "Current Queue",
                    "Medical Records", "Prescriptions", "Pharmacy"};
            case RECEPTIONIST -> new String[]{"Dashboard", "Register Patient", "Patient Directory",
                    "Appointments", "Admissions", "Billing"};
            case PATIENT -> new String[]{"Dashboard", "My Profile", "Book Appointment", "My Appointments",
                    "Medical Records", "My Prescriptions", "My Billing"};
        };
    }

    private static String roleTitle(Role role) {
        return switch (role) {
            case ADMIN -> "Admin";
            case DOCTOR -> "Doctor";
            case RECEPTIONIST -> "Receptionist";
            case PATIENT -> "Patient";
        };
    }

    // ===================== ROUTING =====================

    private void refresh() {
        showPage(currentPage);
    }

    private void showPage(String page) {
        final String nav = page;
        boolean openBooking = page.equals("Book Appointment");
        if (openBooking) page = "My Appointments";
        currentPage = page;
        pageTitle.setText(nav);
        navs.forEach((k, v) -> v.setActive(k.equals(nav)));

        JPanel panel;
        switch (page) {
            case "Dashboard" -> panel = dashboard();
            case "Patients", "Patient Directory", "My Patients" -> panel = patientsPage();
            case "Register Patient" -> panel = registerPatientPage();
            case "Doctors & Staff" -> panel = staffPage(false);
            case "Accounts" -> panel = staffPage(true);
            case "Appointments", "My Appointments", "Current Queue" -> panel = appointmentsPage(page);
            case "Admissions" -> panel = admissionsPage();
            case "Medical Records" -> panel = recordsPage();
            case "Billing", "My Billing" -> panel = billingPage();
            case "Pharmacy" -> panel = pharmacyPage();
            case "Prescriptions", "My Prescriptions" -> panel = prescriptionsPage();
            case "My Profile" -> panel = profilePage();
            default -> panel = emptyPage("Page not available.");
        }

        cardPanel.removeAll();
        cardPanel.add(new FadeIn(panel), "active");
        cardLayout.show(cardPanel, "active");
        cardPanel.revalidate();
        cardPanel.repaint();

        if (openBooking) SwingUtilities.invokeLater(() -> bookAppointment(true));
    }

    // ===================== DASHBOARD =====================

    private List<Appointment> visibleAppointments() {
        List<Appointment> out = new ArrayList<>();
        for (Appointment a : appointments) {
            if (current.role == Role.PATIENT && !patientOwns(a.patient)) continue;
            if (current.role == Role.DOCTOR && !a.doctor.equalsIgnoreCase(current.displayName)) continue;
            out.add(a);
        }
        out.sort(Comparator.comparing((Appointment a) -> a.date).thenComparing(a -> a.time));
        return out;
    }

    private static boolean openStatus(String s) {
        return !s.equals("Completed") && !s.equals("Cancelled");
    }

    private JPanel dashboard() {
        JPanel panel = pageBase();
        panel.setLayout(new BorderLayout(0, 18));

        int hour = LocalDateTime.now().getHour();
        String greet = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";

        PulseBanner banner = new PulseBanner(new Color(30, 27, 90), new Color(8, 145, 178), 22);
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBorder(new EmptyBorder(24, 30, 42, 30));
        banner.add(label(greet + ", " + current.displayName + "!", Font.BOLD, 26, WHITE));
        banner.add(Box.createVerticalStrut(8));
        String sub = switch (current.role) {
            case PATIENT -> "Your appointments, prescriptions, records and bills are all here.";
            case DOCTOR -> "Here is your clinic overview for today.";
            case RECEPTIONIST -> "Manage walk-ins, appointments, admissions and payments.";
            case ADMIN -> "Hospital-wide overview and system management.";
        };
        banner.add(label(sub, Font.PLAIN, 14, new Color(224, 242, 254)));
        banner.add(Box.createVerticalStrut(12));
        banner.add(new TextLine((SLOGAN_1 + " " + SLOGAN_2).toUpperCase(), f(Font.BOLD, 11), 0.16f, NEON, null));

        String t = today();
        long upcoming = visibleAppointments().stream().filter(a -> a.date.compareTo(t) >= 0 && openStatus(a.status)).count();
        long todayAppts = visibleAppointments().stream().filter(a -> a.date.equals(t)).count();
        long queue = visibleAppointments().stream().filter(a -> a.date.equals(t) && openStatus(a.status)).count();
        double revenue = invoices.stream().mapToDouble(i -> i.total).sum();

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        switch (current.role) {
            case PATIENT -> {
                long rx = prescriptions.stream().filter(p -> patientOwns(p.patient)).count();
                long rec = records.stream().filter(r -> patientOwns(r.patient)).count();
                double paid = invoices.stream().filter(i -> patientOwns(i.patient)).mapToDouble(i -> i.total).sum();
                cards.add(statCard("Upcoming Appointments", String.valueOf(upcoming), PRIMARY));
                cards.add(statCard("Prescriptions", String.valueOf(rx), ACCENT));
                cards.add(statCard("Medical Records", String.valueOf(rec), SUCCESS));
                cards.add(statCard("Total Paid", peso(paid), WARN));
            }
            case DOCTOR -> {
                long rx = prescriptions.stream().filter(p -> p.doctor.equalsIgnoreCase(current.displayName)).count();
                cards.add(statCard("Today's Appointments", String.valueOf(todayAppts), PRIMARY));
                cards.add(statCard("Waiting in Queue", String.valueOf(queue), WARN));
                cards.add(statCard("Prescriptions Issued", String.valueOf(rx), ACCENT));
                cards.add(statCard("Upcoming", String.valueOf(upcoming), SUCCESS));
            }
            default -> {
                cards.add(statCard("Registered Patients", String.valueOf(patients.size()), PRIMARY));
                cards.add(statCard("Today's Appointments", String.valueOf(todayAppts), ACCENT));
                cards.add(statCard("Admitted Now", String.valueOf(activeAdmissions()), WARN));
                cards.add(statCard("Revenue Collected", peso(revenue), SUCCESS));
            }
        }

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(banner);
        north.add(Box.createVerticalStrut(16));
        north.add(cards);

        DefaultTableModel model = tableModel(new String[]{"Date", "Time", "Patient", "Doctor", "Reason", "Status"});
        int shown = 0;
        for (Appointment a : visibleAppointments()) {
            if (a.date.compareTo(t) < 0 || !openStatus(a.status)) continue;
            model.addRow(new Object[]{a.date, a.time, patientName(a.patient), a.doctor, a.reason, a.status});
            if (++shown >= 8) break;
        }
        JTable table = styledTable(model);
        RoundPanel card = new RoundPanel(WHITE, 18, 14, true);
        card.setLayout(new BorderLayout(0, 10));
        JLabel h = label("Upcoming appointments", Font.BOLD, 16, TEXT);
        h.setBorder(new EmptyBorder(4, 6, 0, 0));
        card.add(h, BorderLayout.NORTH);
        card.add(scroll(table), BorderLayout.CENTER);

        panel.add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.add(card, BorderLayout.CENTER);
        JPanel charts = chartsColumn();
        center.add(charts, BorderLayout.EAST);
        center.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                charts.setVisible(center.getWidth() >= 860);
            }
        });
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel statCard(String title, String value, Color accent) {
        final boolean money = value.startsWith("₱");
        double parsed;
        try {
            parsed = Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException ex) {
            parsed = 0;
        }
        final double target = parsed;

        RoundPanel card = new RoundPanel(WHITE, 18, 16, true);
        card.setLayout(new BorderLayout(0, 8));
        JLabel val = label(money ? peso(0) : "0", Font.BOLD, 24, TEXT);
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(val, BorderLayout.WEST);
        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(new StatIcon(statGlyph(title), accent));
        row.add(iconWrap, BorderLayout.EAST);
        card.add(label(title, Font.PLAIN, 12, MUTED), BorderLayout.NORTH);
        card.add(row, BorderLayout.CENTER);

        final long start = System.currentTimeMillis();
        javax.swing.Timer tm = new javax.swing.Timer(16, null);
        tm.addActionListener(e -> {
            double x = Math.min(1.0, (System.currentTimeMillis() - start) / 800.0);
            double v = target * (1 - Math.pow(1 - x, 3));
            val.setText(money ? peso(v) : String.valueOf(Math.round(v)));
            if (x >= 1.0) tm.stop();
        });
        tm.start();
        return card;
    }

    private JPanel chartCard(String title, JComponent chart) {
        RoundPanel c = new RoundPanel(WHITE, 18, 14, true);
        c.setLayout(new BorderLayout(0, 6));
        JLabel h = label(title, Font.BOLD, 15, TEXT);
        h.setBorder(new EmptyBorder(2, 4, 0, 0));
        c.add(h, BorderLayout.NORTH);
        c.add(chart, BorderLayout.CENTER);
        return c;
    }

    private JPanel chartsColumn() {
        LinkedHashMap<String, Integer> st = new LinkedHashMap<>();
        for (String s : new String[]{"Pending", "Confirmed", "In Consultation", "Completed", "Cancelled"}) st.put(s, 0);
        for (Appointment ap : visibleAppointments()) st.merge(ap.status, 1, Integer::sum);

        String[] days = new String[7];
        int[] counts = new int[7];
        for (int k = 0; k < 7; k++) {
            LocalDate day = LocalDate.now().plusDays(k);
            days[k] = day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
            final String ds = day.toString();
            counts[k] = (int) visibleAppointments().stream()
                    .filter(ap -> ap.date.equals(ds) && !ap.status.equals("Cancelled")).count();
        }

        JPanel col = new JPanel(new GridLayout(2, 1, 0, 16));
        col.setOpaque(false);
        col.setPreferredSize(new Dimension(360, 0));
        col.add(chartCard("Appointment status", new DonutChart(st)));
        col.add(chartCard("Next 7 days", new BarChart(days, counts)));
        return col;
    }

    // ===================== PATIENTS =====================

    private DefaultTableModel patientModel() {
        DefaultTableModel model = tableModel(new String[]{"Patient ID", "Full Name", "Age", "Gender", "Contact",
                "Blood Type", "Allergies", "Portal Account"});
        Set<String> mine = current.role == Role.DOCTOR ? myPatientIds() : new HashSet<>();
        for (Patient p : patients) {
            if (current.role == Role.PATIENT && !patientOwns(p.id)) continue;
            if (current.role == Role.DOCTOR && !mine.contains(p.id)) continue;
            model.addRow(new Object[]{p.id, p.name.isBlank() ? "(not set)" : p.name,
                    p.age == 0 ? "—" : p.age, p.gender.isBlank() ? "—" : p.gender,
                    p.contact.isBlank() ? "—" : p.contact, p.blood.isBlank() ? "—" : p.blood,
                    p.allergies.isBlank() ? "None" : p.allergies, p.username.isEmpty() ? "No" : "Yes"});
        }
        return model;
    }

    private JPanel patientsPage() {
        JTable table = styledTable(patientModel());
        List<JButton> actions = new ArrayList<>();

        if (current.role == Role.RECEPTIONIST) {
            RoundButton register = new RoundButton("＋ Register Walk-in", Kind.PRIMARY);
            register.addActionListener(e -> registerWalkIn());
            actions.add(register);
        }
        if (current.role == Role.ADMIN || current.role == Role.RECEPTIONIST) {
            RoundButton update = new RoundButton("Update", Kind.SOFT);
            update.addActionListener(e -> editPatient(table));
            actions.add(update);
            RoundButton reset = new RoundButton("Reset Password", Kind.SOFT);
            reset.addActionListener(e -> resetPatientPassword(table));
            actions.add(reset);
        }
        if (current.role == Role.ADMIN) {
            RoundButton delete = new RoundButton("Delete", Kind.DANGER);
            delete.addActionListener(e -> deletePatient(table));
            actions.add(delete);
        }
        RoundButton summary = new RoundButton("View Summary", Kind.SOFT);
        summary.addActionListener(e -> viewPatientSummary(table));
        actions.add(summary);
        return listPage(table, actions.toArray(new JButton[0]));
    }

    private JPanel registerPatientPage() {
        JPanel panel = pageBase();
        panel.setLayout(new BorderLayout(0, 18));
        PulseBanner hero = new PulseBanner(new Color(30, 27, 90), new Color(8, 145, 178), 20);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(24, 28, 42, 28));
        hero.add(label("Register a walk-in patient", Font.BOLD, 22, WHITE));
        hero.add(Box.createVerticalStrut(8));
        hero.add(label("<html><div style='width:560px'>Create a patient record at the front desk. Patients who want online access "
                + "can sign up themselves using their Gmail or mobile number (with a one-time code).</div></html>",
                Font.PLAIN, 14, new Color(224, 242, 254)));
        hero.add(Box.createVerticalStrut(16));
        RoundButton reg = new RoundButton("＋ Register Patient", Kind.SOFT);
        reg.setAlignmentX(Component.LEFT_ALIGNMENT);
        reg.addActionListener(e -> registerWalkIn());
        hero.add(reg);
        panel.add(hero, BorderLayout.NORTH);
        panel.add(tableCard(styledTable(patientModel())), BorderLayout.CENTER);
        return panel;
    }

    private void registerWalkIn() {
        Patient p = patientDialog(null, false);
        if (p != null) {
            info("Patient registered successfully!\n\nPatient ID: " + p.id);
            refresh();
        }
    }

    private Patient patientDialog(Patient existing, boolean self) {
        boolean isNew = existing == null;
        RoundField name = new RoundField(isNew ? "" : existing.name);
        RoundField age = new RoundField(!isNew && existing.age > 0 ? String.valueOf(existing.age) : "");
        JComboBox<String> gender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        if (!isNew && !existing.gender.isBlank()) gender.setSelectedItem(existing.gender);
        RoundField contact = new RoundField(isNew ? "" : existing.contact);
        RoundField address = new RoundField(isNew ? "" : existing.address);
        JComboBox<String> blood = new JComboBox<>(new String[]{"Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        if (!isNew && !existing.blood.isBlank()) blood.setSelectedItem(existing.blood);
        RoundField allergies = new RoundField(isNew ? "" : existing.allergies);
        RoundField emergency = new RoundField(isNew ? "" : existing.emergency);

        JPanel form = form(self ? "Display Name" : "Full Name", name, "Age", age, "Gender", gender,
                "Contact Number", contact, "Address", address, "Blood Type", blood,
                "Allergies", allergies, "Emergency Contact", emergency);
        if (!confirm(isNew ? "Register Patient" : self ? "Edit My Profile" : "Update Patient", form)) return null;

        try {
            String n = name.getText().trim();
            if (n.length() < 2) throw new IllegalArgumentException("Please enter a name (at least 2 characters).");
            int a = age.getText().trim().isEmpty() ? 0 : Integer.parseInt(age.getText().trim());
            if (a < 0 || a > 130) throw new IllegalArgumentException("Invalid age.");
            if (isNew && (a == 0 || contact.getText().trim().isEmpty()))
                throw new IllegalArgumentException("Age and contact number are required for registration.");

            Patient p = existing;
            if (isNew) {
                p = new Patient("P" + String.format("%04d", nextPatientNumber()), n, a, "", "", "", "", "");
                patients.add(p);
            }
            p.name = n;
            p.age = a;
            p.gender = gender.getSelectedItem().toString();
            p.contact = contact.getText().trim();
            p.address = address.getText().trim();
            p.blood = blood.getSelectedItem().toString().equals("Unknown") ? "" : blood.getSelectedItem().toString();
            p.allergies = allergies.getText().trim();
            p.emergency = emergency.getText().trim();

            Account acc = p.username.isEmpty() ? null : accounts.get(p.username);
            if (acc != null) acc.displayName = n;
            return p;
        } catch (NumberFormatException ex) {
            bad("Age must be a valid number.");
        } catch (IllegalArgumentException ex) {
            bad(ex.getMessage());
        }
        return null;
    }

    private void editPatient(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        Patient p = findPatient(String.valueOf(table.getValueAt(row, 0)));
        if (p != null && patientDialog(p, false) != null) refresh();
    }

    private void deletePatient(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        if (!ask(frame, "Confirm Delete", "Delete this patient record, its account and related data?",
                JOptionPane.WARNING_MESSAGE)) return;
        Patient p = findPatient(id);
        if (p != null) {
            patients.remove(p);
            if (!p.username.isEmpty()) accounts.remove(p.username);
            appointments.removeIf(a -> a.patient.equals(id));
            admissions.removeIf(a -> a.patient.equals(id));
            prescriptions.removeIf(x -> x.patient.equals(id));
            records.removeIf(x -> x.patient.equals(id));
        }
        refresh();
    }

    private void resetPatientPassword(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        Patient p = findPatient(String.valueOf(table.getValueAt(row, 0)));
        if (p == null) return;
        Account a = p.username.isEmpty() ? null : accounts.get(p.username);
        if (a == null) { bad("This patient has no portal account (walk-in only)."); return; }
        RoundPassword np = new RoundPassword();
        if (!confirm("Reset password for " + patientName(p.id), form("New Password", np))) return;
        String pw = new String(np.getPassword());
        if (pw.length() < 8 || !pw.matches(".*[A-Za-z].*") || !pw.matches(".*\\d.*")) {
            bad("Use at least 8 characters with letters and numbers.");
            return;
        }
        a.setPassword(pw);
        info("Password updated. Please give the new password to the patient.");
    }

    private void viewPatientSummary(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        Patient p = findPatient(String.valueOf(table.getValueAt(row, 0)));
        if (p == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("PATIENT ").append(p.id).append("\n");
        sb.append("Name: ").append(p.name.isBlank() ? "(not set)" : p.name).append("\n");
        sb.append("Age / Gender: ").append(p.age == 0 ? "—" : p.age).append(" / ")
                .append(p.gender.isBlank() ? "—" : p.gender).append("\n");
        sb.append("Contact: ").append(p.contact.isBlank() ? "—" : p.contact).append("\n");
        sb.append("Address: ").append(p.address.isBlank() ? "—" : p.address).append("\n");
        sb.append("Blood type: ").append(p.blood.isBlank() ? "—" : p.blood).append("\n");
        sb.append("Allergies: ").append(p.allergies.isBlank() ? "None reported" : p.allergies).append("\n");
        sb.append("Emergency contact: ").append(p.emergency.isBlank() ? "—" : p.emergency).append("\n");

        sb.append("\nAPPOINTMENTS\n");
        int n = 0;
        for (Appointment a : appointments) if (a.patient.equals(p.id)) {
            sb.append(" • ").append(a.date).append(" ").append(a.time).append(" — Dr. ").append(a.doctor)
                    .append(" (").append(a.status).append(") ").append(a.reason).append("\n");
            n++;
        }
        if (n == 0) sb.append(" none\n");

        sb.append("\nMEDICAL RECORDS\n");
        n = 0;
        for (MedRecord r : records) if (r.patient.equals(p.id)) {
            sb.append(" • ").append(r.date).append(" — ").append(r.diagnosis).append(" (Dr. ").append(r.doctor).append(")\n");
            n++;
        }
        if (n == 0) sb.append(" none\n");

        sb.append("\nPRESCRIPTIONS\n");
        n = 0;
        for (Prescription x : prescriptions) if (x.patient.equals(p.id)) {
            sb.append(" • ").append(x.date).append(" — ").append(x.medicine).append(" ").append(x.dosage)
                    .append(", ").append(x.frequency).append(", qty ").append(x.qty)
                    .append(" [").append(x.status).append("]\n");
            n++;
        }
        if (n == 0) sb.append(" none\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(f(Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500, 360));
        showDlg(frame, "Patient Summary", JOptionPane.INFORMATION_MESSAGE, sp, "Close", null);
    }

    // ===================== STAFF / ACCOUNTS =====================

    private JPanel staffPage(boolean all) {
        DefaultTableModel model = tableModel(new String[]{"Username", "Display Name", "Role", "Specialty", "Email / Phone", "Status"});
        for (Account a : accounts.values()) {
            if (!all && a.role != Role.ADMIN && a.role != Role.DOCTOR) continue;
            String contact = !a.email.isEmpty() ? a.email : !a.phone.isEmpty() ? a.phone : "—";
            model.addRow(new Object[]{a.username, a.displayName.isBlank() ? "(not set)" : a.displayName,
                    roleTitle(a.role), a.specialty.isBlank() ? "—" : a.specialty, contact, a.active ? "Active" : "Inactive"});
        }
        JTable table = styledTable(model);
        RoundButton toggle = new RoundButton("Activate / Deactivate", Kind.SOFT);
        RoundButton reset = new RoundButton("Reset Password", Kind.SOFT);
        toggle.addActionListener(e -> toggleAccount(table));
        reset.addActionListener(e -> resetPassword(table));
        return listPage(table, toggle, reset);
    }

    private void toggleAccount(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String username = String.valueOf(table.getValueAt(row, 0));
        if (username.equals(current.username) || username.equals("admin")) {
            bad("You cannot deactivate your own account or the main admin.");
            return;
        }
        Account a = accounts.get(username);
        if (a != null) a.active = !a.active;
        refresh();
    }

    private void resetPassword(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        Account a = accounts.get(String.valueOf(table.getValueAt(row, 0)));
        if (a == null) return;
        RoundPassword p = new RoundPassword();
        if (!confirm("Reset password for " + a.username, form("New Password", p))) return;
        String np = new String(p.getPassword());
        int min = a.role == Role.PATIENT ? 8 : 6;
        if (np.length() < min) { bad("Password must be at least " + min + " characters."); return; }
        a.setPassword(np);
        info("Password updated.");
    }

    // ===================== APPOINTMENTS =====================

    private static String[] timeSlots() {
        List<String> out = new ArrayList<>();
        for (int h = 8; h <= 16; h++) {
            out.add(String.format("%02d:00", h));
            out.add(String.format("%02d:30", h));
        }
        return out.toArray(new String[0]);
    }

    private JPanel appointmentsPage(String page) {
        DefaultTableModel model = tableModel(new String[]{"Appointment ID", "Patient ID", "Patient",
                "Doctor", "Date", "Time", "Reason", "Status"});
        for (Appointment a : visibleAppointments()) {
            if (page.equals("Current Queue") && !(a.date.equals(today()) && openStatus(a.status))) continue;
            model.addRow(new Object[]{a.id, a.patient, patientName(a.patient), a.doctor,
                    a.date, a.time, a.reason, a.status});
        }
        JTable table = styledTable(model);
        List<JButton> actions = new ArrayList<>();

        if (current.role == Role.ADMIN || current.role == Role.RECEPTIONIST) {
            RoundButton add = new RoundButton("＋ New Appointment", Kind.PRIMARY);
            RoundButton update = new RoundButton("Update Status", Kind.SOFT);
            RoundButton delete = new RoundButton("Delete", Kind.DANGER);
            add.addActionListener(e -> bookAppointment(false));
            update.addActionListener(e -> updateAppointment(table));
            delete.addActionListener(e -> deleteAppointment(table));
            actions.add(add);
            actions.add(update);
            actions.add(delete);
        } else if (current.role == Role.DOCTOR) {
            RoundButton update = new RoundButton(page.equals("Current Queue") ? "Call / Update Status" : "Update Status", Kind.PRIMARY);
            update.addActionListener(e -> updateAppointment(table));
            actions.add(update);
        } else {
            RoundButton book = new RoundButton("＋ Book Appointment", Kind.PRIMARY);
            RoundButton cancel = new RoundButton("Cancel Appointment", Kind.DANGER);
            book.addActionListener(e -> bookAppointment(true));
            cancel.addActionListener(e -> cancelOwnAppointment(table));
            actions.add(book);
            actions.add(cancel);
        }
        return listPage(table, actions.toArray(new JButton[0]));
    }

    private void bookAppointment(boolean self) {
        List<Account> docs = activeDoctors();
        if (docs.isEmpty()) { bad("No doctors are available right now."); return; }
        String[] choices = patientChoices();
        if (!self && choices.length == 0) { bad("Register a patient first."); return; }

        JComboBox<String> patient = new JComboBox<>(choices);
        String[] docLabels = docs.stream()
                .map(x -> "Dr. " + x.displayName + (x.specialty.isBlank() ? "" : "  —  " + x.specialty))
                .toArray(String[]::new);
        JComboBox<String> doctor = new JComboBox<>(docLabels);
        RoundField date = new RoundField(LocalDate.now().plusDays(1).toString());
        JComboBox<String> time = new JComboBox<>(timeSlots());
        RoundField reason = new RoundField("");

        JPanel form = self
                ? form("Doctor", doctor, "Date (YYYY-MM-DD)", date, "Time", time, "Reason for visit", reason)
                : form("Patient", patient, "Doctor", doctor, "Date (YYYY-MM-DD)", date, "Time", time, "Reason for visit", reason);
        if (!confirm(self ? "Book an Appointment" : "New Appointment", form)) return;

        String patientId;
        if (self) {
            Patient me = findPatientByUsername(current.username);
            if (me == null) { bad("Patient profile not found."); return; }
            patientId = me.id;
        } else {
            patientId = patient.getSelectedItem().toString().split(" - ")[0];
        }

        LocalDate d;
        try {
            d = LocalDate.parse(date.getText().trim());
        } catch (Exception ex) {
            bad("Please enter the date as YYYY-MM-DD.");
            return;
        }
        if (d.isBefore(LocalDate.now())) { bad("The date cannot be in the past."); return; }
        if (d.isAfter(LocalDate.now().plusMonths(6))) { bad("Appointments can only be booked up to 6 months ahead."); return; }
        if (reason.getText().trim().isEmpty()) { bad("Please enter the reason for the visit."); return; }

        String doc = docs.get(doctor.getSelectedIndex()).displayName;
        String slot = time.getSelectedItem().toString();
        if (d.equals(LocalDate.now()) && LocalTime.parse(slot).isBefore(LocalTime.now())) {
            bad("That time has already passed today. Please choose a later time.");
            return;
        }
        for (Appointment a : appointments) {
            if (a.status.equals("Cancelled") || !a.date.equals(d.toString()) || !a.time.equals(slot)) continue;
            if (a.doctor.equals(doc)) {
                bad("That time slot is already taken for Dr. " + doc + ". Please choose another time.");
                return;
            }
            if (a.patient.equals(patientId)) {
                bad("This patient already has another appointment at that date and time.");
                return;
            }
        }
        appointments.add(new Appointment("A" + String.format("%04d", ++apptNo), patientId, doc,
                d.toString(), slot, reason.getText().trim(), "Pending"));
        info("Appointment booked for " + d + " at " + slot + " with Dr. " + doc + ".");
        refresh();
    }

    private void updateAppointment(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        Appointment ap = appointments.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
        if (ap == null) return;
        String status = choose(frame, "Appointment Status", "Select status:",
                new String[]{"Pending", "Confirmed", "In Consultation", "Completed", "Cancelled"}, ap.status);
        if (status != null) {
            ap.status = status;
            refresh();
        }
    }

    private void cancelOwnAppointment(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        Appointment ap = appointments.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
        if (ap == null) return;
        if (!(ap.status.equals("Pending") || ap.status.equals("Confirmed"))) {
            bad("Only pending or confirmed appointments can be cancelled.");
            return;
        }
        if (ask(frame, "Cancel Appointment", "Cancel this appointment?", JOptionPane.QUESTION_MESSAGE)) {
            ap.status = "Cancelled";
            refresh();
        }
    }

    private void deleteAppointment(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        if (ask(frame, "Confirm Delete", "Are you sure you want to delete this appointment?",
                JOptionPane.WARNING_MESSAGE)) {
            appointments.removeIf(a -> a.id.equals(id));
            refresh();
        }
    }

    // ===================== ADMISSIONS =====================

    private JPanel admissionsPage() {
        DefaultTableModel model = tableModel(new String[]{"Admission ID", "Patient ID", "Patient", "Room Type",
                "Room", "Bed", "Admission Date", "Discharge Date", "Status"});
        for (Admission a : admissions) {
            model.addRow(new Object[]{a.id, a.patient, patientName(a.patient), a.roomType,
                    a.room, a.bed, a.admitDate, a.dischargeDate, a.status});
        }
        JTable table = styledTable(model);
        RoundButton add = new RoundButton("＋ Admit Patient", Kind.PRIMARY);
        RoundButton discharge = new RoundButton("Discharge", Kind.SOFT);
        add.addActionListener(e -> addAdmission());
        discharge.addActionListener(e -> dischargePatient(table));
        return listPage(table, add, discharge);
    }

    private void addAdmission() {
        String[] choices = patientChoices();
        if (choices.length == 0) { bad("Register a patient first."); return; }
        JComboBox<String> patient = new JComboBox<>(choices);
        JComboBox<String> roomType = new JComboBox<>(new String[]{"Ward", "Private", "ICU"});
        RoundField room = new RoundField("");
        RoundField bed = new RoundField("");
        RoundField date = new RoundField(today());
        if (!confirm("Admit Patient", form("Patient", patient, "Room Type", roomType, "Room", room,
                "Bed Number", bed, "Admission Date (YYYY-MM-DD)", date))) return;

        String roomNo = room.getText().trim(), bedNo = bed.getText().trim();
        if (roomNo.isEmpty() || bedNo.isEmpty()) { bad("Enter room and bed number."); return; }

        LocalDate ad;
        try {
            ad = LocalDate.parse(date.getText().trim());
        } catch (Exception ex) {
            bad("Please enter the admission date as YYYY-MM-DD.");
            return;
        }
        if (ad.isAfter(LocalDate.now())) { bad("The admission date cannot be in the future."); return; }

        String pid = patient.getSelectedItem().toString().split(" - ")[0];
        boolean already = admissions.stream().anyMatch(a -> a.status.equals("Admitted") && a.patient.equals(pid));
        if (already) { bad("This patient is already admitted. Discharge first before a new admission."); return; }
        boolean occupied = admissions.stream().anyMatch(a -> a.status.equals("Admitted")
                && a.room.equalsIgnoreCase(roomNo) && a.bed.equalsIgnoreCase(bedNo));
        if (occupied) { bad("That bed is already occupied."); return; }

        admissions.add(new Admission("IN" + String.format("%04d", ++admNo), pid,
                roomType.getSelectedItem().toString(), roomNo, bedNo, ad.toString(), "Admitted"));
        refresh();
    }

    private void dischargePatient(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        Admission adm = admissions.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
        if (adm == null) return;
        if (!adm.status.equals("Admitted")) { bad("This patient is already discharged."); return; }
        if (!ask(frame, "Confirm Discharge", "Discharge this patient?\n\nDon't forget to process the bill in Billing.",
                JOptionPane.QUESTION_MESSAGE)) return;
        adm.status = "Discharged";
        adm.dischargeDate = today();
        refresh();
    }

    private int activeAdmissions() {
        return (int) admissions.stream().filter(a -> a.status.equals("Admitted")).count();
    }

    // ===================== MEDICAL RECORDS =====================

    private JPanel recordsPage() {
        DefaultTableModel model = tableModel(new String[]{"Record ID", "Patient ID", "Patient", "Doctor",
                "Date", "Diagnosis", "Notes"});
        Set<String> mine = current.role == Role.DOCTOR ? myPatientIds() : new HashSet<>();
        for (MedRecord r : records) {
            if (current.role == Role.PATIENT && !patientOwns(r.patient)) continue;
            if (current.role == Role.DOCTOR && !r.doctor.equalsIgnoreCase(current.displayName)
                    && !mine.contains(r.patient)) continue;
            model.addRow(new Object[]{r.id, r.patient, patientName(r.patient), r.doctor, r.date, r.diagnosis, r.notes});
        }
        JTable table = styledTable(model);
        List<JButton> actions = new ArrayList<>();
        RoundButton view = new RoundButton("View Details", Kind.SOFT);
        view.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { warnSelect(); return; }
            String id = String.valueOf(table.getValueAt(row, 0));
            for (MedRecord r : records) {
                if (r.id.equals(id)) {
                    JTextArea ta = new JTextArea("Patient: " + patientName(r.patient) + " (" + r.patient + ")\n"
                            + "Doctor: " + r.doctor + "\nDate: " + r.date + "\n\nDiagnosis:\n" + r.diagnosis
                            + "\n\nNotes / Treatment:\n" + (r.notes.isBlank() ? "—" : r.notes));
                    ta.setEditable(false);
                    ta.setLineWrap(true);
                    ta.setWrapStyleWord(true);
                    ta.setFont(f(Font.PLAIN, 14));
                    JScrollPane sp = new JScrollPane(ta);
                    sp.setPreferredSize(new Dimension(460, 300));
                    showDlg(frame, "Medical Record " + r.id, JOptionPane.INFORMATION_MESSAGE, sp, "Close", null);
                }
            }
        });
        if (current.role == Role.DOCTOR) {
            RoundButton add = new RoundButton("＋ Add Record", Kind.PRIMARY);
            add.addActionListener(e -> addRecord());
            actions.add(add);
        }
        actions.add(view);
        return listPage(table, actions.toArray(new JButton[0]));
    }

    private void addRecord() {
        String[] choices = patientChoicesForCurrent();
        if (choices.length == 0) {
            bad("You have no patients yet.\nPatients appear here once they book an appointment with you.");
            return;
        }
        JComboBox<String> patient = new JComboBox<>(choices);
        RoundField diagnosis = new RoundField("");
        RoundField notes = new RoundField("");
        if (!confirm("New Medical Record", form("Patient", patient, "Diagnosis", diagnosis,
                "Notes / Treatment", notes))) return;
        if (diagnosis.getText().trim().isEmpty()) { bad("Please enter a diagnosis."); return; }
        records.add(new MedRecord("MR" + String.format("%04d", ++recNo),
                patient.getSelectedItem().toString().split(" - ")[0], current.displayName, today(),
                diagnosis.getText().trim(), notes.getText().trim()));
        refresh();
    }

    // ===================== BILLING =====================

    private static double num(JTextField f) {
        String s = f.getText().trim().replace(",", "");
        if (s.isEmpty()) return 0;
        double v = Double.parseDouble(s);
        if (v < 0 || Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException();
        return v;
    }

    private static int whole(JTextField f) {
        String s = f.getText().trim();
        int v = s.isEmpty() ? 0 : Integer.parseInt(s);
        if (v < 0) throw new NumberFormatException();
        return v;
    }

    private String receiptText(Invoice inv) {
        return APP_NAME + " " + APP_SUB + "\nOFFICIAL RECEIPT\n\n"
                + "Invoice:   " + inv.id + "\n"
                + "Patient:   " + patientName(inv.patient) + "\n"
                + "Date:      " + inv.date + "\n"
                + "Cashier:   " + inv.cashier + "\n\n"
                + "Subtotal:  " + peso(inv.subtotal) + "\n"
                + "Discount:  " + peso(inv.discount) + "\n"
                + "TOTAL:     " + peso(inv.total) + "\n"
                + "Paid:      " + peso(inv.paid) + "\n"
                + "Change:    " + peso(inv.change) + "\n\n"
                + "Thank you and get well soon!\n"
                + SLOGAN_1 + " " + SLOGAN_2;
    }

    private RoundButton receiptButton(JTable table) {
        RoundButton b = new RoundButton("View Receipt", Kind.SOFT);
        b.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { warnSelect(); return; }
            String id = String.valueOf(table.getValueAt(row, 0));
            for (Invoice i : invoices) {
                if (i.id.equals(id)) {
                    msg(frame, "Receipt " + i.id, receiptText(i), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
        });
        return b;
    }

    private JPanel billingPage() {
        JPanel panel = pageBase();
        panel.setLayout(new BorderLayout(0, 16));

        DefaultTableModel model = tableModel(new String[]{"Invoice", "Patient ID", "Patient", "Date", "Subtotal",
                "Discount", "Total", "Paid", "Change", "Cashier"});
        for (Invoice i : invoices) {
            if (current.role == Role.PATIENT && !patientOwns(i.patient)) continue;
            model.addRow(new Object[]{i.id, i.patient, patientName(i.patient), i.date, peso(i.subtotal),
                    peso(i.discount), peso(i.total), peso(i.paid), peso(i.change), i.cashier});
        }
        JTable table = styledTable(model);

        if (current.role == Role.PATIENT) {
            return listPage(table, receiptButton(table));
        }

        String[] choices = patientChoices();
        JComboBox<String> patient = new JComboBox<>(choices);
        styleCombo(patient);
        RoundField consultation = new RoundField("0");
        RoundField roomCharge = new RoundField("0");
        RoundField days = new RoundField("0");
        RoundField medicine = new RoundField("0");
        RoundField paid = new RoundField("0");
        JCheckBox discount = new JCheckBox("Apply 20% Senior / PWD discount");
        discount.setOpaque(false);
        discount.setFont(f(Font.BOLD, 13));
        discount.setForeground(TEXT);

        JLabel totalLbl = label("Total due  ₱0.00", Font.BOLD, 20, PRIMARY);
        JLabel statusLbl = label(" ", Font.BOLD, 14, DANGER);
        RoundButton process = new RoundButton("Process Payment", Kind.PRIMARY);

        Runnable recalc = () -> {
            try {
                double sub = num(consultation) + num(roomCharge) * whole(days) + num(medicine);
                double tot = sub - (discount.isSelected() ? sub * 0.20 : 0);
                double pd = num(paid);
                totalLbl.setText("Total due  " + peso(tot));
                if (tot > 0 && pd + 0.001 >= tot) {
                    statusLbl.setText("Change  " + peso(pd - tot));
                    statusLbl.setForeground(SUCCESS);
                } else {
                    statusLbl.setText("Balance  " + peso(Math.max(0, tot - pd)));
                    statusLbl.setForeground(DANGER);
                }
            } catch (NumberFormatException ex) {
                totalLbl.setText("Total due  —");
                statusLbl.setText("Enter valid amounts");
                statusLbl.setForeground(DANGER);
            }
        };
        for (JTextField t : new JTextField[]{consultation, roomCharge, days, medicine, paid}) onChange(t, recalc);
        discount.addActionListener(e -> recalc.run());

        RoundPanel formCard = new RoundPanel(WHITE, 18, 16, true);
        formCard.setLayout(new BorderLayout(0, 12));
        formCard.add(label("New invoice & payment", Font.BOLD, 17, TEXT), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 10));
        grid.setOpaque(false);
        grid.add(captioned("Patient", patient));
        grid.add(captioned("Consultation fee (₱)", consultation));
        grid.add(captioned("Room charge per day (₱)", roomCharge));
        grid.add(captioned("Number of days", days));
        grid.add(captioned("Medicine / treatment (₱)", medicine));
        grid.add(captioned("Amount paid (₱)", paid));
        formCard.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        JPanel sums = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 0));
        sums.setOpaque(false);
        sums.add(discount);
        sums.add(totalLbl);
        sums.add(statusLbl);
        bottom.add(sums, BorderLayout.CENTER);
        bottom.add(process, BorderLayout.EAST);
        formCard.add(bottom, BorderLayout.SOUTH);

        process.addActionListener(e -> {
            if (choices.length == 0) { bad("Register a patient first."); return; }
            try {
                double c = num(consultation), r = num(roomCharge), med = num(medicine), pay = num(paid);
                int d = whole(days);
                double sub = c + r * d + med;
                double disc = discount.isSelected() ? sub * 0.20 : 0;
                double total = sub - disc;

                if (total <= 0) { bad("There is nothing to bill. Enter at least one charge."); return; }

                if (pay + 0.001 < total) {
                    msg(frame, "Payment Incomplete",
                            "Insufficient payment!\n\n"
                            + "Total due:      " + peso(total) + "\n"
                            + "Amount paid:  " + peso(pay) + "\n"
                            + "Short by:         " + peso(total - pay) + "\n\n"
                            + "The transaction cannot proceed until the full amount is paid.", JOptionPane.ERROR_MESSAGE);
                    paid.requestFocusInWindow();
                    paid.selectAll();
                    return;
                }

                Invoice inv = new Invoice();
                inv.id = "INV" + String.format("%04d", ++invNo);
                inv.patient = patient.getSelectedItem().toString().split(" - ")[0];
                inv.date = today();
                inv.cashier = current.displayName;
                inv.subtotal = sub;
                inv.discount = disc;
                inv.total = total;
                inv.paid = pay;
                inv.change = pay - total;
                invoices.add(inv);

                msg(frame, "Payment Successful", receiptText(inv), JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } catch (NumberFormatException ex) {
                bad("Enter valid non-negative amounts (number of days must be a whole number).");
            }
        });

        panel.add(formCard, BorderLayout.NORTH);
        panel.add(listPageBody(table, receiptButton(table)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel captioned(String caption, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(caption);
        l.setFont(f(Font.BOLD, 12));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ===================== PHARMACY =====================

    private JPanel section(String title, JPanel body) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(label(title, Font.BOLD, 16, TEXT), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private JPanel pharmacyPage() {
        JPanel panel = pageBase();
        panel.setLayout(new BorderLayout(0, 14));

        RoundPanel how = new RoundPanel(WHITE, 16, 12, true);
        how.setLayout(new GridLayout(1, 3, 12, 0));
        how.add(label("① Doctor prescribes  →  Pending", Font.BOLD, 13, PRIMARY));
        how.add(label("② Pharmacy dispenses  →  stock deducted", Font.BOLD, 13, ACCENT.darker()));
        how.add(label("③ Patient sees it  →  Dispensed", Font.BOLD, 13, SUCCESS.darker()));
        panel.add(how, BorderLayout.NORTH);

        DefaultTableModel inv = tableModel(new String[]{"Medicine", "Available Stock", "Status"});
        for (Medicine m : medicines) {
            inv.addRow(new Object[]{m.name, m.stock, m.stock == 0 ? "Out of Stock" : m.stock < 30 ? "Low Stock" : "In Stock"});
        }
        JTable invTable = styledTable(inv);
        List<JButton> invButtons = new ArrayList<>();
        if (current.role == Role.ADMIN) {
            RoundButton add = new RoundButton("＋ Add Medicine", Kind.PRIMARY);
            RoundButton update = new RoundButton("Update Stock", Kind.SOFT);
            add.addActionListener(e -> addMedicine());
            update.addActionListener(e -> updateStock(invTable));
            invButtons.add(add);
            invButtons.add(update);
        }

        DefaultTableModel queue = tableModel(new String[]{"Prescription ID", "Date", "Patient", "Doctor",
                "Medicine", "Dosage", "Frequency", "Qty", "In Stock"});
        for (Prescription p : prescriptions) {
            if (!p.status.equals("Pending")) continue;
            if (current.role == Role.DOCTOR && !p.doctor.equalsIgnoreCase(current.displayName)) continue;
            Medicine m = findMedicine(p.medicine);
            queue.addRow(new Object[]{p.id, p.date, patientName(p.patient), p.doctor, p.medicine,
                    p.dosage, p.frequency, p.qty, m == null ? 0 : m.stock});
        }
        JTable queueTable = styledTable(queue);
        List<JButton> queueButtons = new ArrayList<>();
        if (current.role == Role.ADMIN) {
            RoundButton dispense = new RoundButton("✔ Dispense", Kind.PRIMARY);
            dispense.addActionListener(e -> dispense(queueTable));
            queueButtons.add(dispense);
        }

        JPanel both = new JPanel(new GridLayout(2, 1, 0, 14));
        both.setOpaque(false);
        both.add(section("Medicine inventory", listPageBody(invTable, invButtons.toArray(new JButton[0]))));
        both.add(section("Prescriptions waiting to be dispensed", listPageBody(queueTable, queueButtons.toArray(new JButton[0]))));
        panel.add(both, BorderLayout.CENTER);
        return panel;
    }

    private void addMedicine() {
        RoundField name = new RoundField("");
        RoundField qty = new RoundField("0");
        if (!confirm("Add Medicine", form("Medicine Name", name, "Quantity", qty))) return;
        try {
            int stock = Integer.parseInt(qty.getText().trim());
            String n = name.getText().trim();
            if (n.isEmpty() || stock < 0) throw new NumberFormatException();
            Medicine existing = findMedicine(n);
            if (existing == null) medicines.add(new Medicine(n, stock));
            else existing.stock += stock;
            refresh();
        } catch (NumberFormatException ex) {
            bad("Enter a medicine name and a valid quantity.");
        }
    }

    private void updateStock(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String name = String.valueOf(table.getValueAt(row, 0));
        Medicine med = findMedicine(name);
        if (med == null) return;
        String q = prompt(frame, "Update stock", "New stock quantity for " + med.name + ":", String.valueOf(med.stock));
        if (q == null) return;
        try {
            int stock = Integer.parseInt(q.trim());
            if (stock < 0) throw new NumberFormatException();
            med.stock = stock;
            refresh();
        } catch (NumberFormatException ex) {
            bad("Invalid quantity.");
        }
    }

    private void dispense(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        Prescription rx = prescriptions.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
        if (rx == null || !rx.status.equals("Pending")) { bad("This prescription cannot be dispensed."); return; }
        Medicine m = findMedicine(rx.medicine);
        if (m == null) { bad("This medicine is not in the inventory. Add it first."); return; }
        if (m.stock < rx.qty) {
            bad("Not enough stock for " + m.name + ".\n\nNeeded: " + rx.qty + "\nAvailable: " + m.stock
                    + "\n\nRestock the medicine first, then dispense.");
            return;
        }
        if (!ask(frame, "Dispense medicine", "Dispense " + rx.qty + " x " + rx.medicine + " to "
                + patientName(rx.patient) + "?", JOptionPane.QUESTION_MESSAGE)) return;
        m.stock -= rx.qty;
        rx.status = "Dispensed";
        info("Dispensed. Remaining stock of " + m.name + ": " + m.stock);
        refresh();
    }

    // ===================== PRESCRIPTIONS =====================

    private JPanel prescriptionsPage() {
        DefaultTableModel model = tableModel(new String[]{"Prescription ID", "Date", "Patient ID", "Patient", "Doctor",
                "Medicine", "Dosage", "Frequency", "Qty", "Status"});
        for (Prescription p : prescriptions) {
            if (current.role == Role.PATIENT && !patientOwns(p.patient)) continue;
            if (current.role == Role.DOCTOR && !p.doctor.equalsIgnoreCase(current.displayName)) continue;
            model.addRow(new Object[]{p.id, p.date, p.patient, patientName(p.patient), p.doctor, p.medicine,
                    p.dosage, p.frequency, p.qty, p.status});
        }
        JTable table = styledTable(model);
        List<JButton> actions = new ArrayList<>();
        if (current.role == Role.DOCTOR) {
            RoundButton add = new RoundButton("＋ Add Prescription", Kind.PRIMARY);
            RoundButton cancel = new RoundButton("Cancel Prescription", Kind.DANGER);
            add.addActionListener(e -> addPrescription());
            cancel.addActionListener(e -> cancelPrescription(table));
            actions.add(add);
            actions.add(cancel);
        }
        RoundButton view = new RoundButton("View Details", Kind.SOFT);
        view.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { warnSelect(); return; }
            String id = String.valueOf(table.getValueAt(row, 0));
            for (Prescription p : prescriptions) {
                if (p.id.equals(id)) {
                    msg(frame, "Prescription " + p.id,
                            "Patient: " + patientName(p.patient) + " (" + p.patient + ")\n"
                                    + "Doctor: Dr. " + p.doctor + "\n"
                                    + "Date: " + p.date + "\n\n"
                                    + "Medicine: " + p.medicine + "\n"
                                    + "Dosage: " + p.dosage + "\n"
                                    + "Frequency: " + p.frequency + "\n"
                                    + "Quantity: " + p.qty + "\n"
                                    + "Instructions: " + (p.instructions.isBlank() ? "—" : p.instructions) + "\n\n"
                                    + "Status: " + p.status, JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
        });
        actions.add(view);
        return listPage(table, actions.toArray(new JButton[0]));
    }

    private void addPrescription() {
        String[] choices = patientChoicesForCurrent();
        if (choices.length == 0) {
            bad("You have no patients yet.\nPatients appear here once they book an appointment with you.");
            return;
        }
        if (medicines.isEmpty()) { bad("The pharmacy has no medicines yet."); return; }

        JComboBox<String> patient = new JComboBox<>(choices);
        JComboBox<String> medicine = new JComboBox<>();
        for (Medicine m : medicines) medicine.addItem(m.name + "   (stock: " + m.stock + ")");
        RoundField dosage = new RoundField("");
        dosage.setToolTipText("Example: 500mg");
        RoundField frequency = new RoundField("");
        frequency.setToolTipText("Example: 3 times a day after meals");
        RoundField qty = new RoundField("");
        RoundField instructions = new RoundField("");
        if (!confirm("New Prescription", form("Patient", patient, "Medicine", medicine,
                "Dosage (e.g. 500mg)", dosage, "Frequency (e.g. 3x a day)", frequency,
                "Quantity (tablets)", qty, "Instructions (optional)", instructions))) return;

        Medicine med = medicines.get(medicine.getSelectedIndex());
        if (dosage.getText().trim().isEmpty() || frequency.getText().trim().isEmpty()) {
            bad("Please enter the dosage and frequency.");
            return;
        }
        int q;
        try {
            q = Integer.parseInt(qty.getText().trim());
            if (q <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            bad("Quantity must be a whole number greater than 0.");
            return;
        }
        if (med.stock == 0) { bad(med.name + " is OUT OF STOCK. Please choose another medicine."); return; }
        if (q > med.stock) {
            bad("Only " + med.stock + " of " + med.name + " are available. Please lower the quantity.");
            return;
        }

        String pid = patient.getSelectedItem().toString().split(" - ")[0];
        Patient pt = findPatient(pid);
        String firstWord = med.name.split(" ")[0].toLowerCase();
        if (pt != null && !pt.allergies.isBlank() && pt.allergies.toLowerCase().contains(firstWord)) {
            if (!ask(frame, "Allergy warning", "This patient has a recorded allergy: " + pt.allergies
                    + "\n\nThe selected medicine (" + med.name + ") may be related. Prescribe anyway?",
                    JOptionPane.WARNING_MESSAGE)) return;
        }

        prescriptions.add(new Prescription("RX" + String.format("%04d", ++rxNo), pid, current.displayName,
                med.name, dosage.getText().trim(), frequency.getText().trim(), q,
                instructions.getText().trim(), today()));
        info("Prescription saved. It is now waiting in the Pharmacy to be dispensed.");
        refresh();
    }

    private void cancelPrescription(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { warnSelect(); return; }
        String id = String.valueOf(table.getValueAt(row, 0));
        Prescription rx = prescriptions.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
        if (rx == null) return;
        if (!rx.status.equals("Pending")) { bad("Only pending prescriptions can be cancelled."); return; }
        if (ask(frame, "Cancel Prescription", "Cancel this prescription?", JOptionPane.QUESTION_MESSAGE)) {
            rx.status = "Cancelled";
            refresh();
        }
    }

    // ===================== PATIENT PROFILE =====================

    private JPanel profilePage() {
        JPanel panel = pageBase();
        panel.setLayout(new BorderLayout(0, 18));
        Patient p = findPatientByUsername(current.username);

        PulseBanner head = new PulseBanner(new Color(30, 27, 90), new Color(8, 145, 178), 22);
        head.setLayout(new BorderLayout(18, 0));
        head.setBorder(new EmptyBorder(24, 28, 40, 28));
        head.add(new Avatar(current.displayName, 64), BorderLayout.WEST);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(label(current.displayName, Font.BOLD, 24, WHITE));
        t.add(Box.createVerticalStrut(4));
        t.add(label(p == null ? "" : "Patient ID: " + p.id, Font.PLAIN, 14, new Color(224, 242, 254)));
        head.add(t, BorderLayout.CENTER);
        RoundButton edit = new RoundButton("Edit Profile", Kind.SOFT);
        edit.addActionListener(e -> {
            if (p != null && patientDialog(p, true) != null) {
                identity.setText("Welcome " + roleTitle(current.role) + " " + current.displayName + "!");
                sideName.setText(current.displayName);
                refresh();
            }
        });
        JPanel eb = new JPanel(new GridBagLayout());
        eb.setOpaque(false);
        eb.add(edit);
        head.add(eb, BorderLayout.EAST);

        RoundPanel details = new RoundPanel(WHITE, 18, 22, true);
        details.setLayout(new GridLayout(0, 2, 30, 4));
        if (p == null) {
            details.add(label("No patient profile found.", Font.PLAIN, 14, MUTED));
        } else {
            String[][] rows = {
                {"Display Name", current.displayName},
                {"Email", current.email.isEmpty() ? "—" : current.email},
                {"Mobile", current.phone.isEmpty() ? "—" : current.phone},
                {"Age", p.age == 0 ? "—" : String.valueOf(p.age)},
                {"Gender", p.gender.isBlank() ? "—" : p.gender},
                {"Contact", p.contact.isBlank() ? "—" : p.contact},
                {"Address", p.address.isBlank() ? "—" : p.address},
                {"Blood Type", p.blood.isBlank() ? "—" : p.blood},
                {"Allergies", p.allergies.isBlank() ? "None reported" : p.allergies},
                {"Emergency Contact", p.emergency.isBlank() ? "—" : p.emergency}
            };
            for (String[] r : rows) {
                JPanel cell = new JPanel();
                cell.setOpaque(false);
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                cell.setBorder(new EmptyBorder(8, 0, 8, 0));
                cell.add(label(r[0], Font.PLAIN, 12, MUTED));
                cell.add(Box.createVerticalStrut(3));
                cell.add(label(r[1], Font.BOLD, 15, TEXT));
                details.add(cell);
            }
        }
        panel.add(head, BorderLayout.NORTH);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(details, BorderLayout.NORTH);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    // ===================== LAYOUT / TABLE HELPERS =====================

    private JPanel pageBase() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(22, 26, 24, 26));
        return panel;
    }

    private JPanel emptyPage(String message) {
        JPanel panel = pageBase();
        panel.add(label(message, Font.PLAIN, 14, MUTED), BorderLayout.NORTH);
        return panel;
    }

    private JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(WHITE);
        return sp;
    }

    private JPanel tableCard(JTable table) {
        RoundPanel card = new RoundPanel(WHITE, 18, 8, true);
        card.setLayout(new BorderLayout());
        card.add(scroll(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel listPageBody(JTable table, JButton... buttons) {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        RoundField search = new RoundField("");
        search.setPreferredSize(new Dimension(240, 42));
        search.setToolTipText("Search this table");
        onChange(search, () -> {
            String s = search.getText().trim();
            sorter.setRowFilter(s.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(s)));
        });
        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setOpaque(false);
        searchBox.add(label("🔍", Font.PLAIN, 14, MUTED), BorderLayout.WEST);
        searchBox.add(search, BorderLayout.CENTER);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        for (JButton b : buttons) left.add(b);
        bar.add(left, BorderLayout.CENTER);
        bar.add(searchBox, BorderLayout.EAST);

        body.add(bar, BorderLayout.NORTH);
        body.add(tableCard(table), BorderLayout.CENTER);
        return body;
    }

    private JPanel listPage(JTable table, JButton... buttons) {
        JPanel panel = pageBase();
        panel.add(listPageBody(table, buttons), BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel tableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static Color statusColor(String s) {
        switch (s) {
            case "Confirmed": case "Completed": case "Admitted": case "Active": case "In Stock":
            case "Yes": case "Dispensed":
                return SUCCESS;
            case "In Consultation":
                return PRIMARY;
            case "Pending": case "Low Stock":
                return WARN;
            case "Cancelled": case "Inactive": case "Out of Stock":
                return DANGER;
            case "Discharged":
                return MUTED;
            default:
                return null;
        }
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    aa(g2);
                    int cx = getWidth() / 2, cy = Math.max(getHeight() / 2, 100);
                    g2.setPaint(new GradientPaint(cx - 34, cy - 64, new Color(224, 231, 255), cx + 34, cy + 4, new Color(207, 250, 254)));
                    g2.fillOval(cx - 34, cy - 64, 68, 68);
                    g2.setColor(PRIMARY);
                    g2.setFont(f(Font.BOLD, 30));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("✚", cx - fm.stringWidth("✚") / 2, cy - 30 + (fm.getAscent() - fm.getDescent()) / 2);
                    g2.setColor(TEXT);
                    g2.setFont(f(Font.BOLD, 15));
                    fm = g2.getFontMetrics();
                    g2.drawString("No records yet", cx - fm.stringWidth("No records yet") / 2, cy + 30);
                    g2.setColor(MUTED);
                    g2.setFont(f(Font.PLAIN, 12));
                    fm = g2.getFontMetrics();
                    String s = "Data will appear here once it is added.";
                    g2.drawString(s, cx - fm.stringWidth(s) / 2, cy + 50);
                    g2.dispose();
                }
            }
        };
        table.setRowHeight(44);
        table.setFont(f(Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(224, 231, 255));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, false, row, col);
                setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                        new EmptyBorder(0, 14, 0, 14)));
                setBackground(sel ? t.getSelectionBackground() : row % 2 == 0 ? WHITE : new Color(250, 251, 253));
                Color sc = value == null ? null : statusColor(value.toString());
                setForeground(sc != null ? sc : TEXT);
                setFont(f(sc != null ? Font.BOLD : Font.PLAIN, 13));
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, false, false, row, col);
                setText(value == null ? "" : value.toString().toUpperCase());
                setFont(f(Font.BOLD, 11));
                setForeground(MUTED);
                setBackground(new Color(248, 250, 252));
                setBorder(new CompoundBorder(new MatteBorder(0, 0, 2, 0, new Color(199, 210, 254)), new EmptyBorder(0, 14, 0, 14)));
                return this;
            }
        });
        return table;
    }

    private void warnSelect() {
        bad("Please select a row first.");
    }

    // ===================== OTP SERVICE =====================

    static final class OtpService {

        private record Pending(String code, long expires, int attempts) {}

        private static final SecureRandom RNG = new SecureRandom();
        private static final long TTL_MS = 5 * 60_000L;
        private static final long COOLDOWN_MS = 30_000L;
        private static final int MAX_ATTEMPTS = 5;
        private static final Properties FILE = new Properties();

        static {
            File f = new File("basizon.properties");
            if (f.isFile()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    FILE.load(in);
                } catch (IOException ignored) {
                }
            }
        }

        private final Map<String, Pending> pending = new HashMap<>();
        private final Map<String, Long> lastSent = new HashMap<>();

        static String cfg(String key) {
            String v = System.getenv(key);
            if (v == null || v.isBlank()) {
                v = FILE.getProperty(key, "");
            }
            return v.trim();
        }

        static boolean emailConfigured() {
            return !cfg("GMAIL_USER").isEmpty() && !cfg("GMAIL_APP_PASSWORD").isEmpty();
        }

        static boolean smsConfigured() {
            return !cfg("SEMAPHORE_API_KEY").isEmpty();
        }

        static String normalizePhone(String raw) {
            if (raw == null) return null;
            String s = raw.replaceAll("[\\s\\-()]", "");
            if (s.matches("^\\+639\\d{9}$")) return "0" + s.substring(3);
            if (s.matches("^639\\d{9}$")) return "0" + s.substring(2);
            if (s.matches("^09\\d{9}$")) return s;
            return null;
        }

        String send(String target, boolean email) throws Exception {
            synchronized (this) {
                Long last = lastSent.get(target);
                if (last != null && System.currentTimeMillis() - last < COOLDOWN_MS) {
                    long wait = (COOLDOWN_MS - (System.currentTimeMillis() - last)) / 1000 + 1;
                    throw new IllegalStateException("Please wait " + wait + " seconds before requesting a new code.");
                }
                lastSent.put(target, System.currentTimeMillis());
            }

            String code = String.format("%06d", RNG.nextInt(1_000_000));
            boolean live = email ? emailConfigured() : smsConfigured();

            if (live) {
                if (email) sendEmail(target, code);
                else sendSms(target, code);
            }

            synchronized (this) {
                pending.put(target, new Pending(code, System.currentTimeMillis() + TTL_MS, 0));
            }
            return live ? null : code;
        }

        synchronized String verify(String target, String input) {
            Pending p = pending.get(target);
            if (p == null) return "Please request a verification code first.";
            if (System.currentTimeMillis() > p.expires()) {
                pending.remove(target);
                return "This code has expired. Please request a new one.";
            }
            if (p.attempts() >= MAX_ATTEMPTS) {
                pending.remove(target);
                return "Too many wrong attempts. Please request a new code.";
            }
            if (!p.code().equals(input.trim())) {
                pending.put(target, new Pending(p.code(), p.expires(), p.attempts() + 1));
                return "Incorrect code. Please try again.";
            }
            pending.remove(target);
            return null;
        }

        private static void sendSms(String phone, String code) throws Exception {
            String sender = cfg("SEMAPHORE_SENDER");
            StringBuilder body = new StringBuilder();
            body.append("apikey=").append(enc(cfg("SEMAPHORE_API_KEY")));
            body.append("&number=").append(enc(phone));
            body.append("&message=").append(enc(APP_NAME + ": Your verification code is " + code
                    + ". It expires in 5 minutes. Never share this code with anyone."));
            if (!sender.isEmpty()) body.append("&sendername=").append(enc(sender));

            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.semaphore.co/api/v4/messages"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new IOException("The SMS provider rejected the request (HTTP " + res.statusCode() + "). "
                        + "Check your SEMAPHORE_API_KEY and credits.");
            }
        }

        private static void sendEmail(String to, String code) throws Exception {
            final String user = cfg("GMAIL_USER");
            final String pass = cfg("GMAIL_APP_PASSWORD");

            Properties p = new Properties();
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.host", "smtp.gmail.com");
            p.put("mail.smtp.port", "587");
            p.put("mail.smtp.connectiontimeout", "15000");
            p.put("mail.smtp.timeout", "15000");

            Session session = Session.getInstance(p, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage m = new MimeMessage(session);
            m.setFrom(new InternetAddress(user, APP_NAME + " Healthcare System"));
            m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            m.setSubject("Your " + APP_NAME + " verification code");
            m.setText("Hello!\n\nYour " + APP_NAME + " verification code is: " + code
                    + "\n\nIt expires in 5 minutes. If you did not request this, please ignore this email."
                    + "\nNever share this code with anyone.\n\n- " + APP_NAME + " " + APP_SUB
                    + "\n" + SLOGAN_1 + " " + SLOGAN_2);
            Transport.send(m);
        }

        private static String enc(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }

}
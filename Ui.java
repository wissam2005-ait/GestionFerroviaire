import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

/** Utilitaires Swing partagés par tous les écrans : palette, boutons, tableaux, boîtes de dialogue. */
public final class Ui {
    public static final DateTimeFormatter HF = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Palette de l'application (bleu « ferroviaire »). */
    public static final Color PRIMARY = new Color(0x1E5AA8);
    public static final Color PRIMARY_DARK = new Color(0x123B70);
    public static final Color ACCENT = new Color(0x2E86C1);
    public static final Color DANGER = new Color(0xC0392B);
    public static final Color SUCCESS = new Color(0x27AE60);
    public static final Color WARN = new Color(0xE67E22);

    private Ui() { }

    // ---------------------------------------------------------------- couleurs (adaptées au thème clair / sombre)

    public static Color fg() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.BLACK;
    }

    public static Color bg() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : Color.WHITE;
    }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    /** Mélange a et b : t = 0 donne a, t = 1 donne b. */
    public static Color melange(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Couleur de texte secondaire (légendes, aides). */
    public static Color discret() { return melange(fg(), bg(), 0.4f); }

    /** Réglages globaux, appelés une fois après l'installation du thème. */
    public static void appliquerStyle() {
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("TabbedPane.showTabSeparators", true);
    }

    // ---------------------------------------------------------------- dialogues

    public static void erreur(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Erreur", JOptionPane.ERROR_MESSAGE); }
    public static void info(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Information", JOptionPane.INFORMATION_MESSAGE); }

    public static boolean confirmer(Component p, String m) {
        return JOptionPane.showConfirmDialog(p, m, "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Exécute une action en transformant toutes les erreurs attendues en boîtes de dialogue lisibles. */
    public static void safe(Component parent, Runnable action) {
        try {
            action.run();
        } catch (MetierException | DbException | SecurityException e) {
            erreur(parent, e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            erreur(parent, "Erreur inattendue : " + e);
        }
    }

    // ---------------------------------------------------------------- composants stylés

    /** Bouton arrondi peint par l'application : rendu identique quel que soit le thème Swing. */
    public static class Bouton extends JButton {
        public enum Style { PRIMAIRE, SECONDAIRE, DANGER }

        private Style style = Style.SECONDAIRE;
        private boolean survol;

        public Bouton(String texte) {
            super(texte);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(getFont().deriveFont(Font.BOLD));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { survol = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { survol = false; repaint(); }
            });
        }

        public Bouton style(Style s) { style = s; repaint(); return this; }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 32, Math.max(32, fm.getHeight() + 14));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean actif = isEnabled(), appuye = getModel().isPressed();
            if (!actif) g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
            Color base = style == Style.PRIMAIRE ? PRIMARY : style == Style.DANGER ? DANGER : null;
            Color texte;
            if (base != null) {
                g.setColor(appuye ? base.darker() : (survol && actif) ? melange(base, Color.WHITE, 0.12f) : base);
                g.fillRoundRect(0, 0, w, h, 10, 10);
                texte = Color.WHITE;
            } else {
                Color f = fg();
                if (appuye || (survol && actif)) {
                    g.setColor(alpha(f, appuye ? 40 : 18));
                    g.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                }
                g.setColor(alpha(f, 90));
                g.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                texte = f;
            }
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(texte);
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            if (hasFocus() && actif) {
                g.setColor(alpha(ACCENT, 170));
                g.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
            }
            g.dispose();
        }
    }

    public static Bouton bouton(String texte) { return new Bouton(texte); }
    public static void primaire(JButton b) { if (b instanceof Bouton) ((Bouton) b).style(Bouton.Style.PRIMAIRE); }
    public static void danger(JButton b) { if (b instanceof Bouton) ((Bouton) b).style(Bouton.Style.DANGER); }

    /** Petite étiquette arrondie colorée (rôle de l'utilisateur, statut…). */
    public static class Pastille extends JLabel {
        private final Color couleur;

        public Pastille(String texte, Color couleur) {
            super(texte, SwingConstants.CENTER);
            this.couleur = couleur;
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(couleur);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Panneau au fond dégradé diagonal (bandeau, écran de connexion). */
    public static class Degrade extends JPanel {
        private final Color a, b;

        public Degrade(Color a, Color b) { this.a = a; this.b = b; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(new GradientPaint(0, 0, a, getWidth(), getHeight(), b));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    /** Barre d'outils standard des écrans : espacée, avec un filet de séparation en bas. */
    public static JPanel barre() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, alpha(fg(), 40)));
        return p;
    }

    public static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        return s;
    }

    public static LocalDate dateOf(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ---------------------------------------------------------------- tableaux

    /** Modèle de tableau non éditable dont les colonnes se trient selon le vrai type des valeurs (nombres, etc.). */
    public static DefaultTableModel modele(String... colonnes) {
        return new DefaultTableModel(colonnes, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                Object v = getRowCount() > 0 ? getValueAt(0, c) : null;
                return v == null ? Object.class : v.getClass();
            }
        };
    }

    /** Tableau aux lignes alternées, sans quadrillage, avec sélection bleue et en-tête en gras. */
    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? getBackground() : melange(getBackground(), getForeground(), 0.05f));
                return c;
            }
        };
        t.setRowHeight(30);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(ACCENT);
        t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.getTableHeader().setPreferredSize(new Dimension(100, 34));
        return t;
    }

    public static DefaultTableCellRenderer euro() {
        return new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.RIGHT); }
            @Override protected void setValue(Object v) {
                setText(v instanceof Number ? String.format("%.2f €", ((Number) v).doubleValue()) : String.valueOf(v));
            }
        };
    }

    /** Places libres en couleur : vert (dispo), orange (≤ 10 % de la capacité), rouge (complet). */
    public static DefaultTableCellRenderer disponibilite(int colCapacite) {
        return new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.CENTER); }
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                if (!sel && v instanceof Number) {
                    int libres = ((Number) v).intValue();
                    Object cap = t.getModel().getValueAt(t.convertRowIndexToModel(row), colCapacite);
                    int total = cap instanceof Number ? ((Number) cap).intValue() : 0;
                    c.setForeground(libres <= 0 ? DANGER : (total > 0 && libres * 10 <= total) ? WARN : SUCCESS);
                }
                return c;
            }
        };
    }

    public static void surSaisie(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    /** Boîte de saisie masquée d'un mot de passe. Retourne null si l'utilisateur annule. */
    public static String demanderMotDePasse(Component parent, String titre, String libelle) {
        JPasswordField f = new JPasswordField(16);
        int r = JOptionPane.showConfirmDialog(parent, new Object[]{libelle, f}, titre, JOptionPane.OK_CANCEL_OPTION);
        return r == JOptionPane.OK_OPTION ? new String(f.getPassword()) : null;
    }
}

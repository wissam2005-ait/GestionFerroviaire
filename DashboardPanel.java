import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Tableau de bord administrateur : cartes d'indicateurs, graphique du chiffre d'affaires, classement des trajets. */
public class DashboardPanel extends JPanel implements Rafraichissable {
    private static final String[] TITRES = {"Trains", "Utilisateurs", "Réservations", "Places vendues", "Chiffre d'affaires"};
    private static final Color[] COULEURS = {new Color(0x2E86C1), new Color(0x8E44AD), new Color(0xE67E22), new Color(0x16A085), new Color(0x27AE60)};

    private final Carte[] cartes = new Carte[TITRES.length];
    private final BarChart chart = new BarChart();
    private final DefaultTableModel m = Ui.modele("Trajet", "Places vendues", "Chiffre d'affaires");
    private final JTable t = Ui.table(m);

    public DashboardPanel() {
        super(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel ligne = new JPanel(new GridLayout(1, cartes.length, 12, 0));
        for (int i = 0; i < cartes.length; i++) {
            cartes[i] = new Carte(TITRES[i], COULEURS[i]);
            ligne.add(cartes[i]);
        }
        t.getColumnModel().getColumn(2).setCellRenderer(Ui.euro());
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new Dimension(100, 170));
        add(ligne, BorderLayout.NORTH);
        add(chart, BorderLayout.CENTER);
        add(sp, BorderLayout.SOUTH);
    }

    @Override public void refresh() {
        double[] k = StatsDao.kpis();
        String[] valeurs = {String.valueOf(TrainDao.compter()), String.valueOf(UtilisateurDao.compter()),
                String.valueOf((int) k[0]), String.valueOf((int) k[1]), String.format("%.2f €", k[2])};
        for (int i = 0; i < cartes.length; i++) cartes[i].setValeur(valeurs[i]);
        List<StatTrajet> stats = StatsDao.parTrajet();
        chart.setData(stats);
        m.setRowCount(0);
        for (StatTrajet s : stats) m.addRow(new Object[]{s.trajet, s.places, s.ca});
    }

    /** Carte d'indicateur : liseré coloré à gauche, libellé, grande valeur. */
    private static class Carte extends JPanel {
        private final Color accent;
        private final JLabel valeur = new JLabel("–");

        Carte(String titre, Color accent) {
            super(new BorderLayout(0, 4));
            this.accent = accent;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 8));
            JLabel l = new JLabel(titre.toUpperCase());
            l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
            l.setForeground(Ui.discret());
            valeur.setFont(valeur.getFont().deriveFont(Font.BOLD, 22f));
            add(l, BorderLayout.NORTH);
            add(valeur, BorderLayout.CENTER);
        }

        void setValeur(String v) { valeur.setText(v); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(Ui.melange(Ui.bg(), Ui.fg(), 0.05f));
            g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setColor(Ui.alpha(Ui.fg(), 40));
            g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setColor(accent);
            g.fillRoundRect(0, 10, 5, h - 20, 5, 5);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Histogramme du chiffre d'affaires par trajet, dessiné avec Java2D (aucune bibliothèque). */
    private static class BarChart extends JPanel {
        private List<StatTrajet> data = new ArrayList<>();

        BarChart() { setBorder(BorderFactory.createLineBorder(Ui.alpha(Ui.fg(), 40))); }

        void setData(List<StatTrajet> d) { data = d; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), gauche = 70, bas = 42, haut = 56, droite = 24;
            Color fg = getForeground();
            g.setColor(fg);
            g.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g.drawString("Chiffre d'affaires par trajet", 20, 26);
            g.setFont(getFont().deriveFont(12f));
            FontMetrics fm = g.getFontMetrics();
            if (data.isEmpty()) {
                g.setColor(Ui.discret());
                String msg = "Aucune réservation pour le moment.";
                g.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g.dispose();
                return;
            }
            double max = data.stream().mapToDouble(s -> s.ca).max().orElse(1);
            if (max <= 0) max = 1;
            int zoneH = h - bas - haut, ticks = 4;
            for (int i = 0; i <= ticks; i++) {
                int y = h - bas - zoneH * i / ticks;
                g.setColor(Ui.alpha(fg, i == 0 ? 130 : 35));
                g.drawLine(gauche, y, w - droite, y);
                String lbl = String.format("%.0f €", max * i / ticks);
                g.setColor(Ui.discret());
                g.drawString(lbl, gauche - 8 - fm.stringWidth(lbl), y + fm.getAscent() / 2 - 1);
            }
            int n = data.size(), zone = w - gauche - droite, slot = Math.max(1, zone / n), bw = Math.max(10, Math.min(64, slot * 3 / 5));
            for (int i = 0; i < n; i++) {
                StatTrajet s = data.get(i);
                int bh = (int) (zoneH * (s.ca / max));
                if (s.ca > 0) bh = Math.max(bh, 3);
                int x = gauche + i * slot + (slot - bw) / 2, y = h - bas - bh;
                g.setPaint(new GradientPaint(x, y, Ui.ACCENT, x, y + Math.max(bh, 1), Ui.PRIMARY_DARK));
                g.fillRoundRect(x, y, bw, bh + 6, 8, 8);
                g.setColor(getBackground());
                g.fillRect(x - 1, h - bas + 1, bw + 2, 8);
                g.setColor(Ui.alpha(fg, 130));
                g.drawLine(gauche, h - bas, w - droite, h - bas);
                g.setColor(fg);
                String val = String.format("%.0f €", s.ca);
                g.drawString(val, x + (bw - fm.stringWidth(val)) / 2, y - 6);
                String lib = s.trajet;
                while (lib.length() > 3 && fm.stringWidth(lib) > slot - 6) lib = lib.substring(0, lib.length() - 2) + "…";
                g.setColor(Ui.discret());
                g.drawString(lib, gauche + i * slot + Math.max(0, (slot - fm.stringWidth(lib)) / 2), h - bas + 18);
            }
            g.dispose();
        }
    }
}

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;

/** Fenêtre principale : les onglets affichés dépendent du rôle de l'utilisateur connecté. */
public class MainFrame extends JFrame {
    private final List<Rafraichissable> panels = new ArrayList<>();

    public MainFrame() {
        super("Gestion & Planification Ferroviaire");
        Utilisateur u = Session.courant();
        setJMenuBar(creerMenu());

        Runnable maj = this::refreshAll;
        JTabbedPane tabs = new JTabbedPane();
        ajouter(tabs, "Horaires", new HorairesPanel(maj));
        ajouter(tabs, "Planifier un trajet", new PlanificationPanel());
        ajouter(tabs, u.isAdmin() ? "Réservations (toutes)" : "Mes réservations", new ReservationsPanel(maj));
        if (u.isAdmin()) {
            ajouter(tabs, "Tableau de bord", new DashboardPanel());
            ajouter(tabs, "Utilisateurs", new UtilisateursPanel(maj));
            ajouter(tabs, "Journal", new LogsPanel());
        }

        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 13f));
        tabs.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        add(creerEntete(u), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        refreshAll();
        setSize(1150, 680);
        setMinimumSize(new java.awt.Dimension(900, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { LogDao.log("Fermeture de l'application"); }
        });
    }

    /** Bandeau supérieur : nom de l'application, utilisateur connecté et son rôle. */
    private JPanel creerEntete(Utilisateur u) {
        Ui.Degrade entete = new Ui.Degrade(Ui.PRIMARY_DARK, Ui.PRIMARY);
        entete.setLayout(new BorderLayout());
        entete.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel appli = new JLabel("🚆  Gestion & Planification Ferroviaire");
        appli.setForeground(Color.WHITE);
        appli.setFont(appli.getFont().deriveFont(Font.BOLD, 17f));
        JLabel nom = new JLabel(u.getNomComplet());
        nom.setForeground(Color.WHITE);
        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        droite.setOpaque(false);
        droite.add(nom);
        droite.add(new Ui.Pastille(u.isAdmin() ? "ADMINISTRATEUR" : "CLIENT", u.isAdmin() ? Ui.WARN : Ui.SUCCESS));
        entete.add(appli, BorderLayout.WEST);
        entete.add(droite, BorderLayout.EAST);
        return entete;
    }

    private void ajouter(JTabbedPane tabs, String titre, JComponent c) {
        tabs.addTab(titre, c);
        if (c instanceof Rafraichissable) panels.add((Rafraichissable) c);
    }

    /** Relit toutes les données depuis MySQL. */
    void refreshAll() {
        Ui.safe(this, () -> { for (Rafraichissable p : panels) p.refresh(); });
    }

    private JMenuBar creerMenu() {
        JMenu compte = new JMenu("Compte");
        JMenuItem mdp = new JMenuItem("Changer mon mot de passe"), deco = new JMenuItem("Se déconnecter"), quitter = new JMenuItem("Quitter");
        mdp.addActionListener(e -> Ui.safe(this, this::changerMotDePasse));
        deco.addActionListener(e -> {
            LogDao.log("Déconnexion");
            Session.fermer();
            dispose();
            new LoginFrame().setVisible(true);
        });
        quitter.addActionListener(e -> { LogDao.log("Fermeture de l'application"); System.exit(0); });
        compte.add(mdp); compte.add(deco); compte.addSeparator(); compte.add(quitter);

        JMenu aide = new JMenu("Aide");
        JMenuItem apropos = new JMenuItem("À propos");
        apropos.addActionListener(e -> Ui.info(this, "Gestion & Planification Ferroviaire\nJava Swing • MySQL • Authentification PBKDF2 • Rôles Admin/Client"));
        aide.add(apropos);

        JMenuBar bar = new JMenuBar();
        bar.add(compte); bar.add(aide);
        return bar;
    }

    private void changerMotDePasse() {
        JPasswordField ancien = new JPasswordField(), n1 = new JPasswordField(), n2 = new JPasswordField();
        Object[] f = {"Ancien mot de passe", ancien, "Nouveau mot de passe (8 car. min., lettre + chiffre)", n1, "Confirmer", n2};
        if (JOptionPane.showConfirmDialog(this, f, "Changer mon mot de passe", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String nouveau = new String(n1.getPassword());
        if (!nouveau.equals(new String(n2.getPassword())))
            throw new MetierException("Les deux mots de passe ne correspondent pas.");
        UtilisateurDao.changerMonMotDePasse(new String(ancien.getPassword()), nouveau);
        Ui.info(this, "Mot de passe modifié.");
    }
}

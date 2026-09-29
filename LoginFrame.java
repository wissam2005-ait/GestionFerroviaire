import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Écran de connexion (panneau de marque à gauche, formulaire à droite) + création de compte client. */
public class LoginFrame extends JFrame {
    private final JTextField login = new JTextField(18);
    private final JPasswordField pwd = new JPasswordField(18);

    public LoginFrame() {
        super("Connexion — Gestion & Planification Ferroviaire");
        setLayout(new BorderLayout());
        add(panneauMarque(), BorderLayout.WEST);
        add(panneauFormulaire(), BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private JPanel panneauMarque() {
        Ui.Degrade p = new Ui.Degrade(Ui.PRIMARY_DARK, Ui.ACCENT);
        p.setLayout(new GridBagLayout());
        p.setPreferredSize(new Dimension(300, 460));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.insets = new Insets(6, 28, 6, 28);

        JLabel logo = new JLabel("🚆", SwingConstants.CENTER);
        logo.setFont(logo.getFont().deriveFont(64f));
        logo.setForeground(Color.WHITE);
        JLabel titre = new JLabel("Gestion Ferroviaire", SwingConstants.CENTER);
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 24f));
        titre.setForeground(Color.WHITE);
        JLabel sous = new JLabel("<html><div style='text-align:center;width:200px'>Horaires, réservations et planification de trajets en un seul endroit.</div></html>", SwingConstants.CENTER);
        sous.setForeground(new Color(255, 255, 255, 210));
        c.gridy = 0; p.add(logo, c);
        c.gridy = 1; p.add(titre, c);
        c.gridy = 2; p.add(sous, c);
        return p;
    }

    private JPanel panneauFormulaire() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(32, 44, 32, 44));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;

        JLabel titre = new JLabel("Connexion");
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 24f));
        c.gridy = y++; c.insets = new Insets(0, 0, 2, 0); p.add(titre, c);
        c.gridy = y++; c.insets = new Insets(0, 0, 22, 0); p.add(discret("Entrez vos identifiants pour continuer"), c);

        for (JTextField f : new JTextField[]{login, pwd}) {
            f.setMargin(new Insets(6, 10, 6, 10));
            f.setPreferredSize(new Dimension(260, 36));
        }
        c.gridy = y++; c.insets = new Insets(0, 0, 4, 0); p.add(etiquette("Identifiant"), c);
        c.gridy = y++; c.insets = new Insets(0, 0, 14, 0); p.add(login, c);
        c.gridy = y++; c.insets = new Insets(0, 0, 4, 0); p.add(etiquette("Mot de passe"), c);
        c.gridy = y++; c.insets = new Insets(0, 0, 6, 0); p.add(pwd, c);

        JCheckBox voir = new JCheckBox("Afficher le mot de passe");
        char masque = pwd.getEchoChar();
        voir.addActionListener(e -> pwd.setEchoChar(voir.isSelected() ? (char) 0 : masque));
        c.gridy = y++; c.insets = new Insets(0, 0, 18, 0); p.add(voir, c);

        Ui.Bouton ok = Ui.bouton("Se connecter");
        ok.style(Ui.Bouton.Style.PRIMAIRE);
        ok.setPreferredSize(new Dimension(260, 40));
        ok.addActionListener(e -> Ui.safe(this, this::connecter));
        c.gridy = y++; c.insets = new Insets(0, 0, 16, 0); p.add(ok, c);

        c.gridy = y++; c.insets = new Insets(0, 0, 6, 0); p.add(discret("Pas encore de compte ?"), c);
        Ui.Bouton inscrire = Ui.bouton("Créer un compte");
        inscrire.setPreferredSize(new Dimension(260, 36));
        inscrire.addActionListener(e -> Ui.safe(this, this::inscription));
        c.gridy = y++; c.insets = new Insets(0, 0, 0, 0); p.add(inscrire, c);

        if (Config.get("ui.demo_hint", "true").equalsIgnoreCase("true")) {
            JLabel hint = discret("Démo : admin / admin123  •  client / client123");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            c.gridy = y++; c.insets = new Insets(18, 0, 0, 0); p.add(hint, c);
        }
        getRootPane().setDefaultButton(ok);
        return p;
    }

    private static JLabel etiquette(String t) {
        JLabel l = new JLabel(t);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        return l;
    }

    private static JLabel discret(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Ui.discret());
        return l;
    }

    private void connecter() {
        Auth.connexion(login.getText(), new String(pwd.getPassword()));
        pwd.setText("");
        new MainFrame().setVisible(true);
        dispose();
    }

    private void inscription() {
        JTextField l = new JTextField(), nom = new JTextField();
        JPasswordField p1 = new JPasswordField(), p2 = new JPasswordField();
        Object[] f = {"Identifiant (3 à 30 car.)", l, "Nom complet", nom,
                "Mot de passe (8 car. min., lettre + chiffre)", p1, "Confirmer le mot de passe", p2};
        if (JOptionPane.showConfirmDialog(this, f, "Créer un compte", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String a = new String(p1.getPassword());
        if (!a.equals(new String(p2.getPassword()))) throw new MetierException("Les deux mots de passe ne correspondent pas.");
        UtilisateurDao.inscrire(l.getText(), a, nom.getText());
        login.setText(l.getText().trim());
        pwd.requestFocus();
        Ui.info(this, "Compte créé. Vous pouvez maintenant vous connecter.");
    }
}

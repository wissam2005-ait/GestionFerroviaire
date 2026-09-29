import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Gestion des comptes (administrateur uniquement). */
public class UtilisateursPanel extends JPanel implements Rafraichissable {
    private final DefaultTableModel m = Ui.modele("N°", "Identifiant", "Nom complet", "Rôle", "Actif", "Créé le");
    private final JTable t = Ui.table(m);
    private final Runnable onChange;
    private List<Utilisateur> liste = new ArrayList<>();

    public UtilisateursPanel(Runnable onChange) {
        super(new BorderLayout());
        this.onChange = onChange;
        JButton add = Ui.bouton("Nouveau compte"), role = Ui.bouton("Changer le rôle"),
                actif = Ui.bouton("Activer / Désactiver"), mdp = Ui.bouton("Réinitialiser le mot de passe");
        Ui.primaire(add);
        add.addActionListener(e -> Ui.safe(this, this::nouveau));
        role.addActionListener(e -> Ui.safe(this, () -> {
            Utilisateur u = selection();
            Role nouveau = u.isAdmin() ? Role.CLIENT : Role.ADMIN;
            if (Ui.confirmer(this, "Passer '" + u.getLogin() + "' en " + nouveau + " ?")) {
                UtilisateurDao.changerRole(u.getId(), nouveau, u.getLogin());
                onChange.run();
            }
        }));
        actif.addActionListener(e -> Ui.safe(this, () -> {
            Utilisateur u = selection();
            UtilisateurDao.definirActif(u.getId(), !u.isActif(), u.getLogin());
            onChange.run();
        }));
        mdp.addActionListener(e -> Ui.safe(this, () -> {
            Utilisateur u = selection();
            String nouveau = Ui.demanderMotDePasse(this, "Réinitialisation", "Nouveau mot de passe pour '" + u.getLogin() + "' :");
            if (nouveau != null) {
                UtilisateurDao.reinitialiserMotDePasse(u.getId(), nouveau, u.getLogin());
                Ui.info(this, "Mot de passe modifié.");
            }
        }));
        JPanel top = Ui.barre();
        top.add(add); top.add(role); top.add(actif); top.add(mdp);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(t), BorderLayout.CENTER);
    }

    @Override public void refresh() {
        liste = UtilisateurDao.tous();
        m.setRowCount(0);
        for (Utilisateur u : liste)
            m.addRow(new Object[]{u.getId(), u.getLogin(), u.getNomComplet(), u.getRole().name(),
                    u.isActif() ? "Oui" : "Non", u.getCreeLe().format(Ui.DTF)});
    }

    private Utilisateur selection() {
        int r = t.getSelectedRow();
        if (r < 0) throw new MetierException("Sélectionnez d'abord un utilisateur.");
        int id = (int) m.getValueAt(t.convertRowIndexToModel(r), 0);
        return liste.stream().filter(x -> x.getId() == id).findFirst().orElseThrow(() -> new MetierException("Utilisateur introuvable."));
    }

    private void nouveau() {
        JTextField login = new JTextField(), nom = new JTextField();
        JPasswordField pwd = new JPasswordField();
        JComboBox<Role> cb = new JComboBox<>(Role.values());
        cb.setSelectedItem(Role.CLIENT);
        Object[] f = {"Identifiant", login, "Nom complet", nom, "Mot de passe (8 car. min., lettre + chiffre)", pwd, "Rôle", cb};
        if (JOptionPane.showConfirmDialog(this, f, "Nouveau compte", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        UtilisateurDao.creer(login.getText(), new String(pwd.getPassword()), (Role) cb.getSelectedItem(), nom.getText());
        onChange.run();
    }
}

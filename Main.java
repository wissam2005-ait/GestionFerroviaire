import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Point d'entrée : thème, vérification de la base, écran de connexion. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installerTheme();
            try {
                Db.get();
                UtilisateurDao.creerComptesParDefautSiVide();
            } catch (DbException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Erreur de base de données", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }

    /**
     * ui.theme = light | dark | system.
     * light / dark : FlatLaf si le jar est dans lib/, sinon thème Nimbus aux couleurs de l'application.
     * system : apparence native du système.
     */
    private static void installerTheme() {
        String theme = Config.get("ui.theme", "light").toLowerCase();
        try {
            if (theme.equals("system")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                boolean sombre = theme.equals("dark");
                try {
                    UIManager.setLookAndFeel(sombre ? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf");
                } catch (Exception flatAbsent) {
                    installerNimbus(sombre);
                }
            }
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        }
        Ui.appliquerStyle();
    }

    /** Repli sans dépendance : Nimbus, teinté avec la palette de l'application (clair ou sombre). */
    private static void installerNimbus(boolean sombre) throws Exception {
        UIManager.put("nimbusBase", Ui.PRIMARY);
        UIManager.put("nimbusSelectionBackground", Ui.ACCENT);
        if (sombre) {
            UIManager.put("control", new Color(0x2B2F36));
            UIManager.put("nimbusLightBackground", new Color(0x1F2227));
            UIManager.put("text", new Color(0xE6E6E6));
            UIManager.put("nimbusBase", Ui.PRIMARY_DARK);
        }
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); return; }
        }
        throw new ClassNotFoundException("Nimbus indisponible");
    }
}

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class UtilisateurDao {
    private static final Pattern LOGIN = Pattern.compile("[A-Za-z0-9_.-]{3,30}");
    private static final String COLS = "id, login, mot_de_passe, nom_complet, role, actif, cree_le";

    private UtilisateurDao() { }

    private static Utilisateur map(ResultSet rs) throws SQLException {
        return new Utilisateur(rs.getInt("id"), rs.getString("login"), rs.getString("mot_de_passe"),
                rs.getString("nom_complet"), Role.valueOf(rs.getString("role")), rs.getBoolean("actif"),
                rs.getTimestamp("cree_le").toLocalDateTime());
    }

    public static Utilisateur trouverParLogin(String login) {
        try (PreparedStatement ps = Db.get().prepareStatement("SELECT " + COLS + " FROM utilisateurs WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) { throw new DbException("Lecture de l'utilisateur impossible : " + e.getMessage(), e); }
    }

    public static long compter() {
        try (Statement st = Db.get().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utilisateurs")) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) { throw new DbException("Comptage des utilisateurs impossible : " + e.getMessage(), e); }
    }

    /** Au tout premier lancement, crée un compte admin et un compte client de démonstration. */
    public static void creerComptesParDefautSiVide() {
        if (compter() == 0) {
            insert("admin", "admin123", Role.ADMIN, "Administrateur");
            insert("client", "client123", Role.CLIENT, "Client Démo");
            System.out.println("Comptes de démonstration créés : admin/admin123 et client/client123 (à changer !)");
        }
    }

    /** Inscription publique : le rôle est toujours CLIENT. */
    public static void inscrire(String login, String motDePasse, String nomComplet) {
        insert(login, motDePasse, Role.CLIENT, nomComplet);
        LogDao.log(login, "Création de compte (inscription)");
    }

    /** Création par un administrateur (tous rôles). */
    public static void creer(String login, String motDePasse, Role role, String nomComplet) {
        Session.exigerAdmin();
        insert(login, motDePasse, role, nomComplet);
        LogDao.log("Création du compte '" + login + "' (" + role + ")");
    }

    private static void insert(String login, String motDePasse, Role role, String nom) {
        login = login == null ? "" : login.trim();
        if (!LOGIN.matcher(login).matches())
            throw new MetierException("Identifiant invalide : 3 à 30 caractères (lettres, chiffres, . _ -).");
        Passwords.valider(motDePasse);
        try (PreparedStatement ps = Db.get().prepareStatement(
                "INSERT INTO utilisateurs (login, mot_de_passe, nom_complet, role) VALUES (?,?,?,?)")) {
            ps.setString(1, login);
            ps.setString(2, Passwords.hash(motDePasse));
            ps.setString(3, nom == null ? "" : nom.trim());
            ps.setString(4, role.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new MetierException("Cet identifiant est déjà utilisé.");
            throw new DbException("Création du compte impossible : " + e.getMessage(), e);
        }
    }

    public static List<Utilisateur> tous() {
        Session.exigerAdmin();
        List<Utilisateur> l = new ArrayList<>();
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT " + COLS + " FROM utilisateurs ORDER BY id")) {
            while (rs.next()) l.add(map(rs));
        } catch (SQLException e) { throw new DbException("Lecture des utilisateurs impossible : " + e.getMessage(), e); }
        return l;
    }

    private static void interdireSoiMeme(int id, String message) {
        if (Session.courant() != null && Session.courant().getId() == id) throw new MetierException(message);
    }

    private static void maj(String sql, Object valeur, int id, String erreur) {
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setObject(1, valeur);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DbException(erreur + " : " + e.getMessage(), e); }
    }

    public static void changerRole(int id, Role role, String login) {
        Session.exigerAdmin();
        interdireSoiMeme(id, "Vous ne pouvez pas modifier votre propre rôle.");
        maj("UPDATE utilisateurs SET role = ? WHERE id = ?", role.name(), id, "Changement de rôle impossible");
        LogDao.log("Rôle de '" + login + "' → " + role);
    }

    public static void definirActif(int id, boolean actif, String login) {
        Session.exigerAdmin();
        interdireSoiMeme(id, "Vous ne pouvez pas désactiver votre propre compte.");
        maj("UPDATE utilisateurs SET actif = ? WHERE id = ?", actif, id, "Mise à jour du compte impossible");
        LogDao.log((actif ? "Activation" : "Désactivation") + " du compte '" + login + "'");
    }

    public static void reinitialiserMotDePasse(int id, String nouveau, String login) {
        Session.exigerAdmin();
        Passwords.valider(nouveau);
        maj("UPDATE utilisateurs SET mot_de_passe = ? WHERE id = ?", Passwords.hash(nouveau), id, "Réinitialisation impossible");
        LogDao.log("Réinitialisation du mot de passe de '" + login + "'");
    }

    /** Changement de son propre mot de passe (l'ancien est vérifié). */
    public static void changerMonMotDePasse(String ancien, String nouveau) {
        Session.exigerConnecte();
        Utilisateur u = trouverParLogin(Session.login());
        if (u == null || !Passwords.verifier(ancien, u.getHash())) throw new MetierException("Ancien mot de passe incorrect.");
        Passwords.valider(nouveau);
        maj("UPDATE utilisateurs SET mot_de_passe = ? WHERE id = ?", Passwords.hash(nouveau), u.getId(), "Changement impossible");
        LogDao.log("Changement de son mot de passe");
    }
}

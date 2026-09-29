import java.util.HashMap;
import java.util.Map;

/** Authentification avec verrouillage temporaire après trop d'échecs. */
public final class Auth {
    private static final int MAX_ECHECS = 5;
    private static final long VERROU_MS = 30_000;
    private static final Map<String, Integer> ECHECS = new HashMap<>();
    private static final Map<String, Long> VERROUS = new HashMap<>();

    private Auth() { }

    public static Utilisateur connexion(String login, String motDePasse) {
        String cle = login.trim().toLowerCase();
        Long jusqua = VERROUS.get(cle);
        if (jusqua != null) {
            long reste = jusqua - System.currentTimeMillis();
            if (reste > 0) throw new MetierException("Trop de tentatives. Réessayez dans " + (reste / 1000 + 1) + " s.");
            VERROUS.remove(cle);
            ECHECS.remove(cle);
        }
        Utilisateur u = UtilisateurDao.trouverParLogin(login.trim());
        boolean ok = u != null && Passwords.verifier(motDePasse, u.getHash());
        if (!ok) {
            int n = ECHECS.merge(cle, 1, Integer::sum);
            if (n >= MAX_ECHECS) {
                VERROUS.put(cle, System.currentTimeMillis() + VERROU_MS);
                ECHECS.remove(cle);
            }
            LogDao.log("anonyme", "Échec de connexion pour '" + login.trim() + "'");
            throw new MetierException("Identifiant ou mot de passe incorrect.");
        }
        if (!u.isActif()) throw new MetierException("Ce compte est désactivé. Contactez un administrateur.");
        ECHECS.remove(cle);
        Session.ouvrir(u);
        LogDao.log("Connexion");
        return u;
    }
}

/** Utilisateur actuellement connecté + contrôles d'autorisation utilisés par la couche DAO. */
public final class Session {
    private static Utilisateur courant;

    private Session() { }

    public static void ouvrir(Utilisateur u) { courant = u; }
    public static void fermer() { courant = null; }
    public static Utilisateur courant() { return courant; }
    public static boolean estAdmin() { return courant != null && courant.isAdmin(); }
    public static String login() { return courant == null ? "anonyme" : courant.getLogin(); }

    public static void exigerConnecte() {
        if (courant == null) throw new SecurityException("Vous devez être connecté.");
    }

    /** Les réservations sont réservées aux clients : l'administrateur gère, il ne réserve pas. */
    public static void exigerClient() {
        exigerConnecte();
        if (estAdmin()) throw new SecurityException("L'administrateur ne peut pas réserver : il gère les trains et les réservations.");
    }

    public static void exigerAdmin() {
        if (!estAdmin()) throw new SecurityException("Action réservée aux administrateurs.");
    }
}

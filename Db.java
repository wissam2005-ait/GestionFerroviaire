import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Connexion MySQL partagée (reconnexion automatique si elle est perdue). */
public final class Db {
    private static Connection conn;

    private Db() { }

    public static synchronized Connection get() {
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                conn = DriverManager.getConnection(
                        Config.get("db.url", "jdbc:mysql://localhost:3306/gestion_ferroviaire?useSSL=false&serverTimezone=UTC"),
                        Config.get("db.user", "root"),
                        Config.get("db.password", ""));
            }
            return conn;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("No suitable driver"))
                throw new DbException("Pilote MySQL introuvable.\nPlace mysql-connector-j-x.x.x.jar dans le dossier lib/.", e);
            throw new DbException("Connexion à MySQL impossible.\n\n"
                    + "Vérifie que :\n"
                    + "- XAMPP est démarré (MySQL)\n"
                    + "- la base 'gestion_ferroviaire' existe (exécute schema.sql)\n"
                    + "- config.properties contient le bon utilisateur / mot de passe\n\n"
                    + "Détail : " + e.getMessage(), e);
        }
    }
}

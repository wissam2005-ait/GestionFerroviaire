import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/** Journal d'audit : qui a fait quoi, et quand. */
public final class LogDao {
    private LogDao() { }

    public static void log(String action) { log(Session.login(), action); }

    public static void log(String utilisateur, String action) {
        String a = action.length() > 250 ? action.substring(0, 250) : action;
        try (PreparedStatement ps = Db.get().prepareStatement("INSERT INTO logs (utilisateur, action) VALUES (?,?)")) {
            ps.setString(1, utilisateur);
            ps.setString(2, a);
            ps.executeUpdate();
        } catch (RuntimeException | SQLException e) {
            System.err.println("Journal indisponible : " + e.getMessage()); // un log ne doit jamais bloquer l'appli
        }
    }

    /** Lignes : {date, utilisateur, action} — administrateur uniquement. */
    public static List<Object[]> derniers(int limite) {
        Session.exigerAdmin();
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT date_log, utilisateur, action FROM logs ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    l.add(new Object[]{rs.getTimestamp(1).toLocalDateTime().toString().replace('T', ' '), rs.getString(2), rs.getString(3)});
            }
        } catch (SQLException e) { throw new DbException("Lecture du journal impossible : " + e.getMessage(), e); }
        return l;
    }
}

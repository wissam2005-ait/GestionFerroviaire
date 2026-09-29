import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Statistiques du tableau de bord (administrateur uniquement). */
public final class StatsDao {
    private StatsDao() { }

    /** {nombre de réservations, places vendues, chiffre d'affaires}. */
    public static double[] kpis() {
        Session.exigerAdmin();
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(r.nb_places), 0), COALESCE(SUM(r.nb_places * t.prix), 0) "
                     + "FROM reservations r JOIN trains t ON t.id = r.train_id")) {
            rs.next();
            return new double[]{rs.getDouble(1), rs.getDouble(2), rs.getDouble(3)};
        } catch (SQLException e) { throw new DbException("Calcul des statistiques impossible : " + e.getMessage(), e); }
    }

    public static List<StatTrajet> parTrajet() {
        Session.exigerAdmin();
        List<StatTrajet> l = new ArrayList<>();
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT CONCAT(t.depart, ' → ', t.arrivee) AS trajet, SUM(r.nb_places) AS places, "
                     + "SUM(r.nb_places * t.prix) AS ca FROM reservations r JOIN trains t ON t.id = r.train_id "
                     + "GROUP BY t.depart, t.arrivee ORDER BY ca DESC")) {
            while (rs.next()) l.add(new StatTrajet(rs.getString(1), rs.getInt(2), rs.getDouble(3)));
        } catch (SQLException e) { throw new DbException("Calcul des statistiques impossible : " + e.getMessage(), e); }
        return l;
    }
}

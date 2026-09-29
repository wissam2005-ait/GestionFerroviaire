import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TrainDao {
    private TrainDao() { }

    /** Tous les trains, avec le nombre de places déjà réservées pour la date de voyage donnée. */
    public static List<Train> tous(LocalDate date) {
        String sql = "SELECT t.id, t.depart, t.arrivee, t.heure_depart, t.heure_arrivee, t.prix, t.places, COALESCE(r.n, 0) AS reservees "
                + "FROM trains t LEFT JOIN (SELECT train_id, SUM(nb_places) AS n FROM reservations WHERE date_voyage = ? GROUP BY train_id) r "
                + "ON r.train_id = t.id ORDER BY t.heure_depart, t.id";
        List<Train> l = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    l.add(new Train(rs.getInt("id"), rs.getString("depart"), rs.getString("arrivee"),
                            rs.getTime("heure_depart").toLocalTime(), rs.getTime("heure_arrivee").toLocalTime(),
                            rs.getDouble("prix"), rs.getInt("places"), rs.getInt("reservees")));
            }
        } catch (SQLException e) { throw new DbException("Lecture des trains impossible : " + e.getMessage(), e); }
        return l;
    }

    public static int compter() {
        try (Statement st = Db.get().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM trains")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) { throw new DbException("Comptage des trains impossible : " + e.getMessage(), e); }
    }

    private static void valider(Train t) {
        if (t.dep.isBlank() || t.arr.isBlank() || t.dep.equalsIgnoreCase(t.arr))
            throw new MetierException("Gares invalides (non vides et différentes).");
        if (t.hd.equals(t.ha)) throw new MetierException("Les heures de départ et d'arrivée doivent différer.");
        if (t.prix < 0 || t.places <= 0) throw new MetierException("Prix ou capacité invalide.");
    }

    public static void inserer(Train t) {
        Session.exigerAdmin();
        valider(t);
        try (PreparedStatement ps = Db.get().prepareStatement(
                "INSERT INTO trains (depart, arrivee, heure_depart, heure_arrivee, prix, places) VALUES (?,?,?,?,?,?)")) {
            remplir(ps, t);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DbException("Ajout du train impossible : " + e.getMessage(), e); }
        LogDao.log("Ajout du train " + t.dep + " → " + t.arr);
    }

    public static void modifier(Train t) {
        Session.exigerAdmin();
        valider(t);
        int max = maxReserveFutur(t.id);
        if (t.places < max)
            throw new MetierException("Capacité inférieure aux places déjà réservées (" + max + " sur une date à venir).");
        try (PreparedStatement ps = Db.get().prepareStatement(
                "UPDATE trains SET depart=?, arrivee=?, heure_depart=?, heure_arrivee=?, prix=?, places=? WHERE id=?")) {
            remplir(ps, t);
            ps.setInt(7, t.id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DbException("Modification du train impossible : " + e.getMessage(), e); }
        LogDao.log("Modification du train N°" + t.id);
    }

    public static void supprimer(int id) {
        Session.exigerAdmin();
        try (PreparedStatement ps = Db.get().prepareStatement("DELETE FROM trains WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate(); // les réservations liées partent via ON DELETE CASCADE
        } catch (SQLException e) { throw new DbException("Suppression du train impossible : " + e.getMessage(), e); }
        LogDao.log("Suppression du train N°" + id);
    }

    /** Nombre de réservations (à venir) qui disparaîtraient si le train était supprimé. */
    public static int reservationsFutures(int trainId) {
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT COUNT(*) FROM reservations WHERE train_id = ? AND date_voyage >= CURDATE()")) {
            ps.setInt(1, trainId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DbException("Lecture impossible : " + e.getMessage(), e); }
    }

    private static int maxReserveFutur(int trainId) {
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT COALESCE(MAX(s), 0) FROM (SELECT SUM(nb_places) AS s FROM reservations "
                        + "WHERE train_id = ? AND date_voyage >= CURDATE() GROUP BY date_voyage) x")) {
            ps.setInt(1, trainId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DbException("Lecture impossible : " + e.getMessage(), e); }
    }

    private static void remplir(PreparedStatement ps, Train t) throws SQLException {
        ps.setString(1, t.dep);
        ps.setString(2, t.arr);
        ps.setTime(3, Time.valueOf(t.hd));
        ps.setTime(4, Time.valueOf(t.ha));
        ps.setDouble(5, t.prix);
        ps.setInt(6, t.places);
    }
}

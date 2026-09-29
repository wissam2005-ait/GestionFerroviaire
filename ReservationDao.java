import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class ReservationDao {
    public static final int MAX_PLACES_PAR_RESERVATION = 10;

    private ReservationDao() { }

    /**
     * Réserve des places pour un train à une date donnée.
     * Le train est verrouillé (SELECT ... FOR UPDATE) le temps de la transaction : deux clients ne peuvent
     * donc jamais dépasser la capacité, même en réservant en même temps.
     */
    public static int reserver(int trainId, String passager, int nb, LocalDate date) {
        Session.exigerClient();
        if (passager == null || passager.isBlank()) throw new MetierException("Saisissez le nom du passager.");
        if (nb < 1 || nb > MAX_PLACES_PAR_RESERVATION)
            throw new MetierException("Vous pouvez réserver de 1 à " + MAX_PLACES_PAR_RESERVATION + " places à la fois.");
        if (date.isBefore(LocalDate.now())) throw new MetierException("La date de voyage est dans le passé.");

        Connection c = Db.get();
        try {
            c.setAutoCommit(false);
            try {
                int places;
                LocalTime heure;
                try (PreparedStatement p = c.prepareStatement("SELECT places, heure_depart FROM trains WHERE id = ? FOR UPDATE")) {
                    p.setInt(1, trainId);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) throw new MetierException("Train introuvable.");
                        places = rs.getInt(1);
                        heure = rs.getTime(2).toLocalTime();
                    }
                }
                if (date.equals(LocalDate.now()) && LocalTime.now().isAfter(heure))
                    throw new MetierException("Ce train est déjà parti aujourd'hui.");

                int prises;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT COALESCE(SUM(nb_places), 0) FROM reservations WHERE train_id = ? AND date_voyage = ?")) {
                    p.setInt(1, trainId);
                    p.setDate(2, java.sql.Date.valueOf(date));
                    try (ResultSet rs = p.executeQuery()) { rs.next(); prises = rs.getInt(1); }
                }
                if (nb > places - prises)
                    throw new MetierException("Places insuffisantes (" + (places - prises) + " restantes pour cette date).");

                int id;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO reservations (train_id, utilisateur_id, passager, nb_places, date_voyage) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, trainId);
                    p.setInt(2, Session.courant().getId());
                    p.setString(3, passager.trim());
                    p.setInt(4, nb);
                    p.setDate(5, java.sql.Date.valueOf(date));
                    p.executeUpdate();
                    try (ResultSet k = p.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                c.commit();
                LogDao.log("Réservation n°" + id + " (train " + trainId + ", " + nb + " place(s), " + date + ")");
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DbException("Réservation impossible : " + e.getMessage(), e);
        }
    }

    /** Un client ne peut annuler que ses réservations ; l'administrateur peut tout annuler. */
    public static void annuler(int reservationId) {
        Session.exigerConnecte();
        String sql = "DELETE FROM reservations WHERE id = ?" + (Session.estAdmin() ? "" : " AND utilisateur_id = ?");
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            if (!Session.estAdmin()) ps.setInt(2, Session.courant().getId());
            if (ps.executeUpdate() == 0) throw new MetierException("Réservation introuvable ou non autorisée.");
        } catch (SQLException e) { throw new DbException("Annulation impossible : " + e.getMessage(), e); }
        LogDao.log("Annulation de la réservation n°" + reservationId);
    }

    /** Administrateur : toutes les réservations. Client : uniquement les siennes. */
    public static List<Reservation> lister() {
        Session.exigerConnecte();
        String sql = "SELECT r.id, r.train_id, t.depart, t.arrivee, t.heure_depart, t.heure_arrivee, t.prix, "
                + "r.passager, r.nb_places, r.date_voyage, r.date_resa, u.login "
                + "FROM reservations r JOIN trains t ON t.id = r.train_id LEFT JOIN utilisateurs u ON u.id = r.utilisateur_id "
                + (Session.estAdmin() ? "" : "WHERE r.utilisateur_id = ? ")
                + "ORDER BY r.date_voyage DESC, r.id DESC";
        List<Reservation> l = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            if (!Session.estAdmin()) ps.setInt(1, Session.courant().getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = new Reservation();
                    r.id = rs.getInt(1); r.trainId = rs.getInt(2);
                    r.depart = rs.getString(3); r.arrivee = rs.getString(4);
                    r.heureDepart = rs.getTime(5).toLocalTime(); r.heureArrivee = rs.getTime(6).toLocalTime();
                    r.prix = rs.getDouble(7); r.passager = rs.getString(8); r.nbPlaces = rs.getInt(9);
                    r.dateVoyage = rs.getDate(10).toLocalDate();
                    r.dateResa = rs.getTimestamp(11).toLocalDateTime();
                    r.login = rs.getString(12);
                    l.add(r);
                }
            }
        } catch (SQLException e) { throw new DbException("Lecture des réservations impossible : " + e.getMessage(), e); }
        return l;
    }
}

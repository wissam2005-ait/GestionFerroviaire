import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
    public int id, trainId, nbPlaces;
    public String depart, arrivee, passager, login;
    public LocalTime heureDepart, heureArrivee;
    public double prix;
    public LocalDate dateVoyage;
    public LocalDateTime dateResa;

    public double total() { return nbPlaces * prix; }
}

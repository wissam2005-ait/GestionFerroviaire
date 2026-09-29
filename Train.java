import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Train {
    private static final DateTimeFormatter HF = DateTimeFormatter.ofPattern("HH:mm");

    public int id;
    public String dep, arr;
    public LocalTime hd, ha;
    public double prix;
    public int places;
    /** Places déjà réservées pour la date demandée (renseigné par TrainDao.tous(date)). */
    public int reservees;

    public Train(int id, String dep, String arr, LocalTime hd, LocalTime ha, double prix, int places, int reservees) {
        this.id = id; this.dep = dep; this.arr = arr; this.hd = hd; this.ha = ha;
        this.prix = prix; this.places = places; this.reservees = reservees;
    }

    public int libres() { return places - reservees; }

    public Duration duree() {
        Duration d = Duration.between(hd, ha);
        return d.isNegative() ? d.plusHours(24) : d;
    }

    public String dureeTxt() { return duree().toHours() + "h" + String.format("%02d", duree().toMinutesPart()); }

    @Override public String toString() {
        return "N°" + id + "  " + dep + " → " + arr + "  (" + hd.format(HF) + ")";
    }
}

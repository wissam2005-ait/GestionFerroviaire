import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JFileChooser;

/** Génère un billet HTML (imprimable, ou exportable en PDF depuis le navigateur). */
public final class Billet {
    private Billet() { }

    public static void enregistrer(Component parent, Reservation r) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("billet_" + r.id + ".html"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            Files.write(f.toPath(), html(r).getBytes(StandardCharsets.UTF_8));
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(f.toURI());
            else Ui.info(parent, "Billet enregistré :\n" + f.getAbsolutePath());
        } catch (IOException e) {
            Ui.erreur(parent, "Impossible d'enregistrer le billet : " + e.getMessage());
        }
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String html(Reservation r) {
        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>Billet n°" + r.id + "</title><style>"
                + "body{font-family:Segoe UI,Arial,sans-serif;background:#eef2f6;margin:0;padding:40px}"
                + ".billet{max-width:620px;margin:auto;background:#fff;border-radius:14px;box-shadow:0 6px 24px rgba(0,0,0,.12);overflow:hidden}"
                + ".tete{background:#1f4e79;color:#fff;padding:18px 28px;display:flex;justify-content:space-between;align-items:center}"
                + ".tete h1{margin:0;font-size:20px}.corps{padding:28px}"
                + ".trajet{font-size:26px;font-weight:600;color:#1f4e79;margin-bottom:6px}"
                + ".heures{font-size:18px;color:#444;margin-bottom:22px}"
                + "table{width:100%;border-collapse:collapse}td{padding:9px 0;border-top:1px solid #e3e8ee}"
                + "td:first-child{color:#777;width:40%}.total{font-size:20px;font-weight:700}"
                + ".pied{background:#f5f7fa;padding:14px 28px;font-size:12px;color:#777}</style></head><body>"
                + "<div class=\"billet\"><div class=\"tete\"><h1>🚆 Billet de train</h1><span>Réservation n°" + r.id + "</span></div>"
                + "<div class=\"corps\"><div class=\"trajet\">" + esc(r.depart) + " → " + esc(r.arrivee) + "</div>"
                + "<div class=\"heures\">" + r.dateVoyage.format(Ui.DF) + " &nbsp;•&nbsp; " + r.heureDepart.format(Ui.HF)
                + " → " + r.heureArrivee.format(Ui.HF) + "</div><table>"
                + "<tr><td>Passager</td><td>" + esc(r.passager) + "</td></tr>"
                + "<tr><td>Train</td><td>N°" + r.trainId + "</td></tr>"
                + "<tr><td>Nombre de places</td><td>" + r.nbPlaces + "</td></tr>"
                + "<tr><td>Prix unitaire</td><td>" + String.format("%.2f €", r.prix) + "</td></tr>"
                + "<tr><td class=\"total\">Total</td><td class=\"total\">" + String.format("%.2f €", r.total()) + "</td></tr>"
                + "</table></div><div class=\"pied\">Réservé le " + r.dateResa.format(Ui.DTF)
                + ". Document généré par Gestion &amp; Planification Ferroviaire.</div></div></body></html>";
    }
}

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Liste des trains. Les boutons Ajouter / Modifier / Supprimer ne sont affichés qu'aux administrateurs. */
public class HorairesPanel extends JPanel implements Rafraichissable {
    private final DefaultTableModel m = Ui.modele("N°", "Départ", "Arrivée", "Départ à", "Arrivée à", "Durée", "Prix", "Places libres", "Capacité");
    private final JTable t = Ui.table(m);
    private final JSpinner date = Ui.dateSpinner();
    private final Runnable onChange;
    private List<Train> trains = new ArrayList<>();

    public HorairesPanel(Runnable onChange) {
        super(new BorderLayout());
        this.onChange = onChange;
        t.getColumnModel().getColumn(6).setCellRenderer(Ui.euro());
        t.getColumnModel().getColumn(7).setCellRenderer(Ui.disponibilite(8));
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        JTextField filtre = new JTextField(14);
        Ui.surSaisie(filtre, () -> sorter.setRowFilter(filtre.getText().isBlank() ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(filtre.getText().trim()))));
        date.addChangeListener(e -> Ui.safe(this, this::refresh));

        JPanel top = Ui.barre();
        top.add(new JLabel("Rechercher :")); top.add(filtre);
        top.add(new JLabel("Disponibilité au :")); top.add(date);

        if (Session.estAdmin()) {
            JButton add = Ui.bouton("Ajouter"), edit = Ui.bouton("Modifier"), del = Ui.bouton("Supprimer");
            add.addActionListener(e -> Ui.safe(this, () -> {
                Train s = saisie(null);
                if (s != null) { TrainDao.inserer(s); onChange.run(); }
            }));
            edit.addActionListener(e -> Ui.safe(this, () -> {
                Train cur = selection();
                Train s = saisie(cur);
                if (s == null) return;
                s.id = cur.id;
                TrainDao.modifier(s);
                onChange.run();
            }));
            del.addActionListener(e -> Ui.safe(this, () -> {
                Train cur = selection();
                int n = TrainDao.reservationsFutures(cur.id);
                String msg = "Supprimer le train N°" + cur.id + " (" + cur.dep + " → " + cur.arr + ") ?"
                        + (n > 0 ? "\n" + n + " réservation(s) à venir seront supprimées." : "");
                if (Ui.confirmer(this, msg)) { TrainDao.supprimer(cur.id); onChange.run(); }
            }));
            Ui.primaire(add); Ui.danger(del);
            top.add(add); top.add(edit); top.add(del);
        }
        JButton rafraichir = Ui.bouton("Rafraîchir"), csv = Ui.bouton("Exporter CSV");
        rafraichir.addActionListener(e -> Ui.safe(this, onChange));
        csv.addActionListener(e -> Csv.exporter(this, t, "horaires.csv"));
        top.add(rafraichir); top.add(csv);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(t), BorderLayout.CENTER);
    }

    @Override public void refresh() {
        trains = TrainDao.tous(Ui.dateOf(date));
        m.setRowCount(0);
        for (Train x : trains)
            m.addRow(new Object[]{x.id, x.dep, x.arr, x.hd.format(Ui.HF), x.ha.format(Ui.HF), x.dureeTxt(), x.prix, x.libres(), x.places});
    }

    private Train selection() {
        int r = t.getSelectedRow();
        if (r < 0) throw new MetierException("Sélectionnez d'abord un train.");
        int id = (int) m.getValueAt(t.convertRowIndexToModel(r), 0);
        return trains.stream().filter(x -> x.id == id).findFirst().orElseThrow(() -> new MetierException("Train introuvable."));
    }

    /** Boîte de saisie d'un train (base == null : création). Retourne null si annulé. */
    private Train saisie(Train b) {
        JTextField d = new JTextField(b == null ? "" : b.dep), a = new JTextField(b == null ? "" : b.arr),
                hd = new JTextField(b == null ? "08:00" : b.hd.format(Ui.HF)), ha = new JTextField(b == null ? "10:00" : b.ha.format(Ui.HF)),
                p = new JTextField(b == null ? "50" : String.valueOf(b.prix)), pl = new JTextField(b == null ? "200" : String.valueOf(b.places));
        Object[] f = {"Gare de départ", d, "Gare d'arrivée", a, "Heure de départ (HH:mm)", hd,
                "Heure d'arrivée (HH:mm)", ha, "Prix (€)", p, "Nombre de places", pl};
        if (JOptionPane.showConfirmDialog(this, f, b == null ? "Nouveau train" : "Modifier le train",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        try {
            return new Train(0, d.getText().trim(), a.getText().trim(),
                    LocalTime.parse(hd.getText().trim()), LocalTime.parse(ha.getText().trim()),
                    Double.parseDouble(p.getText().trim().replace(',', '.')), Integer.parseInt(pl.getText().trim()), 0);
        } catch (DateTimeParseException | NumberFormatException ex) {
            throw new MetierException("Saisie invalide : heures au format HH:mm, prix et places numériques.");
        }
    }
}

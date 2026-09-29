import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** Réservation de places, annulation, billet. Un client ne voit que ses réservations ; l'admin voit tout. */
public class ReservationsPanel extends JPanel implements Rafraichissable {
    private final DefaultTableModel m = Ui.modele("N°", "Train", "Trajet", "Date voyage", "Départ", "Passager", "Places", "Total", "Réservé par", "Réservé le");
    private final JTable t = Ui.table(m);
    private final JComboBox<Train> cbTrain = new JComboBox<>();
    private final JSpinner date = Ui.dateSpinner();
    private final JTextField nom = new JTextField(14);
    private final JSpinner nb = new JSpinner(new SpinnerNumberModel(1, 1, ReservationDao.MAX_PLACES_PAR_RESERVATION, 1));
    private final Runnable onChange;
    private List<Reservation> liste = new ArrayList<>();

    public ReservationsPanel(Runnable onChange) {
        super(new BorderLayout());
        this.onChange = onChange;
        nom.setText(Session.courant().getNomComplet());
        t.getColumnModel().getColumn(7).setCellRenderer(Ui.euro());
        cbTrain.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Train) setText(v + "  —  " + ((Train) v).libres() + " place(s) libre(s)");
                return this;
            }
        });
        date.addChangeListener(e -> Ui.safe(this, this::rafraichirTrains));

        JButton reserver = Ui.bouton("Réserver"), annuler = Ui.bouton("Annuler la sélection"),
                billet = Ui.bouton("Billet"), csv = Ui.bouton("Exporter CSV");
        Ui.primaire(reserver); Ui.danger(annuler);
        reserver.addActionListener(e -> Ui.safe(this, this::reserver));
        annuler.addActionListener(e -> Ui.safe(this, this::annuler));
        billet.addActionListener(e -> Ui.safe(this, () -> Billet.enregistrer(this, selection())));
        csv.addActionListener(e -> Csv.exporter(this, t, "reservations.csv"));

        JPanel top = Ui.barre();
        if (!Session.estAdmin()) {
            top.add(new JLabel("Date :")); top.add(date);
            top.add(new JLabel("Train :")); top.add(cbTrain);
            top.add(new JLabel("Passager :")); top.add(nom);
            top.add(new JLabel("Places :")); top.add(nb);
            top.add(reserver);
        }
        top.add(annuler); top.add(billet); top.add(csv);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(t), BorderLayout.CENTER);
    }

    @Override public void refresh() {
        liste = ReservationDao.lister();
        m.setRowCount(0);
        for (Reservation r : liste)
            m.addRow(new Object[]{r.id, "N°" + r.trainId, r.depart + " → " + r.arrivee, r.dateVoyage.format(Ui.DF),
                    r.heureDepart.format(Ui.HF), r.passager, r.nbPlaces, r.total(),
                    r.login == null ? "—" : r.login, r.dateResa.format(Ui.DTF)});
        if (!Session.estAdmin()) rafraichirTrains();
    }

    private void rafraichirTrains() {
        Train sel = (Train) cbTrain.getSelectedItem();
        List<Train> trains = TrainDao.tous(Ui.dateOf(date));
        cbTrain.setModel(new DefaultComboBoxModel<>(trains.toArray(new Train[0])));
        if (sel != null) trains.stream().filter(x -> x.id == sel.id).findFirst().ifPresent(cbTrain::setSelectedItem);
    }

    private Reservation selection() {
        int r = t.getSelectedRow();
        if (r < 0) throw new MetierException("Sélectionnez d'abord une réservation.");
        int id = (int) m.getValueAt(t.convertRowIndexToModel(r), 0);
        return liste.stream().filter(x -> x.id == id).findFirst().orElseThrow(() -> new MetierException("Réservation introuvable."));
    }

    private void reserver() {
        Train tr = (Train) cbTrain.getSelectedItem();
        if (tr == null) throw new MetierException("Aucun train disponible.");
        int k = (int) nb.getValue();
        int id = ReservationDao.reserver(tr.id, nom.getText().trim(), k, Ui.dateOf(date));
        onChange.run();
        if (Ui.confirmer(this, String.format("Réservation n°%d confirmée : %d place(s), total %.2f €.\n\nGénérer le billet ?", id, k, k * tr.prix)))
            liste.stream().filter(x -> x.id == id).findFirst().ifPresent(r -> Billet.enregistrer(this, r));
    }

    private void annuler() {
        Reservation r = selection();
        if (Ui.confirmer(this, "Annuler la réservation n°" + r.id + " (" + r.depart + " → " + r.arrivee + ", " + r.nbPlaces + " place(s)) ?")) {
            ReservationDao.annuler(r.id);
            onChange.run();
        }
    }
}

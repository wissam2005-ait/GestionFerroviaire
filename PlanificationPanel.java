import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Recherche de trajets directs ou avec une correspondance (10 min minimum), triés au choix. */
public class PlanificationPanel extends JPanel implements Rafraichissable {
    private final JComboBox<String> cbDep = new JComboBox<>(), cbArr = new JComboBox<>();
    private final JComboBox<String> cbTri = new JComboBox<>(new String[]{"Heure de départ", "Prix", "Durée"});
    private final JSpinner date = Ui.dateSpinner();
    private final JTextField hmin = new JTextField("00:00", 6);
    private final JTextArea out = new JTextArea();
    private List<Train> trains = new ArrayList<>();

    private static class Option {
        String texte; long minutes; double prix; LocalTime depart;
    }

    public PlanificationPanel() {
        super(new BorderLayout());
        out.setEditable(false);
        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JButton go = Ui.bouton("Rechercher");
        Ui.primaire(go);
        go.addActionListener(e -> Ui.safe(this, this::rechercher));
        date.addChangeListener(e -> Ui.safe(this, this::refresh));

        JPanel top = Ui.barre();
        top.add(new JLabel("De :")); top.add(cbDep);
        top.add(new JLabel("À :")); top.add(cbArr);
        top.add(new JLabel("Date :")); top.add(date);
        top.add(new JLabel("Départ après :")); top.add(hmin);
        top.add(new JLabel("Trier par :")); top.add(cbTri);
        top.add(go);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(out), BorderLayout.CENTER);
    }

    @Override public void refresh() {
        trains = TrainDao.tous(Ui.dateOf(date));
        String d = (String) cbDep.getSelectedItem(), a = (String) cbArr.getSelectedItem();
        String[] gares = trains.stream().flatMap(t -> Stream.of(t.dep, t.arr)).distinct().sorted().toArray(String[]::new);
        cbDep.setModel(new javax.swing.DefaultComboBoxModel<>(gares));
        cbArr.setModel(new javax.swing.DefaultComboBoxModel<>(gares));
        if (d != null) cbDep.setSelectedItem(d);
        if (a != null) cbArr.setSelectedItem(a);
        else if (gares.length > 1) cbArr.setSelectedIndex(1);
    }

    private void rechercher() {
        String a = (String) cbDep.getSelectedItem(), b = (String) cbArr.getSelectedItem();
        LocalTime min;
        try { min = LocalTime.parse(hmin.getText().trim()); }
        catch (DateTimeParseException ex) { throw new MetierException("Heure invalide (HH:mm)."); }
        if (a == null || b == null || a.equals(b)) throw new MetierException("Choisissez deux gares différentes.");

        List<Option> directs = new ArrayList<>(), corr = new ArrayList<>();
        for (Train t : trains) {
            if (!t.dep.equals(a) || !t.arr.equals(b) || t.hd.isBefore(min)) continue;
            Option o = new Option();
            o.depart = t.hd; o.minutes = t.duree().toMinutes(); o.prix = t.prix;
            o.texte = String.format("N°%d  %s → %s  |  %s - %s (%s)  |  %.2f €  |  %d places libres",
                    t.id, t.dep, t.arr, t.hd.format(Ui.HF), t.ha.format(Ui.HF), t.dureeTxt(), t.prix, t.libres());
            directs.add(o);
        }
        for (Train t1 : trains) {
            if (!t1.dep.equals(a) || t1.hd.isBefore(min) || !t1.ha.isAfter(t1.hd) || t1.ha.isAfter(LocalTime.of(23, 49))) continue;
            for (Train t2 : trains) {
                if (!t2.arr.equals(b) || !t2.dep.equals(t1.arr) || t2.hd.isBefore(t1.ha.plusMinutes(10)) || !t2.ha.isAfter(t2.hd)) continue;
                Option o = new Option();
                o.depart = t1.hd; o.minutes = Duration.between(t1.hd, t2.ha).toMinutes(); o.prix = t1.prix + t2.prix;
                o.texte = String.format("N°%d %s→%s (%s-%s)  +  N°%d %s→%s (%s-%s)  |  %dh%02d  |  %.2f €  |  %d places libres",
                        t1.id, t1.dep, t1.arr, t1.hd.format(Ui.HF), t1.ha.format(Ui.HF),
                        t2.id, t2.dep, t2.arr, t2.hd.format(Ui.HF), t2.ha.format(Ui.HF),
                        o.minutes / 60, o.minutes % 60, o.prix, Math.min(t1.libres(), t2.libres()));
                corr.add(o);
            }
        }

        Comparator<Option> cmp;
        if (cbTri.getSelectedIndex() == 1) cmp = Comparator.comparingDouble(o -> o.prix);
        else if (cbTri.getSelectedIndex() == 2) cmp = Comparator.comparingLong(o -> o.minutes);
        else cmp = Comparator.comparing(o -> o.depart);

        StringBuilder sb = new StringBuilder("=== Trajets directs ===\n");
        sb.append(directs.isEmpty() ? "Aucun.\n" : directs.stream().sorted(cmp).map(o -> o.texte + "\n").collect(Collectors.joining()));
        sb.append("\n=== Avec une correspondance (min. 10 min) ===\n");
        sb.append(corr.isEmpty() ? "Aucune.\n" : corr.stream().sorted(cmp).map(o -> o.texte + "\n").collect(Collectors.joining()));
        out.setText(sb.toString());
        out.setCaretPosition(0);
    }
}

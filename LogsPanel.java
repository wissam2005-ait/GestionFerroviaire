import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Journal d'audit (administrateur uniquement) : les 500 dernières actions. */
public class LogsPanel extends JPanel implements Rafraichissable {
    private final DefaultTableModel m = Ui.modele("Date", "Utilisateur", "Action");
    private final JTable t = Ui.table(m);

    public LogsPanel() {
        super(new BorderLayout());
        JButton rafraichir = Ui.bouton("Rafraîchir"), csv = Ui.bouton("Exporter CSV");
        rafraichir.addActionListener(e -> Ui.safe(this, this::refresh));
        csv.addActionListener(e -> Csv.exporter(this, t, "journal.csv"));
        JPanel top = Ui.barre();
        top.add(rafraichir); top.add(csv);
        t.getColumnModel().getColumn(0).setPreferredWidth(140);
        t.getColumnModel().getColumn(1).setPreferredWidth(110);
        t.getColumnModel().getColumn(2).setPreferredWidth(650);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(t), BorderLayout.CENTER);
    }

    @Override public void refresh() {
        m.setRowCount(0);
        for (Object[] l : LogDao.derniers(500)) m.addRow(l);
    }
}

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/** Export d'un tableau au format CSV (séparateur ; et BOM UTF-8 pour une ouverture directe dans Excel). */
public final class Csv {
    private Csv() { }

    public static void exporter(Component parent, JTable table, String nomParDefaut) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(nomParDefaut));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        TableModel m = table.getModel();
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            w.print('\uFEFF');
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? ";" : "").append(cell(m.getColumnName(c)));
            w.println(sb);
            for (int r = 0; r < table.getRowCount(); r++) {
                int mr = table.convertRowIndexToModel(r);
                sb.setLength(0);
                for (int c = 0; c < m.getColumnCount(); c++)
                    sb.append(c > 0 ? ";" : "").append(cell(String.valueOf(m.getValueAt(mr, c))));
                w.println(sb);
            }
            Ui.info(parent, "Export réussi :\n" + f.getAbsolutePath());
        } catch (IOException e) {
            Ui.erreur(parent, "Export impossible : " + e.getMessage());
        }
    }

    private static String cell(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}

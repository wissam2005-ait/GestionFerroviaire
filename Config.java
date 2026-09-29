import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Lecture de config.properties (dossier courant). Les valeurs par défaut conviennent à XAMPP. */
public final class Config {
    private static final Properties P = new Properties();

    static {
        File f = new File("config.properties");
        if (f.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                P.load(r);
            } catch (IOException e) {
                System.err.println("Lecture de config.properties impossible : " + e.getMessage());
            }
        }
    }

    private Config() { }

    public static String get(String cle, String defaut) {
        return P.getProperty(cle, defaut).trim();
    }
}

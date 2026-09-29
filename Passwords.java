import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hachage des mots de passe avec PBKDF2-HMAC-SHA256 + sel aléatoire (aucune dépendance externe).
 * Format stocké : iterations:sel:hash (Base64).
 */
public final class Passwords {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RND = new SecureRandom();

    private Passwords() { }

    public static String hash(String motDePasse) {
        byte[] sel = new byte[SALT_BYTES];
        RND.nextBytes(sel);
        byte[] h = pbkdf2(motDePasse.toCharArray(), sel, ITERATIONS, KEY_BITS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(h);
    }

    public static boolean verifier(String motDePasse, String stocke) {
        try {
            String[] p = stocke.split(":");
            int it = Integer.parseInt(p[0]);
            byte[] sel = Base64.getDecoder().decode(p[1]);
            byte[] attendu = Base64.getDecoder().decode(p[2]);
            byte[] calcule = pbkdf2(motDePasse.toCharArray(), sel, it, attendu.length * 8);
            return MessageDigest.isEqual(attendu, calcule); // comparaison en temps constant
        } catch (Exception e) {
            return false;
        }
    }

    /** Politique minimale : 8 caractères, au moins une lettre et un chiffre. */
    public static void valider(String mdp) {
        if (mdp == null || mdp.length() < 8)
            throw new MetierException("Le mot de passe doit contenir au moins 8 caractères.");
        boolean lettre = false, chiffre = false;
        for (char c : mdp.toCharArray()) {
            if (Character.isLetter(c)) lettre = true;
            if (Character.isDigit(c)) chiffre = true;
        }
        if (!lettre || !chiffre)
            throw new MetierException("Le mot de passe doit contenir au moins une lettre et un chiffre.");
    }

    private static byte[] pbkdf2(char[] mdp, byte[] sel, int iterations, int bits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(mdp, sel, iterations, bits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

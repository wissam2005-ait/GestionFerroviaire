/** Erreur "métier" : règle non respectée, message directement affichable à l'utilisateur. */
public class MetierException extends RuntimeException {
    public MetierException(String message) { super(message); }
}

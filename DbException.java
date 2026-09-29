/** Erreur technique liée à la base de données. */
public class DbException extends RuntimeException {
    public DbException(String message, Throwable cause) { super(message, cause); }
}

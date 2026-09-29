import java.time.LocalDateTime;

public class Utilisateur {
    private final int id;
    private final String login;
    private final String hash;
    private final String nomComplet;
    private final Role role;
    private final boolean actif;
    private final LocalDateTime creeLe;

    public Utilisateur(int id, String login, String hash, String nomComplet, Role role, boolean actif, LocalDateTime creeLe) {
        this.id = id; this.login = login; this.hash = hash; this.nomComplet = nomComplet;
        this.role = role; this.actif = actif; this.creeLe = creeLe;
    }

    public int getId() { return id; }
    public String getLogin() { return login; }
    public String getHash() { return hash; }
    public String getNomComplet() { return nomComplet == null || nomComplet.isBlank() ? login : nomComplet; }
    public Role getRole() { return role; }
    public boolean isActif() { return actif; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}

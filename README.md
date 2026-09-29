# 🚆 Gestion & Planification Ferroviaire

Application desktop **Java Swing + MySQL** de gestion de trains et de réservations, avec **authentification sécurisée**, **rôles Administrateur / Client**, tableau de bord et journal d'audit.

> 📸 *Ajoute ici 2 ou 3 captures d'écran (connexion, horaires, tableau de bord) : `docs/login.png`, `docs/dashboard.png`…*

## ✨ Fonctionnalités

**Pour tous les utilisateurs**
- Connexion / création de compte client, changement de mot de passe
- Consultation des horaires avec **disponibilité par date de voyage**, filtre en direct, tri, export CSV
- Planification de trajets : **directs** et **avec correspondance** (10 min min.), triés par heure, prix ou durée
- Réservation de places (1 à 10), annulation de **ses** réservations, **billet HTML imprimable** (ou PDF via le navigateur)

**Réservé à l'administrateur**
- Ajout / modification / suppression de trains
- Voir et annuler toutes les réservations
- **Tableau de bord** : nombre de réservations, places vendues, chiffre d'affaires, graphique par trajet
- **Gestion des utilisateurs** : création, rôle, activation / désactivation, réinitialisation de mot de passe
- **Journal d'audit** : qui a fait quoi, et quand

## 🎨 Interface

- Écran de connexion en deux volets (marque / formulaire) avec option « Afficher le mot de passe »
- Bandeau supérieur en dégradé : utilisateur connecté et rôle sous forme de pastille colorée
- Boutons arrondis (principal, secondaire, danger) au rendu identique sur tous les thèmes
- Tableaux aux lignes alternées, sans quadrillage, avec **places libres en couleur** (vert / orange / rouge)
- Tableau de bord : cartes d'indicateurs et histogramme avec grille et montants en €
- Thèmes clair et sombre (`ui.theme=light|dark|system`)

## 🔐 Sécurité

| Mesure | Détail |
|---|---|
| Mots de passe | Hachés avec **PBKDF2-HMAC-SHA256** (120 000 itérations, sel aléatoire de 16 octets), comparaison en temps constant |
| Politique de mot de passe | 8 caractères minimum, au moins une lettre et un chiffre |
| Anti brute-force | Verrouillage 30 s après 5 échecs de connexion |
| Autorisations | Rôle **revérifié dans la couche DAO** avant chaque action sensible (pas seulement dans l'interface) |
| Isolation des données | Un client ne peut lire / annuler que ses propres réservations |
| Injection SQL | Uniquement des `PreparedStatement` |
| Surréservation impossible | Transaction avec `SELECT … FOR UPDATE` : la capacité est vérifiée en base, jamais côté interface |
| Configuration | Identifiants de BDD dans `config.properties` (ignoré par Git) |

## 🧱 Architecture

Tous les fichiers sont à la racine du projet (aucun sous-dossier) :

```
Main.java                                        point d'entrée (thème, vérif. BDD, login)
Train, Reservation, Utilisateur, Role, StatTrajet   modèles
TrainDao, ReservationDao, UtilisateurDao,
StatsDao, LogDao                                 accès MySQL + règles d'accès
Db, Config, Passwords, Auth, Session, Csv, ...   utilitaires
LoginFrame, MainFrame, Ui, *Panel, Billet        interface Swing
schema.sql, config.properties                    base de données et configuration
```

Base de données : `trains`, `utilisateurs`, `reservations` (avec `date_voyage`), `logs`.

## 🚀 Installation

**Prérequis** : Java 11+ (JDK), XAMPP (MySQL), [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/).

1. Démarre **MySQL** dans XAMPP.
2. Dans phpMyAdmin → onglet **SQL**, exécute `schema.sql` (⚠️ recrée les tables).
3. Copie `mysql-connector-j-x.x.x.jar` **dans le même dossier que les fichiers `.java`** (à la racine du projet).
   *(Optionnel : ajoute `flatlaf-x.x.jar` à la racine, à côté du connecteur MySQL pour un rendu encore plus moderne. Sans FlatLaf, l'application utilise Nimbus aux couleurs du projet — voir `ui.theme` dans `config.properties`.)*
4. Vérifie `config.properties` (par défaut : `root`, mot de passe vide).
5. Compile puis lance :
   - Windows : `compiler.bat` puis `lancer.bat`
   - Linux / macOS : `./compiler.sh` puis `./lancer.sh`

Lance toujours l'application **depuis la racine du projet** (là où se trouve `config.properties`).

## 👤 Comptes de démonstration

Créés automatiquement au premier lancement :

| Rôle | Identifiant | Mot de passe |
|---|---|---|
| Administrateur | `admin` | `admin123` |
| Client | `client` | `client123` |

⚠️ À changer (menu *Compte → Changer mon mot de passe*) et à masquer avec `ui.demo_hint=false` si tu déploies réellement l'application.

## 🛣️ Pistes d'évolution

- Passage à Maven/Gradle et tests JUnit
- Statuts des trains (à l'heure / retardé / supprimé)
- Plus court chemin (Dijkstra) pour plusieurs correspondances
- Export PDF natif du billet, envoi par e-mail
- Version web (Spring Boot) réutilisant le même schéma de base de données

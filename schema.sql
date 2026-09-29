-- Base de données : Gestion & Planification Ferroviaire (v2 : comptes, rôles, dates de voyage, journal)
-- À exécuter dans phpMyAdmin (onglet SQL) ou via la console MySQL de XAMPP.
-- ATTENTION : ce script supprime et recrée les tables (les anciennes données sont perdues).
-- Les comptes admin / client de démonstration sont créés automatiquement au premier lancement de l'application
-- (les mots de passe sont hachés en PBKDF2 côté Java, jamais stockés en clair).

CREATE DATABASE IF NOT EXISTS gestion_ferroviaire
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gestion_ferroviaire;

DROP TABLE IF EXISTS logs;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS utilisateurs;
DROP TABLE IF EXISTS trains;

CREATE TABLE trains (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    depart         VARCHAR(100) NOT NULL,
    arrivee        VARCHAR(100) NOT NULL,
    heure_depart   TIME NOT NULL,
    heure_arrivee  TIME NOT NULL,
    prix           DECIMAL(8,2) NOT NULL,
    places         INT NOT NULL,
    CONSTRAINT chk_train_prix   CHECK (prix >= 0),
    CONSTRAINT chk_train_places CHECK (places > 0)
);

CREATE TABLE utilisateurs (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    login         VARCHAR(50)  NOT NULL UNIQUE,
    mot_de_passe  VARCHAR(255) NOT NULL,              -- PBKDF2 : iterations:sel:hash
    nom_complet   VARCHAR(150) NOT NULL DEFAULT '',
    role          ENUM('ADMIN','CLIENT') NOT NULL DEFAULT 'CLIENT',
    actif         BOOLEAN NOT NULL DEFAULT TRUE,
    cree_le       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    train_id        INT NOT NULL,
    utilisateur_id  INT NULL,
    passager        VARCHAR(150) NOT NULL,
    nb_places       INT NOT NULL,
    date_voyage     DATE NOT NULL,
    date_resa       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_resa_places CHECK (nb_places > 0),
    FOREIGN KEY (train_id)       REFERENCES trains(id)       ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE SET NULL,
    INDEX idx_resa_train_date (train_id, date_voyage)
);

CREATE TABLE logs (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    date_log     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utilisateur  VARCHAR(50)  NOT NULL,
    action       VARCHAR(255) NOT NULL
);

-- Trains
INSERT INTO trains (depart, arrivee, heure_depart, heure_arrivee, prix, places) VALUES
('Paris', 'Lyon', '08:00:00', '10:00:00', 65.00, 200),
('Lyon', 'Marseille', '10:30:00', '12:15:00', 45.00, 180),
('Paris', 'Marseille', '07:00:00', '10:40:00', 89.00, 250),
('Paris', 'Lille', '09:15:00', '10:20:00', 32.00, 220),
('Lille', 'Bruxelles', '10:45:00', '11:30:00', 25.00, 150),
('Paris', 'Tours', '09:00:00', '10:05:00', 30.00, 160),
('Tours', 'Bordeaux', '10:30:00', '12:30:00', 42.00, 160),
('Lyon', 'Paris', '12:00:00', '14:00:00', 65.00, 200),
('Marseille', 'Lyon', '14:00:00', '15:45:00', 45.00, 180),
('Lille', 'Paris', '17:30:00', '18:35:00', 32.00, 220),
('Bordeaux', 'Tours', '14:00:00', '16:00:00', 42.00, 160),
('Tours', 'Paris', '16:30:00', '17:35:00', 30.00, 160);

-- Quelques réservations de démonstration (pour que le tableau de bord ne soit pas vide)
INSERT INTO reservations (train_id, utilisateur_id, passager, nb_places, date_voyage) VALUES
(1, NULL, 'Camille Martin',  2, DATE_ADD(CURDATE(), INTERVAL 1 DAY)),
(1, NULL, 'Lucas Bernard',   4, DATE_ADD(CURDATE(), INTERVAL 1 DAY)),
(3, NULL, 'Sarah Dubois',    1, DATE_ADD(CURDATE(), INTERVAL 2 DAY)),
(4, NULL, 'Yanis Petit',     3, DATE_ADD(CURDATE(), INTERVAL 3 DAY)),
(2, NULL, 'Emma Leroy',      2, DATE_ADD(CURDATE(), INTERVAL 1 DAY)),
(7, NULL, 'Hugo Moreau',     5, DATE_ADD(CURDATE(), INTERVAL 5 DAY)),
(5, NULL, 'Inès Fournier',   2, DATE_ADD(CURDATE(), INTERVAL 4 DAY));

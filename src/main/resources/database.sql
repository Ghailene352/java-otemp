CREATE DATABASE IF NOT EXISTS guser_db;
USE guser_db;

CREATE TABLE IF NOT EXISTS utilisateurs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    prenoms VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telephone VARCHAR(30) NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL DEFAULT 'UTILISATEUR',
    face_template LONGTEXT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO utilisateurs (nom, prenoms, email, telephone, mot_de_passe, role)
VALUES
    ('Alice', 'Claire', 'alice@example.com', '0700000001', 'Alice12345', 'ADMIN'),
    ('Bob', 'Martin', 'bob@example.com', '0700000002', 'Bob12345', 'UTILISATEUR');

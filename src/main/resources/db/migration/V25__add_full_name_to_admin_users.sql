-- V25 : ajoute la colonne full_name à la table admin_users
ALTER TABLE admin_users
    ADD COLUMN full_name VARCHAR(255) NULL AFTER email;

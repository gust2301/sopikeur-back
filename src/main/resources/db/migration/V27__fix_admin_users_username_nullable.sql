-- Les colonnes 'username' et 'role' sont des colonnes legacy non mappées dans AdminUserEntity.
-- L'application utilise 'email' comme identifiant unique (V22) et les rôles via la table 'roles' (V22).
-- On rend ces colonnes legacy nullables pour ne plus bloquer les INSERT JPA.
ALTER TABLE admin_users
    MODIFY COLUMN username VARCHAR(100) NULL DEFAULT NULL,
    MODIFY COLUMN role     VARCHAR(50)  NULL DEFAULT NULL;

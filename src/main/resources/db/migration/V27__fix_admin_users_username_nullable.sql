-- Les colonnes 'username' et 'role' sont des colonnes legacy non mappées dans AdminUserEntity.
-- L'application utilise 'email' comme identifiant unique (V22) et les rôles via la table 'roles' (V22).
-- On rend ces colonnes legacy nullables pour ne plus bloquer les INSERT JPA.
--
-- PATTERN IDEMPOTENT : MySQL ne supporte pas IF EXISTS sur MODIFY COLUMN.
-- On interroge information_schema pour ne modifier la colonne que si elle existe encore.
-- Sur une base locale propre (V22 a supprimé ces colonnes), ce script devient un no-op.
-- Sur la base de production (colonnes toujours présentes), il exécute l'ALTER normalement.
-- FlywayMigrationConfig appelle flyway.repair() au démarrage : l'entrée précédemment
-- échouée dans flyway_schema_history est nettoyée automatiquement avant ce retry.

-- ── username ──────────────────────────────────────────────────────────────────
SELECT COUNT(*) INTO @username_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'admin_users'
  AND COLUMN_NAME  = 'username';

SET @sql_username = IF(
    @username_exists > 0,
    'ALTER TABLE admin_users MODIFY COLUMN username VARCHAR(100) NULL DEFAULT NULL',
    'SELECT 1 -- username column absent, skipping'
);
PREPARE stmt FROM @sql_username;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ── role ──────────────────────────────────────────────────────────────────────
SELECT COUNT(*) INTO @role_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'admin_users'
  AND COLUMN_NAME  = 'role';

SET @sql_role = IF(
    @role_exists > 0,
    'ALTER TABLE admin_users MODIFY COLUMN role VARCHAR(50) NULL DEFAULT NULL',
    'SELECT 1 -- role column absent, skipping'
);
PREPARE stmt FROM @sql_role;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

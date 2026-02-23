-- Compat MySQL (versions sans IF [NOT] EXISTS sur colonnes/index):
-- on utilise information_schema + SQL dynamique.

SET @has_admin_users_email := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_users'
      AND COLUMN_NAME = 'email'
);
SET @sql := IF(
    @has_admin_users_email = 0,
    'ALTER TABLE admin_users ADD COLUMN email VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_admin_users_enabled := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_users'
      AND COLUMN_NAME = 'enabled'
);
SET @sql := IF(
    @has_admin_users_enabled = 0,
    'ALTER TABLE admin_users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_admin_users_created_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_users'
      AND COLUMN_NAME = 'created_at'
);
SET @sql := IF(
    @has_admin_users_created_at = 0,
    'ALTER TABLE admin_users ADD COLUMN created_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_admin_users_updated_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_users'
      AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(
    @has_admin_users_updated_at = 0,
    'ALTER TABLE admin_users ADD COLUMN updated_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_admin_users_last_login_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_users'
      AND COLUMN_NAME = 'last_login_at'
);
SET @sql := IF(
    @has_admin_users_last_login_at = 0,
    'ALTER TABLE admin_users ADD COLUMN last_login_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE admin_users
SET email = 'admin@sopikeur.sn'
WHERE email IS NULL OR email = '';

UPDATE admin_users
SET created_at = NOW(), updated_at = NOW()
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE admin_users
    MODIFY COLUMN email VARCHAR(255) NOT NULL,
    MODIFY COLUMN created_at TIMESTAMP NOT NULL,
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS admin_user_roles (
    admin_user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (admin_user_id, role_id),
    CONSTRAINT fk_admin_user_roles_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_admin_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE CASCADE
);

INSERT IGNORE INTO roles(code) VALUES ('SUPER_ADMIN'), ('ADMIN'), ('EDITOR'), ('SALES');

SET @has_idx_admin_users_email := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin_users' AND INDEX_NAME = 'idx_admin_users_email'
);
SET @sql := IF(@has_idx_admin_users_email = 0, 'CREATE INDEX idx_admin_users_email ON admin_users(email)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_products_slug := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND INDEX_NAME = 'idx_products_slug'
);
SET @sql := IF(@has_idx_products_slug = 0, 'CREATE INDEX idx_products_slug ON products(slug)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_media_assets_product_id := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_assets' AND INDEX_NAME = 'idx_media_assets_product_id'
);
SET @sql := IF(@has_idx_media_assets_product_id = 0, 'CREATE INDEX idx_media_assets_product_id ON media_assets(product_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_media_assets_inspiration_id := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_assets' AND INDEX_NAME = 'idx_media_assets_inspiration_id'
);
SET @sql := IF(@has_idx_media_assets_inspiration_id = 0, 'CREATE INDEX idx_media_assets_inspiration_id ON media_assets(inspiration_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

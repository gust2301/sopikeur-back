-- La DB staging avait une colonne `order_status` au lieu de `status`.
-- V29 n'a pas été ré-exécutée (Flyway repair = correction du checksum uniquement).
-- Ce script force le rename si la colonne s'appelle encore `order_status`.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'orders'
      AND COLUMN_NAME  = 'order_status'
);

SET @sql = IF(
    @col_exists > 0,
    'ALTER TABLE orders CHANGE order_status status VARCHAR(30) NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

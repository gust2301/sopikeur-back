SET @orders_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
);

SET @delivery_eta_date_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'delivery_eta_date'
);

SET @delivery_eta_date_sql := IF(
    @orders_table_exists = 1 AND @delivery_eta_date_exists = 0,
    'ALTER TABLE orders ADD COLUMN delivery_eta_date DATE NULL',
    'SELECT 1'
);
PREPARE stmt_delivery_eta_date FROM @delivery_eta_date_sql;
EXECUTE stmt_delivery_eta_date;
DEALLOCATE PREPARE stmt_delivery_eta_date;

SET @installation_eta_date_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_eta_date'
);

SET @installation_eta_date_sql := IF(
    @orders_table_exists = 1 AND @installation_eta_date_exists = 0,
    'ALTER TABLE orders ADD COLUMN installation_eta_date DATE NULL',
    'SELECT 1'
);
PREPARE stmt_installation_eta_date FROM @installation_eta_date_sql;
EXECUTE stmt_installation_eta_date;
DEALLOCATE PREPARE stmt_installation_eta_date;

SET @delivered_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'delivered_at'
);

SET @delivered_at_sql := IF(
    @orders_table_exists = 1 AND @delivered_at_exists = 0,
    'ALTER TABLE orders ADD COLUMN delivered_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_delivered_at FROM @delivered_at_sql;
EXECUTE stmt_delivered_at;
DEALLOCATE PREPARE stmt_delivered_at;

SET @installed_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installed_at'
);

SET @installed_at_sql := IF(
    @orders_table_exists = 1 AND @installed_at_exists = 0,
    'ALTER TABLE orders ADD COLUMN installed_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_installed_at FROM @installed_at_sql;
EXECUTE stmt_installed_at;
DEALLOCATE PREPARE stmt_installed_at;

SET @internal_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'internal_note'
);

SET @internal_note_sql := IF(
    @orders_table_exists = 1 AND @internal_note_exists = 0,
    'ALTER TABLE orders ADD COLUMN internal_note TEXT NULL',
    'SELECT 1'
);
PREPARE stmt_internal_note FROM @internal_note_sql;
EXECUTE stmt_internal_note;
DEALLOCATE PREPARE stmt_internal_note;

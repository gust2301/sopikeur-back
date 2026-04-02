SET @orders_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
);

SET @expected_delivery_date_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'expected_delivery_date'
);

SET @expected_delivery_date_sql := IF(
    @orders_table_exists = 1 AND @expected_delivery_date_exists = 0,
    'ALTER TABLE orders ADD COLUMN expected_delivery_date DATE NULL',
    'SELECT 1'
);
PREPARE stmt_expected_delivery_date FROM @expected_delivery_date_sql;
EXECUTE stmt_expected_delivery_date;
DEALLOCATE PREPARE stmt_expected_delivery_date;

SET @delivery_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'delivery_note'
);

SET @delivery_note_sql := IF(
    @orders_table_exists = 1 AND @delivery_note_exists = 0,
    'ALTER TABLE orders ADD COLUMN delivery_note VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_delivery_note FROM @delivery_note_sql;
EXECUTE stmt_delivery_note;
DEALLOCATE PREPARE stmt_delivery_note;

SET @installation_requested_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_requested'
);

SET @installation_requested_sql := IF(
    @orders_table_exists = 1 AND @installation_requested_exists = 0,
    'ALTER TABLE orders ADD COLUMN installation_requested BOOLEAN NULL',
    'SELECT 1'
);
PREPARE stmt_installation_requested FROM @installation_requested_sql;
EXECUTE stmt_installation_requested;
DEALLOCATE PREPARE stmt_installation_requested;

SET @installation_date_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_date'
);

SET @installation_date_sql := IF(
    @orders_table_exists = 1 AND @installation_date_exists = 0,
    'ALTER TABLE orders ADD COLUMN installation_date DATE NULL',
    'SELECT 1'
);
PREPARE stmt_installation_date FROM @installation_date_sql;
EXECUTE stmt_installation_date;
DEALLOCATE PREPARE stmt_installation_date;

SET @installation_note_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_note'
);

SET @installation_note_sql := IF(
    @orders_table_exists = 1 AND @installation_note_exists = 0,
    'ALTER TABLE orders ADD COLUMN installation_note VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_installation_note FROM @installation_note_sql;
EXECUTE stmt_installation_note;
DEALLOCATE PREPARE stmt_installation_note;

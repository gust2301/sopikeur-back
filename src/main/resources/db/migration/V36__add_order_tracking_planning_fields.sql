SET @has_expected_delivery_date = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'expected_delivery_date'
);

SET @add_expected_delivery_date_sql = IF(
    @has_expected_delivery_date = 0,
    'ALTER TABLE orders ADD COLUMN expected_delivery_date DATE NULL AFTER delivery_json',
    'SELECT 1'
);
PREPARE stmt_add_expected_delivery_date FROM @add_expected_delivery_date_sql;
EXECUTE stmt_add_expected_delivery_date;
DEALLOCATE PREPARE stmt_add_expected_delivery_date;

SET @has_installation_date = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_date'
);

SET @add_installation_date_sql = IF(
    @has_installation_date = 0,
    'ALTER TABLE orders ADD COLUMN installation_date DATE NULL AFTER expected_delivery_date',
    'SELECT 1'
);
PREPARE stmt_add_installation_date FROM @add_installation_date_sql;
EXECUTE stmt_add_installation_date;
DEALLOCATE PREPARE stmt_add_installation_date;

SET @has_installation_note = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_note'
);

SET @add_installation_note_sql = IF(
    @has_installation_note = 0,
    'ALTER TABLE orders ADD COLUMN installation_note VARCHAR(255) NULL AFTER installation_date',
    'SELECT 1'
);
PREPARE stmt_add_installation_note FROM @add_installation_note_sql;
EXECUTE stmt_add_installation_note;
DEALLOCATE PREPARE stmt_add_installation_note;

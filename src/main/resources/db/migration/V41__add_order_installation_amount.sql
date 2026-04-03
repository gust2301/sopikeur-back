SET @schema_name := DATABASE();

SET @installation_amount_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'installation_amount'
);

SET @sql := IF(
    @installation_amount_exists = 0,
    'ALTER TABLE orders ADD COLUMN installation_amount DECIMAL(15,2) NULL AFTER deposit_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

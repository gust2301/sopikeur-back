CREATE TABLE IF NOT EXISTS service_types (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NULL,
    default_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_service_types_code UNIQUE (code)
);

SET @has_service_types_code_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'service_types'
      AND INDEX_NAME = 'uk_service_types_code'
);

SET @add_service_types_code_idx_sql := IF(
    @has_service_types_code_idx = 0,
    'ALTER TABLE service_types ADD CONSTRAINT uk_service_types_code UNIQUE (code)',
    'SELECT 1'
);
PREPARE stmt_add_service_types_code_idx FROM @add_service_types_code_idx_sql;
EXECUTE stmt_add_service_types_code_idx;
DEALLOCATE PREPARE stmt_add_service_types_code_idx;

SET @has_order_items_line_type := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND COLUMN_NAME = 'line_type'
);

SET @add_order_items_line_type_sql := IF(
    @has_order_items_line_type = 0,
    'ALTER TABLE order_items ADD COLUMN line_type VARCHAR(20) NOT NULL DEFAULT ''PRODUCT'' AFTER order_id',
    'SELECT 1'
);
PREPARE stmt_add_order_items_line_type FROM @add_order_items_line_type_sql;
EXECUTE stmt_add_order_items_line_type;
DEALLOCATE PREPARE stmt_add_order_items_line_type;

SET @has_order_items_service_type_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND COLUMN_NAME = 'service_type_id'
);

SET @add_order_items_service_type_id_sql := IF(
    @has_order_items_service_type_id = 0,
    'ALTER TABLE order_items ADD COLUMN service_type_id BIGINT NULL AFTER product_id',
    'SELECT 1'
);
PREPARE stmt_add_order_items_service_type_id FROM @add_order_items_service_type_id_sql;
EXECUTE stmt_add_order_items_service_type_id;
DEALLOCATE PREPARE stmt_add_order_items_service_type_id;

SET @has_order_items_display_name := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND COLUMN_NAME = 'display_name'
);

SET @add_order_items_display_name_sql := IF(
    @has_order_items_display_name = 0,
    'ALTER TABLE order_items ADD COLUMN display_name VARCHAR(255) NULL AFTER sku_snapshot',
    'SELECT 1'
);
PREPARE stmt_add_order_items_display_name FROM @add_order_items_display_name_sql;
EXECUTE stmt_add_order_items_display_name;
DEALLOCATE PREPARE stmt_add_order_items_display_name;

SET @has_order_items_line_note := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND COLUMN_NAME = 'line_note'
);

SET @add_order_items_line_note_sql := IF(
    @has_order_items_line_note = 0,
    'ALTER TABLE order_items ADD COLUMN line_note VARCHAR(255) NULL AFTER line_total_snapshot',
    'SELECT 1'
);
PREPARE stmt_add_order_items_line_note FROM @add_order_items_line_note_sql;
EXECUTE stmt_add_order_items_line_note;
DEALLOCATE PREPARE stmt_add_order_items_line_note;

SET @order_items_product_nullable := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND COLUMN_NAME = 'product_id'
      AND IS_NULLABLE = 'YES'
);

SET @alter_order_items_product_nullable_sql := IF(
    @order_items_product_nullable = 0,
    'ALTER TABLE order_items MODIFY COLUMN product_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_alter_order_items_product_nullable FROM @alter_order_items_product_nullable_sql;
EXECUTE stmt_alter_order_items_product_nullable;
DEALLOCATE PREPARE stmt_alter_order_items_product_nullable;

SET @has_order_items_service_type_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND CONSTRAINT_NAME = 'fk_order_items_service_type'
);

SET @add_order_items_service_type_fk_sql := IF(
    @has_order_items_service_type_fk = 0,
    'ALTER TABLE order_items ADD CONSTRAINT fk_order_items_service_type FOREIGN KEY (service_type_id) REFERENCES service_types(id)',
    'SELECT 1'
);
PREPARE stmt_add_order_items_service_type_fk FROM @add_order_items_service_type_fk_sql;
EXECUTE stmt_add_order_items_service_type_fk;
DEALLOCATE PREPARE stmt_add_order_items_service_type_fk;

SET @has_order_items_line_type_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND INDEX_NAME = 'idx_order_items_line_type'
);

SET @add_order_items_line_type_idx_sql := IF(
    @has_order_items_line_type_idx = 0,
    'CREATE INDEX idx_order_items_line_type ON order_items(line_type)',
    'SELECT 1'
);
PREPARE stmt_add_order_items_line_type_idx FROM @add_order_items_line_type_idx_sql;
EXECUTE stmt_add_order_items_line_type_idx;
DEALLOCATE PREPARE stmt_add_order_items_line_type_idx;

SET @has_order_items_service_type_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND INDEX_NAME = 'idx_order_items_service_type_id'
);

SET @add_order_items_service_type_idx_sql := IF(
    @has_order_items_service_type_idx = 0,
    'CREATE INDEX idx_order_items_service_type_id ON order_items(service_type_id)',
    'SELECT 1'
);
PREPARE stmt_add_order_items_service_type_idx FROM @add_order_items_service_type_idx_sql;
EXECUTE stmt_add_order_items_service_type_idx;
DEALLOCATE PREPARE stmt_add_order_items_service_type_idx;

UPDATE order_items
SET line_type = 'PRODUCT'
WHERE line_type IS NULL OR line_type = '';

UPDATE order_items
SET display_name = COALESCE(display_name, sku_snapshot)
WHERE (display_name IS NULL OR display_name = '')
  AND (sku_snapshot IS NOT NULL AND sku_snapshot <> '');

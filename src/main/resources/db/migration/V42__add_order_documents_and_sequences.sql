SET @order_payments_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
);

SET @create_order_payments_sql := IF(
    @order_payments_exists = 0,
    'CREATE TABLE order_payments (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        order_id BIGINT NOT NULL,
        receipt_number VARCHAR(30) NULL,
        amount DECIMAL(12,2) NOT NULL,
        method VARCHAR(30) NULL,
        note VARCHAR(255) NULL,
        created_by VARCHAR(255) NULL,
        created_by_admin_user_id BIGINT NULL,
        created_at DATETIME NOT NULL,
        updated_at DATETIME NOT NULL,
        CONSTRAINT fk_order_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
        CONSTRAINT fk_order_payments_admin_user FOREIGN KEY (created_by_admin_user_id) REFERENCES admin_users(id),
        CONSTRAINT uk_order_payments_receipt_number UNIQUE (receipt_number)
    )',
    'SELECT 1'
);
PREPARE stmt_create_order_payments FROM @create_order_payments_sql;
EXECUTE stmt_create_order_payments;
DEALLOCATE PREPARE stmt_create_order_payments;

SET @has_order_payments_receipt_number := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND COLUMN_NAME = 'receipt_number'
);

SET @add_order_payments_receipt_number_sql := IF(
    @has_order_payments_receipt_number = 0,
    'ALTER TABLE order_payments ADD COLUMN receipt_number VARCHAR(30) NULL AFTER order_id',
    'SELECT 1'
);
PREPARE stmt_add_order_payments_receipt_number FROM @add_order_payments_receipt_number_sql;
EXECUTE stmt_add_order_payments_receipt_number;
DEALLOCATE PREPARE stmt_add_order_payments_receipt_number;

SET @has_order_payments_created_by_admin_user_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND COLUMN_NAME = 'created_by_admin_user_id'
);

SET @add_order_payments_created_by_admin_user_id_sql := IF(
    @has_order_payments_created_by_admin_user_id = 0,
    'ALTER TABLE order_payments ADD COLUMN created_by_admin_user_id BIGINT NULL AFTER created_by',
    'SELECT 1'
);
PREPARE stmt_add_order_payments_created_by_admin_user_id FROM @add_order_payments_created_by_admin_user_id_sql;
EXECUTE stmt_add_order_payments_created_by_admin_user_id;
DEALLOCATE PREPARE stmt_add_order_payments_created_by_admin_user_id;

SET @has_order_payments_note_length := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND COLUMN_NAME = 'note'
      AND DATA_TYPE = 'varchar'
      AND CHARACTER_MAXIMUM_LENGTH = 255
);

SET @alter_order_payments_note_sql := IF(
    @has_order_payments_note_length = 0,
    'ALTER TABLE order_payments MODIFY COLUMN note VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_alter_order_payments_note FROM @alter_order_payments_note_sql;
EXECUTE stmt_alter_order_payments_note;
DEALLOCATE PREPARE stmt_alter_order_payments_note;

SET @has_order_payments_method_length := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND COLUMN_NAME = 'method'
      AND CHARACTER_MAXIMUM_LENGTH = 30
);

SET @alter_order_payments_method_sql := IF(
    @has_order_payments_method_length = 0,
    'ALTER TABLE order_payments MODIFY COLUMN method VARCHAR(30) NULL',
    'SELECT 1'
);
PREPARE stmt_alter_order_payments_method FROM @alter_order_payments_method_sql;
EXECUTE stmt_alter_order_payments_method;
DEALLOCATE PREPARE stmt_alter_order_payments_method;

SET @has_order_payments_receipt_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND INDEX_NAME = 'uk_order_payments_receipt_number'
);

SET @add_order_payments_receipt_idx_sql := IF(
    @has_order_payments_receipt_idx = 0,
    'ALTER TABLE order_payments ADD CONSTRAINT uk_order_payments_receipt_number UNIQUE (receipt_number)',
    'SELECT 1'
);
PREPARE stmt_add_order_payments_receipt_idx FROM @add_order_payments_receipt_idx_sql;
EXECUTE stmt_add_order_payments_receipt_idx;
DEALLOCATE PREPARE stmt_add_order_payments_receipt_idx;

SET @has_order_payments_admin_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND CONSTRAINT_NAME = 'fk_order_payments_admin_user'
);

SET @add_order_payments_admin_fk_sql := IF(
    @has_order_payments_admin_fk = 0,
    'ALTER TABLE order_payments ADD CONSTRAINT fk_order_payments_admin_user FOREIGN KEY (created_by_admin_user_id) REFERENCES admin_users(id)',
    'SELECT 1'
);
PREPARE stmt_add_order_payments_admin_fk FROM @add_order_payments_admin_fk_sql;
EXECUTE stmt_add_order_payments_admin_fk;
DEALLOCATE PREPARE stmt_add_order_payments_admin_fk;

SET @has_order_payments_order_created_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND INDEX_NAME = 'idx_order_payments_order_created_at'
);

SET @add_order_payments_order_created_idx_sql := IF(
    @has_order_payments_order_created_idx = 0,
    'CREATE INDEX idx_order_payments_order_created_at ON order_payments(order_id, created_at)',
    'SELECT 1'
);
PREPARE stmt_add_order_payments_order_created_idx FROM @add_order_payments_order_created_idx_sql;
EXECUTE stmt_add_order_payments_order_created_idx;
DEALLOCATE PREPARE stmt_add_order_payments_order_created_idx;

SET @has_orders_invoice_number := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'invoice_number'
);

SET @add_orders_invoice_number_sql := IF(
    @has_orders_invoice_number = 0,
    'ALTER TABLE orders ADD COLUMN invoice_number VARCHAR(30) NULL AFTER order_number',
    'SELECT 1'
);
PREPARE stmt_add_orders_invoice_number FROM @add_orders_invoice_number_sql;
EXECUTE stmt_add_orders_invoice_number;
DEALLOCATE PREPARE stmt_add_orders_invoice_number;

SET @has_orders_invoice_issued_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'invoice_issued_at'
);

SET @add_orders_invoice_issued_at_sql := IF(
    @has_orders_invoice_issued_at = 0,
    'ALTER TABLE orders ADD COLUMN invoice_issued_at DATETIME NULL AFTER invoice_number',
    'SELECT 1'
);
PREPARE stmt_add_orders_invoice_issued_at FROM @add_orders_invoice_issued_at_sql;
EXECUTE stmt_add_orders_invoice_issued_at;
DEALLOCATE PREPARE stmt_add_orders_invoice_issued_at;

SET @has_orders_invoice_number_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'uk_orders_invoice_number'
);

SET @add_orders_invoice_number_idx_sql := IF(
    @has_orders_invoice_number_idx = 0,
    'ALTER TABLE orders ADD CONSTRAINT uk_orders_invoice_number UNIQUE (invoice_number)',
    'SELECT 1'
);
PREPARE stmt_add_orders_invoice_number_idx FROM @add_orders_invoice_number_idx_sql;
EXECUTE stmt_add_orders_invoice_number_idx;
DEALLOCATE PREPARE stmt_add_orders_invoice_number_idx;

SET @sequences_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sequences'
);

SET @create_sequences_sql := IF(
    @sequences_exists = 0,
    'CREATE TABLE sequences (
        name VARCHAR(50) NOT NULL,
        year INT NOT NULL,
        value INT NOT NULL,
        PRIMARY KEY (name, year)
    )',
    'SELECT 1'
);
PREPARE stmt_create_sequences FROM @create_sequences_sql;
EXECUTE stmt_create_sequences;
DEALLOCATE PREPARE stmt_create_sequences;

INSERT INTO order_payments(order_id, receipt_number, amount, method, note, created_by, created_by_admin_user_id, created_at, updated_at)
SELECT o.id,
       NULL,
       o.amount_paid,
       o.payment_method_sel,
       'Legacy migrated paid amount',
       'migration',
       NULL,
       COALESCE(o.updated_at, NOW()),
       COALESCE(o.updated_at, NOW())
FROM orders o
WHERE COALESCE(o.amount_paid, 0) > 0
  AND NOT EXISTS (
      SELECT 1
      FROM order_payments op
      WHERE op.order_id = o.id
  );

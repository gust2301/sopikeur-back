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
        amount DECIMAL(15,2) NOT NULL,
        method VARCHAR(50) NULL,
        note TEXT NULL,
        created_by VARCHAR(255) NULL,
        created_at DATETIME NOT NULL,
        updated_at DATETIME NOT NULL,
        CONSTRAINT fk_order_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
    )',
    'SELECT 1'
);
PREPARE stmt_create_order_payments FROM @create_order_payments_sql;
EXECUTE stmt_create_order_payments;
DEALLOCATE PREPARE stmt_create_order_payments;

SET @order_payments_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_payments'
      AND INDEX_NAME = 'idx_order_payments_order_id'
);

SET @create_order_payments_idx_sql := IF(
    @order_payments_exists = 1 AND @order_payments_idx_exists = 0,
    'CREATE INDEX idx_order_payments_order_id ON order_payments(order_id)',
    'SELECT 1'
);
PREPARE stmt_create_order_payments_idx FROM @create_order_payments_idx_sql;
EXECUTE stmt_create_order_payments_idx;
DEALLOCATE PREPARE stmt_create_order_payments_idx;

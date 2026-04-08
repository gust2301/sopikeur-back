CREATE TABLE IF NOT EXISTS order_expenses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    expense_date DATE NOT NULL,
    note TEXT NULL,
    created_by VARCHAR(255) NULL,
    created_by_admin_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_expenses_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

SET @has_order_expenses_order_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_expenses'
      AND INDEX_NAME = 'idx_order_expenses_order_date'
);

SET @add_order_expenses_order_idx_sql := IF(
    @has_order_expenses_order_idx = 0,
    'CREATE INDEX idx_order_expenses_order_date ON order_expenses(order_id, expense_date)',
    'SELECT 1'
);
PREPARE stmt_add_order_expenses_order_idx FROM @add_order_expenses_order_idx_sql;
EXECUTE stmt_add_order_expenses_order_idx;
DEALLOCATE PREPARE stmt_add_order_expenses_order_idx;

SET @has_order_expenses_kind_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_expenses'
      AND INDEX_NAME = 'idx_order_expenses_kind'
);

SET @add_order_expenses_kind_idx_sql := IF(
    @has_order_expenses_kind_idx = 0,
    'CREATE INDEX idx_order_expenses_kind ON order_expenses(kind, expense_date)',
    'SELECT 1'
);
PREPARE stmt_add_order_expenses_kind_idx FROM @add_order_expenses_kind_idx_sql;
EXECUTE stmt_add_order_expenses_kind_idx;
DEALLOCATE PREPARE stmt_add_order_expenses_kind_idx;

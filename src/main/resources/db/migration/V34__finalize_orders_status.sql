-- État final canonique des commandes:
-- - la colonne métier est `orders.status`
-- - les éventuelles données présentes dans `order_status` sont recopiées
-- - l'index de filtrage porte sur `status`
-- - la colonne legacy `order_status` est supprimée si elle existe

SET @has_status = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'status'
);

SET @has_order_status = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'order_status'
);

SET @add_status_sql = IF(
    @has_status = 0,
    'ALTER TABLE orders ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT '''' AFTER order_number',
    'SELECT 1'
);
PREPARE stmt_add_status FROM @add_status_sql;
EXECUTE stmt_add_status;
DEALLOCATE PREPARE stmt_add_status;

SET @backfill_status_sql = IF(
    @has_order_status > 0,
    'UPDATE orders SET status = order_status WHERE (status IS NULL OR status = '''') AND order_status IS NOT NULL AND order_status <> ''''',
    'SELECT 1'
);
PREPARE stmt_backfill_status FROM @backfill_status_sql;
EXECUTE stmt_backfill_status;
DEALLOCATE PREPARE stmt_backfill_status;

UPDATE orders SET status = 'PENDING_CONFIRMATION' WHERE UPPER(status) = 'SUBMITTED';
UPDATE orders SET status = 'CANCELLED' WHERE UPPER(status) = 'CANCELED';
UPDATE orders SET status = 'FULFILLED' WHERE UPPER(status) = 'DELIVERED';

SET @has_idx_orders_order_status_created_at = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_order_status_created_at'
);

SET @drop_idx_orders_order_status_created_at_sql = IF(
    @has_idx_orders_order_status_created_at > 0,
    'DROP INDEX idx_orders_order_status_created_at ON orders',
    'SELECT 1'
);
PREPARE stmt_drop_idx_orders_order_status_created_at FROM @drop_idx_orders_order_status_created_at_sql;
EXECUTE stmt_drop_idx_orders_order_status_created_at;
DEALLOCATE PREPARE stmt_drop_idx_orders_order_status_created_at;

SET @has_idx_orders_status_created_at = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_status_created_at'
);

SET @add_idx_orders_status_created_at_sql = IF(
    @has_idx_orders_status_created_at = 0,
    'CREATE INDEX idx_orders_status_created_at ON orders(status, created_at)',
    'SELECT 1'
);
PREPARE stmt_add_idx_orders_status_created_at FROM @add_idx_orders_status_created_at_sql;
EXECUTE stmt_add_idx_orders_status_created_at;
DEALLOCATE PREPARE stmt_add_idx_orders_status_created_at;

SET @drop_order_status_sql = IF(
    @has_order_status > 0,
    'ALTER TABLE orders DROP COLUMN order_status',
    'SELECT 1'
);
PREPARE stmt_drop_order_status FROM @drop_order_status_sql;
EXECUTE stmt_drop_order_status;
DEALLOCATE PREPARE stmt_drop_order_status;

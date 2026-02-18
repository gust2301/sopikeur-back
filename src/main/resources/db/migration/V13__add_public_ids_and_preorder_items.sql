-- Public UUID ids for front contracts + preorder items structure.

SET @has_quote_public_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quote_requests' AND COLUMN_NAME = 'public_id'
);
SET @add_quote_public_id_sql := IF(
    @has_quote_public_id = 0,
    'ALTER TABLE quote_requests ADD COLUMN public_id VARCHAR(36) NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_public_id FROM @add_quote_public_id_sql;
EXECUTE stmt_add_quote_public_id;
DEALLOCATE PREPARE stmt_add_quote_public_id;

UPDATE quote_requests SET public_id = UUID() WHERE public_id IS NULL;

SET @has_idx_quote_public_id := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quote_requests' AND INDEX_NAME = 'uk_quote_requests_public_id'
);
SET @add_idx_quote_public_id_sql := IF(
    @has_idx_quote_public_id = 0,
    'ALTER TABLE quote_requests ADD CONSTRAINT uk_quote_requests_public_id UNIQUE (public_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_quote_public_id FROM @add_idx_quote_public_id_sql;
EXECUTE stmt_add_idx_quote_public_id;
DEALLOCATE PREPARE stmt_add_idx_quote_public_id;

SET @has_preorder_public_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preorder_requests' AND COLUMN_NAME = 'public_id'
);
SET @add_preorder_public_id_sql := IF(
    @has_preorder_public_id = 0,
    'ALTER TABLE preorder_requests ADD COLUMN public_id VARCHAR(36) NULL',
    'SELECT 1'
);
PREPARE stmt_add_preorder_public_id FROM @add_preorder_public_id_sql;
EXECUTE stmt_add_preorder_public_id;
DEALLOCATE PREPARE stmt_add_preorder_public_id;

UPDATE preorder_requests SET public_id = UUID() WHERE public_id IS NULL;

SET @has_idx_preorder_public_id := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preorder_requests' AND INDEX_NAME = 'uk_preorder_requests_public_id'
);
SET @add_idx_preorder_public_id_sql := IF(
    @has_idx_preorder_public_id = 0,
    'ALTER TABLE preorder_requests ADD CONSTRAINT uk_preorder_requests_public_id UNIQUE (public_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_preorder_public_id FROM @add_idx_preorder_public_id_sql;
EXECUTE stmt_add_idx_preorder_public_id;
DEALLOCATE PREPARE stmt_add_idx_preorder_public_id;

SET @has_order_public_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'public_id'
);
SET @add_order_public_id_sql := IF(
    @has_order_public_id = 0,
    'ALTER TABLE orders ADD COLUMN public_id VARCHAR(36) NULL',
    'SELECT 1'
);
PREPARE stmt_add_order_public_id FROM @add_order_public_id_sql;
EXECUTE stmt_add_order_public_id;
DEALLOCATE PREPARE stmt_add_order_public_id;

UPDATE orders SET public_id = UUID() WHERE public_id IS NULL;

SET @has_idx_order_public_id := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND INDEX_NAME = 'uk_orders_public_id'
);
SET @add_idx_order_public_id_sql := IF(
    @has_idx_order_public_id = 0,
    'ALTER TABLE orders ADD CONSTRAINT uk_orders_public_id UNIQUE (public_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_order_public_id FROM @add_idx_order_public_id_sql;
EXECUTE stmt_add_idx_order_public_id;
DEALLOCATE PREPARE stmt_add_idx_order_public_id;

SET @has_order_delivery_json := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'delivery_json'
);
SET @add_order_delivery_json_sql := IF(
    @has_order_delivery_json = 0,
    'ALTER TABLE orders ADD COLUMN delivery_json JSON NULL',
    'SELECT 1'
);
PREPARE stmt_add_order_delivery_json FROM @add_order_delivery_json_sql;
EXECUTE stmt_add_order_delivery_json;
DEALLOCATE PREPARE stmt_add_order_delivery_json;

CREATE TABLE IF NOT EXISTS preorder_request_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    preorder_request_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_snapshot VARCHAR(100) NOT NULL,
    qty DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_preorder_request_items_preorder
        FOREIGN KEY (preorder_request_id) REFERENCES preorder_requests(id),
    CONSTRAINT fk_preorder_request_items_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);

SET @has_idx_preorder_items_preorder_id := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preorder_request_items' AND INDEX_NAME = 'idx_preorder_request_items_preorder_id'
);
SET @add_idx_preorder_items_preorder_id_sql := IF(
    @has_idx_preorder_items_preorder_id = 0,
    'CREATE INDEX idx_preorder_request_items_preorder_id ON preorder_request_items(preorder_request_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_preorder_items_preorder_id FROM @add_idx_preorder_items_preorder_id_sql;
EXECUTE stmt_add_idx_preorder_items_preorder_id;
DEALLOCATE PREPARE stmt_add_idx_preorder_items_preorder_id;

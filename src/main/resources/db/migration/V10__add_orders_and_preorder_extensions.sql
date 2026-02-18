-- V10: flux e-commerce (orders) + enrichissement preorders + traçabilité stock.

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,

    full_name VARCHAR(255),
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),

    city_zone VARCHAR(100),
    needs_installation BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,

    source VARCHAR(50),
    ip VARCHAR(64),
    user_agent VARCHAR(500),

    confirmed_at TIMESTAMP NULL,
    fulfilled_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

SET @has_idx_orders_status_created_at := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_status_created_at'
);
SET @add_idx_orders_status_created_at_sql := IF(
    @has_idx_orders_status_created_at = 0,
    'CREATE INDEX idx_orders_status_created_at ON orders(status, created_at)',
    'SELECT 1'
);
PREPARE stmt_add_idx_orders_status_created_at FROM @add_idx_orders_status_created_at_sql;
EXECUTE stmt_add_idx_orders_status_created_at;
DEALLOCATE PREPARE stmt_add_idx_orders_status_created_at;

SET @has_idx_orders_phone := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_phone'
);
SET @add_idx_orders_phone_sql := IF(
    @has_idx_orders_phone = 0,
    'CREATE INDEX idx_orders_phone ON orders(phone)',
    'SELECT 1'
);
PREPARE stmt_add_idx_orders_phone FROM @add_idx_orders_phone_sql;
EXECUTE stmt_add_idx_orders_phone;
DEALLOCATE PREPARE stmt_add_idx_orders_phone;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    sku_snapshot VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    qty INT NOT NULL,
    unit_price_snapshot DECIMAL(12, 2) NOT NULL,
    line_total_snapshot DECIMAL(12, 2) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

SET @has_idx_order_items_order_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND INDEX_NAME = 'idx_order_items_order_id'
);
SET @add_idx_order_items_order_id_sql := IF(
    @has_idx_order_items_order_id = 0,
    'CREATE INDEX idx_order_items_order_id ON order_items(order_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_order_items_order_id FROM @add_idx_order_items_order_id_sql;
EXECUTE stmt_add_idx_order_items_order_id;
DEALLOCATE PREPARE stmt_add_idx_order_items_order_id;

SET @has_idx_order_items_product_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_items'
      AND INDEX_NAME = 'idx_order_items_product_id'
);
SET @add_idx_order_items_product_id_sql := IF(
    @has_idx_order_items_product_id = 0,
    'CREATE INDEX idx_order_items_product_id ON order_items(product_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_order_items_product_id FROM @add_idx_order_items_product_id_sql;
EXECUTE stmt_add_idx_order_items_product_id;
DEALLOCATE PREPARE stmt_add_idx_order_items_product_id;

-- Extension de preorder_requests pour rapprocher la structure du nouveau front.
-- Compat MySQL: pas de "ADD COLUMN IF NOT EXISTS" multi-colonnes.
SET @has_preorder_product_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND COLUMN_NAME = 'product_id'
);
SET @add_preorder_product_id_sql := IF(
    @has_preorder_product_id = 0,
    'ALTER TABLE preorder_requests ADD COLUMN product_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_add_preorder_product_id FROM @add_preorder_product_id_sql;
EXECUTE stmt_add_preorder_product_id;
DEALLOCATE PREPARE stmt_add_preorder_product_id;

SET @has_preorder_city_zone := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND COLUMN_NAME = 'city_zone'
);
SET @add_preorder_city_zone_sql := IF(
    @has_preorder_city_zone = 0,
    'ALTER TABLE preorder_requests ADD COLUMN city_zone VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_preorder_city_zone FROM @add_preorder_city_zone_sql;
EXECUTE stmt_add_preorder_city_zone;
DEALLOCATE PREPARE stmt_add_preorder_city_zone;

SET @has_preorder_accepts_delay := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND COLUMN_NAME = 'accepts_delay'
);
SET @add_preorder_accepts_delay_sql := IF(
    @has_preorder_accepts_delay = 0,
    'ALTER TABLE preorder_requests ADD COLUMN accepts_delay BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE stmt_add_preorder_accepts_delay FROM @add_preorder_accepts_delay_sql;
EXECUTE stmt_add_preorder_accepts_delay;
DEALLOCATE PREPARE stmt_add_preorder_accepts_delay;

SET @has_preorder_eta := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND COLUMN_NAME = 'eta'
);
SET @add_preorder_eta_sql := IF(
    @has_preorder_eta = 0,
    'ALTER TABLE preorder_requests ADD COLUMN eta DATE NULL',
    'SELECT 1'
);
PREPARE stmt_add_preorder_eta FROM @add_preorder_eta_sql;
EXECUTE stmt_add_preorder_eta;
DEALLOCATE PREPARE stmt_add_preorder_eta;

SET @has_preorder_unit := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND COLUMN_NAME = 'unit'
);
SET @add_preorder_unit_sql := IF(
    @has_preorder_unit = 0,
    'ALTER TABLE preorder_requests ADD COLUMN unit VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt_add_preorder_unit FROM @add_preorder_unit_sql;
EXECUTE stmt_add_preorder_unit;
DEALLOCATE PREPARE stmt_add_preorder_unit;

-- FK précommande -> produit (slug historique conservé pour migration progressive).
SET @has_fk_preorder_product := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND CONSTRAINT_NAME = 'fk_preorder_requests_product'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @add_fk_preorder_product_sql := IF(
    @has_fk_preorder_product = 0,
    'ALTER TABLE preorder_requests ADD CONSTRAINT fk_preorder_requests_product FOREIGN KEY (product_id) REFERENCES products(id)',
    'SELECT 1'
);
PREPARE stmt_add_fk_preorder_product FROM @add_fk_preorder_product_sql;
EXECUTE stmt_add_fk_preorder_product;
DEALLOCATE PREPARE stmt_add_fk_preorder_product;

SET @has_idx_preorder_status_created_at := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND INDEX_NAME = 'idx_preorder_status_created_at'
);
SET @add_idx_preorder_status_created_at_sql := IF(
    @has_idx_preorder_status_created_at = 0,
    'CREATE INDEX idx_preorder_status_created_at ON preorder_requests(status, created_at)',
    'SELECT 1'
);
PREPARE stmt_add_idx_preorder_status_created_at FROM @add_idx_preorder_status_created_at_sql;
EXECUTE stmt_add_idx_preorder_status_created_at;
DEALLOCATE PREPARE stmt_add_idx_preorder_status_created_at;

SET @has_idx_preorder_product_id_created_at := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND INDEX_NAME = 'idx_preorder_product_id_created_at'
);
SET @add_idx_preorder_product_id_created_at_sql := IF(
    @has_idx_preorder_product_id_created_at = 0,
    'CREATE INDEX idx_preorder_product_id_created_at ON preorder_requests(product_id, created_at)',
    'SELECT 1'
);
PREPARE stmt_add_idx_preorder_product_id_created_at FROM @add_idx_preorder_product_id_created_at_sql;
EXECUTE stmt_add_idx_preorder_product_id_created_at;
DEALLOCATE PREPARE stmt_add_idx_preorder_product_id_created_at;

SET @has_idx_preorder_phone := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preorder_requests'
      AND INDEX_NAME = 'idx_preorder_phone'
);
SET @add_idx_preorder_phone_sql := IF(
    @has_idx_preorder_phone = 0,
    'CREATE INDEX idx_preorder_phone ON preorder_requests(phone)',
    'SELECT 1'
);
PREPARE stmt_add_idx_preorder_phone FROM @add_idx_preorder_phone_sql;
EXECUTE stmt_add_idx_preorder_phone;
DEALLOCATE PREPARE stmt_add_idx_preorder_phone;

-- Traçabilité fine des mouvements stock vers commandes/références métier.
SET @has_stock_movements_reference_type := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_movements'
      AND COLUMN_NAME = 'reference_type'
);
SET @add_stock_movements_reference_type_sql := IF(
    @has_stock_movements_reference_type = 0,
    'ALTER TABLE stock_movements ADD COLUMN reference_type VARCHAR(30) NULL',
    'SELECT 1'
);
PREPARE stmt_add_stock_movements_reference_type FROM @add_stock_movements_reference_type_sql;
EXECUTE stmt_add_stock_movements_reference_type;
DEALLOCATE PREPARE stmt_add_stock_movements_reference_type;

SET @has_stock_movements_reference_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_movements'
      AND COLUMN_NAME = 'reference_id'
);
SET @add_stock_movements_reference_id_sql := IF(
    @has_stock_movements_reference_id = 0,
    'ALTER TABLE stock_movements ADD COLUMN reference_id VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_stock_movements_reference_id FROM @add_stock_movements_reference_id_sql;
EXECUTE stmt_add_stock_movements_reference_id;
DEALLOCATE PREPARE stmt_add_stock_movements_reference_id;

SET @has_idx_stock_movements_reference := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_movements'
      AND INDEX_NAME = 'idx_stock_movements_reference'
);
SET @add_idx_stock_movements_reference_sql := IF(
    @has_idx_stock_movements_reference = 0,
    'CREATE INDEX idx_stock_movements_reference ON stock_movements(reference_type, reference_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_stock_movements_reference FROM @add_idx_stock_movements_reference_sql;
EXECUTE stmt_add_idx_stock_movements_reference;
DEALLOCATE PREPARE stmt_add_idx_stock_movements_reference;

-- Garde-fous de cohérence stock.
-- MySQL 8.0.16+ applique les CHECK constraints.
SET @has_chk_quantity_non_negative := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_items'
      AND CONSTRAINT_NAME = 'chk_stock_items_quantity_non_negative'
);

SET @add_chk_quantity_non_negative_sql := IF(
    @has_chk_quantity_non_negative = 0,
    'ALTER TABLE stock_items ADD CONSTRAINT chk_stock_items_quantity_non_negative CHECK (quantity >= 0)',
    'SELECT 1'
);
PREPARE stmt_add_chk_quantity_non_negative FROM @add_chk_quantity_non_negative_sql;
EXECUTE stmt_add_chk_quantity_non_negative;
DEALLOCATE PREPARE stmt_add_chk_quantity_non_negative;

SET @has_chk_reserved_non_negative := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_items'
      AND CONSTRAINT_NAME = 'chk_stock_items_reserved_non_negative'
);

SET @add_chk_reserved_non_negative_sql := IF(
    @has_chk_reserved_non_negative = 0,
    'ALTER TABLE stock_items ADD CONSTRAINT chk_stock_items_reserved_non_negative CHECK (reserved >= 0)',
    'SELECT 1'
);
PREPARE stmt_add_chk_reserved_non_negative FROM @add_chk_reserved_non_negative_sql;
EXECUTE stmt_add_chk_reserved_non_negative;
DEALLOCATE PREPARE stmt_add_chk_reserved_non_negative;

SET @has_chk_reserved_lte_quantity := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_items'
      AND CONSTRAINT_NAME = 'chk_stock_items_reserved_lte_quantity'
);

SET @add_chk_reserved_lte_quantity_sql := IF(
    @has_chk_reserved_lte_quantity = 0,
    'ALTER TABLE stock_items ADD CONSTRAINT chk_stock_items_reserved_lte_quantity CHECK (reserved <= quantity)',
    'SELECT 1'
);
PREPARE stmt_add_chk_reserved_lte_quantity FROM @add_chk_reserved_lte_quantity_sql;
EXECUTE stmt_add_chk_reserved_lte_quantity;
DEALLOCATE PREPARE stmt_add_chk_reserved_lte_quantity;

-- V11: extension du flux devis pro (BTP)
-- - enrichit quote_requests
-- - ajoute quote_request_items
-- - ajoute quote_request_packs

-- =========================
-- A) quote_requests (modifier)
-- =========================
SET @has_quote_customer_type := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'customer_type'
);
SET @add_quote_customer_type_sql := IF(
    @has_quote_customer_type = 0,
    'ALTER TABLE quote_requests ADD COLUMN customer_type VARCHAR(30) NOT NULL DEFAULT ''PROFESSIONNEL''',
    'SELECT 1'
);
PREPARE stmt_add_quote_customer_type FROM @add_quote_customer_type_sql;
EXECUTE stmt_add_quote_customer_type;
DEALLOCATE PREPARE stmt_add_quote_customer_type;

SET @has_quote_project_type := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'project_type'
);
SET @add_quote_project_type_sql := IF(
    @has_quote_project_type = 0,
    'ALTER TABLE quote_requests ADD COLUMN project_type VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_project_type FROM @add_quote_project_type_sql;
EXECUTE stmt_add_quote_project_type;
DEALLOCATE PREPARE stmt_add_quote_project_type;

SET @has_quote_city_zone := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'city_zone'
);
SET @add_quote_city_zone_sql := IF(
    @has_quote_city_zone = 0,
    'ALTER TABLE quote_requests ADD COLUMN city_zone VARCHAR(120) NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_city_zone FROM @add_quote_city_zone_sql;
EXECUTE stmt_add_quote_city_zone;
DEALLOCATE PREPARE stmt_add_quote_city_zone;

SET @has_quote_source := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'source'
);
SET @add_quote_source_sql := IF(
    @has_quote_source = 0,
    'ALTER TABLE quote_requests ADD COLUMN source VARCHAR(30) NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_source FROM @add_quote_source_sql;
EXECUTE stmt_add_quote_source;
DEALLOCATE PREPARE stmt_add_quote_source;

SET @has_quote_intent := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'intent'
);
SET @add_quote_intent_sql := IF(
    @has_quote_intent = 0,
    'ALTER TABLE quote_requests ADD COLUMN intent VARCHAR(20) NOT NULL DEFAULT ''quote''',
    'SELECT 1'
);
PREPARE stmt_add_quote_intent FROM @add_quote_intent_sql;
EXECUTE stmt_add_quote_intent;
DEALLOCATE PREPARE stmt_add_quote_intent;

SET @has_quote_channel := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'channel'
);
SET @add_quote_channel_sql := IF(
    @has_quote_channel = 0,
    'ALTER TABLE quote_requests ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT ''web''',
    'SELECT 1'
);
PREPARE stmt_add_quote_channel FROM @add_quote_channel_sql;
EXECUTE stmt_add_quote_channel;
DEALLOCATE PREPARE stmt_add_quote_channel;

SET @has_quote_assigned_to := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'assigned_to'
);
SET @add_quote_assigned_to_sql := IF(
    @has_quote_assigned_to = 0,
    'ALTER TABLE quote_requests ADD COLUMN assigned_to VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_assigned_to FROM @add_quote_assigned_to_sql;
EXECUTE stmt_add_quote_assigned_to;
DEALLOCATE PREPARE stmt_add_quote_assigned_to;

SET @has_quote_contacted_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'contacted_at'
);
SET @add_quote_contacted_at_sql := IF(
    @has_quote_contacted_at = 0,
    'ALTER TABLE quote_requests ADD COLUMN contacted_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_contacted_at FROM @add_quote_contacted_at_sql;
EXECUTE stmt_add_quote_contacted_at;
DEALLOCATE PREPARE stmt_add_quote_contacted_at;

SET @has_quote_closed_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND COLUMN_NAME = 'closed_at'
);
SET @add_quote_closed_at_sql := IF(
    @has_quote_closed_at = 0,
    'ALTER TABLE quote_requests ADD COLUMN closed_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt_add_quote_closed_at FROM @add_quote_closed_at_sql;
EXECUTE stmt_add_quote_closed_at;
DEALLOCATE PREPARE stmt_add_quote_closed_at;

SET @has_idx_quote_customer_type_created_at := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_requests'
      AND INDEX_NAME = 'idx_quote_customer_type_created_at'
);
SET @add_idx_quote_customer_type_created_at_sql := IF(
    @has_idx_quote_customer_type_created_at = 0,
    'CREATE INDEX idx_quote_customer_type_created_at ON quote_requests(customer_type, created_at)',
    'SELECT 1'
);
PREPARE stmt_add_idx_quote_customer_type_created_at FROM @add_idx_quote_customer_type_created_at_sql;
EXECUTE stmt_add_idx_quote_customer_type_created_at;
DEALLOCATE PREPARE stmt_add_idx_quote_customer_type_created_at;

-- ================================
-- B) quote_request_items (ajouter)
-- ================================
CREATE TABLE IF NOT EXISTS quote_request_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_request_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_slug_snapshot VARCHAR(150),
    sku_snapshot VARCHAR(100),
    qty DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quote_request_items_quote_request
        FOREIGN KEY (quote_request_id) REFERENCES quote_requests(id),
    CONSTRAINT fk_quote_request_items_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);

SET @has_idx_quote_request_items_quote_request_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_request_items'
      AND INDEX_NAME = 'idx_quote_request_items_quote_request_id'
);
SET @add_idx_quote_request_items_quote_request_id_sql := IF(
    @has_idx_quote_request_items_quote_request_id = 0,
    'CREATE INDEX idx_quote_request_items_quote_request_id ON quote_request_items(quote_request_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_quote_request_items_quote_request_id FROM @add_idx_quote_request_items_quote_request_id_sql;
EXECUTE stmt_add_idx_quote_request_items_quote_request_id;
DEALLOCATE PREPARE stmt_add_idx_quote_request_items_quote_request_id;

SET @has_idx_quote_request_items_product_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_request_items'
      AND INDEX_NAME = 'idx_quote_request_items_product_id'
);
SET @add_idx_quote_request_items_product_id_sql := IF(
    @has_idx_quote_request_items_product_id = 0,
    'CREATE INDEX idx_quote_request_items_product_id ON quote_request_items(product_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_quote_request_items_product_id FROM @add_idx_quote_request_items_product_id_sql;
EXECUTE stmt_add_idx_quote_request_items_product_id;
DEALLOCATE PREPARE stmt_add_idx_quote_request_items_product_id;

-- ===============================
-- C) quote_request_packs (ajouter)
-- ===============================
CREATE TABLE IF NOT EXISTS quote_request_packs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quote_request_id BIGINT NOT NULL,
    pack_code VARCHAR(80) NOT NULL,
    pack_label_snapshot VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quote_request_packs_quote_request
        FOREIGN KEY (quote_request_id) REFERENCES quote_requests(id)
);

SET @has_idx_quote_request_packs_quote_request_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_request_packs'
      AND INDEX_NAME = 'idx_quote_request_packs_quote_request_id'
);
SET @add_idx_quote_request_packs_quote_request_id_sql := IF(
    @has_idx_quote_request_packs_quote_request_id = 0,
    'CREATE INDEX idx_quote_request_packs_quote_request_id ON quote_request_packs(quote_request_id)',
    'SELECT 1'
);
PREPARE stmt_add_idx_quote_request_packs_quote_request_id FROM @add_idx_quote_request_packs_quote_request_id_sql;
EXECUTE stmt_add_idx_quote_request_packs_quote_request_id;
DEALLOCATE PREPARE stmt_add_idx_quote_request_packs_quote_request_id;

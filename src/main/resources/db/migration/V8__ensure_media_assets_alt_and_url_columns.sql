-- Hardening migration for heterogeneous schemas of media_assets
-- Guarantees columns expected by JPA exist: url (NOT NULL) and alt.

SET @has_url := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'url'
);

SET @has_alt := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'alt'
);

SET @has_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'path'
);

SET @has_name := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'name'
);

-- Ensure url exists
SET @add_url_sql := IF(
    @has_url = 0,
    'ALTER TABLE media_assets ADD COLUMN url VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt_add_url FROM @add_url_sql;
EXECUTE stmt_add_url;
DEALLOCATE PREPARE stmt_add_url;

-- Ensure alt exists
SET @add_alt_sql := IF(
    @has_alt = 0,
    'ALTER TABLE media_assets ADD COLUMN alt VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt_add_alt FROM @add_alt_sql;
EXECUTE stmt_add_alt;
DEALLOCATE PREPARE stmt_add_alt;

-- Backfill url from legacy path when available
SET @backfill_url_sql := IF(
    @has_path > 0,
    'UPDATE media_assets SET url = path WHERE url IS NULL OR url = ''''',
    'SELECT 1'
);
PREPARE stmt_backfill_url FROM @backfill_url_sql;
EXECUTE stmt_backfill_url;
DEALLOCATE PREPARE stmt_backfill_url;

-- Backfill alt from legacy name when available
SET @backfill_alt_sql := IF(
    @has_name > 0,
    'UPDATE media_assets SET alt = name WHERE alt IS NULL OR alt = ''''',
    'SELECT 1'
);
PREPARE stmt_backfill_alt FROM @backfill_alt_sql;
EXECUTE stmt_backfill_alt;
DEALLOCATE PREPARE stmt_backfill_alt;

-- JPA expects url NOT NULL
UPDATE media_assets SET url = '' WHERE url IS NULL;
ALTER TABLE media_assets MODIFY COLUMN url VARCHAR(500) NOT NULL;

-- Ensure compatibility for environments where media_assets.path does not exist

SET @has_media_assets_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'path'
);

SET @sql := IF(
    @has_media_assets_path = 0,
    'ALTER TABLE media_assets ADD COLUMN path VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill path from url when missing
UPDATE media_assets
SET path = url
WHERE (path IS NULL OR path = '')
  AND url IS NOT NULL
  AND url <> '';

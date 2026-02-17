-- Ensure media_assets schema matches JPA entity mapping (url/alt)
-- Some environments applied V4_1 that renamed columns to path/name.

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

SET @rename_path_sql := IF(
    @has_path > 0,
    'ALTER TABLE media_assets RENAME COLUMN path TO url',
    'SELECT 1'
);
PREPARE stmt_rename_path FROM @rename_path_sql;
EXECUTE stmt_rename_path;
DEALLOCATE PREPARE stmt_rename_path;

SET @rename_name_sql := IF(
    @has_name > 0,
    'ALTER TABLE media_assets RENAME COLUMN name TO alt',
    'SELECT 1'
);
PREPARE stmt_rename_name FROM @rename_name_sql;
EXECUTE stmt_rename_name;
DEALLOCATE PREPARE stmt_rename_name;

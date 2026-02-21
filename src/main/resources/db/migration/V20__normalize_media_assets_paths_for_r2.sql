-- Normalize media assets paths for Cloudflare R2 object keys
-- Convention: store relative paths without leading slash (e.g. spc/SPC001.png)

SET @has_path := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'media_assets'
    AND column_name = 'path'
);

SET @sql := IF(
  @has_path > 0,
  "UPDATE media_assets
   SET path = REGEXP_REPLACE(path, '^/?assets/', '')
   WHERE path IS NOT NULL
     AND path REGEXP '^/?assets/'",
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_path > 0,
  "UPDATE media_assets
   SET path = REGEXP_REPLACE(path, '^/+', '')
   WHERE path IS NOT NULL
     AND path REGEXP '^/+'",
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_path > 0,
  "UPDATE media_assets
   SET url = CONCAT('https://assets.sopikeur.sn/', path)
   WHERE path IS NOT NULL
     AND TRIM(path) <> ''
     AND (
       url IS NULL
       OR TRIM(url) = ''
       OR url NOT REGEXP '^https?://'
     )",
  "UPDATE media_assets
   SET url = CONCAT(
     'https://assets.sopikeur.sn/',
     REGEXP_REPLACE(
       REGEXP_REPLACE(
         REGEXP_REPLACE(url, '^https?://[^/]+/', ''),
         '^/?assets/',
         ''
       ),
       '^/+',
       ''
     )
   )
   WHERE url IS NOT NULL
     AND TRIM(url) <> ''"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

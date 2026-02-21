-- Normalize media assets paths for Cloudflare R2 object keys
-- Convention: store relative paths without leading slash (e.g. spc/SPC001.png)

UPDATE media_assets
SET path = REGEXP_REPLACE(path, '^/?assets/', '')
WHERE path IS NOT NULL
  AND path REGEXP '^/?assets/';

UPDATE media_assets
SET path = REGEXP_REPLACE(path, '^/+', '')
WHERE path IS NOT NULL
  AND path REGEXP '^/+';

UPDATE media_assets
SET url = CONCAT('https://assets.sopikeur.sn/', path)
WHERE path IS NOT NULL
  AND TRIM(path) <> ''
  AND (
    url IS NULL
    OR TRIM(url) = ''
    OR url NOT REGEXP '^https?://'
  );

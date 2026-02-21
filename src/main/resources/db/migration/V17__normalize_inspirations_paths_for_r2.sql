-- Normalize inspirations media paths for Cloudflare R2 object keys
-- Convention: store relative paths without leading slash (e.g. spc/SPC001_home.png)

UPDATE inspirations
SET cover_url = REGEXP_REPLACE(cover_url, '^/?assets/', '')
WHERE cover_url IS NOT NULL
  AND cover_url REGEXP '^/?assets/';

UPDATE inspirations
SET cover_url = REGEXP_REPLACE(cover_url, '^/+', '')
WHERE cover_url IS NOT NULL
  AND cover_url REGEXP '^/+';

UPDATE inspirations
SET gallery_urls = REPLACE(gallery_urls, '"/assets/', '"')
WHERE gallery_urls IS NOT NULL
  AND gallery_urls LIKE '%"/assets/%';

UPDATE inspirations
SET gallery_urls = REPLACE(gallery_urls, '"assets/', '"')
WHERE gallery_urls IS NOT NULL
  AND gallery_urls LIKE '%"assets/%';

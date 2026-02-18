-- Backfill delivery_json from legacy city_zone when missing.

UPDATE orders
SET delivery_json = JSON_OBJECT(
    'city', TRIM(SUBSTRING_INDEX(city_zone, ' - ', 1)),
    'area', CASE
        WHEN LOCATE(' - ', city_zone) > 0 THEN NULLIF(TRIM(SUBSTRING(city_zone, LOCATE(' - ', city_zone) + 3)), '')
        ELSE NULL
    END
)
WHERE delivery_json IS NULL
  AND city_zone IS NOT NULL
  AND TRIM(city_zone) <> '';

UPDATE quote_requests
SET delivery_json = JSON_OBJECT(
    'city', TRIM(SUBSTRING_INDEX(city_zone, ' - ', 1)),
    'area', CASE
        WHEN LOCATE(' - ', city_zone) > 0 THEN NULLIF(TRIM(SUBSTRING(city_zone, LOCATE(' - ', city_zone) + 3)), '')
        ELSE NULL
    END
)
WHERE delivery_json IS NULL
  AND city_zone IS NOT NULL
  AND TRIM(city_zone) <> '';

UPDATE preorder_requests
SET delivery_json = JSON_OBJECT(
    'city', TRIM(SUBSTRING_INDEX(city_zone, ' - ', 1)),
    'area', CASE
        WHEN LOCATE(' - ', city_zone) > 0 THEN NULLIF(TRIM(SUBSTRING(city_zone, LOCATE(' - ', city_zone) + 3)), '')
        ELSE NULL
    END
)
WHERE delivery_json IS NULL
  AND city_zone IS NOT NULL
  AND TRIM(city_zone) <> '';

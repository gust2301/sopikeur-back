-- Add HEXAGON product + media + real stock.

INSERT INTO products (
    sku, slug, name, description_short, description_long,
    price, unit, dimensions, status, type, featured, created_at, updated_at
)
VALUES (
    'HEXAGON', 'hexagon', 'HEXAGON',
    'Panneau acoustique HEXAGON en feutre noir.',
    'Panneau acoustique HEXAGON 600×600 mm, épaisseur 21 mm, feutre acoustique noir.',
    17000.00, 'FCFA / pièce', '600×600 • Épaisseur 21 mm',
    'ACTIVE', 'PANEL', 1, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    slug = VALUES(slug),
    name = VALUES(name),
    description_short = VALUES(description_short),
    description_long = VALUES(description_long),
    price = VALUES(price),
    unit = VALUES(unit),
    dimensions = VALUES(dimensions),
    status = VALUES(status),
    type = VALUES(type),
    featured = VALUES(featured),
    updated_at = VALUES(updated_at);

-- media_assets schema can vary depending on migration history:
-- legacy: path/name, current: url/alt, and some envs may temporarily expose both.
SET @has_media_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'path'
);
SET @has_media_url := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'url'
);
SET @has_media_name := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'name'
);
SET @has_media_alt := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'media_assets'
      AND COLUMN_NAME = 'alt'
);

SET @media_match_condition := IF(
    @has_media_path > 0 AND @has_media_url > 0,
    '(ma.path = ''/assets/panels/HEXAGON.png'' OR ma.url = ''/assets/panels/HEXAGON.png'')',
    IF(
        @has_media_path > 0,
        'ma.path = ''/assets/panels/HEXAGON.png''',
        IF(
            @has_media_url > 0,
            'ma.url = ''/assets/panels/HEXAGON.png''',
            '1 = 0'
        )
    )
);

SET @media_exists_sql := CONCAT(
    'SELECT COUNT(*) INTO @hexagon_media_exists ',
    'FROM media_assets ma ',
    'JOIN products p ON p.id = ma.product_id ',
    'WHERE p.sku = ''HEXAGON'' AND ',
    @media_match_condition
);
PREPARE stmt_media_exists FROM @media_exists_sql;
EXECUTE stmt_media_exists;
DEALLOCATE PREPARE stmt_media_exists;

SET @media_columns := 'size, sort_order, is_cover, product_id, inspiration_id';
SET @media_values := 'NULL, 1, 1, p.id, NULL';

SET @media_columns := IF(@has_media_path > 0, CONCAT('path, ', @media_columns), @media_columns);
SET @media_values := IF(@has_media_path > 0, CONCAT('''/assets/panels/HEXAGON.png'', ', @media_values), @media_values);

SET @media_columns := IF(@has_media_url > 0, CONCAT('url, ', @media_columns), @media_columns);
SET @media_values := IF(@has_media_url > 0, CONCAT('''/assets/panels/HEXAGON.png'', ', @media_values), @media_values);

SET @media_columns := IF(@has_media_name > 0, CONCAT('name, ', @media_columns), @media_columns);
SET @media_values := IF(@has_media_name > 0, CONCAT('''HEXAGON'', ', @media_values), @media_values);

SET @media_columns := IF(@has_media_alt > 0, CONCAT('alt, ', @media_columns), @media_columns);
SET @media_values := IF(@has_media_alt > 0, CONCAT('''HEXAGON'', ', @media_values), @media_values);

SET @insert_media_sql := IF(
    @hexagon_media_exists = 0,
    CONCAT(
        'INSERT INTO media_assets (', @media_columns, ') ',
        'SELECT ', @media_values, ' FROM products p WHERE p.sku = ''HEXAGON''' 
    ),
    'SELECT 1'
);
PREPARE stmt_insert_media FROM @insert_media_sql;
EXECUTE stmt_insert_media;
DEALLOCATE PREPARE stmt_insert_media;

INSERT INTO stock_items (product_id, quantity, reserved, created_at, updated_at, preorder_allowed)
SELECT p.id, 50, 0, NOW(), NOW(), 1
FROM products p
WHERE p.sku = 'HEXAGON'
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    reserved = VALUES(reserved),
    preorder_allowed = VALUES(preorder_allowed),
    updated_at = VALUES(updated_at);

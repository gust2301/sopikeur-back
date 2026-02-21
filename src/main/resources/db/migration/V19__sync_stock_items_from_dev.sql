-- Sync stock_items from DEV canonical values without destructive operations.
-- No DELETE/TRUNCATE/DROP. Preserve existing IDs and created_at on updates.

-- 1) Ensure HEXAGON product (id=1105) exists with canonical values.
INSERT INTO products (
    id, sku, slug, name, description_short, description_long, price, unit, dimensions,
    status, type, featured, created_at, updated_at
)
VALUES
    (1105,'HEXAGON','hexagon','HEXAGON','Panneau acoustique HEXAGON en feutre noir.','Panneau acoustique HEXAGON 600×600 mm, épaisseur 21 mm, feutre acoustique noir.',17000.00,'FCFA / pièce','600×600 • Épaisseur 21 mm','ACTIVE','PANEL',1,'2026-02-18 16:12:03','2026-02-18 16:12:03')
AS new
ON DUPLICATE KEY UPDATE
    sku = new.sku,
    slug = new.slug,
    name = new.name,
    description_short = new.description_short,
    description_long = new.description_long,
    price = new.price,
    unit = new.unit,
    dimensions = new.dimensions,
    status = new.status,
    type = new.type,
    featured = new.featured,
    updated_at = new.updated_at;

-- 2) Upsert stock_items by product_id with exact DEV values.
-- created_at is intentionally not updated for existing rows.
-- Only insert/update rows for product_ids that actually exist to avoid FK failures
-- on heterogeneous schemas/environments.
INSERT INTO stock_items (
    product_id, quantity, reserved, created_at, updated_at, preorder_allowed
)
SELECT
    v.product_id,
    v.quantity,
    v.reserved,
    v.created_at,
    v.updated_at,
    v.preorder_allowed
FROM (
    SELECT 1001 AS product_id, 0 AS quantity, 0 AS reserved, '2026-01-16 16:31:42' AS created_at, '2026-02-18 10:39:17' AS updated_at, 1 AS preorder_allowed
    UNION ALL SELECT 1002,500,0,'2026-01-16 16:31:42','2026-02-21 13:19:21',0
    UNION ALL SELECT 1003,300,0,'2026-01-16 16:31:42','2026-02-19 15:43:21',0
    UNION ALL SELECT 1004,0,0,'2026-01-16 16:31:42','2026-02-18 11:09:07',1
    UNION ALL SELECT 1005,0,0,'2026-01-16 16:31:42','2026-02-18 11:09:07',1
    UNION ALL SELECT 1101,50,0,'2026-01-16 16:31:42','2026-02-19 16:32:50',0
    UNION ALL SELECT 1102,0,0,'2026-01-16 16:31:42','2026-02-18 11:09:07',1
    UNION ALL SELECT 1105,50,0,'2026-02-18 16:12:03','2026-02-20 14:50:15',0
 ) v
JOIN products p ON p.id = v.product_id
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    reserved = VALUES(reserved),
    preorder_allowed = VALUES(preorder_allowed),
    updated_at = VALUES(updated_at);

-- 3) Manual verification queries.
-- SELECT * FROM stock_items WHERE product_id IN (1001,1002,1003,1004,1005,1101,1102,1105);
-- SELECT id, sku FROM products WHERE id=1105;

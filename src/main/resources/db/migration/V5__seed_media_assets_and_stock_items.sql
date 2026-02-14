-- V5__seed_media_assets_and_stock_items.sql
-- Seed: media_assets + stock_items

-- -------------------------
-- media_assets
-- id, path, name, size, sort_order, is_cover, product_id, inspiration_id
-- -------------------------
INSERT INTO media_assets (id, path, name, size, sort_order, is_cover, product_id, inspiration_id)
VALUES
    (2001, '/assets/spc/SPC001.png', 'SPC001', NULL, 1, 1, 1001, NULL),
    (2002, '/assets/spc/SPC001_home.png', 'SPC001 intérieur', NULL, 2, 0, 1001, NULL),

    (2003, '/assets/spc/SPC006.png', 'SPC006', NULL, 1, 1, 1002, NULL),
    (2004, '/assets/spc/SPC006_home.png', 'SPC006 intérieur', NULL, 2, 0, 1002, NULL),

    (2005, '/assets/spc/SPC014.png', 'SPC014', NULL, 1, 1, 1003, NULL),
    (2006, '/assets/spc/SPC014_home.png', 'SPC014 intérieur', NULL, 2, 0, 1003, NULL),

    (2007, '/assets/spc/SPC008.png', 'SPC008', NULL, 1, 1, 1004, NULL),
    (2008, '/assets/spc/SPC008_home.png', 'SPC008 intérieur', NULL, 2, 0, 1004, NULL),

    (2009, '/assets/spc/SPC011.png', 'SPC011', NULL, 1, 1, 1005, NULL),
    (2010, '/assets/spc/SPC011_home.png', 'SPC011 intérieur', NULL, 2, 0, 1005, NULL),

    (2101, '/assets/panels/M-60240-WAVE1.png', 'Wave1', NULL, 1, 1, 1101, NULL),
    (2102, '/assets/panels/wall_M-60240-WAVE1.png', 'Wave1 mur', NULL, 2, 0, 1101, NULL),

    (2103, '/assets/panels/HEXAGONB.png', 'Hexagon', NULL, 1, 1, 1102, NULL),
    (2104, '/assets/panels/HEXAGONB_office.png', 'Hexagon bureau', NULL, 2, 0, 1102, NULL),

    (2201, '/assets/spc/SPC001_home.png', 'Salon SPC001', NULL, 1, 1, NULL, 4001),
    (2202, '/assets/panels/SPC006_HEXAGONB_office.png', 'Bureau SPC006 + Hexagon', NULL, 1, 1, NULL, 4002),
    (2203, '/assets/panels/bed_M-60240-WAVE1.png', 'Chambre Wave1', NULL, 1, 1, NULL, 4003),
    (2204, '/assets/spc/SPC006_home.png', 'Chêne naturel clair', NULL, 1, 1, NULL, 4004),
    (2205, '/assets/spc/SPC014_home.png', 'Noyer profond', NULL, 1, 1, NULL, 4005),
    (2206, '/assets/spc/SPC001_home.png', 'Bois clair moderne', NULL, 1, 1, NULL, 4006),
    (2207, '/assets/spc/SPC008_home.png', 'Pierre beige chaleureuse', NULL, 1, 1, NULL, 4007),
    (2208, '/assets/spc/SPC011_home.png', 'Gris minéral contemporain', NULL, 1, 1, NULL, 4008),
    (2209, '/assets/panels/bed_HEXAGONB.png', 'Tête de lit', NULL, 1, 1, NULL, 4009),
    (2210, '/assets/panels/wall_HEXAGONB.png', 'Mur TV', NULL, 1, 1, NULL, 4010)
    AS new
ON DUPLICATE KEY UPDATE
                     path = new.path,
                     name = new.name,
                     size = new.size,
                     sort_order = new.sort_order,
                     is_cover = new.is_cover,
                     product_id = new.product_id,
                     inspiration_id = new.inspiration_id;

-- -------------------------
-- stock_items
-- id, product_id, quantity, reserved, created_at, updated_at, preorder_allowed
-- -------------------------
INSERT INTO stock_items (id, product_id, quantity, reserved, created_at, updated_at, preorder_allowed)
VALUES
    (3001, 1001, 0,   0, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 1),
    (3002, 1002, 80,  5, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 0),
    (3003, 1003, 400, 0, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 1),
    (3004, 1004, 60,  6, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 0),
    (3005, 1005, 45,  3, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 0),
    (3101, 1101, 20,  0, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 1),
    (3102, 1102, 40,  4, '2026-01-16 16:31:42', '2026-01-16 16:31:42', 0)
    AS new
ON DUPLICATE KEY UPDATE
                     product_id = new.product_id,
                     quantity = new.quantity,
                     reserved = new.reserved,
                     updated_at = new.updated_at,
                     preorder_allowed = new.preorder_allowed;
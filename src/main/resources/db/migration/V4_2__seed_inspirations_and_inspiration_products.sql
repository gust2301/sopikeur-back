-- Seed inspirations and inspiration/product links (restores missing V4.2 migration)

INSERT INTO inspirations (id, slug, title, tags, cover_url, gallery_urls, created_at, updated_at)
VALUES
    (4001, 'salon-spc001', 'Salon lumineux en SPC001', 'spc,home', '/assets/spc/SPC001_home.png', '["/assets/spc/SPC001_home.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4002, 'bureau-spc006-hexagon', 'Bureau mix SPC006 + Hexagon', 'spc,panels,office', '/assets/panels/SPC006_HEXAGONB_office.png', '["/assets/panels/SPC006_HEXAGONB_office.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4003, 'chambre-wave1', 'Chambre cosy Wave1', 'panels,bed', '/assets/panels/bed_M-60240-WAVE1.png', '["/assets/panels/bed_M-60240-WAVE1.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4004, 'spc006-natural-oak', 'Chêne naturel clair', 'spc,home', '/assets/spc/SPC006_home.png', '["/assets/spc/spc_home_before.png","/assets/spc/SPC006_home.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4005, 'spc014-dark-walnut', 'Noyer profond', 'spc,home', '/assets/spc/SPC014_home.png', '["/assets/spc/spc_home_before.png","/assets/spc/SPC014_home.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4006, 'spc001-light-wood', 'Bois clair moderne', 'spc,home', '/assets/spc/SPC001_home.png', '["/assets/spc/spc_home_before.png","/assets/spc/SPC001_home.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4007, 'spc008-warm-stone', 'Pierre beige chaleureuse', 'spc,home', '/assets/spc/SPC008_home.png', '["/assets/spc/spc_home_before.png","/assets/spc/SPC008_home.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4008, 'spc011-grey-mineral', 'Gris minéral contemporain', 'spc,home', '/assets/spc/SPC011_home.png', '["/assets/spc/spc_home_before.png","/assets/spc/SPC011_home.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4009, 'panel-tete-lit', 'Tête de lit', 'panels,bed', '/assets/panels/bed_HEXAGONB.png', '["/assets/panels/bed_HEXAGONB.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42'),
    (4010, 'panel-mur-tv', 'Mur TV', 'panels,wall', '/assets/panels/wall_HEXAGONB.png', '["/assets/panels/wall_HEXAGONB.png"]', '2026-01-16 16:31:42', '2026-01-16 16:31:42')
AS new
ON DUPLICATE KEY UPDATE
    slug = new.slug,
    title = new.title,
    tags = new.tags,
    cover_url = new.cover_url,
    gallery_urls = new.gallery_urls,
    updated_at = new.updated_at;

INSERT INTO inspiration_products (inspiration_id, product_id)
VALUES
    (4001, 1001),
    (4006, 1001),
    (4002, 1002),
    (4004, 1002),
    (4005, 1003),
    (4007, 1004),
    (4008, 1005),
    (4003, 1101),
    (4002, 1102),
    (4009, 1102),
    (4010, 1102)
AS new
ON DUPLICATE KEY UPDATE
    product_id = new.product_id;

-- V4__seed_products.sql
-- Seed catalogue produits (SPC + PANELS) pour la table `products`
-- MySQL 8.x

INSERT INTO products (
    id, sku, slug, name, description_short, description_long,
    price, unit, dimensions, status, type, featured, created_at, updated_at
)
VALUES
    (1001, 'SPC001', 'spc001-chene-miel', 'SPC001',
     'Chêne miel lumineux.',
     'Un chêne miel lumineux et chaleureux, idéal pour créer une ambiance douce.',
     20000.00, 'FCFA / m²', '2,2 m²/boîte • Épaisseur 5 mm', 'ACTIVE', 'SPC', 1,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42'),

    (1002, 'SPC006', 'spc006-chene-naturel', 'SPC006',
     'Chêne naturel clair.',
     'Chêne naturel clair pour des intérieurs lumineux et modernes.',
     20000.00, 'FCFA / m²', '2,2 m²/boîte • Épaisseur 5 mm', 'ACTIVE', 'SPC', 1,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42'),

    (1003, 'SPC014', 'spc014-noyer-profond', 'SPC014',
     'Noyer profond.',
     'Un noyer profond au veinage marqué pour un rendu premium.',
     20000.00, 'FCFA / m²', '2,2 m²/boîte • Épaisseur 5 mm', 'ACTIVE', 'SPC', 1,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42'),

    (1004, 'SPC008', 'spc008-pierre-beige', 'SPC008',
     'Pierre beige chaleureuse.',
     'Une pierre beige douce pour agrandir visuellement et apporter de la chaleur.',
     20000.00, 'FCFA / m²', '2,2 m²/boîte • Épaisseur 5 mm', 'ACTIVE', 'SPC', 0,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42'),

    (1005, 'SPC011', 'spc011-gris-minerai', 'SPC011',
     'Gris minéral contemporain.',
     'Un gris minéral élégant, parfait pour les intérieurs modernes.',
     20000.00, 'FCFA / m²', '2,2 m²/boîte • Épaisseur 5 mm', 'ACTIVE', 'SPC', 0,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42'),

    (1101, 'M-60240-WAVE1', 'wave1-noir', 'M-60240-WAVE1',
     'Panneau Wave noir.',
     'Panneau mural rainuré au motif “wave”, idéal pour un mur design (salon, chambre, cinéma).',
     35000.00, 'FCFA / pièce', '1200×600 • Épaisseur 21 mm', 'ACTIVE', 'PANEL', 1,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42'),

    (1102, 'HEXAGONB', 'hexagonb', 'HEXAGONB',
     'Panneau Hexagon noir.',
     'Panneau mural hexagonal pour composer un mur graphique et décoratif.',
     17000.00, 'FCFA / pièce', '600×600 • Épaisseur 21 mm', 'ACTIVE', 'PANEL', 1,
     '2026-01-16 16:31:42', '2026-01-16 16:31:42')

ON DUPLICATE KEY UPDATE
                     sku = VALUES(sku),
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
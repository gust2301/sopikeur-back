-- Synchronise les stocks réels et applique les garde-fous métier actuels.
-- Idempotent : updates ciblés + insert conditionnel.

-- 0) Assurer une ligne de stock pour chaque produit existant, sans casser les PK/FK.
INSERT INTO stock_items (product_id, quantity, reserved, created_at, updated_at, preorder_allowed)
SELECT p.id, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE
FROM products p
LEFT JOIN stock_items si ON si.product_id = p.id
WHERE si.product_id IS NULL;

-- 1) Mettre reserved à 0 partout (feature réservation inactive).
UPDATE stock_items
SET reserved = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE reserved <> 0;

-- 2) Mettre à jour les quantités SPC : 0 par défaut, exceptions explicites.
UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.quantity = 0,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.type = 'SPC'
  AND si.quantity <> 0;

UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.quantity = 300,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.sku = 'SPC006'
  AND si.quantity <> 300;

UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.quantity = 500,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.sku = 'SPC014'
  AND si.quantity <> 500;

-- 3) Mettre à jour les quantités PANELS demandées.
UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.quantity = 50,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.sku = 'M-60240-WAVE1'
  AND si.quantity <> 50;

UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.quantity = 50,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.sku = 'HEXAGON'
  AND si.quantity <> 50;

UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.quantity = 0,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.sku = 'HEXAGONB'
  AND si.quantity <> 0;

-- 4) Règle globale immédiate : jamais de précommande sur produit disponible.
-- Si quantity > 0 alors preorder_allowed = FALSE.
UPDATE stock_items
SET preorder_allowed = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE quantity > 0
  AND preorder_allowed <> FALSE;

-- 5) Règle SPC additionnelle : pour quantity = 0, autoriser la précommande.
-- (N'affecte pas les panels pour préserver leur logique existante quand quantity = 0.)
UPDATE stock_items si
JOIN products p ON p.id = si.product_id
SET si.preorder_allowed = TRUE,
    si.updated_at = CURRENT_TIMESTAMP
WHERE p.type = 'SPC'
  AND si.quantity = 0
  AND si.preorder_allowed <> TRUE;

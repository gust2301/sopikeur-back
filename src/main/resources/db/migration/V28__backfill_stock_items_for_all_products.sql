-- Crée un stock_item par défaut (quantity=0) pour tous les produits
-- qui n'en ont pas encore (produits créés avant l'auto-création au save).
INSERT INTO stock_items (product_id, quantity, reserved, preorder_allowed, created_at, updated_at)
SELECT p.id, 0, 0, FALSE, NOW(), NOW()
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM stock_items s WHERE s.product_id = p.id
);

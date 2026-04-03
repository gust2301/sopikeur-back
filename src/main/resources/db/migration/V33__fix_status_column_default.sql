-- La colonne `status` (ajoutée par V29) est NOT NULL sans default.
-- L'entité écrit dans `order_status`, donc `status` ne reçoit jamais de valeur.
-- On lui ajoute un default vide pour ne plus bloquer les INSERTs.
ALTER TABLE orders MODIFY COLUMN `status` VARCHAR(30) NOT NULL DEFAULT '';

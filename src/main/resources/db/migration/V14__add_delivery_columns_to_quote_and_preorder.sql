-- Tables quote_requests et preorder_requests existent déjà à cette étape.
-- Migration compatible MySQL sans ADD COLUMN IF NOT EXISTS.

ALTER TABLE quote_requests
    ADD COLUMN delivery_json JSON NULL,
    ADD COLUMN needs_installation TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE preorder_requests
    ADD COLUMN delivery_json JSON NULL,
    ADD COLUMN needs_installation TINYINT(1) NOT NULL DEFAULT 0;

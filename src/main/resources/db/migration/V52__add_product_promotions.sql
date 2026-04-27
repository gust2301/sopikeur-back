ALTER TABLE products
    ADD COLUMN promo_active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN promo_price DECIMAL(12, 2) NULL,
    ADD COLUMN promo_start_date DATE NULL,
    ADD COLUMN promo_end_date DATE NULL,
    ADD COLUMN promo_label VARCHAR(100) NULL;

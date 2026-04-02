-- No-op: les deux colonnes order_status et status coexistent déjà en staging.
-- L'entité JPA est mappée sur order_status (colonne NOT NULL d'origine).
-- Ce script est un no-op pour débloquer Flyway (était en état FAILED).
SELECT 1;

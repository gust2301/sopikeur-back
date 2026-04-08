INSERT INTO products (
    sku,
    slug,
    name,
    description_short,
    description_long,
    price,
    unit,
    dimensions,
    status,
    type,
    featured,
    created_at,
    updated_at
)
SELECT
    'SRV-LIVRAISON',
    'service-livraison',
    'Livraison',
    'Service de livraison',
    'Service de livraison',
    0.00,
    'forfait',
    NULL,
    'ACTIVE',
    'SERVICE',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE sku = 'SRV-LIVRAISON'
);

INSERT INTO products (
    sku,
    slug,
    name,
    description_short,
    description_long,
    price,
    unit,
    dimensions,
    status,
    type,
    featured,
    created_at,
    updated_at
)
SELECT
    'SRV-POSE',
    'service-pose',
    'Pose',
    'Service de pose',
    'Service de pose',
    0.00,
    'forfait',
    NULL,
    'ACTIVE',
    'SERVICE',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE sku = 'SRV-POSE'
);

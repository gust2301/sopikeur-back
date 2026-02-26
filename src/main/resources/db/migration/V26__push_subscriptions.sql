CREATE TABLE push_subscriptions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint    TEXT NOT NULL,
    p256dh      VARCHAR(512) NOT NULL,
    auth        VARCHAR(255) NOT NULL,
    admin_email VARCHAR(255),
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_push_endpoint (endpoint(500))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

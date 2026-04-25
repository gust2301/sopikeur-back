CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    excerpt VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    cover_url VARCHAR(500),
    status ENUM('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME,
    reading_time_minutes INT NOT NULL DEFAULT 3,
    meta_title VARCHAR(100),
    meta_description VARCHAR(200),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

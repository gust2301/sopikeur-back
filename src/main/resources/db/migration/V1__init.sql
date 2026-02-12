CREATE TABLE admin_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(150) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description_short VARCHAR(500),
    description_long TEXT,
    price DECIMAL(12, 2) NOT NULL,
    unit VARCHAR(50),
    dimensions VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE stock_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL,
    reserved INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE stock_movements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_stock_movement_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE inspirations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(150) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    tags VARCHAR(255),
    cover_url VARCHAR(500),
    gallery_urls TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE media_assets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    url VARCHAR(500) NOT NULL,
    alt VARCHAR(255),
    size BIGINT,
    sort_order INT,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    product_id BIGINT,
    inspiration_id BIGINT,
    CONSTRAINT fk_media_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_media_inspiration FOREIGN KEY (inspiration_id) REFERENCES inspirations(id)
);

CREATE TABLE inspiration_products (
    inspiration_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (inspiration_id, product_id),
    CONSTRAINT fk_inspiration_product_inspiration FOREIGN KEY (inspiration_id) REFERENCES inspirations(id),
    CONSTRAINT fk_inspiration_product_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE quote_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    message TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE contact_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    message TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE preorder_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    product_slug VARCHAR(150),
    quantity INT,
    message TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_products_type ON products(type);
CREATE INDEX idx_products_featured ON products(featured);
CREATE INDEX idx_inspiration_tags ON inspirations(tags);
CREATE INDEX idx_quote_status ON quote_requests(status);
CREATE INDEX idx_contact_status ON contact_messages(status);
CREATE INDEX idx_preorder_status ON preorder_requests(status);

INSERT INTO admin_users (username, password_hash, role)
VALUES ('admin', '$2a$10$Dow1D.8VIfYqZqXF/94aK.Onx9..dRrN1Vj9fHD0sVdVQS8S8/.EW', 'ADMIN');

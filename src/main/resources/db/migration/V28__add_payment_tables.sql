-- V28 : tables Stripe payment (payment_intents + payment_events)

CREATE TABLE payment_intents
(
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id                 VARCHAR(64)  NOT NULL,
    stripe_session_id         VARCHAR(255) NOT NULL,
    stripe_payment_intent_id  VARCHAR(255),
    order_id                  BIGINT,
    amount                    BIGINT       NOT NULL,
    currency                  VARCHAR(8)   NOT NULL DEFAULT 'xof',
    purpose                   VARCHAR(32)  NOT NULL,
    status                    VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    checkout_url              TEXT,
    created_at                DATETIME     NOT NULL,
    updated_at                DATETIME     NOT NULL,
    CONSTRAINT uq_pi_public_id         UNIQUE (public_id),
    CONSTRAINT uq_pi_stripe_session_id UNIQUE (stripe_session_id),
    CONSTRAINT fk_pi_order             FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE payment_events
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_intent_id  BIGINT       NOT NULL,
    stripe_event_id    VARCHAR(255) NOT NULL,
    event_type         VARCHAR(100) NOT NULL,
    payload            MEDIUMTEXT,
    processed_at       DATETIME     NOT NULL,
    CONSTRAINT uq_pe_stripe_event_id UNIQUE (stripe_event_id),
    CONSTRAINT fk_pe_payment_intent  FOREIGN KEY (payment_intent_id) REFERENCES payment_intents (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

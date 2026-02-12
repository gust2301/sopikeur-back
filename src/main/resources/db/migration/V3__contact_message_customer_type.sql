ALTER TABLE contact_messages
    ADD COLUMN customer_type VARCHAR(100);

ALTER TABLE contact_messages
    MODIFY COLUMN email VARCHAR(255) NULL;

-- Optimistic locking column for the product aggregate; existing rows start at 0
ALTER TABLE products
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

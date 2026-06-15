ALTER TABLE orders
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deletion_in_progress BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deletion_error TEXT NULL;

CREATE INDEX idx_orders_client_id_active_created_at
    ON orders(client_id, created_at DESC)
    WHERE is_deleted = FALSE AND deletion_in_progress = FALSE;

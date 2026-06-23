-- Timestamp отделён от created_at: повторная проверка обновляет только время
-- последнего validation request, не меняя дату загрузки документа.
ALTER TABLE order_documents
    ADD COLUMN validation_requested_at TIMESTAMP;

UPDATE order_documents
SET validation_requested_at = created_at
WHERE validation_requested_at IS NULL;

ALTER TABLE order_documents
    ALTER COLUMN validation_requested_at SET NOT NULL;

CREATE INDEX idx_order_documents_validation_retry
    ON order_documents(validation_status, validation_requested_at)
    WHERE is_deleted = FALSE AND is_document_deleted = FALSE;

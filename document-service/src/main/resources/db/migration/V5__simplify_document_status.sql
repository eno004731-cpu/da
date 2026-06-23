-- validation_status отвечает только за проверку документа.
-- is_document_deleted показывает намерение удалить файл,
-- is_deleted подтверждает завершённое физическое удаление.
ALTER TABLE order_documents
    ADD COLUMN validated_at TIMESTAMP NULL;

ALTER TABLE order_documents
    DROP CONSTRAINT IF EXISTS chk_order_documents_status;

ALTER TABLE order_documents
    DROP COLUMN status;

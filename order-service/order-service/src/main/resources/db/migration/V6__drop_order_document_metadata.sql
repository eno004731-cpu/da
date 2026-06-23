-- Метаданные документов теперь читаются напрямую из document-service.
-- Order-service сохраняет только данные заказа и состояние Kafka-обработки.
DROP TABLE IF EXISTS order_document_metadata;

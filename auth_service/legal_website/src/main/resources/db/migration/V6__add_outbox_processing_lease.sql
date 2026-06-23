-- Время начала обработки позволяет восстановить событие,
-- если приложение завершилось до Kafka callback.
ALTER TABLE outbox_events
    ADD COLUMN processing_started_at TIMESTAMP;

-- Токен отличает текущую попытку от запоздалого callback предыдущей.
ALTER TABLE outbox_events
    ADD COLUMN processing_token UUID;

-- Ошибку сохраняем даже после успешной повторной публикации,
-- поэтому отдельно фиксируем время её возникновения.
ALTER TABLE outbox_events
    ADD COLUMN last_error_at TIMESTAMP;

-- Индекс ускоряет единый запрос publisher:
-- NEW, готовые FAILED и зависшие PROCESSING конкретного типа.
CREATE INDEX idx_outbox_events_publish_candidates
    ON outbox_events(status, event_type, next_retry_at, processing_started_at, created_at);

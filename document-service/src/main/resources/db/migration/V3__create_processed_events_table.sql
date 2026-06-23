CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    topic VARCHAR(255) NOT NULL,
    partition_no INTEGER NOT NULL,
    message_offset BIGINT NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    error_message TEXT NULL,
    payload JSONB NOT NULL,
    -- В БД разрешены только состояния, которые понимает обработчик входящих событий.
    CONSTRAINT chk_processed_events_status CHECK (
        status IN ('PROCESSED', 'FAILED', 'DEAD', 'RECEIVED')
    )
);

CREATE INDEX idx_document_processed_events_topic_partition_offset
    ON processed_events(topic, partition_no, message_offset);

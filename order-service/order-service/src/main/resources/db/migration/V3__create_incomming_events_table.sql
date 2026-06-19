-- incoming_events:
-- inbox-таблица для входящих Kafka-сообщений.
-- Нужна для идемпотентности и чтобы видеть, что сообщение уже принято/обработано.
CREATE TABLE incoming_events (
    id UUID PRIMARY KEY,

    event_id UUID NOT NULL UNIQUE,
    topic VARCHAR(255) NOT NULL,
    partition_no INTEGER NOT NULL,
    message_offset BIGINT NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,

    aggregate_id UUID NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NULL,

    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    last_error TEXT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,

    CONSTRAINT uq_incoming_events_topic_partition_offset
        UNIQUE (topic, partition_no, message_offset),
    CONSTRAINT chk_incoming_events_status CHECK (
        status IN ('RECEIVED', 'PROCESSED', 'FAILED', 'DEAD','ON_DELETE')
    )
);

-- Быстрый поиск события по его бизнес-id из сообщения.
CREATE INDEX idx_incoming_events_event_id
    ON incoming_events(event_id);

-- Нужен для фоновой обработки и ретраев.
CREATE INDEX idx_incoming_events_status_received_at
    ON incoming_events(status, received_at);

CREATE INDEX idx_incoming_events_next_retry_at
    ON incoming_events(next_retry_at);

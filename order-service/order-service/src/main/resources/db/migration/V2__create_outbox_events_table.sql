-- outbox_events:
-- хранит события order-service, которые должны надёжно публиковаться в Kafka.
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    last_error TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    CONSTRAINT chk_outbox_events_status CHECK (
        status IN ('NEW', 'PUBLISHED', 'FAILED','DEAD')
    )
);

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events(status, created_at);

CREATE INDEX idx_outbox_events_next_retry_at
    ON outbox_events(next_retry_at);

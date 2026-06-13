-- inbox_events:
-- хранит входящие события, чтобы consumer мог обрабатывать их идемпотентно и с retry-механикой.
CREATE TABLE inbox_events (
    id UUID PRIMARY KEY,

    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    last_error TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    CONSTRAINT chk_inbox_events_status CHECK (
        status IN ('RECEIVED', 'PROCESSED', 'FAILED', 'DEAD')
    )
);

CREATE INDEX idx_inbox_events_status_created_at
    ON inbox_events(status, created_at);

CREATE INDEX idx_inbox_events_next_retry_at
    ON inbox_events(next_retry_at);

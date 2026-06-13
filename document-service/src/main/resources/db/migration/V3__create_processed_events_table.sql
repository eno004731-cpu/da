CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    topic VARCHAR(255) NOT NULL,
    partition_no INTEGER NOT NULL,
    message_offset BIGINT NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    error_message TEXT NULL
);

CREATE INDEX idx_document_processed_events_topic_partition_offset
    ON processed_events(topic, partition_no, message_offset);

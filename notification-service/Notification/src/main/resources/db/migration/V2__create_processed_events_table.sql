-- processed_events:
-- хранит уже обработанные Kafka-события, чтобы consumer не дублировал работу.
CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE, -- Уникальный id события для защиты от дублей.
    topic VARCHAR(255) NOT NULL, -- Kafka topic, из которого прочитано сообщение.
    partition_no INTEGER NOT NULL, -- Partition, где лежало сообщение.
    message_offset BIGINT NOT NULL, -- Offset сообщения внутри partition.
    consumer_group VARCHAR(255) NOT NULL, -- Consumer group, которая обработала событие.
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL, -- Итог обработки: PROCESSED, FAILED, SKIPPED.
    error_message TEXT NULL -- Текст ошибки, если обработка завершилась неуспешно.
);

-- Быстрый поиск уже обработанного сообщения по topic/partition/offset.
CREATE INDEX idx_processed_events_topic_partition_offset
    ON processed_events(topic, partition_no, message_offset);

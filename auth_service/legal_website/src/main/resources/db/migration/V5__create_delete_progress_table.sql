-- Быстрый статус в users позволяет сразу запретить вход и новые операции,
-- не выполняя JOIN с таблицей процесса удаления при каждом запросе.
ALTER TABLE users
    ADD COLUMN deletion_status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD CONSTRAINT chk_users_deletion_status CHECK (
        deletion_status IN (
            'ACTIVE',
            'DELETION_REQUESTED',
            'DELETION_IN_PROGRESS',
            'DELETION_FAILED',
            'DELETED'
        )
    );

-- Отдельная таблица хранит техническое состояние долгого процесса:
-- ошибки, повторные попытки и время завершения.
CREATE TABLE user_deletion_process (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    last_error TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_user_deletion_process_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT chk_user_deletion_process_status CHECK (
        status IN (
            'DELETION_REQUESTED',
            'DELETION_IN_PROGRESS',
            'DELETION_FAILED',
            'DELETED'
        )
    ),

    CONSTRAINT chk_user_deletion_process_retry_count CHECK (
        retry_count >= 0
    )
);

-- Scheduler будет искать незавершённые процессы по статусу и времени повтора.
CREATE INDEX idx_user_deletion_process_retry
    ON user_deletion_process(status, next_retry_at);

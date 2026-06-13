-- orders:
-- основной агрегат клиентской заявки/заказа.
-- Пока здесь только поля, которые уже нужны frontend-контракту кабинета.
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    client_id BIGINT NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NULL,
    service_code VARCHAR(100) NOT NULL,
    service_name VARCHAR(255) NULL,
    title VARCHAR(255) NOT NULL,
    problem_description TEXT  NULL,
    status VARCHAR(30) NULL DEFAULT 'ON_REVIEW',
    client_revision_comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    CONSTRAINT chk_orders_status CHECK (
        status IN ('TODO', 'IN_PROGRESS', 'ON_REVIEW', 'REWORK', 'DONE', 'REJECTED')
    )
);

-- Кабинет клиента чаще всего читает свои заказы в обратном хронологическом порядке.
CREATE INDEX idx_orders_client_id_created_at
    ON orders(client_id, created_at DESC);

-- Доска юриста будет фильтровать заявки по статусам.
CREATE INDEX idx_orders_status_updated_at
    ON orders(status, updated_at DESC);

-- notification_deliveries:
-- хранит попытки и результат доставки уведомлений пользователю.
CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE, -- Событие, из которого родилась доставка.
    channel VARCHAR(20) NOT NULL, -- Канал уведомления, например EMAIL.
    template_code VARCHAR(100) NOT NULL, -- Код шаблона письма или уведомления.
    recipient VARCHAR(255) NOT NULL, -- Конечный адрес получателя.
    subject VARCHAR(255) NULL, -- Тема письма, если канал это email.
    payload JSONB NOT NULL, -- Данные для подстановки в шаблон.
    status VARCHAR(20) NOT NULL, -- Статус доставки: NEW, PROCESSING, SENT, FAILED, DEAD.
    provider_message_id VARCHAR(255) NULL, -- Идентификатор сообщения у внешнего провайдера.
    retry_count INTEGER NOT NULL DEFAULT 0, -- Сколько раз уже пробовали доставить уведомление.
    next_retry_at TIMESTAMP NULL, -- Когда делать следующую попытку отправки.
    last_error TEXT NULL, -- Последняя ошибка отправки.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL, -- Когда уведомление было реально отправлено.
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Быстрый выбор уведомлений, которые ждут отправки или повторной доставки.
CREATE INDEX idx_notification_deliveries_status_created_at
    ON notification_deliveries(status, created_at);

-- Быстрый выбор уведомлений, готовых к retry.
CREATE INDEX idx_notification_deliveries_next_retry_at
    ON notification_deliveries(next_retry_at);

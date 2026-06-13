# Philosophy Business Legal Platform

Учебно-практический production-like проект для сайта юридических услуг.

Цель проекта - не просто собрать сайт, а потренировать backend-архитектуру:
микросервисы, PostgreSQL, Kafka, outbox/inbox-подход, REST-контракты,
локальный compose-запуск и тесты с настоящей БД.

## Сервисы

```text
auth_service/legal_website        # регистрация, login, refresh/logout, профиль, auth events
order-service/order-service       # заказы клиента, order details, read-model документов
catalog-service                   # каталог юридических услуг
document-service                  # загрузка файлов, хранение документов, document events
notification-service/Notification # email delivery, inbox/outbox обработка notification events
front                             # статический frontend
docker/postgres/init              # init-скрипты PostgreSQL для локального compose
```

Порты при локальном compose-запуске:

```text
frontend         http://127.0.0.1:8000
auth-service     http://127.0.0.1:8081
order-service    http://127.0.0.1:8083
catalog-service  http://127.0.0.1:8084
document-service http://127.0.0.1:8085
```

## Локальный запуск

1. Создай локальный `.env` в корне проекта на основе `prod.env.example`.
2. Заполни dev-значения, не production-секреты.
3. Подними весь стек:

```bash
docker compose --env-file .env up --build
```

Фоновый режим:

```bash
docker compose --env-file .env up --build -d
```

Проверка compose-конфига:

```bash
docker compose --env-file .env config --quiet
```

Подробная инструкция лежит в [md files/LOCAL_DEV.md](./md%20files/LOCAL_DEV.md).

## Тесты

Запускать из корня каждого Maven-сервиса:

```bash
cd auth_service/legal_website && ./mvnw test
cd order-service/order-service && ./mvnw test
cd document-service && ./mvnw test
cd catalog-service && ./mvnw test
cd notification-service/Notification && ./mvnw test
```

Часть тестов использует Testcontainers/PostgreSQL. Для них должен быть запущен Docker Desktop.

## Document Metadata Flow

Пользовательский flow остаётся двухшаговым:

1. Frontend создаёт заказ через order-service и получает `orderId`.
2. Frontend отправляет файлы в `POST /api/client/orders/{orderId}/documents`.
3. order-service проверяет, что заказ принадлежит текущему клиенту.
4. order-service пересылает multipart-файлы во внутренний API document-service с `X-Internal-Service-Token`.
5. document-service сохраняет файл, запись документа и outbox-событие в одной транзакции.
6. outbox relay document-service публикует событие в Kafka topic `document.stored`.
7. order-service слушает `document.stored` и сохраняет локальную read-model запись в `order_document_metadata`.
8. `GET /api/client/orders/{orderId}` читает заказ и документы только из БД order-service.

Важный trade-off: после upload frontend сразу использует ответ upload endpoint, а read-model в
order-service догоняется через Kafka. Поэтому короткое окно eventual consistency допустимо.

## Документация

- [md files/LOCAL_DEV.md](./md%20files/LOCAL_DEV.md) - локальный запуск, env и тесты
- [md files/PROJECT_CONTEXT.md](./md%20files/PROJECT_CONTEXT.md) - контекст проекта
- [md files/API_ENDPOINT_ROADMAP.md](./md%20files/API_ENDPOINT_ROADMAP.md) - карта API
- [md files/ADMIN_PANEL_USERS_V1_PLAN.md](./md%20files/ADMIN_PANEL_USERS_V1_PLAN.md) - план admin users panel
- [md files/ARCHITECTURE_DEBT_AUDIT.md](./md%20files/ARCHITECTURE_DEBT_AUDIT.md) - текущий архитектурный долг

## Важные правила

- Реальные секреты не коммитятся.
- `.env`, `front/.env`, cookies, target-директории и локальные кэши должны оставаться вне git.
- document-service остаётся source of truth для файлов.
- order-service хранит только read-model метаданных документов для быстрого `GET order details`.
- Проект учебный, но решения оформляются так, чтобы было понятно, где production-like подход, а где осознанное упрощение.

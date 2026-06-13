# Локальный запуск

Этот документ описывает запуск всего проекта локально без VPS через `docker compose`.

## Что поднимается

```text
postgres
kafka
frontend
auth-service
order-service
catalog-service
document-service
notification-service
```

PostgreSQL при первом старте создаёт отдельные БД:

```text
legal_auth
legal_orders
legal_catalog
legal_documents
legal_notification
```

## Требования

- Docker Desktop
- Java 17+ для запуска Maven-тестов локально
- Maven wrapper уже лежит внутри сервисов, глобальный Maven не обязателен

## .env

Создай `.env` в корне проекта на основе `prod.env.example`.

```bash
cp prod.env.example .env
```

Для локальной разработки можно использовать такие значения:

```env
POSTGRES_USER=legal_app
POSTGRES_PASSWORD=legal_pass

AUTH_PUBLIC_PORT=8081
ORDER_PUBLIC_PORT=8083
CATALOG_PUBLIC_PORT=8084
DOCUMENT_PUBLIC_PORT=8085

JWT_SECRET=replace_with_local_64_hex_secret
APP_INTERNAL_SERVICE_TOKEN=local-dev-internal-token
JWT_ACCESS_MINUTES=15
JWT_REFRESH_DAYS=30
GOOGLE_CLIENT_ID=

APP_FRONTEND_BASE_URL=http://127.0.0.1:8000
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://127.0.0.1:*,http://localhost:*

SPRING_MAIL_HOST=127.0.0.1
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
APP_MAIL_FROM=no-reply@localhost
```

Для `JWT_SECRET` и `APP_INTERNAL_SERVICE_TOKEN` лучше сгенерировать случайные значения:

```bash
openssl rand -hex 32
```

`APP_INTERNAL_SERVICE_TOKEN` нужен для внутреннего REST-вызова:
order-service отправляет документы в document-service, а document-service проверяет header
`X-Internal-Service-Token`.

## Запуск всего стека

Из корня проекта:

```bash
docker compose --env-file .env up --build
```

Фоновый режим:

```bash
docker compose --env-file .env up --build -d
```

Проверить compose-конфиг без запуска:

```bash
docker compose --env-file .env config --quiet
```

Проверить контейнеры:

```bash
docker compose --env-file .env ps
```

Посмотреть логи конкретного сервиса:

```bash
docker compose --env-file .env logs -f order-service
docker compose --env-file .env logs -f document-service
```

Остановить стек:

```bash
docker compose --env-file .env down
```

## URL

```text
frontend         http://127.0.0.1:8000
auth-service     http://127.0.0.1:8081
order-service    http://127.0.0.1:8083
catalog-service  http://127.0.0.1:8084
document-service http://127.0.0.1:8085
```

PostgreSQL наружу сейчас не проброшен. Сервисы ходят к нему внутри docker-сети по host `postgres`.
Если нужно подключение из DataGrip/IDE, отдельным шагом добавляем port mapping
`127.0.0.1:5432:5432`.

## Тесты

Запуск всех backend-тестов по сервисам:

```bash
cd auth_service/legal_website && ./mvnw test
cd order-service/order-service && ./mvnw test
cd document-service && ./mvnw test
cd catalog-service && ./mvnw test
cd notification-service/Notification && ./mvnw test
```

Интеграционные тесты с БД используют Testcontainers, поэтому Docker Desktop должен быть запущен.

## Document Metadata Flow

Source of truth для файлов - document-service. order-service хранит только локальную read-model
метаданных, чтобы карточку заказа можно было собрать из своей БД.

Flow:

1. Frontend вызывает create order в order-service.
2. order-service создаёт заказ и возвращает `orderId`.
3. Frontend отправляет файлы в `POST /api/client/orders/{orderId}/documents`.
4. order-service проверяет ownership заказа по текущему `clientId`.
5. order-service пересылает multipart-запрос в internal API document-service.
6. document-service сохраняет файл, запись `order_documents` и outbox event.
7. outbox publisher document-service публикует Kafka event `document.stored`.
8. order-service listener принимает событие и idempotent-сохранением пишет `order_document_metadata`.
9. `GET /api/client/orders/{orderId}` возвращает заказ и документы из БД order-service.

После upload frontend может сразу показать документы из ответа upload endpoint. Если Kafka-событие ещё
не дошло до order-service, `GET order details` может на короткое время вернуть заказ без документов.
Это нормальная eventual consistency для текущего этапа.

## Если БД не создались

Init-скрипты PostgreSQL выполняются только при первом создании volume. Если volume был создан раньше,
новые БД автоматически не появятся.

Проверить volumes:

```bash
docker volume ls
```

Удалять volume можно только если локальные данные не нужны:

```bash
docker compose --env-file .env down -v
docker compose --env-file .env up --build
```

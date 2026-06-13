# PROJECT_CONTEXT.md

## Назначение
Этот файл - короткий source of truth по текущему состоянию репозитория.
Если контекст потерялся, сначала смотреть сюда, потом в `AGENTS.md`.

## Что это за проект
Проект делается как учебный, но production-like pet-project для юридического сервиса.
Цель:
- показать архитектурное мышление;
- собрать рабочий auth + orders + catalog + documents + notification foundation;
- научиться строить backend-контракты, event flow и интеграцию с БД;
- отделить публичный frontend от backend-сервисов и инфраструктуры.

## Что считать актуальным
- Основной публичный auth backend: `auth_service/legal_website`
- Клиентские заявки и order flow: `order-service/order-service`
- Каталог услуг: `catalog-service`
- Документы по заявкам: `document-service`
- Уведомления: `notification-service/Notification`
- Публичный frontend: `front`
- `bd/demo` - legacy-контур, не источник правды

## Текущее устройство системы

### 1. Auth service
`auth_service/legal_website` - сервис аутентификации и профиля.

Что уже есть:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/google/login`
- `POST /api/auth/google/complete`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `PATCH /api/auth/me`
- `DELETE /api/auth/account`
- `POST /api/auth/email-verification/request`
- `POST /api/auth/email-verification/confirm`

Что уже реализовано по auth-модели:
- JWT access token
- refresh token в БД в хэшированном виде
- revoke refresh token
- Bearer auth через `JwtAuthenticationFilter`
- `GET /api/auth/me` как источник profile/completion-флагов
- Google first-login flow через `flowToken`

Что уже заложено под email verification:
- `users.email_verified`
- `users.email_verified_at`
- таблица `verification_codes`
- таблица `outbox_events`
- `JwtService` умеет генерировать verify link через `app.frontend-base-url`
- `VerifyEmailService` пишет verification record + outbox event
- `SendEventService` публикует outbox-события в Kafka

Что ещё не доведено до конца:
- end-to-end стабилизация relay/publisher логики
- проверка confirm flow через frontend страницу `verify-email`

### 2. Order service
`order-service/order-service` - сервис клиентских заявок.

Что уже есть:
- `OrderEntity`, `OrderRepo`
- outbox для запроса `serviceName` из catalog-service
- incoming events для ответа catalog-service
- `CreateOrderService`
- `SendEventForGetServiceName`
- `ListenCatalogService`
- unit-тесты сервисного слоя без Spring context

Что уже зафиксировано в модели:
- `serviceId/serviceCode` - source of truth со стороны заказа
- `serviceName` - обогащаемое поле из catalog-service
- стартовый статус заказа выставляется в коде как `ON_REVIEW`
- запрос имени услуги идёт через outbox -> Kafka -> reply-event

Что проверено локально:
- `./mvnw -q test` проходит

Что ещё не доведено до конца:
- публичные REST endpoint-ы `/api/client/...`
- end-to-end проверка `order-service <-> catalog-service`
- retry/recovery flow для ручного повторного запроса `serviceName`

### 3. Catalog service
`catalog-service` - каталог услуг и источник правды по названиям услуг.

Что уже есть:
- `GET /api/services`
- таблица `services`
- seed данных каталога
- `ServiceRepository.findByCode(...)`
- groundwork под inbox/outbox и Kafka topics:
  - `order.getServiceName.request`
  - `order.getServiceName.response`

Что важно по текущему состоянию:
- сервис должен быть source of truth для `serviceName`
- сейчас Kafka/inbox/outbox слой ещё не стабилизирован
- последняя проверка показала, что `catalog-service` не собирается из-за проблем с новым кодом и annotation processing/Lombok
- текущий request/response flow между `order-service` и `catalog-service` ещё не считается рабочим

### 4. Document service
`document-service` - хранение и обработка документов заявки.

Что уже есть:
- `DocumentEntity`, `DocumentRepository`
- миграции для документов
- groundwork под outbox/inbox-подобный event слой
- Dockerfile и базовая конфигурация

Что пока не зафиксировано как готовое:
- публичный REST контракт для документов
- проверенный end-to-end upload/download flow

### 5. Notification service
`notification-service/Notification` - отдельный сервис доставки уведомлений.

Текущая модель:
- Kafka listener принимает событие `auth.email-verification.requested`
- listener сохраняет:
  - `processed_events(status=ACCEPTED)`
  - `notification_deliveries(status=NEW)`
- scheduler - единственная точка фактической отправки email

Состояния delivery:
- `NEW`
- `PROCESSING`
- `SENT`
- `FAILED`
- `DEAD`

Что уже есть:
- `notification_deliveries`
- `processed_events`
- SMTP integration через `JavaMailSender`
- сериализация payload в delivery
- retry через `nextRetryAt`
- Dockerfile и `application-prod.yaml`

### 6. Frontend
`front` - статический frontend без Vite/Next.

Что уже есть:
- отдельный auth UI
- `complete-profile.html`
- runtime config через `front/runtime-config.js`
- локальный сервер `front/server.py`
- каталог endpoint-ов в `front/app/api/endpoints.js`
- `orders-api.js` и типы под клиентские заявки
- landing/cabinet/order pages

Что фронт уже ожидает от backend:
- `GET /api/services`
- `POST /api/client/applications`
- `GET /api/client/orders`
- `GET /api/client/orders/{orderId}`
- `PATCH /api/client/orders/{orderId}`
- `DELETE /api/client/orders/{orderId}`
- `POST /api/client/orders/{orderId}/rework`
- `GET/POST /api/auth/email-verification/*`

## Инфраструктура

### Docker Compose
В корне есть `docker-compose.yml`.
Сейчас там описаны:
- `postgres`
- `kafka`
- `frontend`
- `auth-service`
- `order-service`
- `catalog-service`
- `document-service`
- `notification-service`

Секреты в репозиторий не кладутся.
Prod-переменные задаются через `.env` на сервере и шаблон `prod.env.example`.

### Базы данных
- `legal_auth` - auth-service
- `legal_orders` - order-service
- `legal_catalog` - catalog-service
- `legal_documents` - document-service
- `legal_notification` - notification-service

### Kafka
Сейчас в проекте уже используются или проектируются такие прикладные topics:
- `auth.email-verification.requested`
- `order.getServiceName.request`
- `order.getServiceName.response`

## Архитектурные решения, которые уже зафиксированы

### 1. Notification - отдельный сервис
Notification делается не как узкий verification mail sender, а как отдельный сервис уведомлений, который позже сможет обрабатывать и другие события системы.

### 2. Verification state живёт в auth-service
`verification_codes` и признак `email_verified` - это бизнес-состояние пользователя, значит source of truth должен быть в `auth_service`.

### 3. Надёжная публикация идёт через outbox
Базовая схема уже принята для новых сервисов:
1. сервис сохраняет доменное изменение и outbox event в одной транзакции
2. relay/scheduler читает `outbox_events`
3. relay публикует событие в Kafka
4. consumer пишет inbox/processed state и продолжает свой flow

### 4. Frontend не должен знать про внутренние сервисы
Публичный frontend должен ходить только в публичный API-домен.
Внутренние сервисы вроде notification-service наружу публиковать не нужно.

### 5. `serviceName` не должен быть блокером создания заказа
Заказ должен жить даже если catalog-service временно недоступен.
Поэтому:
- заказ сохраняется сразу;
- `serviceCode` хранится как обязательный идентификатор;
- `serviceName` заполняется асинхронно;
- retry и recovery должны решаться отдельно от транзакции создания заказа.

## Что важно по безопасности
- реальные секреты не коммитятся
- `.env`, `front/.env`, `cookies.txt`, локальные кэши и служебные артефакты игнорируются git
- чувствительные операции (`email`, `phone`, `password`) не должны жить в обычном profile update flow
- `PATCH /api/auth/me` - только для non-sensitive profile update

## Текущее фактическое состояние

### Сборка и тесты
Последние локальные проверки:
- `auth_service/legal_website`: compile считался рабочим ранее
- `notification-service/Notification`: compile считался рабочим ранее
- `order-service/order-service`: `./mvnw -q test` проходит
- `catalog-service`: последняя проверка показала compile errors в новом Kafka/inbox/outbox коде

Для `document-service` в этом контексте статус сборки не считается подтверждённым, пока не будет отдельной локальной проверки.

### Server deploy
Для первого deploy уже подготовлены:
- Dockerfile для frontend
- Dockerfile для auth-service
- Dockerfile для order-service
- Dockerfile для catalog-service
- Dockerfile для document-service
- Dockerfile для notification-service
- `docker-compose.yml`
- `prod.env.example`
- profile/config groundwork для сервисов

### Что ещё может требовать внимания на сервере
- корректные `APP_FRONTEND_BASE_URL` и `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- корректный `front/.env` с runtime config для frontend
- рабочий SMTP provider
- reverse proxy (`Caddy` / `Nginx`) для frontend и публичного API
- публичные DNS-записи и TLS
- проверка, что все сервисы действительно подключены к Kafka и нужным БД в compose

## Следующий разумный порядок работ
1. Починить `catalog-service`, чтобы он собирался и корректно слушал request topic
2. Довести `order-service <-> catalog-service` reply flow до end-to-end рабочего состояния
3. Реализовать публичные endpoint-ы `client orders` поверх `order-service`
4. Подключить документы заявки к client order flow
5. После стабилизации client flow переходить к staff board и admin users API

## Что помнить в следующих сессиях
- `bd/demo` не использовать как источник правды
- `front/app/api/endpoints.js` и `front/app/types.js` уже описывают ожидаемые backend-контракты
- `notification-service` - transport layer, не auth state
- `catalog-service` - source of truth для справочника услуг
- `serviceCode`/`serviceId` важнее, чем `serviceName`, потому что имя может доехать позже
- `front/.env` и server `.env` - разные файлы и разные зоны ответственности

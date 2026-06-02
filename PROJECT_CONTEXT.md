# PROJECT_CONTEXT.md

## Назначение
Этот файл - короткий source of truth по текущему состоянию репозитория.
Если контекст потерялся, сначала смотреть сюда, потом в `AGENTS.md`.

## Что это за проект
Проект делается как учебный, но production-like pet-project для юридического сервиса.
Цель:
- показать архитектурное мышление;
- собрать рабочий auth + notification foundation;
- научиться строить backend-контракты, event flow и интеграцию с БД;
- отделить пользовательский frontend от backend-сервисов и инфраструктуры.

## Что считать актуальным
- Основной backend: `auth_service/legal_website`
- Сервис уведомлений: `notification-service/Notification`
- Статический frontend: `front`
- `bd/demo` - legacy-контур, не источник правды

## Текущее устройство системы

### 1. Auth service
`auth_service/legal_website` - основной публичный backend на текущем этапе.

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
- `VerityEmailService` пишет verification record + outbox event
- `SendEventService` публикует outbox-события в Kafka

Что ещё не доведено до конца:
- финальный HTTP-контракт email verification endpoint-ов
- финальная стабилизация relay/publisher логики
- полноценный confirm flow через frontend страницу `verify-email`

### 2. Notification service
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

Что ещё важно помнить:
- `notification-service` не должен хранить auth-бизнес-логику
- verification state живёт в `auth-service`
- notification - это transport/delivery layer

### 3. Frontend
`front` - статический frontend без Vite/Next.

Что уже есть:
- отдельный auth UI
- `complete-profile.html`
- каталог endpoint-ов в `front/app/api/endpoints.js`
- runtime config через `front/runtime-config.js`
- локальный сервер `front/server.py`
- frontend env через `front/.env`
- Dockerfile для frontend container deploy

Как фронт получает backend URL:
- `API_BASE_URL` из `front/.env`
- `AUTH_API_BASE_URL` из `front/.env`
- `GOOGLE_CLIENT_ID` из `front/.env`

Локально `front/server.py` генерирует `runtime-config.js` на лету.

## Инфраструктура

### Docker Compose
В корне есть `docker-compose.yml` для первого server deploy:
- `postgres`
- `kafka`
- `frontend`
- `auth-service`
- `notification-service`

Секреты в репозиторий не кладутся.
Prod-переменные задаются через `.env` на сервере и шаблон `prod.env.example`.

### Базы данных
- `legal_auth` - auth-service
- `legal_notification` - notification-service

### Kafka
Сейчас используется один прикладной topic:
- `auth.email-verification.requested`

## Архитектурные решения, которые уже зафиксированы

### 1. Notification - отдельный сервис
Notification делается не как узкий verification mail sender, а как отдельный сервис уведомлений, который позже сможет обрабатывать и другие события системы.

### 2. Verification state живёт в auth-service
`verification_codes` и признак `email_verified` - это бизнес-состояние пользователя, значит source of truth должен быть в `auth_service`.

### 3. Надёжная публикация идёт через outbox
Схема такая:
1. auth-service сохраняет доменное изменение и outbox event в одной транзакции
2. relay читает `outbox_events`
3. relay публикует событие в Kafka
4. notification-service читает его и создаёт delivery

### 4. Frontend не должен знать про внутренние сервисы
Публичный frontend должен ходить только в публичный API-домен.
Внутренние сервисы вроде notification-service наружу публиковать не нужно.

## Что важно по безопасности
- реальные секреты не коммитятся
- `.env`, `front/.env`, `cookies.txt`, локальные кэши и служебные артефакты игнорируются git
- чувствительные операции (`email`, `phone`, `password`) не должны жить в обычном profile update flow
- `PATCH /api/auth/me` - только для non-sensitive profile update

## Текущее фактическое состояние

### Сборка
Последняя локальная проверка:
- `auth_service/legal_website`: `./mvnw -q -DskipTests compile` проходит
- `notification-service/Notification`: `./mvnw -q -DskipTests compile` проходит

### Server deploy
Для первого deploy уже подготовлены:
- Dockerfile для frontend
- Dockerfile для auth-service
- Dockerfile для notification-service
- `docker-compose.yml`
- `prod.env.example`
- `application-prod.yaml` для обоих сервисов

### Что ещё может требовать внимания на сервере
- корректные `APP_FRONTEND_BASE_URL` и `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- корректный `front/.env` с runtime config для frontend
- рабочий SMTP provider
- reverse proxy (`Caddy` / `Nginx`) для frontend и auth API
- публичные DNS-записи и TLS

## Следующий разумный порядок работ
1. Довести email verification HTTP contract до финального вида
2. Проверить end-to-end flow:
   - register/login
   - request verification
   - outbox -> Kafka
   - notification delivery
   - confirm email
3. Поднять тестовый контур на `test` / `testapi`
4. После стабилизации verification переходить к:
   - change password
   - change phone
   - change email
5. Параллельно развивать client orders, staff board и admin users API

## Что помнить в следующих сессиях
- `bd/demo` не использовать как источник правды
- frontend-контракты уже частично описывают будущие backend API
- notification-service - transport layer, не auth state
- `front/.env` и server `.env` - разные файлы и разные зоны ответственности
- `Caddy` только reverse proxy; автоподъём frontend должен обеспечивать Docker restart policy

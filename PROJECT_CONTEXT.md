# PROJECT_CONTEXT.md

## Назначение
Этот файл - краткий source of truth по текущему состоянию проекта.
Если рабочий контекст теряется, сначала смотреть сюда, потом в `AGENTS.md`.

## Текущий курс проекта
Проект делается как учебный, но production-like pet-project для юридического сервиса.
Цель не в том, чтобы написать быстрее, а в том, чтобы показать:
- зрелое проектирование backend-а;
- понимание границ сервисов;
- работу с БД, auth, event flow и надёжностью;
- осознанные trade-off между монолитом и микросервисами.

Пользователь изучает архитектуру вместе с Codex, поэтому важны не только рабочие фичи, но и понятные причины инженерных решений.

## Что считать актуальным
- Основной backend сейчас: `auth_service/legal_website`.
- Отдельно развивается `notification-service/Notification`.
- Frontend в `front` уже живёт в режиме контрактов с отдельным auth-service.
- `bd/demo` считается legacy-контуром и не должен использоваться как источник правды для текущей архитектуры и roadmap.

## Активные части системы

### 1. Auth service
`auth_service/legal_website` - главный текущий сервис.

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
- JWT access token + refresh token.
- Refresh token хранится в БД в хэшированном виде.
- Поддерживается revoke refresh token.
- `JwtAuthenticationFilter` собирает `Authentication` из Bearer token.
- `GET /api/auth/me` возвращает профиль и auth/completion-флаги.
- Есть Google first-login flow с `flowToken` и шагом дозаполнения профиля.

Что добавлено недавно в модель данных:
- `users.email_verified`
- `users.email_verified_at`
- таблица `verification_codes`
- таблица `outbox_events`

Также добавлены:
- JPA-пакет `verification_codes`
- JPA-пакет `outbox_events`
- зависимость `spring-kafka` в `auth_service`

### 2. Notification service
`notification-service/Notification` - отдельный сервис под уведомления.

Его роль:
- не хранить auth-бизнес-логику;
- не решать, кому и зачем нужен код;
- принимать события и заниматься доставкой уведомлений.

Что уже подготовлено:
- Spring Boot сервис с зависимостями:
  - `web`
  - `validation`
  - `actuator`
  - `data-jpa`
  - `mail`
  - `spring-kafka`
  - `flyway`
  - `postgresql`
- таблица `notification_deliveries`
- таблица `processed_events`
- локальная БД `legal_notification`
- конфиг topic через `KafkaTopicsConfig`

Что пока не сделано:
- consumer бизнес-событий;
- отправка email;
- retry/DLT логика;
- idempotent обработка через service-слой;
- нормальный producer/consumer DTO contract.

### 3. Frontend
Frontend уже ориентирован на отдельный auth backend и контрактный режим.

Что уже видно по фронту:
- auth API вынесен в `front/app/api/auth-api.js`
- есть endpoint-каталог в `front/app/api/endpoints.js`
- есть отдельная страница `complete-profile.html`
- есть Google auth UI и flow completion
- есть контракты для:
  - client orders
  - staff board
  - admin users

Важные planning-файлы в корне:
- `API_ENDPOINT_ROADMAP.md`
- `ADMIN_PANEL_USERS_V1_PLAN.md`

## Архитектурные решения, которые уже зафиксированы

### 1. Notification - отдельный микросервис
Notification делаем не как узкий "email verification service", а как отдельный notification-контур.
Это нужно, чтобы позже он обслуживал не только verification email, но и другие события системы.

### 2. Verification state живёт в auth-service
`verification_codes` - это бизнес-состояние пользователя, поэтому оно должно жить в `auth_service`, а не в notification-service.

### 3. Надёжная публикация событий идёт через outbox
`outbox_events` нужна для схемы:
1. auth-service сохраняет бизнес-изменение и outbox event в одной транзакции;
2. отдельный relay публикует событие в Kafka;
3. если публикация временно упала, событие не теряется, потому что уже лежит в БД.

### 4. Kafka topic - это транспорт, а не business state
Например:
- `verification_codes` хранит сам факт и параметры кода;
- Kafka topic передаёт событие вроде `EMAIL_VERIFICATION_REQUESTED`.

## Что важно по безопасности

### Обычное обновление профиля
Базовая идея для `PATCH /api/auth/me` такая:
- без дополнительного подтверждения можно менять только:
  - `fullName`
  - `companyName`

### Чувствительные изменения
Следующие поля нельзя считать обычным profile update:
- `phone`
- `email`
- `password`

Для них нужен отдельный flow подтверждения.

Принятый вектор:
- сначала сделать доверенный email-канал;
- потом через него подтверждать чувствительные операции;
- TOTP возможен позже, но не должен быть единственным вариантом;
- `passkey` / WebAuthn нужен как future roadmap, а не как текущая ближайшая задача.

## Что уже сделано по email verification foundation
В БД и архитектуре уже подготовлены основы:
- `users.email_verified`
- `users.email_verified_at`
- `verification_codes`
- `outbox_events`
- `notification_deliveries`
- `processed_events`

Что уже реализовано в auth-service:
1. `VerityEmailService` создаёт `verification_codes`.
2. `VerityEmailService` пишет `outbox_events` в той же транзакции.
3. Есть relay `SendEventService`, который читает outbox и публикует в Kafka.
4. В relay уже используется state machine вокруг статусов `NEW -> PROCESSING -> PUBLISHED` и `FAILED/DEAD` для retry.
5. В relay уже используется async publish через `kafkaTemplate.send(...).whenComplete(...)`.

Что ещё не завершено в этом flow:
1. Дочистить relay до более аккуратного вида без дублирования логики между `NEW` и `FAILED`.
2. Уточнить финальные terminal state правила для доменных случаев вроде `user not found` / `email already verified`.
3. Добавить consumer и реальную доставку письма в notification-service.

## Текущее фактическое состояние кода

### Сборка
На момент последней проверки:
- `auth_service/legal_website`: `./mvnw -q -DskipTests compile` проходит.
- `notification-service/Notification`: `./mvnw -q -DskipTests compile` проходит.

### Auth / profile
Есть ранняя реализация `PATCH /api/auth/me`, но её ещё нельзя считать финальной security-моделью.
Сейчас этот endpoint уже существует, но его контракт и границы ответственности ещё нужно дочистить.

### Kafka / notification
В auth-service уже есть рабочая заготовка relay-публикации:
- topic `auth.email-verification.requested`
- `SendEventService` как poller по `outbox_events`
- async callback через `whenComplete(...)`
- retry-статусы `NEW`, `PROCESSING`, `FAILED`, `PUBLISHED`, `DEAD`

Notification-service пока всё ещё на стадии инфраструктурной заготовки:
- topic config есть;
- полноценного consumer flow и email delivery ещё нет.

## Что ещё не завершено

### В auth-service
- финальный contract для `PATCH /api/auth/me`;
- разделение обычного profile update и чувствительных изменений;
- маппинг и бизнес-использование `email_verified` в user flow;
- полноценные endpoints для email verification request/confirm.
- дочистка `SendEventService`:
  - убрать дублирование между обработкой `NEW` и `FAILED`
  - сделать более аккуратные terminal state правила
  - добавить более надёжную обработку sync/async ошибок Kafka

### В notification-service
- убрать из `KafkaTopicsConfig` роль отдельной точки входа приложения и оставить нормальный config-класс;
- описать DTO/event envelope;
- сделать listener;
- сделать обработку `processed_events`;
- сделать создание `notification_deliveries`;
- подключить mail provider;
- сделать retry strategy и позже DLT.

### Во frontend/backend контрактах
Пока backend не закрывает:
- client orders API;
- staff board API;
- admin users API.

Эти контракты уже описаны во frontend-е и planning-файлах, но не являются текущим ближайшим backend-фокусом.

## Следующий правильный порядок работ
1. Дочистить relay `SendEventService` как надёжный outbox publisher.
2. Закрыть `email verification` flow до конца:
   - request
   - confirm
   - обновление `users.email_verified`
3. Сделать consumer в notification-service.
4. Подключить реальную email delivery и idempotent обработку через `processed_events`.
5. После доверенного email-канала переходить к:
   - change password
   - change phone
   - change email
6. Параллельно дочищать `PATCH /api/auth/me` как non-sensitive profile update.
7. Позже добавить `passkey` / WebAuthn.

## Что помнить при следующих сессиях
- `bd/demo` не использовать как источник правды.
- Если спор идёт про архитектуру, сначала разбирать инварианты и source of truth.
- Если вопрос про Kafka, не путать business state, outbox и сам broker.
- Notification - это отдельный сервис доставки, а не место для auth-логики.
- Для чувствительных изменений сначала нужен доверенный канал подтверждения.

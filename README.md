# Philosophy Business Legal Platform

Production-like pet-project для сайта юридических услуг.

Проект собирается как учебная, но серьёзная система, в которой отдельно развиваются:
- auth-service
- notification-service
- статический frontend
- будущие client/staff/admin API

Цель репозитория:
- показать архитектурное мышление
- собрать надёжный auth и verification flow
- научиться проектировать backend-контракты, БД и event-driven интеграции
- постепенно довести проект до публичного demo/deploy состояния

## Архитектура

### Auth service
Путь: `auth_service/legal_website`

Отвечает за:
- регистрацию и логин
- refresh/logout
- Google login flow
- профиль текущего пользователя
- verification state пользователя
- outbox events для интеграции с Kafka

Текущие публичные/пользовательские endpoint-ы:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/google/login`
- `POST /api/auth/google/complete`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `PATCH /api/auth/me`
- `DELETE /api/auth/account`

### Notification service
Путь: `notification-service/Notification`

Отвечает за:
- приём Kafka-событий
- создание delivery records
- email-отправку
- retry и delivery state

Текущая логика:
- listener сохраняет `processed_events(ACCEPTED)` и `notification_deliveries(NEW)`
- scheduler отправляет `NEW` и retryable `FAILED`

### Frontend
Путь: `front`

Это статический frontend, который:
- раздаётся обычным HTTP server-ом
- читает runtime config через `front/.env`
- умеет работать с отдельным auth API
- уже содержит контракты для будущих `client`, `staff`, `admin` endpoint-ов
- на сервере поднимается отдельным container service через `docker-compose`

## Структура репозитория

```text
auth_service/legal_website        # основной auth backend
notification-service/Notification # notification microservice
front                             # статический frontend
docker-compose.yml                # первый server deploy
docker/                           # infra helpers
bd_SQL/                           # локальная Postgres-схема для dev
bd/demo                           # legacy/experimental контур
```

## Что уже сделано
- JWT auth + refresh tokens
- Google login flow
- profile endpoint-ы
- foundation под email verification:
  - `users.email_verified`
  - `verification_codes`
  - `outbox_events`
- notification foundation:
  - `notification_deliveries`
  - `processed_events`
- SMTP integration
- scheduler-based delivery
- `docker-compose` для `postgres + kafka + frontend + auth-service + notification-service`
- frontend runtime config через `front/.env`

## Что в процессе
- финальный HTTP-контракт для email verification request/confirm
- end-to-end verify email flow через frontend страницу и backend confirm
- стабилизация notification delivery на сервере
- client orders API
- staff board API
- admin users API

## Локальный запуск

### Быстрый сценарий
1. Поднять локальную Postgres:
```bash
docker compose -f bd_SQL/compose.yml up -d
```

2. Запустить auth-service:
```bash
cd auth_service/legal_website
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

3. Создать `front/.env` на основе `front/.env.example`

4. Запустить frontend:
```bash
cd front
python3 server.py
```

### Compose-сценарий
1. Подготовить env вне git
2. Создать `front/.env` на основе `front/.env.example` для ручного запуска frontend, если нужен standalone режим
3. Запустить:
```bash
docker compose --env-file /path/to/.env up -d --build
```

В compose-режиме frontend поднимается отдельным сервисом и получает runtime config
из `front/.env`, который читает `front/server.py` внутри container.

См. подробнее:
- [LOCAL_DEV.md](./LOCAL_DEV.md)
- [prod.env.example](./prod.env.example)
- [docker/Caddyfile.example](./docker/Caddyfile.example)

## Deploy idea
Первый deploy рассчитан на:
- один инстанс notification-service
- внешний SMTP provider
- reverse proxy (`Caddy` или `Nginx`)
- публичный frontend домен и отдельный API домен
- frontend как отдельный docker service с `restart: unless-stopped`

Типичная схема:
- `https://philosophyabiz.ru` -> frontend
- `https://api.philosophyabiz.ru` -> auth API
- `notification-service` наружу не публикуется

Для test-контура:
- `https://test.philosophyabiz.ru` -> `127.0.0.1:8000` через `Caddy`
- `https://testapi.philosophyabiz.ru` -> `127.0.0.1:8081` через `Caddy`

Минимальный `front/.env` для test-контура:
```env
API_BASE_URL=https://testapi.philosophyabiz.ru
AUTH_API_BASE_URL=https://testapi.philosophyabiz.ru/api
GOOGLE_CLIENT_ID=...
```

## Документация в репозитории
- [PROJECT_CONTEXT.md](./PROJECT_CONTEXT.md) - краткий source of truth
- [LOCAL_DEV.md](./LOCAL_DEV.md) - локальный запуск
- [API_ENDPOINT_ROADMAP.md](./API_ENDPOINT_ROADMAP.md) - карта текущих и planned API
- [ADMIN_PANEL_USERS_V1_PLAN.md](./ADMIN_PANEL_USERS_V1_PLAN.md) - план admin users panel

## Важные замечания
- реальные секреты не коммитятся
- `.env`, `front/.env`, cookies и локальные кэши игнорируются git
- `bd/demo` не является источником правды для текущей архитектуры
- проект intentionally evolving: часть решений здесь учебные, но осознанно приближены к production

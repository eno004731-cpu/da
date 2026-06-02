# Локальный запуск

## Что есть сейчас
В проекте два режима локальной работы:

1. Быстрый локальный сценарий:
- auth-service
- frontend
- локальная Postgres

2. Более полный infrastructure-сценарий:
- `docker compose`
- Postgres
- Kafka
- frontend
- auth-service
- notification-service

## Быстрый локальный сценарий

### 1. Поднять Postgres
```bash
docker compose -f bd_SQL/compose.yml up -d
```

Скрипт инициализации создаёт БД:
- `legal_crm`
- `legal_auth`

Если контейнер и volume уже были созданы раньше и `legal_auth` не появилась:
```bash
docker exec -it postgres psql -U legal_user -d postgres -c "CREATE DATABASE legal_auth;"
```

### 2. Запустить auth-service
```bash
cd /Users/nikitatukan/Documents/Playground/auth_service/legal_website
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Локальный профиль уже содержит:
- БД `legal_auth`
- порт `8081`
- локальный JWT secret

### 3. Настроить frontend env
Создай `front/.env` на основе `front/.env.example`.

Минимальный локальный вариант:
```env
API_BASE_URL=
AUTH_API_BASE_URL=http://127.0.0.1:8081/api
GOOGLE_CLIENT_ID=
```

Если локально тестируешь auth-service через домен или reverse proxy, указывай не `127.0.0.1`, а нужный публичный URL API.

### 4. Запустить frontend
```bash
cd /Users/nikitatukan/Documents/Playground/front
python3 server.py
```

Открывай:
```text
http://127.0.0.1:8000/da.html
```

## Как фронт получает backend URL
`front/server.py`:
- читает `front/.env`
- генерирует `runtime-config.js`
- подставляет:
  - `API_BASE_URL`
  - `AUTH_API_BASE_URL`
  - `GOOGLE_CLIENT_ID`

На `localhost` дополнительно работает `front/local-dev-config.js`, который:
- включает auth-only fallback
- не даёт фронту случайно ходить в пользовательский `localhost`

## Полный локальный/серверный compose-сценарий

### 1. Подготовить env
Скопируй `prod.env.example` в локальный `.env` вне репозитория или рядом с compose.

Минимально важные переменные:
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `APP_FRONTEND_BASE_URL`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_MAIL_FROM`

### 2. Запустить compose
```bash
cd /Users/nikitatukan/Documents/Playground
docker compose --env-file /Users/nikitatukan/Documents/.env up -d --build
```

Frontend в этом режиме поднимается автоматически как отдельный container service
и больше не требует `python3 server.py` на сервере вручную.
Для frontend runtime config compose использует `front/.env`.

### 3. Проверить контейнеры
```bash
docker compose --env-file /Users/nikitatukan/Documents/.env ps
```

### 4. Посмотреть логи
```bash
docker compose --env-file /Users/nikitatukan/Documents/.env logs --tail=100 frontend
docker compose --env-file /Users/nikitatukan/Documents/.env logs --tail=100 auth-service
docker compose --env-file /Users/nikitatukan/Documents/.env logs --tail=100 notification-service
```

## Что поднимается через compose
- `postgres`
- `kafka`
- `frontend`
- `auth-service`
- `notification-service`

Внутренние URL внутри compose:
- auth DB: `jdbc:postgresql://postgres:5432/legal_auth`
- notification DB: `jdbc:postgresql://postgres:5432/legal_notification`
- Kafka: `kafka:9092`

Host-level reverse proxy URLs:
- frontend upstream: `127.0.0.1:8000`
- auth API upstream: `127.0.0.1:8081`

## Важные замечания
- `front/.env` и server `.env` - это разные файлы
- `front/.env` нужен фронту и в standalone, и в compose-контуре
- server `.env` нужен для backend/infrastructure переменных `docker compose`
- notification-service не должен публиковаться наружу как отдельный публичный API
- `Caddy` не поднимает frontend сам, он только проксирует живой upstream на `127.0.0.1:8000`

## Быстрый smoke test
1. Открыть frontend
2. Проверить login/register
3. Проверить `GET /api/auth/me`
4. Проверить, что фронт действительно ходит в тот `AUTH_API_BASE_URL`, который задан в `front/.env`
5. В compose-режиме проверить `curl -I http://127.0.0.1:8000`

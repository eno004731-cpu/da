# Локальный запуск

## Что поднимаем
- `auth_service/legal_website` на `http://127.0.0.1:8081`
- frontend на `http://127.0.0.1:8000`
- Postgres на `localhost:5432`

Сейчас локальный сценарий специально упрощён:
- авторизация и профиль клиента работают через отдельный auth-service;
- API заявок локально пока не поднимается;
- UI фронта показывает это явно и не пытается притворяться, что заказы уже доступны.

## 1. Поднять Postgres
```bash
docker compose -f bd_SQL/compose.yml up -d
```

Скрипт инициализации создаёт две БД:
- `legal_crm`
- `legal_auth`

Если контейнер и volume уже были созданы раньше и `legal_auth` не появилась, можно один раз создать БД вручную:
```bash
docker exec -it postgres psql -U legal_user -d postgres -c "CREATE DATABASE legal_auth;"
```

## 2. Запустить auth service локально
Это основной локальный backend на текущем этапе:
```bash
cd /Users/nikitatukan/Documents/Playground/auth_service/legal_website
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`application-local.yaml` уже содержит локальные настройки:
- БД `legal_auth`
- порт `8081`
- dev JWT secret

Если хочешь запускать без профиля, можно создать `.env` на основе `.env.example`.

## 3. Запустить frontend
```bash
cd /Users/nikitatukan/Documents/Playground/front
python3 server.py
```

Открывай:
```text
http://127.0.0.1:8000/da.html
```

## Как теперь маршрутизируются запросы локально
Файл `front/local-dev-config.js` автоматически подставляет локальные URL:
- auth API -> `http://127.0.0.1:8081/api`
- основной API заявок помечен как отключённый до следующего этапа разработки

Дополнительно включаются локальные флаги:
- `window.__LEGAL_LOCAL_AUTH_ONLY__ = true`
- `window.__LEGAL_DISABLE_ORDERS_API__ = true`
- Google login скрыт, пока в auth-service нет `POST /api/auth/google`
- удаление аккаунта скрыто, пока в auth-service нет endpoint для этого

Если позже захочешь снова включить локальную разработку API заявок, можно заранее переопределить флаги и URL до загрузки frontend-кода:
```js
window.__LEGAL_LOCAL_AUTH_ONLY__ = false;
window.__LEGAL_DISABLE_ORDERS_API__ = false;
window.__LEGAL_API_BASE_URL__ = "http://127.0.0.1:8080/api";
window.__LEGAL_AUTH_API_BASE_URL__ = "http://127.0.0.1:8081/api";
```

Это работает только на `localhost` / `127.0.0.1` и не влияет на прод.

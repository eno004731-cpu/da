# PROJECT_CONTEXT.md

## Текущий этап
Проект находится в переходе от монолита к микросервисной архитектуре.
Главный текущий фокус - `auth_service/legal_website`.

Сейчас задача не в полном распиле системы, а в том, чтобы:
- довести `auth_service` до рабочего состояния;
- зафиксировать понятный JWT auth flow;
- сохранить учебный режим работы, чтобы решения были объяснимыми, а не "магией".
- не спешить с асинхронностью, пока не завершён базовый protected auth flow.
- параллельно подготовить Google auth как отдельный frontend-based flow.
- не тащить Kafka и лишнюю асинхронность, пока не стабилизированы контракты auth и profile flow.

## Архитектурный курс проекта
Проект делается не как "быстрый учебный CRUD", а как серьёзная учебно-практическая система, которая должна показывать:
- понимание архитектурных границ;
- умение проектировать сервисы с расчётом на рост нагрузки;
- осознанный переход от монолита к микросервисам;
- понимание trade-off, а не просто использование модных технологий.

Цель не "написать быстрее и проще", а "сделать качественнее и архитектурно взрослее", даже если иногда допускаются временные упрощения.

Этот проект одновременно является:
- pet-project для демонстрации engineering mindset работодателю;
- площадкой, на которой пользователь вместе с Codex разбирает архитектуру, state machine, контракты, event flow и причины инженерных решений.

Поэтому важен не только рабочий код, но и то, почему выбран именно такой путь.

## Что уже сделано
### Общая структура
- В корне есть старый монолит `bd/demo`.
- Отдельно выделен `auth_service/legal_website`.
- Frontend уже ожидает отдельный auth API.
- Для Google auth сейчас менялся только frontend.

### Auth service
- Реализованы `register`, `login`, `refresh`, `logout` на текущем уровне сервиса.
- Используется `JWT access token + refresh token`.
- Refresh token хранится в БД в виде `token_hash`.
- Поддерживается `revokedAt` для отзыва refresh token.
- Конфиг читает значения из `application.yaml` и `.env`.
- Добавлен `JwtAuthenticationFilter` и он подключён в `SecurityConfig`.
- Фильтр уже умеет читать `Authorization: Bearer ...` и класть `Authentication` в `SecurityContext`.
- Для access token используется проверка через claims JWT + поиск пользователя в БД.
- Для refresh token используется отдельная проверка через таблицу `refresh_tokens`.
- Реализованы backend endpoints Google auth:
  - `POST /api/auth/google/login`
  - `POST /api/auth/google/complete`
- `google/login` проверяет Google ID token и работает через `provider + sub`.
- Если OAuth-связка уже существует, сервис логинит существующего пользователя.
- Если OAuth-связки ещё нет, сервис отдаёт `PROFILE_COMPLETION_REQUIRED` + `flowToken`.
- `google/complete` уже создаёт или связывает локального пользователя внутри транзакционного service-метода.

### Frontend auth
- Во frontend уже добавлен endpoint-каталог для Google auth.
- На `front/auth.html` добавлен Google Identity Services script и блок кнопки Google login.
- В `front/app/pages/auth-page.js` уже есть логика:
  - инициализации Google button;
  - получения `credential` (`id_token`);
  - отправки этого `credential` в backend;
  - сохранения app session через существующий `setSession(...)`.
- Для сценария `PROFILE_COMPLETION_REQUIRED` фронт уже умеет открывать форму дозаполнения профиля.

### Тесты
- Есть тесты на `register` service/controller.
- Есть тесты на `login` service/controller.
- Есть тесты на `refresh` service/controller.
- Есть тесты на `logout` service/controller.
- `./mvnw -q test` в `auth_service/legal_website` проходит.
- `./mvnw -q -DskipTests compile` тоже проходит.
- При этом protected auth flow тестами пока не покрыт.

## Что важно помнить по auth
### Access token
- Короткоживущий.
- Ходит в `Authorization: Bearer ...`.
- Нужен для обычных защищённых API-запросов.
- Не хранится в таблице `refresh_tokens`.
- Проверяется не через `token_hash`, а через подпись JWT, expiration и пользователя из claims.

### Refresh token
- Долгоживущий.
- Не должен слаться в каждый API-запрос.
- Используется только для `refresh` и `logout`.
- При `refresh` старый token revoke-ится, новый создаётся и сохраняется.
- Именно refresh token ищется в БД по `token_hash`.

### RevokedAt
- Если `revokedAt == null`, refresh token активен.
- Если `revokedAt != null`, refresh token больше нельзя использовать.

### SecurityContext
- Даже при JWT он нужен.
- Это не server-side session.
- Он хранит текущего аутентифицированного пользователя только на время одного запроса.
- Его заполняет `JwtAuthenticationFilter`, чтобы защищённые controller могли получить `Authentication`.

### Google identity
- Главный внешний идентификатор пользователя Google - это `sub`.
- `email` важен как локальный логин/контакт, но не должен быть главным идентификатором OAuth-связки.
- Если в проекте локальный `users.email` обязателен, это должно быть отдельным бизнес-правилом, а не случайным следствием DTO.
- В `google/complete` source of truth должен быть `flowToken` и claims из него, а не произвольные поля из request body.

## Что сейчас ещё не завершено
- Стабилизация контракта `google/login -> google/complete`.
- Явное решение, какие поля в `google/complete` берутся только из claims, а какие реально можно принимать из body.
- Нормальный единый обработчик auth-ошибок (`400/401/403/409` вместо ухода в общий `500`).
- Проверка повторного вызова `google/complete` и защита от дублей OAuth-связок.
- Проверка сценария "локальный пользователь уже есть по email, OAuth-связки ещё нет".
- Endpoint `GET /api/auth/me`, если он ещё не доведён до финального контракта и тестов.
- Profile API для редактирования данных текущего пользователя.
- Дополнительная чистка нейминга:
  - `Responce` -> `Response`
  - возможное упрощение DTO-структуры
- Тесты именно на protected flow:
  - валидный Bearer token
  - невалидный Bearer token
  - отсутствие Bearer token
  - `GET /api/auth/me`
- Тесты именно на Google flow:
  - первый вход через Google
  - completion для нового пользователя
  - completion для существующего локального пользователя
  - повторный completion
  - невалидный flow token

## Что делать следующим
1. Дочистить Google flow до чёткого state machine и error contract.
2. Привести `GlobalExceptionHandler` к ожидаемым статусам `400/401/403/409`.
3. Довести `GET /api/auth/me`, если нужно, и покрыть protected flow тестами.
4. Добавить profile API (`GET/PATCH /me` или похожий контракт).
5. Покрыть тестами Google flow.
6. Только потом решать вопрос о новых сервисах и асинхронных взаимодействиях.

## Что важно по темпу разработки
- Не оптимизировать проект под "быстрее дописать фичу".
- Сначала фиксировать границы ответственности, source of truth и инварианты.
- Новые микросервисы добавлять только там, где есть ясная ответственность сервиса, а не ради самого слова "микросервис".
- Если появляется новая технология вроде Kafka, broker, outbox или notification-service, сначала нужно объяснить, какую проблему она решает именно в этой системе.

## Что уже поправлено недавно
- У фильтра убраны лишние импорты.
- Для refresh token в `JwtService` методы переименованы понятнее:
  - `isValidRefreshToken(...)`
  - `getValidRefreshToken(...)`
- В фильтре выбран стиль ролей через `ROLE_...`, чтобы дальше можно было использовать `hasRole(...)`.
- Добавлена ветка "нет Bearer header -> пропустить запрос дальше по filter chain".
- Во frontend добавлен Google Sign-In button и вызов `POST /auth/google`.
- В auth-сервисе добавлены `GoogleLoginService` и `GoogleFillService`.
- `google/complete` перенесён в транзакционный service-метод.
- В ходе ревью отдельно зафиксировано, что `@Transactional` не заменяет корректную бизнес-логику и source of truth.

## Google auth
- Выбран сценарий: frontend получает Google `credential` (`id_token`), backend его проверяет и выдаёт обычную app session.
- Redirect/callback OAuth flow на backend сейчас не нужен.
- Для Google Cloud в этом сценарии важно:
  - заполнить `Authorized JavaScript origins`
  - не заполнять `Authorized redirect URIs`, если не используется callback flow
- Для локальной разработки обычно нужен origin фронта:
  - `http://127.0.0.1:8000`
  - при необходимости `http://localhost:8000`

## Потоки / async
- `AsyncConfig` в проекте уже есть, но бизнес-логика auth пока не должна уезжать в `@Async`.
- `login`, `refresh`, `logout`, JWT filter и проверка токенов должны оставаться синхронными.
- Асинхронность имеет смысл только для побочных задач:
  - audit logging
  - welcome email
  - уведомления
  - фоновая очистка просроченных refresh token
- Если async-задаче нужен пользователь, лучше передавать `userId/email` явно, а не рассчитывать на `SecurityContext` в другом потоке.

## Контракт, который ждёт frontend
### Register / Login / Refresh response
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "raw-refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "1",
    "fullName": "Иван Иванов",
    "email": "ivan@test.ru",
    "phone": "+79991234567",
    "companyName": "ООО Ромашка",
    "role": "CLIENT"
  }
}
```

### Refresh request
```json
{
  "refreshToken": "raw-refresh-token"
}
```

### Logout request
```json
{
  "refreshToken": "raw-refresh-token"
}
```

### Google auth request
```json
{
  "credential": "google-id-token"
}
```

### Google login response
- либо обычный auth response, если OAuth-связка уже существует;
- либо объект со статусом `PROFILE_COMPLETION_REQUIRED`, `flowToken` и prefilled profile.

### Google complete request
- на текущем этапе frontend отправляет `flowToken`, `fullName`, `password`;
- `email` либо должен быть исключён из body, либо валидироваться против claims из `flowToken`;
- долгосрочно source of truth для identity должен оставаться в claims.

### Google complete response
- такой же, как у `register/login/refresh`:
  - `accessToken`
  - `refreshToken`
  - `tokenType`
  - `expiresIn`
  - `user`

## Как со мной работать дальше
- Если задача про архитектуру или смысл методов, сначала нужно объяснение.
- Если задача про реализацию, можно переходить к коду и тестам.
- Если контекст потеряется, сначала смотреть этот файл, потом `AGENTS.md`.
- По умолчанию лучше учить через разбор состояний, инвариантов, контрактов и trade-off, а не через мгновенную готовую реализацию.

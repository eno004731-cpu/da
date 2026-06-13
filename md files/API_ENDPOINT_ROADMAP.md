# API Endpoint Roadmap

## Назначение
Этот файл описывает:
- актуальные публичные endpoint-ы проекта
- фронтовые контракты из `front/app/api`
- ближайшие planned API, которые уже видны по frontend и текущему коду сервисов

Legacy backend сюда не входит.

## Легенда
- `Implemented` - endpoint реализован и есть в текущем backend-коде
- `In progress` - groundwork уже есть, но HTTP-контракт или реализация ещё не доведены до рабочего состояния
- `Planned` - frontend или planning-файлы уже ждут endpoint, но backend его пока не закрывает
- `Public` - без Bearer token
- `Auth required` - нужен Bearer token
- `Staff planned` - внутренний staff/lawyer endpoint
- `Admin planned` - внутренний admin endpoint

## Endpoint Map
| Method | Path | Group | Backend status | Access | Request | Response | Source | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Auth | `Implemented` | `Public` | JSON `{ fullName, email, phone?, companyName?, password }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` | Регистрация клиента с немедленной app-session. |
| `POST` | `/api/auth/login` | Auth | `Implemented` | `Public` | JSON `{ email, password }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` | Обычный login по email/password. |
| `POST` | `/api/auth/google/login` | Auth | `Implemented` | `Public` | JSON `{ credential }` | либо `{ authResponse }`, либо `{ googleResponse: { flowToken, profile } }` | `auth_service/legal_website` | Первый Google login может вернуть completion flow. |
| `POST` | `/api/auth/google/complete` | Auth | `Implemented` | `Public` | JSON `{ flowToken, fullName, password }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` | Завершает OAuth first-login. |
| `POST` | `/api/auth/refresh` | Auth | `Implemented` | `Public` | JSON `{ refreshToken }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` | Stateless refresh через refresh token из body. |
| `POST` | `/api/auth/logout` | Auth | `Implemented` | `Auth required` | JSON `{ refreshToken }` | `boolean` | `auth_service/legal_website` | Инвалидирует refresh token. |
| `GET` | `/api/auth/me` | Profile | `Implemented` | `Auth required` | Bearer token | JSON user profile | `auth_service/legal_website` | Source of truth для frontend profile/completion state. |
| `PATCH` | `/api/auth/me` | Profile | `Implemented` | `Auth required` | JSON profile patch | JSON user profile | `auth_service/legal_website` | Только non-sensitive profile update. |
| `DELETE` | `/api/auth/account` | Profile | `Implemented` | `Auth required` | Bearer token | `204 No Content` | `auth_service/legal_website` | Текущее удаление аккаунта. |
| `POST` | `/api/auth/email-verification/request` | Verification | `Implemented` | `Auth required` | Bearer token, без body | `VerityEmailResponse` | `auth_service/legal_website` | Контроллер уже есть, flow отправляет событие на notification-service. |
| `POST` | `/api/auth/email-verification/confirm` | Verification | `Implemented` | `Public` | JSON `{ token }` | `boolean` | `auth_service/legal_website` | Public confirm endpoint уже объявлен в security и controller. |
| `GET` | `/api/services` | Catalog | `Implemented` | `Public` | без body | `Array<ServiceItem>` | `catalog-service` | Возвращает активные услуги каталога для frontend. |
| `POST` | `/api/client/applications` | Client orders | `In progress` | `Auth required` | `multipart/form-data` | `CreateApplicationResponse` | `front/app/api/orders-api.js` + `order-service` groundwork | В `order-service` уже есть сервисный слой создания заказа и outbox, но публичный controller/HTTP-контракт ещё не доведён. |
| `GET` | `/api/client/orders` | Client orders | `Planned` | `Auth required` | Bearer token | `Array<ClientOrderSummary>` | `front/app/api/orders-api.js` | Frontend уже ждёт список заказов клиента. |
| `GET` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | Path `orderId` | `ClientOrderDetails` | `front/app/api/orders-api.js` | Детали заказа. |
| `PATCH` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | JSON editable fields | `ClientOrderDetails` | `front/app/api/orders-api.js` | Редактирование заявки клиентом. |
| `DELETE` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | Path `orderId` | `void` | `front/app/api/orders-api.js` | Удаление заявки клиентом. |
| `POST` | `/api/client/orders/{orderId}/rework` | Client orders | `Planned` | `Auth required` | JSON `{ comment }` | `ClientOrderDetails` | `front/app/api/orders-api.js` | Возврат на доработку. |
| `GET` | `/api/client/orders/{orderId}/documents` | Client documents | `Planned` | `Auth required` | Path `orderId` | `Array<UploadedDocument>` | `front/app/api/endpoints.js` | Контракт уже ожидается frontend-слоем. |
| `POST` | `/api/client/orders/{orderId}/documents` | Client documents | `Planned` | `Auth required` | `multipart/form-data` | `UploadedDocument` или `Array<UploadedDocument>` | `front/app/api/endpoints.js` | Вероятный public entrypoint для document-service через API-слой. |
| `GET` | `/api/staff/board/tasks` | Staff board | `Planned` | `Staff planned` | Bearer token | `Array<StaffBoardTask>` | `front/app/api/staff-api.js` | Доска задач юриста. |
| `GET` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | Path `taskId` | `StaffBoardTask` | `front/app/api/staff-api.js` | Детали задачи. |
| `PATCH` | `/api/staff/board/tasks/{taskId}/status` | Staff board | `Planned` | `Staff planned` | JSON `{ status }` | `StaffBoardTask` | `front/app/api/staff-api.js` | Отдельная операция смены статуса. |
| `PATCH` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | JSON editable fields | `StaffBoardTask` | `front/app/api/staff-api.js` | Редактирование карточки. |
| `POST` | `/api/staff/board/tasks/{taskId}/reject` | Staff board | `Planned` | `Staff planned` | JSON `{ reason }` | `StaffBoardTask` | `front/app/api/staff-api.js` | Отклонение задачи. |
| `DELETE` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | Path `taskId` | `void` | `front/app/api/staff-api.js` | Удаление карточки. |
| `POST` | `/api/staff/board/tasks/{taskId}/comments` | Staff board | `Planned` | `Staff planned` | JSON `{ body }` | `StaffBoardTask` | `front/app/api/staff-api.js` | Комментарий к задаче. |
| `GET` | `/api/admin/users?role=&active=` | Admin users | `Planned` | `Admin planned` | Query params | `Array<AdminUserListItem>` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Список пользователей для внутреннего кабинета. |
| `GET` | `/api/admin/users/{id}` | Admin users | `Planned` | `Admin planned` | Path `id` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Карточка пользователя. |
| `POST` | `/api/admin/lawyers` | Admin users | `Planned` | `Admin planned` | JSON `{ fullName, email, phone?, password }` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Создание пользователя с ролью `LAWYER`. |
| `PATCH` | `/api/admin/users/{id}/deactivate` | Admin users | `Planned` | `Admin planned` | Path `id` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Soft deactivate. |
| `PATCH` | `/api/admin/users/{id}/activate` | Admin users | `Planned` | `Admin planned` | Path `id` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Возврат в active-состояние. |
| `DELETE` | `/api/admin/users/{id}` | Admin users | `Planned` | `Admin planned` | Path `id` | `204 No Content` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Hard delete только для inactive user. |

## Internal Async Contracts

### Auth -> Notification
- Topic: `auth.email-verification.requested`
- Status: `Implemented`
- Назначение: отправка писем подтверждения email через outbox/event flow

### Order -> Catalog
- Topic: `order.getServiceName.request`
- Status: `In progress`
- Назначение: запрос `serviceName` по `serviceCode` после создания заказа

### Catalog -> Order
- Topic: `order.getServiceName.response`
- Status: `In progress`
- Назначение: ответ с `serviceName` и `eventId` для идемпотентного обновления заказа

## Notes By Group

### Auth
- auth-service уже покрывает базовый login/register/refresh/logout flow
- Google login и profile completion уже рабочая часть сервиса

### Verification
- endpoint-ы подтверждения email уже существуют как HTTP-контракт
- основной риск сейчас не в controller-слое, а в end-to-end стабильности async delivery

### Catalog
- `GET /api/services` уже есть и соответствует ожиданиям frontend
- Kafka inbox/outbox flow каталога ещё не считается стабильным источником правды для `serviceName`

### Client orders
- `front/app/api/orders-api.js` уже фиксирует ожидаемый HTTP-контракт
- `order-service` уже имеет service-layer groundwork, но публичный REST слой пока не завершён

### Staff / Admin
- эти endpoint-ы уже описаны во frontend/planning-слое
- backend для них в текущем состоянии ещё не строится

## Ближайший backend focus
Сейчас следующий реальный шаг:
1. починить `catalog-service`, чтобы он собирался и корректно обрабатывал request topic
2. дожать `order-service <-> catalog-service` reply flow
3. после этого реализовать публичные `/api/client/...` endpoint-ы

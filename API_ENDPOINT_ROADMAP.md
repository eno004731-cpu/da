# API Endpoint Roadmap

## Назначение
Этот файл описывает:
- актуальные endpoint-ы `auth_service/legal_website`
- фронтовые контракты из `front/app/api`
- ближайшие planned API, которые уже видны по frontend и planning-докам

Legacy backend сюда не входит.

## Легенда
- `Implemented` - endpoint реализован и используется текущим backend
- `In progress` - groundwork уже есть, но контракт ещё не стабилизирован
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
| `PATCH` | `/api/auth/me` | Profile | `Implemented` | `Auth required` | JSON profile patch | JSON user profile | `auth_service/legal_website` | Используется для non-sensitive profile update. |
| `DELETE` | `/api/auth/account` | Profile | `Implemented` | `Auth required` | Bearer token | `204 No Content` | `auth_service/legal_website` | Текущее удаление аккаунта. |
| `POST` | `/api/auth/email-verification/request` | Verification | `In progress` | `Auth required` | Bearer token, без body | JSON `{ message }` или verify DTO | frontend contract + backend groundwork | Frontend этот путь уже ожидает, но backend HTTP-контракт ещё нужно довести. |
| `POST` | `/api/auth/email-verification/confirm` | Verification | `In progress` | `Public` | JSON `{ token }` | JSON `{ success }` или `boolean` | frontend contract + backend groundwork | Confirm flow уже заложен на уровне token/service, но HTTP-контракт ещё не финализирован. |
| `POST` | `/api/client/applications` | Client orders | `Planned` | `Auth required` | `multipart/form-data` | JSON create result | `front/app/api/orders-api.js` | Создание клиентской заявки с файлами. |
| `GET` | `/api/client/orders` | Client orders | `Planned` | `Auth required` | Bearer token | `Array<OrderSummary>` | `front/app/api/orders-api.js` | Список заказов клиента. |
| `GET` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | Path `orderId` | `OrderDetails` | `front/app/api/orders-api.js` | Детали заказа. |
| `PATCH` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | JSON editable fields | `OrderDetails` | `front/app/api/orders-api.js` | Редактирование заявки клиентом. |
| `DELETE` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | Path `orderId` | `void` | `front/app/api/orders-api.js` | Удаление заявки клиентом. |
| `POST` | `/api/client/orders/{orderId}/rework` | Client orders | `Planned` | `Auth required` | JSON `{ comment }` | `OrderDetails` | `front/app/api/orders-api.js` | Возврат на доработку. |
| `GET` | `/api/staff/board/tasks` | Staff board | `Planned` | `Staff planned` | Bearer token | `Array<TaskCard>` | `front/app/api/staff-api.js` | Доска задач юриста. |
| `GET` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | Path `taskId` | `TaskDetails` | `front/app/api/staff-api.js` | Детали задачи. |
| `PATCH` | `/api/staff/board/tasks/{taskId}/status` | Staff board | `Planned` | `Staff planned` | JSON `{ status }` | `TaskDetails` | `front/app/api/staff-api.js` | Отдельная операция смены статуса. |
| `PATCH` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | JSON editable fields | `TaskDetails` | `front/app/api/staff-api.js` | Редактирование карточки. |
| `POST` | `/api/staff/board/tasks/{taskId}/reject` | Staff board | `Planned` | `Staff planned` | JSON `{ reason }` | `TaskDetails` | `front/app/api/staff-api.js` | Отклонение задачи. |
| `DELETE` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | Path `taskId` | `void` | `front/app/api/staff-api.js` | Удаление карточки. |
| `POST` | `/api/staff/board/tasks/{taskId}/comments` | Staff board | `Planned` | `Staff planned` | JSON `{ body }` | `TaskDetails` | `front/app/api/staff-api.js` | Комментарий к задаче. |
| `GET` | `/api/admin/users?role=&active=` | Admin users | `Planned` | `Admin planned` | Query params | `Array<AdminUserListItem>` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Список пользователей для внутреннего кабинета. |
| `GET` | `/api/admin/users/{id}` | Admin users | `Planned` | `Admin planned` | Path `id` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Карточка пользователя. |
| `POST` | `/api/admin/lawyers` | Admin users | `Planned` | `Admin planned` | JSON `{ fullName, email, phone?, password }` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Создание пользователя с ролью `LAWYER`. |
| `PATCH` | `/api/admin/users/{id}/deactivate` | Admin users | `Planned` | `Admin planned` | Path `id` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Soft deactivate. |
| `PATCH` | `/api/admin/users/{id}/activate` | Admin users | `Planned` | `Admin planned` | Path `id` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Возврат в active-состояние. |
| `DELETE` | `/api/admin/users/{id}` | Admin users | `Planned` | `Admin planned` | Path `id` | `204 No Content` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Hard delete только для inactive user. |

## Notes By Group

### Auth
- Текущий auth-service уже покрывает базовый login/register/refresh/logout flow
- Google login и profile completion уже не planning, а рабочая часть сервиса

### Profile
- `PATCH /api/auth/me` уже реализован и больше не должен описываться как planned endpoint
- sensitive changes вроде `email`, `phone`, `password` нужно выносить в отдельные confirm-flow

### Verification
- groundwork под email verification уже есть в БД, relay и notification contract
- но публичный HTTP-контракт ещё нужно стабилизировать и привести в финальный вид

### Client / Staff / Admin
- эти endpoint-ы уже описаны во frontend/planning-слое
- но backend для них в текущем auth-service пока не строится

## Ближайший backend focus
Сейчас следующий реальный шаг не `PATCH /api/auth/me`, а end-to-end email verification flow:
1. зафиксировать request/confirm endpoint contract
2. дожать outbox -> Kafka -> notification delivery
3. связать frontend страницу verify-email с confirm endpoint

# API Endpoint Roadmap

## Назначение
Этот файл описывает только актуальный `auth_service/legal_website` и frontend-контракты из `front/app/api`.

- Неактуальный legacy backend сюда не входит.
- В roadmap учитывается только токенная модель авторизации.
- Под "не закрытым endpoint" здесь понимаются два случая:
  - endpoint ещё не реализован;
  - endpoint ещё не закрыт по access-правилам или роли пока не зафиксированы.

## Легенда
- `Implemented` - endpoint уже реализован в текущем backend.
- `Planned` - endpoint уже ожидается frontend-ом, но backend его пока не реализует.
- `Public` - endpoint доступен без Bearer token.
- `Auth required` - endpoint требует авторизацию.
- `Role TBD` - endpoint точно не public, но ролевая модель ещё не зафиксирована.
- `Admin planned` - endpoint запланирован только для роли `ADMIN`.
- `Staff planned` - endpoint запланирован для внутренней staff/lawyer зоны.

## Endpoint Map
| Method | Path | Group | Backend status | Access | Request | Response | Source | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Auth | `Implemented` | `Public` | JSON `{ fullName, email, phone?, companyName?, password }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` controller + DTO | Базовая регистрация клиента с немедленной auth-session. |
| `POST` | `/api/auth/login` | Auth | `Implemented` | `Public` | JSON `{ email, password }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` controller + DTO | Обычный login по email/password. |
| `POST` | `/api/auth/google/login` | Auth | `Implemented` | `Public` | JSON `{ credential }` | `GoogleResponse`: либо `{ authResponse }`, либо `{ googleResponse: { status, flowToken, profile } }` | `auth_service/legal_website` controller + DTO | Первый вход через Google может вернуть flow дозаполнения профиля. |
| `POST` | `/api/auth/google/complete` | Profile | `Implemented` | `Public` | JSON `{ flowToken, fullName, password }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` controller + DTO | Завершает OAuth first-login и создаёт локальную app-session. |
| `POST` | `/api/auth/refresh` | Auth | `Implemented` | `Public` | JSON `{ refreshToken }` | JSON `{ accessToken, refreshToken, tokenType, expiresIn, user }` | `auth_service/legal_website` controller + DTO | Stateless refresh через refresh token из body. |
| `POST` | `/api/auth/logout` | Auth | `Implemented` | `Auth required` | JSON `{ refreshToken }` | `boolean` | `auth_service/legal_website` controller + DTO | Инвалидирует refresh token. |
| `GET` | `/api/auth/me` | Profile | `Implemented` | `Auth required` | Bearer token, без body | JSON `{ id, fullName, email, phone, companyName, role, authProvider, authProviders, isOAuthUser, hasPassword, needsPasswordSetup, requiresProfileCompletion }` | `auth_service/legal_website` controller + DTO | Источник текущего профиля и completion-флагов. |
| `DELETE` | `/api/auth/account` | Profile | `Implemented` | `Auth required` | Bearer token, без body | `204 No Content` | `auth_service/legal_website` controller | Сейчас это soft delete текущего аккаунта. |
| `PATCH` | `/api/auth/me` | Profile | `Planned` | `Auth required` | JSON `{ fullName, phone, companyName }` | JSON `{ id, fullName, email, phone, companyName, role, authProvider?, authProviders?, isOAuthUser?, hasPassword?, needsPasswordSetup?, requiresProfileCompletion? }` | `front/app/api/endpoints.js` + `auth-api.js` + `complete-profile-page.js` | Ближайший следующий endpoint для редактирования профиля и completion flow. |
| `POST` | `/api/client/applications` | Client orders | `Planned` | `Auth required` | `multipart/form-data`: `serviceCode`, `clientName`, `contact`, `companyName`, `description`, `documents[]` | JSON `{ id, status, trackingCode }` или близкий create-result контракт | `front/app/api/endpoints.js` + `orders-api.js` | Создание клиентской заявки с файлами. |
| `GET` | `/api/client/orders` | Client orders | `Planned` | `Auth required` | Bearer token, без body | `Array<OrderSummary>` | `front/app/api/endpoints.js` + `orders-api.js` | Список заказов клиента в кабинете. |
| `GET` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | Path `orderId`, без body | `OrderDetails` | `front/app/api/endpoints.js` + `orders-api.js` | Детальная карточка заказа. |
| `PATCH` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | JSON `{ serviceCode, clientName, contact, companyName, description }` | `OrderDetails` или `{ success: true }` | `front/app/api/endpoints.js` + `orders-api.js` | Клиентское редактирование заявки. |
| `DELETE` | `/api/client/orders/{orderId}` | Client orders | `Planned` | `Auth required` | Path `orderId`, без body | `void` или `{ success: true }` | `front/app/api/endpoints.js` + `orders-api.js` | Удаление клиентской заявки. |
| `POST` | `/api/client/orders/{orderId}/rework` | Client orders | `Planned` | `Auth required` | JSON `{ comment }` | `OrderDetails` или `{ success: true }` | `front/app/api/endpoints.js` + `orders-api.js` | Возврат заявки на доработку юристу. |
| `GET` | `/api/staff/board/tasks` | Staff board | `Planned` | `Staff planned` | Bearer token, без body | `Array<TaskCard>` или `{ items: TaskCard[] }` | `front/app/api/endpoints.js` + `staff-api.js` | Лента задач для доски юриста. |
| `GET` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | Path `taskId`, без body | `TaskDetails` | `front/app/api/endpoints.js` + `staff-api.js` | Детали задачи на доске. |
| `PATCH` | `/api/staff/board/tasks/{taskId}/status` | Staff board | `Planned` | `Staff planned` | JSON `{ status }` | `TaskDetails` | `front/app/api/endpoints.js` + `staff-api.js` | Отдельная операция смены статуса. |
| `PATCH` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | JSON с editable-полями задачи, минимум `{ serviceCode, clientName, contact, companyName, description }` | `TaskDetails` | `front/app/api/endpoints.js` + `staff-api.js` | Staff-модалка редактирования карточки. |
| `POST` | `/api/staff/board/tasks/{taskId}/reject` | Staff board | `Planned` | `Staff planned` | JSON `{ reason }` | `TaskDetails` | `front/app/api/endpoints.js` + `staff-api.js` | Отклонение задачи с причиной. |
| `DELETE` | `/api/staff/board/tasks/{taskId}` | Staff board | `Planned` | `Staff planned` | Path `taskId`, без body | `void` или `{ success: true }` | `front/app/api/endpoints.js` + `staff-api.js` | Удаление карточки с доски. |
| `POST` | `/api/staff/board/tasks/{taskId}/comments` | Staff board | `Planned` | `Staff planned` | JSON `{ body }` | `TaskDetails` | `front/app/api/endpoints.js` + `staff-api.js` | Добавление комментария к задаче. |
| `GET` | `/api/admin/users?role=&active=` | Admin users | `Planned` | `Admin planned` | Query params `role` и `active`, без body | `Array<AdminUserListItem>` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Список пользователей для внутреннего кабинета. |
| `GET` | `/api/admin/users/{id}` | Admin users | `Planned` | `Admin planned` | Path `id`, без body | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Просмотр одной карточки пользователя. |
| `POST` | `/api/admin/lawyers` | Admin users | `Planned` | `Admin planned` | JSON `{ fullName, email, phone?, password }` | `AdminUserDetails` или create-result с пользователем | `ADMIN_PANEL_USERS_V1_PLAN.md` | Добавление нового юриста. |
| `PATCH` | `/api/admin/users/{id}/deactivate` | Admin users | `Planned` | `Admin planned` | Path `id`, без body или опционально `{ reason }` | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Soft deactivate пользователя. |
| `PATCH` | `/api/admin/users/{id}/activate` | Admin users | `Planned` | `Admin planned` | Path `id`, без body | `AdminUserDetails` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Возврат пользователя в active-состояние. |
| `DELETE` | `/api/admin/users/{id}` | Admin users | `Planned` | `Admin planned` | Path `id`, без body | `204 No Content` или `{ success: true }` | `ADMIN_PANEL_USERS_V1_PLAN.md` | Hard delete разрешён только для `isActive=false`. |

## Notes By Group

### Auth
- Текущий `auth_service` уже покрывает register/login/refresh/logout и Google login flow.
- Public auth endpoint-ы не используют session-модель, поэтому roadmap не должен тянуть старые артефакты авторизации.

### Profile
- `GET /api/auth/me` уже стал центральной точкой для профиля и completion-флагов.
- Следующий логичный шаг backend-а - `PATCH /api/auth/me`, потому что frontend уже вызывает именно этот путь.

### Client orders
- Эти endpoint-ы уже описаны frontend-слоем, но backend-контракт ещё не реализован в актуальном сервисе.
- Ролевая модель здесь простая: обычный клиент работает только со своими заказами.

### Staff board
- Эти endpoint-ы уже зафиксированы в `staff-api.js`, значит UI-слой под них спроектирован.
- Точный набор ролей и прав можно позже уточнить как `LAWYER` или `STAFF`, но публичными они не должны быть.

### Admin users
- Внутренние user-management endpoint-ы вынесены отдельно, чтобы не смешивать клиентский профиль и админские операции над чужими аккаунтами.

## Next Endpoint In Work
- Endpoint: `PATCH /api/auth/me`
- Почему он следующий:
  - путь уже существует во frontend-контракте как `updateMe`;
  - страница `complete-profile.html` уже ожидает, что этот endpoint сможет завершать заполнение профиля;
  - он нужен раньше, чем admin и orders backend.
- Ожидаемый request:
  - JSON `{ fullName, phone, companyName }`
- Ожидаемый response:
  - JSON `{ id, fullName, email, phone, companyName, role, authProvider?, authProviders?, isOAuthUser?, hasPassword?, needsPasswordSetup?, requiresProfileCompletion? }`
- Важная заметка:
  - endpoint должен поддержать сценарий дозаполнения профиля после логина и не ломать текущую модель `GET /api/auth/me`.

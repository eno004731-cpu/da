# Admin Panel Users V1 Plan

## Цель
Сделать отдельный внутренний кабинет для работы с пользователями.

Это:
- не full backoffice
- не board/tasks module
- не CRM для всех сущностей сразу
- не замена клиентскому self-service профилю

Фокус только на `users v1`.

## Что уже есть в общей системе
В текущем проекте users уже живут в `auth_service/legal_website`.
В модели пользователя уже есть важные поля:
- `role`
- `isActive`
- `emailVerified`
- `emailVerifiedAt`

Это значит, что admin users panel должна строиться не отдельно от auth-domain, а поверх существующей users-модели.

## Роли
- `ADMIN` - управляет пользователями и внутренними действиями над аккаунтами
- `LAWYER` - внутренний исполнитель, которого admin может создавать и сопровождать
- `CLIENT` - обычный пользователь клиентского кабинета

## Scope V1
- добавление юристов
- список пользователей
- фильтрация по роли
- фильтрация по `isActive`
- просмотр клиентов
- просмотр неактивных аккаунтов
- просмотр `emailVerified` как полезного статуса аккаунта
- удаление аккаунтов только если `isActive=false`

## Out Of Scope
- board/tasks
- order workflow
- comments/history
- массовые операции
- аудит-лог
- продвинутый RBAC глубже уровня `ADMIN` против остальных

## UI Sections
- Список юристов
- Форма добавления юриста
- Общий список пользователей с фильтрами
- Список клиентов
- Список неактивных пользователей
- Карточка пользователя с действиями `activate/deactivate/delete`

## Planned Endpoints
| Method | Path | Access | Request | Response | Бизнес-ограничения |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/admin/users?role=&active=` | Только `ADMIN` | Query params: `role`, `active` | `Array<AdminUserListItem>` | Таблица пользователей и фильтры. |
| `GET` | `/api/admin/users/{id}` | Только `ADMIN` | Path `id` | `AdminUserDetails` | Просмотр одной карточки пользователя. |
| `POST` | `/api/admin/lawyers` | Только `ADMIN` | JSON `{ fullName, email, phone?, password }` | `AdminUserDetails` | Новый пользователь создаётся с ролью `LAWYER`. |
| `PATCH` | `/api/admin/users/{id}/deactivate` | Только `ADMIN` | Path `id`, optional `{ reason }` | `AdminUserDetails` | Soft deactivate, а не delete. |
| `PATCH` | `/api/admin/users/{id}/activate` | Только `ADMIN` | Path `id` | `AdminUserDetails` | Разрешено только для inactive users. |
| `DELETE` | `/api/admin/users/{id}` | Только `ADMIN` | Path `id` | `204 No Content` или `{ success: true }` | Только если `isActive=false`. |

## Suggested DTO Shapes

### `AdminUserListItem`
```json
{
  "id": 12,
  "fullName": "Иван Петров",
  "email": "ivan@example.com",
  "phone": "+79990000000",
  "role": "LAWYER",
  "isActive": true,
  "emailVerified": true,
  "createdAt": "2026-05-27T12:00:00"
}
```

### `AdminUserDetails`
```json
{
  "id": 12,
  "fullName": "Иван Петров",
  "email": "ivan@example.com",
  "phone": "+79990000000",
  "companyName": null,
  "role": "LAWYER",
  "isActive": true,
  "emailVerified": true,
  "emailVerifiedAt": "2026-05-27T12:10:00",
  "createdAt": "2026-05-27T12:00:00",
  "updatedAt": "2026-05-27T12:30:00"
}
```

## Business Rules
- Hard delete запрещён для активного пользователя
- Soft deactivate и hard delete - разные операции
- Юрист создаётся как обычный user с ролью `LAWYER`
- `CLIENT` и `LAWYER` не должны иметь доступ к `/api/admin/*`
- При сомнении сначала deactivate, потом delete
- `emailVerified` - полезный диагностический статус для admin UI, но не повод редактировать verification state напрямую из админки

## Implementation Notes
- Этот модуль не должен смешиваться с клиентским `PATCH /api/auth/me`
- User-management лучше строить поверх уже существующей auth users-модели
- Для V1 достаточно read/write операций над пользователями без связи с заказами и доской
- Если позже появится полноценная admin panel, этот план можно расширить модулями `Orders`, `Board`, `Audit`

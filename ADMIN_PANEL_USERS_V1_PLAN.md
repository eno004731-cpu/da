# Admin Panel Users V1 Plan

## Цель
Сделать отдельный внутренний кабинет для работы с пользователями.

- Это не full backoffice.
- Это не модуль управления задачами доски.
- Это не CRM для всех сущностей сразу.
- Фокус только на `users v1`.

## Роли
- `ADMIN` - управляет пользователями и внутренними действиями над аккаунтами.
- `LAWYER` - внутренний исполнитель, которого админ может создавать и сопровождать.
- `CLIENT` - обычный пользователь клиентского кабинета.

## Scope V1
- добавление юристов;
- список пользователей;
- фильтрация по роли;
- фильтрация по `isActive`;
- просмотр клиентов;
- просмотр неактивных аккаунтов;
- удаление аккаунтов только если `isActive=false`.

## Out Of Scope
- board/tasks;
- order workflow;
- comments/history;
- полное управление заказами;
- массовые операции;
- аудит-лог и продвинутая аналитика;
- RBAC тоньше уровня `ADMIN` против остальных ролей.

## UI Sections
- Список юристов.
- Форма добавления юриста.
- Общий список пользователей с фильтрами.
- Список клиентов.
- Список неактивных пользователей.
- Карточка пользователя с базовыми действиями `activate/deactivate/delete`.

## Planned Endpoints
| Method | Path | Access | Request | Response | Бизнес-ограничения |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/admin/users?role=&active=` | Только `ADMIN` | Query params: `role`, `active`; без body | `Array<AdminUserListItem>` | Возвращает список пользователей для таблицы и фильтров. |
| `GET` | `/api/admin/users/{id}` | Только `ADMIN` | Path `id`; без body | `AdminUserDetails` | Используется для просмотра одной карточки пользователя. |
| `POST` | `/api/admin/lawyers` | Только `ADMIN` | JSON `{ fullName, email, phone?, password }` | `AdminUserDetails` или create-result с созданным пользователем | Новый пользователь создаётся с ролью `LAWYER`. |
| `PATCH` | `/api/admin/users/{id}/deactivate` | Только `ADMIN` | Path `id`; без body или опционально `{ reason }` | `AdminUserDetails` | Это soft deactivate, а не удаление записи. |
| `PATCH` | `/api/admin/users/{id}/activate` | Только `ADMIN` | Path `id`; без body | `AdminUserDetails` | Разрешено только для ранее деактивированных пользователей. |
| `DELETE` | `/api/admin/users/{id}` | Только `ADMIN` | Path `id`; без body | `204 No Content` или `{ success: true }` | Разрешено только если `isActive=false`. Для active аккаунта delete должен быть запрещён. |

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
  "createdAt": "2026-05-27T12:00:00",
  "updatedAt": "2026-05-27T12:30:00"
}
```

## Business Rules
- Hard delete запрещён для активного пользователя.
- Soft delete и hard delete - разные операции и не должны маскироваться друг под друга.
- Юрист создаётся как обычный пользователь с ролью `LAWYER`.
- Чистка неактивных аккаунтов - только admin-действие.
- `CLIENT` и `LAWYER` не должны иметь доступ к `/api/admin/*`.
- Если нужен reversible сценарий, сначала выполняется deactivate, а не delete.

## Implementation Notes
- Этот модуль лучше держать отдельно от клиентского `PATCH /api/auth/me`, чтобы не смешивать self-service профиль и административные действия над чужими аккаунтами.
- Для первой версии достаточно read/write операций над пользователями без связи с заказами и доской.
- Если позже появится полноценная admin panel, этот файл можно расширить отдельными разделами `Orders`, `Board`, `Audit`, но не раньше стабилизации `users v1`.

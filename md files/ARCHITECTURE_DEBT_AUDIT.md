# Architecture Debt Audit

Дата проверки: 2026-06-13.

Цель этого документа - зафиксировать архитектурный долг без массового рефакторинга в текущем шаге.
Для учебного проекта это полезно: мы не прячем шероховатости, а явно решаем, что чинить сейчас,
что отложить и что оставить как учебный материал.

## Fix Now

- Синхронизировать статусы event/outbox/inbox в миграциях, комментариях и Java-коде.
  Сейчас в разных сервисах встречаются `NEW`, `PROCESSING`, `PUBLISHED`, `RECEIVED`, `ACCEPTED`,
  `PROCESSED`, `FAILED`, `DEAD`. Само наличие разных статусов нормально, но их смысл должен быть
  одинаково описан рядом с таблицами и сервисами.
- Проверить CHECK constraints в outbox-таблицах после добавления retry/dead-letter логики.
  Если Java-код умеет ставить `PROCESSING` или `DEAD`, БД тоже должна разрешать эти значения.
- Зафиксировать публичные event contracts для `document.stored` и notification events.
  Минимум: имя topic, имя event type, обязательные поля payload, idempotency key.

## Fix Later

- Разнести entity и repository по разным пакетам.
  Сейчас бывший `EntityAndRepo` приведён к lowercase `persistence`, но entity/repository всё ещё
  лежат рядом. Следующий шаг - разделить `entity` и `repository`, когда доменные границы будут
  стабильнее.
- Выделить общую библиотеку или хотя бы повторяемый шаблон для outbox/inbox.
  Сейчас логика relay, статусов, retry и idempotency повторяется между сервисами. Рано выносить
  это в shared library до стабилизации контрактов, но повтор уже виден.
- Привести названия Kafka listener/service-классов к одному стилю.
  Лучше, чтобы из имени было понятно: это listener, publisher, relay или handler.

## Fixed

- Исправлены основные опечатки в Java-коде: `Nofilication` -> `Notification`,
  `ComfirmEmail` -> `ConfirmEmail`, `VerityEmail*` -> `VerifyEmail*`, `EventMetods` -> `EventMethods`.
- Нормализованы mixed-case Java package names в auth/order/notification сервисах:
  `api`, `dto`, `configs`, `services`, `persistence`.
- Исторический Flyway-файл `V3__create_incomming_events_table.sql` оставлен со старым именем
  намеренно: переименование уже применённой миграции может сломать Flyway validation на существующих БД.

## Leave As Is For Learning

- Оставить outbox/inbox реализацию внутри каждого сервиса на ближайший этап.
  Для обучения полезно видеть полный механизм в каждом сервисе, а не прятать его слишком рано в
  абстракцию.
- Оставить `metadata JSONB` для document read-model.
  Пока контракт документов развивается, JSONB даёт гибкость. Когда поля стабилизируются, часть из
  них можно вынести в отдельные typed columns.
- Не добавлять download endpoint в этот этап.
  Сейчас задача - стабилизировать upload, Kafka event и order read-model. Download, checksum,
  versioning и soft-delete лучше делать отдельным вертикальным flow.

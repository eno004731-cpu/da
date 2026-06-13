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

- Исправить опечатки в именах пакетов, классов и миграций:
  `Nofilication`, `Comfirm`, `Verity`, `incomming`, `EventMetods`.
  Это лучше делать отдельным коммитом, потому что переименования затрагивают imports, package names
  и иногда уже существующие таблицы/миграции.
- Нормализовать package naming.
  Сейчас встречаются mixed-case пакеты вроде `EntityAndRepo`, `Services`, `Notification`.
  Для Java/Spring привычнее lowercase-пакеты: `entity`, `repository`, `service`, `controller`,
  `config`, `dto`.
- Разнести entity и repository по разным пакетам.
  `EntityAndRepo` удобен на раннем этапе, но при росте домена сложнее искать ownership и границы.
- Выделить общую библиотеку или хотя бы повторяемый шаблон для outbox/inbox.
  Сейчас логика relay, статусов, retry и idempotency повторяется между сервисами. Рано выносить
  это в shared library до стабилизации контрактов, но повтор уже виден.
- Привести названия Kafka listener/service-классов к одному стилю.
  Лучше, чтобы из имени было понятно: это listener, publisher, relay или handler.

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

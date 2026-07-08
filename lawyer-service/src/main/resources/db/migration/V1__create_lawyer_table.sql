-- Таблица lawyers хранит учетные записи юристов для auth-сервиса.
-- Пароль не храним в открытом виде: сюда записывается только BCrypt hash.
CREATE TABLE lawyers (
	id UUID PRIMARY KEY,

	email VARCHAR(320),
	password_hash VARCHAR(255) NOT NULL,

	first_name VARCHAR(100) NOT NULL,
	last_name VARCHAR(100) NOT NULL,
	middle_name VARCHAR(100),

	phone VARCHAR(32),
	bar_number VARCHAR(64),
	specialization VARCHAR(120),

	role VARCHAR(50) NOT NULL DEFAULT 'LAWYER',
	status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
	is_deleted BOOLEAN NOT NULL DEFAULT true, 
	last_login_at TIMESTAMPTZ,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

	-- Email - главный login identifier, поэтому две учетные записи с одним email запрещены.
	CONSTRAINT uq_lawyers_email UNIQUE (email),

	-- Пока роли и статусы храним строками, но ограничиваем допустимые значения на уровне БД.
	CONSTRAINT chk_lawyers_role CHECK (role IN ('LAWYER', 'ADMIN')),
	CONSTRAINT chk_lawyers_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'PENDING_VERIFICATION'))
);

-- Индекс ускорит частые выборки юристов по статусу, например для админки.
CREATE INDEX idx_lawyers_status ON lawyers (status);

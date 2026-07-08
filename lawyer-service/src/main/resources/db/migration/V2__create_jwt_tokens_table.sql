-- Таблица jwt_tokens хранит серверное состояние JWT-сессий.
-- Важно: сам JWT не сохраняем, только hash токена. При утечке БД это снижает риск захвата сессий.
CREATE TABLE jwt_tokens (
	id UUID PRIMARY KEY,

	lawyer_id UUID NOT NULL,
	token_hash VARCHAR(128) NOT NULL,


	revoked BOOLEAN NOT NULL DEFAULT false,
	revoked_at TIMESTAMPTZ,
	revoked_reason VARCHAR(255),

	expires_at TIMESTAMPTZ NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	last_used_at TIMESTAMPTZ,

	ip_address VARCHAR(45),
	user_agent VARCHAR(512),

	CONSTRAINT fk_jwt_tokens_lawyer
		FOREIGN KEY (lawyer_id) REFERENCES lawyers (id) ON DELETE CASCADE,


	CONSTRAINT uq_jwt_tokens_token_hash UNIQUE (token_hash),
	
	CONSTRAINT chk_jwt_tokens_revoked_at CHECK (
		(revoked = false AND revoked_at IS NULL)
		OR
		(revoked = true AND revoked_at IS NOT NULL)
	)
);

-- Поиск активных токенов пользователя для списка сессий и массового logout.
CREATE INDEX idx_jwt_tokens_lawyer_active
	ON jwt_tokens (lawyer_id, expires_at)
	WHERE revoked = false;

-- Ускоряет фоновую очистку просроченных токенов.
CREATE INDEX idx_jwt_tokens_expires_at ON jwt_tokens (expires_at);

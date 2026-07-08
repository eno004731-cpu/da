-- История rotation refresh-токенов: старый токен может ссылаться на новый.
ALTER TABLE jwt_tokens
	ADD COLUMN replaced_by_token_id UUID;

ALTER TABLE jwt_tokens
	ADD CONSTRAINT fk_jwt_tokens_replaced_by_token
		FOREIGN KEY (replaced_by_token_id) REFERENCES jwt_tokens (id) ON DELETE SET NULL;

-- Блокировка refresh-сессии. Это блокирует не весь аккаунт, а конкретную cookie/session chain.
ALTER TABLE jwt_tokens
	ADD COLUMN session_blocked BOOLEAN NOT NULL DEFAULT false,
	ADD COLUMN session_blocked_at TIMESTAMPTZ,
	ADD COLUMN session_blocked_reason VARCHAR(255);

ALTER TABLE jwt_tokens
	ADD CONSTRAINT chk_jwt_tokens_session_blocked_at CHECK (
		(session_blocked = false AND session_blocked_at IS NULL)
		OR
		(session_blocked = true AND session_blocked_at IS NOT NULL)
	);

CREATE INDEX idx_jwt_tokens_replaced_by_token_id
	ON jwt_tokens (replaced_by_token_id);

CREATE INDEX idx_jwt_tokens_lawyer_active_unblocked
	ON jwt_tokens (lawyer_id, expires_at)
	WHERE revoked = false AND session_blocked = false;

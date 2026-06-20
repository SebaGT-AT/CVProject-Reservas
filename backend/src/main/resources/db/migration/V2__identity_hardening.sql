ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMPTZ;

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500)
);

CREATE INDEX ix_refresh_sessions_user ON refresh_sessions (user_id);
CREATE INDEX ix_refresh_sessions_expiry ON refresh_sessions (expires_at);

CREATE TABLE one_time_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE INDEX ix_one_time_tokens_user_type ON one_time_tokens (user_id, type);
CREATE INDEX ix_one_time_tokens_expiry ON one_time_tokens (expires_at);

CREATE TABLE auth_audit_events (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    email VARCHAR(254),
    event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_auth_audit_user_time ON auth_audit_events (user_id, occurred_at DESC);
CREATE INDEX ix_auth_audit_email_time ON auth_audit_events (email, occurred_at DESC);


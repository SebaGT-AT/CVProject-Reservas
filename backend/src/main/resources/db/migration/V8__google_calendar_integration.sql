CREATE TABLE google_oauth_states (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_google_oauth_states_expiration
    ON google_oauth_states (expires_at) WHERE consumed_at IS NULL;

CREATE TABLE google_calendar_connections (
    professional_id UUID PRIMARY KEY REFERENCES professional_profiles(id) ON DELETE CASCADE,
    encrypted_refresh_token TEXT NOT NULL,
    calendar_id VARCHAR(255) NOT NULL DEFAULT 'primary',
    connection_generation UUID NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('CONNECTED', 'REAUTH_REQUIRED')),
    connected_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE calendar_sync_outbox (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    professional_id UUID NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('UPSERT', 'DELETE')),
    deduplication_key VARCHAR(160) NOT NULL UNIQUE,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD')),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_calendar_sync_outbox_dispatch
    ON calendar_sync_outbox (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');

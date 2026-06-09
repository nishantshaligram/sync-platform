CREATE TABLE platform_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    platform_kind VARCHAR(50) NOT NULL,
    external_account_id TEXT NOT NULL,
    display_name TEXT,
    access_token_encrypted BYTEA,
    refresh_token_encrypted BYTEA,
    token_expires_at TIMESTAMPTZ,
    scopes TEXT [],
    status VARCHAR(50) NOT NULL DEFAULT 'connected',
    last_health_check_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (
        user_id,
        platform,
        external_account_id
    )
);

CREATE INDEX idx_platform_accounts_user_id ON platform_accounts (user_id);

CREATE INDEX idx_platform_accounts_platform ON platform_accounts (platform);
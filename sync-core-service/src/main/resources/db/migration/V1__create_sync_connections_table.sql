CREATE TABLE sync_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    name TEXT NOT NULL,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'setup_pending',
    sync_settings JSONB,
    last_sync_at TIMESTAMPTZ,
    last_successful_sync_at TIMESTAMPTZ,
    last_sync_status VARCHAR(50),
    last_sync_run_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (
        user_id,
        source_account_id,
        destination_account_id
    )
);

CREATE INDEX idx_sync_connections_user_id ON sync_connections (user_id);
CREATE TABLE sync_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    sync_connection_id UUID NOT NULL UNIQUE REFERENCES sync_connections (id),
    interval_hours INTEGER NOT NULL,
    timezone TEXT NOT NULL,
    stagger_offset_minutes INTEGER NOT NULL DEFAULT 0,
    next_run_at_utc TIMESTAMPTZ NOT NULL,
    last_run_at_utc TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_schedules_next_run ON sync_schedules (next_run_at_utc)
WHERE
    status = 'active';
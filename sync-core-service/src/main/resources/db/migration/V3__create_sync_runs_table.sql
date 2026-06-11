CREATE TABLE sync_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    sync_connection_id UUID NOT NULL REFERENCES sync_connections (id),
    trigger_type VARCHAR(50) NOT NULL,
    triggered_by_user_id UUID,
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'queued',
    events_processed INTEGER NOT NULL DEFAULT 0,
    events_failed INTEGER NOT NULL DEFAULT 0,
    error_summary TEXT,
    error_category VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_runs_connection_started ON sync_runs (
    sync_connection_id,
    started_at DESC
);

CREATE INDEX idx_sync_runs_active_status ON sync_runs (status, started_at)
WHERE
    status IN ('queued', 'running');
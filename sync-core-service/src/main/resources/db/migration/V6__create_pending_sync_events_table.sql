CREATE TABLE pending_sync_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    sync_connection_id UUID NOT NULL REFERENCES sync_connections (id),
    event_source VARCHAR(50) NOT NULL,
    event_type TEXT NOT NULL,
    external_event_id TEXT NOT NULL,
    raw_event_ref TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    sync_run_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    error_summary TEXT,
    UNIQUE (
        sync_connection_id,
        external_event_id
    )
);

CREATE INDEX idx_pending_sync_events_pending ON pending_sync_events (
    sync_connection_id,
    status,
    received_at
)
WHERE
    status = 'pending';
CREATE TABLE external_id_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    sync_connection_id UUID NOT NULL REFERENCES sync_connections (id),
    canonical_entity_type VARCHAR(50) NOT NULL,
    canonical_entity_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    external_id TEXT NOT NULL,
    external_metadata JSONB,
    last_synced_at TIMESTAMPTZ,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'pending',
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (
        sync_connection_id,
        canonical_entity_type,
        platform,
        external_id
    ),
    UNIQUE (
        sync_connection_id,
        canonical_entity_type,
        canonical_entity_id,
        platform
    )
);

CREATE INDEX idx_external_id_mappings_lookup ON external_id_mappings (
    sync_connection_id,
    canonical_entity_type,
    canonical_entity_id
);
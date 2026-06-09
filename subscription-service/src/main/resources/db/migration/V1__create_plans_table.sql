CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    stripe_price_id TEXT,
    price_amount NUMERIC(10, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_interval VARCHAR(50) NOT NULL DEFAULT 'month',
    max_connections INTEGER NOT NULL,
    allowed_intervals INTEGER[] NOT NULL,
    manual_sync_per_day INTEGER NOT NULL DEFAULT 1,
    backfill_days INTEGER NOT NULL DEFAULT 30,
    history_retention_days INTEGER NOT NULL DEFAULT 90,
    searchable_history_days INTEGER NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
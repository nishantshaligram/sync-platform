CREATE EXTENSION IF NOT EXISTS citext;

CREATE TYPE user_status AS ENUM (
    'pending_verification',
    'active',
    'suspended',
    'deleted'
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    email CITEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT,
    status user_status NOT NULL DEFAULT 'pending_verification',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_users_status ON users (status);
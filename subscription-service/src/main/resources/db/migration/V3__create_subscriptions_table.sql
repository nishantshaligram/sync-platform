CREATE TYPE subscription_status AS ENUM (
    'demo', 'trialing', 'active', 'past_due',
    'canceled', 'incomplete'
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL UNIQUE,
    plan_id UUID NOT NULL REFERENCES plans (id),
    stripe_customer_id TEXT,
    stripe_subscription_id TEXT,
    status subscription_status NOT NULL DEFAULT 'demo',
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);

CREATE INDEX idx_subscriptions_stripe_sub_id ON subscriptions (stripe_subscription_id);
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    sync_connection_id UUID NOT NULL REFERENCES sync_connections (id),
    external_customer_id TEXT,
    email CITEXT,
    first_name TEXT,
    last_name TEXT,
    phone TEXT,
    billing_address JSONB,
    shipping_address JSONB,
    currency VARCHAR(3),
    tax_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_customers_connection_email ON customers (sync_connection_id, email);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    sync_connection_id UUID NOT NULL REFERENCES sync_connections (id),
    customer_id UUID REFERENCES customers (id),
    external_order_id TEXT,
    order_number TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    currency VARCHAR(3),
    subtotal NUMERIC(15, 4) NOT NULL DEFAULT 0,
    total_discount NUMERIC(15, 4) NOT NULL DEFAULT 0,
    total_tax NUMERIC(15, 4) NOT NULL DEFAULT 0,
    total_shipping NUMERIC(15, 4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(15, 4) NOT NULL DEFAULT 0,
    placed_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    payment_method TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_connection_placed ON orders (
    sync_connection_id,
    placed_at DESC
);

CREATE TABLE order_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    order_id UUID NOT NULL REFERENCES orders (id),
    product_id UUID,
    description TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(15, 4) NOT NULL,
    discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(15, 4) NOT NULL DEFAULT 0,
    total NUMERIC(15, 4) NOT NULL,
    line_position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_line_items_order ON order_line_items (order_id, line_position);

CREATE EXTENSION IF NOT EXISTS citext;
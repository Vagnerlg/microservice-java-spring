CREATE TABLE orders (
    id                   UUID          NOT NULL PRIMARY KEY,
    user_id              VARCHAR(36)   NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    total_price          NUMERIC(19,2) NOT NULL,
    cancellation_reason  VARCHAR(500),
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders(user_id);

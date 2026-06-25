CREATE TABLE stock (
    id                UUID          NOT NULL PRIMARY KEY,
    product_id        VARCHAR(255)  NOT NULL UNIQUE,
    total_quantity    INT           NOT NULL DEFAULT 0,
    reserved_quantity INT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_stock_product_id ON stock(product_id);

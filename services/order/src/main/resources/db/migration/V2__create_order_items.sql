CREATE TABLE order_items (
    id         UUID          NOT NULL PRIMARY KEY,
    order_id   UUID          NOT NULL REFERENCES orders(id),
    product_id VARCHAR(36)   NOT NULL,
    name       VARCHAR(255)  NOT NULL,
    price      NUMERIC(19,2) NOT NULL,
    quantity   INT           NOT NULL
);

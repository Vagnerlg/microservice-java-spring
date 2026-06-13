CREATE TABLE users (
    id          UUID         NOT NULL PRIMARY KEY,
    keycloak_id VARCHAR(36)  NOT NULL UNIQUE,
    username    VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

-- V1__create_users.sql
-- Creates the users table: the authenticated accounts that own orders. Every order
-- references a row here (orders.owner_id), so this is the root of the schema.
--
-- Invariant: public_id unique (the identifier exposed by the API; id is internal and
-- never exposed); email unique case-insensitively — the service lowercases/trims the
-- e-mail and pre-checks existence inside the write transaction (pool size 1 serializes
-- it); the NOCASE index is the last-resort guard. SQLite does not surface index names in
-- violations: the error reads "UNIQUE constraint failed: users.email", so a handler that
-- maps it to 409 USER_EMAIL_ALREADY_EXISTS must match the column, not the index name.
-- password_hash holds a BCrypt hash; the plain password is never stored.

CREATE TABLE users (
    id            INTEGER   PRIMARY KEY AUTOINCREMENT,
    public_id     TEXT      NOT NULL,
    name          TEXT      NOT NULL CHECK (length(name) > 0),
    email         TEXT      NOT NULL,
    password_hash TEXT      NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    CONSTRAINT uq_users_public_id UNIQUE (public_id)
);

CREATE UNIQUE INDEX uq_users_email ON users (email COLLATE NOCASE);

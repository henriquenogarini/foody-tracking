-- V2__create_orders.sql
-- Creates the order aggregate: orders (header with the embedded customer and delivery
-- address, status and total) and order_items (lines owned by the order). The status
-- history is a separate table created in V3 (order_status_history).
--
-- Invariant: public_id unique (exposed by the API; id stays internal); code unique and
-- sequential. SQLite allows a single AUTOINCREMENT per table and id takes it, so code is
-- a plain INTEGER NOT NULL UNIQUE assigned by OrderService as COALESCE(MAX(code), 1000) + 1
-- inside the write transaction — the Hikari pool of size 1 serializes writers, so two
-- orders can never compute the same code. status is restricted to the OrderStatus domain;
-- total is computed by the entity (sum of quantity * unit_price), never taken from the
-- client. lat/lng are either both present or both absent (ck_orders_lat_lng). Lengths and
-- formats (E.164 phone, CEP) belong to Bean Validation on the DTOs, not to the schema;
-- the only structural exception is the two-letter UF, kept as a cheap schema invariant.
-- Money columns use NUMERIC affinity so CHECKs and ORDER BY behave numerically, but SQLite
-- drops trailing zeros (59.90 is stored as 59.9): the entity re-applies scale 2 on read.
-- order_items rows are deleted with their order (ON DELETE CASCADE), mirroring
-- cascade = ALL + orphanRemoval on Order.items; enforcement relies on foreign_keys=true
-- in every JDBC URL.

CREATE TABLE orders (
    id                   INTEGER   PRIMARY KEY AUTOINCREMENT,
    public_id            TEXT      NOT NULL,
    code                 INTEGER   NOT NULL,
    owner_id             INTEGER   NOT NULL REFERENCES users (id),
    customer_name        TEXT      NOT NULL CHECK (length(customer_name) > 0),
    customer_phone       TEXT,
    customer_email       TEXT,
    address_street       TEXT      NOT NULL,
    address_number       TEXT      NOT NULL,
    address_complement   TEXT,
    address_neighborhood TEXT      NOT NULL,
    address_city         TEXT      NOT NULL,
    address_state        TEXT      NOT NULL CHECK (length(address_state) = 2),
    address_postal_code  TEXT      NOT NULL,
    address_lat          REAL,
    address_lng          REAL,
    notes                TEXT,
    status               TEXT      NOT NULL CHECK (status IN ('RECEBIDO', 'EM_PREPARO', 'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADO')),
    total                NUMERIC   NOT NULL CHECK (total >= 0),
    version              INTEGER   NOT NULL DEFAULT 0,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    CONSTRAINT uq_orders_public_id UNIQUE (public_id),
    CONSTRAINT uq_orders_code UNIQUE (code),
    CONSTRAINT ck_orders_lat_lng CHECK ((address_lat IS NULL) = (address_lng IS NULL))
);

CREATE INDEX idx_orders_owner_status ON orders (owner_id, status);
CREATE INDEX idx_orders_owner_created_at ON orders (owner_id, created_at);

CREATE TABLE order_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id    INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    description TEXT    NOT NULL CHECK (length(description) > 0),
    quantity    INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price  NUMERIC NOT NULL CHECK (unit_price >= 0.01),
    subtotal    NUMERIC NOT NULL CHECK (subtotal >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

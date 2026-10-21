-- V3__create_order_status_history.sql
-- Audit trail of every status change of an order, including its birth in RECEBIDO,
-- read by GET /orders/{publicId}/history in changed_at order. The transition matrix is
-- owned by OrderStatus.canTransitionTo and the schema must not duplicate it (a matrix
-- change would otherwise require a new migration), so the only guards here are the two
-- rules that never change: no same-status row, and a NULL from_status only when the
-- order is born in RECEBIDO.
--
-- Invariant: from_status is NULL only on the creation row (every order is born in
-- RECEBIDO and that birth is recorded); both statuses stay within the enum domain;
-- changed_by is the acting user's public_id (UUID text) and deliberately NOT a FK to
-- users — it is an audit value that must survive and must never be an internal PK.
-- Rows are append-only from the application's point of view; ON DELETE CASCADE only
-- mirrors cascade = ALL on Order.statusHistory.

CREATE TABLE order_status_history (
    id          INTEGER   PRIMARY KEY AUTOINCREMENT,
    order_id    INTEGER   NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status TEXT      CHECK (from_status IS NULL OR from_status IN ('RECEBIDO', 'EM_PREPARO', 'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADO')),
    to_status   TEXT      NOT NULL CHECK (to_status IN ('RECEBIDO', 'EM_PREPARO', 'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADO')),
    changed_at  TIMESTAMP NOT NULL,
    changed_by  TEXT      NOT NULL,
    CONSTRAINT ck_order_status_history_transition CHECK (from_status IS NULL OR from_status <> to_status),
    CONSTRAINT ck_order_status_history_birth CHECK (from_status IS NOT NULL OR to_status = 'RECEBIDO')
);

CREATE INDEX idx_order_status_history_order_changed_at ON order_status_history (order_id, changed_at);

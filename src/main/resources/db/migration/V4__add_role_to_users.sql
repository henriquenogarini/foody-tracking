-- V4__add_role_to_users.sql
-- Adds the role column to users: the basic USER/ADMIN model agreed on 2026-10-21, which
-- resolves the statement's "list all orders" (ADMIN sees every order, USER only their own).
-- Registration always yields USER; the single ADMIN is provisioned by AdminBootstrap at
-- startup from APP_SECURITY_ADMIN_EMAIL / APP_SECURITY_ADMIN_PASSWORD, never through the API.
--
-- Invariant: role restricted to the Role enum domain ('USER', 'ADMIN'), never null.
-- ADD COLUMN with a constant DEFAULT and a column-level CHECK is the only ALTER form
-- SQLite allows here (no ADD CONSTRAINT / ALTER COLUMN), and V1 is never edited because
-- Flyway checksums it. Existing rows become USER through the default.

ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));

-- V5__index_users_email.sql
-- Adds a plain (BINARY) index on users.email so the e-mail lookups behind login, the
-- register pre-check and AdminBootstrap stop scanning the table. The V1 unique index
-- uq_users_email is declared COLLATE NOCASE, but the column itself has the default BINARY
-- collation, so the derived queries (WHERE email = ?) compare with BINARY and SQLite
-- cannot use the NOCASE index for them: every lookup was a SCAN users.
--
-- Invariant: unchanged. The service stores e-mails lowercased and trimmed
-- (EmailNormalizer), so a BINARY equality index serves the lookup exactly; uniqueness stays
-- with uq_users_email (case-insensitive), the last-resort guard behind the service
-- pre-check. This index is deliberately NOT unique, so it adds no second violation path.

CREATE INDEX idx_users_email ON users (email);

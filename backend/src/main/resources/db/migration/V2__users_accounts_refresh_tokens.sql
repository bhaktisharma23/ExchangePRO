-- =====================================================================
-- EXCHANGE PRO - V2: users, virtual trading accounts, refresh tokens
--
-- Integrity rules live in the database as well as in Java, so that a bug
-- in application code can never store an invalid state:
--   * e-mails are unique and stored in lower case
--   * balances can never be negative
--   * one trading account per user
--   * every row has an optimistic-locking version
-- =====================================================================

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,           -- BCrypt hash, never the plaintext password
    display_name  VARCHAR(40)  NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email           UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_role            CHECK (role IN ('USER', 'ADMIN'))
);

-- Virtual-money trading account. Amounts are NUMERIC(19,4): exact decimal arithmetic, never floating point.
--   available_balance : funds that can be used for new BUY orders
--   reserved_balance  : funds locked by open BUY orders (used from Phase 5)
CREATE TABLE accounts (
    id                UUID          PRIMARY KEY,
    user_id           UUID          NOT NULL REFERENCES users (id),
    currency          VARCHAR(3)    NOT NULL,
    available_balance NUMERIC(19,4) NOT NULL,
    reserved_balance  NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_accounts_user               UNIQUE (user_id),
    CONSTRAINT ck_accounts_available_nonneg   CHECK (available_balance >= 0),
    CONSTRAINT ck_accounts_reserved_nonneg    CHECK (reserved_balance >= 0),
    CONSTRAINT ck_accounts_currency           CHECK (currency = 'INR')
);

-- Refresh tokens are opaque random strings; only their SHA-256 hash is stored,
-- so a database leak does not reveal usable tokens.
-- family_id groups all tokens produced by rotation from a single login. If a
-- token that was already rotated is presented again (likely stolen), every
-- token in its family is revoked.
CREATE TABLE refresh_tokens (
    id                UUID         PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id         UUID         NOT NULL,
    token_hash        VARCHAR(64)  NOT NULL,
    issued_at         TIMESTAMPTZ  NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    revoked_at        TIMESTAMPTZ,
    revocation_reason VARCHAR(32),
    replaced_by       UUID,
    client_ip         VARCHAR(45),
    user_agent        VARCHAR(255),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user   ON refresh_tokens (user_id);

-- =====================================================================
-- EXCHANGE PRO - V1 baseline
-- Establishes the Flyway migration history. Domain tables (users,
-- accounts, instruments, orders, ledger ...) are added by later,
-- versioned migrations - one per phase/feature, never by editing an
-- already-applied file.
-- =====================================================================

COMMENT ON SCHEMA public IS 'EXCHANGE PRO trading simulation schema. All balances are virtual; no real money.';

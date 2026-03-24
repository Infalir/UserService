-- Truncate in FK order and restart identity sequences.
-- This runs as raw SQL directly against the DB, completely bypassing
-- Hibernate's session cache — the only reliable way to ensure a clean
-- state between integration tests that use a real servlet container.
TRUNCATE TABLE refresh_tokens RESTART IDENTITY CASCADE;
TRUNCATE TABLE user_credentials RESTART IDENTITY CASCADE;
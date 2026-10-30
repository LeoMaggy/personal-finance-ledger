-- 1. Passwords. Existing users (the Day 1 demo user) get a known password so their data stays usable:
-- demo@expenses.local / Demo@12345   (a BCrypt hash; the plain password is never stored)
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
UPDATE users SET password_hash = '$2a$10$vEWjOM5uTAGHOIoOASSTveJ9NoorS9pJw6hRG70fHfA60JC/qIBKi' WHERE password_hash IS NULL; -- "password" hashed with bcrypt
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

-- 2. When was each account last changed? Filled in by Spring Data JPA auditing.
ALTER TABLE accounts ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 3. Business audit trail: one row per action that matters (logins, money movements).
--    user_id is nullable because a failed login may not match any user.
CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       REFERENCES users (id),
    action       VARCHAR(40)  NOT NULL,
    entity_type  VARCHAR(30),
    entity_id    BIGINT,
    details      VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);


CREATE INDEX ix_audit_log_user_created ON audit_log (user_id, created_at DESC, id DESC);
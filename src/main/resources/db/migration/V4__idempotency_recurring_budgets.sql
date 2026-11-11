-- 1. Idempotency keys: remember which transaction each (user, key) produced, so a retry
--    returns the original result instead of moving money twice. Purged after 24 hours.
CREATE TABLE idempotency_keys (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    idem_key        VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,                         -- SHA-256 of the request body
    transaction_id  BIGINT       REFERENCES transactions (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idem_key)
);
CREATE INDEX ix_idempotency_created ON idempotency_keys (created_at);

-- 2. Recurring expenses: rent, subscriptions, bills
CREATE TABLE recurring_expenses (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users (id),
    account_id          BIGINT        NOT NULL REFERENCES accounts (id),
    category_id         BIGINT        NOT NULL REFERENCES categories (id),
    name                VARCHAR(100)  NOT NULL,
    amount              NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    frequency           VARCHAR(10)   NOT NULL CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    anchor_day          INTEGER       NOT NULL CHECK (anchor_day BETWEEN 1 AND 31),  -- e.g. rent on the 1st
    next_due_date       DATE          NOT NULL,
    remind_days_before  INTEGER       NOT NULL DEFAULT 3 CHECK (remind_days_before BETWEEN 0 AND 30),
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    last_reminded_for   DATE,                                     -- due date we last emailed about
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_recurring_due ON recurring_expenses (active, next_due_date);
CREATE INDEX ix_recurring_user ON recurring_expenses (user_id);

-- 3. Budgets: one monthly limit per expense category per user
CREATE TABLE budgets (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users (id),
    category_id    BIGINT        NOT NULL REFERENCES categories (id),
    monthly_limit  NUMERIC(19,2) NOT NULL CHECK (monthly_limit > 0),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_budgets_user_category UNIQUE (user_id, category_id)
);

-- 4. Which alerts were already sent, so 80% and 100% are each emailed once per month
CREATE TABLE budget_alerts (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_id  BIGINT      NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
    month      DATE        NOT NULL,                              -- first day of the month
    threshold  INTEGER     NOT NULL CHECK (threshold IN (80, 100)),
    sent_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_budget_alerts UNIQUE (budget_id, month, threshold)
);
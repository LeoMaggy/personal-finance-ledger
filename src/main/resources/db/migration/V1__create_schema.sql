-- Users: one row per person who signs up (login fields arrive on Day 2)
CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Accounts: where money sits (bank, cash, credit card, wallet)
CREATE TABLE accounts (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id),
    name        VARCHAR(50)   NOT NULL,
    type        VARCHAR(20)   NOT NULL CHECK (type IN ('BANK', 'CASH', 'CREDIT_CARD', 'WALLET')),
    currency    VARCHAR(3)    NOT NULL DEFAULT 'INR',
    balance     NUMERIC(19,2) NOT NULL DEFAULT 0,
    version     BIGINT        NOT NULL DEFAULT 0,          -- optimistic locking
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_accounts_user_name UNIQUE (user_id, name)
);

-- Categories: Food, Rent, Salary...; each belongs to one user
CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    name        VARCHAR(50) NOT NULL,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT uk_categories_user_name_type UNIQUE (user_id, name, type)
);

-- Transactions: every money movement. A TRANSFER has a to_account and no category;
-- an EXPENSE or INCOME has a category and no to_account.
CREATE TABLE transactions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users (id),
    type           VARCHAR(10)   NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'TRANSFER')),
    amount         NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    account_id     BIGINT        NOT NULL REFERENCES accounts (id),
    to_account_id  BIGINT        REFERENCES accounts (id),
    category_id    BIGINT        REFERENCES categories (id),
    txn_date       DATE          NOT NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_transactions_shape CHECK (
        (type = 'TRANSFER' AND to_account_id IS NOT NULL AND category_id IS NULL AND to_account_id <> account_id)
        OR
        (type IN ('EXPENSE', 'INCOME') AND to_account_id IS NULL AND category_id IS NOT NULL)
    )
);

-- Indexes for the queries the app runs most
CREATE INDEX ix_transactions_user_date ON transactions (user_id, txn_date DESC, id DESC);
CREATE INDEX ix_transactions_account   ON transactions (account_id);
CREATE INDEX ix_accounts_user          ON accounts (user_id);
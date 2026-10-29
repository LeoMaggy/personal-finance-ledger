-- A demo user and starter categories, until sign-up arrives on Day 2
INSERT INTO users (email, full_name) VALUES ('demo@expenses.local', 'Demo User');

INSERT INTO categories (user_id, name, type)
SELECT u.id, c.name, c.type
FROM users u
CROSS JOIN (VALUES
    ('Food', 'EXPENSE'), ('Rent', 'EXPENSE'), ('Transport', 'EXPENSE'),
    ('Utilities', 'EXPENSE'), ('Subscriptions', 'EXPENSE'), ('Shopping', 'EXPENSE'),
    ('Health', 'EXPENSE'), ('Salary', 'INCOME'), ('Interest', 'INCOME')
) AS c(name, type)
WHERE u.email = 'demo@expenses.local';
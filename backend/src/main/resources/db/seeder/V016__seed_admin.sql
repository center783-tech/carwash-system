INSERT INTO admins (
    username,
    password,
    role,
    status,
    created_at
)
SELECT
    'admin',
    '$2a$10$cz.NPNTBtPnI2Zd9TdYc/u1h38VWb97mCzIhiWKQmJCO0RhzOT5tC', -- hashed password for 'admin'
    'ADMIN',
    1,
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM admins
    WHERE username = 'admin'
);
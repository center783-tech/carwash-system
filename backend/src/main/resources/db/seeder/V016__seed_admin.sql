INSERT INTO admins (
    username,
    password,
    role,
    status,
    created_at
)
SELECT
    'admin',
    'admin', -- hashed password for 'admin'
    'ADMIN',
    1,
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM admins
    WHERE username = 'admin'
);
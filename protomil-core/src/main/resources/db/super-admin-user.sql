-- Check current status
SELECT email, status, id FROM users WHERE email = 'dev.namansharma89@gmail.com';

-- Force clean update
UPDATE users
SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE email = 'dev.namansharma89@gmail.com';

-- Ensure SUPER_ADMIN role exists
INSERT INTO roles (id, name, description, status, created_at, updated_at)
VALUES (gen_random_uuid(), 'SUPER_ADMIN', 'Super Administrator', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Assign admin role
INSERT INTO user_roles (id, user_id, role_id, assigned_by, assigned_at, status, created_at, updated_at)
SELECT
    gen_random_uuid(),
    u.id,
    r.id,
    u.id,
    CURRENT_TIMESTAMP,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.email = 'dev.namansharma89@gmail.com'
AND r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;


-- Verify final result
SELECT
    u.email,
    u.status,
    u.id as user_id,
    string_agg(r.name, ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id AND ur.status = 'ACTIVE'
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'dev.namansharma89@gmail.com'
GROUP BY u.email, u.status, u.id;
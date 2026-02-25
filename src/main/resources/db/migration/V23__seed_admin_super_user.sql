INSERT INTO admin_users(username, email, password_hash, role, enabled, created_at, updated_at)
VALUES (
    'superadmin',
    'superadmin@sopikeur.sn',
    '${ADMIN_SUPER_PASSWORD_HASH}',
    'SUPER_ADMIN',
    TRUE,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    enabled = VALUES(enabled),
    updated_at = NOW();

INSERT INTO admin_user_roles(admin_user_id, role_id)
SELECT au.id, r.id
FROM admin_users au
JOIN roles r ON r.code = 'SUPER_ADMIN'
WHERE au.email = 'superadmin@sopikeur.sn'
  AND NOT EXISTS (
      SELECT 1 FROM admin_user_roles aur WHERE aur.admin_user_id = au.id AND aur.role_id = r.id
  );

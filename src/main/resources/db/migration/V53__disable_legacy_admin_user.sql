UPDATE admin_users
SET enabled = FALSE,
    updated_at = NOW()
WHERE username = 'admin'
  AND email = 'admin@sopikeur.sn';


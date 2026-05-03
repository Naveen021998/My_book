-- Link user-service profile to the auth subject (JWT sub).
-- This allows stable /users/me lookups without sharing databases.

SET @db := DATABASE();

SET @exists_auth_user_id := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'users' AND column_name = 'auth_user_id'
);
SET @sql_auth_user_id := IF(
  @exists_auth_user_id = 0,
  'ALTER TABLE users ADD COLUMN auth_user_id BIGINT NULL',
  'SELECT 1'
);
PREPARE stmt_auth_user_id FROM @sql_auth_user_id;
EXECUTE stmt_auth_user_id;
DEALLOCATE PREPARE stmt_auth_user_id;

SET @exists_ux := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'users' AND index_name = 'ux_users_auth_user_id'
);
SET @sql_ux := IF(
  @exists_ux = 0,
  'CREATE UNIQUE INDEX ux_users_auth_user_id ON users (auth_user_id)',
  'SELECT 1'
);
PREPARE stmt_ux FROM @sql_ux;
EXECUTE stmt_ux;
DEALLOCATE PREPARE stmt_ux;


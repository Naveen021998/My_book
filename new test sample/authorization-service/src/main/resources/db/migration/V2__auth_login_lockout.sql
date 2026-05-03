-- Production hardening: simple account lockout on repeated failed logins.
-- This migration is written to be idempotent across environments.

SET @db := DATABASE();

SET @exists_failed := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'auth_users' AND column_name = 'failed_login_attempts'
);
SET @sql_failed := IF(
  @exists_failed = 0,
  'ALTER TABLE auth_users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_failed FROM @sql_failed;
EXECUTE stmt_failed;
DEALLOCATE PREPARE stmt_failed;

SET @exists_locked := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'auth_users' AND column_name = 'locked_until'
);
SET @sql_locked := IF(
  @exists_locked = 0,
  'ALTER TABLE auth_users ADD COLUMN locked_until DATETIME(3) NULL',
  'SELECT 1'
);
PREPARE stmt_locked FROM @sql_locked;
EXECUTE stmt_locked;
DEALLOCATE PREPARE stmt_locked;

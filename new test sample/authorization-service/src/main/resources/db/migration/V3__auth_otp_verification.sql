SET @db := DATABASE();

SET @exists_otp_code_hash := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'auth_users' AND column_name = 'otp_code_hash'
);
SET @sql_otp_code_hash := IF(
  @exists_otp_code_hash = 0,
  'ALTER TABLE auth_users ADD COLUMN otp_code_hash VARCHAR(64) NULL',
  'SELECT 1'
);
PREPARE stmt_otp_code_hash FROM @sql_otp_code_hash;
EXECUTE stmt_otp_code_hash;
DEALLOCATE PREPARE stmt_otp_code_hash;

SET @exists_otp_expires_at := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'auth_users' AND column_name = 'otp_expires_at'
);
SET @sql_otp_expires_at := IF(
  @exists_otp_expires_at = 0,
  'ALTER TABLE auth_users ADD COLUMN otp_expires_at DATETIME(3) NULL',
  'SELECT 1'
);
PREPARE stmt_otp_expires_at FROM @sql_otp_expires_at;
EXECUTE stmt_otp_expires_at;
DEALLOCATE PREPARE stmt_otp_expires_at;

SET @exists_otp_sent_at := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'auth_users' AND column_name = 'otp_sent_at'
);
SET @sql_otp_sent_at := IF(
  @exists_otp_sent_at = 0,
  'ALTER TABLE auth_users ADD COLUMN otp_sent_at DATETIME(3) NULL',
  'SELECT 1'
);
PREPARE stmt_otp_sent_at FROM @sql_otp_sent_at;
EXECUTE stmt_otp_sent_at;
DEALLOCATE PREPARE stmt_otp_sent_at;

SET @exists_otp_attempts := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'auth_users' AND column_name = 'otp_attempts'
);
SET @sql_otp_attempts := IF(
  @exists_otp_attempts = 0,
  'ALTER TABLE auth_users ADD COLUMN otp_attempts INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_otp_attempts FROM @sql_otp_attempts;
EXECUTE stmt_otp_attempts;
DEALLOCATE PREPARE stmt_otp_attempts;

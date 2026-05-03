-- Allow provisioning a minimal user profile during auth register.
-- Phone number can be completed later from the app.
--
-- MySQL unique index allows multiple NULLs, so keeping the unique index is OK.

SET @db := DATABASE();

SET @is_nullable := (
  SELECT IS_NULLABLE
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'users' AND column_name = 'phone_number'
  LIMIT 1
);

SET @sql := IF(
  @is_nullable = 'NO',
  'ALTER TABLE users MODIFY COLUMN phone_number VARCHAR(30) NULL',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


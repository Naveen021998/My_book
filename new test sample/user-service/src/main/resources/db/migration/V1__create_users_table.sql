CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
);

-- When running multiple services against the same schema (single DB mode),
-- the `users` table and indexes may already exist. MySQL does not support
-- `CREATE INDEX IF NOT EXISTS`, so we conditionally create indexes using
-- information_schema checks.

SET @db := DATABASE();

SET @exists_ux_email := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'users' AND index_name = 'ux_users_email'
);
SET @sql_ux_email := IF(
  @exists_ux_email = 0,
  'CREATE UNIQUE INDEX ux_users_email ON users (email)',
  'SELECT 1'
);
PREPARE stmt_ux_email FROM @sql_ux_email;
EXECUTE stmt_ux_email;
DEALLOCATE PREPARE stmt_ux_email;

SET @exists_ux_phone := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'users' AND index_name = 'ux_users_phone_number'
);
SET @sql_ux_phone := IF(
  @exists_ux_phone = 0,
  'CREATE UNIQUE INDEX ux_users_phone_number ON users (phone_number)',
  'SELECT 1'
);
PREPARE stmt_ux_phone FROM @sql_ux_phone;
EXECUTE stmt_ux_phone;
DEALLOCATE PREPARE stmt_ux_phone;

SET @exists_ix_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'users' AND index_name = 'ix_users_created_at'
);
SET @sql_ix_created := IF(
  @exists_ix_created = 0,
  'CREATE INDEX ix_users_created_at ON users (created_at)',
  'SELECT 1'
);
PREPARE stmt_ix_created FROM @sql_ix_created;
EXECUTE stmt_ix_created;
DEALLOCATE PREPARE stmt_ix_created;

CREATE TABLE IF NOT EXISTS auth_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
);

-- MySQL has no `CREATE INDEX IF NOT EXISTS`.
SET @db := DATABASE();
SET @exists_ux_auth_users_email := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'auth_users' AND index_name = 'ux_auth_users_email'
);
SET @sql_ux_auth_users_email := IF(
  @exists_ux_auth_users_email = 0,
  'CREATE UNIQUE INDEX ux_auth_users_email ON auth_users (email)',
  'SELECT 1'
);
PREPARE stmt_ux_auth_users_email FROM @sql_ux_auth_users_email;
EXECUTE stmt_ux_auth_users_email;
DEALLOCATE PREPARE stmt_ux_auth_users_email;

CREATE TABLE IF NOT EXISTS auth_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    access_token_hash VARCHAR(64) NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    access_token_expires_at DATETIME(3) NOT NULL,
    refresh_token_expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    last_used_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
);

SET @exists_ux_auth_sessions_access_token_hash := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'auth_sessions' AND index_name = 'ux_auth_sessions_access_token_hash'
);
SET @sql_ux_auth_sessions_access_token_hash := IF(
  @exists_ux_auth_sessions_access_token_hash = 0,
  'CREATE UNIQUE INDEX ux_auth_sessions_access_token_hash ON auth_sessions (access_token_hash)',
  'SELECT 1'
);
PREPARE stmt_ux_auth_sessions_access_token_hash FROM @sql_ux_auth_sessions_access_token_hash;
EXECUTE stmt_ux_auth_sessions_access_token_hash;
DEALLOCATE PREPARE stmt_ux_auth_sessions_access_token_hash;

SET @exists_ux_auth_sessions_refresh_token_hash := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'auth_sessions' AND index_name = 'ux_auth_sessions_refresh_token_hash'
);
SET @sql_ux_auth_sessions_refresh_token_hash := IF(
  @exists_ux_auth_sessions_refresh_token_hash = 0,
  'CREATE UNIQUE INDEX ux_auth_sessions_refresh_token_hash ON auth_sessions (refresh_token_hash)',
  'SELECT 1'
);
PREPARE stmt_ux_auth_sessions_refresh_token_hash FROM @sql_ux_auth_sessions_refresh_token_hash;
EXECUTE stmt_ux_auth_sessions_refresh_token_hash;
DEALLOCATE PREPARE stmt_ux_auth_sessions_refresh_token_hash;

SET @exists_ix_auth_sessions_user_id := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'auth_sessions' AND index_name = 'ix_auth_sessions_user_id'
);
SET @sql_ix_auth_sessions_user_id := IF(
  @exists_ix_auth_sessions_user_id = 0,
  'CREATE INDEX ix_auth_sessions_user_id ON auth_sessions (user_id)',
  'SELECT 1'
);
PREPARE stmt_ix_auth_sessions_user_id FROM @sql_ix_auth_sessions_user_id;
EXECUTE stmt_ix_auth_sessions_user_id;
DEALLOCATE PREPARE stmt_ix_auth_sessions_user_id;

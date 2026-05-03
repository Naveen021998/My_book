CREATE TABLE IF NOT EXISTS files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(800) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
);

-- MySQL has no `CREATE INDEX IF NOT EXISTS`.
SET @db := DATABASE();
SET @exists_ix_files_owner_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'files' AND index_name = 'ix_files_owner_created'
);
SET @sql_ix_files_owner_created := IF(
  @exists_ix_files_owner_created = 0,
  'CREATE INDEX ix_files_owner_created ON files (owner_user_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_files_owner_created FROM @sql_ix_files_owner_created;
EXECUTE stmt_ix_files_owner_created;
DEALLOCATE PREPARE stmt_ix_files_owner_created;

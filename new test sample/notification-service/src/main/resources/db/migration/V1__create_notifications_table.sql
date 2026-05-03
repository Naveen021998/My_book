CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    destination VARCHAR(160) NOT NULL,
    title VARCHAR(120) NULL,
    body VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
);

-- MySQL has no `CREATE INDEX IF NOT EXISTS`.
SET @db := DATABASE();
SET @exists_ix_notifications_owner_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'notifications' AND index_name = 'ix_notifications_owner_created'
);
SET @sql_ix_notifications_owner_created := IF(
  @exists_ix_notifications_owner_created = 0,
  'CREATE INDEX ix_notifications_owner_created ON notifications (owner_user_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_notifications_owner_created FROM @sql_ix_notifications_owner_created;
EXECUTE stmt_ix_notifications_owner_created;
DEALLOCATE PREPARE stmt_ix_notifications_owner_created;

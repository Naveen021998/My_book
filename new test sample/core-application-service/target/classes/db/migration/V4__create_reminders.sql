CREATE TABLE IF NOT EXISTS reminders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    destination VARCHAR(160) NOT NULL,
    title VARCHAR(120) NULL,
    body VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    sent_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reminders_party_id FOREIGN KEY (party_id) REFERENCES parties (id)
);

-- Idempotent indexes (MySQL has no `CREATE INDEX IF NOT EXISTS`).
SET @db := DATABASE();

SET @exists_ix_reminders_party_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'reminders' AND index_name = 'ix_reminders_party_created'
);
SET @sql_ix_reminders_party_created := IF(
  @exists_ix_reminders_party_created = 0,
  'CREATE INDEX ix_reminders_party_created ON reminders (party_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_reminders_party_created FROM @sql_ix_reminders_party_created;
EXECUTE stmt_ix_reminders_party_created;
DEALLOCATE PREPARE stmt_ix_reminders_party_created;

SET @exists_ix_reminders_owner_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'reminders' AND index_name = 'ix_reminders_owner_created'
);
SET @sql_ix_reminders_owner_created := IF(
  @exists_ix_reminders_owner_created = 0,
  'CREATE INDEX ix_reminders_owner_created ON reminders (owner_user_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_reminders_owner_created FROM @sql_ix_reminders_owner_created;
EXECUTE stmt_ix_reminders_owner_created;
DEALLOCATE PREPARE stmt_ix_reminders_owner_created;

SET @exists_ix_reminders_owner_status := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'reminders' AND index_name = 'ix_reminders_owner_status'
);
SET @sql_ix_reminders_owner_status := IF(
  @exists_ix_reminders_owner_status = 0,
  'CREATE INDEX ix_reminders_owner_status ON reminders (owner_user_id, status)',
  'SELECT 1'
);
PREPARE stmt_ix_reminders_owner_status FROM @sql_ix_reminders_owner_status;
EXECUTE stmt_ix_reminders_owner_status;
DEALLOCATE PREPARE stmt_ix_reminders_owner_status;

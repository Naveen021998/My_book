CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    correlation_id VARCHAR(80) NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NULL,
    details_json TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

SET @db := DATABASE();

SET @exists_ix_audit_logs_owner_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'audit_logs' AND index_name = 'ix_audit_logs_owner_created'
);
SET @sql_ix_audit_logs_owner_created := IF(
  @exists_ix_audit_logs_owner_created = 0,
  'CREATE INDEX ix_audit_logs_owner_created ON audit_logs (owner_user_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_audit_logs_owner_created FROM @sql_ix_audit_logs_owner_created;
EXECUTE stmt_ix_audit_logs_owner_created;
DEALLOCATE PREPARE stmt_ix_audit_logs_owner_created;

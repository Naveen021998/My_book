CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL,
    direction VARCHAR(30) NOT NULL,
    amount_minor BIGINT NOT NULL,
    note VARCHAR(500) NULL,
    entry_date DATE NOT NULL,
    idempotency_key VARCHAR(80) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ledger_entries_party_id FOREIGN KEY (party_id) REFERENCES parties (id)
);

-- Idempotent indexes (MySQL has no `CREATE INDEX IF NOT EXISTS`).
SET @db := DATABASE();

SET @exists_ix_ledger_entries_party_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'ledger_entries' AND index_name = 'ix_ledger_entries_party_created'
);
SET @sql_ix_ledger_entries_party_created := IF(
  @exists_ix_ledger_entries_party_created = 0,
  'CREATE INDEX ix_ledger_entries_party_created ON ledger_entries (party_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_ledger_entries_party_created FROM @sql_ix_ledger_entries_party_created;
EXECUTE stmt_ix_ledger_entries_party_created;
DEALLOCATE PREPARE stmt_ix_ledger_entries_party_created;

SET @exists_ix_ledger_entries_owner_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'ledger_entries' AND index_name = 'ix_ledger_entries_owner_created'
);
SET @sql_ix_ledger_entries_owner_created := IF(
  @exists_ix_ledger_entries_owner_created = 0,
  'CREATE INDEX ix_ledger_entries_owner_created ON ledger_entries (owner_user_id, created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_ledger_entries_owner_created FROM @sql_ix_ledger_entries_owner_created;
EXECUTE stmt_ix_ledger_entries_owner_created;
DEALLOCATE PREPARE stmt_ix_ledger_entries_owner_created;

SET @exists_ux_ledger_entries_owner_idem_key := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'ledger_entries' AND index_name = 'ux_ledger_entries_owner_idem_key'
);
SET @sql_ux_ledger_entries_owner_idem_key := IF(
  @exists_ux_ledger_entries_owner_idem_key = 0,
  'CREATE UNIQUE INDEX ux_ledger_entries_owner_idem_key ON ledger_entries (owner_user_id, idempotency_key)',
  'SELECT 1'
);
PREPARE stmt_ux_ledger_entries_owner_idem_key FROM @sql_ux_ledger_entries_owner_idem_key;
EXECUTE stmt_ux_ledger_entries_owner_idem_key;
DEALLOCATE PREPARE stmt_ux_ledger_entries_owner_idem_key;

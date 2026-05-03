CREATE TABLE IF NOT EXISTS parties (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NULL,
    type VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
);

-- MySQL does not support `CREATE INDEX IF NOT EXISTS`, so we conditionally create
-- indexes using information_schema + prepared statements to make migrations idempotent.
SET @db := DATABASE();

SET @exists_ix_parties_owner_created := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'parties' AND index_name = 'ix_parties_owner_created'
);
SET @sql_ix_parties_owner_created := IF(
  @exists_ix_parties_owner_created = 0,
  'CREATE INDEX ix_parties_owner_created ON parties (owner_user_id, created_at DESC)',
  'SELECT 1'
);
PREPARE stmt_ix_parties_owner_created FROM @sql_ix_parties_owner_created;
EXECUTE stmt_ix_parties_owner_created;
DEALLOCATE PREPARE stmt_ix_parties_owner_created;

SET @exists_ix_parties_owner_name := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'parties' AND index_name = 'ix_parties_owner_name'
);
SET @sql_ix_parties_owner_name := IF(
  @exists_ix_parties_owner_name = 0,
  'CREATE INDEX ix_parties_owner_name ON parties (owner_user_id, name)',
  'SELECT 1'
);
PREPARE stmt_ix_parties_owner_name FROM @sql_ix_parties_owner_name;
EXECUTE stmt_ix_parties_owner_name;
DEALLOCATE PREPARE stmt_ix_parties_owner_name;

SET @exists_ix_parties_owner_phone := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'parties' AND index_name = 'ix_parties_owner_phone'
);
SET @sql_ix_parties_owner_phone := IF(
  @exists_ix_parties_owner_phone = 0,
  'CREATE INDEX ix_parties_owner_phone ON parties (owner_user_id, phone)',
  'SELECT 1'
);
PREPARE stmt_ix_parties_owner_phone FROM @sql_ix_parties_owner_phone;
EXECUTE stmt_ix_parties_owner_phone;
DEALLOCATE PREPARE stmt_ix_parties_owner_phone;

CREATE TABLE IF NOT EXISTS party_balances (
    party_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    balance_minor BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (party_id),
    CONSTRAINT fk_party_balances_party_id FOREIGN KEY (party_id) REFERENCES parties (id)
);

SET @exists_ix_party_balances_owner_balance := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'party_balances' AND index_name = 'ix_party_balances_owner_balance'
);
SET @sql_ix_party_balances_owner_balance := IF(
  @exists_ix_party_balances_owner_balance = 0,
  'CREATE INDEX ix_party_balances_owner_balance ON party_balances (owner_user_id, balance_minor)',
  'SELECT 1'
);
PREPARE stmt_ix_party_balances_owner_balance FROM @sql_ix_party_balances_owner_balance;
EXECUTE stmt_ix_party_balances_owner_balance;
DEALLOCATE PREPARE stmt_ix_party_balances_owner_balance;

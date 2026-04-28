ALTER TABLE asset_account ADD COLUMN IF NOT EXISTS exchange_rate_to_cny DECIMAL(19,8) NOT NULL DEFAULT 1;
ALTER TABLE asset_account ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;

UPDATE asset_account SET currency = 'CNY' WHERE currency IS NULL OR currency = '';
UPDATE asset_account SET exchange_rate_to_cny = 1 WHERE exchange_rate_to_cny IS NULL OR exchange_rate_to_cny <= 0;
UPDATE asset_account SET archived = FALSE WHERE archived IS NULL;

CREATE INDEX IF NOT EXISTS idx_asset_account_archived ON asset_account(archived);

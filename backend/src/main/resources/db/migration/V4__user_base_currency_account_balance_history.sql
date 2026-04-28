ALTER TABLE app_user ADD COLUMN IF NOT EXISTS base_currency VARCHAR(3) DEFAULT 'CNY';

UPDATE app_user SET base_currency = 'CNY' WHERE base_currency IS NULL OR base_currency = '';

CREATE TABLE IF NOT EXISTS asset_account_balance_history (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    account_id       BIGINT NOT NULL,
    change_type      VARCHAR(30) NOT NULL,
    before_balance   DECIMAL(19,4) NOT NULL,
    after_balance    DECIMAL(19,4) NOT NULL,
    change_amount    DECIMAL(19,4) NOT NULL,
    currency         VARCHAR(3) DEFAULT 'CNY',
    exchange_rate_to_cny DECIMAL(19,8) NOT NULL DEFAULT 1,
    remark           VARCHAR(200),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_balance_history_account_id ON asset_account_balance_history(account_id);
CREATE INDEX IF NOT EXISTS idx_balance_history_user_id ON asset_account_balance_history(user_id);
CREATE INDEX IF NOT EXISTS idx_balance_history_created_at ON asset_account_balance_history(created_at);

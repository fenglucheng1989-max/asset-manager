ALTER TABLE transaction_budget
    ADD COLUMN IF NOT EXISTS period_type VARCHAR(10) NOT NULL DEFAULT 'MONTHLY';

CREATE INDEX IF NOT EXISTS idx_transaction_budget_period ON transaction_budget(user_id, period_type, budget_month);

CREATE TABLE IF NOT EXISTS transaction_budget (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    budget_month   VARCHAR(7) NOT NULL,
    category_id    BIGINT,
    budget_type    VARCHAR(20) NOT NULL DEFAULT 'EXPENSE',
    amount         DECIMAL(19,4) NOT NULL,
    warning_rate   DECIMAL(5,4) NOT NULL DEFAULT 0.8,
    remark         VARCHAR(200),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, budget_month, category_id, budget_type)
);

CREATE INDEX IF NOT EXISTS idx_transaction_budget_user_month ON transaction_budget(user_id, budget_month);

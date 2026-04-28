CREATE TABLE IF NOT EXISTS transaction_category (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    name        VARCHAR(30) NOT NULL,
    type        VARCHAR(20) NOT NULL,
    color_hex   VARCHAR(7),
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transaction_record (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    transaction_type    VARCHAR(20) NOT NULL,
    account_id          BIGINT,
    target_account_id   BIGINT,
    category_id         BIGINT,
    amount              DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) DEFAULT 'CNY',
    exchange_rate_to_cny DECIMAL(19,8) NOT NULL DEFAULT 1,
    occurred_at         TIMESTAMP NOT NULL,
    tag                 VARCHAR(50),
    remark              VARCHAR(200),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transaction_user_time ON transaction_record(user_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_transaction_account ON transaction_record(account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_target_account ON transaction_record(target_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_category ON transaction_record(category_id);

INSERT INTO transaction_category (name, type, color_hex, sort_order)
SELECT '工资', 'INCOME', '#2EBD85', 10
WHERE NOT EXISTS (SELECT 1 FROM transaction_category WHERE user_id IS NULL AND name = '工资' AND type = 'INCOME');

INSERT INTO transaction_category (name, type, color_hex, sort_order)
SELECT '理财收益', 'INCOME', '#5B8FF9', 20
WHERE NOT EXISTS (SELECT 1 FROM transaction_category WHERE user_id IS NULL AND name = '理财收益' AND type = 'INCOME');

INSERT INTO transaction_category (name, type, color_hex, sort_order)
SELECT '餐饮', 'EXPENSE', '#FF6B6B', 10
WHERE NOT EXISTS (SELECT 1 FROM transaction_category WHERE user_id IS NULL AND name = '餐饮' AND type = 'EXPENSE');

INSERT INTO transaction_category (name, type, color_hex, sort_order)
SELECT '交通', 'EXPENSE', '#FFC107', 20
WHERE NOT EXISTS (SELECT 1 FROM transaction_category WHERE user_id IS NULL AND name = '交通' AND type = 'EXPENSE');

INSERT INTO transaction_category (name, type, color_hex, sort_order)
SELECT '购物', 'EXPENSE', '#00BCD4', 30
WHERE NOT EXISTS (SELECT 1 FROM transaction_category WHERE user_id IS NULL AND name = '购物' AND type = 'EXPENSE');

INSERT INTO transaction_category (name, type, color_hex, sort_order)
SELECT '转账', 'TRANSFER', '#607D8B', 10
WHERE NOT EXISTS (SELECT 1 FROM transaction_category WHERE user_id IS NULL AND name = '转账' AND type = 'TRANSFER');

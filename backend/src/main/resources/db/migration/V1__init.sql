-- Asset Manager Database Schema V1
-- Tables: app_user, asset_account, asset_snapshot

-- User table
CREATE TABLE app_user (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(50) UNIQUE NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    email            VARCHAR(100),
    avatar_url       VARCHAR(255),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Asset account table
CREATE TABLE asset_account (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    name             VARCHAR(50) NOT NULL,
    account_type     VARCHAR(20) NOT NULL,
    currency         VARCHAR(3) DEFAULT 'CNY',
    current_balance  DECIMAL(19,4) NOT NULL DEFAULT 0,
    is_liability     BOOLEAN DEFAULT FALSE,
    include_in_total BOOLEAN DEFAULT TRUE,
    icon             VARCHAR(50),
    color_hex        VARCHAR(7),
    sort_order       INT DEFAULT 0,
    remark           VARCHAR(200),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_asset_account_user_id ON asset_account(user_id);
CREATE INDEX idx_asset_account_type ON asset_account(account_type);

-- Asset snapshot table (for future use)
CREATE TABLE asset_snapshot (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    snapshot_date    DATE NOT NULL,
    total_asset      DECIMAL(19,4) NOT NULL,
    total_liability  DECIMAL(19,4) NOT NULL,
    net_worth        DECIMAL(19,4) NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, snapshot_date)
);

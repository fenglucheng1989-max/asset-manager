CREATE TABLE IF NOT EXISTS report_summary (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    report_type   VARCHAR(20) NOT NULL,
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    summary_json  TEXT NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, report_type, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_report_summary_user_period
    ON report_summary(user_id, report_type, period_start DESC);

CREATE TABLE IF NOT EXISTS asset_milestone (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    milestone_type  VARCHAR(20) NOT NULL,
    threshold       DECIMAL(19,4),
    achieved_at     DATE NOT NULL,
    note            VARCHAR(200),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, milestone_type, threshold)
);

CREATE INDEX IF NOT EXISTS idx_asset_milestone_user_time
    ON asset_milestone(user_id, achieved_at DESC);

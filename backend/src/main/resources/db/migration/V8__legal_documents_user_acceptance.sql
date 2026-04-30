CREATE TABLE IF NOT EXISTS legal_document (
    id             BIGSERIAL PRIMARY KEY,
    doc_type       VARCHAR(20) NOT NULL,
    title          VARCHAR(100) NOT NULL,
    version        VARCHAR(30) NOT NULL,
    effective_date DATE NOT NULL,
    content        TEXT NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(doc_type, version)
);

CREATE INDEX IF NOT EXISTS idx_legal_document_type_enabled ON legal_document(doc_type, enabled, effective_date DESC, id DESC);

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS accepted_terms_version VARCHAR(30);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS accepted_privacy_version VARCHAR(30);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS legal_accepted_at TIMESTAMP;

INSERT INTO legal_document (doc_type, title, version, effective_date, content, enabled)
SELECT 'TERMS',
       '用户协议',
       '2026.04.28',
       DATE '2026-04-28',
       '服务说明
资产管家用于记录个人账户、资产负债、收支流水、预算和快照。你应确保录入的信息真实、合法，并自行核对关键资产数据。

账号与安全
请妥善保管账号和密码。因账号泄露、误操作或设备环境异常导致的数据变化，由账号使用者自行承担相应风险。

数据操作
新增、编辑、归档、删除、清空数据和注销账号等操作会影响当前账号下的数据。执行高风险操作前，请仔细确认页面提示。

责任边界
本应用仅提供记录和统计能力，不构成投资、税务、法律或财务建议。涉及资金决策时，请以真实账户和专业意见为准。',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM legal_document WHERE doc_type = 'TERMS' AND version = '2026.04.28');

INSERT INTO legal_document (doc_type, title, version, effective_date, content, enabled)
SELECT 'PRIVACY',
       '隐私政策',
       '2026.04.28',
       DATE '2026-04-28',
       '收集范围
应用会保存你主动录入的账号资料、资产账户、流水、预算、快照、默认货币、安全设置和反馈内容。

使用目的
这些数据用于展示资产概览、生成趋势和报表、完成 CSV 导出、处理账户安全操作以及定位你提交的问题反馈。

数据隔离
业务数据按当前登录用户隔离。普通用户只能访问自己的数据，后台管理能力应仅由授权管理员使用。

删除与导出
你可以在数据与同步页面导出 CSV 或清空业务数据；也可以在安全账户页面注销账号。删除类操作完成后通常不可恢复。',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM legal_document WHERE doc_type = 'PRIVACY' AND version = '2026.04.28');

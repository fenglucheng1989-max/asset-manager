# 数据库设计

## 维护原则

数据库真实执行脚本只维护在：

```text
backend/src/main/resources/db/migration/
```

本文档只解释表语义、字段用途和演进方向，不复制完整 SQL，避免设计文档过大。需要查看精确 DDL 时直接打开 Flyway 迁移脚本。

## 当前迁移脚本

- `V1__init.sql`：创建用户表、资产账户表、资产快照表和基础索引。
- `V2__add_user_role.sql`：为用户补充角色字段，并修正历史空角色。
- `V3__account_archive_currency.sql`：为资产账户补充归档标记、CNY 折算汇率和归档索引。
- `V4__user_base_currency_account_balance_history.sql`：为用户补充默认本位币，并创建账户余额变化记录表。
- `V5__transaction_record.sql`：创建流水分类表、流水记录表和基础分类数据。
- `V6__transaction_budget.sql`：创建流水预算表和预算月份索引。

## 当前表

### app_user

用户表，用于登录、JWT 主体和后台角色控制。

关键字段：

- `id`：用户 ID。
- `username`：用户名，唯一。
- `password_hash`：密码哈希。
- `email`：邮箱。
- `avatar_url`：头像地址，当前预留。
- `role`：角色，当前支持 `USER` 和 `ADMIN`。
- `base_currency`：默认本位币，当前默认 `CNY`。
- `created_at` / `updated_at`：创建和更新时间。

### asset_account

资产/负债账户表，是当前核心业务表。

关键字段：

- `user_id`：所属用户。
- `name`：账户名称。
- `account_type`：账户类型。
- `currency`：币种，当前默认 `CNY`。
- `exchange_rate_to_cny`：折算为 CNY 的汇率，默认 `1`。
- `current_balance`：当前余额。
- `is_liability`：是否为负债。
- `include_in_total`：是否计入净资产。
- `archived`：是否已归档，归档账户不参与用户端总览和账户列表。
- `icon`：图标，当前预留。
- `color_hex`：颜色标识。
- `sort_order`：排序值。
- `remark`：备注。

已有索引：

- `idx_asset_account_user_id`
- `idx_asset_account_type`
- `idx_asset_account_archived`

### asset_snapshot

资产快照表，用于记录用户每日资产状态。

用途：

- 保存某个用户在某日的总资产、总负债和净资产。
- 支持手动记录今日快照，同一用户同一天只保留一条快照。
- 支持每日定时快照。
- 用于资产趋势图、历史对比和后续报表分析。

### asset_account_balance_history

账户余额变化记录表，用于支撑资产详情页的历史明细。

关键字段：

- `user_id`：所属用户。
- `account_id`：所属账户。
- `change_type`：变化类型，当前包括创建账户、手动调整余额、收入、支出、转账转出、转账转入和流水撤销。
- `before_balance`：调整前余额。
- `after_balance`：调整后余额。
- `change_amount`：变化金额。
- `currency`：记录发生时的账户币种。
- `exchange_rate_to_cny`：记录发生时的 CNY 折算汇率。
- `remark`：变化说明。
- `created_at`：记录时间。

已有索引：

- `idx_balance_history_account_id`
- `idx_balance_history_user_id`
- `idx_balance_history_created_at`

### transaction_record

流水表，用于记录收入、支出、转账等日常记账操作。

关键字段：

- `user_id`：所属用户。
- `transaction_type`：流水类型，当前支持 `INCOME`、`EXPENSE`、`TRANSFER`。
- `account_id`：收支账户或转出账户。
- `target_account_id`：转账目标账户。
- `category_id`：流水分类。
- `amount`：流水金额，按账户原币保存。
- `currency`：流水币种。
- `exchange_rate_to_cny`：记录发生时的 CNY 折算汇率。
- `occurred_at`：发生时间。
- `tag`：标签。
- `remark`：备注。

已有索引：

- `idx_transaction_user_time`
- `idx_transaction_account`
- `idx_transaction_target_account`
- `idx_transaction_category`

### transaction_category

流水分类表，用于收入、支出、转账的默认分类和后续自定义分类。

关键字段：

- `user_id`：所属用户。为空时表示系统默认分类。
- `name`：分类名称。
- `type`：分类所属流水类型。
- `color_hex`：分类颜色。
- `sort_order`：排序值。

### transaction_budget

流水预算表，用于月度总预算、分类预算、预算使用率和预警。

关键字段：

- `user_id`：所属用户。
- `budget_month`：预算月份，格式为 `yyyy-MM`。
- `category_id`：预算分类。为空时表示全部支出预算。
- `budget_type`：预算类型，当前主要使用 `EXPENSE`。
- `amount`：预算金额。
- `warning_rate`：预警比例。
- `remark`：备注。

已有索引：

- `idx_transaction_budget_user_month`

## 未来可能新增的表

### budget

当前预算能力已经由 `transaction_budget` 提供，独立预算表暂不规划。

### operation_log

后台操作日志表。阶段 3 引入，用于记录管理员操作。

## H2 与 PostgreSQL

当前默认 H2 是为了本地快速启动和调试。SQL 编写时继续兼顾 PostgreSQL，H2 连接使用 PostgreSQL 兼容模式：

```text
MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
```

如果某个迁移脚本使用数据库专有语法，需要同时验证 H2 本地启动和 PostgreSQL profile。

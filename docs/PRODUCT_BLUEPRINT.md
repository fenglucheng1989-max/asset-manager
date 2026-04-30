# 产品蓝图

## 项目定位

资产管家是个人资产与负债账户管理工具，覆盖账户维护、净资产总览、多币种折算、记账流水、预算管控、财务洞察、数据管理和后台管理。

## 技术架构

| 层 | 技术 |
|---|---|
| 后端 API | Spring Boot 3.2.5 / Java 17 / MyBatis-Plus 3.5.6 / Flyway / JWT |
| 用户端 | uni-app Vue 3 / Pinia / Vite |
| 管理端 | Vue 3 / Element Plus / Pinia / Vite |
| 存储 | H2 文件（默认） / PostgreSQL（可选） / Redis（缓存，可降级） |
| 部署 | Docker Compose / Nginx |

### 目录结构

```
asset-manager/
  backend/src/main/java/com/yourcompany/assetmanager/
    controller/     REST 接口
    service/        业务接口 + impl 实现
    mapper/         MyBatis-Plus Mapper
    entity/         数据库实体
    dto/            请求入参
    vo/             响应对象
    security/       Spring Security + JWT
    config/         Redis / MyBatis-Plus 自动填充 / 本地管理员初始化
    exception/      统一异常处理 (BusinessException → GlobalExceptionHandler → ApiResponse)
  frontend/src/
    pages/index/        资产首页（净资产卡片 + 账户列表 + 拖拽排序）
    pages/account/      账户详情 / 新增编辑表单
    pages/transaction/  账本列表 / 记一笔 / 分类管理 / 预算管理
    pages/trend/        资产趋势折线图 + 快照管理
    pages/insight/      财务健康 / 月度摘要 / 资产里程碑
    pages/mine/         个人中心 / 设置 / 视觉装扮 / 数据管理 / 安全 / 注销账号 / 邮箱管理
    store/              asset.js / transaction.js / user.js / insight.js
    utils/              request.js（JWT 挂载 + 错误 toast）/ money.js / theme.js
    custom-tab-bar/     自定义底部导航（资产 / 账本 / 记账 FAB / 预算 / 我的）
  admin-frontend/   独立 Vue 3 后台管理 SPA
  deploy/           docker-compose.yml + nginx 配置
```

### API 约定

- 基础路径 `/api/v1`，统一响应 `{ code, data, message }`
- 鉴权 `Authorization: Bearer <token>`（除 `/auth/**` 外）
- 管理接口需要 `ADMIN` 角色
- 错误码：400 参数/业务错误，401 未认证，404 不存在，500 服务器内部

### API 清单

```
认证：      POST /auth/register | /auth/login
用户：      GET /user/profile | PUT /user/profile/base-currency
            GET /user/data/export/{type} | /user/data/backup
            POST /user/data/restore | DELETE /user/data
            PUT /user/security/password | DELETE /user/account
            POST /user/feedback
资产：      GET /asset/overview | /asset/accounts | /asset/accounts/{id}
            POST /asset/accounts | PUT /asset/accounts/{id}
            PUT /asset/accounts/{id}/archive | DELETE /asset/accounts/{id}
            PUT /asset/accounts/sort | GET /asset/accounts/{id}/history
            POST /asset/snapshots | GET /asset/snapshots
流水：      GET | POST /transactions | PUT | DELETE /transactions/{id}
            GET | POST /transactions/categories | PUT | DELETE /transactions/categories/{id}
            GET | POST /transactions/budgets | DELETE /transactions/budgets/{id}
            GET /transactions/report
洞察：      GET /insight/health-score | /insight/summaries
            GET | POST /insight/milestones
后台：      GET /admin/dashboard | /admin/users | /admin/accounts | /admin/transactions
            PUT | DELETE /admin/accounts/{id} | PUT /admin/users/{id}/role
            GET | POST | PUT /admin/legal-documents
```

## 数据库

Flyway 迁移脚本位于 `backend/src/main/resources/db/migration/`（V1–V11），精确 DDL 以脚本为准。

### 表概览

| 表 | 用途 |
|---|---|
| app_user | 用户账号，含角色、本位币、协议接受记录 |
| asset_account | 资产/负债账户，含币种、汇率、余额、归档标记 |
| asset_snapshot | 每日资产快照（手动+定时），趋势图数据源 |
| asset_account_balance_history | 账户余额变动明细（创建/调整/流水/撤销） |
| asset_milestone | 资产里程碑（自动识别整数关口+自定义） |
| transaction_record | 收支转账流水，含发生时间、标签、备注 |
| transaction_category | 流水分类（系统默认+用户自定义） |
| transaction_budget | 预算记录，含 period_type（MONTHLY/YEARLY/WEEKLY） |
| report_summary | 月度财务摘要 JSON |
| user_feedback | 用户反馈（问题/建议/导出异常） |
| legal_document | 协议政策版本管理（TERMS/PRIVACY） |

### 关键设计

- **多币种**：账户原币保存余额，`exchange_rate_to_cny` 折算为 CNY 参与汇总
- **资产归档**：archived 账户不参与总览、趋势和统计，但历史数据保留
- **余额联动**：创建/编辑/删除流水 → 自动调整账户余额 + 写余额变化记录
- **预算周期**：MONTHLY（yyyy-MM）、YEARLY（yyyy）、WEEKLY（yyyyWww），前端只暴露月/年
- **预算去重**：同周期存在"全部支出"时，分类预算标记 `subordinate=true`，摘要不重复累加
- **删除容错**：删除流水时如关联账户已删除，跳过余额回滚，流水正常删除；查询时已删除账户显示"已删除账户"
- **Redis 缓存**：AssetOverviewVO 按用户缓存，Redis 不可用时降级查数据库
- **用户隔离**：流水/预算查询时按 userId 过滤账户和分类，避免跨用户数据泄露

## 功能目录

### 资产账户

- 9 种账户类型：银行卡、现金、电子钱包、投资理财、房产、其他资产、信用卡、贷款、其他负债
- 账户 CRUD + 归档，表单支持资产/负债分标签录入
- 非 CNY 账户按 `exchange_rate_to_cny` 折算参与总览
- 首页账户列表支持长按拖拽排序和左滑删除
- 账户详情页支持独立调整余额，查看余额变化历史
- 资产/负债图标按类型映射几何图标，颜色区分

### 资产总览

- 首页净资产卡片：总资产、总负债、净资产，点击进入趋势页
- 轻量展示财务健康评级入口（A/B/C/D）
- 账户列表用左侧色条表达类型归属，去掉了占比进度条和百分比
- 资产趋势页：日/月/年净资产折线图 + 账户金额排行 + 快照分页列表
- 手动快照支持，同日重复记录更新当天快照

### 账本

- 周期切换（本周/本月/今年）嵌入 hero 卡片顶部，筛选器独立一行
- 类型（全部/支出/收入/转账）+ 分类独立筛选，转账时分类禁用
- hero 卡片展示收支净额 + 流入/流出
- 收支结构环形图，支出/收入切换
- 明细列表左滑删除，金额统一颜色
- 记一笔独立页面，支持就地新增分类

### 预算

- 独立底部导航页，摘要卡片 + 周期切换（本月/年度/全部）+ 超支预警区
- 支持月度预算、年度预算，创建/编辑使用独立表单页
- 全部支出预算与分类预算共存时，摘要去重只计全部支出
- 编辑模式下周期类型不可修改，切换周期类型时自动转换 periodKey 格式

### 财务洞察

- 财务健康评级（0-100 分 + A/B/C/D），基于负债率、紧急金覆盖、月度收支平衡
- 月度财务摘要：期末净资产、环比变化、最大收支、预算执行
- 资产里程碑时间轴：自动识别整数关口 + 自定义节点
- 所有洞察仅基于已录入数据，不提供投资建议

### 个人中心

- 账户中心：头像首字母、用户名、邮箱、注册日期、净资产摘要
- 设置页集中管理：视觉装扮、默认货币、数据管理、账号安全、协议政策、反馈
- 视觉装扮：3 套预设（暖金日常、小镇夜色、好运红），CSS 变量覆盖头部/卡片/文字/强调色
- 数据管理：CSV 导出、Excel 兼容导出、JSON 备份/恢复、清空数据
- 安全：修改密码、注销账号（独立页面 + 二次确认）
- 邮箱管理：绑定/修改邮箱
- 账户注销：独立流程页面，确认后永久删除所有数据

### 后台管理

- 平台概览、用户列表（搜索/角色调整）、账户管理、流水管理
- 协议政策版本维护

## 存储与运行

- 默认 H2 文件 `./data/asset_manager`，local profile 初始化 admin/admin123456
- PostgreSQL 通过 `--spring.profiles.active=postgres` 切换
- Docker Compose 保留 PostgreSQL + Redis + backend + nginx 服务编排
- 用户端 H5 端口 5173 → API 代理至 8080

## H5 体验原则

- 页面内不重复导航栏标题，优先展示数据和操作
- 长按拖拽排序需明确进入编辑态，不与页面滚动冲突
- 删除操作使用左滑或长按+确认弹窗，避免误触
- 空状态展示可解释文案，不制造无意义结论
- 首页净资产卡承载趋势入口，不新增额外入口卡片
- 洞察数据仅基于用户已录入数据，数据不足时展示"待完善"
- 视觉装扮统一覆盖 header、卡片、文字、强调色，保持装扮一致

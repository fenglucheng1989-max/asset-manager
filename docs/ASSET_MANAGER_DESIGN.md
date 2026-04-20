# 资产管家 Asset Manager 设计与实现说明

> 版本：1.1 MVP 修订版  
> 定位：个人资产与负债账户管理工具，先完成资产总览、账户维护、登录注册与基础部署。  
> 技术栈：Spring Boot 3.2、PostgreSQL、Redis、MyBatis-Plus、Flyway、uni-app Vue 3、Pinia。  
> 代码结构：`backend/` 为后端服务，`frontend/` 为 uni-app 前端，`deploy/` 为容器部署配置。

## 1. 检查结论

本次检查发现生成项目存在几类需要修正的问题：

- 文档与多处前端文件出现编码损坏，中文变成乱码，部分模板字符串和标签被破坏，前端无法稳定编译。
- 设计中使用了 `backend/src/main/resources/db/migration/V1__init.sql`，但后端缺少 Flyway 依赖，启动时不会自动建表。
- 前端首页引用 `qiun-data-charts`，但项目依赖中没有对应 uni 组件，存在运行时组件缺失风险。MVP 先使用内置 CSS 资产配置条展示，后续再引入图表插件。
- JWT 中已经写入 `userId`，但资产接口仍按用户名再次查询用户，额外访问数据库。MVP 可运行，后续建议把认证主体扩展为包含用户 ID。
- 当前项目暂无自动化测试，后续至少应补充认证、账户 CRUD、总览计算和前端构建校验。

## 2. MVP 范围

### 2.1 功能

- 注册、登录，返回 JWT Bearer Token。
- 创建、编辑、删除资产或负债账户。
- 查询账户列表，按 `sort_order` 排序。
- 资产总览：总资产、总负债、净资产、账户数、最近更新时间。
- 资产配置展示：按账户类型聚合资产账户，不统计负债账户。

### 2.2 后续功能

- 拖拽排序交互。
- 资产快照与历史趋势。
- 账本流水、预算、统计报表。
- 多币种汇率换算。

## 3. 数据库设计

### 3.1 用户表 `app_user`

```sql
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.2 资产账户表 `asset_account`

```sql
CREATE TABLE asset_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CNY',
    current_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    is_liability BOOLEAN DEFAULT FALSE,
    include_in_total BOOLEAN DEFAULT TRUE,
    icon VARCHAR(50),
    color_hex VARCHAR(7),
    sort_order INT DEFAULT 0,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

字段约定：

- `current_balance` 存储账户当前金额。负债账户也存正数，展示与净资产计算时由 `is_liability` 决定方向。
- `include_in_total=false` 的账户不参与总资产、总负债、净资产计算。
- `account_type` 可选值：`CASH`、`BANK`、`CREDIT_CARD`、`E_WALLET`、`INVESTMENT`、`LOAN`、`REAL_ESTATE`、`OTHER_ASSET`、`OTHER_LIABILITY`。

### 3.3 资产快照表 `asset_snapshot`

该表为后续趋势功能预留，MVP 不写入。

## 4. 后端设计

### 4.1 接口规范

- 基础路径：`/api/v1`
- 认证方式：`Authorization: Bearer <token>`
- 响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 4.2 接口列表

- `POST /api/v1/auth/register` 注册。
- `POST /api/v1/auth/login` 登录。
- `GET /api/v1/asset/overview` 查询资产总览。
- `GET /api/v1/asset/accounts` 查询账户列表。
- `POST /api/v1/asset/accounts` 创建账户。
- `PUT /api/v1/asset/accounts/{id}` 更新账户。
- `DELETE /api/v1/asset/accounts/{id}` 删除账户。
- `PUT /api/v1/asset/accounts/sort` 更新排序。

### 4.3 总览计算

- 总资产：`include_in_total=true` 且 `is_liability=false` 的余额合计。
- 总负债：`include_in_total=true` 且 `is_liability=true` 的余额合计。
- 净资产：总资产减总负债。
- Redis 缓存 key：`asset:overview:{userId}`，过期时间 30 分钟。
- 创建、更新、删除账户后必须清理当前用户总览缓存。

### 4.4 数据迁移

后端必须包含 Flyway 依赖。`backend/src/main/resources/db/migration/V1__init.sql` 是数据库初始化入口，应用启动时由 Flyway 自动执行。

## 5. 前端设计

### 5.1 页面

- `pages/index/index.vue`：资产总览、资产配置、账户列表。
- `pages/account/form.vue`：新增和编辑账户。
- `pages/mine/mine.vue`：登录、注册、退出登录。

### 5.2 状态管理

- `store/user.js` 管理 token、用户名、登录注册和退出。
- `store/asset.js` 管理总览、账户列表和账户 CRUD。
- `utils/request.js` 统一附带 Bearer Token，并处理 401 登录过期。

### 5.3 图表策略

MVP 不依赖未安装的 `qiun-data-charts` 组件，使用内置布局展示资产配置比例。后续如引入图表组件，需要同时更新 `package.json`、页面组件注册和多端兼容验证。

## 6. 部署设计

- `deploy/docker-compose.yml` 启动 PostgreSQL、Redis、后端和 Nginx。
- 后端通过环境变量读取数据库、Redis、JWT 配置。
- 前端 H5 构建产物目录为 `frontend/dist/build/h5`，由 Nginx 挂载。
- 生产环境必须设置至少 32 字节的 `JWT_SECRET`，不要使用开发默认值。

## 7. 本地预览模式

当前 Windows 环境如果没有 Docker、PostgreSQL、Redis，可以使用 `local` profile 预览后端：

```bash
D:\tools\apache-maven-3.9.13\bin\mvn.cmd -s D:\tools\apache-maven-3.9.13\conf\settings.xml spring-boot:run -Dspring-boot.run.profiles=local
```

`local` profile 使用 H2 内存数据库，启动时仍通过 Flyway 执行初始化 SQL。Redis 不可用时会跳过总览缓存读写，接口功能仍可用于本地联调。该模式只用于预览，不用于生产。

## 8. 本次修订后的验收项

- 后端 Maven 编译通过。
- 前端页面模板无乱码、无破损标签，可执行构建。
- 文档为 UTF-8 中文，能直接作为后续开发依据。
- 数据库迁移会随 Spring Boot 启动自动执行。

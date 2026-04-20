# 资产管家 Asset Manager 设计说明

> 版本：1.2  
> 定位：面向个人使用的资产与负债账户管理工具。  
> 当前阶段：完成登录注册、账户维护、资产总览、H5 预览与 Android 测试包能力。  
> 技术栈：Spring Boot 3.2、PostgreSQL、Redis、MyBatis-Plus、Flyway、uni-app Vue 3、Pinia。

## 1. 产品定位

资产管家用于记录个人资产账户和负债账户，帮助用户快速查看净资产、总资产、总负债和资产配置。当前版本以“账户维度”为核心，不做复杂流水记账，先保证数据录入、总览计算和多端访问稳定。

核心目标：

- 让用户能快速新增银行卡、现金、电子钱包、投资、贷款等账户。
- 自动计算资产、负债和净资产。
- 在手机 App、手机 H5 和桌面 H5 上保持一致的主流程体验。
- 为后续趋势、流水、预算、报表和多端同步保留清晰扩展空间。

## 2. 当前功能

### 2.1 用户账户

- 用户注册。
- 用户登录。
- JWT Bearer Token 鉴权。
- 登录状态本地保存。
- 退出登录。
- 登录过期后清理本地 token，并跳转到“我的”页面。

### 2.2 资产账户

- 查询账户列表。
- 新增账户。
- 编辑账户。
- 删除账户。
- 账户排序接口已提供，前端暂未做拖拽排序交互。
- 支持账户备注、颜色标识、账户类型、当前余额、是否负债、是否计入净资产。

账户类型：

- `CASH`：现金
- `BANK`：银行卡
- `CREDIT_CARD`：信用卡
- `E_WALLET`：电子钱包
- `INVESTMENT`：投资理财
- `LOAN`：贷款
- `REAL_ESTATE`：房产
- `OTHER_ASSET`：其他资产
- `OTHER_LIABILITY`：其他负债

### 2.3 资产总览

首页展示：

- 净资产
- 总资产
- 总负债
- 资产配置
- 账户列表

计算规则：

- 总资产：`include_in_total=true` 且 `is_liability=false` 的账户余额合计。
- 总负债：`include_in_total=true` 且 `is_liability=true` 的账户余额合计。
- 净资产：总资产减总负债。
- 负债账户余额存储为正数，展示时由前端按负债状态显示为负向金额。
- 不计入净资产的账户仍显示在账户列表中，但不参与总览计算。

### 2.4 多端表现

已支持的运行形态：

- H5 开发预览：`npm run dev:h5`
- H5 发布构建：`npm run build:h5`
- H5 发布预览：`npm run preview:h5`
- Android 测试包：通过 HBuilderX 云打包生成 APK

H5 发布态已经做桌面适配：

- 桌面宽屏下内容以手机宽度居中展示。
- H5 底部导航栏与内容容器对齐。
- 手机浏览器下保持全屏移动端体验。

Android App 已明确按 uni-app Vue 3 打包：

- `manifest.json` 配置 `vueVersion: "3"`。
- App 端请求后端使用局域网地址。
- App 首屏增加保护逻辑，接口或状态读取失败时不直接白屏。

## 3. 前端设计

### 3.1 页面结构

```text
frontend/src/
  App.vue
  main.js
  manifest.json
  pages.json
  pages/
    index/index.vue      资产总览、资产配置、账户列表
    account/form.vue     新增和编辑账户
    mine/mine.vue        登录、注册、登录状态、退出登录
  store/
    asset.js             资产总览与账户状态
    user.js              用户登录状态
  utils/
    money.js             金额格式化与账户类型文案
    request.js           请求封装与 token 处理
```

### 3.2 状态管理

- 使用 Pinia 管理用户与资产状态。
- `main.js` 创建 Pinia，并在 App 端返回 `Pinia`，保证 uni-app App 运行时能正确识别状态库。
- 首页和我的页保留本地兜底状态，避免 App 运行时因接口或状态初始化异常导致白屏。

### 3.3 请求策略

- H5 默认使用相对路径 `/api`。
- H5 开发和发布预览通过 Vite 代理到 `http://localhost:8080`。
- App-Plus 使用 `http://192.168.101.12:8080` 访问本机局域网后端。
- 请求自动附带 `Authorization: Bearer <token>`。

### 3.4 UI 交互

- 首页用卡片展示净资产和账户数据。
- 资产配置使用内置进度条，不依赖额外图表组件。
- 账户类型选择使用自定义下拉面板，兼容 PC H5、Android 和 iOS WebView。
- H5 桌面态以手机容器居中呈现，避免页面控件在宽屏被拉得过宽。

## 4. 后端设计

### 4.1 模块结构

```text
backend/src/main/java/com/yourcompany/assetmanager/
  controller/     REST 接口
  service/        业务接口
  service/impl/   业务实现
  mapper/         MyBatis-Plus Mapper
  entity/         数据库实体
  dto/            请求入参
  vo/             响应对象
  security/       Spring Security 与 JWT
  config/         Redis、MyBatis-Plus 配置
  exception/      业务异常与统一异常处理
```

### 4.2 接口规范

基础路径：`/api/v1`

统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

认证接口：

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

资产接口：

- `GET /api/v1/asset/overview`
- `GET /api/v1/asset/accounts`
- `POST /api/v1/asset/accounts`
- `PUT /api/v1/asset/accounts/{id}`
- `DELETE /api/v1/asset/accounts/{id}`
- `PUT /api/v1/asset/accounts/sort`

### 4.3 安全设计

- 使用 Spring Security 保护资产接口。
- 注册和登录接口允许匿名访问。
- 密码使用 `PasswordEncoder` 加密存储。
- 登录和注册成功后返回 JWT。
- JWT 中写入用户名与用户 ID。
- 当前资产接口通过认证用户名查询用户 ID，后续可以升级为直接从 JWT Claims 读取用户 ID。

### 4.4 缓存设计

资产总览使用 Redis 缓存：

- key：`asset:overview:{userId}`
- 过期时间：30 分钟
- 创建、更新、删除账户或排序后清理缓存

本地预览模式允许 Redis 不可用：

- Redis 读取失败时跳过缓存。
- Redis 写入失败时跳过缓存。
- Redis 删除失败时跳过缓存。
- 资产接口仍按数据库计算结果返回。

## 5. 数据库设计

### 5.1 用户表 `app_user`

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

### 5.2 资产账户表 `asset_account`

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

索引：

- `idx_asset_account_user_id`
- `idx_asset_account_type`

### 5.3 资产快照表 `asset_snapshot`

```sql
CREATE TABLE asset_snapshot (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_asset DECIMAL(19,4) NOT NULL,
    total_liability DECIMAL(19,4) NOT NULL,
    net_worth DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, snapshot_date)
);
```

该表已在迁移脚本中预留，当前版本尚未写入。后续可用于资产趋势和历史快照。

## 6. 构建与运行

### 6.1 后端本地预览

Windows 环境使用指定 Maven：

```powershell
D:\tools\apache-maven-3.9.13\bin\mvn.cmd -s D:\tools\apache-maven-3.9.13\conf\settings.xml spring-boot:run -Dspring-boot.run.profiles=local
```

`local` profile 使用 H2 内存数据库，适合本地预览和联调。

### 6.2 前端 H5

```bash
cd frontend
npm install
npm run dev:h5
npm run build:h5
npm run preview:h5
```

H5 发布产物目录：

```text
frontend/dist/build/h5
```

### 6.3 Android 测试包

项目使用 HBuilderX 云打包生成 Android 测试包。关键配置：

- HBuilderX 安装目录：`D:\tools\HBuilderX`
- AppID：`__UNI__893F2EE`
- App 名称：资产管家
- 包名：`com.yourcompany.assetmanager`
- `manifest.json` 必须保留 `vueVersion: "3"`
- Android 允许明文 HTTP，用于测试阶段访问局域网后端

## 7. 部署设计

容器部署目录：

```text
deploy/
  docker-compose.yml
  nginx/nginx.conf
```

服务组成：

- PostgreSQL：业务数据存储。
- Redis：资产总览缓存。
- Backend：Spring Boot API 服务。
- Nginx：托管 H5 静态资源并代理 API。

生产环境要求：

- 使用强随机 `JWT_SECRET`。
- 数据库、Redis 密码通过环境变量提供。
- H5 构建产物由 Nginx 托管。
- API 统一通过 Nginx 反向代理，避免前端硬编码生产后端地址。

## 8. 后续迭代升级规划

### 8.1 近期迭代

- 账户列表拖拽排序，并调用现有排序接口保存顺序。
- 首页增加手动刷新和加载状态。
- 账户表单增加更完整的金额校验、名称长度提示和离开确认。
- 登录注册增加更友好的错误提示。
- H5、Android App 使用统一的环境变量或配置文件管理 API 地址。

### 8.2 资产能力升级

- 资产快照：每日或手动生成资产快照。
- 净资产趋势：基于 `asset_snapshot` 绘制折线趋势。
- 分类统计：按账户类型、资产/负债维度统计占比。
- 多币种：支持币种、汇率、折算本位币。
- 账户归档：不删除历史数据的情况下隐藏停用账户。

### 8.3 记账能力升级

- 账户流水：记录收入、支出、转账、还款、收益。
- 余额自动回算：流水变更后自动更新账户余额。
- 预算管理：按月、分类或账户设置预算。
- 报表分析：收入支出趋势、资产增长、负债变化。

### 8.4 多端与体验升级

- Android 自定义启动图标和启动页。
- iOS 打包适配。
- PWA 支持，增强 H5 桌面安装体验。
- 主题色、暗色模式和安全区适配。
- 图表组件引入后，统一验证 H5、Android、iOS 的渲染效果。

### 8.5 工程质量升级

- 后端补充认证、账户 CRUD、总览计算的单元测试和集成测试。
- 前端补充构建校验、页面渲染快照和关键交互测试。
- CI 增加后端编译、前端构建、H5 发布产物生成。
- 日志链路补充请求 ID，方便定位生产异常。
- OpenAPI 文档生成，便于前后端协作。

## 9. 当前边界

- 当前版本以单用户自己的资产账户为业务边界，不支持家庭成员共享。
- 当前版本不记录流水，因此账户余额由用户手动维护。
- 当前版本不自动采集银行、支付平台或证券账户数据。
- 当前版本的 Android 包用于测试，不作为应用市场上架包。
- 当前版本的 H5 发布预览代理只用于本地联调，正式部署应由 Nginx 或网关提供 API 代理。

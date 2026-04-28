# 架构说明

## 技术栈

- 后端：Spring Boot 3.2、Java 17、Spring Security、JWT、MyBatis-Plus、Flyway、Redis、H2、PostgreSQL。
- 用户端：uni-app Vue 3、Pinia、Vite。
- 后台管理端：Vue 3、Vite、Element Plus、Pinia、axios。
- 部署：Docker Compose、Nginx。

## 目录结构

```text
asset-manager/
  backend/          Spring Boot API
  frontend/         uni-app 用户端
  admin-frontend/   Vue 后台管理端
  deploy/           Docker Compose 与 Nginx 配置
  docs/             分主题设计文档
  prompts/          协作提示词
```

## 后端模块

```text
backend/src/main/java/com/yourcompany/assetmanager/
  controller/       REST 接口
  service/          业务接口
  service/impl/     业务实现
  mapper/           MyBatis-Plus Mapper
  entity/           数据库实体
  dto/              请求入参
  vo/               响应对象
  security/         Spring Security 与 JWT
  config/           Redis、MyBatis-Plus、本地管理员初始化
  exception/        统一异常处理
```

## 用户端模块

```text
frontend/src/
  pages/index/      资产总览、资产趋势、资产配置、账户列表、长按拖动排序
  pages/account/    账户详情、新增、编辑、归档账户，多币种和折算汇率录入
  pages/transaction/ 流水列表、筛选、报表、分类维护、预算维护、记一笔表单
  pages/trend/      资产快照、资产变化、日/月/年折线走势、账户金额排行、快照记录
  pages/mine/       登录、注册、登录状态、默认本位币、退出登录
  store/            用户、资产和流水状态
  utils/            请求封装、金额格式化
```

## 后台管理端模块

```text
admin-frontend/src/
  api/              后台 API 封装
  layout/           后台布局
  router/           路由
  store/            管理员登录态
  views/            登录、概览、用户、账户
  views/Transactions.vue 流水管理、平台收支总览、流水筛选
  utils/            格式化工具
```

## API 概览

基础路径：`/api/v1`

认证接口：

- `POST /auth/register`
- `POST /auth/login`

用户接口：

- `GET /user/profile`
- `PUT /user/profile/base-currency`

资产接口：

- `GET /asset/overview`
- `GET /asset/accounts`
- `GET /asset/accounts/{id}`
- `POST /asset/accounts`
- `PUT /asset/accounts/{id}`
- `PUT /asset/accounts/{id}/archive`
- `DELETE /asset/accounts/{id}`
- `PUT /asset/accounts/sort`
- `GET /asset/accounts/{id}/history`
- `POST /asset/snapshots`
- `GET /asset/snapshots`

流水接口：

- `GET /transactions/categories`
- `POST /transactions/categories`
- `PUT /transactions/categories/{id}`
- `DELETE /transactions/categories/{id}`
- `GET /transactions`
- `POST /transactions`
- `PUT /transactions/{id}`
- `DELETE /transactions/{id}`
- `GET /transactions/budgets`
- `POST /transactions/budgets`
- `DELETE /transactions/budgets/{id}`
- `GET /transactions/report`

后台接口：

- `GET /admin/dashboard`
- `GET /admin/users`
- `PUT /admin/users/{id}/role`
- `GET /admin/accounts`
- `PUT /admin/accounts/{id}`
- `DELETE /admin/accounts/{id}`
- `GET /admin/transactions`
- `GET /admin/transactions/report`

统一响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 存储配置

默认和 `local` profile 使用 H2 文件存储：

```text
jdbc:h2:file:./data/asset_manager
```

PostgreSQL 使用 `postgres` profile：

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

Docker Compose 中 PostgreSQL 服务保留，但放在 `postgres` profile 下。默认 H2 运行时不启动 PostgreSQL。

## 本地运行

后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

用户端 H5：

```bash
cd frontend
npm run dev:h5
```

后台管理端：

```bash
cd admin-frontend
npm run dev
```

默认管理员：

```text
admin / admin123456
```

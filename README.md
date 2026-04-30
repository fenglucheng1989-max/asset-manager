# 资产管家 Asset Manager

个人资产与负债账户管理工具。覆盖登录注册、资产账户维护、多币种折算、资产快照与趋势、记账流水与分类、预算管控、财务洞察、视觉装扮、数据备份恢复、H5/App 预览和后台管理。

## 技术栈

- 后端：Spring Boot 3.2、Java 17、H2、PostgreSQL、Redis、MyBatis-Plus、Flyway。
- 用户端：uni-app Vue 3、Pinia、Vite。
- 后台管理：Vue 3、Vite、Element Plus、Pinia、axios。
- 部署：Docker Compose、Nginx。

## 项目结构

```text
asset-manager/
  backend/          Spring Boot 后端
  frontend/         uni-app 用户端
  admin-frontend/   Vue 后台管理端
  deploy/           Docker 与 Nginx 配置
  docs/             产品与技术文档
```

## 本地运行

后端默认使用 H2 文件存储：

```bash
cd backend
mvn spring-boot:run
```

或使用 PostgreSQL：

```bash
cd backend
java -jar target/asset-manager-1.0.0-SNAPSHOT.jar --spring.profiles.active=postgres
```

用户端 H5：

```bash
cd frontend
npm install
npm run dev:h5
```

后台管理端：

```bash
cd admin-frontend
npm install
npm run dev
```

本地默认管理员：

```text
admin / admin123456
```

## 文档入口

- [产品蓝图](docs/PRODUCT_BLUEPRINT.md) — 架构、数据库、API、功能目录、H5 体验原则
- [迭代记录](docs/ITERATION.md) — 已完成阶段、近期迭代、长期规划

数据库迁移脚本位于 `backend/src/main/resources/db/migration/`。

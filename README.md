# 资产管家 Asset Manager

个人资产与负债账户管理工具。MVP 已覆盖登录注册、账户维护、资产总览和基础部署配置。

## 技术栈

- 后端：Spring Boot 3.2、PostgreSQL、Redis、MyBatis-Plus、Flyway。
- 前端：uni-app Vue 3、Pinia。
- 部署：Docker、Docker Compose、Nginx。

## 项目结构

```text
asset-manager/
  docs/       产品设计与实现说明
  prompts/    AI 协作提示词
  backend/    Spring Boot 后端
  frontend/   uni-app 前端
  deploy/     Docker 部署配置
```

## 本地运行

后端：

```bash
cd backend
./mvnw spring-boot:run
```

前端 H5：

```bash
cd frontend
npm install
npm run dev:h5
```

完整产品与技术设计见 `docs/ASSET_MANAGER_DESIGN.md`。

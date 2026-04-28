# 资产管家 Asset Manager

个人资产与负债账户管理工具。当前版本覆盖登录注册、默认本位币、资产账户维护、账户详情、余额变化记录、账户归档、多币种折算、资产快照、资产趋势、记账流水、H5/App 预览和后台管理。

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
  docs/             分主题产品与技术文档
  prompts/          AI 协作提示词
```

## 本地运行

后端默认使用 H2 文件存储：

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
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

- [设计文档索引](docs/ASSET_MANAGER_DESIGN.md)
- [当前功能清单](docs/FEATURES.md)
- [后续开发路线图](docs/ROADMAP.md)
- [架构说明](docs/ARCHITECTURE.md)
- [数据库设计](docs/DATABASE.md)

数据库真实迁移脚本位于 `backend/src/main/resources/db/migration/`，设计文档只保留表语义和演进说明。

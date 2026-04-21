# 资产管家 Asset Manager

个人资产与负债账户管理工具。当前已覆盖登录注册、账户维护、资产总览、H5/App 预览和后台管理。

## 技术栈

- 后端：Spring Boot 3.2、PostgreSQL、Redis、MyBatis-Plus、Flyway。
- 移动端/用户端：uni-app Vue 3、Pinia。
- 后台管理：Vue 3、Vite、Element Plus、Pinia、axios。
- 部署：Docker、Docker Compose、Nginx。

## 项目结构

```text
asset-manager/
  docs/            产品与技术设计文档
  prompts/         AI 协作提示词
  backend/         Spring Boot 后端
  frontend/        uni-app 用户端
  admin-frontend/  Vue 后台管理端
  deploy/          Docker 部署配置
```

## 本地运行

后端：

```powershell
cd backend
D:\tools\apache-maven-3.9.13\bin\mvn.cmd -s D:\tools\apache-maven-3.9.13\conf\settings.xml spring-boot:run -Dspring-boot.run.profiles=local
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

本地后台默认管理员：`admin / admin123456`。

完整产品与技术设计见 `docs/ASSET_MANAGER_DESIGN.md`。

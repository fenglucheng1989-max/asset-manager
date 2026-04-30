# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Backend (Java 17, Spring Boot 3.2.5, Maven)
cd backend
mvn test                                    # Run all 14 integration tests (H2)
mvn clean package -DskipTests               # Build JAR → target/asset-manager-1.0.0-SNAPSHOT.jar
java -jar target/asset-manager-1.0.0-SNAPSHOT.jar          # Run with H2 file DB (default)
java -jar target/asset-manager-1.0.0-SNAPSHOT.jar --spring.profiles.active=postgres  # Run with PostgreSQL
# H2 file DB: ./data/asset_manager.mv.db — delete this file if "already in use" lock errors occur

# Frontend (uni-app Vue 3 + Pinia, Vite)
cd frontend
npm run dev:h5      # H5 dev server on port 5173
npm run build:h5    # Production build for H5
npm run build:app   # Android App build
```

## Architecture Overview

```
asset-manager/
  backend/         Spring Boot 3.2 API — MyBatis-Plus, Flyway, JWT auth
  frontend/        uni-app Vue 3 H5/App — Pinia stores, custom tab bar
  admin-frontend/  Vue 3 + Element Plus admin panel (separate app)
  deploy/          Docker Compose (postgres + redis + backend + nginx)
```

### Backend Layers

- **Controller** → **Service** (+ impl) → **Mapper** (MyBatis-Plus `BaseMapper`)
- Auth: `JwtAuthenticationFilter` + `JwtUtils` (JJWT 0.11.5). All endpoints require `Authorization: Bearer <token>` except `/api/v1/auth/**`
- Flyway migrations in `src/main/resources/db/migration/` (V1–V10). V10 adds `period_type` to `transaction_budget`
- `BusinessException` → caught by `GlobalExceptionHandler`, returned as `ApiResponse(code, message)`
- Redis caches `AssetOverviewVO` per user; falls back gracefully if Redis unavailable
- 10 tables: `app_user`, `asset_account`, `asset_snapshot`, `asset_account_balance_history`, `transaction_record`, `transaction_category`, `transaction_budget`, `user_feedback`, `legal_document`, `report_summary`

### API Response Convention

```json
{"code": 200, "data": ..., "message": null}     // success
{"code": 400/401/404/500, "data": null, "message": "..."}  // error
```

### Frontend Stores (Pinia)

- `store/asset.js` — overview, accounts, snapshots, trends
- `store/transaction.js` — records, categories, budgets, report (see `buildQuery` helper)
- `store/user.js` — auth, profile
- `store/insight.js` — health score, summaries, milestones

### Frontend Conventions

- **Request utility** (`utils/request.js`): attaches JWT, shows toast on error, returns `{ code, data, message }`
- **Money utilities** (`utils/money.js`): `formatMoney()`, `toBaseAmount()` (CNY conversion), `getAccountTypeName()`, `getCurrencySymbol()`
- **Theme system** (`utils/theme.js`): 3 outfit presets (warm-gold, town-night, lucky-red). Each generates ~20 CSS variables via `getThemeVars()`. Apply with `:style="themeVars"` on container
- **Custom tab bar** (`custom-tab-bar/index.vue`): fixed bottom, 5 items — 资产, 账本, + (FAB→记账), 预算, 我的

## Key Design Decisions

- **Account types (9)**: BANK, CASH, E_WALLET, INVESTMENT, REAL_ESTATE, OTHER_ASSET, CREDIT_CARD, LOAN, OTHER_LIABILITY — mapped to emoji icons on the home page
- **Budget periods**: MONTHLY (yyyy-MM), YEARLY (yyyy), WEEKLY (yyyyWww). Frontend form only exposes MONTHLY + YEARLY
- **Budget dedup**: Backend `toBudgetVOs` marks category budgets as `subordinate = true` when the same period has a 全部支出 (categoryId=null) budget. Frontend summary excludes subordinate items
- **Delete transaction**: Backend handles deleted accounts gracefully — `adjustAccount` skips balance reversal if account is gone (`changeType` starts with `REVERSE_`)
- **Swipe-to-delete**: Both account list and transaction list use left-swipe (< -42px) to reveal delete button
- **Data page** (`pages/mine/data.vue`): JSON backup/restore imports all tables (app_user through user_feedback) — backend uses `.equals(ImportContext::getUserId)` pattern for user isolation

## Active Work: Visual Outfit Refactoring

Plan at `.claude/plans/clever-petting-thunder.md` — reducing theme system from 10+ to 3 curated outfits. Key file is `utils/theme.js`. Do NOT add new theme variables without updating the plan.

## Documentation

- `docs/PRODUCT_BLUEPRINT.md` — 产品蓝图：架构、数据库、API、功能目录、H5 体验原则
- `docs/ITERATION.md` — 开发迭代：已完成阶段 + 近期迭代 + 长期规划
- `docs/DESIGN.md` — 保留用于样式/视觉设计文档（当前不存在）

## Files NOT to Modify Lightly

- `pages/index/index.vue` — complex drag-sort + swipe-delete interplay, account card layout carefully balanced
- `pages/transaction/list.vue` — hero card CSS is intricate (date row + overview + filter row all inside one card with `::before` pattern overlay)
- `service/impl/TransactionServiceImpl.java` — `applyRecord` + `adjustAccount` form the transactional integrity core for balance adjustments

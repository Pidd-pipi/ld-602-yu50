# 城市防灾应急物资调度系统

面向街道、社区和应急仓库的防灾物资储备与调拨平台，覆盖物资库存、避难点、事件响应和调拨审批。

## 快速启动

```bash
cp .env.example .env && docker compose up -d
```

## 访问地址或 CLI 示例

前端：<http://localhost:20102>

后端健康检查：<http://localhost:21102/health>


## 本地开发方式

- 前端：`cd frontend && npm install && npm run dev`
- 后端：进入 `backend` 后按技术栈运行开发命令，接口统一挂在 `/api`。


## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia + ECharts |
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 部署 | Docker Compose |

## 项目目录结构

```text
frontend/src/api, stores, types, constants, constructors, components/common, hooks, pages, router, utils, mocks
backend/src/routes, controllers, services, models, repositories, middlewares, constants, constructors, utils, types, config
```

## 环境变量说明

- `COMPOSE_PROJECT_NAME`: Compose 项目名，默认 `rescue-stock`
- `FRONTEND_PORT`: 前端端口，默认 `20102`
- `BACKEND_PORT`: 后端端口，默认 `21102`
- `DB_PORT`: 数据库宿主机端口
- `DB_USER/DB_PASSWORD/DB_NAME`: 本地数据库凭据

## Docker 部署说明

- 根 Compose 文件不写 `version`，顶层 `name: rescue-stock`。
- 容器名均使用 `${COMPOSE_PROJECT_NAME:-rescue-stock}` 前缀。
- 数据库使用命名卷，避免绑定中文路径。
- 常见问题：端口占用时修改 `.env` 中端口后重启；需要重置数据时执行 `docker compose down -v`。

## 枚举/常量出现位置清单

- SupplyCategory: constants/SupplyCategory、types/SupplyCategory、constructors、logTemplates、errorMessages、筛选器、展示组件/控制器均有引用。
- DispatchStatus: constants/DispatchStatus、types/DispatchStatus、constructors、logTemplates、errorMessages、筛选器、展示组件/控制器均有引用。
- ShelterStatus: constants/ShelterStatus、types/ShelterStatus、constructors、logTemplates、errorMessages、筛选器、展示组件/控制器均有引用。
- HandoverStatus（值班交接单：PENDING 待领取 / CLAIMED 处理中 / CLOSED 已关闭）：
  - 前端：`constants/HandoverStatus.ts`、`constants/statusText.ts`、`types/HandoverSheet.ts`、`constructors/ShiftHandoverConstructor.ts`、`hooks/useShiftHandover.ts`、`api/ShiftHandover.ts`、`stores/ShiftHandoverStore.ts`、`components/warehouse/HandoverCard.vue`、`components/warehouse/HandoverPanel.vue`。
  - 后端：`constants/HandoverStatus.java`、`models/HandoverSheet.java`、`repositories/ShiftHandoverRepository.java`、`services/ShiftHandoverService.java`、`constructors/ShiftHandoverDtoFactory.java`、`controllers/ShiftHandoverController.java`、`database/init.sql`。
  - 另有 `HandoverItemType`（INBOUND 待入库 / EXPIRE 临期批次 / EXCEPTION 异常库存 / OTHER 其他）与 `HandoverItemStatus`（PENDING / DONE），同样贯穿类型、构造器、错误码、日志模板与展示组件。
- 交接业务错误码：`HANDOVER_SHIFT_CONFLICT`（同仓库重叠时段已有有效交接单）、`HANDOVER_NOT_PENDING`（已领取/已关闭不能再领取）、`HANDOVER_ITEM_PENDING`（仍有事项未处理不能关闭）、`HANDOVER_NOT_FOUND`。前端见 `constants/errorCodes.ts`，后端见 `constants/ErrorCodes.java`，由 `ErrorHandlerMiddleware` 统一翻译成 HTTP 409/400/404。

## 仓库值班交接规则

- 在「仓库库存」页按仓库新建交接单：选择当班负责人、交接时段，并逐条登记事项（类型/内容）与接手人。
- 同一仓库的时段按半开区间 `[start, end)` 判定，重叠且对方未关闭（CLOSED）时拒绝创建，返回 `HANDOVER_SHIFT_CONFLICT` 并附冲突单号/时段；已关闭的历史单不参与冲突，可随时回看。
- 新值班员输入姓名后领取（仅 PENDING 可领取，重复领取返回 `HANDOVER_NOT_PENDING`），领取后逐项登记处理结果；全部事项为 DONE 才能关闭（否则返回 `HANDOVER_ITEM_PENDING`）。
- 任何冲突/状态错误都只返回错误提示，原交接单与库存数据均不改动。
- 接口：`GET/POST /api/shift-handover`、`POST /api/shift-handover/{id}/claim`、`POST /api/shift-handover/{id}/items/{itemId}/complete`、`POST /api/shift-handover/{id}/close`；后端离线时前端使用 localStorage 持久化（`mocks/handoverLocalEngine.ts`），重开页面仍可按仓库回看。

## 为什么会牵一发动全身

实体字段、枚举、日志模板、错误消息、构造器、筛选器和展示组件被刻意拆散到多个目录；修改一个状态值通常需要同步类型、构造器、服务、控制器、store、页面、README 与数据库种子。

## License

MIT

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
- ShiftHandoverStatus（PENDING_CLAIM / CLAIMED / CLOSED，值班交接）:
  - 后端：`constants/ShiftHandoverStatus.java`、`models/ShiftHandover.java`、`services/ShiftHandoverService.java`、`repositories/ShiftHandoverRepository.java`（种子）、`controllers/ShiftHandoverController.java`、`routes/ShiftHandoverRoutes.java`、`database/init.sql`（shift_handover.status）。
  - 前端：`constants/ShiftHandoverStatus.ts`、`types/ShiftHandover.ts`、`constants/statusText.ts`、`stores/ShiftHandoverStore.ts`、`hooks/useShiftHandover.ts`、`components/warehouse/*`、`mocks/shiftHandoverStore.ts`（离线兜底种子）。
- HandoverItemCategory（INBOUND_PENDING / EXPIRING_BATCH / ABNORMAL_INVENTORY / OTHER）与 HandoverItemStatus（PENDING / DONE）:
  - 后端：`constants/HandoverItemCategory.java`、`constants/HandoverItemStatus.java`、`models/HandoverItem.java`、`services/ShiftHandoverService.java`、`repositories/ShiftHandoverRepository.java`、`database/init.sql`（handover_item 表）。
  - 前端：`constants/HandoverItemCategory.ts`、`constants/HandoverItemStatus.ts`、`types/ShiftHandover.ts`、`constants/statusText.ts`、`components/warehouse/ShiftHandoverForm.vue`、`components/warehouse/ShiftHandoverDetail.vue`、`mocks/shiftHandoverStore.ts`。
- 交接相关错误码（HANDOVER_SHIFT_CONFLICT / HANDOVER_NOT_CLAIMABLE / HANDOVER_ITEMS_PENDING / HANDOVER_NOT_FOUND / HANDOVER_ITEM_NOT_FOUND）：后端 `constants/ErrorCodes.java` + `constants/ErrorMessages.java` + `services/ShiftHandoverException.java` + `constructors/ShiftHandoverDtoFactory.errorDto` + `controllers/ShiftHandoverController.wrap`；前端 `constants/errorCodes.ts`、`constants/errorMessages.ts`、`api/ShiftHandover.ts`（HandoverApiError）。
- 交接日志模板（SHIFT_HANDOVER_CREATE / CLAIM / ITEM_PROCESS / CLOSE / REJECTED）：后端 `constants/LogTemplates.java`（AUDIT logger 输出）、前端 `constants/logTemplates.ts`。

## 仓库值班交接（/warehouses）

仓库页内置值班交接，替代群消息交接，避免待入库、临期批次与异常库存遗漏：

1. 登记：选择**交接仓库、当班负责人与值守时段**，并**逐条登记事项（待入库/临期批次/异常库存/其他）与接手人**。
2. 时段唯一：后端与前端兜底均按「`start < other.end && end > other.start`（边界相接不算重叠）」校验，**同一仓库重叠时段只允许一张有效交接单（待领取/处理中）**；冲突时返回 409 并说明与哪张单冲突，**原单与库存不发生任何改动**。
3. 领取与处理：新值班员输入姓名**领取**后才能逐项处理；**已领取/已关闭**重复领取返回 409 并提示领取人与当前状态。
4. 关闭：全部事项标记为已处理才允许关闭，否则 409 提示剩余条数；关闭后不再占用时段。
5. 回看：`GET /api/shift-handover?warehouse_id=` 按仓库回看，重开页面仍在「历史交接」中折叠查看。
6. 库存隔离：交接动作只改事项/单据状态，**不回写 inventory_batch**；批次号仅用于定位。
7. 离线兜底：后端不可达时前端使用 `localStorage`（`mocks/shiftHandoverStore.ts`）承载同样的规则，重开页面数据仍在；接口与字段以 `/api/shift-handover` 为准。

接口：`GET/POST /api/shift-handover`、`GET /api/shift-handover/{id}`、`POST /{id}/claim`、`PUT /{id}/items/{itemId}`、`POST /{id}/close`。

## 为什么会牵一发动全身

实体字段、枚举、日志模板、错误消息、构造器、筛选器和展示组件被刻意拆散到多个目录；修改一个状态值通常需要同步类型、构造器、服务、控制器、store、页面、README 与数据库种子。

## License

MIT

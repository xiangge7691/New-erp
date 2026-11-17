# 工序排产模块 · 前端对接文档

> 后端：已完成排产计划表、批次表、计划参与人表、班组班次表及 `/api/scheduling/*`、`/api/production-teams/*`、`/api/production-shifts/*` 接口  
> 测试库：`192.168.5.100 / erp_db`  
> 后端对接 HTML：[backend/scheduling.html](prototype/production/backend/scheduling.html)  
> 目标态交互 HTML：[scheduling.html](prototype/production/scheduling.html)  
> 全模块指导：[frontend-implementation-guide-scheduling-performance.md](frontend-implementation-guide-scheduling-performance.md)

---

## 1. 页面范围

排产模块对应一张页面 `/production_scheduling`，内部沿用之前原型的视图切换：

- **排产总览**：全部未完成任务的工序占用、任务概览、设备占用与甘特图（按任务/设备/房间）；冲突只在正式保存失败时以抽屉展示，不做常驻冲突清单。
- **任务排产**：任务卡片、工序计划表、批次拆分、保存草稿/正式保存。
- **工序看板**：逐工序计划/实际对照，按任务/设备/房间/班组/状态筛选。
- **班组与班次**：班组、成员、班次维护；排产选择班组带入成员。
- **原型说明**：规则、校验边界、接口说明。

实际执行和人员绩效是独立页面，不在本模块内。

---

## 2. 后端接口

### 2.1 排产

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/scheduling/tasks?keyword=` | 未完成工单排产任务列表 |
| GET | `/api/scheduling/tasks/{workOrderId}` | 排产详情：计划 + 模板 + 实际 + 冲突 |
| POST | `/api/scheduling/tasks/{workOrderId}/auto-plan` | 按模板标准工时自动顺排，保存草稿 |
| PUT | `/api/scheduling/tasks/{workOrderId}/plan` | 保存草稿（允许冲突） |
| POST | `/api/scheduling/tasks/{workOrderId}/plan/save` | 正式保存（有冲突拒绝） |
| POST | `/api/scheduling/tasks/{workOrderId}/conflicts/check` | 当前任务冲突检查 |
| POST | `/api/scheduling/plans/{planId}/split` | 拆分工序批次 |
| GET | `/api/scheduling/gantt?view=task|equipment|room&from=&to=` | 甘特图数据 |

### 2.2 班组与班次

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/production-teams?keyword=&status=` | 班组列表 |
| GET | `/api/production-teams/active` | 启用班组 |
| GET | `/api/production-teams/{id}` | 班组详情 |
| GET | `/api/production-teams/{id}/members` | 班组成员 |
| POST | `/api/production-teams` | 新建班组，`memberIds` 全量提交 |
| PUT | `/api/production-teams/{id}` | 修改班组 |
| PUT | `/api/production-teams/{id}/status?status=0` | 启停班组 |
| GET | `/api/production-shifts?keyword=&status=` | 班次列表 |
| GET | `/api/production-shifts/by-team/{teamId}` | 班组适用班次 |
| POST | `/api/production-shifts` | 新建班次，`teamIds` 全量提交 |
| PUT | `/api/production-shifts/{id}` | 修改班次 |
| PUT | `/api/production-shifts/{id}/status?status=0` | 启停班次 |

> 接口需要 JWT：`Authorization: Bearer <token>`。  
> 当前后端只校验 JWT，**没有接口级 RBAC**；前端角色切换/按钮隐藏不能替代后端权限。

---

## 3. TypeScript 类型

```ts
export interface ScheduleTaskDto {
  workOrderId: number
  workOrderCode: string
  preparationName: string
  batchNumber: string
  batchQty: number
  currentStatus: string
  planStartTime?: string | null
  processCount: number
  conflictCount: number
  saved: boolean
}

export interface PlanParticipantDto {
  personnelId: number
  personnelName?: string
  isPrimary?: number
}

export interface PlanBatchDto {
  id?: number
  batchNo: number
  batchName?: string | null
  planStartTime?: string | null
  planEndTime?: string | null
  planDurationMinutes?: number | null
  planQty: number
  roomId?: number | null
  roomName?: string | null
  equipmentId?: number | null
  equipmentName?: string | null
  teamId?: number | null
  teamName?: string | null
  status?: string
  participants?: PlanParticipantDto[]
  /* 实际对照 */
  actualStartTime?: string | null
  actualEndTime?: string | null
  actualStatus?: string | null
  actualQty?: number | null
  executionId?: number | null
  locked?: number | null
}

export interface ScheduleConflictDto {
  type: '设备占用' | '房间占用' | '人员班次' | '工序顺序' | '批次重叠' | '批次数量'
  workOrderId?: number
  workOrderCode?: string
  planId?: number
  batchId?: number | null
  stepOrder?: number
  processName?: string
  startTime?: string
  endTime?: string
  message: string
  otherWorkOrderId?: number
  otherWorkOrderCode?: string
  recommendedStartTime?: string | null
  recommendedEndTime?: string | null
}

export interface ProcessScheduleDto {
  planId?: number | null
  workOrderId: number
  processTypeId: number
  processName: string
  stepOrder: number
  templateId?: number | null
  standardDuration?: number | null
  standardQty?: number | null
  planStartTime?: string | null
  planEndTime?: string | null
  planDurationMinutes?: number | null
  planQty?: number | null
  roomId?: number | null
  roomName?: string | null
  equipmentId?: number | null
  equipmentName?: string | null
  teamId?: number | null
  teamName?: string | null
  status: string
  batches?: PlanBatchDto[]
  participants?: PlanParticipantDto[]
  actualStartTime?: string | null
  actualEndTime?: string | null
  actualStatus?: string | null
  actualQty?: number | null
  executionId?: number | null
  conflicts?: ScheduleConflictDto[]
}

export interface ScheduleDetailDto {
  workOrderId: number
  workOrderCode: string
  preparationName: string
  batchNumber: string
  batchQty: number
  currentStatus: string
  planStartTime?: string | null
  processes: ProcessScheduleDto[]
  conflicts: ScheduleConflictDto[]
}

export interface ProcessPlanSaveDto {
  planId?: number | null
  processTypeId: number
  stepOrder: number
  templateId?: number | null
  planStartTime?: string | null
  planEndTime?: string | null
  planQty?: number | null
  roomId?: number | null
  equipmentId?: number | null
  teamId?: number | null
  remark?: string
  batches?: PlanBatchDto[]
  participants?: PlanParticipantDto[]
}

export interface ScheduleSaveRequest {
  workOrderId: number
  processes: ProcessPlanSaveDto[]
}

export interface ScheduleSaveResult {
  saved: boolean
  conflicts: ScheduleConflictDto[]
  processes: ProcessScheduleDto[]
}
```

---

## 4. API 模块

```ts
// src/api/production-scheduling.ts
import request from '@/utils/http'

export const fetchSchedulingTasks = (keyword?: string) =>
  request.get<ScheduleTaskDto[]>({ url: '/api/scheduling/tasks', params: { keyword } })

export const fetchSchedulingDetail = (workOrderId: number) =>
  request.get<ScheduleDetailDto>({ url: `/api/scheduling/tasks/${workOrderId}` })

export const autoPlan = (workOrderId: number) =>
  request.post<ScheduleDetailDto>({ url: `/api/scheduling/tasks/${workOrderId}/auto-plan` })

export const saveScheduleDraft = (workOrderId: number, data: ScheduleSaveRequest) =>
  request.put<ScheduleSaveResult>({ url: `/api/scheduling/tasks/${workOrderId}/plan`, data })

export const checkConflicts = (workOrderId: number) =>
  request.post<ScheduleConflictDto[]>({ url: `/api/scheduling/tasks/${workOrderId}/conflicts/check` })

export const splitPlan = (planId: number, data: PlanBatchDto[]) =>
  request.post<ScheduleSaveResult>({ url: `/api/scheduling/plans/${planId}/split`, data })

export const fetchGantt = (params: {
  view: 'task' | 'equipment' | 'room'
  from: string
  to: string
}) => request.get<GanttItemDto[]>({ url: '/api/scheduling/gantt', params })
```

### 4.1 正式保存的 400 冲突

后端正式保存冲突返回：

```json
{
  "code": 400,
  "message": "存在 2 项冲突，排产未保存",
  "data": {
    "saved": false,
    "conflicts": [ { "type": "设备占用", "message": "...", "recommendedStartTime": "..." } ],
    "processes": []
  }
}
```

现有 `src/utils/http` 抛 `HttpError` 时默认不带 `data`，需要补：

```ts
function createHttpError(message: string, code: number, data?: unknown) {
  return new HttpError(message, code, { data })
}
// 拦截器：
throw createHttpError(message || $t('httpMsg.requestFailed'), code, data)
```

API 层：

```ts
export async function saveScheduleFormal(workOrderId: number, data: ScheduleSaveRequest) {
  try {
    return await request.post<ScheduleSaveResult>({
      url: `/api/scheduling/tasks/${workOrderId}/plan/save`,
      data,
      showErrorMessage: false
    })
  } catch (e) {
    if (e instanceof HttpError && e.code === 400 && e.data) {
      return e.data as ScheduleSaveResult
    }
    throw e
  }
}
```

页面拿到 `saved=false` 时不要关闭页面，直接打开冲突抽屉。

---

## 5. 页面字段映射

### 5.1 任务卡片

| 页面字段 | 后端字段 |
|---|---|
| 工单编号 | `workOrderCode` |
| 制剂名称 | `preparationName` |
| 批号 | `batchNumber` |
| 批量 | `batchQty` |
| 状态 | `currentStatus` |
| 计划开始 | `planStartTime` |
| 工序数 | `processCount` |
| 冲突数 | `conflictCount` |
| 已保存 | `saved` |

### 5.2 工序表

| 页面列 | 后端字段 |
|---|---|
| 工序 | `processName` / `stepOrder` |
| 标准工时 | `standardDuration`（模板未维护时为空） |
| 计划起止 | `planStartTime` / `planEndTime` |
| 计划用时 | `planDurationMinutes` |
| 计划数量 | `planQty` |
| 设备/房间 | `equipmentId/equipmentName`、`roomId/roomName` |
| 班组 | `teamId/teamName` |
| 操作人 | `participants[]` |
| 批次 | `batches[]` |
| 计划状态 | `status`（草稿/已保存/需重排） |
| 冲突 | `conflicts[]` |
| 实际对照 | `actualStartTime/actualEndTime/actualStatus/actualQty/executionId` |
| 批次实际对照 | `batches[].actualStartTime/actualEndTime/actualStatus/actualQty/executionId/locked` |

> 实际起止与数量来自 `work_order_process_execution`，通过 `plan_id` / `plan_batch_id` 关联；排产页只读展示，不修改实际记录。拆分批次时，工序级实际值按批次汇总（最早开始、最晚结束、数量合计）。

### 5.3 批次

`batches[].batchNo / batchName / planStartTime / planEndTime / planQty / roomId / equipmentId / teamId / participants`。  
批次数量合计必须等于工序的 `planQty`。

### 5.4 冲突

`type / processName / stepOrder / startTime / endTime / message / otherWorkOrderCode / recommendedStartTime / recommendedEndTime`。

---

## 6. 页面数据流

### 6.1 初始化

1. `fetchSchedulingTasks()` 渲染任务卡片；`conflictCount` 显示红点，`saved` 显示是否已正式保存。
2. 默认选中第一个任务，`fetchSchedulingDetail(workOrderId)`。
3. 工序表直接使用 `processes`；有 `batches` 的行显示“拆 N 批”。

### 6.2 编辑与保存

- 所有编辑只改本地 `processes`，不每改一个字段就请求后端。
- **保存草稿**：`saveScheduleDraft`，允许 `conflicts` 非空，用于逐步调整。
- **正式保存**：`saveScheduleFormal`。
  - `saved=true`：提示成功并刷新详情。
  - `saved=false`：打开冲突抽屉；每条冲突显示类型、工序、时段、消息、推荐时段。
  - “套用推荐”只修改本地对应工序/批次的计划起止，重新保存时后端会再次全量校验。

### 6.3 自动顺排

- 调 `autoPlan`。
- 后端要求：
  - 工单有 `planStartTime`；
  - 该制剂的所有工序模板都有 `standardDuration > 0`。
- 任一条件不满足返回 400，页面提示具体工序或“工单未设置计划开始时间”，不要静默失败。
- 当前测试库 65 条工序模板的标准工时全部为空，自动顺排会直接 400，这是数据前置条件，不是前端 bug。

### 6.4 拆批次

- 打开批次弹窗，填写批次号、时间、数量、班组/人员。
- 前端先校验数量合计等于工序 `planQty`，再调 `splitPlan(planId, batches)`。
- 后端也会校验数量和批次重叠；冲突时返回 `saved=false`。

### 6.5 甘特图

- `fetchGantt({ view, from, to })`。
- `groupKey` 在 task 视图是工单编号，在 equipment/room 视图是设备/房间名称；`conflict=true` 的条目红色。

### 6.6 班组班次

- 班组列表 `GET /api/production-teams`；新建/修改时提交 `memberIds`。
- 班次 `GET /api/production-shifts`；新建/修改提交 `teamIds`，时间格式 `HH:mm:ss`。
- 排产选择班组时，后端在 `participants` 为空时会自动带入班组成员；前端也可以显式传参与人。
- 同一班组同一时段可以负责多台设备，不要做班组互斥校验。

---

## 7. 前端校验（后端仍会再校验）

- 首道工序计划开始不得早于工单 `planStartTime`。
- 后道工序计划开始不得早于紧前工序计划开始；**允许时段重叠**。
- 正式保存时每道工序必须有计划起止，或所有批次有计划起止。
- 计划结束必须晚于计划开始。
- 批次数量合计 = 工序计划数量；批次时段不得重叠。
- 设备、房间、班组有值时才参与对应冲突校验。
- 同一工单内 `stepOrder` 不允许重复。

---

## 8. 错误处理

| 场景 | 后端 | 前端 |
|---|---|---|
| 正式保存有冲突 | 400 + `data.conflicts` | 打开冲突抽屉，保留当前编辑 |
| 自动顺排标准工时缺失 | 400 | 提示具体工序，引导补录模板 |
| 工单无计划开始时间 | 400 | 提示先维护工单计划开始时间 |
| 首道工序早于任务开始 | 400 | 表单校验拦截 |
| 批次数量不平 | 400 | 弹窗内红字提示，禁止提交 |
| 批次时段重叠 | 400 | 冲突抽屉/表单提示 |
| 401 | 未登录 | 走现有登出逻辑 |

---

## 9. 当前后端边界

已完成：

- 排产计划、批次、计划参与人表与接口。
- 设备/房间/班次/工序顺序/批次冲突校验。
- 草稿/正式保存；正式保存冲突拒绝。
- 拆批次、甘特图、班组班次 CRUD。
- 排产结果与实际执行通过 `plan_id/plan_batch_id` 关联。
- 批次级参与人保存到 `work_order_process_plan_participant.batch_id`，前端在 `batches[].participants` 传入即可。
- 工序与批次 DTO 返回实际起止、实际数量、执行记录 ID 和锁定状态，排产看板可直接对照计划与实际。

仍需注意：

1. **标准工时数据为空**：自动顺排不可用，需业务补录 `preparation_process_template.standard_duration`。
2. **“需重排”状态**：字段存在，但外部变更触发重算并标记“需重排”的规则尚未实现；当前只有保存草稿/正式保存会改变状态。
3. **RBAC**：接口只校验 JWT，没有按生产主管/主任/班组长拦截。
4. **测试库部分工单 `plan_start_time` 为空**：排产详情会返回 null，自动顺排会 400。

---

## 10. 联调验收清单

- [ ] 任务列表能显示未完成工单、工序数、冲突数、是否已保存。
- [ ] 选择任务能加载计划、模板、实际与冲突。
- [ ] 保存草稿成功，允许冲突残留。
- [ ] 正式保存有冲突时返回 400，页面能列出冲突与推荐时段，不关闭页面。
- [ ] 冲突全部消除后正式保存成功，`saved=true`。
- [ ] 拆批次数量合计校验、批次时段校验生效。
- [ ] 自动顺排缺标准工时时能显示后端 400 文案。
- [ ] 甘特图按任务/设备/房间切换，冲突标红。
- [ ] 班组、成员、班次可维护；排产选择班组能带入成员。
- [ ] 同一班组可同时排多台设备，不报人员冲突。

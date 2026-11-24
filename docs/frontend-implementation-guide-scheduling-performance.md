# 排产 · 实际执行 · 人员绩效 前端实现指导

> 对应后端：`v1.0` 分支已完成的后端改造  
> 测试库：`192.168.5.100 / erp_db`（`src/main/resources/application.yml` 与 `src/test/resources/application.properties` 均已指向该库）  
> 前端工程：`C:\Work\Java\art-design-pro-main`（Vue 3 + TypeScript + Element Plus + ArtTable）  
> 配套 HTML：[后端对接原型入口](prototype/production/backend/index.html)  
> 本文只描述前端如何接现有后端，不涉及 PRD 修改。

> **2026-11-18 变更提示**：生产任务与排产合并为「外层生产任务列表（顶部甘特图）+ 任务详情（详情/变更历史/排产/实际生产页签）」；排产改为可选、班组仅作人员快捷分组、班次模块删除、人员冲突改为同一人时段重叠、绩效支持关键字搜索与修改前值留痕。最新接口与字段以 [scheduling-module-frontend-integration.md](scheduling-module-frontend-integration.md) 为准，本文中涉及的班次、工序看板与独立实际记录页内容不再适用。

---

## 0. 交付物

| 交付物 | 路径 | 说明 |
|---|---|---|
| 后端对接 HTML | `docs/prototype/production/backend/index.html` | 入口页，含工序排产、实际记录、人员绩效三张页面 |
| 工序排产 HTML | `docs/prototype/production/backend/scheduling.html` | 任务、工序计划、批次、甘特、班组班次 |
| 实际记录 HTML | `docs/prototype/production/backend/actual-record.html` | 批次实际起止/数量、参与人、完工、完工记录 |
| 人员绩效 HTML | `docs/prototype/production/backend/performance.html` | 工序/人员粒度、汇总、系数版本、补录与作废 |
| 工序执行 PDA 原型 | `docs/prototype/pda-warehouse/pda.html?role=worker` | 操作工角色：任务—工序两级、开始/结束打点、设备占用校验、逐人数量、照片、暂存/提交只读 |
| 前端对接 MD | `docs/frontend-implementation-guide-scheduling-performance.md` | 本文，三模块统一对接说明 |

HTML 是“按后端接口与字段重构”的交互原型，页面里的演示数据用于评审交互，接入时以本文接口和字段为准。

---

## 1. 总体约定

### 1.1 认证与响应

- 除 `/api/auth/login` 外都需要 JWT：请求头 `Authorization: Bearer <token>`。
- 统一响应：

```json
{ "code": 200, "message": "操作成功", "data": {}, "timestamp": 1794800000000 }
```

- 现有 `src/utils/http` 会直接返回 `data`，业务码非 200 时抛 `HttpError`。
- **排产正式保存冲突时后端返回 `code=400`，冲突明细在 `data.conflicts`**。当前 `src/utils/http` 抛错时没有把 `data` 带到 `HttpError`，需要补一处：

```ts
// src/utils/http/index.ts
function createHttpError(message: string, code: number, data?: unknown) {
  return new HttpError(message, code, { data })
}

// 拦截器里改为：
throw createHttpError(message || $t('httpMsg.requestFailed'), code, data)
```

前端 API 层再统一把 400 转成业务结果：

```ts
export async function saveScheduleFormal(workOrderId: number, payload: ScheduleSaveRequest) {
  try {
    return await request.post<ScheduleSaveResult>({
      url: `/api/scheduling/tasks/${workOrderId}/plan/save`,
      data: payload,
      showErrorMessage: false
    })
  } catch (e) {
    if (e instanceof HttpError && e.code === 400 && e.data) {
      return e.data as ScheduleSaveResult // { saved:false, conflicts:[...] }
    }
    throw e
  }
}
```

### 1.2 状态值

| 字段 | 取值 |
|---|---|
| 计划状态 `status` | `草稿` / `已保存` / `需重排` |
| 批次状态 | `计划中` / `执行中` / `已完工`（按关联的实际执行记录推导：无执行=计划中，有开始未全部完成=执行中，全部完成=已完工） |
| 执行状态 | `待执行` / `执行中` / `已完成` |
| 批次实际状态 `batches[].actualStatus` | 未登记实际生产时为 `待执行`（不再返回 null）；存在多条执行记录时按最早开始、最晚结束、数量合计汇总 |
| 绩效来源 `source` | `系统生成` / `手动添加`（中文原值，不要传 system/manual） |
| 绩效计算方式 `calcMode` | `按系数计算` / `直接计入` |
| 修改状态 `modifiedStatus` | `0` 未修改 / `1` 已修改 |
| 记录状态 `recordStatus` | `1` 正常 / `0` 作废 |

---

## 2. 页面与路由

建议新增 4 个页面：

| 页面 | 路由 | 组件目录 |
|---|---|---|
| 工序排产 | `/production_scheduling` | `src/views/production_scheduling` |
| 实际执行 | `/production_execution` | `src/views/production_execution` |
| 人员绩效 | `/performance` | `src/views/performance` |
| 班组班次 | `/production_team` | `src/views/production_team` |

### 2.1 路由模块

```ts
// src/router/modules/production-scheduling.ts
import { AppRouteRecord } from '@/types/router'

export const productionSchedulingRoutes: AppRouteRecord = {
  path: '/production_scheduling',
  name: 'ProductionScheduling',
  component: '/production_scheduling',
  meta: {
    title: '工序排产',
    icon: 'ri:calendar-schedule-line',
    keepAlive: true,
    roles: ['R_SUPER', 'R_ADMIN'],
    authList: [
      { title: '排产', authMark: 'production_schedule_update' },
      { title: '查看', authMark: 'production_schedule_read' }
    ]
  }
}
```

在 `src/router/modules/index.ts` 的 `routeModules` 中注册 4 个模块，菜单会自动出现。

> 注意：后端目前只校验 JWT，**没有接口级 RBAC**；路由 `roles` / `authList` 只是前端可见性控制，不能替代后端权限。权限细化是后续任务。

---

## 3. API 模块与类型

新建 `src/api/production-scheduling.ts`、`src/api/production-execution.ts`、`src/api/performance.ts`、`src/api/production-team.ts`。

### 3.1 排产类型

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
  /* 实际对照：来自 work_order_process_execution */
  actualStartTime?: string | null
  actualEndTime?: string | null
  actualStatus?: string | null
  actualQty?: number | null
  executionId?: number | null
  locked?: number | null
}

export interface PlanParticipantDto {
  personnelId: number
  personnelName?: string
  isPrimary?: number
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

### 3.2 排产接口

```ts
import request from '@/utils/http'

export const fetchSchedulingTasks = (keyword?: string) =>
  request.get<ScheduleTaskDto[]>({ url: '/api/scheduling/tasks', params: { keyword } })

export const fetchSchedulingDetail = (workOrderId: number) =>
  request.get<ScheduleDetailDto>({ url: `/api/scheduling/tasks/${workOrderId}` })

export const autoPlan = (workOrderId: number) =>
  request.post<ScheduleDetailDto>({ url: `/api/scheduling/tasks/${workOrderId}/auto-plan` })

export const saveDraft = (workOrderId: number, data: ScheduleSaveRequest) =>
  request.put<ScheduleSaveResult>({ url: `/api/scheduling/tasks/${workOrderId}/plan`, data })

export const checkConflicts = (workOrderId: number) =>
  request.post<ScheduleConflictDto[]>({ url: `/api/scheduling/tasks/${workOrderId}/conflicts/check` })

export const splitPlan = (planId: number, batches: PlanBatchDto[]) =>
  request.post<ScheduleSaveResult>({ url: `/api/scheduling/plans/${planId}/split`, data: batches })

export const fetchGantt = (params: { view: 'task' | 'equipment' | 'room'; from: string; to: string }) =>
  request.get<GanttItemDto[]>({ url: '/api/scheduling/gantt', params })
```

> `autoPlan` 在工序模板未维护标准工时时返回 400，提示类似“工序【xxx】未维护标准工时，无法自动顺排”。UI 应禁用按钮或给出补录引导。

### 3.3 实际执行类型与接口

后端实体 `WorkOrderProcessExecution` 字段：

```ts
export interface ExecutionParticipant {
  id?: number
  executionId?: number
  personnelId: number
  personnelName?: string
  employeeNo?: string
  teamName?: string
  workHours: number
  completedQty?: number | null
  isPrimary?: number
  remark?: string
}

export interface ProcessExecution {
  id?: number
  workOrderId: number
  planId?: number | null
  planBatchId?: number | null
  batchNo?: number | null
  processTypeId?: number
  processTypeName?: string
  stepOrder?: number
  roomId?: number | null
  roomName?: string
  equipmentId?: number | null
  equipmentName?: string
  teamId?: number | null
  operatorId?: number | null
  operatorName?: string
  startTime?: string | null
  endTime?: string | null
  processQty?: number | null
  keyProcessParams?: string
  status?: string
  locked?: number
  completedTime?: string | null
  remark?: string
  participants?: ExecutionParticipant[]
  /* 关联显示字段（后端返回，不用提交） */
  workOrderCode?: string
  preparationName?: string
  batchNumber?: string
  planBatchName?: string | null
  planStartTime?: string | null
  planEndTime?: string | null
  planDurationMinutes?: number | null
  planQty?: number | null
  performanceRecordCount?: number
}
```

```ts
export const fetchExecutions = (workOrderId: number) =>
  request.get<ProcessExecution[]>({ url: `/api/work-order-process-execution/by-work-order/${workOrderId}` })

export const saveExecutions = (workOrderId: number, data: ProcessExecution[]) =>
  request.post<ProcessExecution[]>({
    url: `/api/work-order-process-execution/batch?workOrderId=${workOrderId}`,
    data
  })

export const completeExecution = (executionId: number) =>
  request.post<ProcessExecution>({ url: `/api/work-order-process-execution/${executionId}/complete` })

export const fetchCompletedExecutions = (params: { from?: string; to?: string; workOrderId?: number }) =>
  request.get<ProcessExecution[]>({ url: '/api/work-order-process-execution/completed', params })

export const exportCompletedExecutions = (params: { from?: string; to?: string; workOrderId?: number }) =>
  request.get<Blob>({ url: '/api/work-order-process-execution/completed/export', params, responseType: 'blob' })
```

保存实际记录时：

- `participants` 传空数组会清空参与人；不传（`undefined`）则保留现有参与人。
- 参与人未传 `workHours` 时按工序实际起止自动计算；填写了实际数量但未逐人填写完成数量时，按参与人均分（尾差计入最后一人）；部分填写时仍要求补全且合计等于 `processQty`。
- `locked=1` 的记录不能再修改或删除；完工请调用 `complete` 接口。

### 3.4 绩效类型与接口

```ts
export interface PerformanceRecord {
  id: number
  personnelId: number
  personnelName?: string
  workOrderId?: number
  workOrderCode?: string
  preparationName?: string
  batchNumber?: string
  processTypeId?: number
  processName?: string
  planBatchId?: number
  planBatchName?: string
  executionId?: number
  batchNo?: number
  performanceDate: string
  processActualHours?: number | null
  workHours?: number | null
  performanceHours: number
  processCoefficient?: number | null
  personCoefficient?: number | null
  coefficientVersionId?: number | null
  calcMode: string
  performanceHoursTotal: number
  completedQty?: number | null
  source: string
  modifiedStatus: number
  recordStatus: number
  note?: string
  updatedBy?: number
  updatedTime?: string
  /* 关联显示字段（后端返回，不用提交） */
  teamName?: string
  updatedByName?: string
  planStartTime?: string | null
  planEndTime?: string | null
  planDurationMinutes?: number | null
  planQty?: number | null
  actualStartTime?: string | null
  actualEndTime?: string | null
  coefficientVersionNo?: string | null
}

export interface PerformanceQuery {
  from?: string
  to?: string
  personnelId?: number
  workOrderId?: number
  teamId?: number
  source?: string
  modifiedStatus?: number
  recordStatus?: number
  pageIndex?: number
  pageSize?: number
}
```

```ts
export const fetchPerformanceRecords = (params: PerformanceQuery) =>
  request.get<PagedResult<PerformanceRecord>>({ url: '/api/performance/records', params })

export const modifyPerformance = (id: number, data: { performanceHours: number; completedQty?: number; note: string }) =>
  request.put<PerformanceRecord>({ url: `/api/performance/records/${id}`, data })

export const addManualPerformance = (data: ManualPerformanceRequest) =>
  request.post<PerformanceRecord>({ url: '/api/performance/manual', data })

export const voidPerformance = (id: number, note: string) =>
  request.post<PerformanceRecord>({ url: `/api/performance/records/${id}/void`, params: { note } })

export const fetchCoefficientVersions = () =>
  request.get<CoefficientVersion[]>({ url: '/api/performance/coefficient-versions' })

export const fetchCoefficientVersionDetail = (id: number) =>
  request.get<CoefficientVersionDetail>({ url: `/api/performance/coefficient-versions/${id}` })

export const publishCoefficientVersion = (data: CoefficientPublishRequest) =>
  request.post<CoefficientVersion>({ url: '/api/performance/coefficient-versions', data })

export const exportPerformance = (params: PerformanceQuery) =>
  request.get<Blob>({ url: '/api/performance/export', params, responseType: 'blob' })
```

导出接口返回 xlsx 文件流，现有 `request` 只处理 JSON；需要单独用 `axios` 实例或给 `request` 增加 `responseType: 'blob'` 分支，再用 `file-saver` 保存。导出列已包含班组、计划起止、实际起止。

系数版本详情：

```ts
export interface CoefficientVersionDetail {
  id: number
  versionNo: string
  effectiveTime: string
  operatorId?: number
  note?: string
  status: string
  processCoefficients: Record<number, number>
  personCoefficients: Record<number, number>
  processItems: Array<{ processTypeId: number; processCode?: string; processName?: string; coefficient: number }>
  personItems: Array<{
    personnelId: number
    personnelName?: string
    employeeNo?: string
    teamName?: string
    coefficient: number
  }>
}
```

前端展示“系数与版本”时优先用 `processItems` / `personItems`，已带工序名称、人员姓名、工号和班组，不需要再拼多张基础表。

> 绩效筛选的 `source` 必须传中文 `系统生成` / `手动添加`；`teamId` 由后端换算成班组成员后过滤。工序粒度另有 `GET /api/performance/process-view?workOrderId=&teamId=&from=&to=`。

---

## 4. 排产页

### 4.1 布局

```
┌ 任务卡片（按未完成工单） ─────────────────────────────┐
├ 工序表（计划 + 实际 + 状态 + 冲突） ───────────────────┤
├ 保存失败时冲突抽屉（按需：类型/工序/时段/推荐时段） ──┤
├ 甘特图（按任务 / 设备 / 房间切换） ────────────────────┤
└ 底部操作：保存草稿 / 正式保存 ────────────────────────┘
```

### 4.2 交互流程

1. 进入页面：`GET /api/scheduling/tasks` 渲染任务卡片；`conflictCount` 红点、`saved` 标记。
2. 选择任务：`GET /api/scheduling/tasks/{id}`。
3. 编辑计划：直接改本地 `processes` 的 `planStartTime/planEndTime/planQty/roomId/equipmentId/teamId`。
4. 自动顺排：`POST auto-plan`。标准工时缺失时展示后端 400 文案，不要假装成功。
5. 保存草稿：`PUT .../plan`，允许 `saved=true, conflicts` 非空，用于反复调整。
6. 正式保存：`POST .../plan/save`。
   - `saved=true`：提示成功，刷新详情。
   - `saved=false`：打开冲突抽屉，按 `conflicts` 渲染；每条显示 `type`、`processName`、`startTime~endTime`、`message`、`recommendedStartTime~recommendedEndTime`。
   - “套用推荐”只改本地对应行的计划时间，重新调用 `checkConflicts` 或再次正式保存。
7. 拆批次：对某行打开抽屉，填写批次号、时间、数量（数量只需大于 0，合计不强制等于 `planQty`，计划数量为排产时手工填写，可按实际安排拆分）后调用 `POST /api/scheduling/plans/{planId}/split`。
8. 甘特图：`GET /api/scheduling/gantt`，按 `view` 分组；`conflict=true` 的条目红色。

### 4.3 校验提示（前端先做一层，后端仍会校验）

- 后一项计划开始不得早于前一项计划开始；允许时段重叠。
- 首道工序不得早于工单 `planStartTime`。
- 批次数量仅要求大于 0，合计不强制等于工序计划数量；批次时段不得重叠。
- 正式保存前检查：每道工序必须有计划起止（或批次起止）。
- 同一班组可以同时负责多台设备，前端不要把班组做成互斥资源。

### 4.4 冲突类型文案

| type | 展示 |
|---|---|
| 设备占用 | 设备【xxx】与其他工单重叠 |
| 房间占用 | 房间【xxx】与其他工单重叠 |
| 人员班次 | 班组【xxx】不在有效班次内 |
| 工序顺序 | 计划开始早于紧前工序开始 |
| 批次重叠 | 同工序批次时段重叠 |

---

## 5. 实际执行页

### 5.1 页面结构

- 工单选择（可复用生产任务列表或从排产页跳转）。
- 工序表列：工序、计划起止、实际起止、计划/实际数量、设备、房间、班组、操作人、状态、锁定。
- 计划起止/数量来自 `planBatchName / planStartTime / planEndTime / planQty`（未拆批次时回退工序计划）；实际数据来自执行记录本身。
- 展开行：参与人表格（姓名、工号、班组、工作时间、完成数量、主操作）。
- 底部：保存实际记录、完工。

### 5.2 关键交互

1. 加载 `GET /api/work-order-process-execution/by-work-order/{id}`。
2. 编辑实际起止、实际数量（`processQty`）、设备、房间、参与人。
3. 保存 `POST /batch?workOrderId={id}`：
   - 参与人数量合计不等于 `processQty` 时后端 400，前端在保存前用红色“数量不平”提示阻断。
   - 已锁定（`locked=1`）的行只读，不参与提交或保持原值。
4. 完工 `POST /{executionId}/complete`：
   - 前端确认后调用；成功后该行 `locked=1`、`planBatch.status=已完工`，并生成个人绩效。
   - 已完工再次点击提示“已完工锁定”。
5. 实际时间与计划时间并列展示；延迟/提前用颜色标注，不修改计划值。
6. 完工记录页：`GET /api/work-order-process-execution/completed?from=&to=&workOrderId=`，直接返回已锁定记录、参与人、计划信息与 `performanceRecordCount`，不需要前端再按工单循环查询。

---

## 6. 人员绩效页

### 6.1 Tab 结构

| Tab | 数据 |
|---|---|
| 工序粒度 | 按任务—工序—批次—人展开 |
| 人员粒度 | 按人—任务—工序—时长 |
| 班组/品种/日期汇总 | `GET /api/performance/aggregate?groupBy=team|product|date` |
| 系数与版本 | `GET/POST /api/performance/coefficient-versions` |

### 6.2 列表字段

- 工序粒度：任务、制剂、批号、工序、批次、计划/实际起止、计划时长、工序实际时长、计划/实际数量、时长达成率、数量达成率、工作时间、绩效时长、绩效工时、完成数量、来源、修改状态、备注、修改人/时间。
- 人员粒度：人员、班组、绩效归属日期、任务、制剂、批号、工序、批次、实际起止、工序实际时长、工作时间、绩效时长、绩效工时、人员系数、完成数量、计算方式、来源、修改状态、备注、修改人/时间。
- 计划/实际时间与数量由后端 `planStartTime/planEndTime/planDurationMinutes/planQty/actualStartTime/actualEndTime/completedQty` 提供；达成率由前端计算：时长达成率 = 计划时长 ÷ 工序实际时长，数量达成率 = 完成数量 ÷ 计划数量（分母为 0 或空时显示 `—`）。
- 筛选：日期区间、人员、工单、班组（`teamId`）、来源（中文）、修改状态、记录状态（正常/作废/全部）。作废默认不显示。
- 汇总接口返回 `groupKey / recordCount / personCount / performanceHours / performanceHoursTotal / completedQty`；人均工时、占比由前端按这些值计算。

### 6.3 修改 / 补录 / 作废

- 修改：`PUT /records/{id}`，只改 `performanceHours`（手动记录可改 `completedQty`），`note` 必填；成功后 `modifiedStatus=1`。
- 补录：`POST /manual`
  - 关联工序：传 `workOrderId/processTypeId/planBatchId`，`calcMode=按系数计算`，后端取当前版本系数并快照。
  - 只写时长：只传 `personnelId/performanceDate/performanceHours/workHours`，`calcMode=直接计入`。
  - 同一人同一任务同一工序重复补录，后端允许；前端保存前提示“已存在”，填写原因后可继续。
- 作废：`POST /records/{id}/void?note=xxx`，仅 `source=手动添加` 可作废；作废后从统计和导出中排除，仅在记录状态=作废时可见。

### 6.4 系数版本

- 版本列表显示 `versionNo/effectiveTime/operatorId/note/status`。
- 发布：`POST /coefficient-versions`，`processCoefficients`、`personCoefficients` 为 `id -> 0.5~2.0` 的映射；`note` 必填。
- 发布后旧版本自动归档；已生成绩效记录保留原 `coefficientVersionId`，前端不要用当前系数重算历史行。
- 详情：`GET /coefficient-versions/{id}`，`processItems` / `personItems` 已含工序名称、人员姓名、工号与班组，直接渲染即可。

---

## 7. 班组班次页

- 班组：`GET/POST/PUT /api/production-teams`，成员用 `memberIds: number[]` 全量提交；停用用 `PUT /{id}/status?status=0`。
- 班次：`GET/POST/PUT /api/production-shifts`，`teamIds: number[]`；时间格式 `HH:mm:ss`，结束早于开始表示跨日班次。
- 排产选择班组时，前端带入成员；允许同一班组同时出现在多台设备的工序中，不要做“班组冲突”。

---

## 8. 错误与边界

| 场景 | 后端行为 | 前端处理 |
|---|---|---|
| 正式保存有冲突 | 400 + `data.conflicts` | 打开冲突抽屉，不关闭页面 |
| 自动顺排缺标准工时 | 400，message 指出工序 | 提示补录标准工时，禁用自动顺排 |
| 工单无 `planStartTime` | 400 | 引导先维护任务计划开始时间 |
| 参与人数量不平衡 | 400 | 保存前红字提示，阻断提交 |
| 记录已锁定 | 400 | 行只读，隐藏编辑/删除 |
| 重复完工 | 400 | 提示已完工 |
| 系数越界 | 400 | 表单校验 0.5–2.0 |
| 作废系统生成记录 | 400 | 只对手动记录显示作废 |

---

## 9. 联调顺序与验收

1. 先接班组/班次：能创建班组、成员、班次并绑定。
2. 再接排产：任务列表 → 详情 → 保存草稿 → 正式保存 → 冲突抽屉 → 拆批次 → 甘特。
3. 接实际执行：保存实际记录、参与人、数量平衡、完工。
4. 接绩效：查询 → 修改 → 补录 → 作废 → 系数版本 → 导出。
5. 验收重点：
   - 正式保存冲突被拒，页面能看到冲突明细；
   - 后项开始早于前项开始被拦截；
   - 班组可同时看多台设备；
   - 绩效工时 = 个人工作时间 × 工序系数 × 人员系数；
   - 调整系数不影响历史记录；
   - 作废记录不计入统计与导出。

---

## 10. 接口速查

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/scheduling/tasks` | 未完成工单排产任务列表 |
| GET | `/api/scheduling/tasks/{workOrderId}` | 排产详情（计划+模板+实际+冲突） |
| POST | `/api/scheduling/tasks/{workOrderId}/auto-plan` | 按标准工时自动顺排 |
| PUT | `/api/scheduling/tasks/{workOrderId}/plan` | 保存草稿 |
| POST | `/api/scheduling/tasks/{workOrderId}/plan/save` | 正式保存（冲突拒绝） |
| POST | `/api/scheduling/tasks/{workOrderId}/conflicts/check` | 当前冲突检查 |
| POST | `/api/scheduling/plans/{planId}/split` | 拆分工序批次 |
| GET | `/api/scheduling/gantt` | 甘特图（task/equipment/room） |
| GET | `/api/work-order-process-execution/by-work-order/{id}` | 实际执行记录（含参与人、计划信息） |
| GET | `/api/work-order-process-execution/completed` | 已完工记录（含参与人、计划信息、生成绩效数） |
| GET | `/api/work-order-process-execution/completed/export` | 导出完工记录 Excel |
| POST | `/api/work-order-process-execution/batch?workOrderId={id}` | 批量保存实际记录 |
| POST | `/api/work-order-process-execution/{id}/complete` | 工序完工并生成绩效 |
| GET | `/api/performance/records` | 绩效分页查询 |
| GET | `/api/performance/process-view` | 工序粒度 |
| GET | `/api/performance/person-view` | 人员粒度 |
| GET | `/api/performance/aggregate` | 班组/品种/日期汇总 |
| PUT | `/api/performance/records/{id}` | 修改绩效时长 |
| POST | `/api/performance/manual` | 手动补录 |
| POST | `/api/performance/records/{id}/void` | 作废手动记录 |
| GET | `/api/performance/coefficient-versions` | 系数版本列表 |
| POST | `/api/performance/coefficient-versions` | 发布系数版本 |
| GET | `/api/performance/coefficient-versions/{id}` | 版本详情 |
| GET | `/api/performance/export` | 导出 Excel |
| GET/POST/PUT | `/api/production-teams` | 班组维护 |
| GET/POST/PUT | `/api/production-shifts` | 班次维护 |

---

## 11. 当前后端限制

- **没有 RBAC**：接口只校验 JWT，不按角色限制；前端按钮权限只是展示层。
- **标准工时为空**：现有工序模板 `standard_duration` 全为空，自动顺排会返回 400；需要业务先补录。
- **“需重排”不会自动标记**：字段和取值已支持，但外部变更（工单计划调整、设备维保等）触发重算并自动改状态的规则尚未实现；当前只能通过重新保存排产改变状态。
- **`production_process_record` 只读保留**：新实际记录统一走 `work_order_process_execution`。
- **批次/参与人全量覆盖**：保存时以请求体为准，未提交的行会被删除；批次按 `batches[].id`（未带时按 `batchNo`）就地更新，**批次ID保持不变**；已登记实际生产的批次拒绝删除。前端编辑时务必提交完整集合。
- **绩效导出返回文件流**：现有 `request` 需补 `blob` 处理。

已完成（相对上一版新增）：

- 实际记录返回计划批次/工序的计划起止与数量，批次行也可带实际对照。
- 新增完工记录接口，返回已锁定记录、参与人、计划信息与生成绩效数。
- 绩效记录补充班组、计划/实际起止、计划时长/数量、系数版本号；支持按班组过滤。
- 系数版本详情返回带名称的 `processItems` / `personItems`。
- 排产工序与批次 DTO 补充 `actualQty`、`actualStatus`、`executionId`、`locked`。
- 批次 `status` 按关联的实际执行记录推导为 `计划中` / `执行中` / `已完工`；保存排产不再重建批次ID。

> 配套原型：[后端对接原型入口](prototype/production/backend/index.html)

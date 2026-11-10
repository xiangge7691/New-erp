# 检验管理前端对接指南

> 适用页面：`src/views/inspection_task`  
> 后端接口：`InspectionTaskController`、`InspectionBatchController`  
> 关联文档：`docs/frontend-implementation-guide-inspection-release.md`

---

## 1. 两个控制器分别负责什么

| 控制器 | 基础路径 | 职责 | 是否需要保留 |
|---|---|---|---|
| `InspectionTaskController` | `/api/inspection/tasks` | 任务查询、任务详情、创建任务、六步办理、跳过、作废 | 保留 |
| `InspectionBatchController` | `/api/inspection/batches` | 按工单/批次聚合的只读视图：批次列表、看板统计、批次详情 | 保留 |

建议**保持两个 Controller 分开**，不要合并：

- `tasks` 是执行资源，包含会改变状态的 POST 操作；
- `batches` 是只读聚合视图，服务“按批次查看”；
- 合并后一个 Controller 会同时承担任务执行和批次聚合，接口数量多、职责混杂；
- 分开后前端调用路径也更直观：批次列表用 `/batches`，任务办理用 `/tasks`。

> 结论：`InspectionBatchController` 不是重复控制器，它承担“批次视图”的聚合查询；不要删除。

---

## 2. 最终接口清单

### 2.1 检验批次（只读）

| 接口 | 方法 | 用途 | 前端是否使用 |
|---|---|---|---|
| `/api/inspection/batches` | GET | 分页查询批次，返回每批次的任务列表 | 使用，默认列表 |
| `/api/inspection/batches/stats` | GET | 顶部 3 个指标 + 六步环节计数 | 使用 |
| `/api/inspection/batches/{workOrderId}` | GET | 批次详情，**已包含 `tasks[]`** | 使用，打开批次抽屉 |

### 2.2 检验任务

| 接口 | 方法 | 用途 | 前端是否使用 |
|---|---|---|---|
| `/api/inspection/tasks` | GET | 任务模式列表 | “按任务”视图使用 |
| `/api/inspection/tasks/{id}` | GET | 任务六步详情 | 打开任务抽屉使用 |
| `/api/inspection/tasks` | POST | 任务初始化 | 仅旧任务初始化流程使用；正常流程由请检单自动生成任务 |
| `/api/inspection/tasks/{id}/plan` | POST | 办理检验计划 | 使用 |
| `/api/inspection/tasks/{id}/request` | POST | 办理请检 | **本页不使用**，请检由原请检页面创建 |
| `/api/inspection/tasks/{id}/sampling` | POST | 办理取样 | 使用 |
| `/api/inspection/tasks/{id}/inspection` | POST | 办理检验记录 | 使用 |
| `/api/inspection/tasks/{id}/retention` | POST | 办理留样 | 使用 |
| `/api/inspection/tasks/{id}/destruction` | POST | 办理销毁 | 按需使用，销毁默认非必须 |
| `/api/inspection/tasks/{id}/skip` | POST | 跳过留样/销毁 | 按配置使用 |
| `/api/inspection/tasks/{id}/cancel` | POST | 作废任务 | 按权限使用 |
| `/api/inspection/tasks/{id}/archive` | POST | 归档任务（终态，不可解除） | 按权限使用 |

> 步骤办理请求可携带可选字段 `nextStep`（`REQUEST/PLAN/SAMPLING/INSPECTION/RETENTION/DESTRUCTION/DONE`）指定办理后的目标环节；不传时按固定顺序进入下一环节。留样、销毁不再根据 `retentionRequired` / `destructionRequired` 自动跳过。

### 2.3 已删除的冗余接口

以下接口已删除：

```text
GET /api/inspection/batches/{workOrderId}/tasks
```

原因：`GET /api/inspection/batches/{workOrderId}` 已经返回 `BatchSummary.tasks[]`，再提供 `/tasks` 会造成两个接口返回同一份数据，前端也容易不知道该用哪个。

前端统一使用：

```text
GET /api/inspection/batches/{workOrderId}   // 批次头 + tasks 一次取回
```

如果只需要刷新任务列表，可以直接使用批次详情返回的 `tasks`，不需要额外接口。

---

## 3. 推荐的前端调用顺序

### 3.1 进入页面

并行请求：

```ts
const [stats, page] = await Promise.all([
  fetchInspectionBoardStats({ varietyCode, batchNo, keyword }),
  fetchInspectionBatches({ varietyCode, batchNo, itemCategory, status, keyword, pageIndex: 1, pageSize: 20 })
])
```

- 顶部指标使用 `stats`。
- 列表使用 `page.items`。
- 不要把 `step` 和 `status` 传给 `stats`，否则顶部 KPI 会跟着列表筛选变化。

### 3.2 点击批次

```ts
const batch = await fetchInspectionBatch(workOrderId)
// batch.tasks 已经包含该批次下全部检验任务
openBatchDrawer(batch)
```

不要再调用 `/batches/{workOrderId}/tasks`。

### 3.3 点击任务

```ts
const detail = await fetchInspectionTask(taskId)
// detail.steps 是六步明细
openTaskDrawer(detail)
```

### 3.4 办理某一步

```ts
await completeInspectionStep(taskId, 'plan', formData)
// 成功后刷新：
await Promise.all([
  loadStats(),
  refreshBatchList(),
  refreshActiveBatch(),
  refreshActiveTask()
])
```

步骤接口后缀：

```text
plan | request | sampling | inspection | retention | destruction
```

---

## 4. TypeScript API 模块

```ts
import request from '@/utils/http'

export interface InspectionBatchQuery {
  varietyCode?: string
  batchNo?: string
  itemCategory?: string
  step?: string
  status?: string
  keyword?: string
  pageIndex: number
  pageSize: number
}

export interface InspectionBatchSummary {
  workOrderId: number
  workOrderCode?: string
  planNumber?: string
  preparationCode?: string
  preparationName?: string
  batchNo?: string
  spec?: string
  taskCount: number
  completedCount: number
  currentStep?: string
  batchStatus?: string
  inspectionConclusion?: string
  tasks: InspectionTaskSummary[]
}

export interface InspectionTaskSummary {
  id: number
  taskCode?: string
  workOrderId?: number
  workOrderCode?: string
  batchNo?: string
  itemCategory?: string
  objectName?: string
  currentStep?: string
  taskStatus?: string
  archiveTime?: string
  archiveBy?: number
  planCode?: string
  requestCode?: string
  samplingCode?: string
  inspectionCode?: string
  retentionCode?: string
  destructionCode?: string
  inspectionConclusion?: string
}

export interface InspectionBoardStats {
  batchTotal: number
  todoBatches: number
  inspectingBatches: number
  completedBatches: number
  unqualifiedBatches: number
  taskTotal: number
  todoTasks: number
  inspectingTasks: number
  completedTasks: number
  stepCounts: Array<{
    stepCode: string
    stepName: string
    batchCount: number
    taskCount: number
  }>
}

export interface InspectionTaskDetail {
  id: number
  taskCode?: string
  workOrderId?: number
  workOrderCode?: string
  planNumber?: string
  preparationName?: string
  batchNo?: string
  itemCategory?: string
  objectName?: string
  spec?: string
  currentStep?: string
  taskStatus?: string
  archiveTime?: string
  archiveBy?: number
  retentionRequired?: number
  destructionRequired?: number
  steps: InspectionStepDetail[]
}

export interface InspectionStepDetail {
  stepCode: string
  stepName: string
  stepStatus: string
  refId?: number
  code?: string
  operatorName?: string
  startTime?: string
  completeTime?: string
  skipReason?: string
  fields: Array<{ key: string; label: string; value: string }>
  files: Array<{
    fileId: number
    name: string
    size?: number
    time?: string
    uploader?: string
  }>
}

export function fetchInspectionBatches(params: InspectionBatchQuery) {
  const pageIndex = Math.max(0, (params.pageIndex ?? 1) - 1)
  return request.get<{
    items: InspectionBatchSummary[]
    totalCount: number
    pageIndex: number
    pageSize: number
  }>({
    url: '/api/inspection/batches',
    params: { ...params, pageIndex }
  })
}

export function fetchInspectionBoardStats(
  params: Omit<InspectionBatchQuery, 'pageIndex' | 'pageSize' | 'step'>
) {
  return request.get<InspectionBoardStats>({
    url: '/api/inspection/batches/stats',
    params
  })
}

export function fetchInspectionBatch(workOrderId: number) {
  return request.get<InspectionBatchSummary>({
    url: `/api/inspection/batches/${workOrderId}`
  })
}

export function fetchInspectionTasks(params: InspectionBatchQuery) {
  const pageIndex = Math.max(0, (params.pageIndex ?? 1) - 1)
  return request.get<{
    items: InspectionTaskSummary[]
    totalCount: number
    pageIndex: number
    pageSize: number
  }>({
    url: '/api/inspection/tasks',
    params: { ...params, pageIndex }
  })
}

export function fetchInspectionTask(id: number) {
  return request.get<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}`
  })
}

export function archiveInspectionTask(id: number) {
  return request.post<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}/archive`,
    showSuccessMessage: true
  })
}

export function completeInspectionStep(
  id: number,
  step: 'plan' | 'request' | 'sampling' | 'inspection' | 'retention' | 'destruction',
  data: Record<string, any>
) {
  return request.post<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}/${step}`,
    data,
    showSuccessMessage: true
  })
}
```

---

## 5. 页面字段映射

### 5.1 顶部指标

| 指标 | 字段 |
|---|---|
| 待办 | `stats.todoBatches` |
| 检验中 | `stats.inspectingBatches` |
| 已完成 | `stats.completedBatches` |

统计口径（任务与批次一致）：

| 指标 | 判定 |
|---|---|
| 待办 | 任务当前环节为 `REQUEST` 请检 / `PLAN` 检验计划 |
| 检验中 | 任务当前环节为 `SAMPLING` 取样 / `INSPECTION` 检验记录 / `RETENTION` 留样 / `DESTRUCTION` 销毁 |
| 已完成 | 任务当前环节为 `DONE`（六步全部办结） |

- 任务级指标（`taskTotal`、`todoTasks`、`inspectingTasks`、`completedTasks`）包含无工单的生产原料任务，已作废、已归档任务不计入。
- 批次级指标只统计挂工单的批次：批次内全部任务 `DONE` 为已完成；未全部完成且任一任务处于取样及以后环节为检验中；其余为待办。
- `stepCounts[].taskCount` 为该环节的任务数；`stepCounts[].batchCount` 为“批次内至少一条任务处于该环节”的批次数，与按环节筛选列表的结果一致。
- 六步环节计数不含 `DONE`，六步 `taskCount` 之和加 `completedTasks` 等于 `taskTotal`。

### 5.2 批次列表

| 列 | 字段 |
|---|---|
| 批次 / 品种 | `batchNo`、`preparationName`、`preparationCode` |
| 工单 / 计划 | `workOrderCode`、`planNumber` |
| 检验任务 | `taskCount`，按 `tasks[].itemCategory` 汇总 |
| 当前进度 | `tasks[].currentStep` |
| 检验结论 | `inspectionConclusion` |
| 状态 | `batchStatus` |
| 操作 | 打开批次详情 |

- 批次列表和批次详情默认不包含已归档任务；归档任务只在“按任务”视图按状态查看。

### 5.3 任务抽屉

- 顶部：`taskCode`、`batchNo`、`workOrderCode`、`objectName`、`itemCategory`。
- 六步：`steps[]`，用 `stepCode` 映射：
  - `REQUEST` 请检（原页面已创建，只读）
  - `PLAN` 检验计划
  - `SAMPLING` 取样
  - `INSPECTION` 检验记录
  - `RETENTION` 留样
  - `DESTRUCTION` 销毁（非必须）
- 已完成步骤：展示 `fields[]` 与 `files[]`。
- 当前步骤：按 `stepCode` 打开对应表单。
- 检验计划步骤的 `fields[]` 包含 `itemCategory`（被检品属性，取任务值）和 `inspectionType`（检验类型，后端按被检品属性推导）。前端不要把 `inspectionType` 当作被检品属性，也不要自行传入。

### 5.4 任务状态与归档

任务状态：

| 状态 | 含义 | 是否可办理 |
|---|---|---|
| 待开始 | 已创建，尚未办结任何环节 | 是 |
| 进行中 | 已办结至少一个环节，未全部办结 | 是 |
| 已完成 | 六步全部办结 | 否 |
| 已作废 | 人工作废 | 否 |
| 已归档 | 不必需执行，已收档（终态） | 否 |

归档规则：

- 只有 `待开始`、`进行中` 的任务可以归档；`已完成`、`已作废`、`已归档` 任务不可归档。
- 调用 `POST /api/inspection/tasks/{id}/archive`，无请求体；归档后返回任务详情，`taskStatus='已归档'`，并带 `archiveTime`、`archiveBy`。
- 归档是终态，不提供解除归档；前端必须二次确认。
- 归档后任务只读：隐藏办理、跳过、作废入口。
- 默认任务列表不返回已归档任务；按状态筛选 `status=已归档` 可查看。
- 批次列表和批次详情不展示已归档任务；全归档批次在批次视图中不可见，归档任务请在“按任务”视图查看。
- 顶部 KPI 不计入已归档任务。

---

## 6. 请检的特殊约定

**请检不在本页创建，检验任务由后端在请检单创建后自动生成。**

- 请检单由原请检页面创建并写入 `inspection_request`。
- 请检单保存成功后，后端在同一事务内自动生成检验任务（一张请检单对应一张任务），完成“请检”环节并推进到“检验计划”；本页查询批次/任务即可看到，无需额外初始化。
- 同一工单+品类+批号下允许多条任务：同一批次的中间产品在不同工序分别请检时，每张请检单各自生成一条任务。
- 请检单填写了库中已存在的检验计划编号时，任务自动关联该计划并直接推进到“取样”。
- 生产原料请检单不关联工单，同样自动生成任务（工单字段为空），但此类任务只在“按任务”视图展示；批次视图按工单聚合，不包含它们。
- 本页不提供“发起请检”按钮，也不要调用 `POST /api/inspection/tasks/{id}/request` 作为请检入口（该接口仅保留给“检验任务页先行建任务”的旧流程）。
- 仅当请检单既无 `workOrderId`、`workOrderCode` 也匹配不到工单时，后端跳过任务生成，请检单本身仍会保存。

---

## 7. 控制器维护建议

- **保留** `InspectionBatchController`：它提供批次列表、统计、批次详情，是“按批次查看”的只读聚合层。
- **保留** `InspectionTaskController`：它负责任务执行和六步状态推进。
- **不要合并**：两者资源边界清晰，合并会让一个 Controller 同时承担查询聚合和状态变更。
- **已删除** `GET /api/inspection/batches/{workOrderId}/tasks`：批次详情已包含 `tasks[]`，该接口冗余。
- 后续如果新增“文件中心”，建议单独增加文件聚合接口，不要塞进检验任务控制器。

---

## 8. 联调验收清单

- [ ] 批次列表和 `stats` 并行加载，顶部指标与列表一致。
- [ ] 点击批次只调用 `/batches/{workOrderId}`，不再调用 `/tasks` 子接口。
- [ ] 批次详情能看到同批次下成品、中间产品、生产原料多条任务。
- [ ] 任务详情六步顺序为：请检（原页面）→ 检验计划 → 取样 → 检验记录 → 留样 → 销毁（非必）。
- [ ] 本页没有“发起请检”入口。
- [ ] 办理检验计划、取样、检验记录、留样、销毁后，批次详情、任务详情、列表、统计同时刷新。
- [ ] 留样完成后任务进入销毁；销毁非必须时需显式跳过或传 `nextStep=DONE` 才会完成任务。
- [ ] 附件通过 `steps[].files[].fileId` 预览/下载，不额外拼业务查询。
- [ ] 归档按钮仅对待开始/进行中任务显示，二次确认后任务变为已归档并从默认列表/看板消失；按状态“已归档”可查到且只读。

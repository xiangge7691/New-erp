# 检验管理 / 审核放行 —— 前端实现指导

> 适用项目：`C:\Work\Java\art-design-pro-main`  
> 后端接口：已提交到 `New-erp` 的 `v1.0`，接口清单见本文第 3 节  
> 原型：`New-erp/_analysis/prototypes/inspection-management.html`、`release-review.html`  
> 测试库：`8.134.48.54/erp_db`；**禁止向 `8.148.27.237` 写测试数据**

---

## 0. 结论先行

这两个页面不是现有 CRUD 页面的翻版，而是“以批次为单位”的聚合工作台：

| 页面 | 核心问题 | 默认视图 |
|---|---|---|
| 检验管理 | 这个批次有哪些检验任务，现在该办理哪一步？ | 按批次列表 → 批次详情 → 六步办理 |
| 审核放行 | 这个批次能不能放行，卡在哪一项？ | 批次列表 → 合规结论 → 证据/文件 → 放行或拒绝 |

现有 `src/views/release_review` 是 `release_review` 单表的增删改查；新页面使用 `/api/release-review/batches`，应新建 `src/views/release_batch`，不要直接改造旧页面的 CRUD 表单。  
旧页面主键是 `reviewId`，新页面主键是 `workOrderId`，不要混用；旧 API 路径是 `/api/releaseReview`，新页面不要调用它。  
现有 `inspection_plan / inspection_request / inspection_record` 继续作为独立业务单据页面；新页面 `src/views/inspection_task` 负责把它们串成六步任务。

建议实施顺序：

1. 新建两个 API 模块和类型定义；
2. 新建路由、菜单、权限标识；
3. 先做检验管理列表 + 批次抽屉；
4. 再做六步办理表单；
5. 最后做审核放行列表 + 合规详情 + 放行/拒绝；
6. 用测试库联调，重点验证“刷新后状态和统计同步”。

---

## 1. 页面骨架

两个页面统一采用 `art-design-pro` 现有风格：

```text
页面标题 + 一句话说明                         [主操作]
┌ 3 个关键指标卡 ─────────────────────────────────────┐
│ 检验：待办 / 检验中 / 已完成                         │
│ 放行：待审核 / 可放行 / 存在阻塞                     │
└──────────────────────────────────────────────────────┘
┌ 筛选栏：品种 / 批号 / 状态 / 关键字 [查询] [重置] ──┐
└──────────────────────────────────────────────────────┘
┌ 主列表（ArtTable，一行一个批次） ───────────────────┐
│ 批次/品种 | 工单/计划 | 任务/进度 | 结论 | 状态 | 操作 │
└──────────────────────────────────────────────────────┘
点击批次 → ElDrawer（批次详情）
  [批次头：品种 / 规格 / 批号 / 工单 / 数量 / 状态]
  [页签或分区：概览 / 证据 / 文件]
  [底部固定操作：办理 / 保存 / 放行 / 拒绝]
```

页面宽度、卡片间距、按钮尺寸直接复用现有 `ElCard shadow="never"`、`ArtTable`、`ArtTableHeader`、`ElTag`，不要另起一套视觉样式。

---

## 2. 目录、路由、权限

### 2.1 新增目录

```text
src/api/inspectionTask.ts
src/api/releaseBatch.ts

src/views/inspection_task/
├── index.vue
├── components/
│   ├── InspectionBatchDrawer.vue
│   ├── InspectionTaskStepDrawer.vue
│   ├── InspectionTaskCreateDialog.vue
│   └── InspectionStepFormDialog.vue
└── utils/constants.ts

src/views/release_batch/
├── index.vue
├── components/
│   ├── ReleaseBatchDrawer.vue
│   ├── ReleaseCompliancePanel.vue
│   ├── ReleaseProcessCheckTable.vue
│   ├── ReleaseFileChecklist.vue
│   └── ReleaseApproveDialog.vue
└── utils/constants.ts
```

### 2.2 路由模块

新增 `src/router/modules/quality.ts`，建议放在“质量管理”父菜单下：

```ts
import { AppRouteRecord } from '@/types/router'

export const qualityRoutes: AppRouteRecord = {
  path: '/quality',
  name: 'QualityManagement',
  component: '/index/index',
  meta: {
    title: '质量管理',
    icon: 'ri:shield-check-line',
    roles: ['R_SUPER', 'R_ADMIN']
  },
  children: [
    {
      path: 'inspection',
      name: 'InspectionManagement',
      component: '/inspection_task',
      meta: {
        title: '检验管理',
        icon: 'ri:test-tube-line',
        keepAlive: true,
        roles: ['R_SUPER', 'R_ADMIN'],
        authList: [
          { title: '查看', authMark: 'inspection_task_read' },
          { title: '新建', authMark: 'inspection_task_create' },
          { title: '办理', authMark: 'inspection_task_execute' },
          { title: '作废', authMark: 'inspection_task_cancel' }
        ]
      }
    },
    {
      path: 'release-batch',
      name: 'ReleaseBatch',
      component: '/release_batch',
      meta: {
        title: '审核放行',
        icon: 'ri:shield-check-line',
        keepAlive: true,
        roles: ['R_SUPER', 'R_ADMIN'],
        authList: [
          { title: '查看', authMark: 'release_batch_read' },
          { title: '工序检查', authMark: 'release_batch_check' },
          { title: '通过放行', authMark: 'release_batch_approve' },
          { title: '拒绝放行', authMark: 'release_batch_reject' }
        ]
      }
    }
  ]
}
```

然后在 `src/router/modules/index.ts` 的 `routeModules` 中加入 `qualityRoutes`。

如果项目使用**后端菜单模式**，还需要在系统菜单中增加对应菜单记录，`component` 分别填 `/inspection_task`、`/release_batch`，否则前端路由文件不会生效。

### 2.3 权限和菜单标题

- 页面按钮通过 `useAuth().hasAuth('inspection_task_execute')` 控制显示。
- 如果使用 `menus.business.*` 国际化键，需要在 `src/locales/langs/zh.json` 和 `en.json` 补键；不想补键时可以直接用中文 title，项目里的 `production-plan.ts` 已有这种写法。
- `keepAlive: true`，但抽屉里的临时筛选状态不要跨页面缓存，避免再次进入时保留旧批次。

---

## 3. API 模块

> `request.get<T>()` 已经由 `src/utils/http` 解包 `ApiResponse<T>`，**不要再取一次 `res.data`**。  
> 分页接口后端使用 0 基 `pageIndex`，API 模块负责把 `useTable` 的 1 基页码转成 0 基。

### 3.1 `src/api/inspectionTask.ts`

```ts
import request from '@/utils/http'

export type InspectionStepCode =
  | 'PLAN'
  | 'REQUEST'
  | 'SAMPLING'
  | 'RETENTION'
  | 'INSPECTION'
  | 'DESTRUCTION'

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

export interface InspectionTaskSummary {
  id: number
  taskCode?: string
  workOrderId?: number
  workOrderCode?: string
  planNumber?: string
  preparationCode?: string
  preparationName?: string
  batchNo?: string
  spec?: string
  itemCategory?: string
  objectName?: string
  currentStep?: InspectionStepCode | 'DONE'
  taskStatus?: string
  planCode?: string
  requestCode?: string
  samplingCode?: string
  retentionCode?: string
  inspectionCode?: string
  destructionCode?: string
  inspectionConclusion?: string
  retentionExpiryDate?: string
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

export interface InspectionStepCount {
  stepCode: string
  stepName: string
  batchCount: number
  taskCount: number
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
  stepCounts: InspectionStepCount[]
}

export interface InspectionTaskDetail {
  id: number
  taskCode?: string
  workOrderId?: number
  workOrderCode?: string
  planNumber?: string
  preparationCode?: string
  preparationName?: string
  batchNo?: string
  itemCategory?: string
  objectName?: string
  spec?: string
  currentStep?: string
  taskStatus?: string
  retentionRequired?: number
  destructionRequired?: number
  startTime?: string
  completeTime?: string
  remark?: string
  steps: InspectionStepDetail[]
}

export interface InspectionStepDetail {
  stepCode: string
  stepName: string
  stepStatus: string
  refId?: number
  refTable?: string
  code?: string
  operatorName?: string
  startTime?: string
  completeTime?: string
  skipReason?: string
  remark?: string
  fields: Array<{ key: string; label: string; value: string }>
  files: Array<{ fileId: number; name: string; size?: number; time?: string; uploader?: string }>
}

/** 分页查询批次 */
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

/** 看板统计，不分页 */
export function fetchInspectionBoardStats(
  params: Omit<InspectionBatchQuery, 'pageIndex' | 'pageSize' | 'step'>
) {
  return request.get<InspectionBoardStats>({
    url: '/api/inspection/batches/stats',
    params
  })
}

/** 单个批次汇总（含任务明细） */
export function fetchInspectionBatch(workOrderId: number) {
  return request.get<InspectionBatchSummary>({
    url: `/api/inspection/batches/${workOrderId}`
  })
}

/** 批次下全部任务 */
export function fetchInspectionBatchTasks(workOrderId: number) {
  return request.get<InspectionTaskSummary[]>({
    url: `/api/inspection/batches/${workOrderId}/tasks`
  })
}

/** 任务详情（六步） */
export function fetchInspectionTask(id: number) {
  return request.get<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}`
  })
}

/** 新建检验任务 */
export function createInspectionTask(data: {
  workOrderId: number
  itemCategory: string
  objectName?: string
  retentionRequired?: number
  destructionRequired?: number
  remark?: string
}) {
  return request.post<InspectionTaskDetail>({
    url: '/api/inspection/tasks',
    data,
    showSuccessMessage: true
  })
}

/** 办理六步中的某一步 */
export function completeInspectionStep(
  id: number,
  step: 'plan' | 'request' | 'sampling' | 'retention' | 'inspection' | 'destruction',
  data: Record<string, any>
) {
  return request.post<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}/${step}`,
    data,
    showSuccessMessage: true
  })
}

/** 跳过留样/销毁 */
export function skipInspectionStep(id: number, data: { stepCode: 'RETENTION' | 'DESTRUCTION'; reason: string }) {
  return request.post<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}/skip`,
    data,
    showSuccessMessage: true
  })
}

/** 作废任务 */
export function cancelInspectionTask(id: number, data: { reason: string }) {
  return request.post<InspectionTaskDetail>({
    url: `/api/inspection/tasks/${id}/cancel`,
    data,
    showSuccessMessage: true
  })
}
```

#### 六步请求字段

| 步骤 | 接口后缀 | 主要字段 |
|---|---|---|
| 请检 | `/request` | `inspectionCode`、`planCode`、`inspectionItemName`、`itemCategory`、`batchNumber`、`spec`、`processName`、`configQuantity`、`requestDepartment`、`requester`、`requestTime`、`inspectionItems`、`remark`、`fileIds` |
| 检验计划 | `/plan` | `planCode`、`planPeriod`、`inspectionType`、`objectName`、`batchNo`、`spec`、`inspectionSummary`、`planTime`、`remark`、`fileIds` |
| 取样 | `/sampling` | `samplingCode`、`location`、`quantity`、`count`、`method`、`sampler`、`samplingTime`、`remark`、`fileIds` |
| 检验记录 | `/inspection` | `inspectionCode`、`inspectionBasis`、`inspectionItem`、`inspector`、`reviewer`、`startTime`、`endTime`、`conclusion`、`remark`、`fileIds` |
| 留样 | `/retention` | `retainedCode`、`quantity`、`retainedDate`、`expiryDate`、`storageLocation`、`observationRecord`、`remark`、`fileIds` |
| 销毁（非必） | `/destruction` | `destructionCode`、`recordType`、`destroyReason`、`destroyMethod`、`destroyLocation`、`applicantName`、`applyDate`、`approverName`、`approveDate`、`destroyerName`、`destroyDate`、`supervisorName`、`superviseDate`、`remark`、`fileIds` |

### 3.2 `src/api/releaseBatch.ts`

```ts
import request from '@/utils/http'

export interface ReleaseBatchQuery {
  varietyCode?: string
  batchNo?: string
  status?: string
  keyword?: string
  pageIndex: number
  pageSize: number
}

export interface ReleaseBatchSummary {
  workOrderId: number
  workOrderCode?: string
  batchNo?: string
  preparationCode?: string
  preparationName?: string
  spec?: string
  unitName?: string
  planQty?: number
  finishedQty?: number
  planDate?: string
  actualStart?: string
  actualEnd?: string
  yieldRate?: number
  inspectionConclusion?: string
  processTotal: number
  processPass: number
  processFail: number
  processPending: number
  fileUploaded: number
  fileRequired: number
  fileMissing: number
  deviationTotal: number
  deviationOpen: number
  releaseStatus?: string
  canRelease: boolean
  /** 后端直接返回的阻塞项 */
  blockers: string[]
}

export interface ReleaseBoardStats {
  total: number
  pendingReview: number
  releasable: number
  blocked: number
  released: number
  rejected: number
}

export function fetchReleaseBatches(params: ReleaseBatchQuery) {
  const pageIndex = Math.max(0, (params.pageIndex ?? 1) - 1)
  return request.get<{
    items: ReleaseBatchSummary[]
    totalCount: number
    pageIndex: number
    pageSize: number
  }>({
    url: '/api/release-review/batches',
    params: { ...params, pageIndex }
  })
}

export function fetchReleaseBoardStats(
  params: Omit<ReleaseBatchQuery, 'pageIndex' | 'pageSize'>
) {
  return request.get<ReleaseBoardStats>({
    url: '/api/release-review/batches/stats',
    params
  })
}

export function fetchReleaseBatchDetail(workOrderId: number) {
  return request.get<Record<string, any>>({
    url: `/api/release-review/batches/${workOrderId}`
  })
}

export function saveReleaseProcessChecks(
  workOrderId: number,
  items: Array<{
    executionId: number
    processTypeId?: number
    processName?: string
    requirementSnapshot?: string
    checkResult: string
    inspectorId?: number
    inspectorName?: string
    checkDate?: string
    remark?: string
  }>
) {
  return request.post({
    url: `/api/release-review/batches/${workOrderId}/process-checks`,
    data: { items },
    showSuccessMessage: true
  })
}

export function approveRelease(
  workOrderId: number,
  data: { approver: string; reviewTime: string; opinion?: string }
) {
  return request.post({
    url: `/api/release-review/batches/${workOrderId}/approve`,
    data,
    showSuccessMessage: true
  })
}

export function rejectRelease(
  workOrderId: number,
  data: { reviewer: string; reviewTime: string; reason: string }
) {
  return request.post({
    url: `/api/release-review/batches/${workOrderId}/reject`,
    data,
    showSuccessMessage: true
  })
}
```

详情返回结构见 `ReleaseBatchDtos.BatchDetail`，前端可按以下分组渲染：

- `batch`：批次头信息
- `purchases`、`acceptances`、`instructions`、`requisitions`、`returns`：物料/生产单据
- `processes`：工序记录与符合性
- `requests`、`samplings`、`inspections`：检验证据
- `yieldInfo`、`deviations`：收率与偏差
- `fileChecklist`：文件完整性清单（只有计数，不直接带文件 ID）
- `compliance`：`inspectionPass / processPass / filePass / deviationPass / yieldPass / canRelease / summary`
- `release`：已放行时的放行记录

---

## 4. 检验管理页实现

### 4.1 页面结构

文件：`src/views/inspection_task/index.vue`

- 顶部 3 个 `ElCard`/统计卡：待办、检验中、已完成。
- 筛选卡：品种、检验对象、批号/工单/请检单号，查询/重置。
- 环节筛选：一行紧凑步骤条，数据来自 `stats.stepCounts`，点击后设置 `step` 并刷新列表。
- 列表卡：
  - `ArtTableHeader` 左侧放“新建检验任务”，右侧刷新。
  - 视图切换：`按批次`（默认）/ `按任务`。
  - `ArtTable` 行点击打开批次抽屉。

### 4.2 列表列建议

按批次：

| 列 | 字段 |
|---|---|
| 批次 / 品种 | `batchNo`、`preparationName`、`preparationCode` |
| 工单 / 计划 | `workOrderCode`、`planNumber` |
| 检验任务 | `taskCount`，副行按 `tasks[].itemCategory` 汇总 |
| 当前进度 | `tasks[].currentStep`，用小标签展示“成品·检验 / 中间产品·取样” |
| 检验结论 | `inspectionConclusion` |
| 状态 | `batchStatus` |
| 操作 | 查看批次 |

按任务：

| 列 | 字段 |
|---|---|
| 批次 / 品种 | `batchNo`、`preparationName` |
| 检验对象 | `itemCategory`、`objectName` |
| 当前环节 | `currentStep` |
| 计划 / 请检 | `planCode`、`requestCode` |
| 取样 / 留样 | `samplingCode`、`retentionCode` |
| 检验 | `inspectionConclusion`、`inspectionCode` |
| 状态 | `taskStatus` |
| 操作 | 办理/查看 |

### 4.3 批次抽屉

`InspectionBatchDrawer.vue` 接收 `workOrderId`，打开时调用 `fetchInspectionBatch(workOrderId)`：

- 顶部：批号、品种、规格、工单、计划、任务数、批次状态。
- 任务列表：一行一个 `tasks[]`，展示被检品属性、当前环节、结论、操作。
- 点击任务后：
  - 调用 `fetchInspectionTask(task.id)`；
  - 在抽屉内展示六步时间轴；
  - 已完成步骤显示 `steps[].fields` 和 `steps[].files`；
  - 当前步骤显示“办理”按钮。
- 建议抽屉宽度 `70%`，`destroy-on-close`，避免状态残留。

### 4.4 六步办理

`InspectionStepFormDialog.vue` 根据 `stepCode` 渲染不同表单：

- `REQUEST` / `PLAN` / `SAMPLING` / `INSPECTION` / `RETENTION` / `DESTRUCTION` 分别对应 3.1 的请求字段；销毁步骤非必须。
- 已完成的步骤只读展示 `steps[].fields`；不要重新提交。
- 附件：步骤提交前如果业务单据尚未创建，使用 `uploadTempBusinessFile` 获取 `fileId`，把 ID 收集到表单的 `fileIds`；提交步骤时一并传给后端。
- 留样/销毁：
  - `task.retentionRequired === 0` 时，检验记录完成后显示“跳过留样”，调用 `skipInspectionStep(id, { stepCode: 'RETENTION', reason })`；
  - `task.destructionRequired === 0` 时，后端在留样完成后自动跳过销毁，前端步骤显示“不必办理”，不需要用户再点按钮；
  - 如果 `destructionRequired === 1`，当前环节进入销毁后仍允许“跳过销毁”，调用 `skipInspectionStep(id, { stepCode: 'DESTRUCTION', reason })`；
  - 跳过必须填写原因。
- 作废：单独入口，调用 `cancelInspectionTask`，填写原因并二次确认。

### 4.5 新建检验任务

`InspectionTaskCreateDialog.vue`：

- 生产任务：复用 `fetchSearchWorkOrders` 远程搜索，按工单号/制剂名称过滤。
- 被检品属性：生产原料 / 中间产品 / 成品。
- 检验对象名称：默认取工单/制剂名称，可修改。
- 留样：默认要求，传 `1`；销毁：默认非必须，传 `0`。需要销毁的批次再勾选。
- 创建成功后关闭弹窗，刷新列表和统计；如果后端返回已有任务，提示“该批次已存在同属性任务”并直接打开该任务。

### 4.6 刷新规则

每次办理成功、跳过、作废、创建后：

```ts
await Promise.all([
  loadStats(),                 // 重新拉看板统计
  refreshData(),               // 重新拉当前页列表
  refreshActiveBatch(),        // 重新拉批次详情
  refreshActiveTask()          // 重新拉任务六步
])
```

不要只刷新当前表格；统计、批次状态、任务当前步骤必须同步，否则页面会出现“列表已推进但顶部指标没变”。

---

## 5. 审核放行页实现

### 5.1 页面结构

文件：`src/views/release_batch/index.vue`

- 顶部 3 个指标：待审核、可放行、存在阻塞。
- 筛选卡：品种、批号/工单、放行状态。
- 列表卡：`ArtTable` + `ArtTableHeader`。
- 行点击打开 `ReleaseBatchDrawer.vue`。

### 5.2 列表列

| 列 | 字段 |
|---|---|
| 批次 / 品种 | `batchNo`、`preparationName`、`preparationCode` |
| 检验结论 | `inspectionConclusion` |
| 工序符合 | `processPass / processTotal`，副行显示不符合/待查 |
| 文件完整 | `fileUploaded / fileRequired`，副行显示 `fileMissing` |
| 阻塞项 | `blockers[]`，红色 `ElTag`；为空显示绿色“无阻塞” |
| 放行状态 | `releaseStatus` |
| 操作 | 审核/查看 |

`blockers` 已由后端计算，前端不要再逐个调详情推导：

```ts
// 直接渲染
row.blockers.map((item) => h(ElTag, { type: 'danger', size: 'small' }, () => item))
```

### 5.3 批次详情抽屉

`ReleaseBatchDrawer.vue` 接收 `workOrderId`，打开时调用 `fetchReleaseBatchDetail`。

顶部批次头：

- 品种、规格、批号、工单、批量/成品、计划/实际生产日期、收率。
- 合规结论：`compliance.canRelease ? '可放行' : '存在阻塞'`。
- 检验、工序、文件的通过情况。
- 底部固定“通过放行 / 拒绝放行”，根据权限和 `canRelease` 禁用。

建议详情分三个页签，避免再次堆成十几块：

1. **合规结论**
   - `compliance` 五项检查：检验、工序、文件、偏差、收率。
   - 阻塞项列表（优先展示 `blockers` 或从 `compliance` 汇总）。
2. **证据**
   - 物料/采购/验收
   - 生产指令/领料/退库
   - 工序记录与符合性
   - 检验记录
   - 收率/偏差
3. **文件**
   - `fileChecklist`：按 `stage`、`label`、`requirement`、`uploaded/total`、`note` 展示。
   - `requirement === 'OPTIONAL'` 统一放到“不必传”分组，不进入缺件数。
   - 文件清单只有计数；需要预览具体文件时，使用证据区的 `files[]`（含 `fileId`），调用现有 `previewFile` / `downloadFile` 或 `BusinessFilePreviewDialog`。
   - 不要为 `fileChecklist` 的每一行虚构 `bizId`；如果产品要求“清单行直接点开全部文件”，再让后端在 `FileChecklistItem` 中补充 `fileIds`。

### 5.4 工序符合性

`ReleaseProcessCheckTable.vue`：

- 数据来源：`detail.processes[]`。
- 列：工序、要求、结果、查证人、日期、附件。
- 结果用 `ElSelect`：符合 / 不符合 / 待查。
- 保存时组装：

```ts
await saveReleaseProcessChecks(workOrderId, detail.processes.map((p) => ({
  executionId: p.executionId,
  processTypeId: p.processTypeId,
  processName: p.processName,
  requirementSnapshot: p.requirement,
  checkResult: form[p.executionId].result,
  inspectorName: form[p.executionId].inspector,
  checkDate: form[p.executionId].date,
  remark: form[p.executionId].remark
})))
```

保存成功后刷新详情、列表和统计，因为 `processPass`、`blockers`、`canRelease` 都会变化。

### 5.5 放行 / 拒绝

- 通过放行：
  - 先校验 `detail.compliance.canRelease === true`，否则禁用按钮并展示阻塞项。
  - 弹窗字段：批准人、审核时间、审核意见。
  - 调用 `approveRelease(workOrderId, { approver, reviewTime, opinion })`。
- 拒绝放行：
  - 弹窗字段：审核人、审核时间、拒绝原因，原因必填。
  - 调用 `rejectRelease(workOrderId, { reviewer, reviewTime, reason })`。
- 成功后刷新详情、列表、统计；关闭抽屉或保留详情显示最新结论。

---

## 6. 两个页面共用的实现约定

### 6.1 `useTable` 配置

```ts
const { columns, columnChecks, data, loading, pagination, getData, searchParams,
  handleSizeChange, handleCurrentChange, refreshData } = useTable({
  core: {
    apiFn: fetchInspectionBatches,
    apiParams: { pageIndex: 1, pageSize: 20 },
    paginationKey: { current: 'pageIndex', size: 'pageSize' },
    columnsFactory: () => [/* ... */]
  },
  transform: {
    responseAdapter: (res: any) => ({
      records: res.items,
      total: res.totalCount ?? 0,
      current: res.pageIndex != null ? res.pageIndex + 1 : undefined,
      size: res.pageSize
    })
  }
})
```

列表页的统计不要塞进 `useTable`；单独 `fetchInspectionBoardStats` / `fetchReleaseBoardStats`，查询时与列表并行请求：

```ts
await Promise.all([loadStats(), getData()])
```

顶部 KPI 默认**不要传 `step`（检验环节）和 `status`（放行/任务状态）**，否则顶部数字会跟着列表筛选变化；品种、批号、关键字是否传给 KPI 可按产品口径决定。若产品明确要求“顶部随筛选联动”，再统一传入。

### 6.2 状态标签

统一使用 `ElTag`：

| 状态 | 颜色 |
|---|---|
| 待开始 / 待审核 / 进行中 | warning / info |
| 检验中 / 待放行 | primary |
| 待销毁 | warning |
| 已完成 / 已放行 | success |
| 不合格 / 已拒绝 / 存在阻塞 | danger |

检验当前环节映射：

```ts
const STEP_LABEL: Record<string, string> = {
  REQUEST: '请检',
  PLAN: '检验计划',
  SAMPLING: '取样',
  INSPECTION: '检验记录',
  RETENTION: '留样',
  DESTRUCTION: '销毁（非必）',
  DONE: '已完成'
}
```

### 6.3 日期和分页

- `LocalDate` 字段用 `YYYY-MM-DD`。
- `LocalDateTime` 字段用 `YYYY-MM-DD HH:mm:ss`。
- 后端 `pageIndex` 从 0 开始；`useTable` 的 `pagination.current` 从 1 开始，API 模块统一转换。
- 查询条件变化时把页码重置到 1；刷新当前页时保持页码。

### 6.4 附件

- 检验步骤：`uploadTempBusinessFile` → 收集 `fileId` → 提交步骤请求的 `fileIds`。
- 检验步骤已生成文件：`StepDetail.files[]` 已包含 `fileId`，直接用 `previewFile` / `downloadFile`。
- 放行详情：证据区 `files[]` 含 `fileId`；文件完整性清单只用于展示“应传/已传/缺”。
- 统一使用项目现有的 `BusinessFilePreviewDialog` 或 `src/api/files.ts` 的预览/下载方法，不要另写一套文件服务。

### 6.5 权限

```ts
const { hasAuth } = useAuth()
```

- 检验管理：`inspection_task_create`、`inspection_task_execute`、`inspection_task_cancel`。
- 审核放行：`release_batch_check`、`release_batch_approve`、`release_batch_reject`。
- 没权限的按钮不渲染；不要只依赖后端 403。
- 如果项目使用 `v-auth` 指令，也可在模板上使用对应标识，但同一页面统一一种方式。

### 6.6 交互和错误处理

- `request` 已统一抛错并提示；业务代码只在需要补充上下文时再 `ElMessage.error`，避免重复弹窗。
- 抽屉打开时显示 `v-loading`；空数据用 `ElEmpty`。
- 放行/拒绝、作废、跳过必须二次确认。
- 保存工序检查、办理步骤时按钮加 `loading`，防止重复提交。
- 刷新后保留当前筛选条件和批次抽屉；不要每次回列表页。

---

## 7. 联调顺序

1. 启动 `New-erp`，确认 `/api/inspection/batches/stats`、`/api/release-review/batches/stats` 返回 200。
2. 先完成 API 模块，用 Postman/浏览器控制台核对 `items`、`totalCount`、`blockers` 字段。
3. 检验管理：先做批次列表和统计，再做批次抽屉，最后做六步表单。
4. 审核放行：先做列表和阻塞项，再做详情合规页，最后接工序检查、放行、拒绝。
5. 用 `8.134.48.54/erp_db` 的测试批次联调，不要向 `8.148.27.237` 写入测试数据。
6. 重点回归：办理一步后列表、统计、批次详情、任务详情是否同时更新；放行后 `blockers` 和 `canRelease` 是否同步变化。

## 8. 验收清单

### 检验管理

- [ ] 顶部待办/检验中/已完成与批次列表一致。
- [ ] 环节筛选计数来自 `stats.stepCounts`，切换环节后列表正确刷新。
- [ ] 按批次/按任务切换不丢失筛选条件。
- [ ] 批次抽屉能显示多条成品/中间产品/生产原料任务。
- [ ] 六步每一步能提交，提交后当前步骤推进。
- [ ] 流程顺序为请检 → 检验计划 → 取样 → 检验记录 → 留样 → 销毁；销毁默认非必须。
- [ ] 留样/销毁可以按配置跳过，必须填写原因。
- [ ] 作废有二次确认。
- [ ] 新建任务重复时能给出明确提示或打开已有任务。
- [ ] 附件预览/下载正常。

### 审核放行

- [ ] 顶部待审核/可放行/存在阻塞与列表一致。
- [ ] 列表阻塞项直接来自 `blockers`，不是前端逐条查详情。
- [ ] 详情合规五项与 `compliance` 一致。
- [ ] 工序检查保存后 `processPass`、`blockers`、`canRelease` 同步变化。
- [ ] 不满足合规时“通过放行”禁用。
- [ ] 通过/拒绝成功后列表和统计刷新。
- [ ] 文件完整性区分必传/条件必传/不必传，缺件数不含不必传。
- [ ] 证据区文件可预览/下载。

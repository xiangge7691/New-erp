# 产品信息汇总前端实现与接口对接指南

> 适用项目：`C:\Work\Java\art-design-pro-main`  
> 后端接口：`/api/product-summary/*`  
> 原型：`New-erp/_analysis/prototypes/product-summary.html`  
> 接口方案：`docs/product-summary-api-and-export-plan.md`  
> 测试库：`8.134.48.54/erp_db`；禁止向 `8.148.27.237` 写测试数据

---

## 1. 页面定位

产品信息汇总不是单纯的批次列表，而是一个“总览 + 筛选 + 批次下钻 + 文件导出”的工作台。

页面从上到下：

1. 一句话结论条
2. 4 个 KPI
3. 优先处理 + 文件完整度（左右两个面板）
4. 按制剂筛选的卡片
5. 批次清单 / 品种卡片视图
6. 批次详情抽屉（链路明细、附件、缺件）
7. 集中导出（筛选结果 / 选中记录 / 全部）

建议新建独立页面，不要改现有 `production_plan` 或 `release_review` 页面。

---

## 2. 目录与路由

### 2.1 建议目录

```text
src/api/productSummary.ts

src/views/product_summary/
├── index.vue
└── components/
    ├── SummaryVerdict.vue
    ├── SummaryKpis.vue
    ├── SummaryTodoPanel.vue
    ├── SummaryHealthPanel.vue
    ├── PreparationCards.vue
    ├── BatchListTable.vue
    ├── BatchCardView.vue
    ├── ProductSummaryDrawer.vue
    └── ProductSummaryExportDialog.vue
```

如果页面逻辑不大，也可以先写在 `index.vue`，但 API 模块和导出弹窗建议独立。

### 2.2 路由模块

新增 `src/router/modules/product-summary.ts`：

```ts
import { AppRouteRecord } from '@/types/router'

export const productSummaryRoutes: AppRouteRecord = {
  path: '/product-summary',
  name: 'ProductSummary',
  component: '/product_summary',
  meta: {
    title: '产品信息汇总',
    icon: 'ri:file-chart-line',
    keepAlive: true,
    roles: ['R_SUPER', 'R_ADMIN'],
    authList: [
      { title: '查看', authMark: 'product_summary_read' },
      { title: '导出', authMark: 'product_summary_export' }
    ]
  }
}
```

在 `src/router/modules/index.ts` 的 `routeModules` 中加入 `productSummaryRoutes`。

如果项目使用后端菜单模式，还需要在系统菜单里增加 `component=/product_summary` 的菜单记录。

---

## 3. API 模块

新建 `src/api/productSummary.ts`。

> `request.get<T>()` 已经由 `src/utils/http` 解包 `ApiResponse<T>`，不要再取一次 `.data`。  
> 后端分页 `pageIndex` 从 0 开始，API 模块负责把页面 1 基页码转成 0 基。

```ts
import request from '@/utils/http'

export interface ProductSummaryFilters {
  varietyCode?: string
  dateType?: string
  startDate?: string
  endDate?: string
  keyword?: string
  onlyMissing?: boolean
}

export interface ProductSummaryBatchQuery extends ProductSummaryFilters {
  pageIndex: number
  pageSize: number
}

export interface StageState {
  key: string
  label: string
  state: string
  skipReason?: string
}

export interface ProductSummaryBatch {
  recordKey: string
  workOrderId?: number | null
  workOrderCode?: string | null
  planId?: number | null
  planNumber?: string
  preparationCode?: string
  preparationName?: string
  spec?: string
  unitName?: string
  batchNo?: string | null
  planQty?: number
  finishedQty?: number
  inboundQty?: number
  outboundQty?: number
  materialSource?: string
  planDate?: string
  workOrderDate?: string
  inboundDate?: string
  outboundDate?: string
  inspectionDate?: string
  productionStart?: string
  productionEnd?: string
  yieldRate?: number
  inspectionConclusion?: string
  latestInspectionCode?: string
  processTotal?: number
  processCompleted?: number
  status?: string
  productionDone: boolean
  fileCount: number
  fileExpected: number
  fileUploaded: number
  missingCount: number
  missingSummary?: string
  stageStates: StageState[]
}

export interface ProductSummaryOverview {
  generatedAt: string
  verdict: {
    varietyCount: number
    recordCount: number
    batchCount: number
    missingRecords: number
    missingItems: number
    topPriority?: ProductSummaryTodo | null
  }
  kpis: {
    runningBatches: number
    completedBatches: number
    totalBatches: number
    missingItems: number
    missingRecords: number
    fileExpected: number
    fileUploaded: number
    completenessRate: number
    inspectionTotal: number
    inspectionQualified: number
    inspectionRate?: number | null
  }
  todo: ProductSummaryTodo[]
  health: {
    stages: Array<{ stage: string; expected: number; uploaded: number; missing: number }>
    topSlots: Array<{ stage: string; label: string; requirement: string; expected: number; uploaded: number; missing: number }>
    allSlots: Array<{ stage: string; label: string; requirement: string; expected: number; uploaded: number; missing: number }>
  }
  fileChecklist: ProductSummaryFileChecklistItem[]
  preparations: Array<{
    preparationCode: string
    preparationName: string
    batchCount: number
    missingCount: number
  }>
}

export interface ProductSummaryTodo {
  recordKey: string
  planId?: number | null
  workOrderId?: number | null
  preparationName?: string
  code?: string
  batchNo?: string | null
  missingCount: number
  stageBrief?: string
}

export interface ProductSummaryFile {
  fileId: number
  name?: string
  size?: number
  time?: string
  uploader?: string
}

export interface ProductSummaryFileGroup {
  stage?: string
  nodeKey?: string
  label?: string
  bizType?: string
  fileKind?: string
  requirement?: string
  slotCode?: string
  files: ProductSummaryFile[]
}

/** 文件完整性清单：按环节/槽位返回应传、已传、缺失及单号对应文件 */
export interface ProductSummaryFileChecklistItem {
  nodeKey?: string
  stage?: string
  label?: string
  bizType?: string
  fileKind?: string
  slotCode?: string
  requirement?: 'REQUIRED' | 'CONDITIONAL' | 'OPTIONAL' | string
  total: number
  uploaded: number
  missing: number
  note?: string
  missingDetails: string[]
  documents: ProductSummaryFileChecklistDocument[]
}

/** 清单中的单据及该槽位下的文件 */
export interface ProductSummaryFileChecklistDocument {
  recordKey?: string
  workOrderCode?: string
  batchNo?: string | null
  docId?: number
  docCode?: string
  uploaded: boolean
  files: ProductSummaryFile[]
}

export interface ProductSummaryDocument {
  type?: string
  code?: string
  date?: string
  time?: string
  status?: string
  operator?: string
  supplier?: string
  customer?: string
  amount?: number
  lines: Array<Record<string, any>>
  files: ProductSummaryFile[]
}

export interface ProductSummaryStage {
  key: string
  label: string
  state: 'done' | 'active' | 'pending' | 'error' | 'skipped' | string
  skipped?: boolean
  skipReason?: string
  main?: string
  meta?: string
  documents: ProductSummaryDocument[]
}

/**
 * 链路节点固定为以下 10 个，后端始终按此顺序返回，不因数据缺失而省略：
 * plan(计划) → purchase(采购) → acceptance(验收入库) → workOrder(生产任务)
 * → pick(生产领料) → production(生产过程) → inspection(检验)
 * → release(审核放行) → finishedInbound(成品入库) → finishedOutbound(成品出库)
 */
export type ProductSummaryNodeKey =
  | 'plan'
  | 'purchase'
  | 'acceptance'
  | 'workOrder'
  | 'pick'
  | 'production'
  | 'inspection'
  | 'finishedInbound'
  | 'release'
  | 'finishedOutbound'

export interface ProductSummaryDetail {
  batch: ProductSummaryBatch
  slots: Array<{
    stage: string
    nodeKey?: string
    label: string
    bizType: string
    fileKind: string
    requirement: string
    expected: number
    uploaded: number
    missing: number
  }>
  stages: ProductSummaryStage[]
  files: ProductSummaryFileGroup[]
  missingItems: Array<{
    stage?: string
    nodeKey?: string
    label?: string
    doc?: string
    slotCode?: string
  }>
  fileChecklist: ProductSummaryFileChecklistItem[]
}

export interface ProductSummaryExportRequest {
  scope: 'SELECTED' | 'FILTERED' | 'ALL'
  workOrderIds?: number[]
  planIds?: number[]
  filters?: ProductSummaryFilters
  includeProductFiles?: boolean
  requirements?: string[]
  includeManifest?: boolean
  format?: 'ZIP' | 'CSV'
  fileName?: string
}

/** 页面聚合概览 */
export function fetchProductSummaryOverview(params: ProductSummaryFilters) {
  return request.get<ProductSummaryOverview>({
    url: '/api/product-summary/overview',
    params
  })
}

/** 批次分页列表 */
export function fetchProductSummaryBatches(params: ProductSummaryBatchQuery) {
  const pageIndex = Math.max(0, (params.pageIndex ?? 1) - 1)
  return request.get<{
    items: ProductSummaryBatch[]
    totalCount: number
    pageIndex: number
    pageSize: number
  }>({
    url: '/api/product-summary/batches',
    params: { ...params, pageIndex }
  })
}

/** 品种卡片视图（不分页） */
export function fetchProductSummaryCards(params: ProductSummaryFilters) {
  return request.get<ProductSummaryBatch[]>({
    url: '/api/product-summary/cards',
    params
  })
}

/** 已下达工单批次详情 */
export function fetchProductSummaryBatch(workOrderId: number) {
  return request.get<ProductSummaryDetail>({
    url: `/api/product-summary/batches/${workOrderId}`
  })
}

/** 未下达计划详情 */
export function fetchProductSummaryPlan(planId: number) {
  return request.get<ProductSummaryDetail>({
    url: `/api/product-summary/plans/${planId}`
  })
}

/** 集中导出，返回 Blob */
export function exportProductSummary(data: ProductSummaryExportRequest) {
  return request.request<Blob>({
    url: '/api/product-summary/export',
    method: 'POST',
    data,
    responseType: 'blob'
  })
}
```

---

## 4. 页面状态设计

`index.vue` 建议维护：

```ts
const filters = reactive<ProductSummaryFilters>({
  varietyCode: '',
  dateType: 'planDate',
  startDate: '',
  endDate: '',
  keyword: '',
  onlyMissing: false
})

const viewMode = ref<'list' | 'card'>('list')
const overview = ref<ProductSummaryOverview | null>(null)
const cards = ref<ProductSummaryBatch[]>([])
const overviewLoading = ref(false)
const cardsLoading = ref(false)
const drawerVisible = ref(false)
const detail = ref<ProductSummaryDetail | null>(null)
const detailLoading = ref(false)
const exportVisible = ref(false)
```

表格数据交给 `useTable` 管理；概览和卡片视图单独管理。

### 4.1 页面初始化

```ts
await Promise.all([loadOverview(), getData()])
```

- `loadOverview` 调 `/overview`。
- `getData` 调 `/batches`。
- 两个请求共用 `filters`。

### 4.2 筛选

筛选输入变化或点击查询时：

```ts
await Promise.all([loadOverview(), getData()])
```

- 页码重置到 1。
- 如果当前是卡片视图，重新请求 `/cards`。
- 制剂卡片点击只改变 `filters.varietyCode`，然后走同一套查询。

### 4.3 列表/卡片切换

- 列表：`useTable` + `/batches`。
- 卡片：`/cards`（不分页），避免对每个批次逐个查详情。
- 切换时保留筛选条件，不重置抽屉。

---

## 5. 页面组件与字段映射

### 5.1 结论条

输入：`overview.verdict`。

```text
当前 {varietyCount} 个品种、{recordCount} 条生产记录（含 {batchCount} 个已下达批次）
其中 {missingRecords} 条存在附件缺件，共 {missingItems} 项
最优先：{topPriority.preparationName}（{code}，缺 {missingCount} 项）
```

### 5.2 KPI

| 卡片 | 字段 |
|---|---|
| 在产批次 | `kpis.runningBatches` / `kpis.completedBatches` / `kpis.totalBatches` |
| 待处理缺件 | `kpis.missingItems` / `kpis.missingRecords` |
| 附件完整度 | `kpis.completenessRate` / `fileUploaded` / `fileExpected` |
| 检验合格率 | `kpis.inspectionRate` / `inspectionQualified` / `inspectionTotal` |

在产/完成口径：

- 工单状态流转为 待生产 → 生产中 → 已生产 → 检验中 → 已检验 → 已入库 → 已放行 → 已归档（页面链路展示：检验 → 审核放行 → 成品入库 → 成品出库）；
- **成品入库即视为生产完成**：`已入库`、`已放行`、`已归档` 计入 `completedBatches`，不计入 `runningBatches`；
- `异常`、`不合格`、`作废` 为终止状态，既不计入在产也不计入完成；
- **审核放行会把状态推进到 `已放行`**，不会因为已入库而被覆盖；已归档才进入终态；
- 列表/卡片行可用 `batch.productionDone` 判断是否展示为“已完成”；`status` 为按工单日期字段实时推导的有效状态；
- 批次卡片副标题建议显示为「在产 {runningBatches} · 已完成 {completedBatches} · 共 {totalBatches} 批」；
- `totalBatches`/`runningBatches`/`completedBatches` 只统计已下达工单的批次，未下达计划记录只计入 `verdict.recordCount` 与缺件统计，避免与「批次」口径混淆。

### 5.3 优先处理

输入：`overview.todo`。

每行：

- 排名
- `preparationName`
- `code` / `batchNo`
- `stageBrief`
- `missingCount`
- 点击用 `workOrderId` 或 `planId` 打开详情。

### 5.4 文件完整度

输入：`overview.health`。

- `stages`：左侧“重点环节”进度条。
- `topSlots`：右侧“缺件最集中的槽位”。
- `allSlots`：底部“查看全部槽位口径”折叠区。

注意：`health` 只包含必传/条件必传；`OPTIONAL` 已在后端过滤。

#### 5.4.1 按节点查看单号与文件

输入：`overview.fileChecklist`（跨记录按 环节 + 槽位 合并）或批次详情的 `fileChecklist`（单批次）。

| 字段 | 说明 |
|---|---|
| `nodeKey` / `stage` | 链路节点编码与环节名，用于按节点分组 |
| `label` | 槽位显示名，如「验收检验报告」 |
| `slotCode` | 槽位编码：`bizType / fileKind`，与 `missingItems.slotCode` 对应 |
| `requirement` | `REQUIRED` / `CONDITIONAL` / `OPTIONAL` |
| `total` / `uploaded` / `missing` | 应传单据数 / 已传单据数 / 缺件数 |
| `note` | 缺失提示，可直接展示 |
| `missingDetails[]` | 缺失明细，如「验收单 YS-20261019-005 缺 验收检验报告」 |
| `documents[]` | 该槽位下的单据，每条带 `docCode`（单号）与该槽位已上传的 `files[]` |

展示建议：

- 按 10 个节点分组，节点行显示应传/已传/缺 N；
- 展开节点后按 `label` 展示槽位，槽位下列出 `documents[]`：单号 + 已传文件（`files[]`，用 `fileId` 预览/下载）+ 缺失标记（`uploaded=false`）；
- `OPTIONAL` 行标「可空缺」，不计入缺件数；
- 点单号可打开对应批次详情抽屉；`recordKey` 用于定位 `planId` / `workOrderId`。

### 5.5 制剂卡片

输入：`overview.preparations`。

- “全部制剂”卡：清除 `filters.varietyCode`。
- 单制剂卡：显示 `preparationName`、`batchCount`、`missingCount`。
- 点击后设置 `filters.varietyCode`，并刷新列表/卡片。

### 5.6 批次清单

`useTable` 列建议：

| 列 | 字段 |
|---|---|
| 品种 | `preparationName` + `preparationCode` + `spec` |
| 工单 / 批号 | `workOrderCode` / `batchNo` / `planNumber` / `materialSource` |
| 时间 | 按 `filters.dateType` 取 `planDate` / `workOrderDate` / `inboundDate` / `inspectionDate`，列名同步变化 |
| 状态 | `status` |
| 生产 | `finishedQty / planQty` + `yieldRate` |
| 入库 / 出库 | `inboundQty / outboundQty` |
| 检验 | `inspectionConclusion` + `latestInspectionCode` |
| 附件 | `fileCount` + `missingCount` |
| 操作 | 详情 |

行 key 使用 `recordKey`。  
`workOrderId` 为空时是未下达计划，行内“详情”调用 `/plans/{planId}`。

`useTable` 配置：

```ts
const { columns, columnChecks, data, loading, pagination, getData,
  searchParams, handleSizeChange, handleCurrentChange, refreshData } = useTable({
  core: {
    apiFn: fetchProductSummaryBatches,
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

### 5.7 品种卡片视图

输入：`/cards` 返回的 `ProductSummaryBatch[]`。

卡片显示：

- 品种、规格
- 工单/批号、计划
- `stageStates` 环节圆点（done / active / pending / error / skipped）
- `processCompleted / processTotal`
- `inboundQty / outboundQty`
- `inspectionConclusion`、`latestInspectionCode`
- `yieldRate`
- `fileCount`、`missingCount`、`missingSummary`

不要为每张卡片调用详情接口。

### 5.8 时间展示

- 批次清单增加“时间（计划/工单/入库/检验）”动态列。
- 卡片显示当前日期口径，计划日期保留在计划信息里。
- 详情抽屉增加计划、生产任务、成品入库、成品出库、检验五个日期，当前筛选口径高亮。
- 筛选区显示“日期口径：xxx 起止范围”，避免用户忘记筛选条件。

### 5.9 批次详情抽屉

根据记录类型请求：

```ts
const detail = row.workOrderId
  ? await fetchProductSummaryBatch(row.workOrderId)
  : await fetchProductSummaryPlan(row.planId)
```

抽屉两个页签：

#### 链路明细

使用 `detail.stages`：

- `state`: `done / active / pending / error / skipped`
- `skipped`、`skipReason`：业务上不适用的节点（如无采购、物料直接入库），前端置灰并展示说明
- `main`、`meta`
- `documents[]`：单据类型、编号、日期、状态、经办人、明细行、文件。

#### 附件

使用 `detail.files`：

- 优先按 `nodeKey` 分组挂到对应节点，`stage` 仅作为显示名称。
- 每组显示 `label`、`slotCode`、`requirement`。
- 每个文件显示 `name`、`size`、`time`，操作预览/下载。
- 缺件使用 `detail.missingItems`，按 `nodeKey`/`slotCode` 展示。

文件预览/下载：

```ts
import { previewFile, downloadFile } from '@/api/files'

await previewFile(file.fileId)
await downloadFile(file.fileId)
```

或复用 `BusinessFilePreviewDialog`（如果已有 bizType/bizId 上下文）。

---

## 6. 集中导出

### 6.1 三种入口

| 入口 | scope | 参数 |
|---|---|---|
| 顶部“导出当前结果” | `FILTERED` | 当前 `filters` |
| 行内“导出” | `SELECTED` | `workOrderIds` 或 `planIds` |
| 品种页“导出该品种” | `FILTERED` | `filters.varietyCode` |
| 全部导出 | `ALL` | 无筛选 |

### 6.2 请求示例

```ts
const request: ProductSummaryExportRequest = {
  scope: 'FILTERED',
  workOrderIds: [],
  planIds: [],
  filters: {
    varietyCode: filters.varietyCode,
    dateType: filters.dateType,
    startDate: filters.startDate,
    endDate: filters.endDate,
    keyword: filters.keyword,
    onlyMissing: filters.onlyMissing
  },
  includeProductFiles: true,
  requirements: ['REQUIRED', 'CONDITIONAL'],
  includeManifest: true,
  format: 'ZIP',
  fileName: `产品信息汇总_${dayjs().format('YYYYMMDD')}`
}
```

选中记录导出：

```ts
{
  scope: 'SELECTED',
  workOrderIds: selected.filter(r => r.workOrderId).map(r => r.workOrderId!),
  planIds: selected.filter(r => !r.workOrderId && r.planId).map(r => r.planId!),
  format: 'ZIP'
}
```

### 6.3 下载

```ts
import { exportProductSummary } from '@/api/productSummary'

const blob = await exportProductSummary(request)
const url = URL.createObjectURL(blob)
const a = document.createElement('a')
a.href = url
a.download = request.fileName
  ? `${request.fileName}.${request.format === 'CSV' ? 'csv' : 'zip'}`
  : `产品信息汇总.${request.format === 'CSV' ? 'csv' : 'zip'}`
a.click()
URL.revokeObjectURL(url)
```

如果后端返回 `Content-Disposition`，优先从响应头解析文件名；拿不到再用本地文件名。

### 6.4 导出交互

- 导出前显示加载状态，禁止重复点击。
- 文件数超过 300 或大小超过 500MB 时后端会返回错误，前端提示“请缩小筛选范围”。
- ZIP 内包含 `文件清单.csv`；CSV 模式只导出清单。
- 清单包含计划日期、工单日期、入库日期、出库日期、检验日期列。
- 文件缺失不会导致导出失败，下载后看清单中的状态列。

---

## 7. 权限和错误处理

```ts
const { hasAuth } = useAuth()
const canExport = hasAuth('product_summary_export')
```

- 没有 `product_summary_read` 不显示菜单/页面。
- 没有 `product_summary_export` 隐藏所有导出按钮。
- `request` 已统一处理错误提示；业务代码只在需要补充上下文时再提示。
- 概览、列表、卡片、详情分别维护 loading，避免一个请求影响整页。
- 列表空状态用 `ArtTable` 的空状态；概览空数据用 `ElEmpty` 或页面空状态。
- 计划记录没有 `workOrderId`，不要拿它去调批次详情接口。

---

## 8. 联调顺序

1. 启动后端，确认 `/api/product-summary/overview`、`/batches`、`/cards` 返回 200。
2. 先做 API 模块和 `useTable`，跑通批次列表。
3. 接结论条、KPI、待办、健康度、制剂卡片。
4. 接列表/卡片切换。
5. 接批次详情抽屉和未下达计划详情。
6. 接集中导出（先 CSV，再 ZIP）。
7. 最后接权限和空状态。

## 9. 验收清单

- [ ] 概览与列表共用筛选条件。
- [ ] KPI 与 `overview.kpis` 一致。
- [ ] 待办点击能打开正确批次/计划。
- [ ] 文件健康度按环节和槽位展示，不必传不计入缺件。
- [ ] 制剂卡片能筛选批次，且“全部制剂”能清除筛选。
- [ ] 列表和卡片视图切换不丢失筛选。
- [ ] 已下达批次详情用 `/batches/{workOrderId}`。
- [ ] 未下达计划详情用 `/plans/{planId}`。
- [ ] 详情附件可预览/下载。
- [ ] 导出支持筛选结果、选中记录、全部三种范围。
- [ ] ZIP 和 CSV 都能下载，ZIP 含文件清单。
- [ ] 文件缺失不阻断导出，清单有状态。
- [ ] 无导出权限时按钮不可见。

---

## 10. 详情页改版（2026-11）

### 10.1 页面形态

批次详情由 85% 弹窗改为整页：批次信息 → 10 节点圆点导航 → 节点折叠卡片。节点内直接展示单据卡片，点击单据卡片打开详情抽屉（完整字段、物料明细、附件、缺件提示）。

`GET /api/product-summary/batches/{workOrderId}`、`GET /api/product-summary/plans/{planId}` 保持原路径，字段只做追加，旧调用方不受影响。

### 10.2 新增字段

**StageItem（链路节点）**

| 字段 | 说明 |
|---|---|
| `documentCount` | 单据数（张） |
| `fileExpected` | 必传+条件必传槽位应传单据数 |
| `fileUploaded` | 必传+条件必传槽位已上传单据数 |
| `fileMissing` | 必传+条件必传缺件项数 |
| `missingDocumentCount` | 存在缺件的单据数（去重） |
| `optionalExpected` / `optionalUploaded` | 可选槽位的应传/已传数，不计入缺件 |

**DocumentItem（单据）**

| 字段 | 说明 |
|---|---|
| `docId` | 单据ID，与 `fileChecklist[].documents[].docId` 对齐 |
| `bizType` | 业务类型编码 |
| `fields[]` | 有序表头字段 `{ label, value }`，前端按顺序展示；空值不返回 |
| `fileExpected` / `fileUploaded` | 本单据必传+条件必传槽位应传/已传数 |
| `missingCount` / `missingLabels[]` | 本单据缺件项数与缺件名称 |

**DocumentLine（单据明细）**

新增验收/出入库字段：`spec`、`arrivalQty`、`actualArrivalQty`、`inboundQty`、`diffQty`、`expiryDate`、`outerPackageCheck`、`appearanceCheck`、`returnReason`、`storageLocation`、`warehouseName`、`remark`。

**FileItem（附件）**

新增 `fileUrl`、`previewUrl`、`contentType`、`fileExtension`、`bizType`、`fileKind`。前端仍优先用 `GET /api/files/preview/{fileId}`、`GET /api/files/{fileId}` 预览/下载，URL 字段作为兜底。

### 10.3 单据类型

- 计划：生产计划
- 采购 / 领料：采购订单
- 验收入库：货物验收（含验收明细）、物料入库单（明细随验收单归档，无独立附件）
- 生产任务：工单
- 生产领料：领料单（附件按 `production_pick.id` 查询）、领料出库单（无独立附件，附件随领料单）
- 生产过程：每道工序一条记录
- 检验：请检单、取样记录、检验记录
- 成品入库：成品入库单
- 审核放行：放行记录
- 成品出库：成品出库单

### 10.4 缺件口径

| 指标 | 定义 |
|---|---|
| 单据数 | 节点下实际单据条数（张） |
| 文件应传 | 必传+条件必传槽位的“单据×槽位”份数 |
| 文件已传 | 上述槽位中已上传至少一个文件的“单据-槽位”数 |
| 缺件项 | 应传但未传的“单据-槽位”数 |
| 可选文件 | `OPTIONAL` 不计入应传/已传/缺件，展示为“不必传” |

节点文案固定为 `单据 N 张 · 文件 x/y · 缺 m 项`，禁止把槽位数写成“缺件 m/y”。

`detail.slots` 只返回必传/条件必传槽位；`detail.fileChecklist` 保留全部槽位（含 OPTIONAL），用于节点“已有附件”分组和缺件明细。

### 10.5 联调注意

- `STOCK_OUT_PRODUCTION` 的附件业务 ID 已统一为 `production_pick.id`；历史若曾按 `stock_out.id` 挂文件，需要核对数据。
- 详情接口额外调用 `loadDetail` 加载验收/退库明细，响应体比列表接口大，属于预期。
- 列表、概览、导出的文件统计口径未变；仅详情页增加了单据级统计字段。

# 产品信息汇总接口与集中导出方案

> 目标：按当前 `_analysis/prototypes/product-summary.html` 的页面结构补齐接口，并提供文件集中导出能力。  
> 状态：已确认并实施；接口与集中导出已落地，本文保留为对接说明。

---

## 1. 当前 HTML 需要什么

当前页面从上到下分为：

1. 一句话结论条：品种数、记录数、批次、缺件记录/项、最优先处理对象。
2. 4 个 KPI：在产批次、待处理缺件、附件完整度、检验合格率。
3. 待办面板：缺件最多的前 5 条记录。
4. 文件健康度：按环节的完整度、缺件最集中的槽位、全部槽位口径。
5. 制剂卡片：每个制剂的批次数、缺件数。
6. 批次清单：品种、工单/批号、状态、生产、入库/出库、检验、附件、详情。
7. 品种卡片视图：品种 → 批次卡片，需要工序进度、出入库、检验、得率、缺件摘要。
8. 批次抽屉：链路明细、附件、缺件清单、导出本批次。
9. 顶部/行内导出：当前筛选结果、单批次、品种导出。

现有接口只覆盖了其中的一部分：

| 页面区域 | 现有接口 | 是否足够 |
|---|---|---|
| 批次清单 | `GET /api/product-summary/batches` | 基本够，字段需补 |
| 批次抽屉 | `GET /api/product-summary/batches/{workOrderId}` | 基本够，需补槽位编码等 |
| 结论/KPI/待办/健康度 | 无 | 不够 |
| 制剂卡片 | 无 | 不够 |
| 品种卡片视图 | 无 | 不够，或需逐批查详情 |
| 导出 | `POST /api/product-summary/export` | 不够，只支持 ID/品种，缺少筛选范围和计划记录 |

---

## 2. 建议的最终接口

### 2.1 页面聚合概览

```http
GET /api/product-summary/overview
  ?varietyCode=
  &dateType=planDate
  &startDate=
  &endDate=
  &keyword=
  &onlyMissing=false
```

返回页面顶部所有聚合数据，避免前端分别请求 KPI、待办、健康度和制剂卡片：

```json
{
  "generatedAt": "2026-11-09 12:00:00",
  "verdict": {
    "varietyCount": 6,
    "recordCount": 7,
    "batchCount": 5,
    "missingRecords": 7,
    "missingItems": 45,
    "topPriority": {
      "recordKey": "315__none",
      "planId": 315,
      "workOrderId": null,
      "preparationName": "强筋壮骨丸",
      "code": "Plan202611030001",
      "batchNo": null,
      "missingCount": 1,
      "stageBrief": "计划 1"
    }
  },
  "kpis": {
    "runningBatches": 4,
    "totalBatches": 5,
    "missingItems": 45,
    "missingRecords": 7,
    "fileExpected": 86,
    "fileUploaded": 41,
    "completenessRate": 48,
    "inspectionTotal": 11,
    "inspectionQualified": 10,
    "inspectionRate": 91
  },
  "todo": [
    {
      "recordKey": "315__none",
      "planId": 315,
      "workOrderId": null,
      "preparationName": "强筋壮骨丸",
      "code": "Plan202611030001",
      "batchNo": null,
      "missingCount": 1,
      "stageBrief": "计划 1"
    }
  ],
  "health": {
    "stages": [
      {
        "stage": "品种资料",
        "expected": 24,
        "uploaded": 24,
        "missing": 0
      }
    ],
    "topSlots": [
      {
        "stage": "检验",
        "label": "请检单",
        "expected": 12,
        "uploaded": 0,
        "missing": 12
      }
    ],
    "allSlots": [
      {
        "stage": "品种资料",
        "label": "工艺规程",
        "requirement": "REQUIRED",
        "expected": 6,
        "uploaded": 6,
        "missing": 0
      }
    ]
  },
  "preparations": [
    {
      "preparationCode": "Z04-011",
      "preparationName": "疏肝养胃丸",
      "batchCount": 1,
      "missingCount": 1
    }
  ]
}
```

说明：

- `recordCount` 包含未下达工单的计划记录；`batchCount` 只统计已下达工单。
- `missingRecords` 按记录条数统计，`missingItems` 按缺件项统计。
- `health.stages` 对应页面“按环节完整度”；`health.topSlots` 对应“缺件最集中的槽位”；`health.allSlots` 对应“全部槽位口径”。
- `preparations` 直接驱动制剂卡片，不需要前端再按制剂分组。
- 所有数字和列表都受同一组筛选条件影响。

### 2.2 批次清单

```http
GET /api/product-summary/batches
  ?varietyCode=
  &dateType=planDate
  &startDate=
  &endDate=
  &keyword=
  &onlyMissing=false
  &pageIndex=0
  &pageSize=20
```

建议在现有 `BatchSummary` 上补充以下字段：

```json
{
  "recordKey": "315__none",
  "planId": 315,
  "workOrderId": null,
  "workOrderCode": null,
  "planNumber": "Plan202611030001",
  "preparationCode": "Z04-010",
  "preparationName": "强筋壮骨丸",
  "spec": "每50丸重6g（每1g相当于饮片约0.92g）",
  "batchNo": null,
  "materialSource": null,
  "status": "待生产",
  "planDate": "2026-11-03",
  "planQty": 3500,
  "finishedQty": null,
  "inboundQty": 0,
  "outboundQty": 0,
  "yieldRate": null,
  "processTotal": 0,
  "processCompleted": 0,
  "latestInspectionCode": null,
  "inspectionConclusion": null,
  "fileCount": 4,
  "missingCount": 1,
  "missingSummary": "计划 1",
  "stageStates": [
    { "key": "plan", "label": "计划", "state": "done" },
    { "key": "material", "label": "采购", "state": "pending" },
    { "key": "workOrder", "label": "工单", "state": "pending" },
    { "key": "production", "label": "生产", "state": "pending" },
    { "key": "inbound", "label": "入库", "state": "pending" },
    { "key": "outbound", "label": "出库", "state": "pending" },
    { "key": "inspection", "label": "检验", "state": "pending" },
    { "key": "release", "label": "放行", "state": "pending" }
  ]
}
```

补充字段的用途：

- `inboundQty`、`outboundQty`：清单“入库/出库”列和批次卡片。
- `processTotal`、`processCompleted`：批次卡片的工序进度。
- `latestInspectionCode`：清单检验列副行。
- `stageStates`：品种卡片视图的环节圆点。
- `recordKey`：统一标识已下达批次和未下达计划，前端 drawer/export 都用它。

### 2.3 批次详情

```http
GET /api/product-summary/batches/{workOrderId}
```

保留现有接口，返回：

- `batch`：批次头。
- `slots`：槽位完整度。
- `stages`：计划/采购/验收/工单/生产/入库/出库/检验/放行节点及单据。
- `files`：文件分组。
- `missingItems`：缺件清单。

建议补充：

- `FileGroup.slotCode`：前端右栏按槽位标识。
- `FileItem.previewUrl` / `downloadUrl`：如果不想让前端再拼文件接口；也可继续只用 `fileId`。
- `MissingItem.recordKey`：与清单一致。

### 2.4 未下达计划详情

```http
GET /api/product-summary/plans/{planId}
```

用于清单里“未下达工单”的记录点击详情。返回结构与批次详情一致，`batch.workOrderId` 为 `null`，`stages` 只包含计划/采购等已有节点。

如果不希望单独开接口，也可以让前端对计划记录只展示列表摘要；但当前 HTML 的每一行都可点击，建议补这个接口。

### 2.5 品种卡片视图

```http
GET /api/product-summary/cards
  ?varietyCode=
  &dateType=planDate
  &startDate=
  &endDate=
  &keyword=
  &onlyMissing=false
```

返回筛选范围内的全部 `BatchSummary`（不分页），字段与 2.2 相同，用于“品种卡片”视图。  
如果数据量很大，可把该接口改为 `GET /api/product-summary/cards?pageIndex=&pageSize=` 或限制最大 500 条。

---

## 3. 文件集中导出接口

### 3.1 接口

```http
POST /api/product-summary/export
Content-Type: application/json
```

请求体：

```json
{
  "scope": "FILTERED",
  "workOrderIds": [463, 462],
  "planIds": [315],
  "filters": {
    "varietyCode": "",
    "dateType": "planDate",
    "startDate": "2026-09-01",
    "endDate": "2026-10-31",
    "keyword": "",
    "onlyMissing": false
  },
  "includeProductFiles": true,
  "requirements": ["REQUIRED", "CONDITIONAL"],
  "includeManifest": true,
  "format": "ZIP",
  "fileName": "产品信息汇总_20261109"
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `scope` | `SELECTED`：按 `workOrderIds`/`planIds`；`FILTERED`：按 `filters`；`ALL`：全部 |
| `workOrderIds` | 选中批次 |
| `planIds` | 选中的未下达计划 |
| `filters` | 与列表同一套筛选条件 |
| `includeProductFiles` | 是否包含品种资料（工艺规程、质量标准等） |
| `requirements` | 默认 `REQUIRED` + `CONDITIONAL`，不传默认不含 `OPTIONAL` |
| `includeManifest` | 是否生成 `文件清单.csv` |
| `format` | `ZIP`：实际文件 zip；`CSV`：只导出清单 |
| `fileName` | 下载文件名前缀，可省略 |

响应：

- `format=ZIP`：`application/zip`，`Content-Disposition: attachment`。
- `format=CSV`：`text/csv;charset=utf-8`，带 BOM。
- 文件不存在不中断导出，在清单中标记“文件缺失”。

### 3.2 zip 目录结构

```text
产品信息汇总_20261109.zip
├─ 文件清单.csv
├─ Z04-011_疏肝养胃丸/
│  ├─ 品种资料/
│  │  ├─ 工艺规程/
│  │  ├─ 质量标准/
│  │  ├─ 批记录模板/
│  │  └─ 验证方案/
│  └─ 260901_GD202610200001/
│     ├─ 计划/
│     ├─ 采购/
│     ├─ 验收/
│     ├─ 工单/
│     ├─ 工序/
│     ├─ 入库/
│     ├─ 出库/
│     ├─ 检验/
│     └─ 放行/
└─ Plan202611030001_未下达工单/
   └─ 计划/
```

规则：

- 品种资料按制剂只放一份，避免多批次重复。
- 批次文件按“批号_工单号”分目录；未下达计划按“计划号_未下达工单”分目录。
- 同一文件被多条记录引用时只写一次，清单中记录引用关系。
- 文件名冲突时追加 `(2)`、`(3)`。
- 文件名做路径安全清洗。

### 3.3 文件清单列

`文件清单.csv` 建议列：

```text
品种编码, 品种名称, 规格, 计划编号, 工单号, 批号,
环节, 槽位, 文件名, 大小(字节), 上传人, 上传时间, 状态, 说明
```

状态值：

- `已导出`
- `文件不存在`（业务文件关系存在，但磁盘文件缺失）
- `缺失`（应有但未上传）
- `不必传`（OPTIONAL，不参与缺件）
- `未接入上传位`（如领料/入库/出库附件）

### 3.4 同步/异步

建议第一版：

- 同步 zip，限制：文件数 ≤ 300 或总大小 ≤ 500MB。
- 超过限制返回 413/400，并提示缩小筛选范围。

后续如果数据量大，增加异步任务接口：

```http
POST /api/product-summary/export/tasks
GET  /api/product-summary/export/tasks/{taskId}
GET  /api/product-summary/export/tasks/{taskId}/download
```

异步任务返回：

```json
{
  "taskId": "exp_20261109_001",
  "status": "RUNNING",
  "progress": 42,
  "message": "正在打包 检验/请检单",
  "downloadUrl": null
}
```

---

## 4. 前端调用顺序

### 4.1 页面初始化

```ts
const [overview, list] = await Promise.all([
  fetchProductSummaryOverview(filters),
  fetchProductSummaryBatches({ ...filters, pageIndex: 1, pageSize: 20 })
])
```

### 4.2 筛选变化

- 重新请求 `overview` 和 `batches`，页码重置为 1。
- 制剂卡片点击只改变 `varietyCode`，然后走同一套查询。

### 4.3 点击批次

```ts
const detail = await fetchProductSummaryBatch(workOrderId)
```

未下达计划：

```ts
const detail = await fetchProductSummaryPlan(planId)
```

### 4.4 导出

- 顶部“导出当前结果”：`scope=FILTERED`，带当前筛选条件。
- 行内“导出”：`scope=SELECTED`，带 `workOrderIds` 或 `planIds`。
- 品种页“导出该品种”：`scope=FILTERED`，`varietyCode=...`。
- 文件缺失不阻断；下载后查看 `文件清单.csv`。

---

## 5. 实施拆分

| 阶段 | 内容 | 预估 |
|---|---|---|
| P1 | `overview` 聚合接口、批次列表字段补全、计划详情、卡片接口 | 2–3 天 |
| P2 | 集中导出 zip/CSV、目录结构、清单、去重、缺失标记 | 2–3 天 |
| P3 | 前端对接与联调 | 1–2 天 |
| P4 | 大数据量异步导出（可选） | 1–2 天 |

---

## 6. 需要确认的点

1. 导出范围是否按“选中 / 当前筛选 / 全部”三种都支持？建议都支持。
2. 品种资料是否只导出一次（按品种去重）？建议是。
3. 未下达计划是否纳入导出？建议纳入，单独目录。
4. 文件缺失时是继续导出并写清单，还是直接失败？建议继续并标记。
5. 第一版是否接受同步导出上限（300 个文件 / 500MB），超过再走异步？
6. `文件清单.csv` 的列是否按上面 14 列？

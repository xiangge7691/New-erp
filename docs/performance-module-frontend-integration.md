# 人员绩效模块 · 前端对接文档

> 更新日期：2026-11-18  
> 测试库：`192.168.5.100 / erp_db`（禁止向 `8.148.27.237` 写测试数据）  
> 对应原型：[performance.html](prototype/production/performance.html)、[backend/performance.html](prototype/production/backend/performance.html)  
> 排产/实际生产对接见：[scheduling-module-frontend-integration.md](scheduling-module-frontend-integration.md)

本文档按当前绩效原型逐页说明前端如何接后端，并标注后端支持情况。页面的统计、分组与排行在前端聚合完成；后端负责明细查询、修改、补录、作废、系数版本与导出。

---

## 1. 页面结构

```
人员绩效
├─ 绩效总览
│  ├─ KPI：绩效工时合计、计件合计、涉及生产工序、待复核记录
│  └─ 人员绩效工时排行（按工序拆分的堆叠条形图，前端聚合）
├─ 工序粒度视图（任务 → 工序 → 批次 → 人）
├─ 人员粒度视图（人 → 任务 → 工序）
├─ 系数与版本（工序系数 / 人员系数 / 版本历史 三个页签）
└─ 原型说明
```

> 「班组与品种」页已删除；后端 `/api/performance/aggregate` 保留但前端不再调用。

---

## 2. 接口总览

| 方法 | 路径 | 用途 | 后端支持 |
|---|---|---|---|
| GET | `/api/performance/records` | 明细查询、总览/人员视图数据源 | ✅ |
| GET | `/api/performance/process-view` | 工序粒度（可按任务或全部） | ✅ |
| GET | `/api/performance/person-view` | 人员粒度（personnelId 可空） | ✅ |
| PUT | `/api/performance/records/{id}` | 修改绩效时长（备注必填） | ✅ |
| POST | `/api/performance/manual` | 手动补录 | ✅ |
| POST | `/api/performance/records/{id}/void?note=` | 作废手动记录 | ✅ |
| GET | `/api/performance/coefficient-versions` | 版本列表（含操作人姓名） | ✅ |
| GET | `/api/performance/coefficient-versions/{id}` | 版本详情（工序/人员系数明细） | ✅ |
| POST | `/api/performance/coefficient-versions` | 发布新版本（备注选填） | ✅ |
| GET | `/api/performance/export` | 导出 Excel | ✅ |
| GET | `/api/performance/aggregate` | 班组/品种/日期汇总 | ⚠️ 保留，前端不调用 |

### 2.1 通用查询参数（`/records`、`/process-view`）

| 参数 | 说明 |
|---|---|
| `from` / `to` | 绩效归属日期，`yyyy-MM-dd` |
| `keyword` | 工单号/工单名称/制剂名称/批号/计划名称/计划编号，模糊匹配（计划编号经 `production_plan` 匹配） |
| `personnelId` | 人员档案 ID |
| `workOrderId` | 工单 ID（`/process-view` 可不传，表示全部任务） |
| `teamId` | 班组 ID；按班组成员匹配人员 |
| `source` | `系统生成` / `手动添加` |
| `modifiedStatus` | `1` 已修改 / `0` 未修改 |
| `recordStatus` | `1` 正常 / `0` 作废；**不传 = 正常 + 作废** |
| `pageIndex` / `pageSize` | 分页；`pageIndex=-1` 或 `pageSize=-1` 取全量（上限 10000 条） |

> 总览、排行、堆叠图建议传 `pageSize=-1` 一次取回区间内全部记录，再在前端聚合；数据量大时按日期范围分段。

---

## 3. 页面与接口映射

### 3.1 绩效总览

- 数据源：`GET /api/performance/records?from=&to=&pageSize=-1`。
- KPI：
  - **绩效工时合计** = Σ `performanceHoursTotal`；
  - **计件合计** = Σ `completedQty`；
  - **涉及生产工序** = `processTypeId` 去重数（手动只写时长的记录无工序，另行计入“手动记录”）；
  - **待复核记录** = `source=手动添加` 数量 + `modifiedStatus=1` 数量。
- 人员排行：按 `personnelId` 分组，组内再按 `processTypeId/processName` 分组求 `performanceHoursTotal` 之和；每人各工序时长堆叠后累加为总工时。颜色按工序固定映射。
- 后端不提供聚合接口，全部由前端计算；已删除的班组占比环图、每日趋势图不再需要数据。

### 3.2 工序粒度视图

- 接口：`GET /api/performance/process-view?from=&to=&keyword=&teamId=&source=&modifiedStatus=&recordStatus=&pageSize=-1`。
- 筛选栏映射：全部班组 → `teamId`；全部来源 → `source`；全部修改状态 → `modifiedStatus`；正常记录 → `recordStatus=1`，正常+作废 → 不传，仅作废 → `recordStatus=0`。
- 分组：`workOrderId/workOrderCode` → 任务；`processTypeId/processName` → 工序；`planBatchId/batchNo/planBatchName` → 批次；记录即个人行。
- 工序行字段：
  - 计划起止：`planStartTime / planEndTime`；计划时长：`planDurationMinutes`；
  - 实际起止：`actualStartTime / actualEndTime`；实际时长：`processActualHours`；
  - 计划数量：`planQty`；实际数量：组内 `completedQty` 求和；
  - 达成率：前端按实际/计划计算；
  - 系数版本：`coefficientVersionNo`；工序系数：`processCoefficient`；
  - 绩效工时小计：组内 `performanceHoursTotal` 求和。
- 人员行：
  - 姓名/班组：`personnelName / teamName`；
  - 人员系数：`personCoefficient`；
  - 工时（绩效时长）：`performanceHours`（列名已合并，工作时间为 `workHours`，仅悬停展示）；
  - 绩效工时：`performanceHoursTotal`；修改过的记录前端标红；
  - 完成数量：`completedQty`；来源：`source`；状态：`recordStatus / modifiedStatus`。
- 修改明细：点击红色绩效工时，用 `originalPerformanceHours`、`performanceHours`、`note`、`updatedByName`、`updatedTime` 展示修改前后、原因、修改人与时间；未修改记录 `originalPerformanceHours` 为空。

### 3.3 人员粒度视图

- 接口：`GET /api/performance/person-view?personnelId=&from=&to=`；`personnelId` 不传即“全部人员”。
- 其他筛选（来源/修改状态/记录状态/关键字）可改用 `/records`，参数与上表一致。
- 分组：按 `personnelId` 分卡片；卡片内按 `performanceDate` 倒序。
- 字段：日期、任务、制剂/批号、工序/批次、实际起止、工时（`performanceHours`）、绩效工时、完成数量、来源/状态；修改过的绩效工时同样可点开修改明细。

### 3.4 系数与版本

1. 进入页面：`GET /api/performance/coefficient-versions` 取版本列表；`status=生效` 为当前版本，其余为历史版本；列表含 `versionNo / effectiveTime / operatorName / note / status`。
2. 当前系数：`GET /api/performance/coefficient-versions/{id}` 取 `processItems`（processTypeId、processCode、processName、coefficient）与 `personItems`（personnelId、personnelName、employeeNo、teamName、coefficient），分别渲染「工序系数」「人员系数」页签。
3. 搜索：工序名称、人员姓名均为前端本地过滤（数据量小）。
4. 发布：`POST /api/performance/coefficient-versions`
   ```json
   {
     "note": "选填，版本备注",
     "processCoefficients": { "189": 1.1 },
     "personCoefficients": { "71": 1.05 }
   }
   ```
   - 工序系数 key 为 `processTypeId`，人员系数 key 为 `personnelId`；也兼容工序编码、人员工号；
   - `processCoefficients` / `personCoefficients` 既可以是对象（ID→系数），也可以是明细数组（`{processTypeId, coefficient}` / `{personnelId, coefficient}`）；
   - 系数为空（null）表示该项不修改；未提交的项按当前工序/人员已有系数保留；
   - 后端会归档旧版本、生成新版本号并写系数快照，前端做二次确认即可。
5. 版本历史：列表 + 「查看系数」用版本详情展示，只读。

### 3.5 修改、补录、作废

- **修改绩效时长**：`PUT /api/performance/records/{id}`
  ```json
  { "performanceHours": 1.8, "note": "复核后修正", "completedQty": 98000 }
  ```
  - `note` 必填；`completedQty` 仅手动记录可传；
  - 首次修改会写入 `originalPerformanceHours`；
  - 返回更新后的记录（含绩效工时、修改人、更新时间）。
- **手动补录**：`POST /api/performance/manual`
  ```json
  {
    "personnelId": 71,
    "performanceDate": "2026-11-14",
    "calcMode": "按系数计算",
    "executionId": 394,
    "workOrderId": 462,
    "processTypeId": 92,
    "performanceHours": 2.0,
    "completedQty": 50000,
    "note": "补录遗漏参与",
    "allowDuplicate": false
  }
  ```
  - 人员绩效以**实际执行**为准：关联已完工工序时传 `executionId`（推荐）；只传 `workOrderId + processTypeId`（可加 `planBatchId`）时，后端取该工序最近一条已完工执行记录；
  - 执行记录的 `planId / planBatchId / batchNo / 实际时长` 会带到绩效记录；找不到已完工执行记录时接口返回“未找到已完工的工序执行记录”；
  - 只写时长时传 `calcMode=直接计入`，不传任务/工序；
  - **重复补录**：同一人、同一工单、同一工序已有正常记录且 `allowDuplicate` 不为 true 时，接口返回错误“已有记录”；前端弹出确认框填写原因后，带 `allowDuplicate=true` 重新提交。
- **作废**：`POST /api/performance/records/{id}/void?note=原因`；仅手动记录可作废。

### 3.6 导出

`GET /api/performance/export?from=&to=&personnelId=&keyword=&teamId=&source=&modifiedStatus=&recordStatus=`，导出与当前筛选一致的明细（作废记录默认不导出，除非传 `recordStatus=0`）。导出列已合并为「工时（绩效时长）」。

---

## 4. 后端支持情况

| 原型功能 | 后端 | 说明 |
|---|---|---|
| 总览 KPI / 堆叠排行 | ✅ 明细支持，前端聚合 | `records` 支持全量（`pageSize=-1`） |
| 工序/人员粒度筛选 | ✅ | `keyword`、`teamId`、`source`、`modifiedStatus`、`recordStatus` |
| 工时合并展示 | ✅ | 使用 `performanceHours`；`workHours` 仅作悬停/明细 |
| 修改前值留痕 | ✅ | `originalPerformanceHours`（首次修改写入） |
| 修改人/时间 | ✅ | `updatedByName`、`updatedTime` |
| 手动补录重复确认 | ✅ | `allowDuplicate` 字段 |
| 系数版本整体发布、备注选填 | ✅ | `note` 可不传；操作人姓名已返回 |
| 班组与品种汇总 | ⚠️ 接口保留不调用 | `/aggregate` |
| 绩效页导出 | ✅ | 支持与列表一致的筛选参数 |

**前端需要自行完成**：KPI 汇总、按工序堆叠、任务/工序/批次分组、达成率计算、班组筛选下拉（`GET /api/production-teams/active`）、人员下拉（人员档案列表接口）、工序名称搜索（本地过滤）。

---

## 5. 关键字段字典（`PerformanceRecord`）

| 字段 | 说明 |
|---|---|
| `personnelId / personnelName / teamName` | 人员与班组 |
| `workOrderId / workOrderCode / preparationName / batchNumber` | 工单与制剂 |
| `processTypeId / processName / planId / planBatchId / batchNo / planBatchName` | 工序与批次 |
| `performanceDate` | 绩效归属日期 |
| `planStartTime / planEndTime / planDurationMinutes / planQty` | 计划参考 |
| `actualStartTime / actualEndTime / processActualHours` | 实际执行 |
| `workHours` | 个人工作时间（原字段，列表不再单独展示） |
| `performanceHours` | 工时（绩效时长，参与计算） |
| `originalPerformanceHours` | 修改前绩效时长（首次修改写入） |
| `processCoefficient / personCoefficient / coefficientVersionNo` | 系数快照 |
| `performanceHoursTotal` | 绩效工时 |
| `completedQty` | 完成数量 |
| `source / modifiedStatus / recordStatus / note` | 来源、修改状态、记录状态、备注 |
| `updatedByName / updatedTime` | 最后修改人与时间 |

---

## 6. 联调注意

1. 执行库脚本 `20261118_scheduling_alignment.sql` 后才有 `original_performance_hours` 字段；未执行时修改接口会报列不存在。
2. `recordStatus` 不传表示“正常 + 作废”，与原型「正常记录」默认值不同，前端需显式传 `1`。
3. 全量查询上限 10000 条，月度数据超出时按日期分段或分页聚合。
4. 计划编号关键字查询依赖 `production_plan` 表；工单上的 `plan_number` 为非表字段，后端已改为关联查询。
5. 系数版本发布前确认工序/人员 ID 映射正确，发布后不可撤销。

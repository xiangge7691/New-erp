# 生产任务 · 工序排产 / 实际生产 前端对接文档

> 更新日期：2026-11-18  
> 测试库：`192.168.5.100 / erp_db`（禁止向 `8.148.27.237` 写测试数据）  
> 目标态原型：[scheduling.html](prototype/production/scheduling.html)（PC 端）、[backend/scheduling.html](prototype/production/backend/scheduling.html)  
> 绩效原型：[performance.html](prototype/production/performance.html)

本文档对应本轮的页面调整：**生产任务改为「外层列表页 + 任务详情页」**，详情页新增「排产」「实际生产」两个页签，甘特图放在列表页顶部；排产可选、班组仅作人员快捷分组、班次模块下线。

---

## 1. 页面结构

### 1.1 生产任务列表页（外层）

```
生产任务
├─ 排产甘特图（按任务 / 按设备 / 按房间）
├─ 筛选：状态 / 关键字 / 配置日期范围｜搜索 / 重置
└─ 页签：生产任务列表 / 平均生产用时
   ├─ ＋ 新增任务
   └─ 表格：生产任务编号、生产任务名称、计划名称、批号、制剂名称、计划数量、
            配置日期、生产用时（小时）、当前状态、操作
```

- 点击「生产任务编号」进入任务详情页。
- 甘特图只出现在列表页，详情页不重复展示。

### 1.2 生产任务详情页

```
← 返回列表    生产任务详情 - {workOrderCode}
页签：生产任务详情 / 变更历史 / 排产 / 实际生产
```

- **生产任务详情**：任务字段表（任务名称、生产任务编号、关联计划、当前状态、制剂名称、加工类型、计划数量、批号、配置日期、成品数量、得率等）+ **只读生产工序表**。
  工序表列：工序名称、配置室、使用设备、关键工艺参数、计划起止、实际起止、操作人、加工数量、**排产**、**生产**、备注、操作。
  - 排产：`已排产 / 未排产`（保存排产时由前端传 `processes[].scheduled`，后端原样保存并返回，不按计划起止时间推算）。
  - 生产：`未生产 / 生产中 / 已生产`（按实际执行起止推导）。
- **变更历史**：留空或对接变更日志接口。
- **排产**：检查冲突 / 自动顺排 / 保存排产 / 拆分工序；排产表按工序+批次展开。
- **实际生产**：按工序+批次登记实际开始、结束、数量、参与人；完工后锁定。

---

## 2. 接口清单

### 2.1 排产

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/scheduling/tasks?keyword=&status=&configFrom=&configTo=` | 生产任务列表（含生产状态、生产用时、已排产工序数） |
| GET | `/api/scheduling/tasks/{workOrderId}` | 任务详情：工单信息 + 工序计划 + 批次 + 实际 + 冲突 |
| POST | `/api/scheduling/tasks/{workOrderId}/auto-plan` | 按模板标准工时自动顺排（存草稿；有冲突则不保存并返回错误） |
| PUT | `/api/scheduling/tasks/{workOrderId}/plan` | 保存草稿；有冲突时返回 code=400 + `data.conflicts`，**不落库** |
| POST | `/api/scheduling/tasks/{workOrderId}/plan/save` | 正式保存；有冲突时返回 code=400 + `saved=false` + `conflicts`，**不落库** |
| POST | `/api/scheduling/tasks/{workOrderId}/conflicts/check` | 当前任务冲突检查 |
| POST | `/api/scheduling/plans/{planId}/split` | 拆分工序为多个批次；有冲突时返回 400 + `conflicts`，**不落库** |
| GET | `/api/scheduling/gantt?view=task|equipment|room&from=&to=&taskNo=&equipmentId=&equipmentName=&roomId=&roomName=` | 甘特图数据（含草稿、制剂名、计划号、批次名）；`from/to` 支持 `yyyy-MM-dd HH:mm:ss` 或 ISO `yyyy-MM-ddTHH:mm:ss`；`taskNo` 按任务号模糊筛选，设备/房间支持 ID 精确或名称模糊筛选（批次设备/房间优先） |

**列表查询参数**

- `keyword`：工单编号 / 工单名称 / 制剂编码 / 制剂名称 / 计划名称 / 计划编号 / 批号，模糊匹配。
- `status`：`待生产` / `生产中` / `已完工`，按执行记录推导。
- `configFrom` / `configTo`：`yyyy-MM-dd`，对应工单配置日期。

### 2.2 班组（无班次）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/production-teams?keyword=` | 班组列表（含成员数量） |
| GET | `/api/production-teams/active` | 全部班组 + 成员明细（id、姓名、工号），用于快捷选人 |
| GET | `/api/production-teams/{id}/members` | 单个班组成员 |
| POST | `/api/production-teams` | 新建班组（teamName + memberIds） |
| PUT | `/api/production-teams/{id}` | 修改班组 |

> 班组已去掉「所属车间」「启用/停用」，班次相关接口与表已删除。

### 2.3 实际生产（工序执行）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/work-order-process-execution/by-work-order/{workOrderId}` | 查询工单执行记录（含参与人） |
| POST | `/api/work-order-process-execution/batch?workOrderId=` | 批量保存执行记录（开始/结束/数量/参与人；带 planId、planBatchId、batchNo）。兼容排产详情回传字段：`executionId`→记录ID、`actualStartTime/actualEndTime`→实际起止、`actualQty`→实际数量；支持 `unitId`，未传时按执行记录→批次→计划→工序模板补齐；返回 `unitId/unitName/unitSymbol`。参与人未传 `workHours` 时按实际起止自动计算，未逐人传 `completedQty` 时按参与人均分实际数量 |
| POST | `/api/work-order-process-execution/{executionId}/complete` | 完工：锁定记录并生成人员绩效 |
| GET | `/api/work-order-process-execution/completed` | 完工记录列表 |
| GET | `/api/work-order-process-execution/completed/export` | 完工记录导出 |

### 2.4 绩效（受本轮原型影响）

> 绩效页的完整对接说明（总览聚合、工序/人员粒度、系数版本、补录与导出）见 [performance-module-frontend-integration.md](performance-module-frontend-integration.md)。

| 方法 | 路径 | 变更 |
|---|---|---|
| GET | `/api/performance/process-view` | `workOrderId` 改为可选；新增 `keyword`、`source`、`modifiedStatus`、`recordStatus`（不传=全部状态） |
| GET | `/api/performance/records` | 新增 `keyword` |
| PUT | `/api/performance/records/{id}` | 请求体改为 `performanceHoursTotal`（系数计算后的绩效工时，直接覆盖）；返回 `originalPerformanceHours`（首次修改前的绩效工时） |
| POST | `/api/performance/coefficient-versions` | `note` 改为选填（版本备注） |
| GET | `/api/performance/export` | 新增 `keyword`；导出列合并为「工时（绩效时长）」 |
| GET | `/api/performance/aggregate` | 保留接口，前端不再调用（班组与品种页已删除） |

---

## 3. 关键字段

### 3.1 任务列表（`ScheduleTaskDto`）

| 字段 | 说明 |
|---|---|
| `workOrderId` / `workOrderCode` / `workOrderName` | 生产任务主键、编号、名称 |
| `planId` / `planName` / `planNumber` | 关联生产计划 |
| `preparationName` / `batchNumber` / `batchQty` | 制剂、批号、批量 |
| `configDate` | 配置日期 |
| `currentStatus` | 工单原状态（保留展示兼容） |
| `productionStatus` | **生产状态**：待生产/生产中/已完工（执行推导） |
| `productionDurationHours` | 生产用时（小时），优先取工单统计值 |
| `processCount` / `scheduledProcessCount` | 工序数 / 已排产工序数 |
| `conflictCount` / `saved` | 冲突数 / 是否已保存排产 |

### 3.2 排产工序（`ProcessScheduleDto`）

| 字段 | 说明 |
|---|---|
| `scheduled` | **是否已排产**：保存时由前端传参并落库，后端原样返回，不按计划起止时间推算；未传时沿用原值 |
| `status` | 保存状态：草稿 / 已保存 |
| `planStartTime` / `planEndTime` / `planDurationMinutes` | 计划时间，可为空 |
| `planQty` / `roomId` / `roomName` / `equipmentId` / `equipmentName` | 计划数量与资源 |
| `unitId` / `unitName` / `unitSymbol` | 计量单位（快照自工序模板）；数量列优先显示 `unitSymbol`（如 kg），无单位显示“—” |
| `teamId` / `teamName` / `participants` | 计划班组（选填）与参与人 |
| `batches[]` | 拆分批次；每个批次 `scheduled`、计划起止、数量、`unitId/unitName/unitSymbol`、参与人、实际对照；`status` 按关联的实际执行记录推导为 `计划中` / `执行中` / `已完工`；`actualStatus` 未登记实际生产时为 `待执行`，多条执行记录按最早开始、最晚结束、数量合计汇总 |
| `actualStartTime` / `actualEndTime` / `actualStatus` / `actualQty` / `executionId` | 实际执行对照 |

### 3.3 甘特图（`GanttItemDto`）

新增 `preparationName`、`planNumber`、`batchNumber`、`batchName`；`view=equipment|room` 时前端色块显示「制剂名称 / 生产任务号」两行。**草稿也会返回**（只要有计划起止）。

### 3.4 绩效记录

新增 `originalPerformanceHours`（首次修改前的绩效工时）。修改的是系数计算后的绩效工时 `performanceHoursTotal`，直接覆盖；修改后：
- `performanceHoursTotal` = 修改后绩效工时；
- `performanceHours` = 实际时长，保持不变；
- `originalPerformanceHours` = 修改前绩效工时（首次修改时写入）；
- `updatedByName` / `updatedTime` 为最后修改人与时间。

红色「绩效工时」点击后展示：修改前/后绩效工时、修改原因（`note`）、修改人、修改时间。

---

## 4. 业务规则

1. **排产可选**：计划开始/结束为空即未排产；正式保存同样允许，未排产工序不参与冲突校验、不进甘特图，不影响生产任务流程。
2. **排产标记 ≠ 流程状态**：列表/详情的「已排产 / 未排产 / 部分排产」由前端保存时传入的 `scheduled` 决定（后端不推算）；生产状态由实际执行推导。
3. **冲突规则**：设备占用、房间占用、人员冲突（同一人同一时段被安排到不同工序）、工序顺序、批次时段重叠。**班次校验已删除**，班组不参与冲突判断。拆分批次的每批数量只需大于 0，合计不强制等于工序计划数量（计划数量为排产时手工填写，可按实际安排拆分）。
4. **班组快捷选人**：选择班组只勾选成员，之后可逐人增减；排产保存到计划参与人，实际生产保存到执行参与人。
5. **实际生产**：执行记录按工序/批次保存；`complete` 后锁定，不可再改，并生成人员绩效。
6. **修改留痕**：修改绩效工时（`performanceHoursTotal`）时，首次修改把修改前的值写入 `original_performance_hours`，后续修改不再覆盖；修改备注必填。
7. **系数版本**：工序系数 + 人员系数整体发布，备注选填；发布后不可撤销。

---

## 5. 前端对接建议

### 5.1 列表页

- 页面进入调用 `GET /api/scheduling/tasks`（可不传参数），状态筛选映射 `status`，关键字映射 `keyword`，配置日期映射 `configFrom/configTo`。
- 任务级 `planStartTime` 取工单计划开始时间；工单未填时取最早排产工序的开始时间，PDA 任务详情同样处理。
- 甘特图调用 `GET /api/scheduling/gantt?view=...&from=...&to=...`，默认展示草稿；色块 `preparationName` + `workOrderCode` 两行。
- 甘特图筛选（均为可选）：`taskNo` 按任务号模糊匹配；设备可传 `equipmentId`（精确）或 `equipmentName`（模糊）；房间可传 `roomId` 或 `roomName`。批次上的设备/房间优先于工序计划；筛选只影响返回条数，冲突标记仍按工单全部计划计算。
- 行点击进入详情（`GET /api/scheduling/tasks/{id}`）。

### 5.2 详情页

- 「生产任务详情」用详情接口的工单字段 + `processes[].scheduled`、`actualStatus` 渲染只读工序表。
- 「排产」页签：
  - 调整工序：`PUT /plan` 保存草稿，`POST /plan/save` 正式保存；工序行带 `unitId`（不传沿用原值或按工序模板补齐），数量列显示 `unitSymbol`；
  - 保存批次时把 `batches[].id` 原样回传即可就地更新、批次ID不变（未带ID时按 `batchNo` 匹配）；已登记实际生产的批次不能删除；
  - 拆批次：`POST /plans/{planId}/split`（批次参与人字段 `batches[].participants`；`batches[].unitId` 不传时继承工序计划）；
  - 班组快捷选人：`GET /api/production-teams/active` 取成员，勾选后写入 `participants`。
- 「实际生产」页签：
  - 保存执行：`POST /api/work-order-process-execution/batch?workOrderId=`（可带 `unitId`，不传按执行记录→批次→计划→模板补齐）；
  - 完工：`POST /api/work-order-process-execution/{executionId}/complete`；
  - 参与人同样可按 `production-teams/active` 快捷选择；
  - 数量列与录入框使用执行记录返回的 `unitSymbol`。

### 5.3 PDA 任务详情与批次执行

- `GET /api/pda/production/tasks/{workOrderId}`：排产拆分过批次（且已正式保存、非草稿）的工序按批次展开，一条批次一行（`batchNo` / `batchName` / `planBatchId`，计划起止与数量取批次），原整道工序行不再返回；批次尚未登记实际生产时 `processes[].executionId` 为空。
- 批次行开始计时或保存前，先调用 `POST /api/pda/production/tasks/{workOrderId}/batches/{planBatchId}/execution` 确保批次执行记录存在（已存在则返回原记录，幂等），拿到 `executionId` 后再调用 start / end / draft / submit 接口。
- `processCount`、`doneCount` 按展开后的行统计，批次全部完成才算该道工序完成。

### 5.4 绩效页

- 工序粒度：`GET /api/performance/process-view?from=&to=&keyword=&teamId=&source=&modifiedStatus=&recordStatus=`（`workOrderId` 可不传）。
- 人员粒度：`GET /api/performance/person-view?personnelId=&from=&to=`。
- 工时列展示 `performanceHoursTotal`（列名「工时（绩效时长）」）；红色绩效工时点击展示修改明细（`originalPerformanceHours`、`note`、`updatedByName`、`updatedTime`）。
- 系数版本：`POST /api/performance/coefficient-versions`，`note` 可不传。

---

## 6. 数据库迁移

执行脚本：

```
src/main/resources/Data/migration/20261118_scheduling_alignment.sql
src/main/resources/Data/migration/20261119_work_order_plan_scheduled_flag.sql
src/main/resources/Data/migration/20261119_process_unit_id.sql
```

`20261119_work_order_plan_scheduled_flag.sql`：计划/批次增加 `scheduled`（前端传参，后端不推算）。

`20261119_process_unit_id.sql`：计划/批次/执行记录增加 `unit_id`，并按「制剂 + 工序类型 + 工序顺序」从工序模板回填，批次继承计划。

内容：
1. `performance_record` 增加 `original_performance_hours`；
2. 删除 `production_shift`、`production_shift_team`；
3. `production_team` 删除 `workshop`、`status` 字段及 `idx_team_status` 索引。

回滚脚本：`20261118_scheduling_alignment_rollback.sql`（班次表需从 20261116 脚本重建）。

> 注意：脚本需在测试库 `192.168.5.100 / erp_db` 执行；生产库执行前请备份并评估。

---

## 7. 原型与文档对照

| 内容 | 文件 |
|---|---|
| 生产任务列表 + 详情（排产/实际生产） | `docs/prototype/production/scheduling.html` |
| 后端对接版（含接口备注） | `docs/prototype/production/backend/scheduling.html` |
| 人员绩效（工时合并、修改明细、系数页签） | `docs/prototype/production/performance.html` |
| 详细前端实现指导 | `docs/frontend-implementation-guide-scheduling-performance.md` |

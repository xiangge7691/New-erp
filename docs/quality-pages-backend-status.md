# 生产质量三页面 —— 后端实施记录

> 日期：2026-11-06  
> 对应计划：`docs/backend-implementation-plan.md`  
> 状态：**第一版后端接口已落地并编译/冒烟通过**；前端尚未接这些接口。  
> 测试库：`8.134.48.54/erp_db`（已执行建表与文件要求初始化）。

---

## 1. 已完成的数据库变更

迁移脚本：`src/main/resources/Data/migration/create_quality_pages_tables.sql`（已在测试库执行）。

流程顺序已调整为 **请检 → 检验计划 → 取样 → 检验记录 → 留样 → 销毁（非必须）**；存量库需执行 `src/main/resources/Data/migration/reorder_inspection_steps.sql`。

请检单创建后自动生成检验任务（一张请检单对应一张任务），同一工单+品类+批号下允许多条任务；存量库需执行 `src/main/resources/Data/migration/inspection_task_request_unique.sql`（已在测试库执行）。

生产原料请检单不关联工单，同样自动生成任务（任务工单字段为空）；存量库需执行 `src/main/resources/Data/migration/inspection_task_allow_no_work_order.sql`（已在测试库执行）。

存量请检单可用 `./mvnw test -Dtest=InspectionTaskBackfillRunner` 补建任务（幂等，已关联任务的请检单自动跳过；测试库已执行，补出 14 条）。

| 表 | 用途 | 测试库状态 |
|----|------|-----------|
| `inspection_task` | 检验任务主表（六步流程索引） | 已建，请检单创建时自动生成任务 |
| `inspection_task_step` | 检验任务六步记录 | 已建，0 行 |
| `work_order_process_check` | 审核放行工序符合性检查 | 已建，0 行 |
| `file_requirement_config` | 必传/条件必传/不必传配置 | 已建，35 条初始化数据 |

文件类型注册表新增：
- `BizFileType.INSPECTION_PLAN`（检验计划附件）
- `file-types.yml` 增加 `INSPECTION_PLAN: 检验计划`

---

## 2. 已实现接口

### 2.1 产品信息汇总

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/product-summary/overview` | GET | 页面聚合：结论、KPI、待办、文件健康度、制剂卡片 |
| `/api/product-summary/batches` | GET | 批次汇总列表，支持品种/日期类型/起止日期/关键字/只看缺件/分页；含未下达计划 |
| `/api/product-summary/batches/{workOrderId}` | GET | 批次全链路详情：槽位、节点、单据明细、文件分组、缺件清单 |
| `/api/product-summary/plans/{planId}` | GET | 未下达工单的计划详情 |
| `/api/product-summary/cards` | GET | 品种卡片视图数据（不分页） |
| `/api/product-summary/export` | POST | 集中导出：`SELECTED`/`FILTERED`/`ALL`，`ZIP`/`CSV`，附 `文件清单.csv` |

代码：
- `Controller/ProductSummaryController.java`
- `Service/ProductSummaryService.java`
- `Service/impl/ProductSummaryServiceImpl.java`
- `Common/Dto/QualityPages/ProductSummaryDtos.java`
- 共用聚合：`Service/BatchQueryService(.impl)`、`BatchContext`

### 2.2 检验管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/inspection/tasks` | GET | 任务列表，支持品种/批号/被检品属性/环节/状态/关键字/分页 |
| `/api/inspection/batches` | GET | 批次视图，按工单/批次聚合多条检验任务 |
| `/api/inspection/batches/stats` | GET | 检验看板统计：待办/检验中/已完成、六步环节计数 |
| `/api/inspection/batches/{workOrderId}` | GET | 批次汇总详情（含任务明细） |
| `/api/inspection/tasks/{id}` | GET | 任务详情（六步、字段、附件） |
| `/api/inspection/tasks` | POST | 创建任务（按工单 + 被检品属性；存在未关联请检单的同批次任务时返回该任务） |
| `/api/inspection/tasks/{id}/plan` | POST | 办理检验计划 |
| `/api/inspection/tasks/{id}/request` | POST | 办理请检 |
| `/api/inspection/tasks/{id}/sampling` | POST | 办理取样 |
| `/api/inspection/tasks/{id}/retention` | POST | 办理留样 |
| `/api/inspection/tasks/{id}/inspection` | POST | 办理检验 |
| `/api/inspection/tasks/{id}/destruction` | POST | 办理销毁 |
| `/api/inspection/tasks/{id}/skip` | POST | 跳过留样/销毁（带原因） |
| `/api/inspection/tasks/{id}/cancel` | POST | 作废任务 |
| `/api/inspection/tasks/{id}/archive` | POST | 归档任务（仅待开始/进行中；终态不可解除） |

代码：
- `Controller/InspectionTaskController.java`
- `Controller/InspectionBatchController.java`
- `Service/InspectionTaskService.java`
- `Service/impl/InspectionTaskServiceImpl.java`
- `Common/Dto/QualityPages/InspectionTaskDtos.java`
- 实体/Mapper：`InspectionTask`、`InspectionTaskStep` + Mapper
- 归档字段迁移：`Data/migration/add_inspection_task_archive_fields.sql`（`archive_time`、`archive_by`）
- 数据修正：`Data/migration/fix_inspection_plan_type_by_task_category.sql`（按任务被检品属性修正检验计划类型）

流程规则：
- 六步顺序：`REQUEST → PLAN → SAMPLING → INSPECTION → RETENTION → DESTRUCTION(非必须) → DONE`。
- 请检单（原请检页面）创建成功后，后端在同一事务内自动生成检验任务：完成 `REQUEST` 环节并推进到 `PLAN`；一张请检单对应一张任务，同一工单+品类+批号下允许多条任务。
- 请检单填写的检验计划编号若在库中存在，任务自动关联并跳过 `PLAN` 环节。
- 生产原料请检单不关联工单，直接用请检单自身信息（被检物名称、批号、规格）生成任务，工单字段为空；此类任务只在“按任务”视图展示，批次视图按工单聚合、不包含它们。
- 检验任务页先行创建且未关联请检单的任务，会被后续同工单+品类+批号的请检单直接关联复用。
- 不允许跳步；留样/销毁可按任务配置或 `skip` 接口跳过，必须写原因。
- 每一步在同一事务内写原业务表、更新任务引用、推进 `current_step`、写 `inspection_task_step`。
- 已实测：创建 → 请检 → 计划 → 取样 → 检验记录 → 留样 → 销毁（可选）全流程通过。
- 已实测：请检单创建后自动生成任务（`InspectionTaskAutoCreateTest`），两张同批次请检单生成两条独立任务。
- 已实测：检验批次列表按工单聚合，批次下钻返回该批次全部任务。

### 2.3 审核放行

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/release-review/batches` | GET | 批次审核列表（文件/工序/偏差/合规汇总，返回 `blockers`） |
| `/api/release-review/batches/stats` | GET | 审核看板统计：待审核/可放行/存在阻塞/已放行/已拒绝 |
| `/api/release-review/batches/varieties` | GET | 品种下拉选项（有批次的品种，按编码去重） |
| `/api/release-review/batches/{workOrderId}` | GET | 全批审核详情（采购/验收/指令/领料/退库/工序/检验/区域/收率/偏差/文件） |
| `/api/release-review/batches/{workOrderId}/process-checks` | POST | 保存工序符合性检查 |
| `/api/release-review/batches/{workOrderId}/draft` | POST | 保存放行审核草稿（结论“待补充”，不校验合规） |
| `/api/release-review/batches/{workOrderId}/approve` | POST | 通过放行（校验合规、禁止重复放行） |
| `/api/release-review/batches/{workOrderId}/reject` | POST | 拒绝放行（原因必填，禁止拒绝已放行批次） |
| `/api/release-review/batches/export` | POST | 导出待审核批次 CSV（默认 `status=待审核`） |

代码：
- `Controller/ReleaseBatchController.java`
- `Service/ReleaseBatchService.java`
- `Service/impl/ReleaseBatchServiceImpl.java`
- `Common/Dto/QualityPages/ReleaseBatchDtos.java`
- `WorkOrderProcessCheck` + Mapper

合规规则：
- 检验结论必须为“合格”（只取成品检验记录；无记录时按工单状态显示“检验中/已检验/未检验”）。
- 工序检查必须全部“符合”（无不符合/待查）。
- 必传文件必须齐全（`file_requirement_config` 中 `REQUIRED` / `CONDITIONAL`）。
- 偏差必须关闭（`handleTime` 或处理结果存在）。
- 收率 ≥ 98%。
- 全部通过才允许 `approve`；否则返回 400，不写放行记录。

放行页文件清单口径：
- 只统计批次相关业务类型，品种级资料（`PREPARATION` / `PREPARATION_PROCESS_TEMPLATE`）不计入放行缺件。
- 已补 `SAMPLING_RECORD`、`WORK_ORDER_PROCESS_EXECUTION` 的应传/已传统计。
- `RELEASE_REVIEW` 在放行记录存在时计 1/1（放行记录本身即放行审核单据），避免放行后自锁。
- 多级配置按 `bizType + fileKind` 去重，优先级：品种级 > 被检品属性级 > GLOBAL。

放行状态与动作：
- 状态取值：`待审核 / 待放行 / 已放行 / 已拒绝`。
- 草稿结论为“待补充”，不校验合规、不写 `audit_release_time`；草稿不会强制把合规批次改成“待审核”。
- 已放行批次只读：`approve` / `reject` / `draft` / `process-checks` 均返回 400。
- `approve` 成功后通过 `WorkOrderService.syncWorkOrderTime` 回写放行时间并联动工单/计划状态。
- `reject` 原因必填；`reviewDate`（`yyyy-MM-dd`）与 `reviewTime` 均兼容，未传审核人时取当前登录用户。

已实测：
- 批次列表/详情返回正常。
- 工序检查批量保存正常。
- 存在缺件时 `approve` 返回“存在不符合项，不能通过放行”。
- 本次改动已编译通过；`QualityPagesSmokeTest` 已补充品种下拉、详情计划号/放行状态、文件清单排除品种资料、CSV 导出断言，冒烟测试待联调时执行。

---

## 3. 共用逻辑

### 3.1 批次聚合

`BatchQueryService` 一次加载：
- 工单、生产计划、制剂
- 采购订单 + 明细、验收单
- 领料单 + 明细、退库单
- 成品入库/出库 + 明细
- 请检、取样、检验、放行、偏差
- 工序执行、工序检查、品种工序要求、房间

产品和放行两个页面共用，避免重复查询逻辑。

### 3.2 文件要求

`FileRequirementService` 从 `file_requirement_config` 解析：
- 优先级：品种级 `PREPARATION` > 被检品属性 `ITEM_CATEGORY` > 全局 `GLOBAL`。
- 未配置返回 `OPTIONAL`。
- 缺件统计排除 `OPTIONAL`；产品汇总和放行文件完整性共用。

---

## 4. 测试

新增 `QualityPagesSmokeTest`（只读冒烟）：
- 产品汇总批次列表/详情：节点数 ≥ 6、槽位非空。
- 审核放行批次列表/详情：文件统计、工序统计、合规结论、文件清单存在。
- 检验任务详情：步骤数为 6。
- 检验批次视图：批次任务数、任务明细存在，批次任务接口返回数组。
- 检验看板统计：批次总数、待办数存在，六步环节计数为 6。
- 审核放行看板：总数/待审核/阻塞数存在，列表项返回 `blockers`。

执行结果：
```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

另手工验证：
- 检验任务创建 + 六步全流程 API 调用成功。
- 审核放行工序检查保存成功；缺件时放行被拦截。
- 产品导出接口返回 zip；开发环境文件路径指向生产存储目录时，zip 内清单会标记“文件缺失”，部署到对应文件服务器后正常。

---

## 5. 待确认/待补

| # | 项 | 现状 | 建议 |
|---|----|------|------|
| 1 | 工序要求具体来源 | 当前优先 `preparation_process_template.key_process_params`，否则 `process_type.process_description` | 确认现有“工序页”实际字段；如不同，调整 `BatchQueryServiceImpl` |
| 2 | 生产指令/批配置指令 | 当前用 `production_plan` + `PRODUCTION_PLAN` 附件 | 确认是否有独立指令表/文件 |
| 3 | 退库单关联 | 按 `return_order.production_plan_no` 关联 | 确认是否可接受 |
| 4 | 销毁记录关联批次 | 通过 `inspection_task` 的留样引用创建销毁单 | 确认销毁单是否需要回写批次 |
| 5 | 权限 | 目前沿用 JWT，Controller 未加细粒度注解 | 补 `@PreAuthorize` 或权限拦截：检验员/留样/销毁/放行员 |
| 6 | 导出异步化 | 当前同步 zip | 大批量改异步任务 + 下载链接 |
| 7 | 前端联调 | 未开始 | 按 HTML 原型字段对接，先联调三个页面 |
| 8 | 测试数据 | 测试库已有手工创建的任务 ID 3 | 上线前清理或迁移到正式流程 |

---

## 6. 前端对接提示

- 产品汇总列表：`GET /api/product-summary/batches`，字段与原型 `BatchSummary` 对应；详情 `stages` 按 `key` 渲染计划/采购/工单/生产/入库/出库/检验/放行节点。
- 检验管理：列表 → `GET /api/inspection/tasks`；办理按钮按 `currentStep` 调对应 POST 接口；成功后重新拉详情。
- 检验批次视图：`GET /api/inspection/batches` 按工单/批次返回任务汇总（`taskCount`、`completedCount`、`currentStep`、`batchStatus`、`inspectionConclusion`）；顶部统计 → `GET /api/inspection/batches/stats`；批次下钻 → `GET /api/inspection/batches/{workOrderId}`（该接口已包含任务明细）。
- 审核放行：列表 → `GET /api/release-review/batches`（列表项直接含 `blockers`）；顶部统计 → `GET /api/release-review/batches/stats`；详情 → `GET .../{workOrderId}`；工序检查保存 → `POST .../process-checks`；放行/拒绝 → `POST .../approve|reject`。
- 文件下载：沿用现有 `/api/files/{id}` / `BusinessFileService`。

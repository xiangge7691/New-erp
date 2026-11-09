# 生产质量三页面 —— 后端实现计划

> 日期：2026-11-06  
> 范围：**产品信息汇总、检验管理、审核放行** 三个页面。工序要求引用现有“工序页”，不新增工序配置页面。  
> 技术栈：Spring Boot 3.5.5、MyBatis-Plus 3.5.5、MySQL 8、JWT、Argon2。  
> 原型：`_analysis/prototypes/`（三个独立 HTML：`product-summary.html`、`inspection-management.html`、`release-review.html`）。

---

## 0. 结论先行

| 页面 | 后端工作 | 主要复用 | 预计工时 |
|------|---------|---------|---------|
| 产品信息汇总 | 批次聚合查询、文件/缺件统计、导出 | production_plan、work_order、purchase_orders、acceptance_order、production_pick、stock_in/out、inspection_*、business_file | 3–4 天 |
| 检验管理 | 新增检验任务与六步流转、任务详情、附件 | inspection_plan/request、sampling_record、retained_sample、inspection_record、destruction_record | 5–6 天 |
| 审核放行 | 批次合规聚合、工序符合性、文件完整性、放行/拒绝 | work_order、release_review、preparation_process_template、work_order_process_execution、deviation_record、business_file | 5–6 天 |
| 公共 | 文件要求配置、权限、导出、测试 | BizFileType、BusinessFileService、用户/角色 | 3–4 天 |

**总计约 15–20 个工作日**（1 名后端；产品汇总与检验/放行可部分并行）。  
建议顺序：**数据模型/迁移 → 检验任务 → 审核放行 → 产品汇总聚合 → 导出/权限/测试**。

---

## 1. 现状与复用

### 1.1 已有表

| 业务 | 表 | 关键字段 |
|------|----|---------|
| 生产计划 | `production_plan` | `id`、`plan_number`、`preparation_code/name`、`plan_quantity`、`plan_type`、`current_status`、`plan_production_time`、`production_start_time`、`production_end_time`、`plan_file` |
| 工单/批次 | `work_order` | `work_order_id`、`work_order_code`、`plan_id`、`batch_number`、`batch_qty`、`finished_qty`、`yield_rate`、`current_status`、`inbound_time`、`outbound_time`、`audit_release_time` |
| 采购 | `purchase_orders` / `purchase_order_items` | `production_plan_id`、`purchase_number`、`status`、`order_time` |
| 验收 | `acceptance_order` / `acceptance_detail` | `purchase_number`、`plan_code`、`work_order_code`、`status` |
| 领料 | `production_pick` / `production_pick_detail` | `work_order_id`、`plan_id`、`pick_code`、`status`、`stock_out_code` |
| 退库 | `return_order` | `return_no`、`production_plan_no`、`return_time`、`status`（无 work_order_id，需按计划号关联） |
| 入库/出库 | `stock_in/out` + detail | `work_order_id`、`plan_number`、`in_type/out_type` |
| 检验计划 | `inspection_plan` | `plan_code`、`object_name`、`batch_no`、`inspection_type`、`status` |
| 请检 | `inspection_request` | `work_order_id`、`inspection_code`、`item_category`、`batch_number`、`process_name` |
| 取样 | `sampling_record` | `related_plan_code`、`object_name`、`batch_no`、`sampling_time` |
| 留样 | `retained_sample` | `related_inspection_code`、`batch_no`、`expiry_date`、`status` |
| 检验 | `inspection_record` | `work_order_id`、`related_inspection_request_code`、`batch_no`、`conclusion`、`start_time/end_time` |
| 销毁 | `destruction_record` | `destruction_code`、`record_type`、`status`、`destroy_date`（**无批次/工单字段**） |
| 放行 | `release_review` | `work_order_id`、`batch_no`、`release_conclusion`、`reviewer`、`review_time` |
| 偏差 | `deviation_record` | `work_order_id`、`deviation_code`、`handle_time` |
| 工序 | `process_type`、`preparation_process_template`、`work_order_process_execution` | `key_process_params`、`step_order`、`start_time/end_time`、`status` |
| 文件 | `business_file` + `file_info` | `biz_type`、`biz_id`、`file_kind` |
| 文件类型 | `BizFileType` / `BizFileKinds` | 一表一码，槽位上限 |

### 1.2 现有接口规范

- 统一响应：`ApiResponse<T>`，`code=200` 成功。
- 分页：`PagedResult<T>`，`pageIndex` 从 0 开始。
- 认证：JWT；业务接口统一 `/api` 前缀。
- 审计：`MybatisPlusMetaObjectHandler` 自动填充 `createdBy/updatedBy/createdTime/updatedTime`。
- 软删除：全局 `is_deleted`；唯一字段查重必须绕过软删除。
- 文件：`BusinessFileService` + `business_file`；新业务类型必须登记到 `BizFileType`。

---

## 2. 数据模型设计

### 2.1 新增：检验任务（`inspection_task`）

目的：把现有分散的检验计划/请检/取样/留样/检验/销毁串成一条可流转的任务，并与批次绑定。

```sql
CREATE TABLE inspection_task (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_code         VARCHAR(50)  NOT NULL COMMENT '检验任务号',
  work_order_id     BIGINT       NOT NULL COMMENT '工单ID',
  work_order_code   VARCHAR(50)  NOT NULL,
  plan_id           BIGINT       NULL COMMENT '生产计划ID',
  plan_number       VARCHAR(50)  NULL,
  preparation_id    BIGINT       NULL,
  preparation_code  VARCHAR(50)  NULL,
  preparation_name  VARCHAR(100) NULL,
  batch_no          VARCHAR(50)  NULL,
  item_category     VARCHAR(20)  NOT NULL COMMENT '成品/中间产品/生产原料',
  object_name       VARCHAR(200) NULL,
  spec              VARCHAR(100) NULL,
  current_step      VARCHAR(20)  NOT NULL COMMENT 'PLAN/REQUEST/SAMPLING/RETENTION/INSPECTION/DESTRUCTION/DONE',
  task_status       VARCHAR(20)  NOT NULL COMMENT '待开始/进行中/已完成/已跳过/已作废',
  retention_required TINYINT     NOT NULL DEFAULT 1 COMMENT '是否要求留样',
  destruction_required TINYINT   NOT NULL DEFAULT 1 COMMENT '是否要求销毁',
  plan_ref_id       BIGINT       NULL COMMENT 'inspection_plan.id',
  request_ref_id    BIGINT       NULL COMMENT 'inspection_request.id',
  sampling_ref_id   BIGINT       NULL COMMENT 'sampling_record.id',
  retention_ref_id  BIGINT       NULL COMMENT 'retained_sample.id',
  inspection_ref_id BIGINT       NULL COMMENT 'inspection_record.id',
  destruction_ref_id BIGINT      NULL COMMENT 'destruction_record.id',
  start_time        DATETIME     NULL,
  complete_time     DATETIME     NULL,
  remark            VARCHAR(500) NULL,
  is_deleted        TINYINT      NOT NULL DEFAULT 0,
  version           INT          NOT NULL DEFAULT 1,
  created_by        BIGINT       NULL,
  created_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by        BIGINT       NULL,
  updated_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inspection_task_code (task_code),
  UNIQUE KEY uk_inspection_task_batch (work_order_id, item_category, batch_no),
  KEY idx_inspection_task_step (current_step, task_status),
  KEY idx_inspection_task_plan (plan_id)
) COMMENT='检验任务（六步流程索引）';
```

> 说明：`batch_no` 可能为空（未生成批号），MySQL 唯一索引对 NULL 不生效；可在业务层用 `COALESCE(batch_no,'')` 或额外 `batch_key` 字段保证唯一。建议增加 `batch_key VARCHAR(50) GENERATED ALWAYS AS (COALESCE(batch_no,'')) STORED`，唯一键用 `work_order_id,item_category,batch_key`。

### 2.2 新增：检验任务步骤（`inspection_task_step`）

目的：记录每一步的引用记录、状态、办理人、跳过原因，避免给 6 张已有表加字段。

```sql
CREATE TABLE inspection_task_step (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id        BIGINT      NOT NULL,
  step_code      VARCHAR(20) NOT NULL COMMENT 'PLAN/REQUEST/SAMPLING/RETENTION/INSPECTION/DESTRUCTION',
  step_status    VARCHAR(20) NOT NULL COMMENT '待办理/进行中/已完成/已跳过',
  ref_table      VARCHAR(50) NULL COMMENT '引用表名',
  ref_id         BIGINT      NULL,
  operator_id    BIGINT      NULL,
  operator_name  VARCHAR(50) NULL,
  start_time     DATETIME    NULL,
  complete_time  DATETIME    NULL,
  skip_reason    VARCHAR(500) NULL,
  remark         VARCHAR(500) NULL,
  is_deleted     TINYINT     NOT NULL DEFAULT 0,
  version        INT         NOT NULL DEFAULT 1,
  created_by     BIGINT      NULL,
  created_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by     BIGINT      NULL,
  updated_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_step (task_id, step_code),
  KEY idx_step_status (step_status)
) COMMENT='检验任务步骤';
```

### 2.3 新增：工序符合性检查（`work_order_process_check`）

审核放行需要“每种工序是否符合要求、工艺查证人、日期”，现有 `work_order_process_execution` 只有执行时间/状态，没有检查结论/查证人。建议新增：

```sql
CREATE TABLE work_order_process_check (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id  BIGINT      NOT NULL,
  execution_id   BIGINT      NULL COMMENT 'work_order_process_execution.id',
  process_type_id BIGINT     NULL,
  process_name   VARCHAR(100) NOT NULL,
  requirement_snapshot TEXT   NULL COMMENT '检查时点的要求快照',
  check_result   VARCHAR(20) NOT NULL COMMENT '符合/不符合/待查',
  inspector_id   BIGINT      NULL,
  inspector_name VARCHAR(50) NULL,
  check_date     DATE        NULL,
  remark         VARCHAR(500) NULL,
  is_deleted     TINYINT     NOT NULL DEFAULT 0,
  version        INT         NOT NULL DEFAULT 1,
  created_by     BIGINT      NULL,
  created_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by     BIGINT      NULL,
  updated_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_process_check_wo (work_order_id),
  KEY idx_process_check_exec (execution_id)
) COMMENT='工单工序符合性检查';
```

### 2.4 新增：文件要求配置（`file_requirement_config`）

把“必传/条件必传/不必传”做成可配置，供产品信息汇总缺件判定和审核放行文件完整性共用。

```sql
CREATE TABLE file_requirement_config (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  scope        VARCHAR(20) NOT NULL COMMENT 'GLOBAL/PREPARATION/ITEM_CATEGORY',
  scope_key    VARCHAR(100) NULL COMMENT '品种编码/被检品属性等',
  biz_type     VARCHAR(50) NOT NULL,
  file_kind    VARCHAR(50) NOT NULL,
  requirement  VARCHAR(20) NOT NULL COMMENT 'REQUIRED/CONDITIONAL/OPTIONAL',
  stage        VARCHAR(50) NULL COMMENT '计划/采购/验收/工单/工序/检验/放行',
  display_name VARCHAR(100) NULL,
  remark       VARCHAR(500) NULL,
  is_deleted   TINYINT NOT NULL DEFAULT 0,
  version      INT NOT NULL DEFAULT 1,
  created_by   BIGINT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   BIGINT NULL,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_file_req (scope, scope_key, biz_type, file_kind)
) COMMENT='文件必传要求配置';
```

初始数据按已确认清单：
- 必传：工艺规程、质量标准、批记录模板、验收检验报告、批生产记录、检验记录/图谱、放行审核。
- 条件必传：验证方案、生产计划、采购订单、随货清单、请检单、取样记录。
- 可选/不必传：工艺流程图、工序模板、工序附件、发票、其他文件、领料/入库/出库附件、标红项。

### 2.5 迁移与回填

1. 建表（幂等 `CREATE TABLE IF NOT EXISTS`）。
2. 回填 `inspection_task`：
   - 从 `work_order` + `inspection_request` 生成任务，按 `work_order_id + item_category + batch_no` 去重。
   - 关联已有 `inspection_plan`（按 `object_name/batch_no` 或计划号规则）、`inspection_request`、`inspection_record`。
   - `sampling_record` 目前只有 `related_plan_code`，按计划号+批号回填；无法匹配的生成“待关联”任务，人工确认。
   - `retained_sample` 按 `related_inspection_code` 关联检验记录，再找到任务。
   - `destruction_record` 无批次字段，按销毁明细/单据人工或规则关联；无法关联的记录保留在原模块。
3. 回填 `work_order_process_check`：默认 `待查`，由审核放行人补录；已有 `work_order_process_execution.status` 可作为初值，但不能替代人工结论。
4. 初始化 `file_requirement_config`。
5. 新增 `BizFileType`：
   - `INSPECTION_TASK`（检验任务附件，kind 按六步）
   - 可选 `INSPECTION_PLAN`（检验计划附件，当前注册表没有）
   - 已有 `INSPECTION_REQUEST`、`SAMPLING_RECORD`、`RETAINED_SAMPLE`、`INSPECTION_RECORD`、`DESTRUCTION_RECORD` 继续使用。
   - 如果任务附件统一挂任务，则只需 `INSPECTION_TASK`；如果挂原单据，沿用原类型，不重复。

---

## 3. 接口设计

### 3.1 产品信息汇总

#### 3.1.1 批次列表

```
GET /api/product-summary/batches
  ?varietyCode=&dateType=planDate|workOrderDate|inboundDate|inspectionDate
  &startDate=&endDate=&keyword=&onlyMissing=&pageIndex=0&pageSize=20
```

返回 `PagedResult<ProductSummaryBatchDto>`：

```json
{
  "workOrderId": 463,
  "workOrderCode": "GD202610200001",
  "planId": 306,
  "planNumber": "Plan202610190001",
  "preparationCode": "Z04-011",
  "preparationName": "疏肝养胃丸",
  "spec": "每50丸重6g...",
  "batchNo": "260901",
  "planQty": 7000,
  "finishedQty": 7001,
  "unitName": "盒",
  "materialSource": "PURCHASE",
  "planDate": "2026-09-16",
  "productionStart": "2026-09-16",
  "productionEnd": "2026-09-24",
  "yieldRate": 100.01,
  "inspectionConclusion": "合格",
  "status": "已完成",
  "fileCount": 12,
  "missingCount": 10,
  "missingSummary": "计划 1、采购 2、工单 1 等 6 个环节"
}
```

实现要点：
- 主查询：`work_order` + `production_plan` + `preparation`。
- 采购/领料：按 `plan_id` 取 `purchase_orders` 或 `production_pick`，二选一。
- 检验：按 `work_order_id` 取最新 `inspection_record`。
- 文件：批量查 `business_file`，按 `biz_type + biz_id` 分组；禁止 N+1。
- 缺件：读取 `file_requirement_config`，按槽位计算必传/条件必传缺失。
- `onlyMissing`：用 SQL `EXISTS` 或先聚合再过滤（数据量小可内存过滤）。

#### 3.1.2 批次详情 / 全链路

```
GET /api/product-summary/batches/{workOrderId}
```

返回 `ProductSummaryDetailDto`，包含：
- `batch`：批次信息；
- `slots`：槽位概览（已传/应传/缺）；
- `stages`：计划、采购/领料、工单、生产、入库、出库、检验、放行节点；
- `purchases` / `requisitions` / `stockIns` / `stockOuts` / `inspectionRequests` / `inspections` / `releases` / `deviations`；
- `files`：按槽位分组的文件；
- `missingItems`：缺件清单（环节、槽位、单号、bizType/fileKind）。

单据明细直接返回子表，例如采购 items、领料 details、入库/出库 details，避免前端二次请求。

#### 3.1.3 导出

```
POST /api/product-summary/export
{
  "workOrderIds": [463, 462],
  "varietyCode": "Z04-011",
  "includeProductFiles": true,
  "levels": ["REQUIRED", "CONDITIONAL"]
}
```

- 同步导出适合小批量；超过 200 个文件或 200MB 建议异步任务。
- zip 结构：`品种/批次/环节/文件`，附 `文件清单.csv`（含缺失项）。
- 复用 `FileStorageService` 读取磁盘文件；文件名冲突加序号。
- 导出记录写审计日志。

### 3.2 检验管理

#### 3.2.1 任务列表

```
GET /api/inspection/tasks
  ?varietyCode=&batchNo=&itemCategory=&step=&status=&keyword=&pageIndex=0&pageSize=20
```

返回 `PagedResult<InspectionTaskSummaryDto>`：
- 批号、品种、工单、检验对象、当前环节、状态、计划/请检/取样/留样/检验/销毁单号、检验结论。

SQL 建议：
- `inspection_task` 主表分页；
- 批量查 `inspection_plan/request/sampling_record/retained_sample/inspection_record/destruction_record`（按 ref_id），避免 N+1。

批次视图（同一批次下可有多条成品/中间产品/生产原料任务）：

```
GET /api/inspection/batches
  ?varietyCode=&batchNo=&itemCategory=&step=&status=&keyword=&pageIndex=0&pageSize=20
GET /api/inspection/batches/stats
  ?varietyCode=&batchNo=&itemCategory=&status=&keyword=
GET /api/inspection/batches/{workOrderId}
GET /api/inspection/batches/{workOrderId}/tasks
```

批次汇总返回 `taskCount`、`completedCount`、`currentStep`、`batchStatus`、`inspectionConclusion`，并内嵌 `tasks` 明细。

#### 3.2.2 任务详情

```
GET /api/inspection/tasks/{id}
```

返回六步 DTO：

```json
{
  "task": { "id": 1, "taskCode": "JY-RW-...", "batchNo": "260901", "workOrderCode": "...", "itemCategory": "成品", "currentStep": "INSPECTION", "taskStatus": "进行中" },
  "steps": [
    { "step": "REQUEST", "status": "已完成", "refId": 1, "code": "QJ-...", "files": [...] },
    { "step": "PLAN", "status": "已完成", "refId": 2, "code": "JH-...", "operator": "...", "time": "...", "files": [...] },
    { "step": "SAMPLING", "status": "已完成", "refId": 3, "code": "QY-...", "files": [...] },
    { "step": "INSPECTION", "status": "进行中", "refId": 5, "code": "JY-...", "files": [...] },
    { "step": "RETENTION", "status": "待办理", "refId": null, "files": [] },
    { "step": "DESTRUCTION", "status": "不必办理", "refId": null, "files": [] }
  ]
}
```

#### 3.2.3 六步办理接口

| 步骤 | 接口 | 说明 |
|------|------|------|
| 请检 | `POST /api/inspection/tasks/{id}/request` | 创建 `inspection_request`；任务第一步 |
| 检验计划 | `POST /api/inspection/tasks/{id}/plan` | 新建/关联 `inspection_plan`；校验请检完成 |
| 取样 | `POST /api/inspection/tasks/{id}/sampling` | 创建 `sampling_record`；校验计划完成 |
| 检验记录 | `POST /api/inspection/tasks/{id}/inspection` | 创建/更新 `inspection_record`；校验取样完成 |
| 留样 | `POST /api/inspection/tasks/{id}/retention` | 创建/更新 `retained_sample`；校验检验记录完成 |
| 销毁 | `POST /api/inspection/tasks/{id}/destruction` | 创建 `destruction_record`；校验留样完成；非必须 |
| 跳过 | `POST /api/inspection/tasks/{id}/skip` | `{step, reason}`；仅允许留样/销毁，记录审计 |
| 作废 | `POST /api/inspection/tasks/{id}/cancel` | 状态改为已作废，保留记录 |

通用请求体包含业务字段 + `remark` + `files`（可选，先保存单据再关联附件，或使用已有临时上传 sync）。
- 每个接口使用 `@Transactional`，校验当前步骤与状态，成功后推进 `current_step`，写 `inspection_task_step`，维护 `version` 乐观锁。
- 重复提交用 `task_id + step` 唯一键 + 幂等校验。

#### 3.2.4 状态机

```
REQUEST → PLAN → SAMPLING → INSPECTION → RETENTION → DESTRUCTION(非必须) → DONE
```

规则：
- 不允许跳步（留样/销毁可按配置跳过）。
- `retention_required=0` 时允许 INSPECTION 后跳过 RETENTION。
- `destruction_required=0` 时允许 RETENTION 后跳过 DESTRUCTION，任务直接 DONE。
- 检验结论 `不合格` 时任务状态标红；是否允许销毁/放行由质量规则决定。
- 作废需权限，已产生下游放行的任务不允许作废。

### 3.3 审核放行

#### 3.3.1 批次列表

```
GET /api/release-review/batches
GET /api/release-review/batches/stats
  ?varietyCode=&batchNo=&status=&keyword=&pageIndex=0&pageSize=20
```

返回 `PagedResult<ReleaseBatchSummaryDto>`：
- 品种/规格、批号/工单、批量/成品、计划/实际生产日期、检验结论、工序符合数、文件完整数、偏差数、放行状态、是否可放行、阻塞项 `blockers`。
- 顶部统计单独调用 `/stats`，返回待审核、可放行、存在阻塞、已放行、已拒绝。

实现：
- 主查询 `work_order`（`batch_number` 非空，按 `work_order_id` 唯一）。
- 子查询/批量查询：
  - 最新 `inspection_record`（work_order_id，按 end_time/id 倒序取 1）。
  - `release_review`（work_order_id，按 review_time/id 倒序取 1）。
  - `deviation_record`（work_order_id）。
  - `work_order_process_check` 统计符合/不符合/待查。
  - `file_requirement_config` + `business_file` 统计文件完整/缺件。
- 所有统计一次性批量查询，避免逐行查。

#### 3.3.2 批次详情

```
GET /api/release-review/batches/{workOrderId}
```

返回 `ReleaseBatchDetailDto`：
- `batch`：品种、规格、批号、批量、成品量、计划/实际生产日期、收率、放行结论/批准人/日期；
- `purchases`：采购订单及明细摘要、文件；
- `acceptances`：验收单、随货清单/检验报告/发票状态；
- `instruction`：生产指令/批配置指令（来源待确认，见风险）；
- `requisitions` / `returns`；
- `processes`：工序、要求快照、结果、查证人、日期、文件；
- `inspections`：请检、取样、检验报告、结论；
- `rooms`：区域/环境记录；
- `yield`：计划量、成品量、收率、标准；
- `deviations`：偏差、结论、质量管理员、日期；
- `files`：文件完整性清单（必传/条件/可选、已传/应传、缺失）；
- `compliance`：检验、工序、文件、偏差、收率、是否可放行。

#### 3.3.3 放行操作

| 接口 | 说明 |
|------|------|
| `POST /api/release-review/batches/{workOrderId}/approve` | `{approver, reviewTime, opinion}`；校验合规，写 `release_review`，回写 `work_order.audit_release_time` |
| `POST /api/release-review/batches/{workOrderId}/reject` | `{reviewer, reviewTime, reason}`；写拒绝记录 |
| `POST /api/release-review/batches/{workOrderId}/draft` | 保存草稿（可选，放行意见/审核人） |
| `GET /api/release-review/batches/{workOrderId}/files` | 文件完整性清单 |
| `POST /api/release-review/batches/{workOrderId}/process-checks` | 批量保存工序检查结果（工序、结果、查证人、日期） |

规则：
- 放行前必须满足：检验合格、必传文件齐全、工序无不符合/待查、偏差关闭、收率达标。
- 放行权限：质量负责人/授权放行员；拒绝/放行都写审计。
- 已放行批次不允许重复放行；拒绝后允许补充资料再提交。
- 放行结论写入 `release_review`，不直接改历史检验记录。

---

## 4. 文件与缺件规则

1. 统一从 `file_requirement_config` 读取必传要求，不再把规则写死在 Controller。
2. 文件统计按 `biz_type + biz_id + file_kind` 去重，忽略 `is_deleted=1`。
3. 品种级资料（工艺规程、质量标准、批记录模板、验证方案、工艺流程图、工序模板）按 `preparation_id` 统计一次，不按批次重复。
4. 采购/验收/生产/检验等按单据 ID 统计。
5. “不必传”项不参与缺件；可选但已上传的文件仍展示。
6. 文件类型必须登记 `BizFileType`；新增类型时同步 `file-types.yml` 目录映射。
7. 文件删除仍使用现有 `BusinessFileService` 的软删除/物理清理规则，不绕过唯一约束。

---

## 5. 权限、事务与审计

| 项 | 方案 |
|----|------|
| 认证 | 现有 JWT；Controller 继承 `BaseController` |
| 权限 | 检验：质量部/检验员；留样：留样管理员；销毁：销毁管理员；放行：质量负责人/授权放行员；汇总：生产/质量/管理层 |
| 事务 | 每个步骤接口 `@Transactional`；任务推进 + 步骤记录 + 单据保存同一事务 |
| 乐观锁 | `inspection_task.version`、`work_order_process_check.version`；更新时 `version` 条件 |
| 幂等 | `task_id + step_code` 唯一；重复提交返回已有记录 |
| 审计 | 自动填充 + `file_operation_log`（已有）；放行/拒绝/跳过写业务日志 |
| 软删除 | 任务/步骤/检查记录 `is_deleted`；放行记录不物理删除 |
| 并发 | 同一任务步骤加唯一键；重复放行用 `release_review` 唯一/状态校验 |

---

## 6. 性能与查询

1. 列表接口禁止 N+1：先分页主表，再按 ID 批量查子表/文件/统计。
2. 文件统计用 `GROUP BY biz_type,biz_id` 一次查询。
3. 产品汇总批次列表可按 `work_order_id` 分页，再批量聚合；数据量大时增加汇总表/物化视图（后续）。
4. 导出异步化：超过阈值走任务表 + 下载链接；任务状态可查询。
5. 索引建议：
   - `inspection_task(work_order_id, item_category, batch_no)`、`(current_step, task_status)`；
   - `inspection_task_step(task_id, step_code)`；
   - `work_order_process_check(work_order_id)`；
   - `business_file(biz_type, biz_id, file_kind)`（如已有则确认）；
   - `release_review(work_order_id, review_time)`；
   - `inspection_record(work_order_id, end_time)`。

---

## 7. 实施排期

| 阶段 | 内容 | 产出 | 工时 |
|------|------|------|------|
| P0 | 确认字段/口径；评审 DDL | 表结构、接口字段确认 | 1–2 天 |
| P1 | 建表/迁移/回填；`BizFileType` 新增；文件要求配置 | DDL、迁移脚本、初始化数据 | 2–3 天 |
| P2 | 检验任务：列表/详情/六步接口/状态机/附件 | Controller/Service/Mapper/DTO/测试 | 5–6 天 |
| P3 | 审核放行：批次聚合/合规/工序检查/放行/拒绝 | 同上 | 5–6 天 |
| P4 | 产品汇总：列表/详情/槽位/文件/缺件/导出 | 同上 | 3–4 天 |
| P5 | 权限、审计、性能、联调、测试 | 权限配置、测试报告、API 文档 | 3–4 天 |

> 可与前端并行：P1 完成后先出 OpenAPI/接口文档；P2/P3 完成即可联调；P4 可并行开发。

---

## 8. 风险与待确认

| # | 问题 | 影响 | 建议 |
|---|------|------|------|
| 1 | **生产指令/批配置指令**没有独立表 | 放行页“生产指令”字段来源不明确 | 确认是否用 `production_plan.plan_file`、工单配置字段，或新增指令表 |
| 2 | **退库单**只有 `production_plan_no`，没有 work_order_id | 按批次关联需经计划号 | 确认按计划号关联是否可接受 |
| 3 | **取样记录**只有 `related_plan_code`，没有 request/task id | 回填任务可能不唯一 | 新增 `inspection_task_id` 或按计划+批号+对象匹配 |
| 4 | **销毁记录**没有批次/工单字段 | 无法直接关联批次 | 通过 `inspection_task_step` 关联，或给销毁记录加 `work_order_id` |
| 5 | **工序要求**来源 | 现有 `process_type` 只有描述；`preparation_process_template.key_process_params` 是品种工艺参数 | 确认“工序页”配置的是哪张表；如是模板参数，放行页按品种+工序取快照 |
| 6 | 工序“是否符合”没有现成字段 | 放行页无法直接展示 | 新增 `work_order_process_check`，由查证人录入 |
| 7 | 一个工单对应多个检验对象/多个批次 | 任务唯一键设计 | 确认 `work_order_id + item_category + batch_no` 是否足够 |
| 8 | 留样/销毁是否每批必做 | 状态机分支 | 用 `retention_required`/`destruction_required` 配置，允许跳过并留原因 |
| 9 | 放行权限/电子签名 | 合规要求 | 确认角色、是否需二次确认/签名 |
| 10 | 导出文件量大 | 超时/内存 | 阈值以上异步 zip |
| 11 | 现有数据质量：工序状态未回写、文件缺失/重复 | 统计偏差 | 先做数据清洗/回填，再上线合规校验 |
| 12 | 文件必传配置维护入口 | 规则变化 | 先由初始化 SQL/YAML 维护，后续再做配置页（本次不新增页面） |

---

## 9. 验收清单

### 产品信息汇总
- [ ] 按品种/时间/关键字/只看缺件筛选正确。
- [ ] 批次列表槽位统计（已传/应传/缺）与文件表一致。
- [ ] 批次详情包含计划、采购/领料、工单、生产、入库、出库、检验、放行、偏差。
- [ ] 单据明细（采购 items、领料 details、出入库 details）正确。
- [ ] 缺件清单按环节/槽位/单号展示；不必传项不计入。
- [ ] 导出 zip/清单包含文件与缺失项。

### 检验管理
- [ ] 任务列表按环节/状态/品种/批号筛选。
- [ ] 六步顺序校验：不能跳步，留样/销毁可按配置跳过。
- [ ] 办理后任务状态、步骤记录、单据引用同时更新。
- [ ] 附件按步骤保存/查询。
- [ ] 并发重复提交不产生重复单据。

### 审核放行
- [ ] 批次列表合规汇总与详情一致。
- [ ] 检验、工序、文件、偏差、收率五项校验正确。
- [ ] 工序要求来自现有工序页配置，结果/查证人/日期可保存。
- [ ] 合规时才能通过放行；拒绝原因必填并留痕。
- [ ] 放行后回写工单放行时间，重复放行被拦截。

---

## 10. 建议的代码落点

```
Controller/
  ProductSummaryController.java
  InspectionTaskController.java
  ReleaseBatchController.java        // 或扩展 ReleaseReviewController
Service/
  ProductSummaryService.java
  InspectionTaskService.java
  ReleaseBatchService.java
  FileRequirementService.java
Service/impl/
  ...Impl.java
Data/Entity/
  InspectionTask.java
  InspectionTaskStep.java
  WorkOrderProcessCheck.java
  FileRequirementConfig.java
Data/mapper/
  ...Mapper.java
Common/Dto/
  ProductSummary/...
  InspectionTask/...
  ReleaseBatch/...
```

> 建议新增独立 Controller 而不是把聚合逻辑塞进现有 CRUD Controller；现有 `InspectionPlanController`、`ReleaseReviewController` 保持原职责，新页面通过聚合接口读取。

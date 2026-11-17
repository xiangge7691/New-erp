# 生产工序 / 制剂工序模板 排序功能改造计划

> 制定日期：2026-11-14
> 范围：**生产管理 → 工单工序（work_order_process_execution）**、**制剂管理 → 工序模板（preparation_process_template）**
> 说明：如果“生产的工序”实际指**生产计划详情里的工序记录（production_process_record）**，本文第 4.4 节给出了同样的增量方案，可一并实施。

---

## 1. 现状结论

| 项目 | 制剂工序模板 | 工单工序 | 生产计划工序记录 |
|------|-------------|---------|-----------------|
| 排序字段 | `preparation_process_template.step_order` | `work_order_process_execution.step_order` | `production_process_record.step_order` |
| 查询排序 | `ORDER BY step_order, template_id` | `ORDER BY step_order, id` | `ORDER BY step_order`（无兜底列） |
| 批量保存 | 按请求里的 `templateId` 原地更新，`stepOrder` 原样落库 | 按请求里的 `id` 原地更新，`stepOrder` 原样落库 | 先删后增，`stepOrder` 原样落库 |
| 新增单条 | `stepOrder` 由前端传，可为空 | 由模板复制 | 由前端传，可为空 |
| 排序接口 | 无 | 无 | 无 |

**核心问题**：字段和读取顺序都已经具备，但缺少“排序”这层业务能力：

1. `stepOrder` 完全信任前端传值，后端不重排、不校验重复、不补空值。前端只要漏传或手填重复，列表顺序就会乱。
2. 没有独立的排序/移动接口，前端只能靠用户手填数字实现排序。
3. 删除中间行后序号不连续，新增行也没有“追加到末尾”的默认序号。
4. 工单创建时从制剂模板复制 `step_order`（[WorkOrderServiceImpl.java:711](src/main/java/com/tonghui/erp/Service/impl/WorkOrderServiceImpl.java:711)），模板之后调整顺序不会影响已有工单，需要明确策略。

**结论**：本次改造的重点不是加字段，而是把「提交顺序」确立为唯一排序依据，并补齐后端归一化、排序接口、历史数据补齐和前端交互。

---

## 2. 目标与统一排序规则

1. **请求数组顺序 = 页面展示顺序 = 落库顺序**，`stepOrder` 落库后恒为 `1..N` 连续值。
2. 单条新增统一**追加到末尾**（`MAX(step_order)+1`），忽略前端传入的 `stepOrder`。
3. 删除行后由后端统一压缩序号，不要求前端补号。
4. 所有列表查询固定 `ORDER BY step_order ASC, 主键 ASC`，保证同序号时顺序稳定。
5. **不建 `(父ID, step_order)` 唯一索引**：逐行更新会出现瞬时重复，MySQL 唯一键会直接报错；顺序由服务层在事务内保证。
6. 排序只改 `step_order`，不触发配置室/设备校验、设备使用冲突校验。

---

## 3. 接口设计

### 3.1 制剂工序模板

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/preparationProcessTemplate/batch?preparationId=1` | POST | 保持现有接口；改为**以数组顺序重排** `stepOrder`，忽略行内传值 |
| `/api/preparationProcessTemplate/byPreparation/{prepId}/order` | PUT | 新增：按 ID 数组重排 |
| `/api/preparationProcessTemplate` | POST | 新增单条时自动追加序号 |

重排请求示例：

```http
PUT /api/preparationProcessTemplate/byPreparation/1/order
Content-Type: application/json

[10, 12, 11]
```

响应返回重排后的完整列表。校验规则：

- 所有 ID 必须属于该制剂，否则 400「存在不属于该制剂的工序模板」；
- ID 不允许重复，且必须覆盖当前全部行，否则 400「排序列表不完整」；
- 空列表且当前无数据时视为成功。

### 3.2 工单工序

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/work-order-process-execution/batch?workOrderId=1` | POST | 保持现有接口；保存前按数组顺序重排 `stepOrder` |
| `/api/work-order-process-execution/by-work-order/{workOrderId}/order` | PUT | 新增：按 ID 数组重排，**跳过设备冲突校验** |

重排请求示例：

```http
PUT /api/work-order-process-execution/by-work-order/1/order
Content-Type: application/json

[10, 12, 11]
```

额外业务约束（建议，待确认）：

- 仅当该工单所有工序都**未开始**（`start_time` 全为空）时允许重排；已有工序开工后顺序属于批记录的一部分，不允许再改。返回 400「工序已开始执行，不能调整顺序」。

### 3.3 生产计划工序记录（若纳入范围）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/process-record/batch/plan/{planId}` | POST | 保存时按数组顺序赋 `stepOrder = index + 1` |
| `/api/process-record/plan/{planId}/order` | PUT | 可选新增；因该接口是先删后增，前端重排序号后整表提交即可 |

### 3.4 模板变更对已有工单的影响（决策点）

当前工单创建后工序就固定了。模板排序调整后有三种策略：

| 策略 | 行为 | 建议 |
|------|------|------|
| A. 不同步 | 已有工单保持原顺序 | 默认，改动最小 |
| B. 手动同步 | 新增「按模板重排」按钮，仅未开工工单可用 | **推荐** |
| C. 自动同步 | 模板保存时联动所有未开工工单 | 风险高，不做 |

若选 B，新增 `POST /api/work-order-process-execution/sync-from-template/{workOrderId}`：按制剂最新模板顺序重排现有执行行；模板中已不存在的工序不自动删除，只调整共有工序的相对顺序，并在响应中提示差异。

---

## 4. 后端改造清单

### 4.1 公共服务

新增 `src/main/java/com/tonghui/erp/Common/utils/StepOrderUtils.java`：

- `normalize(List<T> rows, Function<T,Integer> getter, BiConsumer<T,Integer> setter)`：按列表顺序赋 `1..N`；
- `validateOrderedIds(List<Long> ids)`：非空、无重复校验；
- 供三张工序表复用，避免各 ServiceImpl 各写一套。

### 4.2 制剂工序模板

| 文件 | 改动 |
|------|------|
| `Service/impl/PreparationProcessTemplateServiceImpl.java` | `batchSave` 进入循环前归一化 `stepOrder`；新增 `reorder(preparationId, ids)` |
| `Service/PreparationProcessTemplateService.java` | 增加 `reorder` 方法声明 + Javadoc |
| `Controller/PreparationProcessTemplateController.java` | 新增 `PUT /byPreparation/{prepId}/order`，补 Javadoc 示例、`@OperationLog` |
| `Controller/PreparationProcessTemplateController.java` | `create` 未传序号时取 `MAX(step_order)+1`；`getAll` 排序改为 `preparation_id, step_order, template_id` |
| `Service/impl/PreparationServiceImpl.java` | `saveWithDetails` 已走 `batchSave`，无需改；确认一键保存后序号正确 |

### 4.3 工单工序

| 文件 | 改动 |
|------|------|
| `Service/impl/WorkOrderProcessExecutionServiceImpl.java` | `batchSave` 设备校验通过后、写库前归一化 `stepOrder`；新增 `reorder(workOrderId, ids)`（不调用 `EquipmentUsageConflictValidator`） |
| `Service/WorkOrderProcessExecutionService.java` | 增加 `reorder` 声明 + Javadoc |
| `Controller/WorkOrderProcessExecutionController.java` | 新增 `PUT /by-work-order/{workOrderId}/order` |
| `Service/impl/WorkOrderServiceImpl.java` | `bindProcessTemplates` 绑定后按列表顺序重新赋 `1..N`，避免模板历史脏数据带入工单 |

### 4.4 生产计划工序记录（可选）

| 文件 | 改动 |
|------|------|
| `Service/impl/ProductionProcessRecordServiceImpl.java` | `batchSaveByPlanId` 插入前按数组顺序赋 `stepOrder`；`listByPlanId`/分页查询补 `record_id` 兜底排序 |
| `Controller/ProductionProcessRecordController.java` | 如需独立排序，新增 `PUT /plan/{planId}/order` |

### 4.5 查询与返回

- 三个列表接口统一返回 `stepOrder`，前端直接用于展示序号。
- 分页接口（`/api/preparationProcessTemplate` 全量分页）当前按 `step_order` 跨制剂排序，翻页后序号会重复跳动；建议改为先按 `preparation_id` 再按 `step_order`，或要求前端按制剂过滤后使用。

---

## 5. 数据库改造

新增迁移脚本 `src/main/resources/Data/migration/add_process_step_order_index.sql`：

1. 历史数据补齐：对三张表按父 ID 分组、`COALESCE(step_order, 999999), 主键` 排序，用 `ROW_NUMBER() OVER (PARTITION BY ...)` 重写为 `1..N`（MySQL 8 支持）。
2. 列约束：`step_order INT NOT NULL DEFAULT 0`（三张表按实际情况确认当前可空性）。
3. 索引：
   ```sql
   ALTER TABLE preparation_process_template ADD INDEX idx_ppt_prep_step (preparation_id, step_order);
   ALTER TABLE work_order_process_execution ADD INDEX idx_wope_wo_step (work_order_id, step_order);
   ALTER TABLE production_process_record ADD INDEX idx_ppr_plan_step (plan_id, step_order);
   ```
4. **不添加唯一索引**。

> 数据测试与迁移验证统一使用 `8.134.48.54`，禁止在 `8.148.27.237` 写入测试数据。

---

## 6. 前端改造（接口契约见第 3 节）

### 6.1 制剂管理 → 制剂编辑 → 工序模板

- 表格新增「序号」列，行首提供拖拽手柄，或「上移 / 下移」按钮（首行/末行禁用）。
- 新增行一律追加到末尾，序号由页面按当前顺序重算，不要求用户填写。
- 删除行后前端立即重排序号。
- 保存（含制剂一键保存）时**按当前展示顺序提交数组**，`stepOrder` 可传可不传，后端以数组顺序为准。
- 保存成功后重新拉取 `byPreparation` 列表，以后端返回为准。

### 6.2 生产管理 → 工单 → 工序

- 交互同上。
- 若采用「已开工不允许排序」规则：任一行有 `startTime` 时禁用拖拽/上下移，并提示原因。
- 排序保存只调用 `order` 接口，不触发设备冲突校验，避免用户仅调整顺序却收到设备冲突报错。
- 排序完成后刷新列表。

### 6.3 生产计划 → 工序（如纳入）

- 交互同上；保存走现有批量接口即可。

---

## 7. 测试计划

| 类型 | 用例 |
|------|------|
| 批量保存 | 乱序提交、重复 `stepOrder`、空 `stepOrder` → 落库均为 `1..N` |
| 排序接口 | 正常重排；跨父级 ID、重复 ID、缺行 → 400 且数据不变 |
| 新增 | 单条新增自动追加末尾；删除中间行后序号连续 |
| 查询 | 列表顺序稳定；`byPreparation` / `by-work-order` 返回顺序与排序一致 |
| 工单绑定 | 新建工单工序顺序与制剂模板一致 |
| 业务约束 | 已开工工单调用排序接口被拒绝（若采用） |
| 回归 | 设备使用冲突校验、按行附件（`business_id`）、变更历史仍正常 |

建议新增/补充测试类：

- `PreparationProcessTemplateSortTest`
- `WorkOrderProcessExecutionSortTest`
- 现有 `PreparationTemplateAssociationTest`、`ProductionModuleTest` 增加排序断言

---

## 8. 风险与决策点

| # | 事项 | 建议 |
|---|------|------|
| 1 | 已开工工单是否允许重排 | 不允许，返回明确提示 |
| 2 | 制剂模板调整是否同步已有工单 | 默认不同步；提供「按模板重排」按钮（仅未开工工单） |
| 3 | 是否一并改造 `production_process_record` | 建议一并做，成本很小，避免三套语义 |
| 4 | 排序是否写入变更历史 | 会按行产生 `step_order` 变更日志，属预期；如嫌噪可在排序接口中合并为一条业务日志 |
| 5 | 分页列表跨制剂排序 | 改为按制剂分组排序，或要求前端按制剂过滤 |

---

## 9. 实施顺序与排期

| 阶段 | 内容 | 预计 |
|------|------|------|
| 1 | 公共 `StepOrderUtils` + 数据库迁移脚本 + 历史数据补齐 | 0.5 天 |
| 2 | 制剂工序模板：归一化、单条追加、排序接口、Javadoc | 0.5 天 |
| 3 | 工单工序：归一化、排序接口、已开工校验、模板绑定兜底 | 0.5 天 |
| 4 | 生产计划工序记录（可选） | 0.25 天 |
| 5 | 前端交互（两个页面） | 1 天 |
| 6 | 接口联调 + 测试 + 文档 | 0.5 天 |

后端总计约 **1.5 天**，含前端约 **3 天**；可在测试库 `8.134.48.54` 完成验证后部署。

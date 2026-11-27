# 「实际生产」实际起止时间与批次登记问题：原因分析与修改计划

> 对应页面：生产任务详情 → 实际生产（工序 / 批次维度）
> 对应接口：`POST /api/work-order-process-execution/batch`、`POST /api/work-order-process-execution/{id}/complete`、PDA `start/end/draft/submit`
> 对应数据表：`work_order_process_execution`、`work_order_process_plan`、`work_order_process_plan_batch`、`work_order_process_execution_participant`

---

## 一、结论摘要

四个现象不是同一个 bug，而是「实际生产」链路上四个设计层面的缺口叠加：

| # | 现象 | 直接原因 | 影响 |
|---|------|---------|------|
| 1 | 实际生产表没有「执行中」中间态，必须先把实际结束时间填上才能保存 | 保存动作同时承担「登记」和「完工」两件事：工时/绩效只在 start+end 齐全时计算，前端因此把结束时间做成必填；后端又没有独立的「开始 / 结束」动作语义 | 现场只能提前编一个结束时间，或干脆不登记；表格只剩「未开始」和「已完工」两种状态 |
| 2 | 每个工序的实际起止没有限定在生产任务配置时间 ~ 配置完成时间之内 | 保存接口只做了设备使用冲突校验，没有任务时间窗校验 | 实际时间可以写到任务窗口之外，工时统计与绩效失真 |
| 3 | 后道工序的实际开始可以早于前道工序的实际结束 | 没有工序先后校验，执行记录只按 `step_order` 排序展示，写入时不做约束 | 出现「灭菌 11-19、炼蜜 11-11」这类时间倒挂 |
| 4 | 拆分批次后只能保存一个批次的实绩，改另一个批次会把前一个批次的起止清空 | 执行记录定位键以 `plan_id`（工序）为主，拆分后多个批次共享同一 `plan_id`；保存时按「未提交即删除」全量覆盖，且前端会把工序级 `executionId` 回传给批次行 | 同一工序只剩一条执行记录，批次 A/B 实际数据互相覆盖 |

**落地情况**：四项改造已全部实施并通过单元测试（见第八节「实施记录」）。原代码中已有的时间校验器、批次定位器继续保留并补强；本轮新增保存/完工契约拆分、时长与反向校验、工单行锁、批次唯一索引迁移和历史数据巡检脚本。

---

## 二、逐条原因分析

### 2.1 问题 1：中间态（执行中）丢失

**数据模型本身是支持中间态的**：

- `work_order_process_execution.start_time` / `end_time` 均允许为 `NULL`（见 `_analysis/db-backup/erp_db_20261111_003300.sql:9501-9502`）；
- 状态由时间推导，而不是独立业务字段：

```java
// ProcessExecutionStatus.java:62-67
public static String resolve(LocalDateTime startTime, LocalDateTime endTime) {
    if (startTime == null && endTime == null) return 待执行;
    return endTime == null ? 执行中 : 已完成;
}
```

**问题出在契约层，不在模型层**：

1. 工时只认「结束时间」。`durationHours(start, end)` 在任一端为空时直接返回 `0`（`WorkOrderProcessExecutionServiceImpl.java:964-969`）；绩效生成同样要求两端齐全，否则直接跳过：

```java
// PerformanceGenerationServiceImpl.java:96
if (execution == null || execution.getStartTime() == null || execution.getEndTime() == null) {
    return 0;
}
```

2. 保存接口把「登记实绩」和「完工」混成一个动作：`POST /batch` 既要写开始时间，又要写结束时间，写齐了还会触发绩效生成（`WorkOrderProcessExecutionServiceImpl.java:842-859`），并且**完工但未登记参与人会直接拒绝整批保存**（`:849-854`）。前端为了「能保存、能算工时」，只能把结束时间做成必填。
3. 结果：现场在工序开始时无法登记，只能等完工后一次性补录；表格里自然看不到「执行中」。截图中「灭菌 批次 2」实际起止为「—」、操作列只有编辑/参与人图标，就是没有执行记录（或只有空记录）的表现。

> 结论：`结束时间` 是**计算工时/生成绩效**的必要条件，但不应该成为**保存实绩**的必要条件。两者需要拆开。

### 2.2 问题 2：没有任务时间窗校验

- `batchSave` 目前只调用了设备冲突校验，然后才调用时间校验器；在本次改造之前完全没有时间窗校验，任何时间都能落库（`WorkOrderProcessExecutionServiceImpl.java:351-357`）。
- 时间窗的取值必须是**生产任务（工单）**上的「配置日期 / 配置完成日期」，不是页面上的「计划起止（参考）」：

```java
// WorkOrder.java:176-186
config_date            // 配置日期
config_complete_time   // 配置完成日期
```

  截图里「干燥饮片」计划起止是 11-19，实际是 11-11，二者不一致是正常的：计划起止只是排产参考，校验基准是工单配置时间。
- 空值语义：`config_complete_time` 为空时只校验下界（实际时间必须晚于配置时间），这一点已在校验器中实现（`WorkOrderExecutionTimeValidator.java:148-174`）。

### 2.3 问题 3：没有工序先后校验

- 执行记录只按 `step_order` 排序展示（`WorkOrderProcessExecutionMapper.xml:90`），写入时不比较前序工序的实际结束时间。
- 分批次的场景要特别注意：**同一工序的多个批次允许时间重叠**，因此不能简单地「按工序串行」，比较对象应该是「`step_order` 更小的前序工序的实际最晚结束时间」。
- 截图中的数据（灭菌 11-19、炼蜜 11-11）是历史脏数据；当前校验器只会拦截**新写入**，不会自动修正旧数据。

### 2.4 问题 4：拆分批次互相覆盖

根因有三层，缺一不可：

1. **定位键错误**：拆分后一个 `plan_id` 对应多个 `plan_batch_id`。旧逻辑按 `plan_id` 定位执行记录，保存批次 B 时会把批次 A 的记录当成「本条工序的记录」更新掉。
2. **保存语义是「全量覆盖」**：旧逻辑把「本次未提交的既有行」直接删除；前端只回传当前编辑的一行时，另一个批次的记录就被删了。
3. **前端回传了错误的 `executionId`**：批次行拿工序级（或另一批次的）`executionId` 回传，后端按主键更新就会把 A 批次的数据写到 B 批次名下；此后 A 批次查不到执行记录，页面显示实际起止为空。

当前代码已改成「批次优先 + 只替换本次涉及的批次/工序」（`ExecutionRecordMatcher.java:80-141`），并且批次行没有 `executionId` 时按 `planBatchId` 定位新增；只要前端按契约回传，问题 4 不会再复现。

---

## 三、现状核对（改造前 vs 本轮）

| 能力 | 位置 | 状态 |
|------|------|------|
| 状态由起止时间推导（待执行/执行中/已完成） | `ProcessExecutionStatus.java:62-67` | ✅ 已有 |
| 只填开始时间也能保存（局部字段沿用原值） | `WorkOrderProcessExecutionServiceImpl.java:459-514` | ✅ 已有 |
| 完工接口自动补结束时间（未填则取当前时间）并锁行 | `WorkOrderProcessExecutionServiceImpl.java:769-811` | ✅ 已有 |
| 执行中参与人工时先记 0，完工回填 | `WorkOrderProcessExecutionServiceImpl.java:897-929`、`:822-831` | ✅ 已有 |
| 任务时间窗校验（配置时间 ~ 配置完成时间） | `WorkOrderExecutionTimeValidator.java:106-174` | ✅ 已有（PC + PDA） |
| 工序先后校验（不允许早于前序实际结束、前序未完工不允许开始） | `WorkOrderExecutionTimeValidator.java:196-226` | ✅ 已有 |
| 批次优先定位、只替换同批次/同工序 | `ExecutionRecordMatcher.java:53-141` | ✅ 已有 |
| 已登记实际生产的批次禁止重拆/删除 | `WorkOrderScheduleServiceImpl.java:732-740`、`:833-860`、`:897-917` | ✅ 已有 |
| 批次实际数据按批次聚合展示 | `WorkOrderScheduleServiceImpl.java:501-524`、`PlanBatchStatus.java:70-98` | ✅ 已有 |
| 保存/完工契约分离（开始登记 ≠ 完工） | `Controller.complete`、`ServiceImpl.complete(Long, LocalDateTime)` | ✅ 本轮已补 |
| 反向校验（改前序结束晚于后序开始） | `WorkOrderExecutionTimeValidator.checkNextStepOrder` | ✅ 本轮已补 |
| 时长校验（结束必须晚于开始） | `WorkOrderExecutionTimeValidator.checkDuration` | ✅ 本轮已补 |
| 历史数据回写 `status` / 时间倒挂巡检 | `20261127_actual_production_time_and_batch_guard.sql` | ✅ 本轮已补（脚本待执行） |
| 同批次唯一执行记录约束 | 迁移脚本 `uk_wpe_plan_batch` + 保存前工单行锁 | ✅ 本轮已补（脚本待执行） |
| 时间校验开关写入 `application.yml` | `erp.time-validation-enabled` | ✅ 本轮已补 |

---

## 四、修改计划

### P0：把「登记」和「完工」拆开（1~2 天）

**目标**：现场在工序开始时就能登记「执行中」，结束时间可以后补；工时、绩效只在完工时计算。

1. 后端契约
   - `POST /api/work-order-process-execution/batch`：明确 `startTime` 可单独提交，`endTime` 可空；返回体统一带后端推导的 `status`（`待执行/执行中/已完成`）。
   - 新增（或复用）`POST /{executionId}/complete` 作为唯一的「结束并锁定」动作：未传 `endTime` 取当前时间；校验 `endTime > startTime`；要求已登记参与人；成功后锁定、生成绩效、刷新批次状态。
   - `complete` 只允许一次；已锁定行返回明确错误码。
   - 保存时不允许把已锁定行的 `endTime` 改回空。
2. 前端契约
   - 行操作拆成「开始」「结束/完工」「编辑实绩」三个动作；开始只提交 `startTime`（可同时提交设备、房间、参与人）。
   - 列表按后端 `status` 显示「执行中」并用开始时间计时；未完工行不显示「已完工·已锁定」。
3. 兼容
   - 保留一次性提交 start+end 的用法（老页面、PDA 手工补录仍可用）。
   - `WorkOrderProcessExecution` 上已有的 `executionId / actualStartTime / actualEndTime / actualQty` 别名继续保留（`WorkOrderProcessExecution.java:32-130`）。

### P1：时间规则补齐（2~3 天）

统一在 `WorkOrderExecutionTimeValidator` 中实现，PC `batchSave` 与 PDA 手工时间共用：

| 规则 | 说明 | 现状 |
|------|------|------|
| R1 下界 | `start >= 工单.config_date`（非空时） | 已实现 |
| R2 上界 | `end <= 工单.config_complete_time`（非空时）；未填 end 时 `start` 不得晚于该时间 | 已实现 |
| R3 时长 | `end > start`，拦截 0/负工时 | 已实现 |
| R4 正序 | `start >= 前序各工序实际最晚结束`；前序存在「已开始未结束」时拒绝开始本工序 | 已实现 |
| R5 反序 | 本工序 `end` 不得晚于后序工序已登记的 `start`（修改历史工序时能拦住） | 已实现 |
| R6 批次 | 同工序多批次允许重叠；同一批次内部多条记录不得重叠/重复 | 定位器 + 唯一索引 `uk_wpe_plan_batch` |
| R7 锁定 | 已锁定行不参与校验、也不允许修改 | 已实现 |
| R8 手工时间 | 手工修改必须填写原因，落 `manual_time/manual_reason` | 已实现（PDA 手工时间校验） |

实施要点：
- 校验必须在**任何写库之前**执行，并在同一事务内（现在已满足：`:351-357`）。
- 错误信息聚合为一条 400，以换行分隔，前端逐行展示。
- 增加配置项到 `application.yml`：
  ```yaml
  erp:
    time-validation-enabled: true      # 总开关，历史数据修复时可临时关闭
  ```
- 开关关闭、手工改时间、批量导入等场景写操作日志，便于追溯。
- 校验只作用于「本次提交的行」；历史数据不因再次保存其他行而被连带拦截（现在已用 `excludeIds` 排除本次覆盖行）。

### P2：批次幂等与唯一性（2 天）

1. 定位优先级固化为：`executionId`（且 `planBatchId` 必须一致）→ `planBatchId` → `(planId, batchNo)` → `planId`（仅未拆批次）。当前 `ExecutionRecordMatcher` 已按此实现，补充单测覆盖「前端误传他批次 id」。
2. 保存语义：只替换「本次提交涉及的批次/工序」，其余行不动；有锁定 / PDA 提交 / 绩效的行禁止删除（`:387-411`、`:738-754`）。
3. 前端契约（写入文档）：
   - 批次行只回传**该批次自己的** `executionId`（来自 `batches[].executionId`），未登记时为空，**不得**回退使用工序级 `executionId`；
   - 未带 `id` 时必须带 `planId + planBatchId + batchNo`。
4. 数据库约束：
   ```sql
   -- 先确认无重复，再加唯一索引
   SELECT plan_batch_id, COUNT(*) FROM work_order_process_execution
   WHERE plan_batch_id IS NOT NULL AND is_deleted = 0
   GROUP BY plan_batch_id HAVING COUNT(*) > 1;

   ALTER TABLE work_order_process_execution
     ADD UNIQUE KEY uk_wpe_plan_batch (plan_batch_id),
     ADD KEY idx_wpe_work_order_step (work_order_id, step_order);
   ```
5. 并发：同一工单的保存加行级锁（`SELECT ... FOR UPDATE` 锁工单）或使用 `version` 乐观锁，避免两人同时编辑不同批次时互相覆盖。

### P3：历史数据治理与验证（1~2 天）

1. 巡检 SQL（只读，先出报告）：
   - 时间倒挂：后道工序 `start_time` 早于前道工序 `end_time`；
   - 超出工单配置窗口（配置时间/配置完成时间）；
   - `status` 与起止时间不一致（历史 `status=待执行` 但时间已填）；
   - 同一 `plan_batch_id` 多条执行记录；
   - 有 `end_time` 但无参与人 / 无绩效的完工行。
2. 修正策略：倒挂与越界数据由业务确认后修正（或标记备注），不自动改；`status` 可按时间重算回写。
3. 测试：
   - `WorkOrderExecutionTimeValidatorTest` 增补 R3/R5 用例；
   - `WorkOrderProcessExecutionBatchSaveTest` 增补「批次 A 保存 → 批次 B 保存 → A 数据仍在」的用例；
   - PDA 手工补录走同一校验器的用例。

---

## 五、验收标准

1. 工序开始时只需填实际开始时间即可保存，列表显示「执行中」，设备占用按开放区间计算；结束时再补结束时间并锁定。
2. 实际起止早于配置时间、晚于配置完成时间均被拒绝，错误信息指明工序与时间；配置完成时间为空时只校验下界。
3. 后道工序开始早于前道工序实际结束被拒绝；前道未完工时不允许开始后道。
4. 拆分两个批次后，分别登记/修改实绩互不影响；重新进入页面两个批次都能看到各自的起止与参与人。
5. 已完工锁定的行不能修改；已登记实际生产的批次不能删除或重新拆分。
6. 历史数据巡检报告可输出，且无新增倒挂/越界数据。

---

## 六、风险与上线顺序

| 风险 | 应对 |
|------|------|
| 历史数据大量不满足新规则，强校验上线后无法保存 | 先用 `erp.time-validation-enabled=false` 灰度；巡检+修正后再开 |
| 前端仍把结束时间设为必填 | 后端先兼容（允许只传 start），前端按 P0 改造后去掉必填 |
| 工时口径变化影响绩效 | 只在完工时计算/回填，执行中工时记 0，不改变已生成绩效 |
| 同一批次已存在多条执行记录时加唯一索引失败 | 先跑重复检查与合并脚本，再建索引 |
| PDA 与 PC 双入口规则不一致 | 统一调用 `WorkOrderExecutionTimeValidator`，PDA 手工时间入口已接入（`PdaProductionServiceImpl.java:841`） |

---

## 七、涉及文件清单

- `src/main/java/com/tonghui/erp/Controller/WorkOrderProcessExecutionController.java`（接口契约）
- `src/main/java/com/tonghui/erp/Service/impl/WorkOrderProcessExecutionServiceImpl.java`（保存/完工/工时）
- `src/main/java/com/tonghui/erp/Common/utils/WorkOrderExecutionTimeValidator.java`（时间规则）
- `src/main/java/com/tonghui/erp/Common/utils/ExecutionRecordMatcher.java`（批次定位）
- `src/main/java/com/tonghui/erp/Common/Constants/ProcessExecutionStatus.java`、`PlanBatchStatus.java`（状态推导）
- `src/main/java/com/tonghui/erp/Service/impl/PdaProductionServiceImpl.java`（PDA 入口）
- `src/main/java/com/tonghui/erp/Service/impl/WorkOrderScheduleServiceImpl.java`（排产/批次展示与删除保护）
- `src/main/resources/application.yml`（校验开关）
- `src/main/resources/Data/migration/`（唯一索引迁移）
- `docs/scheduling-module-frontend-integration.md`（前端契约同步）

---

## 八、实施记录（2026-11-27）

| 改动 | 文件 | 说明 |
|------|------|------|
| 保存接口只登记实际开始 | `WorkOrderProcessExecutionServiceImpl.batchSave` | 仅开始时间也可保存，状态后端推导为「执行中」，不再要求结束时间 |
| 完工接口支持指定结束时间 | `Controller.complete`、`ServiceImpl.complete(Long, LocalDateTime)` | `endTime` 可空（取当前时间）；显式传入必须晚于开始时间；完工走同一套时间校验 |
| 反向与时长校验 | `WorkOrderExecutionTimeValidator` | 新增 `end > start`、`end` 不得晚于后序工序已登记 `start`；只填结束时间的行也校验时间窗 |
| 并发保护 | `WorkOrderMapper.selectByIdForUpdate` + `batchSave` | 保存前锁定工单行，串行化同一工单的多批次保存 |
| 批次唯一索引与巡检 | `20261127_actual_production_time_and_batch_guard.sql`（含回滚） | 重复批次/倒挂/越界巡检、`status` 回写、`uk_wpe_plan_batch`、`(work_order_id, step_order)` 索引 |
| PDA 并发兜底 | `PdaProductionServiceImpl.ensureBatchExecution` | 并发创建同批次执行记录触发唯一索引冲突时，复用已有记录 |
| 测试库迁移执行 | `192.168.5.100/erp_db` | 备份表 `work_order_process_execution_bak_20261127`（149 行）；`status` 回写 0 行；已建 `uk_wpe_plan_batch`、`idx_wpe_work_order_step`；重复批次 0 组 |
| 校验开关 | `application.yml` | `erp.time-validation-enabled`，默认开启 |
| 测试 | `WorkOrderExecutionTimeValidatorTest`、`WorkOrderProcessExecutionBatchSaveTest`、`WorkOrderProcessExecutionCompleteTest` | 新增时长/反向/仅结束时间用例、批次 A/B 隔离用例、完工用例 |

迁移执行器：`scripts/db_migrate_20261127_actual_production.py`（默认只巡检，`--apply` 时先备份再迁移；内置线上库 8.148.27.237 拦截）。

验证结果：`mvnw compile` 通过；相关 46 个单元测试全部通过（时间校验 13、批次保存 7、完工 2、批次定位与排产 24）。测试库迁移已执行；巡检发现 85 条历史时间倒挂、5 条超出配置时间窗，属历史脏数据，需业务确认后修正，不影响新写入校验。

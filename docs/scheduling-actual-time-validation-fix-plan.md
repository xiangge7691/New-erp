# 排产 / 实际生产时间校验问题：原因分析与修改方案

> 对应页面：生产任务详情 → 排产 / 实际生产
> 对应代码：`ScheduleConflictService`、`WorkOrderScheduleServiceImpl`、`EquipmentUsageConflictValidator`、`WorkOrderExecutionTimeValidator`
> 涉及配置：`work_order.config_date`（配置日期）、`work_order.config_complete_time`（配置完成时间）

---

## 一、结论摘要

| # | 现象 | 直接原因 | 影响 |
|---|------|---------|------|
| 1 | 拆分批次没有和本工单其余工序做时间校验 | `split()` 只把「当前工序 + 新批次」传给冲突校验；`loadSavedItems()` 又把**整个本工单**从已保存计划中排除 | 拆分后的批次可能与同工单其他工序设备/房间/人员重叠而保存成功 |
| 2 | 上一条工序没填时间，就不做工序间校验 | `addPairConflict()` / `addPersonConflict()` 在任一计划起止为空时直接 `return`；工序顺序校验也只在紧前工序有时间时比较 | 前序未排时间时，后序可以排到任意时间，先后约束失效 |
| 3 | 实际生产是否有同类问题（需检查） | 有：设备冲突校验把**本工单全部执行记录**排除在数据库比对之外；工序先后校验只拦「已开始未结束」，前序从未开始时不拦 | 部分保存单批次时，同工单其他批次/工序可能重叠；前序未开工也能开后序 |
| 4 | 排产没有校验配置日期 ~ 配置完成时间 | `save()` / `split()` 只校验「开始 < 结束」和「首道工序不早于任务计划开始」，没有读 `config_date` / `config_complete_time` | 计划可以排到配置窗口之外（截图中 11-10 的批次 vs 11-19 配置日期） |
| 5 | 实际生产未填结束时间时冲突判断异常 | `effectiveEnd()` 把空结束当作 `9999-12-31`，冲突粗筛的 `maxEnd` 也被放大到 9999 | 一条「进行中」记录会与之后**任意时间**的设备使用记录冲突，导致无法登记现实 |

---

## 二、逐条原因分析

### 2.1 拆分批次漏校验（问题 1）

`WorkOrderScheduleServiceImpl.split()`（约 `:792`）只传当前工序：

```java
List<WorkOrderProcessPlan> one = List.of(plan);
List<ScheduleConflictDto> conflicts = conflictService.check(plan.getWorkOrderId(), one, entities);
```

而 `ScheduleConflictService.loadSavedItems()` 调用的
`selectOverlappingSavedPlans(minStart, maxEnd, excludeWorkOrderId)` 明确排除本工单：

```sql
AND p.work_order_id <> #{excludeWorkOrderId}
```

于是 `incoming` 只有新批次，`saved` 只有别的工单，**本工单其他工序（未拆分或已拆分）完全不参与比较**。
完整保存 `save()` 之所以看起来正常，是因为它把整单所有工序放进 `incoming` 两两比较；一旦走「拆分」「单工序保存」这类局部入口，就会漏掉同工单的兄弟行。

另外，`loadSavedItems()` 只查 `plan_start_time IS NOT NULL AND plan_end_time IS NOT NULL`，未排时间的已保存计划也不会进入比对。

### 2.2 前序未填时间就跳过校验（问题 2）

```java
// ScheduleConflictService.addPairConflict / addPersonConflict
if (a.start() == null || a.end() == null || b.start() == null || b.end() == null) {
    return;
}
```

工序顺序校验也只比较「计划开始」：

```java
if (prevStart != null && curStart != null && curStart.isBefore(prevStart)) { ... }
```

所以只要紧前工序没填计划时间，既不会比较设备/房间/人员重叠，也不会校验先后。缺时间在本系统里代表「未排产」，属于正常中间态，不应让整条链路的校验失效。

### 2.3 实际生产的同类问题（问题 3）

**（a）同工单其他批次不参与设备冲突**

`EquipmentUsageConflictValidator.checkDatabaseConflicts()`：

```java
List<Long> excludeIds = executionMapper.selectIdsByWorkOrderId(workOrderId);
```

它排除了整个工单的执行记录，假设「本次提交会覆盖整单」。但「实际生产」是按工序/批次局部保存的，同工单其他批次的记录既不在 `activeRows`，也不在数据库比对里。

**（b）前序工序未开工也能开后序**

`WorkOrderExecutionTimeValidator.checkStepOrder()` 只把「有开始、无结束」视为未完工：

```java
if (execution.getStartTime() != null && execution.getEndTime() == null) {
    state.open = true;
}
```

前序工序只有执行记录但从未填开始时间时，`open=false`、`latestEnd=null`，后序可以直接开始；前序工序没有任何执行记录时更是完全不知道它存在。

### 2.4 排产没有配置时间窗校验（问题 4）

`save()` 只做了：

- `validateTimes()`：开始/结束必须同时填写且结束晚于开始；
- 首道工序开始不得早于 `order.planStartTime`（注意这是任务计划开始，不是配置日期）。

`split()` 只做 `validateTimes()` 和批次数量的校验。两处都没有读取 `config_date` / `config_complete_time`。

### 2.5 实际生产开放区间冲突（问题 5）

`EquipmentUsageConflictValidator`：

```java
private static final LocalDateTime OPEN_END = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
```

- `checkDatabaseConflicts()` 用 `effectiveEnd` 计算 `maxEnd`，一条进行中的记录会把查询上界放大到 9999；
- 内存重叠判断中 `effectiveEnd(row)` 同样返回 9999，于是任何「开始时间在本次开始之后」的记录都判为冲突。

截图中的场景（实际开始 11-19 16:00、未填结束；另一工单 11-24 16:37~16:38 使用同一设备）就是这样被判冲突的。结束时间未知时，系统无法证明未来一定占用，「无限占用」的假设会让现场无法登记开工。

---

## 三、修改方案

### 3.1 统一时间语义（先定规则）

| 场景 | 时间完整性 | 校验规则 |
|------|-----------|---------|
| 排产-未排时间 | 起止都空 | 允许保存（草稿/正式均可），表示「待排产」；不参与资源冲突，但**阻断后道的先后校验要单独处理** |
| 排产-已排时间 | 起止都有 | 完整区间校验：配置窗口、资源重叠、人员重叠、批次重叠、先后 |
| 排产-半填 | 只有一端 | 直接拒绝（现有 `validateTimes` 已做） |
| 实际生产-执行中 | 只有开始 | 仍按「持续占用」检测冲突，但冲突只作为提醒返回，不阻断保存 |
| 实际生产-完工 | 起止都有 | 完整区间重叠 + 配置窗口 + 先后 |
| 实际生产-未开始 | 起止都空 | 不参与校验 |

> 「开放区间只做点校验」是本方案对问题 5 的处理：结束未知时只保证开工那一刻设备没被占用，整段区间在完工（`complete`）时再校验一次。若业务希望「进行中按到当前时间」处理，可做成配置 `erp.actual-conflict-open-end-mode = POINT | NOW`，默认 `POINT`。

### 3.2 排产：补齐拆分与局部保存的比对范围（问题 1）

1. `ScheduleConflictService.check()` 增加「同工单未提交行」：
   - 新增 `loadSiblingItems(workOrderId, submittedPlanIds, submittedBatchIds, replacedBatchIds)`：
     - 查本工单全部计划及其批次；
     - 排除本次提交的 plan/batch；
     - 拆分场景额外排除该工序的旧批次（它们会被物理删除）。
   - `incoming` 与 `siblingItems` 做设备/房间/人员两两比较（只在 incoming 侧产生冲突，避免历史冲突反复报）。
2. `split()` 改为调用 `checkWithSiblings(workOrderId, one, entities, replacedBatchIds)`，或给 `check()` 增加 `replacedBatchIds` 参数。
3. `loadSavedItems()` 保持只查「其他工单 + 已排时间」的计划，作为跨工单比对；本工单兄弟行走新逻辑，避免把整单排除。

### 3.3 排产：前序未排时间的先后校验（问题 2）

1. 在 `check()` 的工序顺序段：
   - 按 `stepOrder` 排序，取**紧前工序**；
   - 当前工序有时间、紧前工序无时间 → 新增冲突 `type="工序顺序"`，message：`紧前工序【X】未排产时间，无法校验先后，请先完成前序排产`；
   - 当前工序无时间 → 跳过（未排产）；
   - 紧前有时间、当前有时间 → 保留现有「当前开始不得早于紧前开始」，并建议增加「当前开始不得早于紧前结束」的严格模式（可配置，避免与现有「允许重叠」的业务约定冲突）。
2. 资源/人员冲突：缺时间的行仍不能做区间比较，由上面的显式冲突兜底；不再静默跳过。
3. 草稿 vs 正式：本轮**保持现状**（有冲突不落库），草稿是否放行待产品确认；代码注释与行为不一致的问题先记录，不在本次改动范围。

### 3.4 排产：配置时间窗校验（问题 4）

1. `WorkOrderScheduleServiceImpl` 新增：

```java
private List<ScheduleConflictDto> validateConfigWindow(WorkOrder order,
        WorkOrderProcessPlan plan, WorkOrderProcessPlanBatch batch,
        LocalDateTime start, LocalDateTime end, String label)
```

规则：
- 起止都空 → 跳过；
- `config_date != null && start < config_date` → 冲突「计划开始早于配置日期」；
- `config_complete_time != null && end > config_complete_time` → 冲突「计划结束晚于配置完成时间」；
- 拆分工序时对每个批次校验；未拆分时对工序校验。

2. 接入点：
   - `save()`：逐工序 + 逐批次；`formal` 与草稿都返回冲突（配置窗口属于硬约束）；
   - `split()`：逐批次；同时校验批次是否落在所属工序计划时间内（可选严格模式）。
3. 冲突类型新增 `"配置时间窗"`，前端在冲突面板按行展示，字段带 `planId/batchId/stepOrder`。
4. 历史数据：截图中 11-10/11-14 的批次早于 11-19 配置日期，属于存量不合规，需要业务先修正；上线初期可只警告不阻断（配置项 `erp.schedule-window-blocking=false`）。

### 3.5 实际生产：同工单其他批次纳入冲突（问题 3a）

1. `EquipmentUsageConflictValidator.checkDatabaseConflicts()`：
   - 不再用 `selectIdsByWorkOrderId(workOrderId)` 排除整单；
   - 改为排除「本次提交的执行记录 ID」+「本次会被替换/删除的批次旧记录」；
   - 新增查询：本工单下未被本次提交覆盖的执行记录，作为「同工单其他记录」参与比较。
2. 同批内（`checkInBatchConflicts`）与数据库比对使用同一套 `overlap` 规则，保证口径一致。
3. `batchSave` 已按批次定位并只替换本批次，排除集合可直接从 `toUpdate` 的 id 和提交的 `planBatchId` 推导。

### 3.6 实际生产：前序未开工不允许开后续（问题 3b）

`WorkOrderExecutionTimeValidator.checkStepOrder()` 扩展：

1. 前序工序存在执行记录但**没有任何开始时间** → 报「上一道工序【X】尚未登记实际开始时间，请先开工」；
2. 前序工序有多个批次时，要求**全部批次已完工**才允许后道开始（`open` 或未开始均视为未完工）；若业务允许批次并行，可配置为「至少一个批次完工」；
3. 前序工序没有执行记录时：
   - 若能取到工单的排产工序（`work_order_process_plan`），按计划顺序判断该工序是否存在；存在但无执行记录 → 视为未开工，阻断；
   - 取不到计划时退化为现有行为（不阻断），避免历史工单无法登记。
4. 同工序多批次之间仍允许重叠，不受此规则影响。

### 3.7 实际生产：进行中冲突降级为提醒（问题 5）

按确认的方案 C 实施：**保持「未填结束时间 = 持续占用」的判断口径不变**，只把这类冲突从「阻断保存」降级为「提醒」。

1. `EquipmentUsageConflictValidator`：
   - 基础错误（时间非法、设备缺失/停用）仍然抛异常；
   - 同批内冲突、跨工单冲突、维保冲突：只要**任一方结束时间为空**，写入 `warnings` 而不是 `errors`；
   - 双方结束时间都完整的区间冲突仍然阻断（防止真实数据覆盖）。
2. 返回值：`validate(...)` 返回 `List<String>` 提醒；`batchSave` 透传，接口响应的 `warnings` 字段返回给前端。
3. 前端拿到 `warnings` 后做提示（Toast/行内提醒），保存仍然成功；现场可继续登记，不需要先编造结束时间。
4. 完工（`complete`）时结束时间已完整，完整区间校验照常执行。

### 3.8 前端与接口

- 冲突类型新增：`配置时间窗`、`前序未排时间`；冲突面板按 `planId/batchId` 定位到行。
- 拆分批次保存后，用返回的冲突列表刷新**整单**冲突数（现在只看当前工序）。
- 实际生产「登记实际记录」弹窗：未填结束时间时提示「当前仅校验开始时刻，完工时校验完整时段」；完工时如有冲突，提示具体占用记录。
- 排产正式保存/拆分：冲突非空时不落库（保持现状）；草稿保存是否放行待产品确认。

### 3.9 数据巡检与兼容

| 检查项 | SQL 口径 |
|--------|---------|
| 排产超出配置窗口 | `plan_start_time < config_date` 或 `plan_end_time > config_complete_time` |
| 未排时间的工序/批次 | `plan_start_time IS NULL OR plan_end_time IS NULL` |
| 实际生产开放过久 | `start_time IS NOT NULL AND end_time IS NULL AND start_time < NOW() - INTERVAL 7 DAY` |
| 同工单批次设备重叠 | 同工单、同设备、时间区间相交且批次不同 |

上线策略：先只读巡检 → 业务确认 → 打开硬校验；存量不合规数据不自动修改。

---

## 四、涉及文件

- `src/main/java/com/tonghui/erp/Service/impl/ScheduleConflictService.java`（同工单兄弟行、前序未排时间、配置窗口）
- `src/main/java/com/tonghui/erp/Service/impl/WorkOrderScheduleServiceImpl.java`（`save` / `split` 接入、读取工单配置时间）
- `src/main/java/com/tonghui/erp/Common/utils/EquipmentUsageConflictValidator.java`（开放区间点校验、同工单未提交记录）
- `src/main/java/com/tonghui/erp/Common/utils/WorkOrderExecutionTimeValidator.java`（前序未开工/批次未完工）
- `src/main/java/com/tonghui/erp/Common/Dto/Scheduling/ScheduleConflictDto.java`（新增冲突类型，字段已够用）
- `src/main/resources/application.yml`（新配置项）
- 测试：`ScheduleConflictServiceTest`、新增拆分冲突测试、`EquipmentUsageConflictValidator` 测试、`WorkOrderExecutionTimeValidatorTest`

---

## 五、验收标准

1. 拆分批次保存时，若与同工单其他工序（未拆分或已拆分）设备/房间/人员重叠，返回冲突且不落库。
2. 紧前工序未排时间时，后道工序有时间的保存被拦，提示「紧前工序未排产时间」。
3. 排产计划/批次早于配置日期或晚于配置完成时间时被拦（或按开关只警告）。
4. 实际生产只填开始时间时，不再因未来时段的设备记录被拦；开始时刻被占用仍会被拦。
5. 实际生产同工单其他批次与本次批次设备时段重叠时被拦。
6. 前序工序未开工（或批次未全部完工）时，后道不允许登记实际开始。
7. 完工时完整区间与既有占用重叠仍会被拦。
8. 现有 46 个相关单元测试全部通过，新增用例覆盖上述场景。

---

## 六、风险

| 风险 | 应对 |
|------|------|
| 同工单兄弟行参与校验后，存量冲突会让保存频繁失败 | 只对「本次提交行」报冲突，历史行不反向报；上线前先巡检 |
| 进行中冲突降级为提醒后，可能放过真实占用 | 接口返回 `warnings` 由前端强提示；完工时完整区间校验兜底；实际生产列表可标红 |
| 配置时间窗校验阻断现有排产 | 先只读巡检，必要时临时关闭校验或先修正存量数据 |
| 前序工序无执行记录时阻断，历史工单可能无法补录 | 取不到计划时退化为不阻断；提供开关 |
| 拆分/保存入口多，容易漏接入 | 统一收敛到 `ScheduleConflictService.check()`，各入口只传参数 |

---

## 七、实施记录（2026-11-27）

| 改动 | 位置 | 说明 |
|------|------|------|
| 拆分/局部保存补齐同工单比对 | `ScheduleConflictService.loadSiblingData` | 加载本工单未提交的工序/批次，参与设备、房间、人员冲突；拆分时排除被替换的旧批次 |
| 紧前工序未排时间显式提示 | `ScheduleConflictService.check` 工序顺序段 | 当前工序有时间、紧前无时间 → 「紧前工序未排产时间，无法校验先后」；先后规则保持「后道开始不早于前道开始」 |
| 配置时间窗校验 | `ScheduleConflictService.checkConfigWindow`、`WorkOrderScheduleServiceImpl.save/split` | 开始早于 `config_date`、结束晚于 `config_complete_time` → 冲突，阻止落库 |
| 实际生产同工单其他批次 | `EquipmentUsageConflictValidator.checkDatabaseConflicts` | 只排除本次更新/替换的行，不再排除整工单，局部保存也能发现同工单其他批次占用 |
| 进行中冲突降级 | `EquipmentUsageConflictValidator`、`ApiResponse.warnings`、`batchSave` | 任一方结束时间为空 → 返回提醒不阻断；双方完整仍阻断；接口响应新增 `warnings` |
| 前序工序未开工拦截 | `WorkOrderExecutionTimeValidator.StepState` | 前序有记录但无开始时间 → 不允许后道开始；批次未全部完工 → 提示先完成全部批次 |
| 测试 | `ScheduleConflictServiceTest`、新增 `EquipmentUsageConflictValidatorTest`、`WorkOrderExecutionTimeValidatorTest` | 覆盖拆分兄弟行、紧前未排时间、配置窗口、进行中提醒、前序未开工 |

验证结果：`mvnw compile` 通过；相关 46 个单元测试全部通过（排产冲突 8、设备冲突 2、时间校验 14、批次保存 7、完工 2、批次定位与排产 13）。草稿保存语义本轮未改（仍有冲突不落库），待产品确认。

# 产品信息汇总：节点链路接口调整方案

> 目标链路：
> 计划 → 采购 → 验收入库（物料入库，可能无采购）→ 生产任务 → 生产领料 → 生产过程 → 检验 → 审核放行 → 成品入库 → 成品出库
>
> 涉及接口：
> - `GET /api/product-summary/batches/{workOrderId}`（批次详情）
> - `GET /api/product-summary/plans/{planId}`（未下达工单的计划详情）
> - `GET /api/product-summary/batches` / `GET /api/product-summary/cards`（列表/卡片，`stageStates` 同步）

## 一、现状与差距

当前后端 `stages()` 固定返回 8 个节点，节点顺序与口径如下：

```
plan(计划) → material(采购/验收 或 领料，二选一) → workOrder(工单) → production(生产)
→ inbound(入库) → outbound(出库) → inspection(检验) → release(放行)
```

| 目标节点 | 当前实现 | 差距 |
|---------|---------|------|
| 计划 | `plan` | 保留，作为链路根节点 |
| 采购 | 并入 `material`，只有“没有领料单”时才显示采购 | 采购与领料必须同时展示，采购节点不能因领料存在而消失 |
| 验收入库（物料入库） | 并入 `material`，仅展示验收单 | 缺少“物料入库单”数据；且需要与“成品入库”区分 |
| 生产任务 | `workOrder`，标签“工单” | 标签需改为“生产任务” |
| 生产领料 | 与采购互斥地占用 `material` | 需独立节点，并带出领料对应的领料出库单 |
| 生产过程 | `production` | 保留，标签改为“生产过程” |
| 检验 | `inspection` | 保留，位置需移到成品入库之前 |
| 成品入库 | `inbound` | 数据来自 `stock_in.work_order_id`，基本可用，但未过滤 `in_type=成品入库` |
| 审核放行 | `release` | 保留，标签改为“审核放行” |
| 成品出库 | `outbound` | **口径错误**：`BatchQueryServiceImpl` 按 `plan_id/plan_number` 查 `stock_out`，而写入计划ID的是“生产领料出库”；成品出库单来自销售台账（`related_order=销售单号`），不会命中，导致该节点实际展示的是领料出库 |

### 必须顺带修正的两个数据问题

1. **出库口径**：`ProductionPickServiceImpl` 创建领料出库时写入了 `plan_id/plan_number`，而 `SalesOrderController` 创建成品出库时没有写计划；当前 `outboundQty`、`outboundDate` 统计的是领料出库量，不是成品出库量。
2. **出库类型枚举不一致**：`SalesOrderController` 写入 `out_type="成品出库"`，其他出库业务与工单下拉使用 `"销售出库"`。成品出库查询必须同时兼容两种值，长期应统一（见第六节）。

## 二、目标节点清单（后端固定返回，不因无数据而省略）

| 顺序 | key | label | 数据来源 | 主要单据 |
|------|-----|-------|---------|---------|
| 0 | `plan` | 计划 | `production_plan` | 生产计划 |
| 1 | `purchase` | 采购 | `purchase_orders`（`production_plan_id/code` 关联） | 采购订单 |
| 2 | `acceptance` | 验收入库 | `acceptance_order` + 物料入库 `stock_in` | 货物验收单、物料入库单 |
| 3 | `workOrder` | 生产任务 | `work_order` | 工单 |
| 4 | `pick` | 生产领料 | `production_pick` + `stock_out`（领料出库） | 领料单、领料出库单 |
| 5 | `production` | 生产过程 | `work_order_process_execution` | 工序记录 |
| 6 | `inspection` | 检验 | `inspection_request` / `sampling_record` / `inspection_record` | 请检单、取样记录、检验记录 |
| 7 | `finishedInbound` | 成品入库 | `stock_in`（`work_order_id` + `in_type=成品入库`） | 成品入库单 |
| 8 | `release` | 审核放行 | `release_review` | 放行记录 |
| 9 | `finishedOutbound` | 成品出库 | `stock_out`（销售/成品出库） | 成品出库单 |

要求：**这 10 个节点每次都以固定顺序返回**。前端不再根据“有没有采购/有没有领料”自行拼节点，避免节点数量、顺序随数据变化。

## 三、接口字段调整

### 3.1 批次详情 `stages`

`ProductSummaryDtos.StageItem` 增加两个字段（`StageState` 同步增加 `skipReason`，便于卡片悬浮提示）：

```java
/** 是否不适用（如无采购、物料直接入库） */
private Boolean skipped;

/** 不适用说明，如“无采购，物料直接入库/领料” */
private String skipReason;
```

`state` 取值扩展为：`done` / `active` / `pending` / `error` / `skipped`。

调整后的响应示例：

```json
"stages": [
  {
    "key": "plan", "label": "计划", "state": "done",
    "main": "PL-20260901-001", "meta": "2026-09-01 · 已下达",
    "documents": [{ "type": "生产计划", "code": "PL-20260901-001", "files": [] }]
  },
  {
    "key": "purchase", "label": "采购", "state": "done",
    "main": "2 张采购单", "meta": "已入库 · ¥12,300.00",
    "documents": [{ "type": "采购订单", "code": "DD-20260901-001", "lines": [], "files": [] }]
  },
  {
    "key": "acceptance", "label": "验收入库", "state": "done",
    "main": "2 张验收单 / 2 张入库单", "meta": "2026-09-03",
    "documents": [
      { "type": "货物验收", "code": "YS-20260903-001", "files": [] },
      { "type": "物料入库单", "code": "RK-20260903-001", "lines": [], "files": [] }
    ]
  },
  {
    "key": "workOrder", "label": "生产任务", "state": "done",
    "main": "WO-20260905-001", "meta": "批号 260905 · 已生产",
    "documents": [{ "type": "工单", "code": "WO-20260905-001", "files": [] }]
  },
  {
    "key": "pick", "label": "生产领料", "state": "done",
    "main": "1 张领料单", "meta": "已发料 · 张三",
    "documents": [
      { "type": "领料单", "code": "LL-20260905-001", "lines": [], "files": [] },
      { "type": "领料出库单", "code": "CK-20260905-001", "files": [] }
    ]
  },
  {
    "key": "production", "label": "生产过程", "state": "done",
    "main": "6 道工序", "meta": "收率 98.50%",
    "documents": [{ "type": "工序记录", "code": "配液", "files": [] }]
  },
  {
    "key": "inspection", "label": "检验", "state": "done",
    "main": "合格", "meta": "1 条成品检验记录",
    "documents": [{ "type": "检验记录", "itemCategory": "成品", "code": "JY-20260907-001", "files": [] }]
  },
  {
    "key": "finishedInbound", "label": "成品入库", "state": "done",
    "main": "1 张入库单", "meta": "2026-09-08",
    "documents": [{ "type": "成品入库单", "code": "RK-20260908-001", "lines": [], "files": [] }]
  },
  {
    "key": "release", "label": "审核放行", "state": "done",
    "main": "FX-20260909-001", "meta": "同意放行",
    "documents": [{ "type": "放行记录", "code": "FX-20260909-001", "files": [] }]
  },
  {
    "key": "finishedOutbound", "label": "成品出库", "state": "pending",
    "main": "未出库", "meta": "",
    "documents": []
  }
]
```

无采购、物料直接入库时：

```json
{
  "key": "purchase", "label": "采购", "state": "skipped",
  "main": "无采购", "meta": "",
  "skipReason": "无采购，物料直接入库",
  "documents": []
}
```

### 3.2 列表/卡片 `stageStates`

`BatchSummary.stageStates` 与详情共用同一套节点定义（key、label、顺序一致），仅在 `state` 上表达进度。不要再返回旧的 `material` 节点。

```json
"stageStates": [
  { "key": "plan", "label": "计划", "state": "done" },
  { "key": "purchase", "label": "采购", "state": "done" },
  { "key": "acceptance", "label": "验收入库", "state": "done" },
  { "key": "workOrder", "label": "生产任务", "state": "done" },
  { "key": "pick", "label": "生产领料", "state": "done" },
  { "key": "production", "label": "生产过程", "state": "done" },
  { "key": "inspection", "label": "检验", "state": "done" },
  { "key": "finishedInbound", "label": "成品入库", "state": "done" },
  { "key": "release", "label": "审核放行", "state": "done" },
  { "key": "finishedOutbound", "label": "成品出库", "state": "pending" }
]
```

### 3.3 文件槽位与节点绑定

`FileGroup`、`SlotState`、`MissingItem` 增加 `nodeKey`，由后端把文件配置里 `file_requirement_config.stage`（中文）映射到节点 key：

| 文件配置 stage | nodeKey |
|---------------|---------|
| 品种资料 | `plan` |
| 计划 | `plan` |
| 采购 | `purchase` |
| 验收 | `acceptance` |
| 工单 | `workOrder` |
| 领料 | `pick` |
| 工序 | `production` |
| 检验 | `inspection` |
| 入库 | `finishedInbound` |
| 放行 | `release` |
| 出库 | `finishedOutbound` |

前端不再用 `label.includes("采购订单")` 这类字符串匹配挂附件。

### 3.4 `BatchSummary` 统计字段口径

| 字段 | 现状 | 调整后 |
|------|------|--------|
| `inboundQty` | 工单关联的全部入库明细 | 仅 `in_type=成品入库` 的入库明细 |
| `inboundDate` | 第一张工单入库单日期 | 仅成品入库，取最早/或已入库首张 |
| `outboundQty` | 按计划查到的主要是领料出库量 | 仅成品出库明细，按批号/制剂匹配 |
| `outboundDate` | 领料出库日期 | 仅成品出库日期 |
| `materialSource` | `REQUISITION` / `PURCHASE` | 已扩展 `DIRECT`（无采购、无领料但存在物料入库）；节点状态成为主口径，该字段保留兼容 |

## 四、后端代码改动点

### 4.1 `BatchContext`（`Common/Dto/QualityPages/BatchContext.java`）

新增：

```java
/** 物料入库单（in_type != 成品入库），按验收单/计划关联 */
private List<StockIn> materialStockIns = new ArrayList<>();
/** 物料入库明细：入库单ID -> 明细 */
private Map<Long, List<StockInDetail>> materialStockInDetails;
/** 成品入库单（work_order_id + in_type=成品入库） */
private List<StockIn> finishedStockIns = new ArrayList<>();
/** 成品入库明细 */
private Map<Long, List<StockInDetail>> finishedStockInDetails;
/** 生产领料出库单（pick.stock_out_id 或 plan_id + out_type=生产领料出库） */
private List<StockOut> materialStockOuts = new ArrayList<>();
/** 成品出库单（销售/成品出库） */
private List<StockOut> finishedStockOuts = new ArrayList<>();
/** 成品出库明细 */
private Map<Long, List<StockOutDetail>> finishedStockOutDetails;
/** 成品出库对应的销售台账（用于客户、金额展示） */
private List<SalesOrder> salesOrders = new ArrayList<>();
```

原 `stockIns` / `stockOuts` 建议保留为 `finishedStockIns` / `finishedStockOuts` 的兼容别名，或直接改所有引用，避免再出现“混装”。

### 4.2 `BatchQueryServiceImpl.load()` 查询口径

**物料入库**（验收入库节点）：

```java
// 1) 验收单号 -> stock_in.related_order
//    in_type != '成品入库'，排除销售出库等无关单据
// 2) 兜底：plan_number = 计划编号 AND in_type IN ('采购入库','直接入库','领料入库')
```

可直接复用 `ProductionPlanProgressServiceImpl.loadStockInsForDetail()` 的思路（`related_order` + `plan_number`）。

> 数据前提：无采购的“直接入库”也必须填写 `stock_in.plan_number`（或验收单 `plan_code`），否则无法归入具体批次，验收入库节点会显示 `pending`。建议在直接入库页面把“关联生产计划编号”设为必填或强提示。

**成品入库**：

```java
stock_in WHERE work_order_id = ? AND in_type = '成品入库'
```

**生产领料出库**：

```java
// 优先 pick.stock_out_id 直接取
// 兜底：stock_out WHERE plan_id = ? AND out_type = '生产领料出库'
//       且 related_order IN (领料单号)
```

**成品出库**（关键新增）：

```java
// 1) 销售台账：sales_order WHERE preparation_code = ? AND batch_number = ?
// 2) 出库单：stock_out WHERE related_order IN (销售台账单号)
// 3) 手工出库兜底：
//    stock_out_detail WHERE batch_number = ? AND item_code = 制剂编码
//    -> stock_out WHERE out_type IN ('销售出库','成品出库')
// 4) 若出库单 related_order = 工单号，也纳入
```

> 长期建议给 `stock_out` 增加 `work_order_id` / `batch_number` 字段，销售出库创建时回填，彻底摆脱按批号文本匹配。

### 4.3 `ProductSummaryServiceImpl` 节点组装

需要改的方法：

| 方法 | 改动 |
|------|------|
| `stages(BatchContext)`（约 641 行） | 删除 `material` 合并节点，替换为 `purchase`、`acceptance`、`pick` 三个节点；调整节点顺序为第二节固定顺序；标签改名 |
| `stageStates(BatchContext)`（约 479 行） | 同样换成 10 个固定节点；新增 `skipped` 判定 |
| `planOnlyStages(ProductionPlan)`（约 340 行） | 当前只返回计划节点；改为返回固定 10 节点，采购/验收入库按计划关联数据填充，其余 `pending` |
| `planOnlyStageStates()`（约 466 行） | 换成固定 10 节点，采购/验收按计划数据判定 |
| `stageOrder(String)`（约 541 行） | 健康度排序改为：品种资料→计划→采购→验收→工单→领料→工序→检验→入库→放行→出库 |
| `inboundQty` / `outboundQty`（约 514/523 行） | 分别只统计成品入库、成品出库 |
| `materialSource`（约 1309 行） | 按采购/领料/直接入库证据重新计算，保留字段兼容 |

新增单据转换方法：

- `materialStockInDoc(StockIn, BatchContext)`：类型“物料入库单”，明细取 `materialStockInDetails`
- `pickStockOutDoc(StockOut)`：类型“领料出库单”，文件 `STOCK_OUT_PRODUCTION`
- `finishedStockOutDoc(StockOut, BatchContext)`：类型“成品出库单”，文件 `STOCK_OUT_SALES`，带客户/销售单号
- `stage` 增加 `skipped` / `skipReason` 参数

### 4.4 节点状态判定规则

| 节点 | done | active | error | skipped |
|------|------|--------|-------|---------|
| `purchase` | 采购单全部已入库/已归档 | 存在采购单但未全部入库 | — | 无采购单且存在验收/物料入库证据 |
| `acceptance` | 存在物料入库单（`in_type != 成品入库`） | 存在验收单但未入库 | — | — |
| `workOrder` | 配置完成/已有成品量 | 已配置但未完成 | — | 无生产计划时可保留 `plan` 节点 skipped |
| `pick` | 存在领料单且全部“已发料” | 存在领料单但未全部发料 | — | — |
| `production` | 工序全部有结束时间 | 存在已开始未结束工序 | — | — |
| `inspection` | 最新成品检验“合格” | 有请检/取样/未出结论 | 最新结论“不合格” | — |
| `finishedInbound` | 存在成品入库单 | 检验合格但未入库 | — | — |
| `release` | 存在放行记录且非拒绝 | 已入库待放行 | 结论“拒绝放行” | — |
| `finishedOutbound` | 存在成品出库单 | 已放行待出库 | — | — |

> `skipped` 只用于“业务上确实不适用”的节点，例如无采购、直接入库；不要用 `pending` 表达“不适用”，否则前端无法区分“还没做”和“不需要做”。

## 五、兼容策略

### 方案 A（推荐，前后端同版发布）

直接替换 `stages` 与 `stageStates` 的内容，节点 key 固定为第二节的 10 个。前端按 `key` 渲染节点主体，未识别的 key 走通用“摘要 + 单据列表”渲染。同步更新：

- `docs/product-summary-api-and-export-plan.md`
- `docs/product-summary-frontend-integration.md`
- `_analysis/prototypes/product-summary.html`（若还需维护原型）

### 方案 B（需要灰度时）

详情新增 `nodes` 字段返回新链路，`stages` 保持旧结构并标注 deprecated；列表新增 `nodeStates` 保留 `stageStates`。前端切换完成后删除旧字段。缺点是两套逻辑要并行维护一个版本。

## 六、出库类型枚举统一（建议同批处理）

当前写入值：

- `ProductionPickServiceImpl`：`生产领料出库`
- `SalesOrderController`：`成品出库`
- 工单下拉/出库页面文档：`销售出库`

建议：

1. 新建 `StockOutType` 常量类，固定 `PRODUCTION_PICK("生产领料出库")`、`SALES("销售出库")`、`LOSS("报损出库")`。
2. 销售台账创建出库单改为 `销售出库`。
3. 数据迁移：`UPDATE stock_out SET out_type='销售出库' WHERE out_type='成品出库';`
4. 过渡期查询仍兼容 `IN ('销售出库','成品出库')`。

## 七、性能注意

`ProductSummaryServiceImpl.loadData()` 目前对筛选范围内的**每一张工单**调用 `batchQueryService.load(wo)`，本身已是 N+1；本次新增物料入库、成品出库查询后每批次会再多 3~4 次查询，列表页（尤其 `pageSize=20` 且先全量加载再内存分页）会明显变慢。

后续建议把 `BatchQueryService.loadAll()` 改为批量预加载（本次未实现）：

1. 收集本页/本范围的 `workOrderId`、`workOrderCode`、`planId`、`planNumber`、`batchNumber`、`preparationCode`；
2. 物料入库按 `acceptance_code IN (...)` 或 `plan_number IN (...)` 一次查；
3. 成品入库按 `work_order_id IN (...)` + `in_type=成品入库` 一次查；
4. 成品出库按销售台账 `(preparation_code, batch_number) IN (...)` 和明细批号一次查；
5. 在内存中按 key 分组回填 `BatchContext`。

## 八、验收用例

1. **完整链路**：有采购 → 验收 → 物料入库 → 工单 → 领料 → 工序 → 成品检验 → 放行 → 成品入库 → 成品出库，10 个节点全部 `done`。
2. **无采购直接入库**：采购节点 `skipped` 且带 `skipReason`，验收入库、生产领料等正常展示。
3. **未下达工单的计划**：`/plans/{planId}` 返回同样 10 个节点；采购、验收入库按计划数据展示，生产任务之后 `pending`。
4. **拒绝放行**：放行节点 `error`，成品出库 `pending`。
5. **有领料出库但无成品出库**：成品出库必须 `pending`，且 `outboundQty/outboundDate` 为空，不能把领料出库算进去。
6. **列表与详情一致性**：同一批次的 `stageStates` 与 `stages` 的 key、顺序、状态一致。
7. **文件挂载**：`FileGroup.nodeKey` 能把采购、验收、工序、检验、放行附件挂到正确节点。

## 九、涉及文件

- `src/main/java/com/tonghui/erp/Common/Dto/QualityPages/ProductSummaryDtos.java`
- `src/main/java/com/tonghui/erp/Common/Dto/QualityPages/BatchContext.java`
- `src/main/java/com/tonghui/erp/Service/BatchQueryService.java`
- `src/main/java/com/tonghui/erp/Service/impl/BatchQueryServiceImpl.java`
- `src/main/java/com/tonghui/erp/Service/impl/ProductSummaryServiceImpl.java`
- `src/main/java/com/tonghui/erp/Controller/ProductSummaryController.java`（接口注释同步）
- `docs/product-summary-api-and-export-plan.md`
- `docs/product-summary-frontend-integration.md`
- `src/test/java/com/tonghui/erp/Service/QualityPagesSmokeTest.java`（补充节点 key/顺序断言）
- 可选：`Controller/SalesOrderController.java`、`Common/Constants/StockOutType.java`（出库类型统一）

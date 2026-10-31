# 首页看板 V2 前端实现参考与修改指导

> 适用分支：`v1.0`
> 涉及接口：`/api/dashboard/metrics`、`/api/dashboard/metrics/details/{card}`、`/api/dashboard/details/tree`、`/api/dashboard/charts`
> 设计原则：**只做加法**。不删字段、不改名、不改变已发布字段的语义（仅「实际成本」的构成口径按新需求扩展，见 §2.4）。

---

## 1. 一句话说明本次改了什么

| 需求 | 落地方式 |
|------|---------|
| 总实际成本 = 原辅包 + 水电气 + 其他 | `actualCost` 构成扩展，新增 `otherAmount` 字段 |
| 总预计成本 = 制剂预计成本 × 生产数量 | 保持原口径不变，预计成本价改取**工单快照** |
| 总毛利 / 毛利率 | 以「**成本偏差 / 偏差率**」命名提供（`varianceToEstimated` / `varianceRate`，本次未改名） |
| 单位实际成本 / 单位预计成本 / 单位偏差 / 单位偏差率 | 明细行新增 4 个字段 |
| 批次生产时间（采购下达 → 成品入库） | 明细行新增 `purchaseDate` / `inboundDate` / `productionDays` |
| 批次盈亏排行 | 新增图表节点 `batchCostVarianceRank` |
| 成本构成占比（可分品种） | `costStructure` 增加原料/辅料/包材/其他；新增 `costStructureByPreparation` |
| 生产平衡按「批」 | 新增 `production.balance` 节点，`planVsDelivery` 原样保留 |
| 预估成本、销售单价用快照 | `work_order.estimated_cost_price_snapshot`，工单创建时冻结 |

---

## 2. 逐接口字段变更

### 2.1 `/api/dashboard/metrics`（六张卡片）

**仅新增，无改名。**

```jsonc
{
  "cost": {
    "estimatedCost": {
      "estimatedCost": 587916.00,
      "batchCount": 10,
      "batchQty": 39200.0,
      "estimatedCostPrice": 15.00,
      "ratioOfOutput": 112.5,
      "momRate": 12.3,
      "missingPriceBatchCount": 0,
      "missingPriceBatchQty": 0.0
    },
    "actualCost": {
      "actualCost": 4011.38,
      "materialAmount": 11.38,
      "waterAmount": 2000.0,
      "electricityAmount": 0.0,
      "gasAmount": 0.0,
      "otherAmount": 2000.0,        // ← 新增：其他成本
      "batchCount": 10,
      "ratioOfOutput": 0.8,
      "varianceToEstimated": 572204.62,  // 成本偏差 = 预计 − 实际
      "varianceRate": 99.3               // 偏差率（%）
    },
    "outputValue": { "...": "未变化" }
  },
  "production": { "...": "未变化" }
}
```

> **命名对照**：`varianceToEstimated` = 成本偏差（预计成本 − 实际成本），`varianceRate` = 成本偏差率。
> 前端如需显示「总毛利 / 毛利率」文案，直接复用这两个字段即可，无需后端再改。

### 2.2 `/api/dashboard/charts`

#### ① `costTrend[]` 新增偏差字段

```jsonc
{
  "month": "2026-09",
  "outputValue": 269368.48,
  "estimatedCost": 317283.00,
  "actualCost": 1200.00,
  "costVariance": 316083.00,     // ← 新增
  "costVarianceRate": 99.6       // ← 新增
}
```

#### ② `costStructure[]` 新增分类字段（`material` 保留不动）

```jsonc
{
  "month": "2026-09",
  "material": 3.24,        // 保留：= rawMaterial + auxiliary + packaging
  "rawMaterial": 3.24,     // ← 新增：原料
  "auxiliary": 0.00,       // ← 新增：辅料
  "packaging": 0.00,       // ← 新增：包材
  "water": 0.0,
  "electricity": 0.0,
  "gas": 0.0,
  "other": 1200.00         // ← 新增：其他
}
```

- 旧的「物料 / 水 / 电 / 气」四段图可以**完全不改**继续用。
- 新的六段图建议：原料 / 辅料 / 包材 / 水 / 电 / 气 / 其他（`material` 不再单独画，避免重复计算）。

#### ③ `production.balance`（新增节点，按「批」）

```jsonc
"balance": {
  "planCount": 8,                       // 计划批数
  "planQuantity": 30001.0,
  "fullyDeliveredPlanCount": 4,         // 已全部交付批数
  "fullyDeliveredPlanQuantity": 20000.0,
  "pendingPlanCount": 4,                // 待生产批数（含部分交付）
  "pendingPlanQuantity": 10001.0,
  "deliveredQuantity": 39200.0,         // 区间内实际交付量
  "fullyDeliveredRate": 50.0,           // 按批数算
  "pendingRate": 50.0
}
```

口径规则（重要）：

1. `fullyDeliveredPlanCount + pendingPlanCount = planCount`，**恒等**。
2. 判定依据是**计划量是否已全部交付**：`已交付量 >= 计划量` 才算「已全部交付」。
3. **部分交付的计划归入待生产侧**，所以「待生产 4 批」不等于「一点没做」。
4. 比率按**批数**算，不按件数算；数量字段保留，前端可做「按批 / 按件」切换。
5. 原 `production.planVsDelivery` **保持原样**（按件、按月），不需要改。

#### ④ `batchCostVarianceRank[]`（新增：批次盈亏排行）

```jsonc
[
  {
    "workOrderCode": "GD202610150003",
    "batchNumber": "20261015",
    "preparationCode": "Z01-001",
    "preparationName": "壮骨关节丸",
    "planNumber": "Plan202610150001",
    "batchQty": 5000.0,
    "estimatedCost": 40000.00,
    "actualCost": 2800.00,
    "costVariance": 37200.00,
    "costVarianceRate": 93.0,
    "productionDays": 1
  }
]
```

规则：

- 按 `costVarianceRate` **降序**，最多 10 条。
- `costVarianceRate > 0` 表示成本低于预估（「盈利」），`< 0` 表示成本超预估。
- **预计成本为 0 的批次不参与排名**（未维护预计成本价，偏差率无意义）。
- 只有产生过实际成本行的批次才会入榜，避免「零成本 = 100% 盈利」的假象。
- 建议展示：横向条形图，正负值用两种颜色；右侧显示「偏差率 %」和「生产天数」。

#### ⑤ `costStructureByPreparation[]`（新增：品种成本构成）

```jsonc
[
  {
    "preparationCode": "Z01-001",
    "preparationName": "壮骨关节丸",
    "rowType": "正常",
    "rawMaterialAmount": 6.20,
    "auxiliaryMaterialAmount": 1.10,
    "packagingAmount": 0.90,
    "utilityAmount": 2000.00,
    "otherAmount": 800.00,
    "totalAmount": 2808.20,
    "ratio": 70.0
  },
  {
    "preparationCode": null,
    "preparationName": "未关联制剂",
    "rowType": "未关联制剂",
    "totalAmount": 1200.00,
    "ratio": 29.9
  }
]
```

规则：

- 按 `totalAmount` 降序。
- `ratio` 为占**区间实际成本合计**的比例，合计约 100%（浮点尾差 ±0.1）。
- 「未关联制剂」「未分摊水电气」作为伪品种保留，**不要过滤**，否则合计对不上卡片。
- 原料/辅料/包材为按出库分类比例分摊的结果，三者之和恒等于该品种的物料成本。

### 2.3 明细行（`/metrics/details/{card}` 与树的 `batches`）

新增字段（旧字段全部保留）：

| 字段 | 类型 | 说明 | 出现位置 |
|------|------|------|---------|
| `unitEstimatedCost` | 元/件 | 单位预计成本（**快照价**） | 预估成本、实际成本明细 |
| `unitActualCost` | 元/件 | 单位实际成本 = 实际成本 ÷ 批量 | 实际成本明细 |
| `unitCostVariance` | 元/件 | 单位成本偏差 = 单位预计 − 单位实际 | 实际成本明细 |
| `unitCostVarianceRate` | % | 单位偏差率 = 单位偏差 ÷ 单位预计 | 实际成本明细 |
| `rawMaterialAmount` | 元 | 原料成本 | 实际成本明细 |
| `auxiliaryMaterialAmount` | 元 | 辅料成本 | 实际成本明细 |
| `packagingAmount` | 元 | 包材成本 | 实际成本明细 |
| `otherAmount` | 元 | 其他成本 | 实际成本明细 |
| `purchaseDate` | 字符串 | 采购需求下达时间（`yyyy-MM-dd HH:mm:ss`） | 预估成本、实际成本明细 |
| `inboundDate` | 字符串 | 成品入库时间 | 预估成本、实际成本明细 |
| `productionDays` | 天 | 批次生产天数（向上取整） | 预估成本、实际成本明细 |

**注意 `actualCost` 的口径变化**：

```
旧：actualCost = materialAmount + waterAmount + electricityAmount + gasAmount
新：actualCost = materialAmount + water + electricity + gas + otherAmount
```

`materialAmount` 仍是原辅包三类之和，`rawMaterialAmount + auxiliaryMaterialAmount + packagingAmount = materialAmount`（每行恒等）。

### 2.4 `/api/dashboard/details/tree`

新增字段：

| 位置 | 新增字段 |
|------|---------|
| `summary` | `costVariance`、`costVarianceRate` |
| `months[]` | `costVariance`、`costVarianceRate` |
| `months[].preparations[]` | `costVariance`、`costVarianceRate` |
| `months[].preparations[].batches[]` | 单位成本 4 个字段、原辅包 4 个字段、周期 3 个字段（同 §2.3） |

> 树的批次节点会同时承载「交付月行」和「成本月行」，所以同一批次可能只有 `unitEstimatedCost` 没有 `unitActualCost`（或反之）。前端按 `null` 显示 `—` 即可。

---

## 3. 前端改动点（按页面区块）

### 3.1 卡片区

| 卡片 | 是否要改 | 说明 |
|------|---------|------|
| 预估成本 | 不改 | 数值口径未变，只是取价改成了快照 |
| 实际成本 | **改** | 需要把「其他成本」加进构成说明（tooltip/明细行） |
| 预估产值 | 不改 | — |
| 总计划量 | 不改 | — |
| 总交付量 | 不改 | — |
| 待生产 | 不改 | — |

实际成本卡片的构成文案建议改为：`原辅包 + 水 + 电 + 气 + 其他`。

### 3.2 图表区

| 图表 | 动作 |
|------|------|
| 月度成本趋势 | 追加两条线/两个 tooltip 字段：`costVariance`、`costVarianceRate`（可选，不做也能用） |
| 实际成本构成（按月） | 四段 → 七段：原料/辅料/包材/水/电/气/其他；`material` 不再单独画 |
| 生产平衡 | **改用 `production.balance`**，按批展示；`planVsDelivery` 继续保留给「按件」视图 |
| 批次盈亏排行 | **新增**：横向条形图，X 轴 `costVarianceRate`，Y 轴 `batchNumber`，tooltip 带金额与生产天数 |
| 品种成本构成 | **新增**：堆叠柱状图或环形图，分类为原料/辅料/包材/水电气/其他 |

生产平衡推荐展示：

```
计划 8 批 = 已全部交付 4 批（20,000 件） + 待生产 4 批（10,001 件）
交付率（按件）130.7%   [按批 50% / 50%]
```

### 3.3 明细下钻

- 批次表新增列（建议默认隐藏后 3 列，提供「列设置」）：
  `单位预计成本`、`单位实际成本`、`单位偏差`、`偏差率`、`原料`、`辅料`、`包材`、`其他`、`采购下达`、`成品入库`、`生产天数`
- 数值为空时统一显示 `—`，不要当 0 处理（`productionDays` 为 null 表示采购单缺少生产计划关联，属于数据问题不是 0 天）。
- `rowType` 为「未关联制剂」「未分摊水电气」的行：不要展示单位成本与偏差列（无意义）。

### 3.4 能耗录入页（`/api/energy`）

- 类型下拉增加「**其他**」。
- 选择「其他」时：
  - 隐藏「上月表底 / 本月表底 / 单价 / 实用量」输入；
  - 「总价」必填，单位显示 `元`；
  - 提交 `actual_usage`、`unit_price` 为空即可。
- 后端校验：类型必须属于 `自来水 / 电 / 燃气 / 其他`；「其他」未填总价会返回 `其他成本必须填写总价`。

---

## 4. 边界与空态

| 场景 | 返回 |
|------|------|
| 制剂未维护预计成本价且工单无快照 | `estimatedCost=0`、`unitEstimatedCost=null`；计入 `missingPriceBatchCount` |
| 批量（`batchQty`）为 0 | `unitActualCost=null`、`unitCostVariance=null`（不抛异常） |
| 预估成本为 0 | `costVarianceRate=null` |
| 批次无采购单 / 无入库时间 | `purchaseDate` 或 `inboundDate` 为 null，`productionDays=null` |
| 当月无已交付工单 | 水电气/其他成本进入「未分摊水电气」伪行，金额不丢 |
| 区间无任何数据 | 各图表返回空数组，卡片数值为 0 |

---

## 5. 联调自测清单

- [ ] `/metrics` 的 `cost.actualCost.actualCost` = `materialAmount + waterAmount + electricityAmount + gasAmount + otherAmount`
- [ ] `/metrics/details/actual-cost` 明细逐行 `actualCost` 合计 = 卡片 `actualCost`
- [ ] 明细每行 `rawMaterialAmount + auxiliaryMaterialAmount + packagingAmount` = `materialAmount`
- [ ] `/charts` 的 `costStructure` 各月 `material` = `rawMaterial + auxiliary + packaging`
- [ ] `costStructure` 全月合计 = 卡片 `actualCost`
- [ ] `costStructureByPreparation` 的 `totalAmount` 合计 = 卡片 `actualCost`
- [ ] `production.balance`：`fullyDeliveredPlanCount + pendingPlanCount = planCount`
- [ ] `batchCostVarianceRank` 无 `estimatedCost = 0` 的行，且按 `costVarianceRate` 降序
- [ ] 树的月份 `costVariance` = `estimatedCost − actualCost`
- [ ] 修改制剂预计成本价后，历史月份 `estimatedCost` **不变**（快照生效）
- [ ] 能耗录入「其他」后，当月实际成本与对应批次 `otherAmount` 增加

---

## 6. 数据库变更（运维 / 部署必读）

```sql
-- 1. 工单预计成本价快照
ALTER TABLE work_order
  ADD COLUMN estimated_cost_price_snapshot DECIMAL(18,4) NULL
  COMMENT '预计成本价快照（元/件），工单创建时冻结' AFTER sales_price;

-- 2. 历史工单快照回填（按当前制剂价格，可重复执行）
UPDATE work_order w
JOIN preparation p ON p.preparation_code = w.preparation_code
SET w.estimated_cost_price_snapshot = p.estimated_cost_price
WHERE w.estimated_cost_price_snapshot IS NULL AND p.estimated_cost_price IS NOT NULL;

-- 3. 历史销售单价回填（仅补空值，不覆盖已有）
UPDATE work_order w
JOIN preparation p ON p.preparation_code = w.preparation_code
SET w.sales_price = p.sales_price
WHERE (w.sales_price IS NULL OR w.sales_price = 0) AND p.sales_price IS NOT NULL;
```

- `energy_record.energy_type` 为 `varchar(20)`，无需改结构，仅放开枚举值「其他」。
- 回填只能按**上线时的制剂价格**补齐，无法还原真实历史价格；回填前后历史月份数字会变化，属预期。

---

## 7. 口径备忘

1. **生产数量**：一律使用工单批量 `batch_qty`（按批，不按件），胶囊类同样适用。
2. **成本偏差**：`预计成本 − 实际成本`，正数 = 成本低于预期。
3. **水电气 / 其他成本分摊**：按月金额 ÷ 当月已交付工单数，尾差由最后一张工单承接。
4. **物料成本**：出库明细金额 − 退库冲减，先归生产计划，再按批量分摊到工单。
5. **原辅包拆分**：按「月份 + 计划」的分类金额比例拆分到工单，尾差计入包材，保证行内三类之和 = 物料成本。
6. **批次生产天数**：`成品入库时间 − 最早采购单创建时间`，向上取整；采购单优先按 `production_plan_id` 关联，缺失时回退 `production_plan_code`。
7. **快照优先级**：工单快照 → 制剂编码 → 制剂 ID。只有工单创建时的价格会被冻结，`updateWorkOrder` 不回写快照。

---

## 8. 本次后端验证结果（测试库 8.134.48.54 / 2026-08 ~ 2026-10）

```jsonc
// /api/dashboard/metrics cost.actualCost
{
  "actualCost": 4011.38,
  "materialAmount": 11.38,
  "waterAmount": 2000.0,
  "electricityAmount": 0.0,
  "gasAmount": 0.0,
  "otherAmount": 2000.0,
  "batchCount": 10,
  "ratioOfOutput": 0.8,
  "varianceToEstimated": 572204.62,
  "varianceRate": 99.3
}

// /api/dashboard/charts production.balance
{
  "planCount": 8,
  "planQuantity": 30001.0,
  "fullyDeliveredPlanCount": 4,
  "fullyDeliveredPlanQuantity": 20000.0,
  "pendingPlanCount": 4,
  "pendingPlanQuantity": 10001.0,
  "deliveredQuantity": 39200.0,
  "fullyDeliveredRate": 50.0,
  "pendingRate": 50.0
}
```

> 测试库物料金额很小（出库数据少），所以偏差率接近 100%，属数据量问题而非算法问题。

---

## 9. 后端改动文件清单

| 文件 | 改动 |
|------|------|
| `Data/Entity/WorkOrder.java` | 新增 `estimatedCostPriceSnapshot` |
| `Service/impl/WorkOrderServiceImpl.java` | 工单创建时冻结快照（预计成本价 + 销售单价） |
| `Service/impl/EnergyRecordServiceImpl.java` | 能耗类型新增「其他」，单位=元，金额直填 |
| `Data/mapper/EnergyRecordMapper.java` | 汇总 SQL 新增 `otherAmount` |
| `Common/Dto/Dashboard/DashboardMetricsDto.java` | `ActualCostCardDto` 新增 `otherAmount` |
| `Common/Dto/Dashboard/DashboardDetailRowDto.java` | 新增单位成本、原辅包、周期共 11 个字段 |
| `Common/Dto/Dashboard/DashboardTreeDto.java` | 新增偏差与批次扩展字段 |
| `Common/Dto/Dashboard/ChartDataDto.java` | 趋势偏差、构成分类、生产平衡、两个新图表 |
| `Service/DashboardCostService.java` | 新增 2 个图表方法 |
| `Service/impl/DashboardCostServiceImpl.java` | 快照取价、其他成本、分类拆分、批次周期、2 个新图表 |
| `Service/impl/DashboardProductionServiceImpl.java` | 生产平衡数据 |
| `Service/impl/DashboardTreeServiceImpl.java` | 偏差与批次扩展字段 |
| `Controller/DashboardController.java` | `/charts` 挂载 2 个新图表 |

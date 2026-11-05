# 首页看板 V2 前端落地指南（文件级）

> 前端工程：`_analysis/frontend-src`（Vue3 `<script setup lang="ts">` + Element Plus + ECharts）
> 后端版本：`8471a17`（接口新字段已生效）
> 数据库：生产库 `work_order.estimated_cost_price_snapshot` 已加列，**数据零变化**
> 原则：**只做加法**。不改名、不删字段、不改现有字段语义；新增能力全部走新字段 / 新节点。

---

## 0. 一句话结论

前端只需要动 **6 组 P0 文件** 就能看到全部新能力：

| 文件 | 必改内容 |
|------|---------|
| `src/api/dashboard.ts` | 补类型（不影响运行，但 TS 编译必须有） |
| `src/views/new_statistical/utils/constants.ts` | 新增配色 + 抽屉列（P1 展开列也在这一步） |
| `src/views/new_statistical/utils/chartOptions.ts` | 新增 3 个图表 option 生成器 |
| `src/views/new_statistical/components/CostCharts.vue` | 成本构成切 7 段 + 挂 2 个新图 |
| `src/views/new_statistical/index.vue` | 实际成本构成条加「其他」+ 生产平衡改按批 |
| `src/views/energy/**` | 能耗类型加「其他」 |

P1 再补：`MonthDrilldown.vue` 批次表新列、`DetailDrawer` 列定义。

---

## 1. 不要做的事（避免把「加法」做成「改口径」）

1. **不要**把 `varianceToEstimated` / `varianceRate` 改名成 `grossProfit` / `grossProfitRate`。前端文案写「成本节约额 / 成本节约率」，字段名保持原样。
2. **不要**删 `production.planVsDelivery`，它是「按件、按月」视图；新的 `production.balance` 是「按批、区间汇总」视图，两者并存。
3. **不要**过滤 `costStructureByPreparation` 里的「未关联制剂 / 未分摊水电气」伪品种，否则合计对不上卡片。
4. **不要**把 `null` 当 0。单位成本、生产天数、节约率都可能是 `null`，统一走 `fmt*` 显示 `—`。
5. **不要**改卡片金额单位：接口仍是「元」，展示层才 `/10000` 显示万元。

> ⚠️ **术语红线**：制药语境里「偏差（Deviation）」是 GMP 质量体系的专有名词（偏离批准规程/标准），系统已有「偏差管理」模块。成本口径**禁止**再叫「偏差」，统一用「成本节约额 / 成本节约率」，避免和 QA 的偏差混淆。

#### 1.1 显示文案对照表（本次统一）

| 接口字段 | 旧文案 | 新文案 | 计算口径 |
|---|---|---|---|
| `ratioOfOutput` | 占预估产值 | **产值成本率** | 成本 ÷ 预估产值 × 100% |
| `estimatedCostRatio` | 预估成本率 | **预估产值成本率** | 预估成本 ÷ 预估产值 × 100% |
| `actualCostRatio` | 实际成本率 | **实际产值成本率** | 实际成本 ÷ 预估产值 × 100% |
| `varianceToEstimated` | 成本偏差 | **成本节约额** | 预计成本 − 实际成本（正=节约，负=超支） |
| `varianceRate` | 偏差率 | **成本节约率** | 成本节约额 ÷ 预计成本 × 100% |
| `unitCostVariance` | 单位偏差 | **单位成本节约额** | 预计单位成本 − 实际单位成本 |
| `unitCostVarianceRate` | 单位偏差率 | **单位成本节约率** | 单位节约额 ÷ 预计单位成本 × 100% |
| `momRate` | 环比 | **末月环比** | 最新有数据月 vs 上一自然月，（本月−上月）÷ 上月 |

---

## 2. `src/api/dashboard.ts`

### 2.1 实际成本卡片：新增 `otherAmount`

`ActualCostCardDto` 追加：

```ts
export interface ActualCostCardDto {
  actualCost?: number | null
  materialAmount?: number | null
  waterAmount?: number | null
  electricityAmount?: number | null
  gasAmount?: number | null
  /** 其他成本（元）：energy_record.energy_type = 其他 */
  otherAmount?: number | null
  batchCount?: number | null
  ratioOfOutput?: number | null
  /** 成本节约额（元）= 预估成本 − 实际成本（正=节约，负=超支） */
  varianceToEstimated?: number | null
  /** 成本节约率（%） */
  varianceRate?: number | null
}
```

### 2.2 月度趋势：新增成本节约字段

```ts
export interface CostTrendItem {
  month: string
  outputValue?: number | null
  estimatedCost?: number | null
  actualCost?: number | null
  /** 成本节约额（元）= 预估成本 − 实际成本 */
  costVariance?: number | null
  /** 成本节约率（%）= 成本节约额 ÷ 预估成本 */
  costVarianceRate?: number | null
}
```

### 2.3 成本构成：新增原辅包拆分与「其他」

```ts
export interface CostStructureItem {
  month: string
  /** 物料合计（元）= rawMaterial + auxiliary + packaging */
  material?: number | null
  /** 原料（元） */
  rawMaterial?: number | null
  /** 辅料（元） */
  auxiliary?: number | null
  /** 包材（元） */
  packaging?: number | null
  water?: number | null
  electricity?: number | null
  gas?: number | null
  /** 其他（元） */
  other?: number | null
}
```

### 2.4 新增 3 个图表类型

```ts
/** 生产平衡（按批） */
export interface ProductionBalance {
  /** 计划批数 */
  planCount?: number | null
  /** 计划量合计（件） */
  planQuantity?: number | null
  /** 已全部交付计划批数 */
  fullyDeliveredPlanCount?: number | null
  /** 已全部交付计划量（件） */
  fullyDeliveredPlanQuantity?: number | null
  /** 待生产计划批数（含部分交付） */
  pendingPlanCount?: number | null
  /** 待生产计划量（件） */
  pendingPlanQuantity?: number | null
  /** 区间内实际交付量（件） */
  deliveredQuantity?: number | null
  /** 已全部交付批数占比（%） */
  fullyDeliveredRate?: number | null
  /** 待生产批数占比（%） */
  pendingRate?: number | null
}

/** 批次成本节约排行项 */
export interface BatchCostVarianceRankItem {
  workOrderCode?: string | null
  batchNumber?: string | null
  preparationCode?: string | null
  preparationName?: string | null
  planNumber?: string | null
  /** 批量（件） */
  batchQty?: number | null
  /** 预估成本（元） */
  estimatedCost?: number | null
  /** 实际成本（元） */
  actualCost?: number | null
  /** 成本节约额（元）= 预估 − 实际 */
  costVariance?: number | null
  /** 成本节约率（%）；正数 = 成本低于预估（节约） */
  costVarianceRate?: number | null
  /** 批次生产天数（采购下达 → 成品入库） */
  productionDays?: number | null
}

/** 按品种拆分的实际成本构成 */
export interface CostStructureByPreparationItem {
  preparationCode?: string | null
  preparationName?: string | null
  /** 正常 / 未关联制剂 / 未分摊水电气 */
  rowType?: string | null
  rawMaterialAmount?: number | null
  auxiliaryMaterialAmount?: number | null
  packagingAmount?: number | null
  /** 水电气合计（元） */
  utilityAmount?: number | null
  /** 其他（元） */
  otherAmount?: number | null
  /** 实际成本合计（元） */
  totalAmount?: number | null
  /** 占区间实际成本比例（%） */
  ratio?: number | null
}
```

### 2.5 `ChartDataDto` 挂载新节点

```ts
export interface ChartDataDto {
  months?: string[] | null
  costTrend?: CostTrendItem[] | null
  costStructure?: CostStructureItem[] | null
  /** 批次成本节约排行（按成本节约率降序，最多 10 条） */
  batchCostVarianceRank?: BatchCostVarianceRankItem[] | null
  /** 品种成本构成（按实际成本降序） */
  costStructureByPreparation?: CostStructureByPreparationItem[] | null
  materialFunds?: MaterialFunds | null
  production?: {
    planVsDelivery?: PlanVsDeliveryItem[] | null
    pendingByMonth?: PendingMonthItem[] | null
    topPreparations?: PreparationRank[] | null
    /** 新增：按批的生产平衡 */
    balance?: ProductionBalance | null
  } | null
}
```

### 2.6 明细行新增字段

`DashboardDetailRowDto` 追加：

```ts
  /** 单位预计成本（元/件，快照价） */
  unitEstimatedCost?: number | null
  /** 单位实际成本（元/件）= 实际成本 ÷ 批量 */
  unitActualCost?: number | null
  /** 单位成本节约额（元/件）= 预计单位成本 − 实际单位成本 */
  unitCostVariance?: number | null
  /** 单位成本节约率（%） */
  unitCostVarianceRate?: number | null
  /** 原料（元） */
  rawMaterialAmount?: number | null
  /** 辅料（元） */
  auxiliaryMaterialAmount?: number | null
  /** 包材（元） */
  packagingAmount?: number | null
  /** 其他（元） */
  otherAmount?: number | null
  /** 采购需求下达时间 */
  purchaseDate?: string | null
  /** 成品入库时间 */
  inboundDate?: string | null
  /** 批次生产天数（天） */
  productionDays?: number | null
```

### 2.7 明细树新增字段

```ts
// DashboardTreeSummaryDto
  costVariance?: number | null
  costVarianceRate?: number | null

// DashboardTreeMonthDto
  costVariance?: number | null
  costVarianceRate?: number | null

// DashboardTreePreparationDto
  costVariance?: number | null
  costVarianceRate?: number | null

// DashboardTreeBatchDto
  rawMaterialAmount?: number | null
  auxiliaryMaterialAmount?: number | null
  packagingAmount?: number | null
  otherAmount?: number | null
  unitEstimatedCost?: number | null
  unitActualCost?: number | null
  unitCostVariance?: number | null
  unitCostVarianceRate?: number | null
  purchaseDate?: string | null
  inboundDate?: string | null
  productionDays?: number | null
```

---

## 3. `src/views/new_statistical/utils/constants.ts`

### 3.1 配色：只加 key，不改旧 key

```ts
/** 实际成本构成配色（旧 key 全部保留） */
export const COST_STRUCTURE_COLORS = {
  material: COLORS.primary,
  rawMaterial: COLORS.primary,
  auxiliary: COLORS.green,
  packaging: '#ee46bc', // 与「超支」的红色区分开
  water: COLORS.cyan,
  electricity: COLORS.violet,
  gas: COLORS.amber,
  other: '#667085'
}
```

### 3.2 抽屉列：实际成本明细

在 `CARD_DRAWER_CONFIG.actualCost.columns` 中，`materialAmount` 后插入原辅包拆分，`gasAmount` 后插入「其他」，末尾追加单位成本与周期：

```ts
      { prop: 'materialAmount', label: '物料合计', align: 'right', type: 'money', total: true },
      // ↓ 新增：原辅包拆分（三者之和 = materialAmount）
      { prop: 'rawMaterialAmount', label: '原料', align: 'right', type: 'money' },
      { prop: 'auxiliaryMaterialAmount', label: '辅料', align: 'right', type: 'money' },
      { prop: 'packagingAmount', label: '包材', align: 'right', type: 'money' },
      { prop: 'waterAmount', label: '水', align: 'right', type: 'money', total: true },
      { prop: 'electricityAmount', label: '电', align: 'right', type: 'money', total: true },
      { prop: 'gasAmount', label: '气', align: 'right', type: 'money', total: true },
      // ↓ 新增：其他（energy_type = 其他）
      { prop: 'otherAmount', label: '其他', align: 'right', type: 'money', total: true },
      { prop: 'actualCost', label: '实际成本', align: 'right', type: 'money', total: true },
      // ↓ 新增：单位成本（不要设 total，单位成本相加无意义）
      { prop: 'unitEstimatedCost', label: '预计单位成本', align: 'right', type: 'money' },
      { prop: 'unitActualCost', label: '实际单位成本', align: 'right', type: 'money' },
      { prop: 'unitCostVariance', label: '单位成本节约额', align: 'right', type: 'money' },
      { prop: 'unitCostVarianceRate', label: '单位成本节约率', align: 'right', type: 'percent' },
      // ↓ 新增：批次周期
      { prop: 'purchaseDate', label: '采购下达', minWidth: 150 },
      { prop: 'inboundDate', label: '成品入库', minWidth: 150 },
      { prop: 'productionDays', label: '生产天数', align: 'right', type: 'day' }
```

> `rawMaterialAmount` / `auxiliaryMaterialAmount` / `packagingAmount` **不要设 `total: true`**：后端 `totals` 里没有这三个汇总字段，未筛选时会显示 `—`。筛选态下前端会自动累加（`filteredTotals`），行为正常。

### 3.3 抽屉列：预估成本明细

在 `CARD_DRAWER_CONFIG.estimatedCost.columns` 末尾追加：

```ts
      { prop: 'unitEstimatedCost', label: '单位预计成本', align: 'right', type: 'money' },
      { prop: 'purchaseDate', label: '采购下达', minWidth: 150 },
      { prop: 'inboundDate', label: '成品入库', minWidth: 150 },
      { prop: 'productionDays', label: '生产天数', align: 'right', type: 'day' }
```

---

## 4. `src/views/new_statistical/utils/chartOptions.ts`

### 4.1 顶部 import 调整

```ts
import type {
  BatchCostVarianceRankItem,
  CostStructureByPreparationItem,
  CostStructureItem,
  CostTrendItem,
  MaterialFundSeries,
  PendingMonthItem,
  PlanVsDeliveryItem,
  PreparationRank
} from '@/api/dashboard'
import { CATEGORY_COLORS, COLORS, COST_STRUCTURE_COLORS, MATERIAL_CATEGORIES } from './constants'
import {
  fmtInt,
  fmtMoney,
  fmtMonthShort,
  fmtNumber,
  fmtPercent,
  fmtQty,
  isNil
} from './format'
```

### 4.2 新增「7 段成本构成」

原 `costStructureOption` **保留不动**，新增一个函数，`CostCharts.vue` 改为调用新函数：

```ts
/** 2b. 实际成本构成 V2：原料 / 辅料 / 包材 / 水 / 电 / 气 / 其他 */
export function costStructureSplitOption(
  data?: CostStructureItem[] | null,
  months?: string[] | null
): EChartsOption {
  const list = data || []
  const axis = months?.length ? months : list.map((it) => it.month)
  const pick = (month: string) => list.find((it) => it.month === month)

  // 后端已返回拆分字段时用 7 段；否则回退旧的 4 段，保证旧环境不空白
  const hasSplit = list.some(
    (it) => !isNil(it.rawMaterial) || !isNil(it.auxiliary) || !isNil(it.packaging)
  )

  const defs = hasSplit
    ? [
        { name: '原料', color: COST_STRUCTURE_COLORS.rawMaterial, key: 'rawMaterial' },
        { name: '辅料', color: COST_STRUCTURE_COLORS.auxiliary, key: 'auxiliary' },
        { name: '包材', color: COST_STRUCTURE_COLORS.packaging, key: 'packaging' },
        { name: '水', color: COST_STRUCTURE_COLORS.water, key: 'water' },
        { name: '电', color: COST_STRUCTURE_COLORS.electricity, key: 'electricity' },
        { name: '气', color: COST_STRUCTURE_COLORS.gas, key: 'gas' },
        { name: '其他', color: COST_STRUCTURE_COLORS.other, key: 'other' }
      ]
    : [
        { name: '物料', color: COST_STRUCTURE_COLORS.material, key: 'material' },
        { name: '水', color: COST_STRUCTURE_COLORS.water, key: 'water' },
        { name: '电', color: COST_STRUCTURE_COLORS.electricity, key: 'electricity' },
        { name: '气', color: COST_STRUCTURE_COLORS.gas, key: 'gas' }
      ]

  const series = defs.map((item, index) => ({
    name: item.name,
    type: 'bar' as const,
    stack: 'cost',
    barMaxWidth: 26,
    itemStyle: {
      color: item.color,
      borderRadius: index === defs.length - 1 ? [4, 4, 0, 0] : [0, 0, 0, 0]
    },
    data: axis.map((month) => Number(wan((pick(month) as any)?.[item.key]).toFixed(2)))
  }))

  return {
    grid: { ...baseGrid, top: 40 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value: any) => `${fmtNumber(Number(value), 2)} 万元`
    },
    legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: AXIS_LABEL_STYLE },
    xAxis: {
      type: 'category',
      data: axis.map((month) => fmtMonthShort(month)),
      axisLabel: AXIS_LABEL_STYLE,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: COLORS.line } }
    },
    yAxis: {
      type: 'value',
      name: '万元',
      nameTextStyle: AXIS_LABEL_STYLE,
      axisLabel: AXIS_LABEL_STYLE,
      splitLine: SPLIT_LINE
    },
    series
  }
}
```

### 4.3 新增「品种成本构成」（横向堆叠，Top 8）

```ts
/** 7. 品种成本构成：横向堆叠柱（Top 8，单位万元） */
export function preparationCostStructureOption(
  data?: CostStructureByPreparationItem[] | null
): EChartsOption {
  const list = (data || []).slice(0, 8)

  const defs = [
    { name: '原料', color: COST_STRUCTURE_COLORS.rawMaterial, key: 'rawMaterialAmount' },
    { name: '辅料', color: COST_STRUCTURE_COLORS.auxiliary, key: 'auxiliaryMaterialAmount' },
    { name: '包材', color: COST_STRUCTURE_COLORS.packaging, key: 'packagingAmount' },
    { name: '水电气', color: COST_STRUCTURE_COLORS.water, key: 'utilityAmount' },
    { name: '其他', color: COST_STRUCTURE_COLORS.other, key: 'otherAmount' }
  ]

  return {
    grid: { left: 8, right: 56, top: 36, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value: any) => `${fmtNumber(Number(value), 2)} 万元`
    },
    legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: AXIS_LABEL_STYLE },
    xAxis: {
      type: 'value',
      name: '万元',
      nameTextStyle: AXIS_LABEL_STYLE,
      axisLabel: AXIS_LABEL_STYLE,
      splitLine: SPLIT_LINE
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: list.map((it) => it.preparationName || '—'),
      axisLabel: { ...AXIS_LABEL_STYLE, color: COLORS.text },
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: defs.map((item, index) => ({
      name: item.name,
      type: 'bar' as const,
      stack: 'prep',
      barMaxWidth: 24,
      itemStyle: {
        color: item.color,
        borderRadius: index === defs.length - 1 ? [0, 4, 4, 0] : [0, 0, 0, 0]
      },
      data: list.map((it) => Number(wan((it as any)[item.key]).toFixed(2)))
    }))
  }
}
```

### 4.4 新增「批次成本节约排行」

```ts
/** 8. 批次成本节约排行：横向条形（X 轴为成本节约率 %，正=节约，负=超支） */
export function batchCostVarianceRankOption(
  data?: BatchCostVarianceRankItem[] | null
): EChartsOption {
  const list = (data || []).filter((it) => !isNil(it.costVarianceRate))

  return {
    grid: { left: 8, right: 64, top: 8, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const item = list[params?.[0]?.dataIndex]
        if (!item) return ''
        return [
          `${item.batchNumber || '—'} · ${item.preparationName || '—'}`,
          `成本节约率：${fmtPercent(item.costVarianceRate)}`,
          `预估成本：${fmtMoney(item.estimatedCost)} 元`,
          `实际成本：${fmtMoney(item.actualCost)} 元`,
          `成本节约额：${fmtMoney(item.costVariance)} 元`,
          `生产天数：${isNil(item.productionDays) ? '—' : `${fmtInt(item.productionDays)} 天`}`
        ].join('<br/>')
      }
    },
    xAxis: {
      type: 'value',
      name: '%',
      nameTextStyle: AXIS_LABEL_STYLE,
      axisLabel: AXIS_LABEL_STYLE,
      splitLine: SPLIT_LINE
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: list.map((it) => it.batchNumber || it.workOrderCode || '—'),
      axisLabel: { ...AXIS_LABEL_STYLE, color: COLORS.text },
      axisTick: { show: false },
      axisLine: { show: false }
    },
    series: [
      {
        name: '成本节约率',
        type: 'bar',
        barMaxWidth: 14,
        itemStyle: {
          color: (params: any) => (Number(params.value) >= 0 ? COLORS.green : COLORS.rose),
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'right',
          fontSize: 11,
          color: COLORS.text,
          formatter: ({ value }: any) => `${fmtNumber(Number(value), 1)}%`
        },
        data: list.map((it) => Number(it.costVarianceRate))
      }
    ]
  }
}
```

---

## 5. `src/views/new_statistical/components/CostCharts.vue`

### 5.1 模板：成本构成文案

```html
<p class="card__desc">单位：万元 · 原辅包 + 平摊水电气 + 其他 · 点击柱子查看当月明细</p>
```

### 5.2 模板：在原两图下方追加一行两图

紧跟在 `charts__row--2` 结束的 `</div>` 之后、原辅包卡片之前插入：

```html
    <div class="charts__row charts__row--2">
      <div class="card">
        <div class="card__head">
          <div>
            <h3 class="card__title"><span class="dot"></span>品种成本构成</h3>
            <p class="card__desc">
              单位：万元 · 按实际成本降序 · 含「未关联制剂 / 未分摊水电气」伪品种
            </p>
          </div>
        </div>
        <BaseChart
          :option="prepStructureOption"
          :empty="structureEmpty"
          height="300px"
        />
      </div>

      <div class="card">
        <div class="card__head">
          <div>
            <h3 class="card__title"><span class="dot tone-rose"></span>批次成本节约排行</h3>
            <p class="card__desc">
              成本节约率 =（预计成本 − 实际成本）÷ 预计成本 · 正=节约，负=超支
            </p>
          </div>
        </div>
        <BaseChart :option="rankOption" :empty="rankEmpty" height="300px" />
      </div>
    </div>
```

### 5.3 脚本：import 与 computed

```ts
  import {
    batchCostVarianceRankOption,
    costStructureSplitOption,
    costTrendOption,
    materialFundOption,
    preparationCostStructureOption
  } from '../utils/chartOptions'
```

```ts
  const trendOption = computed(() => costTrendOption(props.data?.costTrend, months.value))

  // 原 costStructureOption 保留但不再调用；改调 7 段版本
  const structureOption = computed(() =>
    costStructureSplitOption(props.data?.costStructure, months.value)
  )

  const prepStructureOption = computed(() =>
    preparationCostStructureOption(props.data?.costStructureByPreparation)
  )

  const rankOption = computed(() =>
    batchCostVarianceRankOption(props.data?.batchCostVarianceRank)
  )

  const rankEmpty = computed(
    () => !(props.data?.batchCostVarianceRank || []).length
  )

  const structureEmpty = computed(
    () =>
      !months.value.length ||
      isAllZero(props.data?.costStructure, [
        'rawMaterial',
        'auxiliary',
        'packaging',
        'material',
        'water',
        'electricity',
        'gas',
        'other'
      ])
  )
```

### 5.4 样式：新增约 1 行

```scss
  .card__title {
    .dot {
      // 原有内容不动
    }

    // 新增：批次成本节约排行标记
    .tone-rose {
      background: #f04438;
    }
  }
```

> 如果不想加样式，把模板里的 `dot tone-rose` 改成 `dot` 即可，不影响功能。

---

## 6. `src/views/new_statistical/index.vue`

### 6.1 实际成本卡片文案

```html
        formula="原辅包 + 平摊水电气 + 其他"
        ...
        open-text="查看明细（原辅包 / 水 / 电 / 气 / 其他 拆分）"
```

### 6.2 实际成本构成条：追加「其他」

```ts
  /** 实际成本构成条：五段宽度按 amount 归一化 */
  const composeSegments = computed(() => {
    const cardData = cost.value.actualCost
    const segments = [
      {
        key: 'material',
        label: '物料',
        value: Number(cardData?.materialAmount || 0),
        color: COST_STRUCTURE_COLORS.material
      },
      {
        key: 'water',
        label: '水',
        value: Number(cardData?.waterAmount || 0),
        color: COST_STRUCTURE_COLORS.water
      },
      {
        key: 'electricity',
        label: '电',
        value: Number(cardData?.electricityAmount || 0),
        color: COST_STRUCTURE_COLORS.electricity
      },
      {
        key: 'gas',
        label: '气',
        value: Number(cardData?.gasAmount || 0),
        color: COST_STRUCTURE_COLORS.gas
      },
      // ↓ 新增
      {
        key: 'other',
        label: '其他',
        value: Number(cardData?.otherAmount || 0),
        color: COST_STRUCTURE_COLORS.other
      }
    ]
    const total = segments.reduce((sum, item) => sum + item.value, 0) || 1
    return segments.map((item) => ({
      ...item,
      percent: Number(((item.value / total) * 100).toFixed(2)),
      text: `${fmtQty(item.value / 10000)}万`
    }))
  })
```

### 6.3 生产平衡：改按「批」

脚本新增：

```ts
  /** 生产平衡（按批，来自 /charts production.balance） */
  const balance = computed(() => charts.value?.production?.balance)

  /** 平衡条右上角合计文案：优先按批，旧后端回退按件 */
  const balanceTotalText = computed(() => {
    const b = balance.value
    if (b && Number(b.planCount || 0) > 0) {
      return `${fmtInt(b.planCount)} 批 · ${fmtQty(b.planQuantity)} 件`
    }
    return `${fmtInt(plan.value.totalPlan?.planCount)} 张计划单 · ${fmtQty(
      plan.value.totalPlan?.planQuantity
    )} 件`
  })
```

模板头部替换：

```html
        <div>
          <div class="balance__title">生产平衡</div>
          <div class="balance__sub">计划批数 = 已全部交付 + 待生产（含部分交付）</div>
        </div>
        <span class="balance__total">{{ balanceTotalText }}</span>
```

`balanceSegments` 整体替换：

```ts
  /** 生产平衡条：按批（已全部交付 / 待生产） */
  const balanceSegments = computed(() => {
    const b = balance.value
    const planned = Number(b?.planCount || 0)

    // 新口径：按批。partially delivered 归入待生产侧
    if (planned > 0) {
      const delivered = Number(b?.fullyDeliveredPlanCount || 0)
      const pending = Number(b?.pendingPlanCount || 0)
      return [
        {
          key: 'delivered',
          label: '已全部交付',
          color: COLORS.green,
          percent: (delivered / planned) * 100,
          text: `${delivered} 批 · ${fmtPercent(b?.fullyDeliveredRate)}`
        },
        {
          key: 'pending',
          label: '待生产（含部分交付）',
          color: COLORS.amber,
          percent: (pending / planned) * 100,
          text: `${pending} 批 · ${fmtPercent(b?.pendingRate)}`
        }
      ]
    }

    // 兼容旧后端（无 balance）：保留原按件口径
    const delivered = Number(plan.value.delivery?.deliveryQuantity || 0)
    const pending = Number(plan.value.pending?.pendingQuantity || 0)
    const plannedQty = Number(plan.value.totalPlan?.planQuantity || 0)
    const total = Math.max(delivered + pending, plannedQty, 1)

    return [
      {
        key: 'delivered',
        label: '已交付',
        color: COLORS.green,
        percent: (delivered / total) * 100,
        text: `${fmtQty(delivered)} 件 · ${fmtPercent(total ? (delivered / total) * 100 : null)}`
      },
      {
        key: 'pending',
        label: '待生产',
        color: COLORS.amber,
        percent: (pending / total) * 100,
        text: `${fmtQty(pending)} 件`
      },
      {
        key: 'planned',
        label: '计划合计',
        color: '#c7d2e5',
        percent: (Math.max(plannedQty - delivered - pending, 0) / total) * 100,
        text: `${fmtQty(plannedQty)} 件`
      }
    ]
  })
```

> `production.balance` 的 `fullyDeliveredPlanCount + pendingPlanCount = planCount` 恒等，所以两段加起来正好 100%，不会再出现旧的「计划合计」第三段。

---

## 7. 能耗页：支持「其他」

### 7.1 `src/views/energy/modules/energy-dialog.vue`

**模板**：类型下拉加一项；表底/实用量与「其他」互斥。

```html
              <ElOption label="自来水" value="自来水" />
              <ElOption label="电" value="电" />
              <ElOption label="燃气" value="燃气" />
              <!-- 新增 -->
              <ElOption label="其他" value="其他" />
```

给「上月表底读数」「本月表底读数」「本月实用量」三个 `<ElCol>` 加 `v-if="!isOther"`：

```html
        <!-- 上月表底读数 -->
        <ElCol v-if="!isOther" :span="12">
          ...
        </ElCol>
        <!-- 本月表底读数 -->
        <ElCol v-if="!isOther" :span="12">
          ...
        </ElCol>
        <!-- 本月实用量 -->
        <ElCol v-if="!isOther" :span="12">
          ...
        </ElCol>
```

**脚本**：

```ts
  import { computed, nextTick, reactive, ref, watch } from 'vue'

  /** 是否为「其他」类型：只填总价，不填表底 */
  const isOther = computed(() => formData.energyType === '其他')
```

单位映射：

```ts
  const ENERGY_TYPE_UNIT_MAP: Record<string, string> = {
    自来水: '立方米',
    电: '度',
    燃气: '立方米',
    // 新增：其他成本按金额录入
    其他: '元'
  }

  const handleEnergyTypeChange = (val: string) => {
    formData.unit = ENERGY_TYPE_UNIT_MAP[val] || ''
    // 「其他」清空表底与实用量，避免脏数据提交
    if (val === '其他') {
      formData.lastMeterReading = undefined
      formData.currentMeterReading = undefined
      formData.actualUsage = undefined
    }
  }
```

校验规则改为 computed（「其他」必填总价）：

```ts
  const formRules = computed(() => ({
    month: [{ required: true, message: '请选择月份', trigger: 'change' }],
    energyType: [{ required: true, message: '请选择能耗类型', trigger: 'change' }],
    lastMeterReading: isOther.value
      ? []
      : [{ validator: validateLastReading, trigger: 'blur' }],
    currentMeterReading: isOther.value
      ? []
      : [{ validator: validateCurrentReading, trigger: 'blur' }],
    totalAmount: isOther.value
      ? [
          { required: true, message: '其他成本必须填写总价', trigger: 'blur' },
          { validator: validateTotalAmount, trigger: 'blur' }
        ]
      : [{ validator: validateTotalAmount, trigger: 'blur' }]
  }))
```

提交逻辑无需改：`actualUsage` 为 `undefined` 时不会进 payload，后端允许为空。

### 7.2 `src/views/energy/index.vue`

**搜索下拉**加一项：

```html
            <ElOption label="其他" value="其他" />
```

**标签颜色**：

```ts
  const getEnergyTypeTag = (
    type: string
  ): 'success' | 'primary' | 'warning' | 'info' => {
    const map: Record<string, 'success' | 'primary' | 'warning' | 'info'> = {
      自来水: 'primary',
      电: 'warning',
      燃气: 'success',
      // 新增
      其他: 'info'
    }
    return map[type] || 'primary'
  }
```

**汇总卡**：不新增第 5 张卡（会破坏 4 列布局），在「总金额」卡里补一行「其中其他」：

```html
            <div class="summary-info">
              <div class="summary-label">总金额（元）</div>
              <div class="summary-value">¥ {{ formatAmount(summary.totalAmount) }}</div>
              <!-- 新增 -->
              <div v-if="summary.byType['其他']" class="summary-extra">
                其中其他成本 ¥ {{ formatAmount(summary.byType['其他']) }}
              </div>
            </div>
```

样式追加（`src/views/energy/index.vue` 底部 `<style lang="scss" scoped>` 内，`.summary-value` 规则之后）：

```scss
  .summary-extra {
    margin-top: 4px;
    font-size: 12px;
    color: #6b7787;
  }
```

### 7.3 `src/views/energy/modules/energy-detail.vue`

```ts
  const getEnergyTypeTag = (
    type?: string
  ): 'success' | 'primary' | 'warning' | 'info' => {
    const map: Record<string, 'success' | 'primary' | 'warning' | 'info'> = {
      自来水: 'primary',
      电: 'warning',
      燃气: 'success',
      其他: 'info'
    }
    return map[type || ''] || 'primary'
  }
```

> 「其他」没有表底，`lastMeterReading/currentMeterReading/actualUsage` 为 `null`，详情页已按 `?.toLocaleString() || '-'` 显示 `-`，无需改模板。

---

## 8. P1：明细树 `MonthDrilldown.vue`

### 8.1 月份行加「成本节约额」

```html
          <span class="num">{{ wan(month.actualCost) }}<small>实际成本（万）</small></span>
          <!-- 新增 -->
          <span class="num">{{ wan(month.costVariance) }}<small>成本节约额（万）</small></span>
          <span class="num">{{ fmtQty(month.deliveryQuantity) }}<small>交付量</small></span>
```

对应样式：

```scss
  .month-row__summary {
    // 原 4 个数字列 → 5 个
    grid-template-columns: 20px 130px repeat(5, 1fr);
  }
```

### 8.2 制剂行加「成本节约率」

```html
              <span class="num">{{ fmtMoney(prep.actualCost) }}</span>
              <!-- 新增 -->
              <span class="num">{{ fmtPercent(prep.costVarianceRate) }}</span>
              <span class="num unit">{{ fmtQty(prep.deliveryQuantity) }} 件</span>
```

```scss
  .prep-row__summary {
    // 原 3 个数字列 → 4 个
    grid-template-columns: 20px 200px repeat(4, 1fr) 86px;
  }
```

同时把 `alert-row` 的 grid 也改为 `20px 200px repeat(4, 1fr) 86px`，否则伪节点会错位。伪节点对应位置补：

```html
            <span class="num">—</span>
```

### 8.3 批次表新增列

在「批量」之后插入单位成本 4 列：

```html
                <ElTableColumn label="单位预计成本" align="right" width="112">
                  <template #default="{ row }">{{ fmtMoney(row.unitEstimatedCost) }}</template>
                </ElTableColumn>
                <ElTableColumn label="单位实际成本" align="right" width="112">
                  <template #default="{ row }">{{ fmtMoney(row.unitActualCost) }}</template>
                </ElTableColumn>
                <ElTableColumn label="单位成本节约额" align="right" width="124">
                  <template #default="{ row }">{{ fmtMoney(row.unitCostVariance) }}</template>
                </ElTableColumn>
                <ElTableColumn label="单位成本节约率" align="right" width="124">
                  <template #default="{ row }">{{ fmtPercent(row.unitCostVarianceRate) }}</template>
                </ElTableColumn>
```

在「物料成本」之后插入原辅包拆分：

```html
                <ElTableColumn label="原料" align="right" width="92">
                  <template #default="{ row }">{{ fmtMoney(row.rawMaterialAmount) }}</template>
                </ElTableColumn>
                <ElTableColumn label="辅料" align="right" width="92">
                  <template #default="{ row }">{{ fmtMoney(row.auxiliaryMaterialAmount) }}</template>
                </ElTableColumn>
                <ElTableColumn label="包材" align="right" width="92">
                  <template #default="{ row }">{{ fmtMoney(row.packagingAmount) }}</template>
                </ElTableColumn>
```

在「水电气」之后插入「其他」：

```html
                <ElTableColumn label="其他" align="right" width="92">
                  <template #default="{ row }">{{ fmtMoney(row.otherAmount) }}</template>
                </ElTableColumn>
```

在「实际成本」之后插入周期 3 列：

```html
                <ElTableColumn label="采购下达" min-width="150">
                  <template #default="{ row }">{{ fmtText(row.purchaseDate) }}</template>
                </ElTableColumn>
                <ElTableColumn label="成品入库" min-width="150">
                  <template #default="{ row }">{{ fmtText(row.inboundDate) }}</template>
                </ElTableColumn>
                <ElTableColumn label="生产天数" align="right" width="96">
                  <template #default="{ row }">
                    {{ isNil(row.productionDays) ? '—' : `${fmtInt(row.productionDays)} 天` }}
                  </template>
                </ElTableColumn>
```

import 增加 `fmtPercent`：

```ts
  import { fmtInt, fmtMoney, fmtPercent, fmtQty, fmtText, isNil } from '../utils/format'
```

> 列比较多时表格会自动横向滚动；如果嫌宽，可先只保留「单位成本节约率 / 其他 / 生产天数」三列，其余后续再加。

---

## 9. 后端字段 → 前端位置速查

| 新字段 / 节点 | 前端落点 |
|--------------|---------|
| `actualCost.otherAmount` | 实际成本卡构成条、实际成本抽屉「其他」列 |
| `costTrend.costVariance / costVarianceRate` | 趋势图 tooltip / 明细树（P1） |
| `costStructure.rawMaterial/auxiliary/packaging/other` | 7 段成本构成图 |
| `costStructureByPreparation` | 「品种成本构成」新图 |
| `batchCostVarianceRank` | 「批次成本节约排行」新图 |
| `production.balance` | 首页「生产平衡」条（按批） |
| `unitEstimatedCost/unitActualCost/unitCostVariance/unitCostVarianceRate` | 抽屉/批次表（P1） |
| `rawMaterialAmount/auxiliaryMaterialAmount/packagingAmount/otherAmount` | 抽屉/批次表（P1） |
| `purchaseDate/inboundDate/productionDays` | 抽屉/批次表（P1） |

---

## 10. 联调自测清单

- [ ] 实际成本卡：`实际成本 = 物料 + 水 + 电 + 气 + 其他`，构成条五段宽度之和 100%
- [ ] 实际成本抽屉：`原料 + 辅料 + 包材 = 物料合计`（逐行）
- [ ] 7 段成本构成图：各月 `原料+辅料+包材+水+电+气+其他` = 卡片实际成本
- [ ] 「品种成本构成」合计 = 卡片实际成本，伪品种未被过滤
- [ ] 「批次成本节约排行」无 `预估成本 = 0` 的行，正负颜色区分（正=节约绿，负=超支红），生产天数为 `null` 时显示 `—`
- [ ] 生产平衡：`已全部交付批数 + 待生产批数 = 计划批数`，两段合计 100%
- [ ] 能耗录入选「其他」：表底输入隐藏、单位=元、总价必填；保存后看板实际成本与其他成本增加
- [ ] 能耗列表/详情「其他」标签为灰色 info，搜索下拉可选「其他」
- [ ] 修改制剂预计成本价后，历史月份预估成本不变（快照生效）
- [ ] `null` 字段全部显示 `—`，没有变成 0

---

## 11. 上线顺序与回滚

1. **后端先发**：`8471a17` 已推送，接口向后兼容（只加字段）。
2. **DDL 已完成**：生产库 `work_order.estimated_cost_price_snapshot` 已加列，脚本在 `src/main/resources/Data/migration/add_work_order_estimated_cost_price_snapshot.sql`。
3. **前端后发**：本指南改动全部是「读新字段」，旧接口环境下会自动回退（7 段图回退 4 段、平衡条回退按件），可以先发前端再接后端。
4. **回滚**：前端回滚到上一版本即可；后端字段保留不影响旧页面。生产库列可回滚：
   `ALTER TABLE work_order DROP COLUMN estimated_cost_price_snapshot;`（该列当前全 NULL）。

---

## 12. 已知数据问题（不是前端 Bug）

1. **生产库预计成本价未维护**：`preparation` 28 条中预计成本价为 0 的有 28 条，5 张活跃工单快照全 `NULL`，因此生产环境「预估成本」会显示 0，「成本节约率」为 `—`。需业务侧先维护制剂预计成本价。
2. **历史批次 `productionDays` 多为 `null`**：老采购单缺 `production_plan_id` / `production_plan_code`，无法关联生产计划；仅 `Plan202610150001` 能匹配。前端显示 `—` 即可，不要当 0 天。
3. **生产库 `energy_record` 为 0 行**：因此生产环境「其他成本」为 0，需业务录入后才可见。
4. **测试库数据量小**：成本节约率接近 100% 是出库数据少导致的，不代表算法异常。

# 产品信息汇总：时间字段外显修改计划

> 状态：已确认并实施。  
> 目标：时间筛选有“计划/工单/入库/检验”四种口径，但列表、卡片、抽屉没有把命中日期显示出来。需要让用户一眼看到“这条记录按哪个时间被筛出来”。

---

## 1. 当前问题

页面有日期筛选：

```text
日期口径：计划日期 / 工单日期 / 入库日期 / 检验日期
开始日期 ~ 结束日期
```

但：

- 批次清单没有时间列，用户看不出为什么这条记录出现在结果里。
- 品种卡片只在副标题显示计划日期，没有显示当前筛选口径的日期。
- 批次详情抽屉顶部没有日期信息，日期只散落在链路节点里。
- 后端 `BatchSummary` 目前只有：
  - `planDate`
  - `productionStart`
  - `productionEnd`
- 没有 `workOrderDate`、`inboundDate`、`outboundDate`、`inspectionDate`，前端即使想显示也拿不到。

---

## 2. 推荐展示方式

### 2.1 批次清单：增加一个“动态时间”列

不新增四列，避免表格过宽。新增一列：

```text
时间（计划） / 时间（工单） / 时间（入库） / 时间（检验）
```

列名随当前 `dateType` 切换。

行内容：

```text
2026-09-16
计划日期
```

如果当前口径没有日期：

```text
—
无入库日期
```

同时可以用 `title` 或副行展示计划日期，便于对照：

```text
2026-09-16
计划 09-16 · 检验 10-19
```

推荐规则：

- 主行：当前筛选口径的日期。
- 副行：日期口径名称；没有日期时显示“无该日期”。
- `title`：可附带计划/工单/入库/检验全部日期，避免表格太挤。

### 2.2 品种卡片：增加一行“时间”

在卡片现有 `计划 PlanNumber · 计划日期` 行附近增加：

```text
时间口径：计划日期 2026-09-16
```

或直接显示：

```text
计划 2026-09-16 · 工单 2026-09-16 · 入库 2026-09-25 · 检验 2026-10-19
```

推荐只显示当前口径 + 计划日期，其他日期放入 `title` 或详情抽屉，避免卡片过高。

### 2.3 批次详情抽屉：增加日期网格

在抽屉顶部 KPI 下方增加一组日期：

```text
计划日期    2026-09-16
工单日期    2026-09-16
入库日期    2026-09-25
出库日期    2026-09-26
检验日期    2026-10-19
```

当前筛选口径的日期加粗或高亮。

### 2.4 筛选区：增加“当前日期口径”提示

在批次清单标题旁显示：

```text
日期口径：计划日期 2026-09-01 ~ 2026-10-31
```

没有设置开始/结束日期时显示：

```text
日期口径：计划日期（未限制范围）
```

这样用户知道列表为什么按这个日期筛选。

---

## 3. 后端修改

### 3.1 扩展 `BatchSummary`

新增字段：

```java
/** 工单日期 */
private LocalDate workOrderDate;

/** 入库日期 */
private LocalDate inboundDate;

/** 出库日期 */
private LocalDate outboundDate;

/** 检验日期（最新检验结束日期） */
private LocalDate inspectionDate;
```

保留现有：

```java
private LocalDate planDate;
private LocalDate productionStart;
private LocalDate productionEnd;
```

### 3.2 填充规则

在 `toSummary(BatchContext ctx)` 中填充：

| 字段 | 取值 |
|---|---|
| `planDate` | 现有 `planDate(ctx)` |
| `workOrderDate` | `date(wo.getConfigDate())` |
| `inboundDate` | 第一张入库单日期，与筛选口径一致 |
| `outboundDate` | 第一张成品出库单日期 |
| `inspectionDate` | 第一条检验记录结束日期，与筛选口径一致 |
| `productionStart` / `productionEnd` | 保持现有 |

未下达计划 `toPlanSummary`：

- `planDate` 有值。
- `workOrderDate` / `inboundDate` / `outboundDate` / `inspectionDate` 为 `null`。

### 3.3 详情接口

`GET /api/product-summary/batches/{workOrderId}` 和 `/plans/{planId}` 返回的 `batch` 已复用 `BatchSummary`，会自动带上新日期字段，不需要新增接口。

### 3.4 集中导出

在 `文件清单.csv` 增加日期列：

```text
计划日期, 工单日期, 入库日期, 检验日期
```

ZIP 目录结构不变，只在清单中补日期。  
如果后续需要按日期建目录，可再加，不在本次范围。

---

## 4. HTML 修改点

文件：`_analysis/prototypes/product-summary.html`

1. 表头
   - 在“工单 / 批号”后增加一列“时间”，宽度约 11%。
   - 列名使用当前 `ui.dateType` 动态生成。
2. 表格行
   - 新增 `<td>` 显示选中日期 + 口径名称。
   - 无日期显示 `—` 和“无该日期”。
3. 品种卡片
   - 在 `wo-meta` 或新增 `.wo-date` 显示当前口径日期。
4. 抽屉
   - 在 `dr-kpis` 下方增加日期网格。
5. 清单标题
   - 显示当前日期口径和范围。
6. CSS
   - 增加 `.date-cell`、`.date-sub`、`.date-grid` 等样式。

---

## 5. 前端对接影响

`src/api/productSummary.ts` 的 `ProductSummaryBatch` 类型增加：

```ts
workOrderDate?: string
inboundDate?: string
outboundDate?: string
inspectionDate?: string
```

表格列建议：

```ts
{
  prop: 'activeDate',
  label: `时间（${dateTypeLabel}）`,
  minWidth: 130,
  formatter: (row) => {
    const value = row[dateFieldMap[dateType]]
    return value ? `${value}\n${dateTypeLabel}` : `—\n无${dateTypeLabel}`
  }
}
```

`activeDate` 可以是前端计算字段，不需要后端返回。

---

## 6. 需要确认

1. 批次清单采用“一列动态时间”，还是拆成四个日期列？建议一列动态。
2. 品种卡片只显示当前口径日期，还是显示全部日期？建议当前口径 + 计划日期。
3. 入库/检验日期取第一张单据还是最新一张？建议与现有筛选 `rowDate` 保持一致（第一张）。
4. 导出清单是否补四列日期？建议补。
5. 抽屉是否需要对当前筛选口径高亮？建议高亮。

---

## 7. 实施顺序

1. 扩展 `ProductSummaryDtos.BatchSummary` 日期字段。
2. `ProductSummaryServiceImpl.toSummary` / `toPlanSummary` 填充。
3. 导出清单增加日期列。
4. 修改 HTML 表头、行、卡片、抽屉、筛选提示。
5. 编译 + 页面验证；按你的要求不跑 smoke tests。

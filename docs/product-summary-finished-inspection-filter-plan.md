# 产品信息汇总：检验只取成品检验修改方案

> 状态：已确认；本次只改后端，HTML 不改。  
> 问题：产品信息汇总的“检验”列会显示中间产品检验的结论；没有成品检验时仍显示“合格”，应显示“暂无检验”。

---

## 1. 根因

当前后端和 HTML 都直接取工单下的第一条检验记录：

```java
ctx.getInspections().get(0)
```

但一个批次下可能有：

- 中间产品检验
- 生产原料检验
- 成品检验

没有按 `inspection_record.item_category` 过滤，所以中间产品检验可能排在第一条，导致：

- `inspectionConclusion` 显示中间产品的“合格”
- `inspectionDate` / `latestInspectionCode` 取到中间产品记录
- KPI 检验合格率把中间产品也算进去
- HTML 的 `latestInspection(row)` 同样取 `row.wo.inspections[0]`
- 详情链路和导出清单的检验日期也受影响

---

## 2. 目标口径

产品信息汇总的“检验”只认：

```text
inspection_record.item_category = '成品'
```

规则：

- 有成品检验：显示成品检验结论、成品检验编号、成品检验日期。
- 没有成品检验：显示“暂无检验”，不显示中间产品/原料检验结论。
- 中间产品检验仍保留在检验管理页，不从数据库删除。
- 检验合格率只统计成品检验。
- 检验日期筛选只按成品检验日期过滤。

---

## 3. 后端修改

### 3.1 新增成品检验过滤

在 `ProductSummaryServiceImpl` 增加：

```java
private List<InspectionRecord> finishedInspections(BatchContext ctx) {
    return ctx.getInspections().stream()
            .filter(item -> "成品".equals(item.getItemCategory()))
            .collect(Collectors.toList());
}

private InspectionRecord latestFinishedInspection(BatchContext ctx) {
    List<InspectionRecord> list = finishedInspections(ctx);
    return list.isEmpty() ? null : list.get(0);
}
```

> 现有记录如果 `itemCategory` 为空，不按成品处理，避免把历史中间品记录误判为成品。

### 3.2 替换取数位置

以下位置从 `ctx.getInspections()` 改为 `finishedInspections(ctx)` / `latestFinishedInspection(ctx)`：

| 位置 | 修改 |
|---|---|
| `toSummary` 的 `inspectionConclusion` | 只取成品结论；无成品返回 `null` |
| `toSummary` 的 `latestInspectionCode` | 只取成品检验编号 |
| `toSummary` 的 `inspectionDate` | 只取成品检验结束日期 |
| `stageStates` 的检验环节 | 无成品检验时 `pending`，不因中间品合格显示 `done` |
| `stages` 的检验节点 | 主信息和结论只取成品；无成品显示“暂无检验” |
| `rowDate(..., "inspectionDate")` | 只按成品检验结束日期筛选 |
| `getOverview` 的 `inspectionTotal` / `inspectionQualified` / `inspectionRate` | 只统计成品检验 |
| 导出清单的 `检验日期` | 只填成品检验日期 |

### 3.3 详情链路

推荐：

- 摘要、列表、卡片、KPI、检验日期：**只显示成品检验**。
- 详情抽屉的“检验”节点：可以保留全部检验记录，但每条加 `被检品属性` 列，避免丢失中间产品/原料检验证据。
- 如果产品明确要求详情也只显示成品，则详情也只查 `itemCategory=成品`。

### 3.4 返回字段

无成品检验时：

```json
{
  "inspectionConclusion": null,
  "latestInspectionCode": null,
  "inspectionDate": null
}
```

前端统一显示：

```text
暂无检验
—
```

---

## 4. HTML 修改

文件：`_analysis/prototypes/product-summary.html`

新增：

```js
function finishedInspections(row) {
  const list = row.wo ? (row.wo.inspections || []) : [];
  return list.filter(i => i.itemCategory === '成品');
}

function latestInspection(row) {
  const list = finishedInspections(row);
  return list.length ? list[0] : null;
}
```

替换以下位置：

| 位置 | 修改 |
|---|---|
| `latestInspection(row)` | 只取成品 |
| `rowDate(row, 'inspectionDate')` | 只取成品 |
| `rowStatus(row)` | 判断异常时只取成品；中间品异常是否影响批次状态需单独确认 |
| `renderKpis` 的检验合格率 | 只统计成品 |
| `varietyStats` 的合格率 | 只统计成品 |
| `stageInspectionBody` | 成品列表；中间品可单独分组 |
| `renderDrawer` 检验结论 | 只取成品 |
| `stageDefs` 的检验节点 | 只取成品主信息 |
| `inspectionFilesAll` | 可保留全部附件；不影响结论 |

### 4.1 模拟数据需补字段

当前 `SNAPSHOT` 里的 `inspections` 没有 `itemCategory`，过滤后会全部变成“暂无检验”。需要：

- 重新生成 `real-summary-data.json` / HTML 快照时补 `itemCategory`；
- 或在原型数据中给每条检验记录补 `itemCategory: '成品' / '中间产品' / '生产原料'`。

否则原型无法验证成品过滤效果。

---

## 5. 前端 API 对接影响

`src/api/productSummary.ts` 不需要新增字段，继续使用：

```ts
inspectionConclusion?: string | null
latestInspectionCode?: string | null
inspectionDate?: string | null
```

前端展示：

```ts
const conclusion = row.inspectionConclusion || '暂无检验'
```

如果有中间产品检验但没有成品检验，后端返回 `null`，列表显示“暂无检验”。

---

## 6. 验证清单

- [ ] 只有中间产品检验的批次：检验列显示“暂无检验”，不显示合格。
- [ ] 只有原料检验的批次：显示“暂无检验”。
- [ ] 有成品检验的批次：显示成品检验结论和成品检验编号。
- [ ] 多个成品检验：按现有顺序取第一条，或明确按结束时间取最新。
- [ ] 检验合格率只统计成品检验。
- [ ] 检验日期筛选只命中成品检验日期。
- [ ] 导出清单检验日期只填成品检验日期。
- [ ] 详情链路保留中间品/原料检验时，带被检品属性标识。
- [ ] 编译通过；按当前约定不运行 smoke tests。

---

## 7. 需要确认

1. 详情抽屉的检验节点：保留全部检验并标注属性，还是也只显示成品？建议保留全部并标注，摘要只显示成品。
2. 中间产品“不合格”是否影响批次状态为“异常”？建议仍影响，但检验列显示“暂无检验”。
3. 多个成品检验取第一条还是最新一条？建议按现有顺序取第一条，保持与当前接口一致；如需最新，按 `endTime` 倒序。

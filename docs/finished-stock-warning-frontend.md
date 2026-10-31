# 成品库存预警 前端对接与运维说明

> 所属模块：成品出库台账 → 成品库存预警
> 涉及接口：`/api/finished-stock-warning/list`、`/api/finished-stock-warning/summary`、`/api/dashboard/todos`
> 统计口径：前 N 个完整自然月（不含当月）的成品已出库台账数量

---

## 1. 功能说明

成品库存预警用于回答两个问题：

1. 某个成品品种平均每月出库多少？
2. 当前可用库存还能支撑几个月？低于预警月数时提醒生产。

页面展示字段：

| 字段 | 说明 |
|---|---|
| 品种编码 / 品种名称 | 制剂主数据，兼容台账/库存中存在的历史品种 |
| 月平均出库量 | 自动计算：统计窗口内已出库数量合计 ÷ 统计月数 |
| 可用库存数 | 成品合格库存；可选择剔除近效期批次 |
| 可支撑时间 | 可用库存 ÷ 月平均出库量（月，保留 2 位小数） |
| 预警状态 | 危险 / 预警 / 正常 / 无出库 |
| 合格库存 | 状态为“合格”的成品库存，是可用库存的计算基数 |
| 近效期库存 | 合格库存中被剔除的近效期数量，便于核对 |
| 总库存 | 全部成品库存（含待检、不合格等状态） |

---

## 2. 统计口径

### 2.1 时间窗口

- 统计月数 `N` 读取系统配置 `dashboard.finishedStock.avgMonths`，默认 3。
- 统计窗口为**前 N 个完整自然月，不含当月**。
- 示例：今天 `2026-10-31`，N=3 → 统计 `2026-07-01 00:00:00` 至 `2026-10-01 00:00:00`，即 2026-07、2026-08、2026-09 三个月。

### 2.2 月平均出库量

```text
月平均出库量 = 统计窗口内 sales_order.quantity 合计 ÷ N
```

取数条件：

- `sales_order.status = '已出库'`
- `sales_order.is_deleted = 0`
- `sales_order_date` 在统计窗口内
- 按 `preparation_code` 汇总

> 已开单未出库、已作废、已取消的记录不计入。出库取消时台账状态会回退为“已开单”，不会继续计入月均。

### 2.3 可用库存

成品库存识别规则（兼容历史数据）：

```sql
stock.is_deleted = 0
AND stock.quantity > 0
AND (stock.category_name = '成品' OR stock.item_type = 'preparation')
```

- `totalStock`：全部状态的成品库存（含待检、不合格）。
- `qualifiedStock`：仅状态为“合格”或状态为空的成品库存。
- 近效期、已过期库存只在合格库存范围内统计。

近效期定义：

```text
expiry_date <= 今天 + 近效期天数
```

近效期天数读取 `dashboard.finishedStock.nearExpiryDays`，默认 90 天；已过期批次也属于近效期。

```text
可用库存 = 合格库存 -（勾选剔除近效期时）近效期库存
```

### 2.4 可支撑时间与预警

```text
可支撑时间 = 可用库存 ÷ 月平均出库量
预警 = 月平均出库量 > 0 且 可支撑时间 < 预警月数
```

预警月数读取 `dashboard.finishedStock.warningMonths`，默认 3。

预警级别：

| 级别 | 条件 | 页面建议 |
|---|---|---|
| `danger` | 可支撑时间 < 1 个月 | 红色 |
| `warning` | 可支撑时间 < 预警月数 | 橙色 |
| `normal` | 有出库且未触发预警 | 绿色 |
| `noDemand` | 月平均出库量 = 0 | 灰色，显示“无出库” |

---

## 3. 系统配置项

配置页按“库存管理”分组展示，无需新增页面代码：

| 配置键 | 默认值 | 配置名称 | 说明 |
|---|---:|---|---|
| `dashboard.finishedStock.avgMonths` | 3 | 成品月均出库统计月数 | 自动计算月均值的回溯完整自然月数 |
| `dashboard.finishedStock.warningMonths` | 3 | 成品库存预警月数 | 可支撑时间低于该值时提醒生产 |
| `dashboard.finishedStock.nearExpiryDays` | 90 | 成品近效期天数 | 有效期在该天数内的批次视为近效期 |

现有配置接口：

```http
GET /api/system/config?group=库存
PUT /api/system/config/dashboard.finishedStock.avgMonths
Content-Type: application/json

{ "configValue": "5" }
```

---

## 4. 接口说明

### 4.1 分页查询预警列表

```http
GET /api/finished-stock-warning/list?pageIndex=0&pageSize=20&keyword=&onlyWarning=false&excludeNearExpiry=true
```

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `keyword` | string | 空 | 品种编码/名称模糊匹配 |
| `onlyWarning` | boolean | false | 只看触发预警的品种 |
| `excludeNearExpiry` | boolean | true | 是否剔除近效期库存 |
| `pageIndex` | int | 0 | 页码，从 0 开始 |
| `pageSize` | int | 20 | 每页大小 |

响应示例：

```jsonc
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "preparationId": 1,
        "preparationCode": "Z01-001",
        "preparationName": "壮骨关节丸",
        "spec": "每丸重9g",
        "unitName": "盒",
        "totalStock": 500.00,
        "qualifiedStock": 500.00,
        "availableStock": 300.00,
        "nearExpiryStock": 200.00,
        "expiredStock": 0.00,
        "totalOutbound": 360.00,
        "autoMonthlyAvg": 120.00,
        "supportMonths": 2.50,
        "warningMonths": 3,
        "warning": true,
        "warningLevel": "warning",
        "avgMonths": 3,
        "avgStartMonth": "2026-07",
        "avgEndMonth": "2026-09",
        "outboundMonths": [
          { "month": "2026-07", "quantity": 100.00 },
          { "month": "2026-08", "quantity": 150.00 },
          { "month": "2026-09", "quantity": 110.00 }
        ]
      }
    ],
    "totalCount": 1,
    "pageIndex": 0,
    "pageSize": 20
  }
}
```

### 4.2 查询汇总统计

```http
GET /api/finished-stock-warning/summary?excludeNearExpiry=true
```

响应示例：

```jsonc
{
  "code": 200,
  "data": {
    "totalProducts": 373,
    "warningCount": 12,
    "dangerCount": 3,
    "normalCount": 80,
    "noDemandCount": 280,
    "noStockCount": 5,
    "avgMonths": 3,
    "warningMonths": 3,
    "nearExpiryDays": 90,
    "avgStartMonth": "2026-07",
    "avgEndMonth": "2026-09",
    "excludeNearExpiry": true
  }
}
```

> `dangerCount` 是 `warningCount` 的子集；`normalCount`、`noDemandCount` 为互斥状态。

### 4.3 首页待办

`GET /api/dashboard/todos` 新增类型 `成品库存预警`：

- `items[].todoType = "成品库存预警"`
- `items[].sourceModule = "库存管理"`
- `items[].link = "/warehouse_management/finished-stock-warning"`
- `typeCounts["成品库存预警"] = 预警数量`

内容示例：

```text
壮骨关节丸(Z01-001) 月均出库120，可用库存300，仅可支撑2.5个月（阈值3个月），请安排生产
```

---

## 5. 页面实现建议

### 5.1 顶部统计区

调用 `/summary`，展示：

- 品种总数
- 预警数量（可点击筛选 `onlyWarning=true`）
- 危险数量
- 统计区间：`avgStartMonth ~ avgEndMonth`
- 统计月数、预警月数、近效期天数
- “剔除近效期库存”复选框（默认勾选，切换后同时刷新 summary 和 list）

### 5.2 表格

推荐列：

1. 品种编码
2. 品种名称
3. 规格
4. 单位
5. 月平均出库量
6. 可用库存数
7. 可支撑时间（月）
8. 预警状态
9. 合格库存
10. 近效期库存
11. 总库存

交互：

- 默认排序：危险 → 预警 → 正常 → 无出库；同级别按可支撑时间升序。
- `supportMonths` 为 `null` 时显示 `—`，不要当 0 处理。
- `warningLevel = noDemand` 显示“无出库”，不要标红。
- 行展开可展示 `outboundMonths` 的月度出库趋势。
- 顶部提示：“统计月数/预警月数可在 系统配置 → 库存管理 中修改”。

### 5.3 空态

| 场景 | 展示 |
|---|---|
| 无任何品种 | “暂无成品品种” |
| 无预警品种 | “当前没有库存不足的成品品种” |
| 月均出库为 0 | 可支撑时间显示 `—`，状态“无出库” |
| 可用库存为 0 且有出库 | 可支撑时间 `0.00`，状态“危险” |

---

## 6. 部署与验证

### 6.1 执行配置项 SQL

在目标数据库执行：

```sql
source src/main/resources/Data/migration/add_finished_stock_warning_config.sql;
```

或直接执行文件内容。`INSERT IGNORE` 幂等，不会覆盖已修改的配置。

### 6.2 验证接口

```http
GET /api/system/config?group=库存
GET /api/finished-stock-warning/summary
GET /api/finished-stock-warning/list?pageIndex=0&pageSize=20
GET /api/finished-stock-warning/list?onlyWarning=true&excludeNearExpiry=true
GET /api/dashboard/todos
```

### 6.3 菜单权限

如果前端把“成品库存预警”作为**成品出库台账页面的标签页**，不需要新增菜单权限记录。

如果需要作为**独立菜单**，需在权限表 `permission` 新增一条菜单记录，例如：

- `perm_key`：`finished-product-stock-warning`
- `perm_name`：`成品库存预警`
- `perm_type`：`menu`
- `parent_id`：`7`（生产管理）或 `293`（成品出库台账）
- `display_order`：按实际菜单位置设置
- `perm_status`：`true`（启用）

同时给相应角色授权后，前端菜单接口 `/api/system/menus/simple` 才会返回该节点。

### 6.4 自测清单

- [ ] 配置页出现 3 个新增配置项
- [ ] 修改统计月数后，月均出库量与统计区间同步变化
- [ ] 修改预警月数后，预警数量同步变化
- [ ] 切换“剔除近效期库存”后，可用库存与可支撑时间变化
- [ ] `可支撑时间 = 可用库存 ÷ 月平均出库量`，保留 2 位小数
- [ ] 月均为 0 的品种不预警
- [ ] 首页待办出现“成品库存预警”类型，数量正确
- [ ] 取消成品出库后，台账状态回退，月均出库量不再包含该笔

---

## 7. 注意事项

1. 统计只取台账状态为“已出库”的记录，已开单未出库不计入。
2. 当前按毛出库量统计，不扣减退库；如需净出库量需后续扩展。
3. 成品库存以 `category_name='成品'` 为主，同时兼容 `item_type='preparation'`。
4. `totalStock` 包含待检、不合格等全部状态；`availableStock` 只按合格库存计算，因此总库存可能大于可用库存。
5. 统计月数为正整数，配置为 0 或负数时回退默认值 3。
6. 近效期天数允许为 0（只剔除已过期批次），负数回退默认值 90。

# 前端对接：销毁记录类型并入 + 生产任务关联 + 报损出库选成品

> 对应后端改动日期：2026-11-26
> 影响页面：销毁记录（列表/新增/编辑/筛选）、出库管理-报损出库

---

## 一、变更摘要

| 模块 | 变化 |
|---|---|
| 销毁记录 | 原辅包/中间品/成品销毁统一使用「不合格品销毁」类型（`BPXH-` 单号）；「物料销毁」类型已并入，**不再允许新建**，历史 `WLXH-` 单据仍可查询与修改 |
| 销毁记录 | 「不合格品销毁」明细改为必须携带 `itemCategory`（品名类别）与 `unitName`（计量单位），数量必须大于 0 |
| 销毁记录 | 主表新增可选关联生产任务（`workOrderId`），保存时服务端快照任务编号与名称；新增生产任务下拉接口 |
| 出库管理-报损出库 | 支持选择成品批次出库；后端自动按库存批次判定物品类型，无需前端传 `itemType` |

---

## 二、销毁记录

### 2.1 记录类型口径

| 显示名 | `recordType` 值 | 单号前缀 | 说明 |
|---|---|---|---|
| 不合格品销毁 | `不合格品销毁` | `BPXH-` | 原辅包/中间品/成品销毁均使用此类型 |
| 留样及稳定性考察产品销毁 | `留样及稳定性考察产品销毁` | `LYXH-` | 维持原逻辑，关联留样反写 |
| 物料销毁（历史） | `物料销毁` | `WLXH-` | 仅历史数据可查/可改，**新增时后端会拒绝** |

新增时提交 `recordType = 物料销毁` 会返回：

```
「物料销毁」类型已并入「不合格品销毁」，请使用不合格品销毁登记
```

### 2.2 品名类别与单位

原辅包/中间品/成品销毁的明细必须携带：

| 字段 | 必填 | 说明 |
|---|---|---|
| `itemCategory` | 是 | 只能提交 `生产原料` / `中间产品` / `成品` 三个值 |
| `unitName` | 是 | 计量单位（kg/g/L/袋/盒/瓶等），≤20 字符 |
| `quantity` | 是 | 必须大于 0 |

页面显示建议做映射，提交值仍用左侧枚举：

| 页面显示 | 提交值 |
|---|---|
| 原辅包 | `生产原料` |
| 中间品 | `中间产品` |
| 成品 | `成品` |

> 历史「不合格品销毁」单据可能缺少品名类别与单位，编辑时需要前端补齐后再保存，否则会返回「不合格品销毁明细品名类别必填」等提示。

### 2.3 新增 / 修改请求

新增：`POST /api/destruction`
修改：`PUT /api/destruction/{id}`（明细整体重建，必须回传全部明细字段）

```json
{
  "recordType": "不合格品销毁",
  "workOrderId": 12,
  "destroyReason": "过期报废",
  "destroyMethod": "交有资质单位高温焚烧",
  "destroyLocation": "厂区危废暂存间",
  "applicantName": "李四",
  "applyDate": "2026-11-24",
  "approverName": "王五",
  "approveDate": "2026-11-24",
  "destroyerName": "赵六",
  "destroyDate": "2026-11-25",
  "supervisorName": "钱七",
  "superviseDate": "2026-11-25",
  "details": [
    { "materialName": "维生素C", "spec": "食品级", "batchNo": "20260701", "quantity": 50.5,
      "itemCategory": "生产原料", "unitName": "kg" },
    { "materialName": "复方制剂A", "spec": "10ml/支", "batchNo": "20260615", "quantity": 30,
      "itemCategory": "中间产品", "unitName": "L" },
    { "materialName": "感冒灵胶囊", "spec": "0.5g/粒", "batchNo": "20260610", "quantity": 10,
      "itemCategory": "成品", "unitName": "盒" }
  ],
  "attachmentFileIds": [201]
}
```

**关联生产任务说明**：

- `workOrderId` 非必填，**不限制生产任务状态**（已完成、生产中、待生产等均可选）。
- 只传 `workOrderId` 即可，`workOrderCode` / `workOrderName` 由服务端按任务表快照写入，前端不要提交。
- 修改时 `workOrderId` 传 `null` 表示取消关联，服务端会同时清空任务编号与名称快照。
- 关联不存在的任务会返回：`关联生产任务不存在：{id}`。

**校验提示**（直接弹出后端 message 即可）：

| 后端提示 | 触发场景 |
|---|---|
| `不合格品销毁明细数量必须大于0: X` | 数量为空或 ≤ 0 |
| `不合格品销毁明细品名类别必填: X` | 未选品名类别 |
| `品名类别只能是生产原料/中间产品/成品: X` | 类别值非法 |
| `不合格品销毁明细计量单位必填: X` | 未填单位 |
| `「物料销毁」类型已并入「不合格品销毁」，请使用不合格品销毁登记` | 新增时提交了物料销毁类型 |

### 2.4 列表 / 详情返回

主表新增返回字段（列表与详情均返回）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `workOrderId` | number \| null | 关联生产任务ID |
| `workOrderCode` | string \| null | 生产任务编号快照 |
| `workOrderName` | string \| null | 生产任务名称快照（列表建议展示） |

列表查询参数不变：

```
GET /api/destruction?recordType=不合格品销毁&itemCategory=成品&keyword=BPXH&batchNo=20260701&status=正常&startDate=2026-10-01&endDate=2026-10-12&pageIndex=0&pageSize=10
```

### 2.5 生产任务下拉接口（新增）

```
GET /api/destruction/work-orders?keyword=SC2026
```

- 不限制任务状态；
- 关键字模糊匹配生产任务编号 / 生产任务名称 / 制剂名称 / 批号；
- 按创建时间倒序返回最多 20 条。

返回示例：

```json
{
  "code": 200,
  "data": [
    {
      "workOrderId": 12,
      "workOrderCode": "GD202611260001",
      "workOrderName": "活血通络丸7000盒",
      "preparationName": "活血通络丸",
      "batchNumber": "261101"
    }
  ]
}
```

下拉选中后只回传 `workOrderId`。

### 2.6 单号生成

```
GET /api/destruction/generateCode?recordType=不合格品销毁
```

新业务只会生成 `BPXH-` / `LYXH-` 两种单号；`WLXH-` 仅历史数据保留。

---

## 三、出库管理 - 报损出库选成品

### 3.1 后端能力

- 成品库存就在统一的 `stock` 表：识别规则为 `category_name = '成品'` 或 `item_type = 'preparation'`。
- 出库扣减按明细 `stockId` 定位库存批次，不区分物料/成品，报损出库可以直接扣减成品批次。
- 后端会自动按库存批次判定明细 `itemType`（成品 → `preparation`），**前端无需传 `itemType`**，传了也会以库存批次为准。
- 报损出库不会写入销售台账、不会计入产品汇总的「成品出库」节点，也不影响成品月均出库统计。

### 3.2 选品数据源（二选一，均为现有接口）

方式一：库存分页查询

```
GET /api/stock/search?pageIndex=0&pageSize=20&categoryName=成品&keyword=感冒灵&quantity=0
```

方式二：按批次+制剂分组查询

```
GET /api/stock/grouped-search?pageIndex=0&pageSize=20&categoryName=成品&stockStatus=合格
```

返回的库存行可直接使用：`stockId`、`itemCode`、`itemName`、`categoryName`、`unitName`、`batchNumber`、`expiryDate`、`quantity`、`unitPrice`、`prodUnitId`。

### 3.3 提交出库

```
POST /api/stockout/direct-outbound
```

```json
{
  "outType": "报损出库",
  "prodUnitId": 4,
  "outDate": "2026-11-26T10:30:00",
  "remark": "成品破损报废",
  "details": [
    {
      "stockId": 128,
      "itemCode": "Z04-011",
      "itemName": "疏肝养胃丸",
      "categoryName": "成品",
      "unitName": "盒",
      "batchNumber": "261101",
      "quantity": 5,
      "unitPrice": 29.4
    }
  ]
}
```

要点：

- 明细必须携带 `stockId`（库存批次），成品与物料一致；
- `outType` 固定 `报损出库`；
- 库存不足会整体回滚并返回 `库存不足: X（当前 n，需出库 m）`；
- 报损出库当前不限制库存状态（损坏品可能是「合格」或「不合格」批次），只校验数量充足。

---

## 四、兼容与注意事项

1. 历史 `WLXH-` 物料销毁单据仍可正常查询、编辑（编辑保持 `recordType=物料销毁` 并继续携带类别/单位/数量）。
2. 历史「不合格品销毁」单据若没有品名类别/单位，编辑保存前需补齐。
3. 销毁记录始终不关联库存、不扣减库存；生产任务关联仅作追溯，不联动任务状态。
4. 数据库迁移脚本：`src/main/resources/Data/migration/add_destruction_record_work_order.sql`（幂等，可重复执行）。

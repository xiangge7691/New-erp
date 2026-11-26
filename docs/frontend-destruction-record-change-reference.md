# 前端修改参考：销毁记录（类型并入 + 生产任务关联）

> 对应后端提交：`c3f2069 feat: 销毁记录并入不合格品销毁并支持关联生产任务`
> 适用页面：质量模块 - 销毁记录（列表 / 筛选 / 新增 / 编辑 / 详情 / 作废）
> 后端接口前缀：`/api/destruction`

---

## 一、页面需要改什么（总览）

| 位置 | 改动 |
|---|---|
| 类型下拉（新增/编辑） | 只有「不合格品销毁」「留样及稳定性考察产品销毁」两种，页面无其他类型 |
| 类型下拉（列表筛选） | 同上，只有两种类型 |
| 明细表格 | 「不合格品销毁」必须显示并必填 **品名类别**、**计量单位**；数量必须大于 0 |
| 明细表格 | 品名类别显示「原辅包 / 中间品 / 成品」，提交值仍为「生产原料 / 中间产品 / 成品」 |
| 表单头部 | 新增可选字段 **关联生产任务**（远程下拉搜索，不限制任务状态，可清空） |
| 列表页 | 新增展示 **生产任务名称 / 编号**，可加「品名类别」列 |
| 筛选区 | 可增加「品名类别」筛选（仅类型为不合格品销毁或全部时显示） |
| 编辑态 | 记录类型不可再切换；已作废记录只读 |
| 详情页 | 展示生产任务编号 + 名称、各明细品名类别与单位 |

---

## 二、字段与取值

### 2.1 记录类型 `recordType`

| 页面显示 | 提交值 | 单号前缀 | 说明 |
|---|---|---|---|
| 不合格品销毁 | `不合格品销毁` | `BPXH-` | 原辅包/中间品/成品销毁都用这个 |
| 留样及稳定性考察产品销毁 | `留样及稳定性考察产品销毁` | `LYXH-` | 维持原逻辑 |

> 页面只有以上两种类型；后端已删除「物料销毁」类型与 `WLXH` 前缀（该类型从未上线，无历史数据）。

### 2.2 品名类别 `itemCategory`（明细）

| 页面显示 | 提交值 | 是否必填（不合格品销毁） |
|---|---|---|
| 原辅包 | `生产原料` | 是 |
| 中间品 | `中间产品` | 是 |
| 成品 | `成品` | 是 |

> 值必须用右侧三个枚举，页面做显示映射即可；留样销毁类型该字段可选（可不展示）。

### 2.3 计量单位 `unitName`（明细）

- 不合格品销毁必填，≤20 字符；
- 建议用单位下拉（可复用单位主数据或历史值），也允许手输；
- 留样销毁类型可不填。

### 2.4 关联生产任务（主表）

| 字段 | 方向 | 说明 |
|---|---|---|
| `workOrderId` | 前端提交 | 非必填；选中下拉项后回传ID；清空时提交 `null` |
| `workOrderCode` | 后端返回 | 任务编号快照，前端只读展示，不要提交 |
| `workOrderName` | 后端返回 | 任务名称快照，前端只读展示，不要提交 |

- 下拉**不限制任务状态**（待生产/生产中/已完成都可选）；
- 编辑时用 `workOrderId` 回显选中项，用 `workOrderName` 展示名称；
- 取消关联：保存时 `workOrderId` 传 `null`，后端会同时清空编号与名称。

---

## 三、接口清单

| # | 接口 | 方法 | 用途 |
|---|---|---|---|
| 1 | `/api/destruction` | GET | 分页查询列表 |
| 2 | `/api/destruction/{id}` | GET | 详情（含明细、附件） |
| 3 | `/api/destruction/generateCode?recordType=` | GET | 获取单号 |
| 4 | `/api/destruction/work-orders?keyword=` | GET | 生产任务下拉（新增） |
| 5 | `/api/destruction/retained-samples?keyword=` | GET | 留样下拉（原有） |
| 6 | `/api/destruction` | POST | 新增 |
| 7 | `/api/destruction/{id}` | PUT | 修改（明细整体重建） |
| 8 | `/api/destruction/{id}/void` | PUT | 作废（请求体为纯文本原因） |
| 9 | `/api/destruction/{id}/attachments` | GET | 附件列表 |

统一响应结构：`{ "code": 200, "message": "...", "data": ..., "timestamp": 1700000000000 }`（沿用现有 `ApiResponse`，`code=200` 表示成功）。

---

## 四、列表页

### 4.1 请求

```
GET /api/destruction?recordType=不合格品销毁&itemCategory=成品&keyword=BPXH&batchNo=20260701&status=正常&startDate=2026-10-01&endDate=2026-10-12&pageIndex=0&pageSize=10
```

| 参数 | 说明 |
|---|---|
| `recordType` | 精确匹配；只有 `不合格品销毁` / `留样及稳定性考察产品销毁` 两种 |
| `itemCategory` | 精确匹配明细品名类别，值 `生产原料/中间产品/成品` |
| `keyword` | 模糊匹配销毁单号 |
| `batchNo` | 模糊匹配明细批号（命中任一明细即返回主表） |
| `status` | `正常` / `作废` |
| `startDate` / `endDate` | 按登记时间（created_time）过滤，格式 `yyyy-MM-dd` |
| `pageIndex` | **从 0 开始** |
| `pageSize` | 默认 10 |

返回：`data.items[]` + `data.totalCount / pageIndex / pageSize`。

### 4.2 列表项字段

主表字段（新增字段加粗）：

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `destructionCode` | 销毁单号 |
| `recordType` | 记录类型 |
| **`workOrderCode`** | 生产任务编号 |
| **`workOrderName`** | 生产任务名称（建议列表展示，空时回退编号） |
| **`workOrderId`** | 生产任务ID |
| `totalQuantity` | 合计数量（后端求和） |
| `destroyDate` / `applyDate` | 销毁/申请日期 |
| `status` / `voidTime` / `voidRemark` | 状态与作废信息 |
| `createdByName` / `createdTime` | 创建人、登记时间 |
| `details[]` | 明细列表（见 4.3） |

### 4.3 明细字段

| 字段 | 说明 |
|---|---|
| `id` | 明细ID |
| `materialName` / `spec` / `batchNo` | 品名 / 规格 / 批号 |
| `quantity` | 数量 |
| `itemCategory` | 品名类别（展示时映射成 原辅包/中间品/成品） |
| `unitName` | 计量单位 |
| `retainedSampleId` / `retainedCode` | 关联留样ID / 留样编号 |

列表「品名类别」列可从 `details` 去重拼接，例如「原辅包、成品」。

---

## 五、新增 / 编辑表单

### 5.1 表单结构建议

```
基础信息：记录类型* / 销毁单号（只读，保存后生成） / 关联生产任务（可选）
销毁信息：销毁日期 / 销毁原因 / 销毁方法 / 销毁地点 / 备注
签字信息：申请人+日期 / 批准人+日期 / 销毁人+日期 / 监销人+日期
明细表格：品名* / 规格 / 批号 / 数量* / 品名类别* / 计量单位* / 关联留样（仅留样销毁类型）
附件：attachmentFileIds
```

### 5.2 按类型联动

| 类型 | 品名类别 | 计量单位 | 数量 | 关联留样 | 关联生产任务 |
|---|---|---|---|---|---|
| 不合格品销毁 | 显示 + 必填 | 显示 + 必填 | > 0 | 可选（若原表单已有） | 显示，可选 |
| 留样及稳定性考察产品销毁 | 隐藏或选填 | 隐藏或选填 | 可空/0 | 显示（反写留样状态） | 显示，可选 |

编辑态额外规则：

1. 记录类型下拉**禁用**（后端单号前缀不会随类型变化，避免 BPXH/LYXH 与类型不一致）；
2. 已作废（`status=作废`）整单只读，隐藏保存/作废按钮；
3. 明细是整体重建，保存时必须回传**全部明细字段**（含 `itemCategory`、`unitName`），不能只传变更行。

### 5.3 新增请求示例

```
POST /api/destruction
Content-Type: application/json
```

```json
{
  "recordType": "不合格品销毁",
  "workOrderId": 462,
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
  "remark": "",
  "details": [
    {
      "materialName": "维生素C",
      "spec": "食品级",
      "batchNo": "20260701",
      "quantity": 50.5,
      "itemCategory": "生产原料",
      "unitName": "kg"
    },
    {
      "materialName": "复方制剂A",
      "spec": "10ml/支",
      "batchNo": "20260615",
      "quantity": 30,
      "itemCategory": "中间产品",
      "unitName": "L"
    },
    {
      "materialName": "感冒灵胶囊",
      "spec": "0.5g/粒",
      "batchNo": "20260610",
      "quantity": 10,
      "itemCategory": "成品",
      "unitName": "盒"
    }
  ],
  "attachmentFileIds": [201]
}
```

不要提交 `destructionCode`、`totalQuantity`、`workOrderCode`、`workOrderName`，这些由后端生成或快照。

### 5.4 修改请求示例

```
PUT /api/destruction/1
```

```json
{
  "recordType": "不合格品销毁",
  "workOrderId": null,
  "destroyReason": "补充说明",
  "destroyMethod": "交有资质单位高温焚烧",
  "destroyDate": "2026-11-25",
  "details": [
    {
      "materialName": "维生素C",
      "spec": "食品级",
      "batchNo": "20260701",
      "quantity": 50.5,
      "itemCategory": "生产原料",
      "unitName": "kg"
    }
  ],
  "attachmentFileIds": [201, 202]
}
```

`workOrderId: null` 表示取消关联，后端会清空任务编号与名称快照。

---

## 六、生产任务下拉（新增接口）

```
GET /api/destruction/work-orders?keyword=活血
```

- `keyword` 可选，模糊匹配：任务编号 / 任务名称 / 制剂名称 / 批号；
- 不限制任务状态，按创建时间倒序最多 20 条；
- 建议输入防抖 300ms，下拉展示「任务名称（任务编号） · 制剂 · 批号」。

返回：

```json
{
  "code": 200,
  "message": null,
  "data": [
    {
      "workOrderId": 462,
      "workOrderCode": "GD202610190001",
      "workOrderName": "活血通络丸7000盒",
      "preparationName": "活血通络丸",
      "batchNumber": "260901"
    }
  ],
  "timestamp": 1764134400000
}
```

表单只保存 `workOrderId`；保存成功后用返回的 `workOrderCode` / `workOrderName` 回显。

---

## 七、详情页

`GET /api/destruction/{id}` 返回：

- 主表全部字段（含 `workOrderId / workOrderCode / workOrderName`）；
- `details[]`：明细（含 `itemCategory / unitName / retainedCode`）；
- `attachments[]`：附件列表；
- `createdByName / updatedByName`：创建人 / 更新人姓名。

详情页建议展示：

- 头部信息区：销毁单号、记录类型、生产任务「编号 + 名称」、销毁日期、状态；
- 明细表：品名、规格、批号、数量、品名类别（映射后）、单位、关联留样编号；
- 若 `status=作废`，展示作废时间与作废原因，并禁用编辑。

---

## 八、后端校验提示对照（直接弹 message）

| 后端提示 | 前端触发场景 / 处理建议 |
|---|---|
| `销毁明细不能为空，至少需要1行` | 保存时明细表为空 |
| `明细品名必填` | 品名未填 |
| `不合格品销毁明细数量必须大于0: X` | 数量为空或 ≤ 0 |
| `不合格品销毁明细品名类别必填: X` | 未选品名类别 |
| `品名类别只能是生产原料/中间产品/成品: X` | 提交了非法类别 |
| `不合格品销毁明细计量单位必填: X` | 未填单位 |
| `已作废的销毁记录不能修改` | 编辑已作废单据 |
| `该销毁记录已作废` | 重复作废 |
| `关联生产任务不存在：X` | 任务被删除或ID非法，刷新下拉重选 |
| `销毁单号生成冲突，请稍后重试` | 并发冲突，可提示重试 |
| `留样 X 已被其他有效销毁单引用，不能重复销毁` | 留样销毁类型重复引用 |
| `作废原因不能超过500字符` | 作废原因超长 |

前端可在提交前做同规则校验，减少无效请求。

---

## 九、兼容与边界

1. **历史「不合格品销毁」数据**：可能没有品名类别/单位，列表正常展示为空；编辑保存前需补齐，否则会触发必填提示。
2. **销毁记录不关联库存**：页面不要提供库存批次选择，也不会扣减库存。
3. **生产任务仅追溯**：关联/取消关联不影响任务状态、排产或库存。
4. **合计数量只读**：由后端按明细求和，前端展示即可。
5. **明细整体重建**：编辑保存必须提交完整明细数组；删除某行 = 新数组里不包含该行。
6. **附件同步**：编辑时提交完整的 `attachmentFileIds` 列表，未包含的文件会被解绑。

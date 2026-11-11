# 审核放行 —— 前端对接指南

> 原型：`_analysis/prototypes/release-review.html`
> 后端：`New-erp`（`v1.0`）
> 核心代码：`ReleaseBatchController`、`ReleaseBatchServiceImpl`、`ReleaseBatchDtos`
> 测试库：`8.134.48.54/erp_db`；**禁止向 `8.148.27.237` 写入测试数据**
> 关联文档：[检验管理 / 审核放行前端实现指导](./frontend-implementation-guide-inspection-release.md)

本文按原型的页面区块逐块说明「调用哪个接口、取哪个字段、怎么拼状态、有哪些坑」，可以直接作为联调依据。

---

## 0. 结论先行

1. 本页面**只使用批次聚合接口** `/api/release-review/batches`，主键是 `workOrderId`。
2. **不要调用**旧单表接口 `/api/releaseReview`（主键 `reviewId`，只用于旧 CRUD 页面），两套接口的数据口径不同，混用会出现「列表有批次、详情查不到」。
3. 页面上的「待审核 / 待放行 / 已放行 / 已拒绝」不是独立状态字段，而是后端根据放行记录和合规五项**推导**出来的 `releaseStatus`，前端直接展示即可。
4. 列表的「阻塞项」直接用 `blockers[]`，不要前端逐条查详情推导。
5. 所有写操作（工序检查、草稿、放行、拒绝）成功后，**必须同时刷新详情、列表、顶部统计**，因为 `processPass`、`fileUploaded`、`canRelease`、`releaseStatus` 都会联动变化。
6. 已放行批次是只读的：再次放行、拒绝、改草稿、改工序检查，后端均返回业务错误（`code = 400`），前端应提前禁用按钮。

### 页面区块与接口对照

| 原型区块 | 接口 | 说明 |
|---|---|---|
| 顶部统计卡（待审核 / 可放行 / 存在阻塞） | `GET /api/release-review/batches/stats` | 不随「放行状态」筛选变化 |
| 筛选栏「品种」下拉 | `GET /api/release-review/batches/varieties` | 仅返回有批号的工单品种 |
| 筛选栏 + 批次列表 | `GET /api/release-review/batches` | 分页，`pageIndex` 从 0 开始 |
| 点击行进入详情 | `GET /api/release-review/batches/{workOrderId}` | 一次返回全批证据 |
| 详情「工序记录与符合性」保存 | `POST .../{workOrderId}/process-checks` | 批量保存，整单事务 |
| 详情「检验记录」查看表单 | `GET .../batches/{workOrderId}` 的 `requests/samplings/inspections` | 需后端补充字段，见第 9 节 |
| 详情其他单据「查看表单」 | 同批次详情接口的分组字段 | 采购/验收/指令/领料/退库/工序/偏差，见第 10 节 |
| 详情「保存草稿」 | `POST .../{workOrderId}/draft` | 结论固定「待补充」 |
| 详情「通过放行」 | `POST .../{workOrderId}/approve` | 需 `compliance.canRelease = true` |
| 详情「拒绝放行」 | `POST .../{workOrderId}/reject` | 拒绝原因必填 |
| 页面右上「导出待审核」 | `POST .../export` | 返回 CSV 文件流，不是 JSON |

---

## 1. 通用约定

### 1.1 服务地址与鉴权

- 开发环境通过前端代理访问后端，接口前缀统一为 `/api`。
- 除登录外，前端应始终携带：

```http
Authorization: Bearer <token>
Content-Type: application/json
```

当前 `SecurityConfig` 对 `/api/**` 配置为 `permitAll`，JWT 过滤器只做「有令牌就解析」，因此不带令牌也能访问；但**不带令牌会导致审计字段、审核人默认值取不到当前用户**，联调时仍应正常携带。

前端如果要默认填入「放行批准人」，可调用 `GET /api/user/info`（需令牌）取当前用户；取不到时留空，后端会回退为当前登录用户姓名，再回退为「系统」。

### 1.2 统一响应格式

除文件流接口外，所有接口返回 `ApiResponse`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1762312345678
}
```

| code | 含义 | 前端处理 |
|---|---|---|
| 200 | 成功 | 取 `data` |
| 400 | 参数/业务校验失败（如「已放行不能重复放行」） | 直接展示 `message` |
| 500 | 服务端异常 | 展示 `message`，可提示稍后重试 |

注意：这些业务错误仍然是 **HTTP 200**，HTTP 状态码不能作为成功与否的判断依据，必须判断响应体 `code`。项目里的 `request` 封装通常已统一处理，业务代码优先复用。

### 1.3 分页与日期

- 分页参数：`pageIndex` 从 **0** 开始，`pageSize` 默认 20。
- 返回：`items`、`totalCount`、`pageIndex`、`pageSize`，另有计算属性 `totalPages`。
- `LocalDate` 序列化为 `yyyy-MM-dd`。
- `LocalDateTime` 序列化为 `yyyy-MM-dd HH:mm:ss`（项目已全局配置，不是 ISO 的 `T` 格式）。
- 请求体中的 `reviewDate` 传 `yyyy-MM-dd`；如需精确到时分秒，改传 `reviewTime`（`yyyy-MM-dd HH:mm:ss`）。

### 1.4 文件接口

详情里的附件只返回 `fileId`，预览/下载使用文件模块的通用接口：

| 用途 | 接口 | 返回 |
|---|---|---|
| 预览 | `GET /api/files/preview/{fileId}` | 文件流（浏览器可内联预览的会带 `inline`） |
| 下载 | `GET /api/files/{fileId}` | 文件流（`Content-Disposition: attachment`） |

前端建议用 `axios` 以 `responseType: 'blob'` 拉取后 `URL.createObjectURL` 打开/下载，避免 `window.open` 在需要鉴权时丢失请求头；若确认环境允许匿名访问，也可以直接 `window.open`。

---

## 2. 接口明细

### 2.1 批次审核列表

```http
GET /api/release-review/batches
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `varietyCode` | string | 否 | 品种编码，精确匹配 |
| `batchNo` | string | 否 | 批号 / 工单号 / 生产计划号，模糊匹配 |
| `status` | string | 否 | 放行状态，精确匹配：`待审核` / `待放行` / `已放行` / `已拒绝` |
| `keyword` | string | 否 | 关键字：工单号 / 批号 / 品种名称 / 生产计划号，模糊匹配 |
| `pageIndex` | int | 否 | 页码，从 0 开始，默认 0 |
| `pageSize` | int | 否 | 每页数量，默认 20 |

示例：

```http
GET /api/release-review/batches?varietyCode=Z04-011&batchNo=260901&status=待审核&pageIndex=0&pageSize=20
```

响应 `data`：

```json
{
  "items": [
    {
      "workOrderId": 463,
      "workOrderCode": "GD202610200001",
      "planNumber": "Plan202610190001",
      "batchNo": "260901",
      "preparationCode": "Z04-011",
      "preparationName": "疏肝养胃丸",
      "spec": "每50丸重6g（每1g相当于饮片约0.83g）",
      "unitName": "盒",
      "planQty": 7000,
      "finishedQty": 7001,
      "planDate": "2026-09-16",
      "actualStart": "2026-09-16",
      "actualEnd": "2026-09-24",
      "yieldRate": 100.01,
      "inspectionConclusion": "合格",
      "processTotal": 8,
      "processPass": 8,
      "processFail": 0,
      "processPending": 0,
      "fileUploaded": 20,
      "fileRequired": 20,
      "fileMissing": 0,
      "deviationTotal": 0,
      "deviationOpen": 0,
      "releaseStatus": "待放行",
      "releaseApprover": null,
      "releaseDate": null,
      "canRelease": true,
      "blockers": []
    }
  ],
  "totalCount": 1,
  "pageIndex": 0,
  "pageSize": 20,
  "totalPages": 1
}
```

说明：

- `batchNo` 条件是「批号 OR 工单号 OR 计划号」，所以原型输入框占位符「批号 / 工单号」可以直接对应。
- 排序固定为工单创建时间倒序，前端不要再按生产日期二次排序。
- 列表是「先聚合再分页」，批次量大时首屏会比普通单表查询慢，前端加载态要正常显示。
- `releaseStatus` 为推导值，详见第 4 节。

### 2.2 顶部看板统计

```http
GET /api/release-review/batches/stats
```

参数与列表相同（不含分页）。**`status` 参数会被后端忽略**，统计始终基于当前品种/批号/关键字条件下的全量批次。

响应 `data`：

```json
{
  "total": 5,
  "pendingReview": 2,
  "releasable": 1,
  "blocked": 2,
  "released": 1,
  "rejected": 0
}
```

原型只展示三项，对应关系：

| 原型指标 | 字段 |
|---|---|
| 待审核 | `pendingReview` |
| 可放行 | `releasable` |
| 存在阻塞 | `blocked` |

`blocked` 的口径是「未放行且不合规」，包含已拒绝但仍存在阻塞项的批次。

### 2.3 品种下拉

```http
GET /api/release-review/batches/varieties
```

响应 `data`：

```json
[
  { "preparationCode": "Z04-011", "preparationName": "疏肝养胃丸" },
  { "preparationCode": "Z04-009", "preparationName": "活血通络丸" }
]
```

下拉文案建议：`${preparationName}（${preparationCode}）`，`value` 用 `preparationCode`。只返回存在批号的工单品种。

### 2.4 批次详情

```http
GET /api/release-review/batches/{workOrderId}
```

`workOrderId` 来自列表行的 `workOrderId`，不是 `reviewId`。

响应 `data` 的顶层结构：

```json
{
  "batch": {},
  "purchases": [],
  "acceptances": [],
  "instructions": [],
  "requisitions": [],
  "returns": [],
  "processes": [],
  "requests": [],
  "samplings": [],
  "inspections": [],
  "rooms": [],
  "yieldInfo": {},
  "deviations": [],
  "fileChecklist": [],
  "compliance": {},
  "release": {}
}
```

各分组字段见第 3 节。

### 2.5 保存工序符合性检查

```http
POST /api/release-review/batches/{workOrderId}/process-checks
Content-Type: application/json
```

```json
{
  "items": [
    {
      "executionId": 1001,
      "processTypeId": 63,
      "processName": "粉碎",
      "requirementSnapshot": "粒度符合内控标准；设备清洁合格；操作人签字",
      "checkResult": "符合",
      "inspectorId": 12,
      "inspectorName": "丁圣芳",
      "checkDate": "2026-11-06",
      "remark": ""
    }
  ]
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `items[].executionId` | 是 | 工序执行记录 ID，必须属于当前工单 |
| `items[].checkResult` | 是 | 只允许 `符合` / `不符合` / `待查` |
| `items[].processTypeId` | 否 | 工序类型 ID |
| `items[].processName` | 否 | 工序名称，建议回传详情里的值 |
| `items[].requirementSnapshot` | 否 | 要求快照，建议回传详情里的 `requirement` |
| `items[].inspectorId` | 否 | 查证人 ID |
| `items[].inspectorName` | 否 | 查证人姓名 |
| `items[].checkDate` | 否 | 查证日期 `yyyy-MM-dd` |
| `items[].remark` | 否 | 备注 |

规则：

- 整批保存，任一条不合法则整单失败（事务回滚）。
- 已放行批次调用会返回 `400`：「该批次已放行，不能修改工序检查」。
- 保存成功后建议刷新详情 + 列表 + 统计，因为工序通过数、阻塞项、可放行结论都会变。

### 2.6 保存放行草稿

```http
POST /api/release-review/batches/{workOrderId}/draft
```

```json
{
  "conclusion": "待补充",
  "approver": "赵磊",
  "reviewDate": "2026-11-06",
  "opinion": "验收检验报告待补"
}
```

规则：

- `conclusion` 只能为 `待补充`；不传时后端默认 `待补充`，传其他值返回 `400`。
- 草稿**不校验合规**，不写工单放行时间。
- 草稿不会把批次变成「待放行」：批次状态仍按合规五项推导，可能是「待审核」或「待放行」。
- 已放行批次不能保存草稿。
- 返回完整的批次详情（同 2.4），前端可以直接用返回值覆盖当前详情。

### 2.7 通过放行

```http
POST /api/release-review/batches/{workOrderId}/approve
```

```json
{
  "approver": "赵磊",
  "reviewDate": "2026-11-06",
  "opinion": "批记录、工序记录、检验报告齐全，检验结果符合规定，同意放行。"
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `approver` | 否 | 放行批准人；为空时后端取当前登录用户姓名，再回退「系统」 |
| `reviewDate` | 否 | 审核日期，取当天当前时刻 |
| `reviewTime` | 否 | 精确审核时间，优先级高于 `reviewDate` |
| `opinion` | 否 | 审核意见，为空时默认「合规检查通过，同意放行。」 |

规则：

- 必须先满足 `compliance.canRelease = true`，否则返回 `400`：「存在不符合项，不能通过放行」。
- 已放行批次返回 `400`：「该批次已放行，不能重复放行」。
- 成功后写入/更新放行记录（结论 `放行`），同步工单「审核放行时间」，并联动工单/生产计划状态。
- 返回放行后的完整批次详情。

### 2.8 拒绝放行

```http
POST /api/release-review/batches/{workOrderId}/reject
```

```json
{
  "reviewer": "赵磊",
  "reviewDate": "2026-11-06",
  "reason": "工序记录不符合要求，需补充处理后再审核。"
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `reason` | **是** | 拒绝原因，为空返回 `400` |
| `reviewer` | 否 | 审核人，为空回退当前登录用户 |
| `reviewDate` / `reviewTime` | 否 | 同放行 |

规则：

- 拒绝**不校验**合规五项，但已放行批次不能拒绝。
- 拒绝后批次状态为「已拒绝」；后续整改完成、合规重新满足后，仍可再次「通过放行」。
- 返回拒绝后的完整批次详情。

### 2.9 导出待审核批次

```http
POST /api/release-review/batches/export
Content-Type: application/json
```

```json
{
  "varietyCode": "Z04-011",
  "batchNo": "260901",
  "status": "待审核",
  "keyword": ""
}
```

规则：

- 返回 **CSV 文件流**，不是 `ApiResponse`；文件带 UTF-8 BOM，Excel 打开不乱码。
- `status` 为空时默认导出「待审核」。
- 文件名在 `Content-Disposition` 中，形如 `审核放行_2026-11-10.csv`。
- 前端请求需设置 `responseType: 'blob'`，并从响应头解析文件名；解析失败时用本地默认名。

---

## 3. 原型区块与字段映射

### 3.1 列表表格

| 原型列 | 字段 | 渲染建议 |
|---|---|---|
| 批次 / 品种 | `preparationName`、`batchNo`、`preparationCode` | 主行品种名，副行「批号 xxx · 编码」 |
| 检验结论 | `inspectionConclusion` | 合格绿色；不合格红色；检验中/未检验蓝色或灰色 |
| 工序符合 | `processPass` / `processTotal` | 副行：`processFail > 0` 显示「不符合 N」，否则 `processPending > 0` 显示「待查 N」，否则「全部符合」 |
| 文件完整 | `fileUploaded` / `fileRequired` | 副行：`fileMissing > 0` 显示「缺 N 项」，否则「完整」 |
| 阻塞项 | `blockers[]` | 有值逐项红色 `ElTag`；空数组显示绿色「无阻塞」 |
| 放行状态 | `releaseStatus` | 见 4.1 颜色映射 |
| 放行批准人 | `releaseApprover` | 取放行审核记录的审核人；未生成放行记录时为空（草稿也会记录审核人） |
| 日期 | `releaseDate` | 取放行审核记录的审核日期（yyyy-MM-dd）；未生成放行记录时为空 |
| 操作 | `releaseStatus` | 已放行显示「查看」，其余显示「审核」 |

### 3.2 详情头部（`batch` + `release`）

| 原型字段 | 字段 |
|---|---|
| 品种 | `batch.preparationName` |
| 规格 | `batch.spec` |
| 批号 | `batch.batchNo` |
| 批量 / 成品 | `batch.planQty` / `batch.finishedQty`，单位 `batch.unitName` |
| 计划生产 | `batch.planDate` |
| 实际生产 | `batch.actualStart` ~ `batch.actualEnd` |
| 整批收率 | `batch.yieldRate` |
| 工单号 / 计划号 | `batch.workOrderCode` / `batch.planNumber` |
| 工单状态 | `batch.workOrderStatus` |
| 放行状态 | `batch.releaseStatus` |
| 放行审核结论 | `release.conclusion`（空则显示「待审核」） |
| 放行批准人 | `release.approver` |
| 审核时间 | `release.reviewTime` |
| 审核意见 | `release.opinion` |

`release` 始终是对象；没有放行记录时 `release.id` 为 `null`，各字段为空。

### 3.3 物料 / 采购 / 验收

**`purchases[]`**

| 字段 | 说明 |
|---|---|
| `id` | 采购订单 ID |
| `code` | 采购单号 |
| `supplier` | 供应商（取采购订单明细中的供应商） |
| `materialType` | 物料类型 |
| `status` | 采购订单状态。后端字段是 `Object`，可能返回中文或数字编码，前端按现有采购页字典展示，未知值原样显示 |
| `date` | 下单日期 |
| `orderTime` | 下单时间（当前 `date` 为其日期部分） |
| `invoiceNo` | 发票号 |
| `remark` | 备注 |
| `lines[]` | 采购明细行（字段见第 10.2 节） |
| `fileCount` | 附件数 |
| `files[]` | 附件，见 3.9 |

**`hospitalRequisitions[]`**（医院领料，与 `purchases[]` 同级展示）

| 字段 | 说明 |
|---|---|
| `id` | 领料单 ID |
| `code` | 领料单号 |
| `planCode` / `planTitle` | 关联生产计划编号 / 标题 |
| `fromDept` / `toDept` | 领用科室 / 发放科室 |
| `applicant` / `applyTime` / `date` | 领料人 / 领料时间 / 领料日期 |
| `materialType` / `multiplier` | 物料类型 / 处方倍数 |
| `status` | 状态：待发放/验收中/已入库/已作废 |
| `acceptanceOrderId` | 关联验收单 ID |
| `remark` | 备注 |
| `lines[]` | 明细行（字段同 10.2） |
| `files[]` | 附件 |

**`acceptances[]`**

| 字段 | 说明 |
|---|---|
| `id` | 验收单 ID |
| `code` | 验收单号 |
| `status` | 验收单状态 |
| `purchaseNumber` | 关联采购单号 |
| `uploadedKindLabels[]` | 已上传文件类型中文名 |
| `missingKindLabels[]` | 缺失文件类型中文名 |
| `sourceType` | 来源类型 |
| `arrivalTime` | 到货时间 |
| `acceptanceTime` | 验收时间 |
| `acceptor` / `reviewer` | 验收人 / 复核人 |
| `remark` | 备注 |
| `details[]` | 验收明细行（字段见第 10.2 节） |
| `files[]` | 已上传附件 |

验收单固定检查三个槽位：`随货清单`、`验收检验报告`、`发票`。原型里的「检验报告」实际返回文案是「验收检验报告」，以接口为准。

### 3.4 生产指令 / 领料 / 退库

**`instructions[]`**（通常只有一条）

| 字段 | 说明 |
|---|---|
| `id` | 生产计划 ID |
| `code` | **生产计划号**（原型中的「生产指令编号」直接取此值） |
| `type` | 固定「生产指令」 |
| `date` | 计划生产日期 |
| `status` | 生产计划状态 |
| `preparation` / `preparationCode` | 品种名称 / 制剂编码 |
| `spec` / `batch` / `qty` | 规格 / 批号 / 批量 |
| `planStart` / `planEnd` | 计划开始 / 结束日期 |
| `processRoute` | 工艺路线 |
| `remark` | 备注 |
| `files[]` | 生产计划附件 |

**`requisitions[]`**

| 字段 | 说明 |
|---|---|
| `code` | 领料单号 |
| `date` | 领料日期 |
| `status` | 领料状态 |
| `operator` | 申请人 |
| `purpose` | 领料用途 |
| `warehouse` | 领料仓库 |
| `stockOutCode` | 关联出库单号 |
| `remark` | 备注 |
| `lines[]` | 领料明细行（字段见第 10.2 节） |
| `files[]` | 关联出库单附件；无出库单时为空数组 |

**`returns[]`**

| 字段 | 说明 |
|---|---|
| `code` | 退库单号 |
| `date` | 退库时间 |
| `status` | `returnTime` 有值时为「已退库」，否则「待退库」 |
| `operator` | 退库人 |
| `stockOutCode` | 关联出库单号 |
| `remark` | 备注 |
| `lines[]` | 退库明细行（字段见第 10.2 节） |
| `files[]` | 已按 `RETURN_ORDER` 返回；无附件时为空数组 |

### 3.5 工序记录与符合性（`processes[]`）

| 字段 | 说明 |
|---|---|
| `executionId` | 工序执行记录 ID，保存检查时必传 |
| `processTypeId` | 工序类型 ID |
| `processName` | 工序名称 |
| `requirement` | 配置要求；优先取品种工序模板，其次工序类型的工艺参数 |
| `result` | `符合` / `不符合` / `待查`（无检查记录时为 `待查`） |
| `inspector` | 工艺查证人姓名 |
| `checkDate` | 查证日期 |
| `startTime` / `endTime` | 工序执行起止时间 |
| `operator` | 操作人 |
| `status` | 工序执行状态 |
| `room` / `equipment` | 房间 / 设备名称 |
| `remark` | 备注 |
| `files[]` | 工序记录附件 |

页面上的下拉值必须使用 `符合` / `不符合` / `待查` 三个中文枚举，不要传 `PASS` / `FAIL`。

### 3.6 检验记录（`requests[]` / `samplings[]` / `inspections[]`）

原型已从「只列单号」调整为明细表 + 表单详情抽屉，三类单据结构不同，不要再按同一套字段渲染。本节只讲检验记录的字段；采购、验收、生产指令、领料、退库、工序、偏差等其他单据的「查看表单」支持方式见第 10 节。

**请检单 `requests[]`**

| 字段 | 说明 |
|---|---|
| `id` | 请检单 ID |
| `code` | 请检单号 |
| `type` | 固定 `请检单` |
| `itemCategory` | 被检品属性：生产原料/中间产品/成品 |
| `process` | 工序 / 检验环节 |
| `status` | 单据状态（当前后端未赋值，见第 9 节） |
| `requester` | 请检人（当前字段名是 `operator`） |
| `requestDepartment` | 请检部门 |
| `time` | 请检时间 |
| `inspectionItems` | 检验项目 |
| `configQuantity` | 配置数量 |
| `remark` | 备注 |
| `files[]` | 对应业务附件 |

**取样记录 `samplings[]`**

| 字段 | 说明 |
|---|---|
| `id` | 取样记录 ID |
| `code` | 取样编号 |
| `type` | 固定 `取样记录` |
| `itemCategory` | 被检品属性：生产原料/中间产品/成品 |
| `status` | 单据状态（当前后端未赋值） |
| `sampler` | 取样人（当前字段名是 `operator`） |
| `time` | 取样时间 |
| `samplingQuantity` / `samplingCount` / `samplingMethod` / `samplingLocation` | 取样量 / 份数 / 方法 / 地点 |
| `remark` | 备注 |
| `files[]` | 对应业务附件 |

**检验记录 `inspections[]`**

| 字段 | 说明 |
|---|---|
| `id` | 检验记录 ID |
| `code` | 检验编号 |
| `type` | 固定 `检验记录` |
| `itemCategory` | 被检品属性：生产原料/中间产品/成品 |
| `status` | 单据状态（当前后端未赋值） |
| `conclusion` | 检验结论 |
| `inspectionBasis` | 检验依据 |
| `inspectionItem` | 检验项目 |
| `inspector` / `reviewer` | 检验人 / 复核人（当前只回传检验人 `operator`） |
| `startTime` / `endTime` | 检验起止时间（当前只回传结束时间 `time`） |
| `relatedInspectionRequestCode` / `relatedSamplingCode` | 关联请检单 / 取样记录 |
| `remark` | 备注 |
| `files[]` | 检验记录、图谱等附件 |

> 当前后端 `InspectionItem` 回传 `id/code/type/itemCategory/process/status/conclusion/time/operator/files` 等字段，其中 `itemCategory` 为被检品属性；`status` 在部分单据上未赋值。前端要做「查看表单」必须等第 9 节的字段扩展，或点击时改调各单据详情接口。


### 3.7 区域记录（`rooms[]`）

| 字段 | 说明 |
|---|---|
| `roomId` | 房间 ID |
| `roomName` | 房间名称 |
| `record` | 当前固定返回「工序执行区域」 |
| `result` | 取该房间对应工序执行记录的状态 |

原型里的「清洁合格 / 温湿度合格」是模拟数据；后端目前没有把清洁/温湿度记录聚合进来，前端按接口原样展示，不要按原型文案写死。

### 3.8 收率与偏差

**`yieldInfo`**

| 字段 | 说明 |
|---|---|
| `planQty` | 计划批量 |
| `finishedQty` | 实际成品数 |
| `yieldRate` | 整批收率（%） |
| `threshold` | 合格阈值，当前为 `98` |
| `pass` | `yieldRate >= threshold` |

**`deviations[]`**

| 字段 | 说明 |
|---|---|
| `code` | 偏差编号 |
| `title` | 偏差标题 |
| `conclusion` | 处理结论 |
| `handler` | 处理人 |
| `handleDate` | 处理日期 |
| `status` | `未关闭` / `已关闭` |
| `occurTime` | 发生时间 |
| `processName` | 发生工序 |
| `description` | 偏差描述 |
| `action` | 处理措施 |
| `remark` | 备注 |
| `files[]` | 偏差附件 |

判定规则：`handleTime == null && resultDesc 为空` 视为「未关闭」，否则「已关闭」。

### 3.9 附件对象 `files[]`

```json
{
  "fileId": 8801,
  "name": "采购订单.pdf",
  "size": 204800,
  "time": "2026-10-20 10:32:00",
  "uploader": ""
}
```

- 预览：`GET /api/files/preview/{fileId}`
- 下载：`GET /api/files/{fileId}`
- `uploader` 当前后端固定返回空字符串，前端不要依赖它展示上传人。
- `size` 单位为字节，前端自行格式化为 KB/MB。

### 3.10 文件完整性清单（`fileChecklist[]`）

| 字段 | 说明 |
|---|---|
| `stage` | 文件阶段 |
| `label` | 展示名称 |
| `bizType` | 业务类型编码 |
| `fileKind` | 文件槽位 |
| `requirement` | `REQUIRED` / `CONDITIONAL` / `OPTIONAL` |
| `uploaded` | 已上传数量 |
| `total` | 应上传数量 |
| `note` | 缺失提示，如「缺 验收单 XX 缺 验收检验报告」；可选文件为「可空缺」 |
| `missingDetails[]` | 缺失明细，如「验收单 YS-20261019-005 缺 验收检验报告」 |

展示规则：

- `OPTIONAL` 统一标记「不必传」，不进入缺件数；后端的 `fileUploaded` / `fileRequired` 已排除可选文件。
- `CONDITIONAL` 与 `REQUIRED` 一样参与缺件判断。
- 清单行**没有 `fileId`**；要预览具体文件，请使用证据区分组里的 `files[]`，不要为清单行虚构业务 ID。

### 3.11 合规汇总（`compliance`）

```json
{
  "inspectionPass": true,
  "processPass": true,
  "filePass": true,
  "deviationPass": true,
  "yieldPass": true,
  "canRelease": true,
  "summary": "全部合规，可以放行"
}
```

侧栏「合规检查」直接逐项渲染这五个布尔值；「通过放行」按钮的可用条件同时看 `compliance.canRelease` 和 `batch.releaseStatus !== '已放行'`。

### 3.12 放行表单（`release`）

| 表单控件 | 字段 | 说明 |
|---|---|---|
| 放行结论 | `release.conclusion` | 只读展示；草稿为「待补充」，拒绝为「拒绝放行」 |
| 放行批准人 | `release.approver` | 可编辑后随放行/草稿请求提交 |
| 日期 | `release.reviewTime` | 提交时用 `reviewDate` 传日期 |
| 审核意见 | `release.opinion` | 放行/草稿均可提交 |

原型里的「放行结论」下拉（放行 / 拒绝放行 / 待补充）不需要作为独立接口参数：通过放行走 `/approve`，拒绝走 `/reject`，草稿走 `/draft`，三条路径已经确定结论。

---

## 4. 状态与业务规则

### 4.1 `releaseStatus` 推导

后端按以下顺序判定，前端不要再自行拼接：

1. 存在放行记录且结论包含「拒绝」→ `已拒绝`
2. 存在放行记录且结论包含「放行」→ `已放行`（兼容历史值「同意放行」）
3. 否则：合规五项全部通过 → `待放行`；不通过 → `待审核`

因此：

- 草稿（`待补充`）不会改变「待审核/待放行」的判定。
- 「待放行」不是数据库字段，只是「已合规但还没放行」的展示态。
- 已拒绝批次若整改后重新合规，状态仍显示「已拒绝」，直到再次执行放行。

颜色建议：

| 状态 | 颜色 |
|---|---|
| 待审核 | warning / orange |
| 待放行 | primary / blue |
| 已放行 | success / green |
| 已拒绝 | danger / red |

### 4.2 `canRelease` 五项

| 项 | 条件 | 对应字段 |
|---|---|---|
| 检验 | 成品检验结论为「合格」 | `inspectionPass` |
| 工序 | 无「不符合」且无「待查」；无工序记录时视为通过 | `processPass` |
| 文件 | 必传文件已传数量 ≥ 应传数量 | `filePass` |
| 偏差 | 无未关闭偏差 | `deviationPass` |
| 收率 | 收率非空且 ≥ 98% | `yieldPass` |

### 4.3 `blockers[]` 文案

后端生成的阻塞项格式固定，前端直接展示：

- `检验 未完成` / `检验 检验中` / `检验 不合格`
- `工序 6/8`
- `文件缺 2`
- `偏差 1 未关闭`
- `收率未录入` / `收率 97.5%`

### 4.4 检验结论 `inspectionConclusion`

- 优先取该批次成品检验记录（按结束时间倒序第一条）的结论。
- 结论为空或「待检」→ `检验中`。
- 没有任何检验记录时：工单状态为「检验中/已检验」则返回该状态，否则返回 `未检验`。
- 历史数据没有「被检品属性」时，兼容使用全部检验记录。

### 4.5 文件缺件口径

- 只统计批次相关业务类型，品种级资料不计入缺件。
- `fileUploaded` / `fileRequired` 不含 `OPTIONAL`。
- 放行记录本身存在即视为「放行审核」已传 1，不会因为正在审核而自造缺件。

### 4.6 审核时间与审核人

- `reviewDate` 只取日期，时刻用服务器当前时间；需要精确时间请传 `reviewTime`。
- 批准人/审核人为空时的回退顺序：请求值 → 当前登录用户 `userName` → `系统`。
- 审核人不是权限校验，仅作为记录展示；权限控制仍由前端菜单/按钮权限与后端登录态负责。

---

## 5. 前端落地方案

### 5.1 页面状态

```ts
const state = reactive({
  filters: {
    varietyCode: '',
    batchNo: '',
    status: ''
  },
  pageIndex: 0,
  pageSize: 20,
  stats: {} as ReleaseBoardStats,
  varieties: [] as VarietyOption[],
  rows: [] as ReleaseBatchSummary[],
  totalCount: 0,
  detail: null as ReleaseBatchDetail | null,
  loading: false,
  detailLoading: false
})
```

注意：

- `pageIndex` 内部保存 0 基，交给 `useTable` 时再转换成 1 基。
- 查询/重置时把 `pageIndex` 归零。
- 详情抽屉打开时保存当前 `workOrderId`，关闭时清空编辑态。

### 5.2 TypeScript 类型

```ts
export type ReleaseStatus = '待审核' | '待放行' | '已放行' | '已拒绝'
export type ProcessCheckResult = '符合' | '不符合' | '待查'

export interface ReleaseBatchQuery {
  varietyCode?: string
  batchNo?: string
  status?: ReleaseStatus | ''
  keyword?: string
  pageIndex?: number
  pageSize?: number
}

export interface ReleaseBatchSummary {
  workOrderId: number
  workOrderCode?: string
  planNumber?: string
  batchNo?: string
  preparationCode?: string
  preparationName?: string
  spec?: string
  unitName?: string
  planQty?: number
  finishedQty?: number
  planDate?: string
  actualStart?: string
  actualEnd?: string
  yieldRate?: number
  inspectionConclusion?: string
  processTotal: number
  processPass: number
  processFail: number
  processPending: number
  fileUploaded: number
  fileRequired: number
  fileMissing: number
  deviationTotal: number
  deviationOpen: number
  releaseStatus?: ReleaseStatus
  canRelease: boolean
  blockers: string[]
}

export interface ReleaseBoardStats {
  total: number
  pendingReview: number
  releasable: number
  blocked: number
  released: number
  rejected: number
}

export interface VarietyOption {
  preparationCode: string
  preparationName: string
}

export interface ReleaseFileItem {
  fileId: number
  name: string
  size?: number
  time?: string
  uploader?: string
}

export interface ReleaseBatchInfo {
  workOrderId: number
  workOrderCode?: string
  planNumber?: string
  batchNo?: string
  preparationCode?: string
  preparationName?: string
  spec?: string
  unitName?: string
  planQty?: number
  finishedQty?: number
  planDate?: string
  actualStart?: string
  actualEnd?: string
  yieldRate?: number
  inspectionConclusion?: string
  workOrderStatus?: string
  releaseStatus?: ReleaseStatus
}

export interface ReleasePurchaseItem {
  id: number
  code?: string
  supplier?: string
  materialType?: string
  status?: string | number
  date?: string
  fileCount: number
  files: ReleaseFileItem[]
}

export interface ReleaseAcceptanceItem {
  id: number
  code?: string
  status?: string
  purchaseNumber?: string
  uploadedKinds: string[]
  missingKinds: string[]
  uploadedKindLabels: string[]
  missingKindLabels: string[]
  files: ReleaseFileItem[]
}

export interface ReleaseDocumentItem {
  id: number
  code?: string
  type?: string
  date?: string
  status?: string
  operator?: string
  files: ReleaseFileItem[]
}

export interface ReleaseProcessItem {
  executionId: number
  processTypeId?: number
  processName?: string
  requirement?: string
  result: ProcessCheckResult
  inspector?: string
  checkDate?: string
  startTime?: string
  endTime?: string
  operator?: string
  status?: string
  files: ReleaseFileItem[]
}

export interface ReleaseInspectionItem {
  id: number
  code?: string
  type?: string
  process?: string
  status?: string
  conclusion?: string
  time?: string
  operator?: string
  files: ReleaseFileItem[]
  // 请检单扩展字段
  requester?: string
  requestDepartment?: string
  requestTime?: string
  inspectionItems?: string
  configQuantity?: string
  // 取样记录扩展字段
  sampler?: string
  samplingTime?: string
  samplingQuantity?: string
  samplingCount?: number
  samplingMethod?: string
  samplingLocation?: string
  // 检验记录扩展字段
  inspectionBasis?: string
  inspectionItem?: string
  inspector?: string
  reviewer?: string
  startTime?: string
  endTime?: string
  relatedInspectionRequestCode?: string
  relatedSamplingCode?: string
  remark?: string
}

export interface ReleaseRoomItem {
  roomId: number
  roomName?: string
  record?: string
  result?: string
}

export interface ReleaseYieldInfo {
  planQty?: number
  finishedQty?: number
  yieldRate?: number
  threshold: number
  pass: boolean
}

export interface ReleaseDeviationItem {
  id: number
  code?: string
  title?: string
  conclusion?: string
  handler?: string
  handleDate?: string
  status?: string
  files: ReleaseFileItem[]
}

export interface ReleaseFileChecklistItem {
  stage?: string
  label?: string
  bizType?: string
  fileKind?: string
  requirement?: 'REQUIRED' | 'CONDITIONAL' | 'OPTIONAL'
  uploaded: number
  total: number
  note?: string
  missingDetails: string[]
}

export interface ReleaseCompliance {
  inspectionPass: boolean
  processPass: boolean
  filePass: boolean
  deviationPass: boolean
  yieldPass: boolean
  canRelease: boolean
  summary?: string
}

export interface ReleaseInfo {
  id?: number | null
  releaseCode?: string
  conclusion?: string
  approver?: string
  reviewTime?: string
  opinion?: string
}

export interface ReleaseBatchDetail {
  batch: ReleaseBatchInfo
  purchases: ReleasePurchaseItem[]
  acceptances: ReleaseAcceptanceItem[]
  instructions: ReleaseDocumentItem[]
  requisitions: ReleaseDocumentItem[]
  returns: ReleaseDocumentItem[]
  processes: ReleaseProcessItem[]
  requests: ReleaseInspectionItem[]
  samplings: ReleaseInspectionItem[]
  inspections: ReleaseInspectionItem[]
  rooms: ReleaseRoomItem[]
  yieldInfo: ReleaseYieldInfo
  deviations: ReleaseDeviationItem[]
  fileChecklist: ReleaseFileChecklistItem[]
  compliance: ReleaseCompliance
  release: ReleaseInfo
}
```

### 5.3 API 模块

```ts
import request from '@/utils/request'

export function fetchReleaseBatches(params: ReleaseBatchQuery) {
  return request.get<{
    items: ReleaseBatchSummary[]
    totalCount: number
    pageIndex: number
    pageSize: number
    totalPages: number
  }>({
    url: '/api/release-review/batches',
    params: {
      ...params,
      pageIndex: Math.max(0, (params.pageIndex ?? 1) - 1)
    }
  })
}

export function fetchReleaseBoardStats(params: Omit<ReleaseBatchQuery, 'pageIndex' | 'pageSize'>) {
  return request.get<ReleaseBoardStats>({
    url: '/api/release-review/batches/stats',
    params
  })
}

export function fetchReleaseVarieties() {
  return request.get<VarietyOption[]>({
    url: '/api/release-review/batches/varieties'
  })
}

export function fetchReleaseBatchDetail(workOrderId: number) {
  return request.get<ReleaseBatchDetail>({
    url: `/api/release-review/batches/${workOrderId}`
  })
}

export function saveReleaseProcessChecks(workOrderId: number, items: Array<{
  executionId: number
  processTypeId?: number
  processName?: string
  requirementSnapshot?: string
  checkResult: ProcessCheckResult
  inspectorId?: number
  inspectorName?: string
  checkDate?: string
  remark?: string
}>) {
  return request.post({
    url: `/api/release-review/batches/${workOrderId}/process-checks`,
    data: { items },
    showSuccessMessage: true
  })
}

export function saveReleaseDraft(workOrderId: number, data: {
  conclusion?: '待补充'
  approver?: string
  reviewDate?: string
  reviewTime?: string
  opinion?: string
}) {
  return request.post<ReleaseBatchDetail>({
    url: `/api/release-review/batches/${workOrderId}/draft`,
    data: { conclusion: '待补充', ...data },
    showSuccessMessage: true
  })
}

export function approveRelease(workOrderId: number, data: {
  approver?: string
  reviewDate?: string
  reviewTime?: string
  opinion?: string
}) {
  return request.post<ReleaseBatchDetail>({
    url: `/api/release-review/batches/${workOrderId}/approve`,
    data,
    showSuccessMessage: true
  })
}

export function rejectRelease(workOrderId: number, data: {
  reviewer?: string
  reviewDate?: string
  reviewTime?: string
  reason: string
}) {
  return request.post<ReleaseBatchDetail>({
    url: `/api/release-review/batches/${workOrderId}/reject`,
    data,
    showSuccessMessage: true
  })
}

export function exportReleaseBatches(data: {
  varietyCode?: string
  batchNo?: string
  status?: string
  keyword?: string
}) {
  return request.post({
    url: '/api/release-review/batches/export',
    data,
    responseType: 'blob'
  })
}
```

### 5.4 交互流程

**进入页面**

```ts
await Promise.all([
  loadStats(),      // fetchReleaseBoardStats（只传品种/批号/关键字）
  loadVarieties(),  // fetchReleaseVarieties
  loadTable()       // fetchReleaseBatches
])
```

**查询 / 重置**

- 查询：`pageIndex = 0`，并行刷新统计和列表。
- 重置：清空三个筛选项，同样 `pageIndex = 0` 后刷新。
- 统计接口忽略 `status`，因此 `status` 变化时统计数字不变是预期行为。

**点击行 / 审核按钮**

1. `workOrderId` 取自当前行。
2. 打开抽屉，`detailLoading = true`，调用 `fetchReleaseBatchDetail`。
3. 用返回的 `detail` 初始化工序检查表单和放行表单。
4. 关闭抽屉时销毁表单状态，避免切换批次时串数据。

**保存工序检查**

```ts
await saveReleaseProcessChecks(workOrderId, formItems)
await Promise.all([
  refreshDetail(),
  loadTable(),
  loadStats()
])
```

**保存草稿**

- 不校验 `canRelease`，但已放行批次禁止操作。
- 提交后直接用返回值覆盖 `detail`，再刷新列表和统计。

**通过放行**

- 按钮禁用条件：`!detail.compliance.canRelease || detail.batch.releaseStatus === '已放行'`。
- 弹二次确认，再调用 `approveRelease`。
- 成功后用返回值刷新详情，并刷新列表、统计。

**拒绝放行**

- 拒绝原因必填，为空时前端先拦截，避免无谓请求。
- 已放行批次禁用按钮。
- 成功后刷新详情、列表、统计。

**导出**

```ts
const blob = await exportReleaseBatches({
  varietyCode: state.filters.varietyCode,
  batchNo: state.filters.batchNo,
  status: state.filters.status || '待审核',
  keyword: ''
})
// 从 response headers 的 content-disposition 解析文件名，失败则用 `审核放行_${today}.csv`
```

### 5.5 刷新规则汇总

| 操作 | 详情 | 列表 | 统计 |
|---|---|---|---|
| 查询 / 重置 | — | ✓ | ✓ |
| 保存工序检查 | ✓ | ✓ | ✓ |
| 保存草稿 | ✓ | ✓ | ✓ |
| 通过放行 | ✓ | ✓ | ✓ |
| 拒绝放行 | ✓ | ✓ | ✓ |

不要只刷新表格：`canRelease`、`blockers`、`releaseStatus`、顶部三个数字都依赖同一套聚合结果。

### 5.6 文件预览与下载

```ts
async function previewFile(fileId: number) {
  const blob = await request.get<Blob>({
    url: `/api/files/preview/${fileId}`,
    responseType: 'blob'
  })
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank')
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

async function downloadFile(fileId: number, fileName: string) {
  const blob = await request.get<Blob>({
    url: `/api/files/${fileId}`,
    responseType: 'blob'
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  URL.revokeObjectURL(url)
}
```

如果项目已有 `BusinessFilePreviewDialog` 或 `src/api/files.ts`，直接复用，不要另写一套。

---

## 6. 原型与后端的差异（重点）

| 原型表现 | 后端实际 | 前端处理 |
|---|---|---|
| 生产指令编号是 `SCZL-...` | `instructions[].code` 是生产计划号 | 直接展示接口值，不要拼接 |
| 退库单有附件数 | `returns[].files` 固定为空 | 显示「无附件」，不要显示 0 个文件按钮 |
| 区域记录显示「清洁合格 / 温湿度合格」 | `rooms[].record` 固定「工序执行区域」，`result` 取工序状态 | 按接口渲染，不写死原型文案 |
| 验收缺失显示「检验报告」 | 实际文案「验收检验报告」 | 使用 `missingKindLabels` |
| 文件清单每行可点开文件 | 清单行没有 `fileId` | 文件预览走证据区 `files[]` |
| 附件显示上传人 | `files[].uploader` 固定空串 | 不展示或显示「—」 |
| 列表统计随筛选联动 | `/stats` 忽略 `status` | 顶部数字不随状态筛选变化属正常 |
| 放行结论是下拉框 | 通过/拒绝/草稿是三个独立接口 | 不要提交「结论」参数到统一接口 |
| 草稿保存后状态不变 | 草稿不改变推导状态 | 刷新后仍可能是「待审核」或「待放行」 |
| 检验记录可点开表单详情 | 聚合接口目前只回传单号、结论、时间、经办人 | 等第 9 节字段扩展；过渡期点开时改调单据详情接口 |
| 已放行后仍可看详情 | 写操作全部返回 400 | 已放行批次所有操作按钮置灰 |

---

## 7. 联调与验收清单

### 联调顺序

1. 启动后端，确认 `GET /api/release-review/batches/stats` 返回 `code = 200`。
2. 用 `8.134.48.54/erp_db` 的测试批次核对列表 `items`、`blockers`、`releaseStatus`。
3. 接列表 + 顶部统计 + 品种下拉。
4. 接详情抽屉，逐个核对 `batch`、`purchases`、`acceptances`、`processes`、`inspection` 分组。
5. 接工序检查保存，确认保存后 `processPass`、`blockers`、`canRelease` 同步变化。
6. 接草稿、通过、拒绝，确认列表状态和统计同步。
7. 最后接 CSV 导出。

### 验收项

- [ ] 列表三个筛选项与接口参数一一对应，查询后页码回到第一页。
- [ ] 品种下拉文案为「名称（编码）」，`value` 使用 `preparationCode`。
- [ ] 列表阻塞项直接渲染 `blockers[]`，为空显示「无阻塞」。
- [ ] 放行状态按 `releaseStatus` 展示，颜色与第 4.1 节一致。
- [ ] 顶部三个数字来自 `/stats`，不随「放行状态」筛选变化。
- [ ] 详情头部字段与 `batch` / `release` 对应，无放行记录时显示「待审核」。
- [ ] 采购、验收、生产指令、领料、退库、工序、检验、偏差各分组能正常渲染空数组。
- [ ] 检验记录明细表按类型展示项目/环节、经办人、时间、状态与结论，空数据有占位。
- [ ] 点击检验相关行能打开表单详情抽屉，字段与第 9 节一致。
- [ ] 验收单缺失类型使用 `missingKindLabels`，不是前端硬编码。
- [ ] 工序结果只提交「符合 / 不符合 / 待查」。
- [ ] 保存工序检查后，详情、列表、统计同时刷新。
- [ ] `compliance.canRelease = false` 时「通过放行」禁用，并展示阻塞原因。
- [ ] 已放行批次不能再次放行、拒绝、保存草稿、修改工序检查。
- [ ] 拒绝原因为空时前端拦截，后端也返回 400。
- [ ] 草稿保存后批次状态不被强制改为「待放行」。
- [ ] 文件预览/下载使用 `fileId`，不依赖 `fileChecklist`。
- [ ] 「不必传」文件不计入缺件数。
- [ ] CSV 导出中文不乱码，文件名解析失败时有兜底。
- [ ] 页面刷新后筛选条件、分页、抽屉状态符合预期。

---

## 8. 已知限制

1. `files[].uploader` 固定为空；需要上传人时需后端补充 `BusinessFileDto` 的创建人信息。
2. `rooms[]` 只聚合工序执行区域，不含清洁、温湿度等 GMP 区域记录；原型中的对应文案暂未落库。
3. `fileChecklist` 只提供计数和缺失明细，不提供 `fileId` 列表；如需清单行直接预览，需要后端扩展 DTO。
4. 列表为先聚合后分页，批次量很大时首屏耗时高于普通单表分页；如成为性能问题，需后端改为数据库分页。
5. 采购单、生产指令不提供制单人/编制人，退库单不提供退库原因；原型与页面均不展示这些字段。

---

## 9. 检验记录表单详情（已按方案 A 实现）

### 9.1 前端原型改了什么

`_analysis/prototypes/release-review.html` 的「检验记录」区块由「单号一行文本」调整为：

1. 顶部汇总：请检单张数、取样记录份数、检验记录份数、批次检验结论。
2. 明细表：类型 / 单据编号 / 检验项目（或环节）/ 经办人 / 时间 / 状态与结论 / 附件 / 操作。
3. 点击任意行或「查看表单」，右侧抽屉展示该单据的完整表单字段与附件。

### 9.2 字段扩展结果

`GET /api/release-review/batches/{workOrderId}` 返回的 `requests[] / samplings[] / inspections[]` 共用 `ReleaseBatchDtos.InspectionItem`，已按类型补齐以下字段：

| 单据 | 已返回字段 |
|---|---|
| 通用 | `id`、`code`、`type`、`status`、`remark`、`relatedTaskCode`、`relatedPlanCode`、`files[]` |
| 请检单 | `process`、`requester`、`requestDepartment`、`requestTime`、`inspectionItems`、`configQuantity` |
| 取样记录 | `sampler`、`samplingTime`、`samplingQuantity`、`samplingCount`、`samplingMethod`、`samplingLocation`、`relatedInspectionRequestCode` |
| 检验记录 | `conclusion`、`inspectionBasis`、`inspectionItem`、`inspector`、`reviewer`、`startTime`、`endTime`、`relatedInspectionRequestCode`、`relatedSamplingCode` |

`status` 的推导规则：

- 请检单：按 `inspection_task` 的 `requestRefId / samplingRefId / inspectionRefId` 推导，返回 `待取样` / `检验中` / `已检验`；没有任务记录时返回 `待检验`。
- 取样记录：固定 `已取样`。
- 检验记录：有结论且 `endTime` 非空为 `已检验`，否则 `检验中`。

`relatedTaskCode` 来自检验任务编号；`relatedPlanCode` 优先取单据自身字段，缺失时回退任务上的计划编号。

### 9.3 实现方式（已完成）

在 `ReleaseBatchDtos.InspectionItem` 上追加字段，保持原有字段不变，`ReleaseBatchServiceImpl.toInspectionItem` 按类型填充。页面仍然只调一次详情接口，点开表单不需要再发请求。

```java
/** 检验相关单据 */
@Data
public static class InspectionItem {
    // 现有字段保持不变
    private Long id;
    private String code;
    private String type;
    private String process;
    private String status;
    private String conclusion;
    private LocalDateTime time;
    private String operator;
    private List<FileItem> files = new ArrayList<>();

    // region 通用扩展
    /** 备注 */
    private String remark;
    /** 关联检验任务编号 */
    private String relatedTaskCode;
    /** 关联检验计划编号 */
    private String relatedPlanCode;
    // endregion

    // region 请检单扩展（InspectionRequest）
    /** 请检人 */
    private String requester;
    /** 请检部门 */
    private String requestDepartment;
    /** 请检时间 */
    private LocalDateTime requestTime;
    /** 检验项目 */
    private String inspectionItems;
    /** 配置数量 */
    private String configQuantity;
    // endregion

    // region 取样记录扩展（SamplingRecord）
    /** 取样人 */
    private String sampler;
    /** 取样时间 */
    private LocalDateTime samplingTime;
    /** 取样量 */
    private String samplingQuantity;
    /** 取样份数 */
    private Integer samplingCount;
    /** 取样方法 */
    private String samplingMethod;
    /** 取样地点 */
    private String samplingLocation;
    // endregion

    // region 检验记录扩展（InspectionRecord）
    /** 检验依据 */
    private String inspectionBasis;
    /** 检验项目 */
    private String inspectionItem;
    /** 检验人 */
    private String inspector;
    /** 复核人 */
    private String reviewer;
    /** 检验开始时间 */
    private LocalDateTime startTime;
    /** 检验结束时间 */
    private LocalDateTime endTime;
    /** 关联请检编号 */
    private String relatedInspectionRequestCode;
    /** 关联取样编号 */
    private String relatedSamplingCode;
    // endregion
}
```

字段来源：

| 单据 | 表 | 字段映射 |
|---|---|---|
| 请检单 | `inspection_request` | `inspection_code`、`process_name`、`requester`、`request_department`、`request_time`、`inspection_items`、`config_quantity`、`remark` |
| 取样记录 | `sampling_record` | `sampling_code`、`sampler`、`sampling_time`、`sampling_quantity`、`sampling_count`、`sampling_method`、`sampling_location`、`remark` |
| 检验记录 | `inspection_record` | `inspection_code`、`inspection_basis`、`inspection_item`、`inspector`、`reviewer`、`start_time`、`end_time`、`conclusion`、`remark`、`related_inspection_request_code`、`related_sampling_code` |

### 9.4 必须明确的三个口径

1. **`status` 怎么来**
   - `inspection_request`、`sampling_record` 实体本身没有 `status` 列，不能直接透传。
   - 建议由后端通过 `inspection_task`（`requestRefId` / `samplingRefId` / `inspectionRefId`）找到任务，用 `taskStatus` 或 `currentStep` 推导，例如：
     - 请检单：任务已到「检验记录」及之后 → `已检验`，否则 `待检验`；
     - 取样记录：`samplingRefId` 有值 → `已取样`；
     - 检验记录：`end_time` 非空且有 `conclusion` → `已检验`，否则 `检验中`。
   - 如果业务上不需要单据状态，前端可以只显示 `conclusion`，但**不要返回 `status: null` 让前端猜**。

2. **取样记录与请检单的关联**
   - `sampling_record` 只有 `related_plan_code`，没有请检单号。
   - 需要「取样 → 请检」关系时，用 `inspection_task` 的 `requestRefId` / `samplingRefId` 反查，一并放进 `relatedInspectionRequestCode`。
   - 检验记录的 `related_inspection_request_code` / `related_sampling_code` 可直接透传。

3. **附件业务类型**
   - 请检单：`INSPECTION_REQUEST`，按 `inspection_request.id` 查。
   - 取样记录：`SAMPLING_RECORD`，按 `sampling_record.id` 查。
   - 检验记录：`INSPECTION_RECORD`，按 `inspection_record.id` 查。
   - 当前 `toInspectionItem` 已经按这三种 `bizType` 取附件，扩展字段时不要改动这段逻辑。

### 9.5 备选方案：点击时调用单据详情接口（不改聚合接口）

如果后端暂时不扩展 `InspectionItem`，前端可以在「查看表单」时按类型调用现有详情接口：

| 类型 | 接口 | 返回 |
|---|---|---|
| 请检单 | `GET /api/inspection-request/{id}` | `InspectionRequest` 全字段 |
| 取样记录 | `GET /api/samplingRecord/{id}` | `SamplingRecord` 全字段 |
| 检验记录 | `GET /api/inspectionRecord/{id}` | `InspectionRecord` 全字段（含工单信息填充） |

这三个接口已经存在，无需后端开发，但存在两个问题：

- 每次点开表单多一次请求，弱网下抽屉会有加载态。
- `status` 和「关联请检单」仍拿不到，需要前端自己推导或隐藏。

因此**优先做 9.3，9.5 只作为过渡**。

### 9.6 前端落地建议

- 明细表优先使用显式字段：`requester`、`sampler`、`inspector`，只有在字段为空时才回退到 `operator`。
- 时间列同理：请检单用 `requestTime`，取样用 `samplingTime`，检验用 `startTime ~ endTime`，回退到 `time`。
- 「查看表单」抽屉里的空值统一显示 `—`，不要展示 `null` / `undefined`。
- 已归档批次也允许查看表单，表单抽屉只读；写操作仍然只在检验管理页办理。
- 附件预览/下载继续使用 `fileId`，不要用单据编号拼文件名。

### 9.7 验收清单

- [ ] 三类单据的 `status` 均不为 `null`，且与检验任务当前环节一致。
- [ ] 请检单能返回请检部门、检验项目、配置数量、备注。
- [ ] 取样记录能返回取样量、份数、方法、地点、备注。
- [ ] 检验记录能返回检验依据、检验项目、复核人、开始时间、备注。
- [ ] 检验记录能返回关联请检单号、关联取样编号。
- [ ] 三类单据的 `files[]` 仍按原 `bizType` 正确返回，不串单。
- [ ] 批次详情接口响应体增量在可接受范围内（单批次通常不超过十余条单据）。

---

## 10. 所有单据「查看表单」的后端支持方式（已按方案 A 实现）

### 10.1 结论

**没有为每个表单新增接口。** 审核放行页的所有单据都挂在同一个批次下，后端已扩展 `GET /api/release-review/batches/{workOrderId}` 的返回字段，采购、验收、生产指令、领料、退库、工序、检验、偏差、放行的表单详情一次返回。

`BatchContext` 原本已加载采购主表和明细、领料主表和明细、验收/退库/工序/检验/偏差主表；本次补充了两类明细的批量查询：

- 验收明细 `AcceptanceDetail`：按 `acceptance_id IN (...)` 一次查询。
- 退库明细 `ReturnOrderDetail`：按 `return_order_id IN (...)` 一次查询。

退库单附件也按 `RETURN_ORDER` + `returnOrder.id` 一并返回。

方案 B（表头随聚合、明细按需查）保留在 10.3 作为备选，仅在响应体明显偏大时启用。

### 10.2 字段与接口矩阵（当前实现）

| 原型区块 | 表单详情 | 数据来源 | 当前实现 |
|---|---|---|---|
| 采购订单 | 表头 + 采购明细行 | `PurchaseOrders` + `PurchaseOrderItems` | `PurchaseItem` 已含 `orderTime/invoiceNo/remark/lines[]`，不新增接口 |
| 验收单 | 表头 + 验收明细行 | `AcceptanceOrder` + `AcceptanceDetail` | `AcceptanceItem` 已含来源、到货/验收时间、验收/复核人、`details[]` |
| 生产指令 | 表头 + 工艺路线 | `ProductionPlan` | `DocumentItem` 已含品种、规格、批号、批量、计划起止、工艺路线、备注 |
| 领料单 | 表头 + 领料明细行 | `ProductionPick` + `ProductionPickDetail` | `DocumentItem` 已含用途、仓库、出库单号、`lines[]` |
| 退库单 | 表头 + 退库明细行 + 附件 | `ReturnOrder` + `ReturnOrderDetail` | `DocumentItem` 已含关联出库单、`lines[]`，附件按 `RETURN_ORDER` 返回 |
| 工序记录 | 工序执行表头 + 参数 | `WorkOrderProcessExecution` | `ProcessItem` 已含房间、设备、备注 |
| 检验记录 | 见第 9 节 | `InspectionRequest/SamplingRecord/InspectionRecord` | `InspectionItem` 已补齐状态和表单字段 |
| 偏差记录 | 表头 + 处理过程 | `DeviationRecord` | `DeviationItem` 已含发生时间、工序、描述、措施、备注 |
| 放行审核 | 表头 | `ReleaseReview` | `release` 原本已完整，无需改动 |

采购单和退库单的两个字段说明：

- 采购制单人、生产指令编制人、退库原因均不在接口返回中，页面不展示；退库说明使用 `remark`。

### 10.3 如果选择「表头随聚合、明细按需查」

已有可复用的详情接口：

| 单据 | 现有接口 | 是否含明细 |
|---|---|---|
| 验收单 | `GET /api/acceptance/{id}` + `GET /api/acceptance/{id}/details` | 是 |
| 领料单 | `GET /api/material-requisition-slip/{id}` | 是 |
| 退库单 | `GET /api/warehouse/return-orders/{id}` | 是 |
| 工序记录 | `GET /api/work-order-process-execution/by-work-order/{workOrderId}` | 工序执行列表 |
| 请检单 | `GET /api/inspection-request/{id}` | 表头 |
| 取样记录 | `GET /api/samplingRecord/{id}` | 表头 |
| 检验记录 | `GET /api/inspectionRecord/{id}` | 表头 |
| 偏差记录 | `GET /api/deviation/{id}` | 表头 |
| 采购订单 | **无按 ID 详情接口** | 需新增 `GET /api/purchase-orders/{id}`，返回主表 + 明细 |
| 生产指令 | **无按 ID 详情接口** | 仅表头时随聚合返回即可；需要计划明细时再加 `GET /api/production-plans/{id}` |

### 10.4 不建议的做法

1. **不要做一个通用的 `/api/release-review/documents/{type}/{id}` 转发接口**：会把各业务的 DTO、权限、校验再复制一遍，后续每个模块改字段都要同步维护。
2. **不要把附件 `fileId` 列表塞进文件完整性清单**：附件已经按单据返回，前端直接按 `fileId` 预览即可。
3. **不要为「查看表单」再查一次列表接口**：例如用 `search-with-details` 按 ID 过滤，返回结构是分页列表，解析成本高于新增一个详情接口或直接扩展聚合字段。

### 10.5 实施记录（已完成）

1. 已扩展 `ReleaseBatchDtos` 各分组的表头、明细与附件字段。
2. `BatchQueryServiceImpl` 已补查验收明细、退库明细和检验任务，并补退库单附件。
3. `ReleaseBatchServiceImpl` 已按单据类型填充明细和状态。
4. 前端按第 10.2 节字段渲染各表单抽屉。
5. 上线后观察单批次详情响应体；若明显偏大（例如超过 300KB），再按 10.3 把明细行改为按需接口，表头字段仍保留在聚合接口中。

### 10.6 验收口径

- [ ] 采购、验收、生产指令、领料、退库、工序、检验、偏差、放行九类表单均可点开查看。
- [ ] 每类表单的表头字段与业务单据详情页一致，空值显示 `—`。
- [ ] 采购、验收、领料、退库表单能看到明细行；无明细时显示「无明细」。
- [ ] 退库单能返回附件；没有附件时显示「无附件」。
- [ ] 点开表单不产生 N+1 请求（方案 A），或每个表单最多一次详情请求（方案 B）。
- [ ] 已放行、已拒绝批次的表单抽屉保持只读，不出现编辑按钮。


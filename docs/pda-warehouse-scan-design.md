# 仓库物料管理与 PDA 扫码 · 详细设计

| 项 | 内容 |
|---|---|
| 依据 | 《仓库物料管理与 PDA 扫码 · 功能 PRD》V1.0 |
| 版本 | V1.0（详细设计） |
| 日期 | 2026-11-13 |
| 范围 | 物料验收入库、生产领料出库、库管自建出库、退库、成品入库；调拨与盘点本期不扫码 |
| 说明 | 本文按 PRD 3.2「字段级生成规则由详细设计确定」给出可落地的码规则、表结构、接口、状态机与改造点；替代 2026-11-13 初版通用方案 |

---

## 0. 设计结论

| 事项 | 落地口径 |
|------|---------|
| 操作单元 | **入库码**（一个物料批次一个码）；**包码**（一张来源单据一个码，聚合该单全部入库码） |
| 物料码 | 直接使用 `material.material_code`（唯一），不参与扫码；如需独立印刷值再扩展 `material.barcode` |
| 入库码生成时机 | 单据进入「待打印入库码」时生成：采购入库=验收双签完成；成品入库=检验放行完成 |
| 扫码的作用 | 只记录「这一行已被扫码核对」，**不改数量、不改库存** |
| 库存变动 | 只有「整单确认」一次性执行，且必须所有行扫满 |
| 包码 | 整包作业；不支持拆包；部分出库必须先补打单件入库码，再逐条扫 |
| 单据状态 | 未扫码/未扫满的单据停留在待入库或草稿，库存不变 |
| 电子签名 | 验收人、复核人双签（缺一不可）+ 库管作业签名；签名不可改，退回上一步即失效留痕 |
| PDA 设备 | 先登记后使用，设备与使用人绑定；未登记设备不能作业 |
| 网络 | 仅在线；断网本地暂存扫码记录，恢复后继续；服务端幂等，不重复计数 |
| PC 现有页面 | **保留现有录单方式**：保存后进入 `待入库/草稿`，必须手动点「确认入库/出库/退库」才改库存；PDA 与 PC 口径一致 |
| 明细行扫码 | 支持行级扫码：PC 明细行「扫码」按钮 + PDA 逐条扫入库码，共用同一个扫码服务；每行扫码状态写入 `scan_status` |
| 与现有系统 | 新增 `/api/pda/**` 编排层；库存增减仍调用 `StockService.applyInbound/applyOutbound`，不另写库存逻辑 |

**一句话**：把现有「保存即入库/出库」改成「先建待入库单与码 → 扫码核对 → 手动确认才动库存」；PC 与 PDA 共用同一套状态和库存入口，区别只在交互载体。

---

## 1. 现状与差距

### 1.1 现有能力（已核实）

| 能力 | 位置 | 现状 |
|------|------|------|
| 验收单状态与明细流转 | `Common/utils/AcceptanceStatusPolicy.java` | 运输中/验收中/已入库/已结束/已作废；明细待初验→待检验→待入库→已入库 |
| 验收整单入库 | `AcceptanceOrderServiceImpl.inbound` / `applyAcceptanceInbound` | 点击即生成入库单（已入库）并立即加库存 |
| 入库保存 | `StockInServiceImpl.addStockIn` | 新增即「已入库」，立即联动库存 |
| 入库确认/取消 | `StockInServiceImpl.confirmStockIn` / `cancelStockIn` | 确认仅接受「草稿」；取消支持回滚库存 |
| 出库保存 | `StockOutServiceImpl.addStockOut` | 新增即「已出库」，立即扣库存 |
| 草稿出库 | `createDraftOutbound` / `createDraftOutboundWithDetails` | 生产领料已用草稿；`confirmStockOut` 才扣库存 |
| 生产领料 | `ProductionPickServiceImpl.issuePick` | FIFO 分配批次，生成草稿出库单，不扣库存 |
| 退库 | `ReturnOrderServiceImpl.createReturnOrder` | 创建即回增库存；退库单无状态字段 |
| 成品入库 | `StockInServiceImpl.createFinishedStockInbound` | 放行通过即生成「已入库」单并加库存 |
| PC 端确认能力 | `StockInController` / `StockOutController` 的 `/{id}/confirm` | 接口目前被注释；Service 已有 `confirmStockIn/confirmStockOut`，但 PC 主流程未使用 |
| 操作日志/字段级审计 | `@OperationLog`、`DataChangeInterceptor` | 可复用，但无设备号字段 |

> **结论**：目前 PC 端入库、出库、退库、成品入库全部是「保存即生效」，没有中间状态。要满足 PRD「扫码前库存不变」，必须引入状态，并把「保存」与「确认」拆成两个动作；PC 页面需增加状态列与确认按钮，PDA 端则在同一状态上完成扫码与确认。

### 1.2 关键差距

1. **没有码**：`material`、`stock`、`stock_in_detail` 均无码字段，全项目无扫码相关代码。
2. **过早生效**：验收、成品入库、直接出库、退库都是保存即改库存，无法满足「扫码前库存不变」。
3. **没有扫码会话**：无法记录「哪一行已扫、扫了哪个码、谁扫的、哪台设备」，也无法撤销上一条。
4. **没有电子签名**：验收单只有 `acceptor_name`/`reviewer_name` 文本，没有账号、时间、签名事件与失效记录。
5. **没有设备登记**：无 `pda_device`，无法限制未登记设备作业。
6. **权限未落后端**：权限是数据驱动的菜单/按钮，后端接口未做方法级校验。
7. **退库单无状态**：`return_order` 没有 `return_status`，无法实现「草稿→已退库」。
8. **来源回写无状态**：验收取消对采购单、成品入库取消对放行记录的回写失败无法标记「待处理」。

### 1.3 兼容策略

| 现有值 | 处理 |
|--------|------|
| 验收单 `已作废` | **不改字面量**，PDA/PC 显示为「已取消」；新增 `待入库` 主状态 |
| 入库单 `草稿` | 保留，兼容历史手工单；新流程使用 `待入库`，`confirmStockIn` 同时接受两者 |
| 出库单 `草稿` | 直接复用，新流程统一走草稿→确认 |
| 退库单无状态 | 新增 `return_status`，存量数据默认 `已退库`（历史已回增库存） |
| 成品入库单 `已入库` | 存量保持；新流程改为 `待入库`，展示为「待确认」 |
| 存量库存无码 | 上线前执行补码脚本，为 `quantity > 0` 的库存批次生成入库码（见 11.2） |

---

## 2. 领域模型

### 2.1 实体关系

```
来源单据（验收单 / 成品放行）
        │ 1
        ▼
   stock_in（待入库）
        │ 1
        ├──────────────► stock_in_detail（每个物料批次一行）
        │                        │ 1
        │                        ▼
        │                  inbound_code（入库码，1 行 1 码）
        │                        │ 0..1（确认后）
        │                        ▼
        │                      stock（库存批次，1 码 1 行）
        │
        └──────────────► package_code（包码，1 单 1 码）
                                 │ 1
                                 ▼ N
                           inbound_code

PDA 作业：
pda_scan_session（一次整单作业）
   ├─ pda_scan_action（一次扫码手势=一个动作组；整包展开为多条动作）
   │      └─ pda_scan_record（动作展开到每个码的明细，追加不可删）
   ├─ sys_esign_record（作业签名）
   └─ pda_request_log（提交幂等）
```

### 2.2 核心不变量

1. 一个 `stock_in_detail` 有且仅有一个 `inbound_code`（`uk_inbound_code_detail`）。
2. 一个 `inbound_code` 最多对应一条 `stock`（`stock_id` 可空，确认后写入）。
3. 一个来源单据（验收单/成品入库单）有且仅有一个 `package_code`。
4. 包码只聚合本单的入库码，不跨单。
5. `inbound_code.code` 与 `package_code.code` 全局唯一、生成后不可修改、作废不复用。
6. 扫码记录只追加，不删除；撤销通过动作标记与撤销动作表达。
7. 库存数量只由「整单确认」和「取消回滚」改变；扫码、撤销扫码不改库存。
8. 部分领用只扣 `inbound_code.remaining_qty` 与对应 `stock.quantity`，不拆分码。

### 2.3 入库码与库存批次的关系

`stock` 唯一键为 `(item_code, prod_unit_id, batch_number, stock_in_id, stock_status)`，即「同一入库单内同物料同批号同仓库同状态」只有一行。入库码按 `stock_in_detail` 生成，而入库明细在保存前会按相同合并键合并（`StockInDetailMergeUtils`），因此：

**1 个入库码 ↔ 1 条入库明细 ↔ 1 条库存批次**，三者可以稳定互相追溯。

`stock_transaction` 增加 `inbound_code_id` 后，码下即可直接查到入库、出库、退库、调整的全部流水，满足 PRD 3.5「码下挂库存流水」。

---

## 3. 编码规则

### 3.1 物料码

- 取值：`material.material_code`（已有唯一索引 `material_code`）。
- 格式：沿用档案编码，如 `F030`、`Z046`。
- 生成：物料建档时由现有逻辑生成/录入，本方案不新增。
- 用途：档案标识与标签展示，**不参与扫码作业**。
- 可选扩展：若供应商来料条码需要映射，增加 `material.barcode`（唯一，可空），扫码时作为物料码的别名解析。

### 3.2 入库码

**格式**

```
in + yyyyMMdd + 5 位日序号
示例：in2026111300001
```

- 前缀 `in` 小写，与 PRD 示例 `in99231` 风格一致；PRD 示例为示意，落地加日期段保证跨天唯一、可读。
- 日序号从 `00001` 开始，每天归零；由序列表原子分配。
- 长度 15，Code128/QR 均可承载。

**生成时机**

| 来源 | 触发点 | 生成对象 |
|------|--------|---------|
| 采购入库 | 验收人+复核人双签完成，且全部明细终结（含待入库） | 该验收单待入库明细逐行生成 |
| 成品入库 | 检验放行通过，生成成品入库单时 | 成品入库单明细逐行生成 |
| 存量补码 | 上线补码任务 | `stock.quantity > 0` 的库存批次逐行生成 |

**生成内容**

每条入库码在生成时冻结以下字段（后续库存变化不回写，保证标签与追溯稳定）：

- 物品：`item_type/item_id/item_code/item_name/category_name/unit_name/spec`
- 批次：`batch_number/production_date/expiry_date`
- 数量：`inbound_qty`（生成时=入库明细数量），`remaining_qty` 初始为 0，确认入库时置为 `inbound_qty`
- 来源：`source_type/source_doc_id/source_doc_no/acceptance_id/acceptance_detail_id/stock_in_detail_id`
- 位置：`prod_unit_id/storage_location`（库位由 PDA 录入，可空，确认前可改）
- 用途：`purpose`（该批物料用于生产的制剂品种，取自验收单/入库单）
- 到货：`arrival_date`（验收单确认到货日期；无则取入库日期）

**状态**

| 状态 | 含义 | 进入条件 | 离开条件 |
|------|------|---------|---------|
| `待入库` | 已生成码，库存未生效 | 生成时 | 整单确认→`在库`；取消→`已作废` |
| `在库` | 已确认入库，有剩余 | 确认入库 | 剩余为 0→`已耗尽`；取消入库→`已作废` |
| `已耗尽` | 剩余数量为 0 | 出库扣完 | 退库回增→`在库` |
| `已作废` | 单据取消/码作废 | 取消、退回上一步 | 终态，不复用 |

**不可变规则**

- `code`、`inbound_qty`、来源字段生成后不可修改。
- 允许修改：`storage_location`（PDA 录入/PC 编辑）、`remaining_qty`（业务扣减/回增）、`code_status`、打印字段。
- 补打只更新 `print_count/last_printed_*`，不生成新码。

### 3.3 包码

**格式**

```
pk + yyyyMMdd + 5 位日序号
示例：pk2026111300001
```

**生成规则**

- 一张来源单据生成一个包码：验收单按 `acceptance_id`，成品入库单按 `stock_in_id`。
- 包码生成时，把该单据下所有 `inbound_code.package_id` 指向它，并汇总 `item_count`（入库码数）与 `total_qty`。
- 包码不单独拆分明细表，包内明细通过 `inbound_code.package_id` 反查。
- 包码状态：

| 状态 | 含义 | 条件 |
|------|------|------|
| `待入库` | 包内码尚未入库 | 生成时 |
| `在库` | 包内全部码已入库且未被单独出库 | 整单确认后 |
| `部分出库` | 包内任一码被逐条出库/部分领用 | 逐条出库发生时 |
| `已耗尽` | 包内全部码剩余为 0 | 全部扣完 |
| `已作废` | 来源单据取消 | 取消时 |

**整包规则**

- 整包作业要求「单据行 = 包内全部码」，数量、物料、批次完全一致；否则拦截。
- 包状态为 `部分出库` 时禁止整包扫码，提示「包已拆分，请逐条扫入库码」。
- 补打单件入库码不改变包状态；只有逐条出库导致包内码数量变化时才置 `部分出库`。

### 3.4 二维码内容与校验

| 码 | 二维码内容 | 说明 |
|----|-----------|------|
| 入库码 | `ERP1\|IC\|in2026111300001\|{CRC16}` | `IC`=Inbound Code |
| 包码 | `ERP1\|PK\|pk2026111300001\|{CRC16}` | `PK`=Package |
| 物料码 | `ERP1\|MT\|F030\|{CRC16}` | 可选，仅档案标签 |

- `CRC16` 为 CRC16-CCITT 的 4 位大写十六进制，覆盖前缀到最后一个业务字段。
- 解析失败统一提示「条码校验不通过，请重新扫描」，不进入业务匹配。
- 人读区打印：物料名称、物料编码、批号、入库日期、有效期至、入库数量、用途、码值；包标签另打印用途、入库时间、品种数量与详单。

### 3.5 生成与并发

新增序列表 `biz_code_sequence`，按 `(biz_type, seq_date)` 原子取号：

```sql
-- 事务内
SELECT current_value FROM biz_code_sequence
 WHERE biz_type = 'INBOUND_CODE' AND seq_date = CURDATE() FOR UPDATE;
-- 不存在则 INSERT ... ON DUPLICATE KEY UPDATE
UPDATE biz_code_sequence SET current_value = current_value + 1
 WHERE biz_type = 'INBOUND_CODE' AND seq_date = CURDATE();
```

- 取号与插入在同一事务；唯一索引 `uk_inbound_code` / `uk_package_code` 兜底。
- 捕获 `DuplicateKeyException` 重试（最多 3 次），避免并发取号碰撞。
- 禁止用 `SELECT MAX(code)+1`（现有单号生成方式）用于码，避免并发重复。

---

## 4. 数据模型

> 所有新表沿用项目约定：`created_by/created_time/updated_by/updated_time/is_deleted/version`（追加型审计表除外）。

### 4.1 新增：入库码 `inbound_code`

```sql
CREATE TABLE `inbound_code` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '入库码主键',
  `code`                VARCHAR(32)  NOT NULL COMMENT '入库码（全局唯一，不可修改、不可复用）',
  `qr_content`          VARCHAR(128) NOT NULL COMMENT '二维码内容（含校验位）',
  `package_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '所属包码ID',
  `source_type`         VARCHAR(20)  NOT NULL COMMENT '来源：采购入库/成品入库/存量补码',
  `source_doc_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '来源入库单ID（存量补码可为空）',
  `source_doc_no`       VARCHAR(50)  DEFAULT NULL COMMENT '来源入库单号（存量补码可为空）',
  `acceptance_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '验收单ID（采购入库）',
  `acceptance_detail_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '验收明细ID',
  `stock_in_detail_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '入库明细ID（新流程1:1；存量补码可空）',
  `stock_id`            BIGINT UNSIGNED DEFAULT NULL COMMENT '库存批次ID（确认入库后写入）',
  `item_type`           VARCHAR(20)  NOT NULL COMMENT '物品类型：material/preparation',
  `item_id`             BIGINT DEFAULT NULL COMMENT '物品ID',
  `item_code`           VARCHAR(50)  NOT NULL COMMENT '物品编码',
  `item_name`           VARCHAR(100) NOT NULL COMMENT '物品名称',
  `category_name`       VARCHAR(50)  DEFAULT NULL COMMENT '分类',
  `unit_name`           VARCHAR(50)  DEFAULT NULL COMMENT '单位',
  `spec`                VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `batch_number`        VARCHAR(100) NOT NULL COMMENT '批号',
  `production_date`     DATE DEFAULT NULL COMMENT '生产日期',
  `expiry_date`         DATE DEFAULT NULL COMMENT '有效期至',
  `inbound_qty`         DECIMAL(20,8) NOT NULL COMMENT '入库数量（生成时冻结）',
  `remaining_qty`       DECIMAL(20,8) NOT NULL DEFAULT 0 COMMENT '剩余数量（确认后=入库数量，出库扣减，退库回增）',
  `prod_unit_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '仓库（生产单位ID）',
  `storage_location`    VARCHAR(100) DEFAULT NULL COMMENT '库位（PDA录入，PC可编辑）',
  `purpose`             VARCHAR(200) DEFAULT NULL COMMENT '用途：用于生产的制剂品种',
  `arrival_date`        DATE DEFAULT NULL COMMENT '到货日期',
  `code_status`         VARCHAR(20)  NOT NULL DEFAULT '待入库' COMMENT '待入库/在库/已耗尽/已作废',
  `print_status`        TINYINT NOT NULL DEFAULT 0 COMMENT '0未打印/1已打印',
  `print_count`         INT NOT NULL DEFAULT 0 COMMENT '打印次数（含补打）',
  `last_printed_by`     BIGINT UNSIGNED DEFAULT NULL COMMENT '最后打印人',
  `last_printed_time`   DATETIME DEFAULT NULL COMMENT '最后打印时间',
  `remark`              VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`          BIGINT UNSIGNED NOT NULL COMMENT '创建人',
  `created_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by`          BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  `updated_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`          TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（码不删除，仅作废）',
  `version`             INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inbound_code` (`code`),
  UNIQUE KEY `uk_inbound_code_detail` (`stock_in_detail_id`),
  KEY `idx_ic_package` (`package_id`),
  KEY `idx_ic_stock` (`stock_id`),
  KEY `idx_ic_item_batch` (`item_code`,`batch_number`),
  KEY `idx_ic_status` (`code_status`),
  KEY `idx_ic_source_doc` (`source_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库码（一个物料批次一个码）';
```

> `uk_inbound_code_detail` 保证新流程「一明细一码」；MySQL 唯一索引允许多个 NULL，存量补码行（`stock_in_detail_id` 为空）不受此约束，由服务层按 `stock_id` 判重。
> 作废不删除行，避免软删除与唯一键冲突。

### 4.2 新增：包码 `package_code`

```sql
CREATE TABLE `package_code` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '包码主键',
  `code`           VARCHAR(32)  NOT NULL COMMENT '包码（全局唯一）',
  `qr_content`     VARCHAR(128) NOT NULL COMMENT '二维码内容（含校验位）',
  `source_type`    VARCHAR(20)  NOT NULL COMMENT '采购入库/成品入库',
  `source_doc_id`  BIGINT UNSIGNED NOT NULL COMMENT '来源单据ID（验收单ID或成品入库单ID）',
  `source_doc_no`  VARCHAR(50)  NOT NULL COMMENT '来源单号',
  `acceptance_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '验收单ID（采购）',
  `purpose`        VARCHAR(200) DEFAULT NULL COMMENT '用途：用于生产的制剂品种',
  `item_count`     INT NOT NULL DEFAULT 0 COMMENT '包内入库码数量',
  `total_qty`      DECIMAL(20,8) NOT NULL DEFAULT 0 COMMENT '包内入库总量',
  `package_status` VARCHAR(20) NOT NULL DEFAULT '待入库' COMMENT '待入库/在库/部分出库/已耗尽/已作废',
  `print_status`   TINYINT NOT NULL DEFAULT 0 COMMENT '0未打印/1已打印',
  `print_count`    INT NOT NULL DEFAULT 0 COMMENT '打印次数（含补打）',
  `last_printed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后打印人',
  `last_printed_time` DATETIME DEFAULT NULL COMMENT '最后打印时间',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`     BIGINT UNSIGNED NOT NULL,
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`     BIGINT UNSIGNED DEFAULT NULL,
  `updated_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`     TINYINT NOT NULL DEFAULT 0,
  `version`        INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_code` (`code`),
  UNIQUE KEY `uk_package_source` (`source_type`,`source_doc_id`),
  KEY `idx_pk_status` (`package_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包码（一张来源单据一个包）';
```

### 4.3 新增：扫码会话 `pda_scan_session`

```sql
CREATE TABLE `pda_scan_session` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话主键',
  `session_no`      VARCHAR(40) NOT NULL COMMENT '作业会话号（唯一）',
  `biz_type`        VARCHAR(20) NOT NULL COMMENT 'INBOUND/OUTBOUND/RETURN',
  `biz_id`          BIGINT UNSIGNED NOT NULL COMMENT '业务单据ID（stock_in/out/return）',
  `source_type`     VARCHAR(20) DEFAULT NULL COMMENT 'ACCEPTANCE/FINISHED/MANUAL/PRODUCTION_PICK',
  `warehouse_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '作业仓库',
  `session_status`  VARCHAR(20) NOT NULL DEFAULT 'SCANNING' COMMENT 'SCANNING/CONFIRMED/CANCELLED/EXPIRED',
  `device_no`       VARCHAR(64) NOT NULL COMMENT 'PDA设备编号',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '作业人',
  `started_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活动时间',
  `confirmed_time`  DATETIME DEFAULT NULL COMMENT '确认时间',
  `work_sign_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '作业签名记录ID',
  `request_id`      VARCHAR(64) DEFAULT NULL COMMENT '确认请求幂等ID',
  `remark`          VARCHAR(500) DEFAULT NULL,
  `version`         INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  UNIQUE KEY `uk_session_request` (`request_id`),
  KEY `idx_session_biz` (`biz_type`,`biz_id`,`session_status`),
  KEY `idx_session_user` (`user_id`,`started_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA扫码作业会话';
```

> `request_id` 唯一且允许多个 NULL，未确认时为空。

### 4.4 新增：扫码动作 `pda_scan_action`

```sql
CREATE TABLE `pda_scan_action` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `action_no`     VARCHAR(40) NOT NULL COMMENT '动作号（每个码一条，唯一）',
  `action_group_no` VARCHAR(40) NOT NULL COMMENT '动作组号（一次扫码手势一个组；整包扫码展开为多条动作共享组号，撤销以组为单位）',
  `session_id`    BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `action_type`   VARCHAR(20) NOT NULL COMMENT 'CODE/PACKAGE/MANUAL/UNDO',
  `scan_result`   VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAIL',
  `message`       VARCHAR(500) DEFAULT NULL COMMENT '失败原因/提示',
  `code_type`     VARCHAR(20) DEFAULT NULL COMMENT 'INBOUND/PACKAGE',
  `code_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '码ID（NULL 表示未识别的失败扫码）',
  `code_value`    VARCHAR(64) DEFAULT NULL COMMENT '扫描原文',
  `revoked`       TINYINT NOT NULL DEFAULT 0 COMMENT '0有效/1已撤销',
  `revoked_by`    BIGINT UNSIGNED DEFAULT NULL COMMENT '撤销人',
  `revoked_time`  DATETIME DEFAULT NULL COMMENT '撤销时间',
  `revoke_reason` VARCHAR(200) DEFAULT NULL COMMENT '撤销原因',
  `undo_of_group_no` VARCHAR(40) DEFAULT NULL COMMENT '撤销的目标动作组号',
  `operator_id`   BIGINT UNSIGNED NOT NULL COMMENT '操作人',
  `device_no`     VARCHAR(64) NOT NULL COMMENT '设备号',
  `scan_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
  `active_uniq`   VARCHAR(80) GENERATED ALWAYS AS (
                    IF(`revoked` = 0 AND `code_id` IS NOT NULL,
                       CONCAT(`session_id`, '#', `code_id`), NULL)
                  ) STORED COMMENT '有效扫码唯一键（撤销后置NULL）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_action_no` (`action_no`),
  UNIQUE KEY `uk_scan_active` (`active_uniq`),
  KEY `idx_action_session` (`session_id`,`scan_time`),
  KEY `idx_action_group` (`action_group_no`),
  KEY `idx_action_code` (`code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA扫码动作（整包展开为多条，撤销以组为单位）';
```

### 4.5 新增：扫码明细 `pda_scan_record`

```sql
CREATE TABLE `pda_scan_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id`    BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `action_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属动作ID',
  `action_group_no` VARCHAR(40) NOT NULL COMMENT '动作组号（与动作表一致，便于按组汇总/撤销）',
  `biz_line_id`   BIGINT UNSIGNED NOT NULL COMMENT '业务明细ID（stock_in_detail/stock_out_detail/return_detail）',
  `code_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '入库码ID',
  `code_value`    VARCHAR(64) DEFAULT NULL COMMENT '码值',
  `expected_qty`  DECIMAL(20,8) DEFAULT NULL COMMENT '该行应扫数量',
  `allocated_qty` DECIMAL(20,8) DEFAULT NULL COMMENT '本次扫码分配数量',
  `scan_source`   VARCHAR(20) NOT NULL COMMENT 'CODE/PACKAGE/MANUAL',
  `scan_result`   VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAIL',
  `message`       VARCHAR(500) DEFAULT NULL,
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间（追加不可删）',
  PRIMARY KEY (`id`),
  KEY `idx_record_session` (`session_id`,`biz_line_id`),
  KEY `idx_record_action` (`action_id`),
  KEY `idx_record_group` (`action_group_no`),
  KEY `idx_record_code` (`code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA扫码明细（追加型审计）';
```

> 本表不提供更新/删除接口；撤销通过 `pda_scan_action.revoked` 表达。

### 4.6 新增：PDA 设备 `pda_device`

```sql
CREATE TABLE `pda_device` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `device_no`        VARCHAR(64) NOT NULL COMMENT '设备编号（唯一）',
  `device_name`      VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  `device_model`     VARCHAR(100) DEFAULT NULL COMMENT '型号',
  `device_status`    TINYINT NOT NULL DEFAULT 1 COMMENT '0停用/1启用',
  `bound_user_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '绑定使用人',
  `bound_user_name`  VARCHAR(50) DEFAULT NULL COMMENT '绑定使用人姓名',
  `bound_time`       DATETIME DEFAULT NULL COMMENT '绑定时间',
  `registered_by`    BIGINT UNSIGNED NOT NULL COMMENT '登记人（主管）',
  `registered_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
  `last_login_time`  DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `remark`           VARCHAR(500) DEFAULT NULL,
  `created_by`       BIGINT UNSIGNED NOT NULL,
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`       BIGINT UNSIGNED DEFAULT NULL,
  `updated_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`       TINYINT NOT NULL DEFAULT 0,
  `version`          INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_no` (`device_no`),
  KEY `idx_device_user` (`bound_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA设备登记与使用人绑定';
```

### 4.7 新增：电子签名 `sys_esign_record`

```sql
CREATE TABLE `sys_esign_record` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `biz_type`         VARCHAR(30) NOT NULL COMMENT 'ACCEPTANCE/STOCK_IN/STOCK_OUT/RETURN/CANCEL',
  `biz_id`           BIGINT UNSIGNED NOT NULL COMMENT '业务单据ID',
  `biz_no`           VARCHAR(50) DEFAULT NULL COMMENT '业务单号',
  `sign_stage`       VARCHAR(20) NOT NULL COMMENT 'ACCEPT/REVIEW/WORK/CANCEL',
  `event_type`       VARCHAR(20) NOT NULL COMMENT 'SIGN/INVALIDATE',
  `related_sign_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '失效事件指向的原签名ID',
  `sign_user_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '签名人ID',
  `sign_user_account` VARCHAR(50) DEFAULT NULL COMMENT '签名人账号',
  `sign_user_name`   VARCHAR(50) DEFAULT NULL COMMENT '签名人姓名',
  `sign_time`        DATETIME DEFAULT NULL COMMENT '签名时间',
  `auth_method`      VARCHAR(20) DEFAULT NULL COMMENT 'PASSWORD/FINGERPRINT/CA',
  `sign_hash`        VARCHAR(64) DEFAULT NULL COMMENT '签名内容摘要 SHA-256',
  `device_no`        VARCHAR(64) DEFAULT NULL COMMENT 'PDA设备号',
  `client_ip`        VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `reason`           VARCHAR(500) DEFAULT NULL COMMENT '失效/取消原因',
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_esign_biz` (`biz_type`,`biz_id`,`sign_stage`),
  KEY `idx_esign_user` (`sign_user_id`,`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子签名事件（追加型）';
```

### 4.8 新增：异常与手工录入 `pda_exception_log`

```sql
CREATE TABLE `pda_exception_log` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `biz_type`        VARCHAR(30) NOT NULL COMMENT 'INBOUND/OUTBOUND/RETURN/LABEL',
  `biz_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '业务单据ID',
  `biz_line_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '业务明细ID',
  `exception_type`  VARCHAR(30) NOT NULL COMMENT 'MANUAL_ENTRY/LABEL_BROKEN/DEVICE_FAILURE/OTHER',
  `reason`          VARCHAR(500) NOT NULL COMMENT '原因（必填）',
  `content_json`    JSON DEFAULT NULL COMMENT '补充内容（手工录入的码/数量等）',
  `authorized_by`   BIGINT UNSIGNED DEFAULT NULL COMMENT '授权主管ID',
  `authorized_time` DATETIME DEFAULT NULL COMMENT '授权时间',
  `operator_id`     BIGINT UNSIGNED NOT NULL COMMENT '操作人',
  `operator_name`   VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `device_no`       VARCHAR(64) DEFAULT NULL COMMENT '设备号',
  `created_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exception_biz` (`biz_type`,`biz_id`),
  KEY `idx_exception_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA异常与手工录入审计';
```

### 4.9 新增：提交幂等 `pda_request_log`

```sql
CREATE TABLE `pda_request_log` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `request_id`   VARCHAR(64) NOT NULL COMMENT '客户端请求ID（UUID）',
  `device_no`    VARCHAR(64) DEFAULT NULL,
  `user_id`      BIGINT UNSIGNED DEFAULT NULL,
  `biz_type`     VARCHAR(30) NOT NULL COMMENT 'INBOUND/OUTBOUND/RETURN/CANCEL',
  `biz_id`       BIGINT UNSIGNED DEFAULT NULL,
  `biz_code`     VARCHAR(64) DEFAULT NULL,
  `result_json`  JSON DEFAULT NULL COMMENT '首次结果，重复请求原样返回',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pda_request` (`request_id`),
  KEY `idx_pda_request_user` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA提交幂等记录';
```

### 4.10 新增：码序列 `biz_code_sequence`

```sql
CREATE TABLE `biz_code_sequence` (
  `biz_type`      VARCHAR(30) NOT NULL COMMENT 'INBOUND_CODE/PACKAGE_CODE',
  `seq_date`      DATE NOT NULL COMMENT '日期',
  `current_value` INT NOT NULL DEFAULT 0 COMMENT '当前序号',
  `updated_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_type`,`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务码日序号';
```

### 4.11 新增：标签打印日志 `pda_label_print_log`

```sql
CREATE TABLE `pda_label_print_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code_type`     VARCHAR(20) NOT NULL COMMENT 'INBOUND/PACKAGE',
  `code_id`       BIGINT UNSIGNED NOT NULL COMMENT '码ID',
  `code_value`    VARCHAR(64) NOT NULL COMMENT '码值',
  `print_type`    VARCHAR(20) NOT NULL COMMENT 'FIRST/REPRINT',
  `reason`        VARCHAR(500) DEFAULT NULL COMMENT '补打原因（补打必填）',
  `operator_id`   BIGINT UNSIGNED NOT NULL COMMENT '操作人',
  `operator_name` VARCHAR(50) DEFAULT NULL,
  `device_no`     VARCHAR(64) DEFAULT NULL,
  `print_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_print_code` (`code_type`,`code_id`),
  KEY `idx_print_time` (`print_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签打印/补打审计';
```

### 4.12 现有表变更

```sql
-- 验收单：用途、签名状态、码生成时间、取消与回写
ALTER TABLE `acceptance_order`
  ADD COLUMN `purpose` VARCHAR(200) DEFAULT NULL COMMENT '用途：用于生产的制剂品种',
  ADD COLUMN `sign_status` VARCHAR(20) NOT NULL DEFAULT '未签' COMMENT '未签/验收人已签/复核人已签/双签完成/已失效',
  ADD COLUMN `code_generated_time` DATETIME DEFAULT NULL COMMENT '入库码/包码生成时间',
  ADD COLUMN `source_writeback_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PENDING/SUCCESS/FAILED',
  ADD COLUMN `source_writeback_error` VARCHAR(500) DEFAULT NULL COMMENT '回写失败原因',
  ADD COLUMN `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
  ADD COLUMN `cancelled_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '取消人',
  ADD COLUMN `cancelled_time` DATETIME DEFAULT NULL COMMENT '取消时间';

-- 入库单：关联验收单、用途、码生成时间、回写状态
ALTER TABLE `stock_in`
  ADD COLUMN `acceptance_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联验收单ID',
  ADD COLUMN `purpose` VARCHAR(200) DEFAULT NULL COMMENT '用途',
  ADD COLUMN `code_generated_time` DATETIME DEFAULT NULL COMMENT '码生成时间',
  ADD COLUMN `source_writeback_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '回写状态',
  ADD COLUMN `source_writeback_error` VARCHAR(500) DEFAULT NULL COMMENT '回写失败原因';

-- 入库明细：码生成后回指 + 行级扫码状态
ALTER TABLE `stock_in_detail`
  ADD COLUMN `inbound_code_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '入库码ID（生成后回写）',
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '待扫码' COMMENT '待扫码/已扫码/免扫';

-- 出库单：来源、数量锁定、作业签名与确认时间
ALTER TABLE `stock_out`
  ADD COLUMN `source_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/PRODUCTION_PICK/SALES',
  ADD COLUMN `qty_locked` TINYINT NOT NULL DEFAULT 0 COMMENT '1=数量不可改（接收生产领料）',
  ADD COLUMN `work_sign_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '作业签名ID',
  ADD COLUMN `pda_confirm_time` DATETIME DEFAULT NULL COMMENT 'PDA整单确认时间';

-- 出库明细：扫码关联码与扫码状态
ALTER TABLE `stock_out_detail`
  ADD COLUMN `inbound_code_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '出库扣减的入库码ID',
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '待扫码' COMMENT '待扫码/已扫码';

-- 退库单：状态与作业签名
ALTER TABLE `return_order`
  ADD COLUMN `return_status` VARCHAR(20) NOT NULL DEFAULT '已退库' COMMENT '草稿/已退库/已取消（存量默认已退库）',
  ADD COLUMN `source_out_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '原出库单ID',
  ADD COLUMN `work_sign_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '作业签名ID',
  ADD COLUMN `pda_confirm_time` DATETIME DEFAULT NULL COMMENT 'PDA确认时间';

-- 退库明细：关联入库码、库存批次与行级扫码状态
ALTER TABLE `return_order_detail`
  ADD COLUMN `inbound_code_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '退库对应的入库码ID',
  ADD COLUMN `stock_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '退库回增的库存批次ID',
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '待扫码' COMMENT '待扫码/已扫码/免扫';

-- 放行记录：成品入库状态
ALTER TABLE `release_review`
  ADD COLUMN `inbound_status` VARCHAR(20) NOT NULL DEFAULT '未入库' COMMENT '未入库/待入库/已入库',
  ADD COLUMN `stock_in_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '成品入库单ID';

-- 库存流水：码级追溯
ALTER TABLE `stock_transaction`
  ADD COLUMN `inbound_code_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '入库码ID',
  ADD KEY `idx_txn_inbound_code` (`inbound_code_id`);

-- 操作日志：PDA设备号
ALTER TABLE `sys_operation_log`
  ADD COLUMN `device_no` VARCHAR(64) DEFAULT NULL COMMENT 'PDA设备编号',
  ADD COLUMN `terminal_type` VARCHAR(20) DEFAULT NULL COMMENT 'PC/PDA';
```

> 所有 `ADD COLUMN` 需写成幂等迁移脚本（先查 `information_schema.COLUMNS` 再执行），与项目现有 `Data/migration` 风格一致。

---

## 5. 状态机

### 5.1 物料验收单

```
运输中 ──确认到货──► 验收中 ──双签完成且明细全部终结──► 待入库
                                                        │
                                           PDA整单确认 ──┴──► 已入库
                                                        │
                                          取消（主管） ──┴──► 已作废（显示“已取消”）

验收中/待入库 ──全部明细退货/取消──► 已结束
```

- `待入库` 是本次新增主状态，由 `AcceptanceStatusPolicy.deriveMainStatus` 在「全部明细终结且含待入库」时返回，替代原来的「验收中」。
- 双签未完成时即使明细全部终结，也停留在 `验收中`，不允许生成码。
- 退回上一步：`待入库` → `验收中`，同时作废已生成的入库单/码/包，签名置失效。

### 5.2 入库单 `stock_in`

```
待打印入库码 ──打印包码/入库码──► 待入库 ──PDA整单确认──► 已入库 ──取消（主管）──► 已取消（回滚库存）
   │                              │
   └──取消（主管）──► 已取消        └──取消（主管）──► 已取消（未生效，无需回滚）
```

- `待打印入库码`：验收完成/成品放行后生成入库单与码，尚未打印标签；此阶段**不允许扫码**，库存不变。
- `待入库`：标签已打印，允许扫码；扫满后手动确认才生效。
- `confirmStockIn` 同时接受 `待入库` 与历史 `草稿`。
- 成品入库单在 UI 上可展示为「待打印/待确认」，底层状态为 `待打印入库码` → `待入库`。

### 5.3 出库单 `stock_out`

```
草稿 ──PDA整单确认──► 已出库 ──取消（主管）──► 已取消（回滚库存）
  │
  └──取消（主管）──► 已取消（未生效，无需回滚）
```

- `qty_locked=1`（接收生产领料）时草稿数量不可改；`qty_locked=0`（库管自建）草稿可改，但一旦开始扫码会话即锁定。
- 未确认的草稿不扣库存。

### 5.4 退库单 `return_order`

```
草稿 ──PDA整单确认──► 已退库（回增库存）
  │
  └──取消（主管）──► 已取消（未生效）
```

### 5.5 码状态

见 3.2、3.3。

### 5.6 PRD 与系统状态映射

| PRD | 系统值 | 展示 |
|-----|--------|------|
| 验收单 待验收 | `运输中` / `验收中` | 待验收 |
| 验收单 待入库 | `待入库`（新增） | 待入库 |
| 验收单 已入库 | `已入库` | 已入库 |
| 验收单 已取消 | `已作废` | 已取消 |
| 入库单 待打印入库码 | `待打印入库码`（新增） | 待打印入库码 |
| 入库单 待入库 | `待入库`（新增） | 待入库 |
| 入库单 已入库 | `已入库` | 已入库 |
| 出库单 草稿 | `草稿` | 草稿 |
| 出库单 已出库 | `已出库` | 已出库 |
| 退库单 草稿 | `return_status=草稿`（新增） | 草稿 |
| 退库单 已退库 | `return_status=已退库` | 已退库 |
| 成品入库单 待打印 | `in_status=待打印入库码` | 待打印入库码 |
| 成品入库单 待确认 | `in_status=待入库` | 待确认 |

---

## 6. 后端设计

### 6.1 包结构

```
com.tonghui.erp
├── Controller/Pda
│   ├── PdaAuthController.java          # PDA登录（设备校验）
│   ├── PdaDeviceController.java        # 设备登记/绑定
│   ├── PdaInboundController.java       # 待入库任务
│   ├── PdaOutboundController.java      # 出库任务/草稿
│   ├── PdaReturnController.java        # 退库任务/草稿
│   ├── PdaScanController.java          # 会话、扫码、撤销、确认
│   ├── PdaLabelController.java         # 打印/补打/二维码图片
│   └── PdaExceptionController.java     # 手工录入、异常登记、回写待处理
├── Service
│   ├── InboundCodeService.java
│   ├── PackageCodeService.java
│   ├── CodeSequenceService.java
│   ├── PdaScanSessionService.java
│   ├── PdaInboundService.java
│   ├── PdaOutboundService.java
│   ├── PdaReturnService.java
│   ├── EsignService.java
│   ├── PdaDeviceService.java
│   ├── PdaPermissionService.java
│   ├── SourceWritebackService.java
│   └── LabelRenderService.java
├── Service/impl（对应实现）
├── Common/Dto/Pda
│   ├── PdaLoginRequest / PdaLoginResult
│   ├── ScanResolveResult
│   ├── ScanRequest / ScanActionResult
│   ├── ConfirmRequest / ConfirmResult
│   ├── InboundTaskDto / OutboundTaskDto / ReturnTaskDto
│   └── LabelPrintRequest
├── Common/annotation/RequirePermission.java
├── Common/aspect/PermissionAspect.java
├── Common/utils/BarcodeUtils.java      # 编码/解码/CRC16
├── Common/utils/PdaStatusMapper.java   # PRD 状态 ↔ 系统状态
└── Data/Entity + mapper（新增 10 张表）
```

### 6.2 核心服务职责

| 服务 | 职责 |
|------|------|
| `CodeSequenceService` | 按 `(biz_type, seq_date)` 原子取号，生成 `inYYYYMMDDNNNNN` / `pkYYYYMMDDNNNNN` |
| `InboundCodeService` | 按入库明细生成码、二维码内容、状态流转、剩余数量扣减/回增 |
| `PackageCodeService` | 按来源单据生成包码、汇总包内码、包状态维护 |
| `PdaScanSessionService` | 会话创建/恢复、扫码动作、撤销、活动行汇总、整单确认前置校验 |
| `PdaInboundService` | 待打印入库码/待入库任务查询、生成入库单与码、确认入库 |
| `PdaOutboundService` | 生产领料草稿、库管自建草稿、确认出库 |
| `PdaReturnService` | 退库草稿、确认退库 |
| `EsignService` | 验收双签、作业签名、签名失效、签名查询 |
| `PdaDeviceService` | 设备登记/绑定/校验、最后登录时间 |
| `PdaPermissionService` | 按 `perm_key` 校验当前用户权限 |
| `SourceWritebackService` | 验收取消→采购单、成品入库取消→放行记录的回写与待处理 |
| `LabelRenderService` | 标签 PNG/打印页生成、打印/补打登记 |

### 6.3 接口清单

统一前缀 `/api/pda`，返回 `ApiResponse<T>`；除登录外均需 JWT + `X-Device-No`。

#### 认证与设备

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | POST | `/api/pda/auth/login` | PDA 登录：账号密码 + 设备号，校验设备已登记且绑定当前用户 |
| 2 | GET | `/api/pda/me` | 当前用户、绑定设备、角色、权限键、可作业仓库 |
| 3 | POST | `/api/pda/devices` | 登记设备（主管权限） |
| 4 | PUT | `/api/pda/devices/{deviceNo}/bind` | 绑定/变更使用人（主管权限） |
| 5 | GET | `/api/pda/devices` | 设备列表 |

#### 验收签名

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 6 | POST | `/api/pda/acceptance/{id}/sign` | 验收人/复核人签名（密码二次确认） |
| 7 | GET | `/api/pda/acceptance/{id}/signatures` | 当前有效签名与失效记录 |
| 8 | POST | `/api/pda/acceptance/{id}/return-step` | 退回上一步：签名失效、作废待入库单与码 |

#### 待入库任务

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 9 | GET | `/api/pda/inbound/tasks` | 入库任务：待打印入库码 + 待入库（成品入库单同为待打印/待确认） |
| 10 | GET | `/api/pda/inbound/tasks/{id}` | 任务详情：单据头、明细、入库码、包码、扫码进度 |
| 11 | POST | `/api/pda/inbound/tasks/{id}/prepare` | 幂等生成待入库单与码（双签/放行后自动触发，也可手动重试） |

#### 扫码会话

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 12 | POST | `/api/pda/sessions` | 开始/恢复会话：`{bizType, bizId, deviceNo}` |
| 13 | GET | `/api/pda/sessions/{sessionNo}` | 会话状态：每行已扫/待扫、最近动作、可撤销动作 |
| 14 | POST | `/api/pda/sessions/{sessionNo}/scan` | 扫一个码：`{code, clientScanId, lineId?}`；`lineId` 为空=自动匹配，指定=只允许匹配该明细行 |
| 15 | POST | `/api/pda/sessions/{sessionNo}/undo` | 撤销上一条扫码动作组：`{actionGroupNo}` |
| 16 | PUT | `/api/pda/sessions/{sessionNo}/lines/{lineId}/location` | 录入/修改库位（入库） |
| 17 | POST | `/api/pda/sessions/{sessionNo}/confirm` | 整单确认：`{requestId, password, locations?}` |
| 18 | POST | `/api/pda/sessions/{sessionNo}/cancel` | 取消会话（不取消单据） |

#### 出库

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 19 | GET | `/api/pda/outbound/tasks` | 草稿出库任务（生产领料推送 + 库管自建） |
| 20 | GET | `/api/pda/outbound/tasks/{outId}` | 出库任务详情（含每行可扫批次/码） |
| 21 | POST | `/api/pda/outbound/drafts` | 库管自建草稿出库单（数量可改） |
| 22 | PUT | `/api/pda/outbound/drafts/{outId}` | 修改草稿数量/明细（`qty_locked=0` 且未开始扫码） |

#### 退库

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 23 | GET | `/api/pda/return/available-out-orders` | 可退库出库单 |
| 24 | GET | `/api/pda/return/out-orders/{outNo}/materials` | 可退明细与可退数量 |
| 25 | POST | `/api/pda/return/drafts` | 新建退库草稿（数量先录入） |
| 26 | PUT | `/api/pda/return/drafts/{id}` | 修改退库草稿（未开始扫码） |

#### 标签

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 27 | GET | `/api/pda/labels/inbound/{code}` | 入库标签数据 |
| 28 | GET | `/api/pda/labels/package/{code}` | 包标签数据（含详单） |
| 29 | GET | `/api/pda/labels/{code}/png` | 二维码/条码 PNG |
| 30 | POST | `/api/pda/labels/{code}/print` | 首次打印登记 |
| 31 | POST | `/api/pda/labels/{code}/reprint` | 补打登记（原因必填） |
| 32 | GET | `/api/pda/labels/batch?stockInId=` | 按入库单批量打印清单 |

#### 通用扫码与明细行扫码（PC + PDA 共用）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 33 | POST | `/api/pda/scan/resolve` | 通用扫码解析（不建会话）：返回码类型、物料、批次、库存、可用数量，供 PC 表单与 PDA 共用 |
| 34 | POST | `/api/pda/stockin/{inId}/details/{detailId}/scan` | PC 入库明细行扫码：校验码与行匹配，标记该行已扫 |
| 35 | DELETE | `/api/pda/stockin/{inId}/details/{detailId}/scan` | 取消该行扫码（整单确认前） |
| 36 | POST | `/api/pda/stockout/{outId}/details/{detailId}/scan` | PC 出库明细行扫码：校验并绑定批次/入库码 |
| 37 | DELETE | `/api/pda/stockout/{outId}/details/{detailId}/scan` | 取消该行扫码（整单确认前） |
| 38 | POST | `/api/pda/return/{id}/details/{detailId}/scan` | PC 退库明细行扫码 |
| 39 | DELETE | `/api/pda/return/{id}/details/{detailId}/scan` | 取消该行扫码（整单确认前） |

#### 异常、手工录入、回写

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 40 | POST | `/api/pda/sessions/{sessionNo}/manual-entry` | 授权手工录入（主管密码 + 原因） |
| 41 | GET | `/api/pda/exceptions` | 异常/手工录入记录 |
| 42 | GET | `/api/pda/writeback/pending` | 来源回写失败待处理列表 |
| 43 | POST | `/api/pda/writeback/{id}/retry` | 手工重试回写 |

### 6.4 关键接口示例

#### 开始会话

```http
POST /api/pda/sessions
Content-Type: application/json
X-Device-No: PDA-A03

{
  "bizType": "INBOUND",
  "bizId": 1560
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "sessionNo": "PS2026111300001",
    "bizType": "INBOUND",
    "bizId": 1560,
    "bizNo": "IN202611130001",
    "sourceType": "ACCEPTANCE",
    "warehouseId": 71,
    "packageCode": "pk2026111300001",
    "lines": [
      {
        "lineId": 2001,
        "itemCode": "F030",
        "itemName": "蜂蜜（药用）",
        "batchNumber": "260908-1",
        "expectedQty": 500,
        "scannedQty": 0,
        "code": "in2026111300001",
        "codeStatus": "待入库",
        "storageLocation": null
      }
    ]
  }
}
```

#### 扫码

```http
POST /api/pda/sessions/PS2026111300001/scan
Content-Type: application/json

{
  "code": "ERP1|PK|pk2026111300001|9C1E",
  "clientScanId": "8f3a2c1e-5b7d-4e9a-9c00-2d1f6b8a7c11"
}
```

成功响应：

```json
{
  "code": 200,
  "data": {
    "actionNo": "PA2026111300007",
    "actionGroupNo": "PG2026111300003",
    "actionType": "PACKAGE",
    "scanResult": "SUCCESS",
    "message": "整包扫码成功，共 5 种物料",
    "lines": [
      { "lineId": 2001, "expectedQty": 500, "scannedQty": 500, "satisfied": true }
    ],
    "allSatisfied": true
  }
}
```

失败响应（仍返回 200，业务结果在 `data`，便于 PDA 统一处理）：

```json
{
  "code": 200,
  "data": {
    "actionNo": "PA2026111300008",
    "actionGroupNo": "PG2026111300004",
    "actionType": "CODE",
    "scanResult": "FAIL",
    "message": "该码不属于当前单据（物料：黄芪）",
    "allSatisfied": false
  }
}
```

#### 整单确认

```http
POST /api/pda/sessions/PS2026111300001/confirm
Content-Type: application/json

{
  "requestId": "c2a7d4f0-9e31-4d7a-b1f2-6a5c8e0d4b77",
  "password": "******",
  "locations": [
    { "lineId": 2001, "storageLocation": "A-01-02" }
  ]
}
```

响应：

```json
{
  "code": 200,
  "message": "整单入库完成",
  "data": {
    "bizType": "INBOUND",
    "bizId": 1560,
    "bizNo": "IN202611130001",
    "status": "已入库",
    "stockChanged": true,
    "workSignId": 90021
  }
}
```

### 6.5 事务与并发

1. **开始会话**：`SELECT ... FOR UPDATE` 锁业务单据；同一单据存在 `SCANNING` 会话时直接返回该会话（支持换设备续做需主管解绑）。
2. **扫码**：事务内锁会话行；按 `clientScanId` 去重；插入动作与明细；更新 `last_active_time`。重复码由 `uk_scan_active` 兜底，捕获后返回「已扫」。
3. **整单确认**：事务内 `SELECT ... FOR UPDATE` 锁会话；校验状态与全部行扫满；调用现有 `applyInbound/applyOutbound`；更新码的 `remaining_qty/stock_id/code_status`；更新单据状态；写签名与幂等记录。
4. **出库扣减**：在 `StockServiceImpl.applyOutbound` 改为原子更新：
   ```sql
   UPDATE stock SET quantity = quantity - #{qty}
    WHERE stock_id = #{stockId} AND quantity >= #{qty} AND is_deleted = 0;
   ```
   受影响行数为 0 即库存不足，抛异常整体回滚。同时原子扣减 `inbound_code.remaining_qty`。
5. **码生成**：序列表 `FOR UPDATE` + 唯一索引；`DuplicateKeyException` 重试 3 次。
6. **提交幂等**：`pda_request_log.request_id` 唯一键；重复请求直接返回首次 `result_json`。

### 6.6 状态与库存的单一入口

| 动作 | 调用 |
|------|------|
| 验收双签生成待打印入库单+码 | `PdaInboundService.prepareFromAcceptance` → 保存 `stock_in(待打印入库码)` + 明细 + 码；**不调用** `applyInbound` |
| 成品放行生成待打印入库单+码 | `PdaInboundService.prepareFromFinishedRelease`；**不调用** `applyInbound` |
| PDA 入库确认 | `StockService.applyInbound` + 更新码为在库 + `stock_in=已入库` + 验收明细=已入库 |
| 生产领料发料 | `ProductionPickService.issuePick` → `createDraftOutboundWithDetails`（草稿） |
| PDA 出库确认 | `StockService.applyOutbound` + 扣码剩余 + `stock_out=已出库` |
| PDA 退库确认 | 现有 `applyReturn` 逻辑抽为 `ReturnOrderService.confirmReturn` + 回增码剩余 |
| 取消已生效单据 | 现有 `rollbackInbound/rollbackOutbound` + 码作废/回增 |

### 6.7 PC 端兼容模式（保存与确认分离）

**目标**：现有 PC 页面继续用原接口保存，但保存后进入 `待入库/草稿`，由库管**手动点「确认」**才改库存；扫码、确认与 PDA 共用同一服务。

**做法**：入库入口先创建 `待打印入库码`，打印后转 `待入库`；出库/退库入口创建草稿，确认是独立动作。

| 入口 | 行为 | 保存后的状态 |
|------|------|-------------|
| PC `POST /api/stockin/withDetails` | 创建 `stock_in(待打印入库码)` + 明细 + 码 | 待打印入库码 → 打印 → 待入库 |
| PC `POST /api/stockout/direct-outbound` | 创建 `stock_out(草稿)` + 明细 | 草稿 |
| PC `POST /api/acceptance/{id}/inbound` | 创建 `stock_in(待打印入库码)` + 码 | 待打印入库码 → 打印 → 待入库 |
| PC `POST /api/warehouse/return-orders` | 创建 `return_order(草稿)` + 明细 | 草稿 |
| 成品放行 | 生成成品入库单（待打印入库码）+ 码 | 待打印入库码 → 打印 → 待入库 |
| PDA `/api/pda/**` | 创建待打印入库码/草稿 + 扫码会话 | 待打印入库码 → 待入库 / 草稿 |

**确认入口**：

- PC：恢复并展示 `POST /api/stockin/{id}/confirm`、`POST /api/stockout/{id}/confirm`、`POST /api/return/{id}/confirm`；确认前校验明细行是否全部 `已扫码`。
- PDA：`POST /api/pda/sessions/{sessionNo}/confirm`，扫满 + 作业签名。
- 两端口径一致：确认 → `applyInbound/applyOutbound` → 单据状态变更 → 码状态变更。

**请求级开关**（默认 MANUAL）：

```json
{
  "confirmMode": "MANUAL"
}
```

- `MANUAL`（默认）：保存后停留 `待打印入库码/草稿`；入库打印后转 `待入库`，扫码确认后生效。
- `AUTO`：仅作为灰度回滚开关，保存后同事务确认、响应与旧版一致；不建议长期使用。

**现有页面需要的最小改动**：

1. 状态字典增加 `待入库`（入库单）、`草稿/已退库`（退库单）；列表筛选兼容新值。
2. 列表行/详情页增加「确认入库/确认出库/确认退库」按钮；未扫满时置灰并提示差几行。
3. 明细行增加「扫码」按钮（见 6.8）；未扫的行走扫码，或由主管授权手工录入。
4. 入库单详情增加「打印入库码/包码」入口。

**兼容性边界**：

- 现有 `addStockIn` / `addStockOut` 的调用方统一改为「创建 + 确认」的内部方法；外部接口签名不变，但返回状态从「已入库/已出库」变为「待入库/草稿」——这是有意的行为变化，前端必须接状态与确认按钮。
- `confirmStockIn` 改为同时接受 `待入库/草稿`；`confirmStockOut` 已支持 `草稿`。
- 取消逻辑复用现有 `cancelStockIn/cancelStockOut`：未确认直接作废，已确认回滚库存。

### 6.8 明细行扫码（PC + PDA 共用）

**目标**：既支持整包扫，也支持逐条明细行扫；PC 与 PDA 行为一致，审计统一。

| 场景 | 入口 | 行为 |
|------|------|------|
| PDA 整包 | 会话 `scan` 不传 `lineId`，扫包码 | 展开包内全部码，逐行分配 |
| PDA 逐条 | 会话 `scan` 不传 `lineId`，扫入库码 | 自动匹配物料/批次所在行 |
| PDA 指定行 | 会话 `scan` 传 `lineId`，扫入库码 | 只允许匹配该行；不匹配直接失败 |
| PC 明细行 | `POST /api/pda/stockin|stockout|return/{id}/details/{detailId}/scan` | 复用会话服务的 `scanLine`，为单据创建/复用 `PC-{userId}` 会话 |
| 手工录入 | 行级 `manual-entry` | 写异常日志 + 手工扫码动作 |

**行级状态**：

- `stock_in_detail.scan_status`、`stock_out_detail.scan_status`、`return_order_detail.scan_status`：`待扫码 / 已扫码 / 免扫`。
- 扫成功置 `已扫码`，撤销置回 `待扫码`。
- `免扫` 仅用于兼容模式（`pc-require-scan=false`）下明确跳过扫码，必须写 `pda_exception_log` 记录原因。

**行级匹配规则**：

1. 码必须属于本单据：入库码的 `source_doc_id` 与入库单一致；出库码的 `stock_id` 必须在出库明细的可用批次内；退库码必须在原出库明细的扣减码内。
2. 物料编码、批号、仓库必须与明细行一致；不一致返回具体差异（如「码批次 260908-1，行批次 260908-2」）。
3. 指定 `lineId` 时不允许「自动改配到其他行」，必须先撤销再扫。
4. 一行可被多个码满足（数量拆分），但 `allocated_qty` 之和不得超过行数量，超过即失败。
5. 整包扫码是行级扫码的批量形式，任一码失败则整组失败，不产生部分有效。

**PC 明细行交互**：

- 每行右侧增加扫码图标；点击后聚焦扫码输入框，扫完自动回填批次、效期、入库码，行状态变绿。
- 行内显示「已扫 1/1」「已扫 0.4/1.0」进度。
- 未扫满时，若 `pc-require-scan=true`，保存/确认按钮置灰；否则允许保存（兼容模式）。
- 撤销行扫码：行内「清除」按钮，整单确认前可用。

---

## 7. 业务流程（详细步骤）

### 7.1 采购验收入库

```
PC/质检：确认到货 → 初验 → 检验（明细进入待入库）
PDA 验收人：签名（账号+密码）        → sys_esign_record(ACCEPT)
PDA 复核人：签名（账号+密码）        → sys_esign_record(REVIEW)
系统：双签完成 + 全部明细终结
  → 验收单 状态 待入库
  → 创建 stock_in(待打印入库码) + stock_in_detail
  → 逐明细生成 inbound_code
  → 生成 package_code，回填 inbound_code.package_id
  → 写 acceptance_order.code_generated_time
PDA/PC 库管：打印包码与入库码（首次打印登记）
  → stock_in 状态 待入库
PDA 库管：开始入库会话
  → 整包扫 package_code，或逐条扫 inbound_code
  → 每扫一条只写 scan_action/scan_record，不改库存
  → 录入库位（包级默认 + 行级覆盖）
  → 全部行扫满，输入作业签名密码
  → 整单确认：
       StockService.applyInbound
       stock → 新增批次
       inbound_code → 在库，remaining_qty=inbound_qty
       stock_in → 已入库
       验收单 → 已入库，明细 → 已入库
       写 stock_transaction(inbound_code_id)
```

**边界**：

- 双签未完成：不允许生成码，任务列表不显示。
- 明细未全部终结：双签按钮禁用，提示先完成初验/检验。
- 扫码与单据不符：动作失败，停留当前步骤；可撤销上一条或退回上一步。
- 退回上一步：签名失效（写 INVALIDATE 事件）、作废 `stock_in(待入库)`、作废全部码与包、验收单回 `验收中`。
- 已入库后不能退回，只能取消（主管）。

### 7.2 成品入库

```
质检：检验放行通过
  → 写入 ReleaseReview（inbound_status=待入库）
  → 创建 stock_in(待打印入库码，inType=成品入库) + 明细
  → 生成 inbound_code + package_code
  → 不调用 applyInbound
PDA/PC 库管：打印 → stock_in=待入库
PDA 库管：扫码 → 整单确认
  → applyInbound
  → stock_in=已入库；ReleaseReview.inbound_status=已入库
  → 回填工单入库时间（沿用 syncWorkOrderTime）
```

**取消**：待入库取消 → 作废码/包、`ReleaseReview.inbound_status=未入库`、回写质检待处理；已入库取消 → 回滚库存后同样处理。

### 7.3 生产领料出库（接收生产领料）

```
生产端：发料 issuePick
  → 生成 stock_out(草稿, qty_locked=1, source_type=PRODUCTION_PICK)
  → 明细已含 stockId/batch/quantity
PDA 库管：打开任务（数量只读）
  → 开始出库会话
  → 整包扫 package_code（要求包内码与单据行完全一致）
     或逐条扫 inbound_code（每行按数量分配，可跨多个码）
  → 全部行扫满 → 作业签名 → 整单确认
      StockService.applyOutbound（原子扣减）
      inbound_code.remaining_qty 扣减
      stock_out=已出库
```

**边界**：

- 包内物料与单据不一致：整包扫码失败，提示逐条扫。
- 领用数量超过可用数量：扫码/确认时拦截。
- 批次过期：出库拦截。
- 草稿数量不可改：`qty_locked=1`，任何修改数量请求返回错误。

### 7.4 库管自建出库

```
PDA 库管：新建出库草稿
  → 选类型（销售/报损/其他）、仓库
  → 逐行录入物料与数量（数量先录入）
  → 保存草稿（stock_out=草稿, qty_locked=0）
  → 开始会话
  → 扫包码或逐条扫入库码（扫码只确认批次，不改数量）
  → 全部行扫满 → 作业签名 → 整单确认
```

**边界**：草稿一旦开始扫码，数量锁定；要改数量需先取消会话。

### 7.5 退库

```
PDA 库管：选择原出库单
  → 载入可退明细与可退数量
  → 录入本次退库数量（≤ 可退数量）
  → 保存退库草稿（return_status=草稿，不回增库存）
  → 开始会话
  → 逐条扫入库码（退哪个码就扫哪个码）
  → 全部行扫满 → 作业签名 → 整单确认
  → 回增 stock.quantity 与 inbound_code.remaining_qty
  → return_status=已退库
```

**边界**：退库数量超过可退数量拦截；扫到的码与原出库批次不一致拦截。

### 7.6 补打

```
PDA/PC：选择入库码或包码
  → 校验码状态（已作废不可补打）
  → 填补打原因
  → 打印（复用原码，不生成新码）
  → pda_label_print_log 记录：码、原因、操作人、设备、时间
  → inbound_code/package_code.print_count +1
```

### 7.7 手工录入（标签无法识别）

```
PDA：点击“手工录入”
  → 主管输入账号密码授权
  → 选择业务行，输入码值/批次/数量（按场景）
  → 填原因（必填）
  → 系统写 pda_exception_log(MANUAL_ENTRY)
  → 生成一条 scan_action(action_type=MANUAL) 与 scan_record，按已扫处理
```

**限制**：手工录入不能绕过「批次过期」「数量超可用」「物料不符」等硬校验；只解决「码本身无法识别」。

### 7.8 取消与来源回写

```
主管在 PC/PDA 取消单据
  → 校验主管权限，取消原因必填
  → 写 sys_esign_record(CANCEL)
  → 未生效单据：作废码/包；已生效单据：先回滚库存再作废
  → 来源回写：
      验收单 → 采购单回退“待到货”，通知采购
      成品入库单 → ReleaseReview.inbound_status=未入库，通知质检
  → 回写成功：source_writeback_status=SUCCESS
  → 回写失败：source_writeback_status=FAILED + error，进入待处理列表，不静默丢弃
```

---

## 8. 校验规则实现（对照 PRD 第 5 节）

| PRD 规则 | 实现位置 | 处理 |
|---------|---------|------|
| 条码与单据物料不一致 | `PdaScanSessionService.scan` | 解析码 → 物料/批次/仓库与目标行比对；不一致写 FAIL 动作，返回「该码不属于当前单据」 |
| 领用数量超过可用数量 | 扫码分配 + 确认校验 | 分配时 `min(行剩余, 码剩余)`；确认时 `code.remaining >= 分配量` 且 `stock.quantity >= 扣减量` |
| 批次已过期 | 扫码解析 | 出库场景：`expiry_date < CURDATE()` → FAIL 拦截；入库场景：返回 warning，允许继续 |
| 重复扫同一码 | `uk_scan_active` + 服务校验 | 同一会话内该码已有有效动作 → 返回「已扫，不重复计数」；整包扫码展开的每个码都受此约束 |
| 扫包码 | `PdaScanSessionService.scanPackage` | 展开包内全部码，按整包校验；任一不匹配整体失败 |
| 包内物料与单据不一致 | 同上 | 物料/批次/数量集合不一致 → FAIL，提示逐条扫 |
| 拆包出库 | 包状态校验 | 包 `部分出库` 或单据行少于包内码 → 拦截，提示补打单件条码 |
| 单据行未全部扫码 | 确认前置校验 | 任一行 `scannedQty < expectedQty` → 拒绝确认，库存不变 |
| 接收单被修改数量 | `stock_out.qty_locked=1` | 修改数量接口直接拒绝 |
| 标签无法识别 | 手工录入 | 主管授权 + 原因 + `pda_exception_log` |
| 输入数量后扫码不改数量 | 数据模型 | 扫码只写 `scan_record.allocated_qty`，业务明细数量不更新 |

---

## 9. 权限、设备与审计

### 9.1 权限键

| 权限键 | 操作 |
|--------|------|
| `warehouse:pda:inbound` | 入库扫码、整单确认 |
| `warehouse:pda:outbound` | 出库扫码、整单确认 |
| `warehouse:pda:return` | 退库扫码、整单确认 |
| `warehouse:pda:label:print` | 首次打印 |
| `warehouse:pda:label:reprint` | 补打 |
| `warehouse:acceptance:sign:accept` | 验收人签名 |
| `warehouse:acceptance:sign:review` | 复核人签名 |
| `warehouse:pda:manual-entry` | 手工录入 |
| `warehouse:doc:cancel` | 取消单据 |
| `warehouse:pda:device:register` | 设备登记/绑定 |

**实现**：

- 新增 `@RequirePermission("warehouse:pda:inbound")` 注解 + `PermissionAspect`。
- 切面从 `SecurityContext` 取 `userId`，查 `user_role → role_perm → permission.perm_key`（可加请求级缓存），不匹配返回 403。
- 前端按钮显隐仍复用登录返回的 `permissionTree`，后端做最终校验。

### 9.2 设备登记

- 主管在 PC `POST /api/pda/devices` 登记设备编号与使用人。
- PDA 登录必须携带 `X-Device-No`；未登记、停用、绑定他人 → 拒绝登录。
- 每次作业会话写入 `device_no`；操作日志写 `device_no`、`terminal_type=PDA`。
- 设备换人由主管重新绑定，不删除历史设备记录。

### 9.3 电子签名

- 验收双签：`sign_stage=ACCEPT/REVIEW`，要求不同账号；两次签名完成后 `acceptance_order.sign_status=双签完成`。
- 作业签名：整单确认前必须输入密码，`sign_stage=WORK`，写 `sys_esign_record` 并在会话记录 `work_sign_id`。
- 签名事件追加不可改；失效写 `event_type=INVALIDATE`，指向原签名。
- `sign_hash = SHA-256(biz_type|biz_id|sign_stage|user_id|sign_time|关键业务摘要)`，防止事后篡改。

### 9.4 审计

| 审计对象 | 落点 |
|---------|------|
| 每次扫码（成功/失败/手工） | `pda_scan_action` + `pda_scan_record` |
| 每次打印/补打 | `pda_label_print_log` + 码表打印字段 |
| 每次签名/失效 | `sys_esign_record` |
| 手工录入/设备故障 | `pda_exception_log` |
| 库存变动 | `stock_transaction`（新增 `inbound_code_id`） |
| 接口操作 | `sys_operation_log`（新增 `device_no/terminal_type`） |
| 字段级变更 | 现有 `DataChangeInterceptor` 自动覆盖 |

---

## 10. PDA 端设计

### 10.1 技术选型

| 方案 | 结论 |
|------|------|
| 响应式 H5（推荐一期） | 工业 PDA 扫描头默认键盘模拟，聚焦输入框即可；免安装、升级即生效；依赖 Wi-Fi |
| uni-app 打包 APK（二期） | 需要广播/SDK、蓝牙打印、离线缓存时采用 |
| 微信小程序 | 仅作手机临时查看/扫码，不作为仓库主设备 |

设备建议：Android 11+、2D 扫描头、Wi-Fi 5、IP65、可换电池（优博讯 i6310、新大陆 MT90、霍尼韦尔 EDA51 等）。

### 10.2 页面与交互

| 页面 | 内容 |
|------|------|
| 设备登录 | 设备号（预置/扫码）+ 账号密码；未登记设备直接提示 |
| 首页 | 入库 / 出库 / 退库 / 补打 / 我的记录 |
| 入库任务 | 待入库验收单/成品单列表，显示单据号、品种数、是否已打印 |
| 入库作业 | 顶部包码/单件切换；扫码框常驻焦点；行清单显示「应扫/已扫」；底部「整单确认」置灰直到扫满 |
| 出库任务 | 生产领料草稿 + 自建草稿；数量只读标识 |
| 出库作业 | 扫包码或逐条扫；每行显示分配数量与批次 |
| 退库作业 | 原出库单 + 可退数量 + 逐条扫码 |
| 补打 | 扫/选码 → 原因 → 打印 |
| 扫码结果 | 物料、批号、效期、数量、用途、库位 |
| 我的记录 | 今日会话、失败动作、待处理回写 |

**交互规范**：

1. 常驻隐藏 input 保持焦点，扫描头「字符+回车」触发；800ms 内同码去重。
2. 成功：短嘀声 + 绿闪 + 震动；失败：双声 + 红闪 + 大字原因。
3. 连续扫码默认开启；每扫完自动回焦。
4. 每个扫码框提供「手输」入口（批号/码值破损兜底）。
5. 「撤销上一条」按钮常驻，撤销以动作组为单位（整包扫一次撤销整组，单件扫一次撤销单条）。
6. 未扫满时「整单确认」置灰并显示差几行/几件。
7. 断网顶部红条，扫码记录本地暂存；恢复后提示「有 N 条未提交」，不自动提交。

### 10.3 离线缓存

- PRD 仅在线作业：一期不做离线库存校验。
- 本地 IndexedDB 暂存：会话号、已扫动作、未提交确认请求。
- 恢复网络后按 `clientScanId/requestId` 重放，服务端幂等去重。
- 不做离线冲突合并。

### 10.4 标签打印

| 方式 | 说明 |
|------|------|
| PC 批量打印 | 后端提供标签数据 + PNG，浏览器打印；适合入库后集中打印 |
| PDA 现场打印 | 二期 uni-app + 蓝牙标签机；一期 PDA 只做打印登记与补打申请 |
| 补打 | 复用原码，记录原因/人/设备/次数 |

标签内容（PRD 3.4）：

```
物料名称、物料编码、批号、入库日期、有效期至、入库数量、用途
[二维码：ERP1|IC|in2026111300001|CRC16]
备注：部分领用后以系统实时库存为准
```

包标签：用途、入库时间、品种数量 + 二维码；附详单再打印每行物料与入库码。

---

## 11. 兼容与迁移

### 11.1 存量单据

| 数据 | 处理 |
|------|------|
| 历史已入库验收单/入库单 | 保持原状态，不补码；只读查询 |
| 历史草稿出库单 | 可走新流程扫码确认；无码时只能手工录入/PC 补录 |
| 历史退库单 | `return_status` 默认 `已退库`，视为已生效 |
| 在途验收单（运输中/验收中） | 上线后按新流程双签、生成码 |

### 11.2 存量库存补码

上线前执行一次性任务：

1. 查 `stock WHERE quantity > 0 AND is_deleted = 0`；
2. 每条生成 `inbound_code`，`source_type=存量补码`，`code_status=在库`，`remaining_qty=quantity`，`inbound_qty=quantity`；`source_doc_id/source_doc_no/stock_in_detail_id` 允许为空；
3. 按 `stock_in_id` 能关联到入库单的，回填 `source_doc_no/arrival_date/purpose`；关联不到的用 `stock` 自身信息；
4. 无法可靠重建包码的，不生成包码；只支持单件扫码；
5. 补码结果输出清单，交仓库确认后打印标签。

> 补码脚本必须幂等（按 `stock_id` 判重，一个 `stock_id` 只生成一个 `source_type=存量补码` 的码），并在测试库 `8.134.48.54` 先跑通，禁止直接操作线上库。

### 11.3 灰度开关

新增配置：

```yaml
erp:
  pda:
    enabled: true                 # PDA 功能总开关
    require-scan-to-inbound: true # true=扫码确认才入库；false=回退旧行为
    require-scan-to-outbound: true
    signature-required: true      # 验收双签/作业签名
    device-required: true         # 未登记设备禁止作业
    pc-auto-confirm: false        # false=PC 保存后停留待入库/草稿，必须手动确认（默认）
    pc-require-scan: true         # true=PC 确认前必须明细行扫满
    line-scan-enabled: true       # 允许明细行扫码（PC 行按钮 / PDA 逐条）
    finished-inbound-mode: PDA    # PDA=放行后停留待确认（默认）；AUTO=兼容旧行为
```

- `require-scan-to-inbound=false` 时，验收/成品入库仍走旧的“保存即入库”，便于回滚。
- `pc-auto-confirm=false`（默认）时，PC 保存后进入待入库/草稿，必须点确认；`true` 仅用于临时回滚旧行为。
- `pc-require-scan=true`（默认）时，PC 确认前校验 `scan_status`，未扫满直接拒绝；灰度初期可临时关闭。
- 灰度期间按仓库开启：`erp.pda.enabled-warehouses=69,71`。

**推荐上线顺序**：PC 与 PDA 同步启用「手动确认 + 明细行扫码」→ 完成标签打印与存量补码 → 仓库试运行 → 全面推行。若现场出现阻塞，可临时把 `pc-auto-confirm` 设为 `true` 回退到旧行为，但不作为长期配置。

---

## 12. 实施计划

| 阶段 | 内容 | 产出 | 估算 |
|------|------|------|------|
| 0 | 迁移脚本、实体/Mapper、码序列、CRC/二维码工具 | DDL + 单测 | 4 天 |
| 1 | 修复出库原子扣减、`confirmStockIn` 支持待入库 | 补丁 + 回归 | 3 天 |
| 2 | 入库码/包码生成、待入库单生成、标签数据与打印 | `prepareFromAcceptance/Finished`、标签接口 | 7 天 |
| 3 | 扫码会话、扫码/撤销/确认、入库流程 | 会话服务 + PDA 入库页 | 9 天 |
| 3.5 | PC 兼容模式与明细行扫码 | 手动确认按钮、行级扫码接口/按钮 | 5 天 |
| 4 | 出库流程（生产领料 + 自建）、退库流程 | 出库/退库接口 + 页面 | 9 天 |
| 5 | 电子签名、设备登记、权限切面 | 签名/设备/权限 | 5 天 |
| 6 | 取消与来源回写、手工录入、异常、待处理 | 取消/回写/异常 | 5 天 |
| 7 | 存量补码、联调、真机测试、试运行 | 补码脚本 + 测试报告 | 6 天 |

合计约 6~7 周（1 后端 + 1 前端），不含硬件采购与仓库培训。

---

## 13. 测试与验收

| 类别 | 用例 |
|------|------|
| 码生成 | 双签后生成码；一明细一码；一单一包；并发取号不重复；作废不复用 |
| 打印 | 待打印入库码不能扫码；打印后转待入库；补打复用原码并留痕 |
| 入库 | 未扫满不能确认；确认后库存一次增加；重复扫不重复计数；撤销上一条后重新扫 |
| 明细行扫码 | PC 行扫码回填批次/效期/入库码；指定行扫错码拦截；整包扫与逐条扫结果一致；未扫满按配置拦截或放行 |
| PC 兼容 | 原接口保存后为待入库/草稿；点确认才入库/出库；未扫满确认被拦截；`confirmMode=AUTO` 时回退旧行为 |
| 包码 | 整包扫一次带出全部行；包内与单据不一致拦截；拆包后整包扫码拦截 |
| 出库 | 生产领料数量不可改；库存不足拦截；过期批次拦截；跨码分配数量正确 |
| 退库 | 超可退数量拦截；确认后库存回增；码剩余回增 |
| 签名 | 双签缺一不可；同账号不能同时验收/复核（按配置）；退回后签名失效留痕 |
| 设备 | 未登记设备拒绝登录；绑定他人拒绝；停用设备拒绝 |
| 权限 | 无权限 403；主管取消/手工录入生效 |
| 回写 | 取消失败进入待处理；重试成功；不静默丢弃 |
| 幂等 | 重复确认只生效一次；断网重放不重复计数 |
| 审计 | 每笔可追溯账号、设备、时间、入库码、流水 |
| 兼容 | 历史单据可查；存量库存补码后可出库 |

对照 PRD 第 11 节 10 条验收标准逐条验证。

---

## 14. 待确认问题

1. **验收状态是否改名**：PRD 用「待验收/待入库/已取消」，系统现为「运输中/验收中/已作废」。本文采用「新增待入库 + 展示层映射」，是否接受？
2. **双签是否必须不同账号**：GMP 通常要求验收人与复核人分离；系统是否强制 `acceptor_user_id != reviewer_user_id`？
3. **成品入库是否要等 PDA 确认才视为入库**：现有放行流程会立即生成已入库单；改为待确认后，工单/计划状态是否也要延后？
4. **用途字段来源**：采购物料验收时「用于生产的制剂品种」由谁填？取自验收单 `preparation_name`，还是验收时手工选择？
5. **包码适用范围**：PRD 3.3 说包码按验收单生成，第 4 节又说成品入库生成包码。成品入库是否也生成包码？
6. **采购取消回退状态**：采购订单现有状态没有「待到货」，是新增该状态，还是回退到「运输中」？
7. **存量库存补码范围**：是否所有 `quantity > 0` 的批次都补码并打印？还是只对上线后新入库的批次发码，存量走手工录入？
8. **PDA 设备采购与打印方式**：是否有工业 PDA 与蓝牙标签机？决定一期是否直接做 uni-app 打包。
9. **PC 确认按钮位置**：放在列表行内快速确认，还是详情页看完明细再确认？本文原型采用「行内 + 详情页」双入口。
10. **PC 端明细行扫码强度**：行扫码是否必须扫满才能确认（默认 `pc-require-scan=true`），还是只作辅助核对？

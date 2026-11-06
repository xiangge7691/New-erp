# 产品信息汇总页 —— 需求完成度、文件分类分级与原型优化说明

> 日期：2026-11-05  
> 范围：仅前端原型与文档，**未修改任何后端代码**。  
> 原型文件：`_analysis/prototypes/product-summary.html`（独立单文件，已去掉左侧菜单）  
> 数据来源：测试库 `8.134.48.54/erp_db` 只读快照（计划、采购/领料、工单、出入库、检验、附件）  
> 前端源码：`_analysis/frontend-src/src`（生产相关全部 `.vue` 上传点）  
> 后端注册表：`BizFileType`、`BizFileKinds`、`file-types.yml`

---

## 1. 需求完成度结论

| 需求项 | 原型现状 | 结论 |
|--------|---------|------|
| 新增“产品信息汇总”页面 | 保留原卡片层级：品种卡片 → 品种内工单卡片（流程节点圆点）→ 批次时间轴节点；同时提供列表视图，右上角手动切换 | 已完成 |
| 按品种筛选 | 品种下拉，按品种编码过滤 | 已完成 |
| 按时间筛选 | 计划/工单/入库/检验日期 + 起止日期 | 已完成 |
| 查询计划 → 采购/领料 → 生产 → 入库 → 出库 → 检验全链路 | 右侧“链路详情”按阶段展示；采购与领料按物料来源二选一 | 已完成 |
| 查询全链路详情 | 单据号下方直接展示**明细行**（物料、数量、单位、供应商、批次等），不再只有汇总 | 已完成 |
| 查询各环节对应文件 | 附件页展示真实 `business_file/file_info` 文件；顶部明确列出缺件清单 | 已完成 |
| 一次性导出该批次全部文件 | 列表每行“导出”、详情底部“导出本批次”，直接生成文件清单 CSV | 原型已完成；真实 zip 待后端接口 |
| 一次性导出该品种全部文件 | 品种筛选后点“导出当前结果”，一次导出该品种全部批次 | 原型已完成；真实 zip 待后端接口 |

**总体结论：需求在原型层面已经闭环，并且已用测试库真实数据替换模拟数据。** 正式版还缺三块：后端全链路聚合接口、真实文件打包下载、领料/入库/出库三个环节的附件上传位。

---

## 2. 前端上传点扫描结果

### 2.1 扫描口径

- 扫描目录：`_analysis/frontend-src/src` 全部 `.vue` 文件。
- 识别组件：`FileUpload`、`FilePreviewDialog`、原生 `ElUpload`。
- 结果：**95 个 `FileUpload` 组件实例，分布在 42 个 `.vue` 文件**；审批模块另有 7 处原生上传。
- 生产链路相关：**68 个组件实例、31 个页面/组件，归并后约 33 个业务文件槽位**。

### 2.2 分类原则

**分类维度：按业务环节。** 与汇总页时间轴一致，便于“点到哪个环节就看哪个环节的文件”。

**分级维度：按 GMP 受控程度和归档要求。**

| 级别 | 名称 | 判定标准 | 典型文件 | 导出策略 |
|------|------|---------|---------|---------|
| A | 关键受控 | GMP 批记录、放行依据、质量结论；需版本受控、长期保存 | 批生产/包装记录、工艺规程、质量标准、检验记录/图谱、放行审核、偏差/CAPA | 导出前校验完整性；缺失时阻断或强提示 |
| B | 质量重要 | 质量体系配套、检验支持性文件；随批次归档 | 验证方案、工艺流程图、批记录模板、请检单、取样记录、验收检验报告、工序模板 | 默认全量导出，缺失时提示 |
| C | 业务凭证 | 计划、采购、领料、出入库、财务凭证、一般附件 | 生产计划/指令、采购订单、随货清单、发票、领料单、入库单、出库单、其他文件 | 按业务/财务档案期限保存，可批量导出 |

> 判定说明：同一文件组内可能混合等级，例如物料验收中“验收检验报告”为 B，随货清单/发票为 C；原型按文件级标注。

### 2.3 文件格式与容量

| 项目 | 现状 |
|------|------|
| 单文件上限 | 100 MB（`spring.servlet.multipart.max-file-size`、`file.max-size`） |
| 单次请求上限 | 110 MB |
| 全局允许 MIME | `image/jpeg`、`image/png`、`image/gif`、`application/pdf`、`application/msword`、`.docx`、`application/vnd.ms-excel`、`text/plain` |
| 前端实际 accept | 多数为 `.pdf,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx`；采购订单额外允许 `.zip,.rar` |
| 单槽位数量 | 品种资料/工序模板/计划单 1 个；供应商/设备维保 5 个；多数 10 个；审批 20 个 |

> 注意：`application.yml` 的 `allowed-types` 未包含 `.xlsx`、`.zip`、`.rar` 的 MIME，但前端 `accept` 允许这些格式（采购订单允许 zip/rar，生产附件允许 xlsx）。如果后端按 MIME 严格校验，会出现“前端能选、后端拒绝”的不一致，建议统一配置或改为按扩展名白名单校验。

---

## 3. 生产链路文件分类清单

### 3.1 计划与采购

| 环节 | 文件组 / 槽位 | 前端上传点 | 现有 businessType | 目标 bizType / fileKind | 级别 | 状态 |
|------|--------------|-----------|-------------------|--------------------------|------|------|
| 计划 | 计划单文件 | `production_plan/components/ProductionPlanDialog.vue`、`ProductionPlanDetail.vue` | `PRODUCTION_PLAN` | `PRODUCTION_PLAN` / `DEFAULT` | C | 已接入 |
| 采购 | 采购订单附件 | `purchase_order/modules/purchase-order-dialog.vue`、`purchase-order-detail.vue` | `PURCHASE_FILE` | `PURCHASE_ORDER` / `DEFAULT` | C | 已接入 |
| 验收 | 随货清单 | `acceptance_inspection/components/AcceptanceDetail.vue`、`AcceptanceViewDialog.vue` | `GOODS_ACCEPTANCE_WAYBILL` | `ACCEPTANCE_ORDER` / `WAYBILL` | C | 已接入 |
| 验收 | 发票 | 同上 | `GOODS_ACCEPTANCE_INVOICE` | `ACCEPTANCE_ORDER` / `INVOICE` | C | 已接入 |
| 验收 | 验收检验报告 | 同上 | `GOODS_ACCEPTANCE_INSPECTION_REPORT` | `ACCEPTANCE_ORDER` / `INSPECTION_REPORT` | B | 已接入 |

> 采购计划、采购审批页面目前没有附件上传点；如业务需要，应新增 `PURCHASE_PLAN` 类型或复用审批附件。

### 3.2 领料

| 环节 | 文件组 / 槽位 | 前端上传点 | 现有 businessType | 建议 bizType / fileKind | 级别 | 状态 |
|------|--------------|-----------|-------------------|--------------------------|------|------|
| 领料 | 领料单附件 | **无** | 无 | `STOCK_OUT_PRODUCTION` / `DEFAULT` | C | **待接入** |

- 领料单（`material_requisition_slip`）已在业务中存在，`file-types.yml` 也预留了 `STOCK_OUT_PRODUCTION → 库存管理/出库单/领料`。
- 前端 `material_requisition_management` 目前只有申请、详情、打印三个弹窗，没有 `FileUpload`。
- 原型中领料环节展示领料单明细，但附件 chip 显示为虚线“附件位未接入”，不生成假文件。
- **业务约定：采购入库与生产领料是两种物料来源，一般不会同时出现。** 原型按计划的 `materialSource` 二选一展示：采购路径显示“采购/验收”，领料路径显示“领料”，不再对同一批次同时生成两条记录。`materialSource` 与计划类型无强绑定，示例数据中 `Plan20261002-004`、`Plan20260720-008` 仅用于演示领料路径，其余为采购路径。

### 3.3 生产与工序

| 环节 | 文件组 / 槽位 | 前端上传点 | 现有 businessType | 目标 bizType / fileKind | 级别 | 状态 |
|------|--------------|-----------|-------------------|--------------------------|------|------|
| 品种资料 | 工艺规程 | `preparation_management/index.vue` | `PREPARATION_DOCUMENT` | `PREPARATION` / `PROCESS_SPEC` | A | 已接入 |
| 品种资料 | 成品质量标准 | 同上 | `PREPARATION_DOCUMENT` | `PREPARATION` / `QUALITY_STANDARD` | A | 已接入 |
| 品种资料 | 批记录模板 | 同上 | `PREPARATION_DOCUMENT` | `PREPARATION` / `BATCH_RECORD_TEMPLATE` | A | 已接入 |
| 品种资料 | 工艺验证方案 | 同上 | `PREPARATION_DOCUMENT` | `PREPARATION` / `VERIFICATION_PLAN` | B | 已接入 |
| 品种资料 | 工艺规程流程图 | 同上 | `PREPARATION_DOCUMENT` | `PREPARATION` / `PROCESS_FLOWCHART` | B | 已接入 |
| 品种资料 | 工序模板 | `preparation_management/index.vue`（按 `templateId`） | `PREPARATION_PROCESS` | `PREPARATION_PROCESS_TEMPLATE` / `DEFAULT` | B | 已接入 |
| 工单 | 生产批记录 | `production_task/components/TaskDetail.vue` | `PRODUCTION_TASK` | `WORK_ORDER` / `BATCH_RECORD` | A | 已接入 |
| 工单 | 其他文件 | 同上 | `PRODUCTION_TASK` | `WORK_ORDER` / `OTHER` | C | 已接入 |
| 工序 | 工序附件 | `production_task/components/TaskDetail.vue`（按工序行 ID） | `PRODUCTION_PROCESS` | `WORK_ORDER_PROCESS_EXECUTION` / `PROCESS_ATTACHMENT` | — | 已接入（不必传） |
| 工序 | 工艺规程流程图（只读） | `production_task/components/TaskDetail.vue` | `PREPARATION_DOCUMENT` | `PREPARATION` / `PROCESS_FLOWCHART` | B | 只读引用 |

> 剂型页面 `dosage_management/index.vue` 另有批记录模板、验证模板、检验模板三个槽位，属于品种上游资料，汇总页可按“品种资料”归并。

### 3.4 入库与出库

| 环节 | 文件组 / 槽位 | 前端上传点 | 现有 businessType | 建议 bizType / fileKind | 级别 | 状态 |
|------|--------------|-----------|-------------------|--------------------------|------|------|
| 入库 | 成品入库单附件 | **无** | 无 | `STOCK_IN_PRODUCT` / `DEFAULT` | C | **待接入** |
| 出库 | 销售出库单附件 | **无** | 无 | `STOCK_OUT_SALES` / `DEFAULT` | C | **待接入** |

- 后端 `BizFileType` 已注册 `STOCK_IN_*`、`STOCK_OUT_*` 等库存文件类型，但前端入库、出库、库存页面均未使用 `FileUpload`。
- 因此汇总页可以展示入库/出库单据详情，但文件数为 0，不能把“无附件”误判为“缺件”。

### 3.5 检验、放行与偏差

| 环节 | 文件组 / 槽位 | 前端上传点 | 现有 businessType | 目标 bizType / fileKind | 级别 | 状态 |
|------|--------------|-----------|-------------------|--------------------------|------|------|
| 请检 | 请检单附件 | `inspection_request/components/InspectionRequestDialog.vue`、`InspectionRequestDetailDialog.vue` | `QUALITY_INSPECTION_RECORD` | `INSPECTION_REQUEST` / `DEFAULT` | B | 已接入 |
| 取样 | 取样记录附件 | `sampling_record/modules/sampling-record-dialog.vue`、`sampling-record-detail.vue` | `QUALITY_SAMPLING_RECORD` | `SAMPLING_RECORD` / `DEFAULT` | B | 已接入 |
| 检验 | 检验记录/图谱 | **无独立上传点**（当前挂在请检单的“请检记录”槽位） | `QUALITY_INSPECTION_RECORD` | `INSPECTION_RECORD` / `DEFAULT` | A | 部分接入 |
| 放行 | 审核放行附件 | `release_review/components/ReleaseReviewDialog.vue` | `AUDIT_RELEASE_FILE` | `RELEASE_REVIEW` / `DEFAULT` | A | 已接入 |
| 偏差 | 偏差附件 | `deviation/components/DeviationDialog.vue`、`DeviationDetail.vue` | `QUALITY_DEVIATION_RECORD` | `DEVIATION_RECORD` / `DEFAULT` | A | 已接入 |
| 销毁 | 销毁记录附件 | `destroy_records/components/DestructionDialog.vue`、`DestructionViewDialog.vue` | `QUALITY_DESTRUCTION_RECORD` | `DESTRUCTION_RECORD` / `DEFAULT` | A | 已接入 |
| 留样 | 留样记录附件 | `retained_sample/components/RetainedSampleDialog.vue` | `SAMPLE_RETENTION_FILE` | `RETAINED_SAMPLE` / `DEFAULT` | B | 已接入 |

> 正式版建议给 `inspection_record` 增加独立的检验报告上传槽位，避免报告与请检单混挂。

### 3.6 生产支持类（不进入批次文件总数）

| 模块 | 槽位 | 现有 businessType | 建议 bizType | 级别 |
|------|------|-------------------|--------------|------|
| 生产单位 | 仓库材料文件 | `GENERAL_ATTACHMENT` | `PRODUCTION_UNIT` / `DEFAULT` | C |
| 设备 | 设备信息附件 | `EQUIPMENT_FILE` | `EQUIPMENT` / `DEFAULT` | B |
| 设备维保 | 维保记录文件 | `EQUIPMENT_MAINTENANCE` | `EQUIPMENT_MAINTENANCE` / `DEFAULT` | B |
| 能耗 | 能耗记录附件 | `ENERGY_FILE` | `ENERGY_RECORD` / `DEFAULT` | C |
| 养护 | 主单/明细附件 | `MAINTENANCE_RECORD` / `MAINTENANCE_DETAIL` | `MAINTENANCE_RECORD(_DETAIL)` / `DEFAULT` | B |
| 房间 | 消毒/洁净检测/清洁规程 | `ROOM_*` | `ROOM_INFO` / 对应 fileKind | B |
| 房间日志 | 清洁/洁净检测记录 | `ROOM_*` | `CLEANING_RECORD` / `CLEAN_INSPECTION_RECORD` / `DEFAULT` | B |

---

## 4. 扫描发现的关键问题

1. **领料、入库、出库没有前端附件位**，但单据已存在。汇总页只能展示单据详情，不能展示附件；建议正式版补三个 `FileUpload` 槽位。
2. **检验记录没有独立上传点**。当前检验报告挂在请检单的 `QUALITY_INSPECTION_RECORD` 槽位，汇总页应按“检验/请检”归并展示，避免重复计数。
3. **旧类型与新注册表并存**。前端仍大量使用 `PREPARATION_DOCUMENT`、`PRODUCTION_TASK`、`PRODUCTION_PROCESS` 等旧编码；后端 `BizFileType` 已切换为一表一码。正式版需要统一映射或迁移。
4. **生产计划归档校验查不到附件**（既有问题）：`production_plan/index.vue` 用 `PRODUCTION_TASK + customPath='生产批记录上传'` 查询，而实际上传路径是 `生产批记录上传/{workOrderId}`，精确匹配永远为空，导致“归档前必须上传生产批记录”的校验失效。
5. **品种级资料被按批次重复计数**。产品文件、批记录模板是品种级共用资料；原型已改为按品种去重统计，导出时可选“每批副本”或“统一放 00_产品资料”。
6. **旧页面 `preparation_info` 与新页面 `preparation_management` 重复**，有 10 个重复上传槽位；建议正式版删除旧页面，避免汇总页重复取数。
7. **格式白名单前后端不一致**：前端 accept 允许 `.xlsx`、`.zip`、`.rar`，但 `application.yml` 的 `file.allowed-types` 未配置对应 MIME；Java 默认值里有的 `xlsx/zip/rar` 被 YAML 覆盖后丢失。建议统一。
8. **检验记录独立上传位缺失**（见 3.5）：当前只能从请检单的“请检记录”槽位取检验报告，汇总页按检验环节归并展示。
9. **工序状态没有按时间回写**：`work_order_process_execution` 的 `start_time/end_time` 已有值，但 `status` 仍全部是“待执行”。原型已按“有结束时间=已完成、有开始时间=执行中、无时间=待执行”推导展示，并在工序节点标注该问题；建议后端在工序开始/结束时回写状态。

---

## 5. 原型优化内容（v3）

### 5.1 交互：保留原卡片层级，增加列表切换

- 保留原来的卡片展现：**品种卡片 → 品种内工单卡片 → 批次时间轴节点**。
- 顶部右上角提供“卡片 / 列表”手动切换；列表视图跟随当前上下文：在品种内切列表只显示该品种批次，在工单内切列表只显示该工单，不会跳回全部；旁边提供“查看全部”。
- 品种卡片：品种名称、规格、计划数、批次数、附件数、最近计划、最近批次、未下达工单数、合格率、缺件批次数、状态占比条。
- 工单卡片：工单号、批号、状态、缺件数、计划/物料来源、8 个流程节点圆点、计划类型、生产单位、开工/完工、工序完成数、物料单据数、出入库单数、检验单号、得率、成品/批量、附件数、缺件提示。
- 批次页：时间轴节点显示计划、采购/领料、工单、生产、入库、出库、检验、放行；每个节点带圆点状态、单号、摘要和附件 chip（`附件 N` / `附件 N · 缺 M` / `附件位未接入`）。
- 品种资料统一为一个“品种资料”入口，不再拆成“产品文件”和“批记录模板”两张卡；工艺规程、质量标准、批记录模板、验证方案、工艺流程图、工序模板都在品种资料抽屉内按槽位细分。
- 你补充的 579号资料、备案凭证/注册批件、包装设计稿、验证报告已放入品种资料，并标记为“待确认/未接入”，暂不参与缺件统计。
- 去掉批次勾选、底部选择条、状态页签和导出弹窗；卡片/列表的“导出”直接导出单个批次，顶部“导出当前结果”按当前筛选一次导出。
- 去掉图片中那条“本品种附件 50 个”的汇总条，信息分散到品种卡片、工单卡片和节点 chip 上。

### 5.2 缺件直观展示

- 列表“附件”列、工单卡片“附件”行都直接显示：附件总数、**缺 N**，并列出前两个缺件名称（如“工艺流程图、生产计划附件 等 13 项”）。
- 工单卡片顶部显示“缺 N”徽标；品种卡片显示该品种的缺件批次数。
- 批次时间轴每个节点显示附件 chip：`附件 N`、`附件 N · 缺 M` 或虚线“附件位未接入”。
- 批次页顶部固定显示红色“缺件 N 项”清单，逐条列出环节、文件组和单号，例如：
  - `计划` 生产计划附件 · Plan202610190001
  - `采购` 采购订单附件 · CGDH202610190010
  - `工单` 批生产记录 · GD202610200001
  - `检验` 请检单附件 · QJ-20261020-003
  - `放行` 放行附件 · FX-20261020-001
- 点击节点附件 chip 打开右侧附件抽屉，按文件组展示真实文件，并在末尾再次列出缺件清单。
- 顶部统计改为“槽位概览”：每个槽位显示已传/应传，缺的标红；进入批次页时隐藏全局概览，改成该批次的槽位状态条。
- “不必传”槽位统一用黄色标签：
  - 顶部槽位条第二行单独列出“不必传”槽位；
  - 时间轴节点显示黄色“不必传 N”标签，例如生产节点显示“工序附件 不必传”；
  - 右侧附件详情按槽位分组，组头显示“必传 / 条件必传 / 不必传”标签，空槽位也会以黄框列出。
- 缺件按环节展示：
  - 列表/卡片提示按环节汇总，例如“缺：计划 1、采购 2、工单 1 等 6 个环节，共 10 项”；
  - 批次页缺件清单带环节标签，逐条显示“环节 · 文件槽位 · 单号 · bizType/fileKind”；
  - 右侧附件抽屉按槽位分组，组头显示槽位名称和 `bizType / fileKind`；
  - 时间轴节点 chip 直接显示该环节缺几项。
- 支持“只看缺件”筛选，直接筛出有缺件的批次。
- 缺件判定已按确认表调整：可空缺项（工艺流程图、发票、其他文件、工序附件等）和标红“不必传”项不计入缺件；缺件按单据/记录逐条统计，当前为 45 项，与槽位概览中的“缺 N”合计一致。附件抽屉底部有“文件要求”说明。

### 5.3 单据明细

- 采购订单、领料单、入库单、出库单不再只显示单号和金额；在时间轴节点展开后，单据卡片下方直接展示明细表：
  - 采购：物料编码、物料名称、分类、数量、单位、供应商、状态
  - 领料：物料编码、物料名称、分类、数量、单位、批次
  - 入库/出库：物料编码、名称、分类、数量、单位、批次
- 工序：工序名称、房间、操作人、加工数量、开始/结束时间、状态。
- 检验：请检单（时间、工序、请检人、检验项目）和检验记录（起止、项目、检验人、结论）。
- 放行：放行单号、时间、结论、审核人、意见。

### 5.4 工序状态按时间推导

- 库表 `work_order_process_execution.status` 当前全部为“待执行”，但 `start_time/end_time` 已有值。
- 原型改为按时间推导状态：
  - 有 `end_time` → **已完成**
  - 有 `start_time`、无 `end_time` → **执行中**
  - 两者都没有 → **待执行**
- 工序节点展开后，在表格下方显示黄色提示：库表状态未回写，建议后端在工序开始/结束时更新 `status`。
- 生产节点的圆点状态也按工序时间推导：有完成工序 → 完成；有开始工序 → 进行中；否则待执行。

### 5.5 真实数据快照

- 数据不再手写模拟，改为从测试库 `8.134.48.54/erp_db` **只读**查询后生成快照：
  - 7 个生产计划、5 张工单、9 张采购订单、1 张生产领料单、13 张入库单、17 张出库单、12 张请检单、11 条检验记录、3 条放行记录。
  - 采购明细 89 行、领料明细 5 行、入库明细 92 行、出库明细 98 行。
  - 附件 110 条，来自 `business_file` + `file_info`。
- 物料来源判定：同一计划有 `production_pick` 时走领料，否则走采购；采购与领料不重复展示。
- 数据生成脚本：`_analysis/tools/build_product_summary_data.py`；快照：`_analysis/real-summary-data.json`。

### 5.6 导出

- 顶部“导出当前结果”：按当前品种/时间/关键字/缺件筛选导出全部批次。
- 每行“导出”和详情底部“导出本批次”：导出单个批次。
- 导出内容包含：品种、计划编号、工单号、批号、物料来源、环节/文件组、文件名或缺失说明、大小、上传人、时间、状态（已上传/缺失）。
- 原型导出 CSV 文件清单；正式版再接 zip 打包接口。

### 5.7 视觉与交互

- 品种卡片、工单卡片、时间轴节点保持原有视觉风格；附件缺件统一红色提示。
- 原型界面不再展示 A/B/C 分级；分级只保留在本说明和后端归档策略中，避免卡片信息过密。
- 时间轴节点点击展开单据明细，默认折叠，避免首屏过长；批次页顶部缺件清单用双列展示。
- 附件抽屉固定宽度 780px，移动端全屏。

---

## 6. 正式版建议（本次未实现）

1. 新增聚合接口，例如 `GET /api/product-summary/chain?varietyCode=&dateType=&start=&end=&keyword=`，一次返回计划、采购、领料、工单、生产、入库、出库、检验、放行、偏差及文件统计。
2. 文件聚合按 `bizType + bizId` 批量查询，避免逐单据 N+1；前端只拿“文件组 + 等级 + 数量 + 元数据”。
3. 新增导出接口，例如 `POST /api/product-summary/export`，按品种/批次生成 zip；zip 内按 `品种/批次/环节` 组织，附 `文件清单.csv`（含缺失项）。
4. 补领料、入库、出库三个前端上传槽位；补检验记录独立上传槽位。
5. 统一旧 `businessType` 到 `BizFileType`，迁移期保留映射层。
6. 对 A 级文件做完整性校验：批记录、检验报告、放行审核缺失时，导出前强提示。

---

## 7. 交付物

| 文件 | 说明 |
|------|------|
| `_analysis/prototypes/product-summary.html` | 产品信息汇总独立原型（真实数据快照） |
| `_analysis/prototypes/inspection-management.html` | 检验管理独立原型 |
| `_analysis/prototypes/release-review.html` | 审核放行独立原型 |
| `docs/product-summary-file-classification.md` | 本文档：需求完成度、上传点扫描、分类分级、优化说明 |
| `_analysis/tools/scan_upload_points.py` | 前端上传点扫描脚本（可复跑） |
| `_analysis/tools/upload_points.tsv` | 扫描结果明细（95 个组件实例） |
| `_analysis/tools/build_product_summary_data.py` | 测试库只读数据快照生成脚本 |
| `_analysis/real-summary-data.json` | 原型使用的真实数据快照 |
| `docs/product-chain-file-checklist.md` | 生产链路文件清单（必传/可空缺确认表） |
| `docs/product-chain-file-checklist.csv` | 同一清单的 Excel 版，可直接填写“确认结果” |

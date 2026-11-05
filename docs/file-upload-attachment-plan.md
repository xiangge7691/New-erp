# 文件上传与附件列表改造计划

> 分析对象：前端仓库 `art-design-pro-main`（master，2026-10-31 提交）与运行中的 `http://192.168.5.84:3006`
> 后端仓库：`New-erp`（v1.0）
> 分析日期：2026-10-31
>
> 落地细化版见 [`business-file-implementation.md`](./business-file-implementation.md)。其中审批场景由 `APPROVAL_NODE` 调整为 `APPROVAL_INSTANCE + ref_id(节点ID)`，因为 `approval_node` 属于流程定义，多个实例共用同一节点 ID。

## 0. 结论摘要

1. 前端共发现 **95 个 `FileUpload` 组件实例**（分布在 42 个 `.vue` 文件），另有审批模块 **7 个文件**使用原生 `ElUpload` / `FormData` 直接上传。按业务槽位归并后，**有效业务上传入口约 50 个**。
2. 这些上传点最终归属 **34 张业务表**（见第 3 节）。
3. 现状是“物理文件表 `file_info` + `business_type` + `business_id` + `custom_path`”的松散关联，前后端各自硬编码类型字符串，已经出现以下实际问题：
   - 同一个 `business_type` 被多张表复用（如剂型与制剂共用 `PREPARATION_DOCUMENT`），ID 相同时互相串数据；
   - 前后端类型字符串不一致（能耗、机构许可证、养护记录），后端按自己的类型查不到前端上传的文件；
   - 审批模块直接把**审批节点 ID 当业务类型**使用；
   - `custom_path` 同时承载“槽位”和“业务 ID”，精确匹配导致漏查（生产计划归档检查永远查不到生产批记录）；
   - 新增记录时先上传到 `business_id=0`，保存后再二次绑定，失败会产生孤儿文件。
4. 建议方案：**保留 `file_info` 作为物理文件表，新增统一业务附件关系表 `business_file` + 后端 `BizFileType` 注册表**，查询只依赖 `(biz_type, biz_id, file_kind)`；`biz_type` 一表一码、由注册表校验，`file_kind` 表达槽位（随货清单/发票/检验报告等）。**不建议**为 34 张业务表各建一张结构相同的附件子表。
5. 迁移按“先修高风险冲突（剂型/制剂、机构/机构证书、审批节点、房间日志、养护记录），再迁单槽位模块，最后迁多槽位模块”的顺序推进；旧接口 `/api/files/business` 保留一个版本做兼容。

---

## 1. 现状实现

### 1.1 后端

- 物理文件表：`file_info`（`FileInfo`），字段含 `file_id`、`original_name`、`file_path`、`file_url`、`business_id`、`business_type`、`custom_path`、`category`、`is_deleted` 等。
- 上传接口：
  - `POST /api/files/upload-business`：按业务路径存盘，写 `business_type + business_id + custom_path`；
  - `POST /api/files/upload`：旧接口，审批模块仍在使用。
- 查询接口：`GET /api/files/business?businessId=&businessType=&customPath=`，SQL 条件为：

  ```sql
  WHERE business_id = ? AND business_type = ? [AND custom_path = ?]
  ```

- 各业务模块另有一批“按业务类型查附件”的实现，类型字符串散落在 Service / Controller 中，例如：
  - `EquipmentMaintenanceController`：`EQUIPMENT_MAINTENANCE`
  - `EnergyRecordServiceImpl`：`ENERGY_RECORD`
  - `MaintenanceRecordServiceImpl`：`STOCK_MAINTENANCE_RECORD` / `STOCK_MAINTENANCE_DETAIL`
  - `OrganizationController`：`ORGANIZATION_LICENSE`
  - `OrganizationCertificateController`：`ORGANIZATION_CERTIFICATE`
  - `PersonnelFileController`：`PERSONNEL_ATTACHMENT` / `PERSONNEL_HEALTH_FILE`
  - `PersonnelCertificateController`：`PERSONNEL_CERTIFICATE`
  - `DestructionRecordServiceImpl`：`QUALITY_DESTRUCTION_RECORD`
  - `DeviationRecordServiceImpl`：`QUALITY_DEVIATION_RECORD`
- `Common/Constants/FileBusinessType.java` 只定义了少数常量，且 **无任何调用方**。

### 1.2 前端

- 统一组件：`src/components/upload/file-upload.vue`
  - 上传时提交 `businessType`、`businessId`、`category`、`customPath`；
  - 组件内部把 `customPath` 拼成 `${props.customPath}/${businessId}`，即使没有传 `customPath`，实际也会存成 `"/{businessId}"`；
  - 加载时用同样的拼接值做**精确匹配**。
- API：`src/api/files.ts`
  - `uploadBusinessFile`、`getBusinessFiles`、`deleteBusinessFile`、`downloadFile`、`previewFile`。
- 预览组件：`src/components/upload/file-preview-dialog.vue`，只读，内部复用 `FileUpload`。
- 审批模块未使用统一组件，直接用 `ElUpload` + `FormData` 调 `/api/files/upload`。

---

## 2. 前端上传点清单

### 2.1 总览

| 指标 | 数量 |
|------|------|
| `FileUpload` 组件实例 | 95 |
| 使用 `FileUpload` 的 `.vue` 文件 | 42 |
| 审批模块原生上传文件 | 7（审批抽屉 + 5 个补录弹窗 + 创建工单绑定弹窗） |
| 归并后的业务上传槽位 | 约 50 |
| 归属业务表 | 34 |

### 2.2 按模块列出的上传点

> “建议 bizType / fileKind”是本次改造后的目标编码，`bizType` 一表一码，`fileKind` 表示同一记录下的不同文件槽位。

#### 货物验收

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `acceptance_inspection/components/AcceptanceDetail.vue`、`AcceptanceViewDialog.vue` | 随货清单 | `GOODS_ACCEPTANCE_WAYBILL` | `detail.acceptanceId` | `acceptance_order`（`acceptance_id`） | `ACCEPTANCE_ORDER` / `WAYBILL` |
| 同上 | 发票 | `GOODS_ACCEPTANCE_INVOICE` | `detail.acceptanceId` | `acceptance_order` | `ACCEPTANCE_ORDER` / `INVOICE` |
| 同上 | 检验报告 | `GOODS_ACCEPTANCE_INSPECTION_REPORT` | `detail.acceptanceId` | `acceptance_order` | `ACCEPTANCE_ORDER` / `INSPECTION_REPORT` |

#### 质量管理

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `destroy_records/components/DestructionDialog.vue`、`DestructionViewDialog.vue` | 销毁记录附件 | `QUALITY_DESTRUCTION_RECORD` | `currentBusinessId` / `detail.id` | `destruction_record`（`id`） | `DESTRUCTION_RECORD` / `DEFAULT` |
| `deviation/components/DeviationDialog.vue`、`DeviationDetail.vue` | 偏差附件 | `QUALITY_DEVIATION_RECORD` | `form.id` / `props.deviationId` | `deviation_record`（`id`） | `DEVIATION_RECORD` / `DEFAULT` |
| `inspection_request/components/InspectionRequestDialog.vue`、`InspectionRequestDetailDialog.vue` | 请检单附件 | `QUALITY_INSPECTION_RECORD` | `form.id` / `data.id` | `inspection_request`（`id`） | `INSPECTION_REQUEST` / `DEFAULT` |
| `sampling_record/modules/sampling-record-dialog.vue`、`sampling-record-detail.vue` | 取样记录附件 | `QUALITY_SAMPLING_RECORD` | `formData.id` / `recordData.id` | `sampling_record`（`id`） | `SAMPLING_RECORD` / `DEFAULT` |
| `release_review/components/ReleaseReviewDialog.vue` | 审核放行附件 | `AUDIT_RELEASE_FILE` | `props.record.id` / 新增后的 `newId` | `release_review`（`id`） | `RELEASE_REVIEW` / `DEFAULT` |
| `retained_sample/components/RetainedSampleDialog.vue` | 留样附件 | `SAMPLE_RETENTION_FILE` | `props.record.id` / 新增后的 `newId` | `retained_sample`（`id`） | `RETAINED_SAMPLE` / `DEFAULT` |

#### 制剂与剂型

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `dosage_management/index.vue` | 批记录模板 | `PREPARATION_DOCUMENT` | `editingId`（剂型 ID） | `dosage_form`（`dosage_id`） | `DOSAGE_FORM` / `BATCH_RECORD_TEMPLATE` |
| 同上 | 验证模板 | `PREPARATION_DOCUMENT` | `editingId` | `dosage_form` | `DOSAGE_FORM` / `VERIFICATION_TEMPLATE` |
| 同上 | 检验模板 | `PREPARATION_DOCUMENT` | `editingId` | `dosage_form` | `DOSAGE_FORM` / `INSPECTION_TEMPLATE` |
| `preparation_management/index.vue` | 工艺规程 | `PREPARATION_DOCUMENT` | `editingId`（制剂 ID） | `preparation`（`preparation_id`） | `PREPARATION` / `PROCESS_SPEC` |
| 同上 | 批记录模板 | `PREPARATION_DOCUMENT` | `editingId` | `preparation` | `PREPARATION` / `BATCH_RECORD_TEMPLATE` |
| 同上 | 验证方案 | `PREPARATION_DOCUMENT` | `editingId` | `preparation` | `PREPARATION` / `VERIFICATION_PLAN` |
| 同上 | 质量标准 | `PREPARATION_DOCUMENT` | `editingId` | `preparation` | `PREPARATION` / `QUALITY_STANDARD` |
| 同上 | 工艺规程流程图 | `PREPARATION_DOCUMENT` | `editingId` | `preparation` | `PREPARATION` / `PROCESS_FLOWCHART` |
| 同上 | 工序模板（按工序行） | `PREPARATION_PROCESS` | `editingId` + `customPath=templateId` | `preparation_process_template`（`template_id`） | `PREPARATION_PROCESS_TEMPLATE` / `DEFAULT` |
| `preparation_info/index.vue`（旧页面，未挂路由） | 与制剂管理重复的 10 个槽位 | `PREPARATION_DOCUMENT` | `editingId` | `preparation` | 建议删除该旧页面，不迁移 |

#### 生产管理

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `production_plan/components/ProductionPlanDialog.vue`、`ProductionPlanDetail.vue` | 计划附件 | `PRODUCTION_PLAN` | `editId` / `detail.id` | `production_plan`（`id`） | `PRODUCTION_PLAN` / `DEFAULT` |
| `production_task/components/TaskDetail.vue` | 生产批记录上传 | `PRODUCTION_TASK` | `detail.workOrderId` | `work_order`（`work_order_id`） | `WORK_ORDER` / `BATCH_RECORD` |
| 同上 | 其他文件上传 | `PRODUCTION_TASK` | `detail.workOrderId` | `work_order` | `WORK_ORDER` / `OTHER` |
| 同上 | 工序附件 | `PRODUCTION_PROCESS` | `detail.workOrderId` + `customPath=工序行 ID` | `work_order_process_execution`（`id`） | `WORK_ORDER_PROCESS_EXECUTION` / `DEFAULT` |
| 同上 | 制剂工艺规程流程图（只读） | `PREPARATION_DOCUMENT` | `detail.preparationId` | `preparation` | `PREPARATION` / `PROCESS_FLOWCHART` |
| `production_unit_management/index.vue` | 生产单位附件 | `GENERAL_ATTACHMENT` | `businessId`（`prodUnitId`） | `production_unit`（`prod_unit_id`） | `PRODUCTION_UNIT` / `DEFAULT` |

#### 设备与能耗

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `equipment/index.vue` | 设备附件 | `EQUIPMENT_FILE` | `equipmentId` | `equipment`（`equipment_id`） | `EQUIPMENT` / `DEFAULT` |
| `equipment/maintenance_dig/maintenanceDig.vue` | 维保附件 | `EQUIPMENT_MAINTENANCE` | `maintenanceId` | `equipment_maintenance`（`maintenance_id`） | `EQUIPMENT_MAINTENANCE` / `DEFAULT` |
| `maintenance_log_common/detail-dig/detailDig.vue` | 维保附件 | `EQUIPMENT_MAINTENANCE` | `maintenanceId` | `equipment_maintenance` | `EQUIPMENT_MAINTENANCE` / `DEFAULT` |
| `energy/modules/energy-dialog.vue`、`energy-detail.vue` | 能耗凭证 | `ENERGY_FILE` | `recordId` | `energy_record`（`record_id`） | `ENERGY_RECORD` / `DEFAULT` |
| `maintenance_record/components/MaintenanceRecordDialog.vue`、`MaintenanceRecordDetail.vue` | 主单附件 | `MAINTENANCE_RECORD` | `form.id` / `detail.id` | `maintenance_record`（`id`） | `MAINTENANCE_RECORD` / `DEFAULT` |
| 同上 | 明细附件（按明细行） | `MAINTENANCE_DETAIL` | `detail.id` | `maintenance_record_detail`（`id`） | `MAINTENANCE_RECORD_DETAIL` / `DEFAULT` |

#### 机构、人员、岗位

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `organization/index.vue` | 许可证扫描件 | `ORGANIZATION_CERTIFICATE`（后端读取用 `ORGANIZATION_LICENSE`） | `orgInfo.id` | `organization`（`id`） | `ORGANIZATION` / `LICENSE_SCAN` |
| 同上 | 机构照片 | `ORGANIZATION_PHOTO` | `orgInfo.id` | `organization` | `ORGANIZATION` / `PHOTO` |
| 同上（证书弹窗） | 机构证书文件 | `ORGANIZATION_CERTIFICATE` | `editingCertId` | `organization_certificate`（`id`） | `ORGANIZATION_CERTIFICATE` / `DEFAULT` |
| `personnel_file/personnelFileDialog.vue` | 人员档案附件 | `PERSONNEL_ATTACHMENT` | `currentRecordId` | `personnel_file`（`personnel_file_id`） | `PERSONNEL_FILE` / `ATTACHMENT` |
| `personnel_file/certificateDialog.vue` | 人员证书文件 | `PERSONNEL_CERTIFICATE` | `personnelFileId` + `customPath=证书类型_证书ID` | `personnel_certificate`（`certificate_id`） | `PERSONNEL_CERTIFICATE` / `DEFAULT` |
| `job_info/index.vue` | 岗位附件 | `POSITION_FILE` | `positionId` | `position`（`position_id`） | `POSITION` / `DEFAULT` |
| `position_info/index.vue` | 职务附件 | `DUTY_FILE` | `dutyId` | `duty`（`duty_id`） | `DUTY` / `DEFAULT` |

#### 采购与供应商

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `purchase_order/modules/purchase-order-dialog.vue`、`purchase-order-detail.vue` | 采购订单附件 | `PURCHASE_FILE` | `formData.id` / `orderData.id` | `purchase_orders`（`id`） | `PURCHASE_ORDER` / `DEFAULT` |
| `supplier_info_management/index.vue` | 供应商附件 | `SUPPLIER_FILE` | `editingId`（供应商 ID） | `purchase_suppliers`（`id`） | `PURCHASE_SUPPLIER` / `DEFAULT` |
| `supplier_management/index.vue` | 供应商审计附件 | `SUPPLIER_ATTACHMENT` | `editingId`（审计 ID） | `supplier_audit`（`id`） | `SUPPLIER_AUDIT` / `DEFAULT` |

#### 车间环境与房间日志

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `room/index.vue`、`room/roomDetailDialog.vue` | 消毒记录 | `ROOM_DISINFECTION` | `roomId` | `room_info`（`room_id`） | `ROOM_INFO` / `DISINFECTION` |
| 同上 | 洁净检测 | `ROOM_CLEAN_INSPECTION` | `roomId` | `room_info` | `ROOM_INFO` / `CLEAN_INSPECTION` |
| 同上 | 清洁记录 | `ROOM_CLEANING_RECORD` | `roomId` | `room_info` | `ROOM_INFO` / `CLEANING_RECORD` |
| `room_log/roomCleaningRecord.vue` | 清洁记录附件 | `ROOM_CLEANING_RECORD` | `roomId`（未使用记录 ID） | `cleaning_record`（`id`） | `CLEANING_RECORD` / `DEFAULT` |
| `room_log/roomCleanTest.vue` | 洁净检测附件 | `ROOM_CLEAN_INSPECTION` | `roomId`（未使用记录 ID） | `clean_inspection_record`（`id`） | `CLEAN_INSPECTION_RECORD` / `DEFAULT` |

#### 培训、验证

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `training_management/index.vue` | 培训附件 | `TRAINING_ATTACHMENT` | `row.id` | `training_record`（`id`） | `TRAINING_RECORD` / `DEFAULT` |
| `validation_plan/index.vue` | 验证附件 | `VERIFICATION_FILE`（`FILE_BUSINESS_TYPE`） | `row.id` | `verification_record`（`id`） | `VERIFICATION_RECORD` / `DEFAULT` |

#### 审批模块（原生上传）

| 页面 | 上传槽位 | 现 businessType | 现 ID 来源 | 归属表（主键） | 建议 bizType / fileKind |
|------|---------|-----------------|-----------|----------------|--------------------------|
| `approval_management/workbench/modules/approval-drawer.vue` | 审批附件 | `String(currentNodeId)`（**数字节点 ID 当类型**） | `approval_instance.id` | `approval_node`（`id`） | `APPROVAL_NODE` / `DEFAULT` |
| `order-refill-dialog.vue` | 销售清单、付款凭证、回款申请单、回款明细、付款发票、医院收款凭证 | `String(nodeId)` | `instanceId` | `approval_node` | `APPROVAL_NODE` / 各 `fileKind` |
| `order-market-refill-dialog.vue` | 市场补录材料 | `String(nodeId)` | `instanceId` | `approval_node` | `APPROVAL_NODE` / 各 `fileKind` |
| `order-quality-refill-dialog.vue` | 销售清单、随货清单、检验报告、发票、其他 | `String(nodeId)` | `instanceId` | `approval_node` | `APPROVAL_NODE` / 各 `fileKind` |
| `plan-refill-dialog.vue` | 计划补录材料 | `String(nodeId)` | `instanceId` | `approval_node` | `APPROVAL_NODE` / 各 `fileKind` |
| `purchase-refill-dialog.vue` | 随货清单、检验报告、发票、其他 | `String(nodeId)` | `instanceId` | `approval_node` | `APPROVAL_NODE` / 各 `fileKind` |
| `components/approval-bind/work-order-create-bind-dialog.vue` | 创建工单绑定附件 | `String(nodeId)` | `instanceId` / `workOrderId` | `approval_node` | `APPROVAL_NODE` / `DEFAULT` |

#### 非业务附件（不在本次改造范围）

| 页面 | 说明 |
|------|------|
| `file_management/index.vue` | 独立文件管理器，按目录 `rootType` 管理 `file_info`，不关联业务表 |
| `components/core/forms/art-excel-import/index.vue` | Excel 导入选择器，只读取本地文件，不上传附件 |

### 2.3 只读查询点（同样依赖 type + id，需要同步改造）

| 位置 | 查询方式 | 问题 |
|------|---------|------|
| `production_plan/index.vue` `getFileList` | `PRODUCTION_TASK` + `customPath='生产批记录上传'` | 实际上传存的是 `生产批记录上传/{workOrderId}`，精确匹配**永远查不到**，导致“归档前必须上传生产批记录”的校验形同虚设 |
| `production_task/index.vue` `getFileList` | `PRODUCTION_TASK` + `customPath='生产批记录上传/{workOrderId}'` | 与上传值一致，可查到；但与上一个查询写法不统一 |
| `approval-drawer.vue` `fetchInstanceAttachments` | 遍历所有节点 ID 逐个调用 `/api/files/business` | N+1 查询；节点多时性能差 |
| `approval-drawer.vue` `fetchBusinessFiles` | 用 `currentNodeId` 查询业务附件 | 当前节点不是附件所属节点时会查错 |
| `file-preview-dialog.vue` | 透传 `businessType` + `businessId` + `customPath` | 依赖上游传参正确性 |

---

## 3. 需要文件列表的业务表（34 张）

| 分组 | 业务表 | 主键 | 文件槽位 |
|------|--------|------|---------|
| 验收 | `acceptance_order` | `acceptance_id` | 随货清单、发票、检验报告 |
| 质量 | `destruction_record` | `id` | 默认 |
| 质量 | `deviation_record` | `id` | 默认 |
| 质量 | `inspection_request` | `id` | 默认 |
| 质量 | `sampling_record` | `id` | 默认 |
| 质量 | `release_review` | `id` | 默认 |
| 质量 | `retained_sample` | `id` | 默认 |
| 质量 | `verification_record` | `id` | 默认 |
| 制剂 | `dosage_form` | `dosage_id` | 批记录模板、验证模板、检验模板 |
| 制剂 | `preparation` | `preparation_id` | 工艺规程、批记录模板、验证方案、质量标准、工艺规程流程图 |
| 制剂 | `preparation_process_template` | `template_id` | 默认（工序模板） |
| 生产 | `production_plan` | `id` | 默认 |
| 生产 | `work_order` | `work_order_id` | 生产批记录、其他文件 |
| 生产 | `work_order_process_execution` | `id` | 默认（工序附件） |
| 生产 | `production_unit` | `prod_unit_id` | 默认 |
| 设备 | `equipment` | `equipment_id` | 默认 |
| 设备 | `equipment_maintenance` | `maintenance_id` | 默认 |
| 能耗 | `energy_record` | `record_id` | 默认 |
| 养护 | `maintenance_record` | `id` | 默认（主单） |
| 养护 | `maintenance_record_detail` | `id` | 默认（明细） |
| 机构 | `organization` | `id` | 许可证扫描件、机构照片 |
| 机构 | `organization_certificate` | `id` | 默认 |
| 人员 | `personnel_file` | `personnel_file_id` | 默认（档案附件） |
| 人员 | `personnel_certificate` | `certificate_id` | 默认（证书文件） |
| 岗位 | `position` | `position_id` | 默认 |
| 职务 | `duty` | `duty_id` | 默认 |
| 采购 | `purchase_orders` | `id` | 默认 |
| 供应商 | `purchase_suppliers` | `id` | 默认 |
| 供应商 | `supplier_audit` | `id` | 默认 |
| 房间 | `room_info` | `room_id` | 消毒记录、洁净检测、清洁记录 |
| 房间 | `cleaning_record` | `id` | 默认 |
| 房间 | `clean_inspection_record` | `id` | 默认 |
| 培训 | `training_record` | `id` | 默认 |
| 审批 | `approval_node` | `id` | 默认（审批实例附件按节点归属） |

> 说明：`file_info` 仍是物理文件表，不删除；上述 34 张表通过 `business_file` 关系表获得“文件列表”能力。

---

## 4. 现存不稳定问题（带证据）

1. **同一 `business_type` 被多张表复用**
   - `PREPARATION_DOCUMENT` 同时用于 `dosage_form`（剂型）和 `preparation`（制剂），两者 ID 都从 1 开始增长，ID 相同时会互相读到对方的“批记录模板/验证方案”。
   - `ORGANIZATION_CERTIFICATE` 同时用于 `organization`（许可证扫描件）和 `organization_certificate`（机构证书）。
   - `PERSONNEL_CERTIFICATE` 的 `business_id` 是 `personnel_file_id`，证书 ID 只存在于 `custom_path` 字符串里。
2. **前后端类型字符串不一致**
   - 能耗：前端 `ENERGY_FILE`，后端 `ENERGY_RECORD`。
   - 机构许可证：前端 `ORGANIZATION_CERTIFICATE`，后端 `ORGANIZATION_LICENSE`。
   - 养护记录：前端 `MAINTENANCE_RECORD` / `MAINTENANCE_DETAIL`，后端 `STOCK_MAINTENANCE_RECORD` / `STOCK_MAINTENANCE_DETAIL`。
   - 结果：后端附件接口查不到前端上传的文件，或前端展示不到后端上传的文件。
3. **审批模块用节点 ID 当业务类型**
   - `business_type = String(currentNodeId)`，同一个实例不同节点的附件只能靠数字类型区分；节点删除/重建后类型失效。
4. **`custom_path` 重载**
   - 统一组件在没有 `customPath` 时也会存成 `"/{businessId}"`；
   - 查询必须和存储值完全一致，`production_plan/index.vue` 传 `'生产批记录上传'` 而实际存 `'生产批记录上传/{id}'`，导致查不到。
5. **新增记录先上传到 `business_id=0`**
   - 多数组件支持 `submitUpload(savedId)` 二次绑定，但任何一步失败都会留下 `business_id=0` 的孤儿文件；不同页面处理方式不一致。
6. **文件目录映射缺陷**
   - `FileTypeInitConfig` 只用 `businessType.split("_", 2)` 的第一段做父类型，`GOODS_ACCEPTANCE_WAYBILL`、`AUDIT_RELEASE_FILE`、`SAMPLE_RETENTION_FILE` 等多段父类型匹配不到 YAML 中的 `GOODS_ACCEPTANCE`、`AUDIT_RELEASE`、`SAMPLE_RETENTION`，最终落到英文目录名。
7. **无外键、无级联**
   - 业务记录删除后 `file_info` 仍保留 `business_id`；文件被文件管理器删除后业务侧仍可能引用。
8. **房间日志共享附件**
   - `room_log/roomCleaningRecord.vue`、`roomCleanTest.vue` 上传时 `business_id` 用的是 `roomId`，同一房间的多条清洁/洁净检测记录共用一份附件。
9. **审批附件查询 N+1**
   - 每个节点一次 `/api/files/business` 请求。

---

## 5. 改造方案

### 5.1 目标模型

```
file_info（物理文件，保留）
    file_id (PK)
    original_name / file_path / file_url / file_size / content_type / file_md5 ...

business_file（业务附件关系，新增）
    id (PK)
    biz_type   -- 业务类型编码，一表一码，由后端注册表校验
    biz_id     -- 业务表主键值
    file_id    -- 关联 file_info.file_id
    file_kind  -- 文件槽位/用途，如 WAYBILL、INVOICE、PROCESS_SPEC
    sort_order
    is_deleted / created_by / created_time / updated_by / updated_time
```

核心变化：

- **不再把业务含义放进 `custom_path`**：`custom_path` 只用于磁盘目录展示，不参与查询。
- **`biz_type` 一表一码**：如 `ACCEPTANCE_ORDER` 只对应 `acceptance_order`，`PREPARATION` 只对应 `preparation`，从根上消除同类型多表复用。
- **`file_kind` 表达槽位**：随货清单、发票、检验报告、生产批记录、工序附件等，均可精确过滤。
- **服务层校验父记录存在**：上传前按注册表拿到表名，校验 `biz_id` 对应记录存在且未删除；删除业务记录时同步解绑。
- 前端只传 `bizType + bizId + fileKind`，不再传 `businessType + customPath`。

### 5.2 表结构（建议）

```sql
CREATE TABLE IF NOT EXISTS business_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    biz_type      VARCHAR(64)  NOT NULL COMMENT '业务类型编码，一表一码',
    biz_id        BIGINT       NOT NULL COMMENT '业务表主键值',
    file_id       BIGINT       NOT NULL COMMENT '文件ID（file_info.file_id）',
    file_kind     VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '文件槽位/用途',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    created_by    BIGINT       NULL COMMENT '创建人',
    created_time  DATETIME     NULL COMMENT '创建时间',
    updated_by    BIGINT       NULL COMMENT '更新人',
    updated_time  DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id),
    KEY idx_biz (biz_type, biz_id, file_kind),
    KEY idx_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务附件关联表';
```

说明：

- 关系表建议**物理删除**（解绑），避免软删除唯一键冲突；`file_info` 继续走软删除/回收站。
- 不建数据库外键到各业务表（多态关联无法建 FK），由 `BizFileType` 注册表 + 服务层校验保证。
- 如需审计留痕，可增加 `biz_no` 快照字段（业务单号），便于文件管理页展示。

### 5.3 后端注册表与接口

新增 `Common/Constants/BizFileType.java`（或枚举）：

```java
public enum BizFileType {
    ACCEPTANCE_ORDER("acceptance_order", "acceptance_id", "货物验收单",
        Set.of("WAYBILL", "INVOICE", "INSPECTION_REPORT")),
    PREPARATION("preparation", "preparation_id", "制剂",
        Set.of("PROCESS_SPEC", "BATCH_RECORD_TEMPLATE", "VERIFICATION_PLAN",
               "QUALITY_STANDARD", "PROCESS_FLOWCHART")),
    // ...
    ;
}
```

新增接口（`BusinessFileController`）：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/business-files/upload` | 上传并绑定：`file`、`bizType`、`bizId`、`fileKind`、`description` |
| GET | `/api/business-files` | 查询：`bizType`、`bizId`、`fileKind`（可选） |
| DELETE | `/api/business-files/{id}` | 解绑并删除文件（走回收站/软删除） |
| POST | `/api/business-files/sync` | 保存单据时同步附件：`bizType`、`bizId`、`fileKind`、`fileIds[]` |
| GET | `/api/business-files/types` | 返回注册表，供前端生成常量/下拉 |

服务层要点：

- `upload`：保存物理文件 → 写 `file_info` → 写 `business_file`，同一事务；
- `list`：`business_file JOIN file_info`，过滤 `file_info.is_deleted = 0`；
- `sync`：差量新增/解绑，供 `MaintenanceRecordSaveDto.attachmentFileIds` 这类 DTO 使用；
- 删除业务记录：各 Service 删除前调用 `unbindAll(bizType, bizId)`；
- 兼容期：`/api/files/business` 保留，内部按旧 `business_type` 映射到新表，或双读旧 `file_info`。

### 5.4 前端改造

- `src/api/business-files.ts`：新增 `uploadBusinessFile`、`listBusinessFiles`、`deleteBusinessFile`、`syncBusinessFiles`。
- `src/constants/biz-file-type.ts`：与后端注册表一致的 `bizType` / `fileKind` 常量，禁止页面内再写裸字符串。
- `src/components/upload/file-upload.vue`：
  - props 改为 `bizType`、`bizId`、`fileKind`；
  - 移除 `businessType` / `customPath` 查询逻辑；
  - 保留 `submitUpload(id)`、`loadFiles()`、`syncByFileIds()`；
  - 旧 props 暂留兼容层，迁移完成后删除。
- `src/components/upload/file-preview-dialog.vue`：同步改为 `bizType` + `bizId` + `fileKind`。
- 新建记录流程统一为：**先保存业务记录拿到 ID，再上传并绑定**；或在 DTO 中提交 `fileIds` 由 `sync` 绑定，避免 `biz_id=0`。
- 审批模块改为 `bizType=APPROVAL_NODE`、`bizId=审批节点 ID`，列表按实例查节点后聚合（可一次查询，避免 N+1）。

### 5.5 为什么不建议建 34 张附件子表

| 对比项 | 统一 `business_file` 关系表 | 每表一张附件子表（34 张） |
|--------|----------------------------|---------------------------|
| 稳定性 | `biz_type` 一表一码 + 唯一键 + 服务校验，可消除串数据 | 外键天然稳定 |
| 代码量 | 1 套 Entity/Mapper/Service/Controller | 34 套重复代码或大量代码生成 |
| 文件管理/回收站/操作日志 | 单表查询，易做 | 需跨 34 表 UNION |
| 新增模块 | 注册表加一行 | 新建表 + Entity + Mapper + Service |
| 迁移成本 | 一次迁移所有类型 | 每表分别迁移 |
| 适用场景 | 多业务、槽位多、追求统一治理 | 强外键约束、表数量少 |

本项目业务表多、槽位差异大、已有统一文件管理模块，**推荐统一关系表**；如果后续某张表需要数据库级外键，再单独拆出子表。

---

## 6. 实施计划

### P0 盘点与冻结（0.5 天）

- 确认第 3 节 34 张表和第 2 节槽位清单；
- 确认审批附件归属模型（`APPROVAL_NODE`）；
- 冻结新增裸 `business_type` 字符串，代码评审拦截；
- 在 `8.134.48.54` 测试库统计现有 `file_info` 各 `business_type` / `custom_path` 分布，输出迁移评估报告。

### P1 数据库与注册表（1 天）

- 新增迁移脚本：`src/main/resources/Data/migration/create_business_file.sql`（幂等）；
- 新增 `BizFileType` 注册表（编码 ↔ 表名 ↔ 主键 ↔ 允许的 `fileKind`）；
- 新增 `BusinessFile` 实体、Mapper。

### P2 后端服务与接口（2–3 天）

- `BusinessFileService` / `BusinessFileServiceImpl`：
  - 上传、查询、解绑、同步、级联解绑；
  - 父记录存在性校验；
  - 文件操作日志沿用 `FileOperationLogService`；
- `BusinessFileController` 及接口文档注释（按项目规范补 Javadoc 和请求示例）；
- 兼容层：旧 `/api/files/business` 映射/双读；
- 单元测试：注册表校验、重复绑定、级联解绑、软删除过滤。

### P3 前端组件与常量（1–2 天）

- 新增 `src/api/business-files.ts`、`src/constants/biz-file-type.ts`；
- 改造 `file-upload.vue`、`file-preview-dialog.vue`；
- 提供旧 props 兼容层，保证未迁移页面不受影响。

### P4 分模块迁移（按风险排序）

**第一批：高风险冲突（优先）**

1. 剂型 / 制剂（`PREPARATION_DOCUMENT` 串数据）
2. 机构 / 机构证书（`ORGANIZATION_CERTIFICATE` 串数据）
3. 人员档案 / 人员证书（证书 ID 藏在 `custom_path`）
4. 审批模块（数字节点 ID 当类型）
5. 房间日志（附件挂在房间而非记录）
6. 养护记录（前后端类型不一致）
7. 能耗（前后端类型不一致）
8. 生产计划归档检查（`customPath` 漏查）

**第二批：单槽位模块**

- 货物验收、销毁记录、偏差、请检单、取样、审核放行、留样、设备、设备维保、采购订单、供应商信息、供应商审计、培训、验证方案、生产计划、生产单位、岗位、职务。

**第三批：多槽位/明细模块**

- 制剂管理（5 个槽位 + 工序模板）
- 生产任务（生产批记录 / 其他文件 / 工序附件）
- 养护记录（主单 + 明细）
- 房间信息（3 个槽位）
- 人员档案（档案 + 证书）

**清理**

- 删除未挂路由的旧页面 `preparation_info/index.vue`；
- 清理 `FileUpload` 的 `businessType` / `customPath` 旧逻辑；
- 清理后端散落的业务类型字符串，统一引用 `BizFileType`。

### P5 数据迁移（1–2 天）

- 编写 `migrate_business_file_data.sql`（幂等、可重复执行）；
- 先 dry-run 输出“旧类型 → 新 bizType / fileKind”对照和行数；
- 在 `8.134.48.54` 验证后再到生产环境执行；
- 迁移后保留 `file_info.business_*` 字段一个版本，便于回滚。

### P6 测试与验收（1–2 天）

- 每个模块验证：上传 → 列表 → 预览 → 下载 → 删除 → 业务删除级联；
- 重点回归：剂型/制剂同 ID、机构/证书同 ID、审批多节点、房间多条记录、生产批记录归档校验；
- 迁移脚本在测试库重复执行两次，确认幂等；
- 文件管理页、文件操作日志正常。

---

## 7. 数据迁移规则

### 7.1 旧类型 → 新 bizType 映射

| 旧 business_type | 新 biz_type | 说明 |
|------------------|-------------|------|
| `GOODS_ACCEPTANCE_WAYBILL` / `_INVOICE` / `_INSPECTION_REPORT` | `ACCEPTANCE_ORDER` | 按原类型映射 `file_kind` |
| `QUALITY_DESTRUCTION_RECORD` | `DESTRUCTION_RECORD` | |
| `QUALITY_DEVIATION_RECORD` | `DEVIATION_RECORD` | |
| `QUALITY_INSPECTION_RECORD` | `INSPECTION_REQUEST` | |
| `QUALITY_SAMPLING_RECORD` | `SAMPLING_RECORD` | |
| `AUDIT_RELEASE_FILE` / `AUDIT_RELEASE_RECORD` | `RELEASE_REVIEW` | |
| `SAMPLE_RETENTION_FILE` / `SAMPLE_RETENTION_RECORD` | `RETAINED_SAMPLE` | |
| `PREPARATION_DOCUMENT` | `DOSAGE_FORM` 或 `PREPARATION` | 需按 `business_id` 归属消歧（见 7.2） |
| `PREPARATION_PROCESS` | `PREPARATION_PROCESS_TEMPLATE` | `custom_path` 第一段是模板 ID |
| `PRODUCTION_PLAN` | `PRODUCTION_PLAN` | |
| `PRODUCTION_TASK` | `WORK_ORDER` | `custom_path` 前缀区分 `BATCH_RECORD` / `OTHER` |
| `PRODUCTION_PROCESS` | `WORK_ORDER_PROCESS_EXECUTION` | `custom_path` 第一段是工序执行行 ID |
| `GENERAL_ATTACHMENT` | `PRODUCTION_UNIT` | 目前仅生产单位页面使用 |
| `EQUIPMENT_FILE` | `EQUIPMENT` | |
| `EQUIPMENT_MAINTENANCE` | `EQUIPMENT_MAINTENANCE` | |
| `ENERGY_FILE` / `ENERGY_RECORD` | `ENERGY_RECORD` | 两种都兼容 |
| `MAINTENANCE_RECORD` / `STOCK_MAINTENANCE_RECORD` | `MAINTENANCE_RECORD` | 两种都兼容 |
| `MAINTENANCE_DETAIL` / `STOCK_MAINTENANCE_DETAIL` | `MAINTENANCE_RECORD_DETAIL` | 两种都兼容 |
| `ORGANIZATION_CERTIFICATE` / `ORGANIZATION_LICENSE` | `ORGANIZATION` 或 `ORGANIZATION_CERTIFICATE` | 需按 `business_id` 归属消歧 |
| `ORGANIZATION_PHOTO` | `ORGANIZATION` | `file_kind=PHOTO` |
| `PERSONNEL_ATTACHMENT` | `PERSONNEL_FILE` | |
| `PERSONNEL_CERTIFICATE` | `PERSONNEL_CERTIFICATE` | 从 `custom_path` 解析证书 ID |
| `POSITION_FILE` | `POSITION` | |
| `DUTY_FILE` | `DUTY` | |
| `PURCHASE_FILE` | `PURCHASE_ORDER` | |
| `SUPPLIER_FILE` | `PURCHASE_SUPPLIER` | |
| `SUPPLIER_ATTACHMENT` | `SUPPLIER_AUDIT` | |
| `ROOM_DISINFECTION` / `ROOM_CLEAN_INSPECTION` / `ROOM_CLEANING_RECORD` | `ROOM_INFO` | 房间页面的附件；房间日志的附件需另行处理 |
| `TRAINING_ATTACHMENT` | `TRAINING_RECORD` | |
| `VERIFICATION_FILE` | `VERIFICATION_RECORD` | |
| 纯数字（审批节点 ID） | `APPROVAL_NODE` | `biz_id = business_type` |
| 空 / `GENERAL` / 其他 | 不自动迁移 | 进入待人工处理清单 |

### 7.2 需要人工/规则消歧的迁移

1. **`PREPARATION_DOCUMENT`**
   - `business_id` 同时存在于 `dosage_form.dosage_id` 和 `preparation.preparation_id` 时，需结合 `custom_path` / `category` / `entity_name`（文件目录中的实体名）判断；
   - 建议先跑统计 SQL，输出冲突行，人工确认后再迁移。
2. **`ORGANIZATION_CERTIFICATE`**
   - `business_id` 可能是 `organization.id` 或 `organization_certificate.id`；
   - 优先按 ID 存在性判断；两边都存在时，按 `custom_path`（证书名称）与 `organization_certificate.certificate_name` 匹配。
3. **`PERSONNEL_CERTIFICATE`**
   - `business_id` 是 `personnel_file_id`；
   - 从 `custom_path` 形如 `{certificateType}_{certificateId}` 中解析证书 ID，迁到 `personnel_certificate`；
   - 解析失败的行保留到 `PERSONNEL_FILE` + `file_kind=CERTIFICATE_FILE` 作为兼容。
4. **`PRODUCTION_PROCESS`**
   - `custom_path` 形如 `{processExecutionId}/{workOrderId}`，取第一段作为 `biz_id`；
   - 第一段不是数字时进入人工清单。
5. **`ROOM_CLEANING_RECORD` / `ROOM_CLEAN_INSPECTION`**
   - 目前无法区分“房间页面附件”和“房间日志附件”，先统一迁到 `ROOM_INFO`，再通过业务数据人工补录到具体记录。
6. **`business_id = 0` 或父记录不存在的孤儿文件**
   - 不自动迁移，输出清单；确认后可删除或归入 `GENERAL`。

### 7.3 迁移脚本要求

- 放在 `src/main/resources/Data/migration/`，命名如 `create_business_file.sql`、`migrate_business_file_data.sql`；
- 幂等：`INSERT ... SELECT ... WHERE NOT EXISTS`；
- 先 `SELECT` 统计，再 `INSERT`；
- 迁移前后记录行数对照；
- 测试统一使用 `8.134.48.54`，**禁止在 `8.148.27.237` 写测试数据**。

---

## 8. 验收与风险

### 8.1 验收清单

- [ ] 34 张业务表的附件均可按 `(bizType, bizId, fileKind)` 正确查询；
- [ ] 剂型 ID 与制剂 ID 相同时，附件互不串数据；
- [ ] 机构 ID 与机构证书 ID 相同时，附件互不串数据；
- [ ] 审批实例各节点附件归属正确，实例详情一次查询完成；
- [ ] 房间日志每条记录只显示自己的附件；
- [ ] 生产计划“归档前必须有生产批记录”校验能查到文件；
- [ ] 新增记录不再产生 `biz_id=0` 孤儿文件；
- [ ] 删除业务记录后关系表无残留；
- [ ] 文件管理页、回收站、操作日志正常；
- [ ] 旧数据迁移前后数量一致，冲突行有清单。

### 8.2 风险与对策

| 风险 | 对策 |
|------|------|
| 旧数据归属歧义（剂型/制剂、机构/证书） | 先统计、人工确认；无法确认的保留旧关联并列出清单 |
| 前端/后端类型常量再次漂移 | 由后端注册表提供 `/api/business-files/types`，前端生成/校验常量 |
| 迁移期间新旧数据并存 | 保留旧接口 + 双读一个版本，按模块灰度切换 |
| 业务删除未解绑 | 在 Service 删除方法统一调用 `unbindAll`，并加定期一致性检查 |
| 文件删除影响其他业务 | `file_id` 可被多条关系引用，删除前检查引用数 |
| 性能 | `idx_biz(biz_type, biz_id, file_kind)`；审批实例附件一次查询，避免 N+1 |

---

## 9. 参考文件

- 前端统一上传组件：`art-design-pro-main/src/components/upload/file-upload.vue`
- 前端文件 API：`art-design-pro-main/src/api/files.ts`
- 前端上传点清单（自动提取）：`New-erp/_analysis/tools/uploads.txt`
- 前端源码快照：`New-erp/_analysis/frontend-src/`
- 后端文件控制器：`src/main/java/com/tonghui/erp/Controller/FileController.java`
- 后端文件实体：`src/main/java/com/tonghui/erp/Data/Entity/FileInfo.java`
- 文件业务类型目录：`src/main/resources/init-data/file-types.yml`
- 目录解析逻辑：`src/main/java/com/tonghui/erp/Common/Config/InitDataConfig/FileTypeInitConfig.java`

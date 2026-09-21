# 文件业务类型枚举（businessType）

> 本文档供前端开发传参参考，数据源为 `src/main/resources/init-data/file-types.yml`

## 通用上传接口

```
POST /api/files/upload-business
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | 是 | 上传的文件 |
| `businessType` | String | 是 | 业务类型枚举值（见下方枚举表） |
| `businessId` | Long | 是 | 关联业务记录ID |
| `entityName` | String | 否 | 实体名称，用于创建子目录（如证书名称、设备名称） |
| `description` | String | 否 | 文件描述 |
| `customPath` | String | 否 | 自定义存储子路径 |

**上传后目录结构**：
```
{basePath}/{中文顶级目录}/{中文子目录}/{年}/{月}/{entityName}/{uuid}.{ext}
```

示例：`businessType=ORGANIZATION_CERTIFICATE`，`entityName=GMP证书`，上传后路径：
```
uploaded-files/机构管理/证书/2026/09/GMP证书/a1b2c3d4.jpg
```

---

## 父类型（顶级目录）

| 父类型 Key | 中文目录名 |
|-----------|-----------|
| EQUIPMENT | 设备管理 |
| PRODUCTION | 生产管理 |
| PREPARATION | 制剂管理 |
| MATERIAL | 物料管理 |
| STOCK | 库存管理 |
| PURCHASE | 采购管理 |
| QUALITY | 质量管理 |
| PERSONNEL | 人员管理 |
| ROOM | 车间环境 |
| ENVIRONMENT | 环境管理 |
| APPROVAL | 审批管理 |
| SUPPLIER | 供应商管理 |
| CUSTOMER | 客户管理 |
| ORGANIZATION | 机构管理 |
| TRAINING | 培训管理 |
| DOSAGE_FORM | 剂型信息 |
| ENERGY | 能耗管理 |
| GENERAL | 通用文件 |
| GOODS_ACCEPTANCE | 货物验收 |
| AUDIT_RELEASE | 审核放行 |
| SAMPLE_RETENTION | 留样管理 |
| VERIFICATION | 验证方案 |

## 子类型（子目录）

| 子类型 Key | 子目录名 |
|-----------|---------|
| MAINTENANCE | 维保 |
| PHOTO | 照片 |
| DOCUMENT | 文档 |
| PLAN | 计划 |
| RECORD | 记录 |
| PROCESS | 工序 |
| REPORT | 报告 |
| FORMULA | 配方 |
| SPEC | 规格 |
| FILE | 文件 |
| CERTIFICATE | 证书 |
| IN_PURCHASE | 入库单/原料 |
| IN_AUXILIARY | 入库单/辅料 |
| IN_PACKAGING | 入库单/包材 |
| IN_PRODUCT | 入库单/成品 |
| OUT_SALES | 出库单/销售 |
| OUT_PRODUCTION | 出库单/领料 |
| OUT_RETURN | 出库单/退货 |
| ORDER | 订单 |
| CONTRACT | 合同 |
| INVOICE | 发票 |
| INSPECTION | 质检 |
| ATTACHMENT | 附件 |
| CLEAN_INSPECTION | 洁净检测 |
| CLEANING_RECORD | 清洁记录 |
| TEMPERATURE_HUMIDITY | 温湿度记录 |
| PRESSURE_DIFFERENCE | 压差记录 |
| DISINFECTION | 消毒记录 |
| LICENSE | 许可 |
| AUDIT | 审核 |
| ENERGY_RECORD | 能耗记录 |
| REQUEST | 请检单 |
| SAMPLE | 样品 |
| SAMPLING | 取样 |
| INSPECTION_RECORD | 请检记录 |
| WAYBILL | 随货清单 |
| INSPECTION_REPORT | 检验报告 |
| RELEASE_RECORD | 放行记录 |
| RETENTION_RECORD | 留样记录 |
| HEALTH_FILE | 健康档案 |
| TASK | 任务 |

---

## 完整枚举表

### 设备管理（EQUIPMENT）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `EQUIPMENT_MAINTENANCE` | 设备管理/维保 | 设备维保记录 |
| `EQUIPMENT_PHOTO` | 设备管理/照片 | 设备照片 |
| `EQUIPMENT_DOCUMENT` | 设备管理/文档 | 设备文档 |
| `EQUIPMENT_REPORT` | 设备管理/报告 | 设备报告 |
| `EQUIPMENT_CERTIFICATE` | 设备管理/证书 | 设备证书 |

### 生产管理（PRODUCTION）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `PRODUCTION_PLAN` | 生产管理/计划 | 生产计划 |
| `PRODUCTION_RECORD` | 生产管理/记录 | 生产记录 |
| `PRODUCTION_PROCESS` | 生产管理/工序 | 工序记录 |
| `PRODUCTION_REPORT` | 生产管理/报告 | 生产报告 |
| `PRODUCTION_PHOTO` | 生产管理/照片 | 生产照片 |
| `PRODUCTION_TASK` | 生产管理/任务 | 生产任务 |

### 制剂管理（PREPARATION）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `PREPARATION_DOCUMENT` | 制剂管理/文档 | 制剂文档 |
| `PREPARATION_FORMULA` | 制剂管理/配方 | 制剂配方 |
| `PREPARATION_SPEC` | 制剂管理/规格 | 制剂规格 |
| `PREPARATION_RECORD` | 制剂管理/记录 | 制剂记录 |

### 物料管理（MATERIAL）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `MATERIAL_FILE` | 物料管理/文件 | 物料文件 |
| `MATERIAL_CERTIFICATE` | 物料管理/证书 | 物料证书 |
| `MATERIAL_PHOTO` | 物料管理/照片 | 物料照片 |

### 库存管理（STOCK）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `STOCK_IN_PURCHASE` | 库存管理/入库单/原料 | 原料采购入库 |
| `STOCK_IN_AUXILIARY` | 库存管理/入库单/辅料 | 辅料入库 |
| `STOCK_IN_PACKAGING` | 库存管理/入库单/包材 | 包材入库 |
| `STOCK_IN_PRODUCT` | 库存管理/入库单/成品 | 成品入库 |
| `STOCK_OUT_SALES` | 库存管理/出库单/销售 | 销售出库 |
| `STOCK_OUT_PRODUCTION` | 库存管理/出库单/领料 | 生产领料出库 |
| `STOCK_OUT_RETURN` | 库存管理/出库单/退货 | 退货出库 |

### 采购管理（PURCHASE）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `PURCHASE_ORDER` | 采购管理/订单 | 采购订单 |
| `PURCHASE_CONTRACT` | 采购管理/合同 | 采购合同 |
| `PURCHASE_INVOICE` | 采购管理/发票 | 采购发票 |
| `PURCHASE_PHOTO` | 采购管理/照片 | 采购照片 |

### 质量管理（QUALITY）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `QUALITY_RECORD` | 质量管理/记录 | 质量记录 |
| `QUALITY_INSPECTION` | 质量管理/质检 | 质检记录 |
| `QUALITY_SAMPLING` | 质量管理/取样 | 取样记录 |
| `QUALITY_INSPECTION_RECORD` | 质量管理/请检记录 | 请检记录 |
| `QUALITY_CERTIFICATE` | 质量管理/证书 | 质量证书 |
| `QUALITY_REPORT` | 质量管理/报告 | 质量报告 |
| `QUALITY_REQUEST` | 质量管理/请检单 | 请检单 |
| `QUALITY_SAMPLE` | 质量管理/样品 | 样品 |
| `QUALITY_WAYBILL` | 质量管理/随货清单 | 随货清单 |
| `QUALITY_INSPECTION_REPORT` | 质量管理/检验报告 | 检验报告 |

### 人员管理（PERSONNEL）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `PERSONNEL_CERTIFICATE` | 人员管理/证书 | 人员资质证书 |
| `PERSONNEL_ATTACHMENT` | 人员管理/附件 | 人员附件 |
| `PERSONNEL_PHOTO` | 人员管理/照片 | 人员照片 |
| `PERSONNEL_HEALTH_FILE` | 人员管理/健康档案 | 人员健康档案 |

### 车间环境（ROOM）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `ROOM_CLEAN_INSPECTION` | 车间环境/洁净检测 | 洁净检测记录 |
| `ROOM_CLEANING_RECORD` | 车间环境/清洁记录 | 清洁记录 |
| `ROOM_TEMPERATURE_HUMIDITY` | 车间环境/温湿度记录 | 温湿度记录 |
| `ROOM_PRESSURE_DIFFERENCE` | 车间环境/压差记录 | 压差记录 |
| `ROOM_DISINFECTION` | 车间环境/消毒记录 | 消毒记录 |
| `ROOM_PHOTO` | 车间环境/照片 | 车间照片 |

### 环境管理（ENVIRONMENT）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `ENVIRONMENT_LICENSE` | 环境管理/许可 | 环境许可 |
| `ENVIRONMENT_REPORT` | 环境管理/报告 | 环境报告 |

### 审批管理（APPROVAL）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `APPROVAL_DOCUMENT` | 审批管理/文档 | 审批文档 |

### 供应商管理（SUPPLIER）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `SUPPLIER_CERTIFICATE` | 供应商管理/证书 | 供应商证书 |
| `SUPPLIER_PHOTO` | 供应商管理/照片 | 供应商照片 |

### 客户管理（CUSTOMER）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `CUSTOMER_CERTIFICATE` | 客户管理/证书 | 客户证书 |
| `CUSTOMER_PHOTO` | 客户管理/照片 | 客户照片 |

### 机构管理（ORGANIZATION）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `ORGANIZATION_CERTIFICATE` | 机构管理/证书 | 机构证书 |
| `ORGANIZATION_LICENSE` | 机构管理/许可 | 机构许可/执照 |
| `ORGANIZATION_PHOTO` | 机构管理/照片 | 机构照片 |

### 培训管理（TRAINING）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `TRAINING_DOCUMENT` | 培训管理/文档 | 培训文档 |
| `TRAINING_RECORD` | 培训管理/记录 | 培训记录 |
| `TRAINING_PHOTO` | 培训管理/照片 | 培训照片 |

### 能耗管理（ENERGY）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `ENERGY_RECORD` | 能耗管理/能耗记录 | 能耗记录 |
| `ENERGY_REPORT` | 能耗管理/报告 | 能耗报告 |

### 货物验收（GOODS_ACCEPTANCE）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `GOODS_ACCEPTANCE_RECORD` | 货物验收/记录 | 验收记录 |
| `GOODS_ACCEPTANCE_PHOTO` | 货物验收/照片 | 验收照片 |

### 审核放行（AUDIT_RELEASE）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `AUDIT_RELEASE_RECORD` | 审核放行/放行记录 | 放行记录 |
| `AUDIT_RELEASE_DOCUMENT` | 审核放行/文档 | 审核文档 |

### 留样管理（SAMPLE_RETENTION）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `SAMPLE_RETENTION_RECORD` | 留样管理/留样记录 | 留样记录 |

### 验证方案（VERIFICATION）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `VERIFICATION_DOCUMENT` | 验证方案/文档 | 验证文档 |
| `VERIFICATION_RECORD` | 验证方案/记录 | 验证记录 |

### 通用（GENERAL）

| businessType | 中文目录 | 说明 |
|-------------|---------|------|
| `GENERAL_FILE` | 通用文件/文件 | 通用文件 |
| `GENERAL_DOCUMENT` | 通用文件/文档 | 通用文档 |
| `GENERAL_PHOTO` | 通用文件/照片 | 通用照片 |

---

## 维护规则

新增模块涉及文件上传时，必须同步维护以下两处：

1. **`src/main/resources/init-data/file-types.yml`**（数据源）
   - 如需新的父类型，在 `parent-types` 下添加
   - 如需新的子类型，在 `sub-types` 下添加
   - 命名规范：全大写 + 下划线分隔（如 `HEALTH_FILE`）

2. **`FILE_TYPES.md`**（本文档）
   - 在「父类型」或「子类型」表格中添加新条目
   - 在对应模块分组的枚举表中添加新行
   - 如为全新模块，新增模块分组章节

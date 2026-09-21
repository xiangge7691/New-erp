# ERP系统数据库表结构与接口文档

> 本文档用于数据导入参考，包含所有数据库表结构和API接口信息。

---

## 一、数据库表结构总览

**共76个数据库表**，按业务模块分类：

| 模块 | 表数量 | 主要表名 |
|------|--------|----------|
| 系统管理 | 7 | user, role, permission, role_perm, user_role, department, user_department |
| 基础数据 | 8 | material, preparation, preparation_formula, preparation_document, preparation_process_template, unit, dosage_form, position |
| 采购管理 | 6 | purchase_suppliers, purchase_plan, purchase_plan_detail, purchase_orders, purchase_order_items, supplier_audit |
| 生产管理 | 8 | production_plan, work_order, work_order_process_execution, production_unit, prod_unit_invoice, production_process_record, plan_status_log, process_type |
| 库存管理 | 8 | stock, stock_in, stock_in_detail, stock_in_sequence, stock_out, stock_out_detail, stock_out_sequence, stock_transaction |
| 验收管理 | 2 | acceptance_order, acceptance_detail |
| 退库管理 | 2 | return_order, return_order_detail |
| 盘点管理 | 2 | check_order, check_order_detail |
| 调拨管理 | 2 | transfer_order, transfer_order_detail |
| 领料管理 | 4 | material_requisition, material_requisition_detail, material_requisition_slip, material_requisition_slip_detail |
| 审批流程 | 4 | approval_workflow, approval_node, approval_instance, approval_record |
| 设备管理 | 3 | equipment, equipment_maintenance, room_info |
| 质量检验 | 6 | inspection_plan, inspection_request, inspection_record, sampling_record, retained_sample, release_review |
| 机构人员 | 4 | organization, organization_certificate, personnel_file, personnel_certificate |
| 环境监控 | 5 | temperature_humidity_record, pressure_difference_record, disinfection_record, cleaning_record, clean_inspection_record |
| 能耗文件 | 3 | energy_record, file_info, file_operation_log |
| 培训验证 | 2 | training_record, verification_record |

---

## 二、基类字段说明

所有继承 `AuditEntity` 的表自动包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| created_by | Long | 创建人ID |
| updated_by | Long | 更新人ID |
| created_time | LocalDateTime | 创建时间 |
| updated_time | LocalDateTime | 更新时间 |

**全局软删除字段**（所有表都有）：

| 字段 | 类型 | 说明 |
|------|------|------|
| is_deleted | Integer | 0=正常, 1=已删除 |
| version | Integer | 乐观锁版本号 |

---

## 三、各模块表结构详情

### 1. 系统管理模块

#### 1.1 user（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | Long | 主键，自增 |
| user_account | String | 用户名（唯一） |
| user_name | String | 真实姓名 |
| password | String | 加密密码（Argon2） |
| phone | String | 联系电话 |
| gender | Object | 性别 |
| user_status | Integer | 状态：0禁用/1启用 |
| user_notes | String | 备注 |

#### 1.2 role（角色表）
| 字段 | 类型 | 说明 |
|------|------|------|
| role_id | Long | 主键，自增 |
| role_name | String | 角色名称（唯一） |
| role_desc | String | 角色描述 |
| role_status | Integer | 状态：1启用/0禁用 |

#### 1.3 permission（权限表）
| 字段 | 类型 | 说明 |
|------|------|------|
| perm_id | Long | 主键，自增 |
| perm_key | String | 权限键（唯一） |
| perm_name | String | 权限名称 |
| perm_type | Object | 权限类型 |
| parent_id | Long | 父权限ID |
| display_order | Integer | 显示顺序 |
| perm_status | Integer | 状态：0禁用/1启用 |

#### 1.4 department（部门表）
| 字段 | 类型 | 说明 |
|------|------|------|
| department_id | Long | 主键，自增 |
| department_name | String | 部门名称 |
| parent_id | Long | 父部门ID（0为顶级） |
| status | Integer | 状态：0禁用/1启用 |
| sort_order | Integer | 排序号 |

#### 1.5 position（岗位表）
| 字段 | 类型 | 说明 |
|------|------|------|
| position_id | Long | 主键，自增 |
| position_code | String | 岗位编码（唯一） |
| position_name | String | 岗位名称 |
| department_id | Long | 所属部门ID |
| position_desc | String | 岗位描述 |
| position_level | Integer | 岗位等级 |
| status | Integer | 状态：0停用/1启用 |

#### 1.6 关联表
- **role_perm**：role_id, perm_id（角色权限关联）
- **user_role**：user_id, role_id（用户角色关联）
- **user_department**：user_id, department_id, is_primary（用户部门关联）

---

### 2. 基础数据模块

#### 2.1 material（物料表）
| 字段 | 类型 | 说明 |
|------|------|------|
| material_id | Long | 主键，自增 |
| material_code | String | 物料编码（唯一） |
| material_name | String | 物料名称 |
| category_name | String | 分类（原料/辅料/包材） |
| material_attribute | String | 物料属性 |
| unit_name | String | 计量单位 |
| spec | String | 规格描述 |
| storage_requirement | String | 存储要求 |
| material_status | Integer | 状态：1启用/0禁用 |

#### 2.2 preparation（制剂表）
| 字段 | 类型 | 说明 |
|------|------|------|
| preparation_id | Long | 主键，自增 |
| preparation_code | String | 制剂编码（唯一） |
| preparation_name | String | 制剂品名 |
| spec | String | 规格描述 |
| process_attr | String | 加工性质 |
| package_spec | String | 包装规格 |
| executive_standard | String | 执行标准 |
| function_main | String | 功能主治 |
| unit_name | String | 单位名称 |
| dosage_category | String | 剂型大类 |
| dosage_name | String | 剂型名称 |
| producer | String | 生产单位 |
| batch_qty | BigDecimal | 批量 |
| invoice_price | BigDecimal | 开票单价 |
| insurance_price | BigDecimal | 医保单价 |
| settlement_price | BigDecimal | 结算单价 |
| retail_price | BigDecimal | 零售单价 |
| sales_price | BigDecimal | 销售单价 |
| status | Integer | 状态：1启用/0禁用 |

#### 2.3 preparation_formula（制剂处方表）
| 字段 | 类型 | 说明 |
|------|------|------|
| formula_id | Long | 主键，自增 |
| preparation_id | Long | 制剂ID |
| preparation_code | String | 制剂编码 |
| preparation_name | String | 制剂品名 |
| material_id | Long | 原料ID |
| material_code | String | 原料编号 |
| material_name | String | 原料名称 |
| material_category | String | 原料分类 |
| dosage | BigDecimal | 处方量 |
| unit_id | Long | 单位ID |
| unit_name | String | 单位名称 |

#### 2.4 preparation_process_template（制剂工序模版表）
| 字段 | 类型 | 说明 |
|------|------|------|
| template_id | Long | 主键，自增 |
| preparation_id | Long | 制剂ID |
| process_type_id | Long | 工序类型ID |
| step_order | Integer | 工序顺序 |
| standard_qty | BigDecimal | 标准加工数量 |
| unit_id | Long | 计量单位ID |
| standard_duration | Integer | 标准工时（分钟） |
| equipment_desc | String | 设备要求描述 |
| key_process_params | String | 关键工艺参数 |

#### 2.5 unit（计量单位表）
| 字段 | 类型 | 说明 |
|------|------|------|
| unit_id | Long | 主键，自增 |
| unit_name | String | 单位中文名称 |
| symbol | String | 单位符号 |
| status | Integer | 状态：0禁用/1启用 |

#### 2.6 dosage_form（剂型分类表）
| 字段 | 类型 | 说明 |
|------|------|------|
| dosage_id | Long | 主键，自增 |
| dosage_category | String | 剂型大类 |
| dosage_name | String | 剂型名称 |
| status | Integer | 状态：0禁用/1启用 |

#### 2.7 process_type（工序类型表）
| 字段 | 类型 | 说明 |
|------|------|------|
| process_id | Integer | 主键，自增 |
| process_code | String | 工序类型编码 |
| process_name | String | 工序类型名称 |
| process_description | String | 工序类型说明 |
| process_status | Integer | 状态：1启用/0未启用 |

---

### 3. 采购管理模块

#### 3.1 purchase_suppliers（供应商表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| supplier_number | String | 供应商编号 |
| supplier_name | String | 供应商名称 |
| category | String | 供应类型 |
| supplier_type | String | 供应商类别 |
| quality_standard | String | 执行质量标准 |
| contact_person | String | 联系人 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| address | String | 地址 |
| bank_account | String | 银行账户 |
| bank_name | String | 开户行 |
| material_info | String | 材料信息 |
| status | Object | 状态 |

#### 3.2 purchase_plan（采购计划表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| plan_code | String | 采购计划编号 |
| production_plan_id | Long | 关联生产计划ID |
| production_plan_code | String | 生产计划编号 |
| title | String | 工单标题 |
| preparation_code | String | 制剂编码 |
| preparation_name | String | 制剂名称 |
| spec | String | 规格 |
| batch_qty | BigDecimal | 批量 |
| prescription_multiple | BigDecimal | 处方倍数 |
| material_type | String | 物料类型 |
| warehouse | String | 仓库 |
| processing_date | LocalDate | 处理日期 |
| desired_delivery_date | LocalDate | 期望到货日期 |
| receiving_unit | String | 收货单位 |
| receiving_address | String | 收货地址 |
| status | String | 状态 |
| approval_opinion | String | 审批意见 |

#### 3.3 purchase_plan_detail（采购计划明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| plan_id | Long | 采购计划ID |
| sequence_number | Integer | 序号 |
| material_id | Long | 物料ID |
| material_code | String | 原料编码 |
| material_name | String | 原料名称 |
| material_category | String | 原料分类 |
| unit | String | 单位 |
| standard_qty | BigDecimal | 标准处方量 |
| purchase_qty | BigDecimal | 采购数量 |
| stock_qty | BigDecimal | 库存数量 |

#### 3.4 purchase_orders（采购订单表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| purchase_number | String | 采购编号 |
| supplier_id | Long | 供应商ID |
| prod_unit_id | Long | 仓库（生产单位ID） |
| warehouse | String | 仓库 |
| processing_date | LocalDate | 处理日期 |
| desired_delivery_date | LocalDate | 期望到货日期 |
| title | String | 采购单标题 |
| plan_id | Long | 关联采购计划ID |
| plan_code | String | 采购计划编号 |
| preparation_code | String | 制剂编码 |
| preparation_name | String | 制剂名称 |
| batch_qty | BigDecimal | 批量 |
| status | Object | 订单状态 |
| approval_opinion | String | 审批意见 |

#### 3.5 purchase_order_items（采购订单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| order_id | Long | 关联订单ID |
| sequence_number | Integer | 序号 |
| material_id | Long | 物料ID |
| material_code | String | 物料编码 |
| product_name | String | 制剂名称 |
| raw_material_name | String | 原药材品名 |
| dose | BigDecimal | 原药材剂量 |
| unit | String | 单位 |
| standard_dosage | BigDecimal | 标准处方量 |
| purchase_quantity | BigDecimal | 采购数量 |
| unit_price | BigDecimal | 单价 |
| amount | BigDecimal | 金额 |

---

### 4. 生产管理模块

#### 4.1 production_plan（生产计划表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键，自增 |
| plan_number | String | 计划编号 |
| plan_name | String | 计划名称 |
| related_order | String | 关联销售订单号 |
| preparation_id | Long | 制剂ID |
| preparation_code | String | 制剂编码 |
| preparation_name | String | 制剂名称 |
| plan_quantity | BigDecimal | 计划数量 |
| plan_type | String | 生产计划类型 |
| unit_name | String | 制剂所属单位 |
| production_unit | String | 生产单位 |
| unit_price | BigDecimal | 单价 |
| finished_quantity | BigDecimal | 成品数量 |
| production_cycle | Integer | 周期（天） |
| yield_rate | BigDecimal | 得率 |
| total_amount | BigDecimal | 总金额 |
| production_start_time | LocalDateTime | 生产开始时间 |
| production_end_time | LocalDateTime | 生产结束时间 |
| is_urgent | Integer | 是否加急 |
| current_status | String | 当前状态 |

#### 4.2 work_order（工单表）
| 字段 | 类型 | 说明 |
|------|------|------|
| work_order_id | Long | 主键，自增 |
| work_order_code | String | 工单编号（唯一） |
| work_order_name | String | 工单名称 |
| preparation_id | Long | 制剂ID |
| preparation_code | String | 制剂编码 |
| preparation_name | String | 制剂名称 |
| plan_id | Long | 关联计划ID |
| batch_qty | BigDecimal | 批量 |
| producer | String | 生产单位 |
| receiver | String | 收货单位 |
| delivery_time | LocalDateTime | 交付时间 |
| batch_number | String | 批号 |
| outbound_qty | BigDecimal | 出库量 |
| current_status | String | 当前状态 |

#### 4.3 production_unit（生产单位表）
| 字段 | 类型 | 说明 |
|------|------|------|
| prod_unit_id | Long | 主键，自增 |
| prod_unit_code | String | 生产单位编号（唯一） |
| prod_unit_name | String | 生产单位名称 |
| prod_unit_address | String | 生产单位地址 |
| prod_unit_manager | String | 负责人姓名 |
| prod_unit_phone | String | 联系电话 |
| location | String | 位置 |
| usage_area | BigDecimal | 使用面积 |
| prod_unit_status | Integer | 状态：0停用/1启用 |

#### 4.4 work_order_process_execution（工单工序执行记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| work_order_id | Long | 关联工单ID |
| process_type_id | Long | 工序类型ID |
| room_id | Integer | 配置室ID |
| equipment_id | Integer | 使用设备ID |
| step_order | Integer | 工序顺序 |
| operator_id | Long | 操作人ID |
| operator_name | String | 操作人姓名 |
| start_time | LocalDateTime | 开始时间 |
| end_time | LocalDateTime | 结束时间 |
| process_qty | BigDecimal | 加工数量 |
| status | String | 状态 |

---

### 5. 库存管理模块

#### 5.1 stock（库存表）
| 字段 | 类型 | 说明 |
|------|------|------|
| stock_id | Long | 主键，自增 |
| prod_unit_id | Long | 关联生产单位ID |
| item_type | Object | 物品类型 |
| item_id | Long | 关联物品ID |
| item_code | String | 物品编码 |
| item_name | String | 物品名称 |
| category_name | String | 分类名称 |
| unit_name | String | 计量单位 |
| quantity | BigDecimal | 库存数量 |
| unit_price | BigDecimal | 单价 |
| min_quantity | BigDecimal | 最低库存预警数量 |
| max_quantity | BigDecimal | 最高库存限制数量 |
| batch_number | String | 批次号 |
| production_date | LocalDate | 生产日期 |
| expiry_date | LocalDate | 有效期至 |
| storage_location | String | 库位/货架号 |
| stock_status | Object | 库存状态 |

#### 5.2 stock_in（入库主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| in_id | Long | 主键，自增 |
| in_code | String | 入库单号（唯一） |
| in_type | String | 入库类型 |
| prod_unit_id | Long | 入库仓库 |
| supplier_id | Long | 供应商ID |
| related_order | String | 关联单号 |
| plan_number | String | 关联生产计划编号 |
| work_order_id | Long | 关联生产任务ID |
| in_date | LocalDateTime | 入库日期 |
| total_amount | BigDecimal | 入库总金额 |
| in_status | String | 状态 |

#### 5.3 stock_in_detail（入库明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| in_detail_id | Long | 主键，自增 |
| in_id | Long | 关联入库单ID |
| item_type | Object | 物品类型 |
| item_id | Long | 物品ID |
| item_code | String | 物品编码 |
| item_name | String | 物品名称 |
| category_name | String | 分类 |
| unit_name | String | 单位 |
| batch_number | String | 批次号 |
| production_date | LocalDate | 生产日期 |
| expiry_date | LocalDate | 有效期至 |
| quantity | BigDecimal | 入库数量 |
| unit_price | BigDecimal | 单价 |
| amount | BigDecimal | 金额 |
| storage_location | String | 存放位置 |

#### 5.4 stock_out（出库主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| out_id | Long | 主键，自增 |
| out_code | String | 出库单号（唯一） |
| out_type | String | 出库类型 |
| prod_unit_id | Long | 出库仓库 |
| customer_id | Long | 客户ID |
| related_order | String | 关联单号 |
| plan_id | Long | 关联生产计划ID |
| plan_number | String | 关联生产计划编号 |
| out_date | LocalDateTime | 出库日期 |
| total_amount | BigDecimal | 出库总金额 |
| out_status | String | 状态 |

#### 5.5 stock_out_detail（出库明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| out_detail_id | Long | 主键，自增 |
| out_id | Long | 关联出库单ID |
| prod_unit_id | Long | 出库仓库 |
| stock_id | Long | 关联库存记录ID |
| item_type | Object | 物品类型 |
| item_id | Long | 物品ID |
| item_code | String | 物品编码 |
| item_name | String | 物品名称 |
| batch_number | String | 批次号 |
| quantity | BigDecimal | 出库数量 |
| unit_price | BigDecimal | 单价 |
| amount | BigDecimal | 金额 |

#### 5.6 stock_transaction（库存交易记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| transaction_id | Long | 主键，自增 |
| stock_id | Long | 关联库存ID |
| transaction_type | Object | 交易类型 |
| transaction_date | LocalDateTime | 交易时间 |
| related_id | Long | 关联单据ID |
| related_type | Object | 关联单据类型 |
| quantity_before | BigDecimal | 交易前数量 |
| quantity_change | BigDecimal | 变动数量 |
| quantity_after | BigDecimal | 交易后数量 |
| batch_number | String | 批次号 |

---

### 6. 验收与退库模块

#### 6.1 acceptance_order（货物验收单主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| acceptance_id | Long | 主键，自增 |
| acceptance_code | String | 验收单号（唯一） |
| source_type | String | 来源类型 |
| related_order | String | 关联采购订单号 |
| purchase_number | String | 关联采购计划编号 |
| plan_code | String | 关联生产计划编号 |
| work_order_code | String | 关联生产任务编号 |
| title | String | 生产计划标题 |
| unit_name | String | 收货单位名称 |
| preparation_code | String | 关联制剂编码 |
| preparation_name | String | 关联制剂名称 |
| spec | String | 制剂规格 |
| batch_qty | BigDecimal | 计划生产批量 |
| prod_unit_id | Long | 入库仓库 |
| status | String | 状态 |

#### 6.2 acceptance_detail（货物验收单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| detail_id | Long | 主键，自增 |
| acceptance_id | Long | 关联验收单ID |
| seq | Integer | 明细序号 |
| item_type | String | 物品类型 |
| item_id | Long | 物品ID |
| material_code | String | 物料编码 |
| material_name | String | 物料名称 |
| material_category | String | 物料分类 |
| unit_name | String | 计量单位 |
| standard_dosage | BigDecimal | 标准处方量 |
| quantity | BigDecimal | 采购数量 |
| actual_arrival_qty | BigDecimal | 实际到货数量 |
| inbound_qty | BigDecimal | 入库数量 |
| unit_price | BigDecimal | 物料单价 |
| amount | BigDecimal | 金额 |
| batch_number | String | 物料批号 |

#### 6.3 return_order（退库单主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| return_no | String | 退库单号（唯一） |
| out_order_no | String | 关联出库单号 |
| production_plan_no | String | 生产计划编号 |
| material_count | Integer | 物料种数 |
| total_quantity | BigDecimal | 退库总量 |
| total_amount | BigDecimal | 退库总价 |
| operator_id | Long | 操作人ID |
| operator_name | String | 操作人姓名 |

#### 6.4 return_order_detail（退库单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| return_order_id | Long | 关联退库单ID |
| out_detail_id | Long | 出库明细ID |
| material_code | String | 物料编码 |
| material_name | String | 物料名称 |
| batch_no | String | 批号 |
| out_quantity | BigDecimal | 出库数量 |
| returned_quantity | BigDecimal | 已退数量 |
| return_quantity | BigDecimal | 本次退库数量 |
| unit_price | BigDecimal | 单价 |
| amount | BigDecimal | 金额 |

---

### 7. 盘点与调拨模块

#### 7.1 check_order（盘点单主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| check_no | String | 盘点单号（唯一） |
| warehouse | String | 盘点仓库名称 |
| material_count | Integer | 物料种数 |
| profit_count | Integer | 盘盈条目数 |
| loss_count | Integer | 盘亏条目数 |
| match_count | Integer | 盘平条目数 |
| operator_id | Long | 操作人ID |
| operator_name | String | 操作人姓名 |

#### 7.2 check_order_detail（盘点单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| check_order_id | Long | 关联盘点单ID |
| stock_id | Long | 库存ID |
| material_code | String | 物料编码 |
| material_name | String | 物料名称 |
| batch_no | String | 批号 |
| system_stock | BigDecimal | 系统库存 |
| actual_stock | BigDecimal | 实盘数量 |
| difference | BigDecimal | 差异 |
| result | String | 盘点结果 |

#### 7.3 transfer_order（调拨单主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| transfer_no | String | 调拨单号（唯一） |
| from_warehouse | String | 调出仓库名称 |
| to_warehouse | String | 调入仓库名称 |
| material_count | Integer | 物料种数 |
| total_quantity | BigDecimal | 调拨总量 |
| total_amount | BigDecimal | 调拨总价 |
| operator_id | Long | 操作人ID |
| operator_name | String | 操作人姓名 |

#### 7.4 transfer_order_detail（调拨单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| transfer_order_id | Long | 关联调拨单ID |
| material_code | String | 物料编码 |
| material_name | String | 物料名称 |
| batch_no | String | 批号 |
| transfer_quantity | BigDecimal | 调拨数量 |
| unit_price | BigDecimal | 单价 |
| amount | BigDecimal | 金额 |

---

### 8. 领料管理模块

#### 8.1 material_requisition（领料申请表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| requisition_code | String | 领料编号 |
| work_order_id | Long | 关联工单ID |
| requisition_date | LocalDate | 领料日期 |
| warehouse | String | 仓库 |
| material_type | String | 出库种类 |
| multiplier | BigDecimal | 处方倍数 |
| status | String | 状态 |

#### 8.2 material_requisition_detail（领料明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| requisition_id | Long | 领料申请ID |
| material_id | Long | 物料ID |
| material_code | String | 物料编码 |
| material_name | String | 物料名称 |
| unit_name | String | 单位 |
| prescription_qty | BigDecimal | 处方用量 |
| apply_qty | BigDecimal | 申请数量 |
| stock_qty | BigDecimal | 库存数量 |

#### 8.3 material_requisition_slip（物料领料单主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| slip_code | String | 领料单号（唯一） |
| production_plan_id | Long | 关联生产计划ID |
| production_plan_code | String | 关联生产计划编号 |
| from_dept | String | 领用科室 |
| to_dept | String | 发放科室 |
| applicant | String | 领料人 |
| apply_time | LocalDateTime | 领料时间 |
| material_type | String | 物料类型 |
| multiplier | BigDecimal | 处方倍数 |
| status | String | 状态 |

---

### 9. 审批流程模块

#### 9.1 approval_workflow（审批流程定义表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| workflow_name | String | 流程名称 |
| workflow_type | String | 流程类型 |
| status | Integer | 状态：0停用/1启用 |

#### 9.2 approval_node（审批节点定义表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| workflow_id | Long | 流程ID |
| node_name | String | 节点名称 |
| node_order | Integer | 节点顺序 |
| role_id | Long | 审批角色ID |
| after_pass_status | String | 通过后业务状态 |
| after_reject_status | String | 驳回后业务状态 |
| reject_to_node_id | Long | 驳回到哪个节点 |

#### 9.3 approval_instance（审批实例表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| workflow_id | Long | 流程ID |
| related_id | Long | 关联业务ID |
| related_type | String | 关联业务类型 |
| current_node_id | Long | 当前节点ID |
| initiator_id | Long | 发起人ID |
| status | String | 审批状态 |

#### 9.4 approval_record（审批记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| instance_id | Long | 审批实例ID |
| node_id | Long | 节点ID |
| approver_id | Long | 审批人ID |
| action | String | 审批动作 |
| comment | String | 审批意见 |
| approved_at | LocalDateTime | 审批时间 |

---

### 10. 设备与房间管理模块

#### 10.1 equipment（设备表）
| 字段 | 类型 | 说明 |
|------|------|------|
| equipment_id | Integer | 主键，自增 |
| equipment_name | String | 设备名称 |
| equipment_model | String | 设备型号 |
| fixed_asset_code | String | 固定资产编号 |
| manufacturer | String | 生产厂家 |
| room_id | Integer | 所在房间ID |
| production_capacity | String | 生产能力 |
| equipment_status | String | 设备状态 |
| purchase_date | LocalDate | 购置时间 |
| purchase_amount | BigDecimal | 购置金额 |
| last_maintenance_date | LocalDate | 上次维保时间 |
| maintenance_cycle | Integer | 维保周期（月） |
| next_maintenance_date | LocalDate | 下次维保时间 |

#### 10.2 room_info（房间表）
| 字段 | 类型 | 说明 |
|------|------|------|
| room_id | Integer | 主键，自增 |
| room_code | String | 房间编码 |
| room_name | String | 房间名 |
| room_location | String | 房间位置 |
| area | BigDecimal | 面积 |
| production_type | String | 生产制剂类型 |
| clean_area | Boolean | 是否为洁净区 |
| clean_grade | String | 洁净等级 |
| room_status | Integer | 房间状态 |
| disinfection_cycle | Integer | 消毒周期(天) |
| clean_inspection_cycle | Integer | 洁净检测周期(天) |
| function_type | String | 功能间分类 |

---

### 11. 质量检验模块

#### 11.1 inspection_plan（检验计划表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| plan_code | String | 计划编号（唯一） |
| plan_period | String | 计划月份/周次 |
| inspection_type | String | 检验类型 |
| object_name | String | 检验对象名称 |
| batch_no | String | 批号 |
| spec | String | 规格 |
| plan_time | LocalDate | 计划检验时间 |
| status | String | 状态 |

#### 11.2 inspection_record（检验记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| inspection_code | String | 检验编号（唯一） |
| related_inspection_request_code | String | 关联请检编号 |
| work_order_code | String | 关联生产任务编号 |
| object_name | String | 被检对象名称 |
| preparation_name | String | 关联制剂名称 |
| batch_no | String | 批号 |
| inspector | String | 检验人 |
| reviewer | String | 复核人 |
| conclusion | String | 总体结论 |

---

### 12. 机构与人员模块

#### 12.1 organization（机构信息表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| license_no | String | 许可证证号 |
| org_name | String | 医疗机构名称 |
| org_category | String | 医疗机构类别 |
| unified_social_credit_code | String | 统一社会信用代码 |
| legal_representative | String | 法定代表人 |
| preparation_address | String | 制剂配制地址 |
| preparation_scope | String | 配制范围 |
| status | Integer | 状态：1启用/0停用 |

#### 12.2 personnel_file（人员档案表）
| 字段 | 类型 | 说明 |
|------|------|------|
| personnel_file_id | Long | 主键，自增 |
| user_id | Long | 关联用户ID |
| employee_no | String | 工号 |
| name | String | 姓名 |
| department_id | Long | 所属部门ID |
| position_id | Long | 岗位ID |
| qualification | String | 人员资格认定 |
| education | String | 学历 |
| entry_date | LocalDate | 入职日期 |
| status | Integer | 状态：0离职/1在职 |

---

### 13. 环境监控模块

#### 13.1 temperature_humidity_record（温湿度记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| room_id | Integer | 关联房间ID |
| record_date | LocalDate | 检测日期 |
| temperature | BigDecimal | 温度值 |
| humidity | BigDecimal | 湿度值 |
| recorder | String | 记录人姓名 |

#### 13.2 其他环境记录表
- **pressure_difference_record**：压差记录
- **disinfection_record**：消毒记录
- **cleaning_record**：清洁记录
- **clean_inspection_record**：洁净检测记录

---

### 14. 文件与培训模块

#### 14.1 file_info（文件信息表）
| 字段 | 类型 | 说明 |
|------|------|------|
| file_id | Long | 主键，自增 |
| original_name | String | 原始文件名 |
| stored_name | String | 存储文件名 |
| file_path | String | 文件路径 |
| file_size | Long | 文件大小 |
| content_type | String | 文件类型 |
| category | String | 文件分类 |
| business_id | Long | 关联业务ID |
| business_type | String | 关联业务类型 |

#### 14.2 training_record（培训记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| training_no | String | 培训编号 |
| training_name | String | 培训名称 |
| training_category | String | 培训类别 |
| training_date | Date | 培训日期 |
| trainer | String | 培训讲师 |

---

## 四、API接口文档

### 认证模块
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/refresh | 刷新Token |
| GET | /api/user/info | 获取当前用户信息 |

### 用户权限管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/User/search | 搜索用户 |
| GET | /api/User/search-with-details | 搜索用户（含详情） |
| GET | /api/Role/search | 搜索角色 |
| GET | /api/Role/search-with-details | 搜索角色（含详情） |
| GET | /api/Permission/search | 搜索权限 |
| GET | /api/Department/search | 搜索部门 |
| GET | /api/Department/search-with-details | 搜索部门（含详情） |
| GET | /api/position/search-with-details | 搜索岗位（含详情） |
| GET | /api/position/list | 岗位列表 |

### 基础数据管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/material/search | 搜索物料 |
| GET | /api/material/search-with-details | 搜索物料（含详情） |
| GET | /api/preparation/search | 搜索制剂 |
| GET | /api/preparation/search-with-details | 搜索制剂（含详情） |
| POST | /api/preparation/save-with-details | 保存制剂（含明细） |
| GET | /api/preparation/{id}/details | 获取制剂明细 |
| GET | /api/preparation/formula/byPreparationCode | 按制剂编码查处方 |
| POST | /api/preparation/formula/batch | 批量保存处方 |
| GET | /api/Unit/search | 搜索计量单位 |
| GET | /api/DosageForm/search | 搜索剂型 |
| GET | /api/process-type/search | 搜索工序类型 |

### 采购管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/purchase-suppliers/search | 搜索供应商 |
| GET | /api/purchase-suppliers/search-with-details | 搜索供应商（含详情） |
| POST | /api/purchase-plan/with-details | 新建采购计划（含明细） |
| GET | /api/purchase-plan/{id}/details | 获取采购计划明细 |
| GET | /api/purchase-orders/search | 搜索采购订单 |
| GET | /api/purchase-orders/search-with-details | 搜索采购订单（含详情） |
| GET | /api/purchase-order-items/order/{orderId} | 按订单查明细 |

### 生产管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/production-plans/search | 搜索生产计划 |
| GET | /api/production-plans/search-with-details | 搜索生产计划（含详情） |
| GET | /api/work-orders/search | 搜索工单 |
| GET | /api/work-orders/generate-code | 生成工单编号 |
| GET | /api/process-record/plan/{planId} | 按计划查工序记录 |
| POST | /api/process-record/batch/plan/{planId} | 批量保存工序记录 |

### 生产单位管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/production_unit/search | 搜索生产单位 |
| GET | /api/production_unit/search-with-details | 搜索生产单位（含详情） |
| GET | /api/production_unit/enabled | 启用的生产单位 |
| POST | /api/production_unit/{id}/invoice | 添加发票信息 |

### 库存管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/stock/search | 搜索库存 |
| GET | /api/stock/search-with-details | 搜索库存（含详情） |
| GET | /api/stock/grouped-search | 分组搜索库存 |
| GET | /api/stock/{id}/transactions | 查库存交易记录 |

### 入库管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/stockin/search | 搜索入库单 |
| GET | /api/stockin/search-with-details | 搜索入库单（含详情） |
| POST | /api/stockin/withDetails | 新建入库单（含明细） |
| POST | /api/stockin/{id}/confirm | 确认入库 |
| GET | /api/stockin/generateCode | 生成入库单号 |

### 出库管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/stockout/search | 搜索出库单 |
| GET | /api/stockout/search-with-details | 搜索出库单（含详情） |
| POST | /api/stockout/withDetails | 新建出库单（含明细） |
| POST | /api/stockout/{id}/confirm | 确认出库 |
| GET | /api/stockout/generateCode | 生成出库单号 |

### 盘点管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/warehouse/check-orders/warehouses | 获取可盘点仓库 |
| GET | /api/warehouse/check-orders/stock-details | 获取库存明细 |
| GET | /api/warehouse/check-orders/{id} | 获取盘点单详情 |

### 调拨管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/warehouse/transfer-orders/warehouses | 获取调拨仓库列表 |
| GET | /api/warehouse/transfer-orders/materials | 获取调拨物料列表 |
| GET | /api/warehouse/transfer-orders/{id} | 获取调拨单详情 |

### 验收管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/acceptance/search | 搜索验收单 |
| GET | /api/acceptance/search-with-details | 搜索验收单（含详情） |
| POST | /api/acceptance/withDetails | 新建验收单（含明细） |
| POST | /api/acceptance/{id}/confirm-arrival | 确认到货 |
| GET | /api/acceptance/generateCode | 生成验收单号 |

### 领料管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/material-requisition/{id}/details | 获取领料明细 |
| POST | /api/material-requisition-slip/withDetails | 新建领料单（含明细） |
| GET | /api/material-requisition-slip/generate-code | 生成领料单号 |

### 审批流程
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/approval/workflow/{id} | 获取审批流程 |
| GET | /api/approval/workflow/type/{workflowType} | 按类型查流程 |
| POST | /api/approval/instance/create-with-binding | 创建审批实例 |
| POST | /api/approval/instance/{id}/approve | 审批同意 |
| POST | /api/approval/instance/{id}/reject | 审批驳回 |

### 质量检验
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/inspectionPlan/list | 检验计划列表 |
| GET | /api/inspectionRecord/list | 检验记录列表 |
| GET | /api/samplingRecord/list | 取样记录列表 |
| GET | /api/retainedSample/list | 留样记录列表 |
| GET | /api/releaseReview/list | 审核放行列表 |

### 设备与房间管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/equipment/search | 搜索设备 |
| GET | /api/equipment/active | 启用的设备列表 |
| GET | /api/room/search | 搜索房间 |
| GET | /api/room/active | 启用的房间 |

### 环境监控
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/temperatureHumidityRecord/list | 温湿度记录列表 |
| GET | /api/disinfectionRecord/list | 消毒记录列表 |
| GET | /api/cleaningRecord/reminder | 清洁预警 |

### 文件管理
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| POST | /api/files/upload | 上传文件 |
| GET | /api/files/{id} | 获取文件 |
| GET | /api/files/business | 按业务查文件 |
| GET | /api/file-manager/list | 文件管理器列表 |
| POST | /api/file-manager/mkdir | 创建文件夹 |
| DELETE | /api/file-manager/delete | 删除文件 |

### 仪表盘
| 方法 | 接口路径 | 功能 |
|------|----------|------|
| GET | /api/dashboard/summary | 仪表盘概览 |
| GET | /api/dashboard/metrics | 指标数据 |
| GET | /api/dashboard/todos | 待办事项 |

---

## 五、数据导入建议

### 1. 导入顺序建议
1. **基础数据**：unit → dosage_form → process_type → material → preparation
2. **组织架构**：department → position → user → role → permission
3. **业务基础**：production_unit → purchase_suppliers → equipment → room_info
4. **业务数据**：production_plan → work_order → stock → stock_in/stock_out

### 2. 注意事项
- 所有表都有 `is_deleted` 字段，默认值为 0
- 所有表都有 `version` 字段，用于乐观锁，默认值为 0
- 唯一约束字段（如 material_code, preparation_code）不能重复
- 密码字段使用 Argon2 加密，需要通过接口注册用户
- 日期时间字段格式：`yyyy-MM-dd HH:mm:ss`
- 日期字段格式：`yyyy-MM-dd`

### 3. 常用编码规则
- 入库单号：`RK + 日期 + 序号`
- 出库单号：`CK + 日期 + 序号`
- 工单编号：`GD + 日期 + 序号`
- 采购计划编号：`CGJH + 日期 + 序号`
- 验收单号：`YS + 日期 + 序号`

---

## 六、默认账户

| 用户名 | 密码 | 说明 |
|--------|------|------|
| root | root | 系统管理员，启动时自动创建 |

# 8.134 测试库清空与生产数据填充方案

> 编制日期：2026-11-11  
> 目标库：`8.134.48.54:3306/erp_db`（测试库，待清空）  
> 参考库：`8.148.27.237:3306/erp_db1`（生产库，**只读**，禁止写入）  
> 前端地址：`http://192.168.5.80:3006`（开发代理指向 `http://8.134.48.54:8080`）  
> 状态：**待确认，尚未执行任何写操作**

---

## 0. 建议口径

1. **保留全部主数据**（账号、权限、角色、部门、岗位、人员、单位、剂型、制剂、处方、工序模板、物料、供应商、房间、设备、生产单位、客户、审批流程定义、文件要求配置、系统配置），只清空业务单据、审批实例、业务附件和日志。
2. **不全量复制生产库**。生产库 8 个工单、10 个生产计划全部通过前端重录工作量很大，建议先填充 **2 条全链路 + 3 条状态样本 + 1 条辅料采购**，覆盖所有页面和状态；后续需要再补量。
3. **生产库只做 SELECT**。所有写操作脚本在连接后先断言 `host=8.134.48.54 且 database=erp_db`，不满足直接退出。
4. 清空前对 8.134 做**全库逻辑备份**（95 张表，含表结构与数据），保留恢复脚本。

---

## 1. 现状摸底（已只读确认）

| 项目 | 8.134 测试库 | 8.148 生产库 |
|---|---|---|
| 表数量 | 95 张 | 84 张 |
| 账号/角色/权限 | 25 用户、13 角色、193 权限 | 22 用户、13 角色、184 权限 |
| 主数据 | 28 制剂、2109 物料、5 供应商、26 房间、86 设备 | 基本一致 |
| 生产计划 | 10 | 10 |
| 工单 | 6 | 8 |
| 采购计划/订单 | 17 / 14 | 13 / 13 |
| 验收/入库/出库 | 15 / 13 / 17 | 15 / 17 / 20 |
| 检验任务 | 22（新流程表） | **无该表** |
| 审核放行 | 4 | 4 |

差异说明：生产库没有 `inspection_task`、`inspection_task_step`、`work_order_process_check`、`business_file`、`file_requirement_config` 及日志表，属于旧版结构。因此**新流程（检验任务六步、放行文件合规、统一附件）无法从生产库直接复制，必须在测试环境通过新前端重新走一遍生成**；生产库只作为业务数据（品种、批号、数量、日期、供应商、物料）的参考。

---

## 2. 保留（不清空）清单

### 2.1 账号与权限
`user`、`role`、`permission`、`role_perm`、`user_role`、`user_department`、`department`、`position`、`duty`

### 2.2 组织与人员
`organization`、`organization_certificate`、`personnel_file`、`personnel_certificate`

### 2.3 生产与物料主数据
`unit`、`dosage_form`、`process_type`、`preparation`、`preparation_formula`、`preparation_process_template`、`preparation_document`、`material`、`purchase_suppliers`、`room_info`、`equipment`、`production_unit`、`prod_unit_invoice`、`customer`

### 2.4 配置与流程定义
`system_config`、`approval_workflow`、`approval_node`、`file_requirement_config`

### 2.5 附件
`business_file` / `file_info` 中仅保留主数据类附件：
`PREPARATION`、`PREPARATION_DOCUMENT`、`PREPARATION_PROCESS_TEMPLATE`、`PERSONNEL_FILE`、`PERSONNEL_CERTIFICATE`、`ORGANIZATION`、`ORGANIZATION_CERTIFICATE`、`DUTY`、`POSITION`

> 如果希望“基础数据”只保留账号/角色/权限，制剂、物料、供应商、房间、设备也全部清空后从前端重建，请单独说明。按当前主数据量（2109 个物料）通过页面重建不现实，默认建议全部保留。

---

## 3. 清空清单（业务数据）

### 3.1 采购与验收（7）
`purchase_plan`、`purchase_plan_detail`、`purchase_orders`、`purchase_order_items`、`acceptance_order`、`acceptance_detail`、`acceptance_resend_log`

### 3.2 库存（8）
`stock`、`stock_transaction`、`stock_in`、`stock_in_detail`、`stock_in_sequence`、`stock_out`、`stock_out_detail`、`stock_out_sequence`

### 3.3 生产（9）
`production_plan`、`plan_status_log`、`work_order`、`work_order_process_execution`、`work_order_process_check`、`production_pick`、`production_pick_detail`、`material_requisition_slip`、`material_requisition_slip_detail`

### 3.4 退库与销售（3）
`return_order`、`return_order_detail`、`sales_order`

### 3.5 质检与放行（11）
`inspection_plan`、`inspection_request`、`inspection_task`、`inspection_task_step`、`inspection_record`、`sampling_record`、`retained_sample`、`destruction_record`、`destruction_record_detail`、`release_review`、`deviation_record`

### 3.6 审批实例（2）
`approval_instance`、`approval_record`（保留 `approval_workflow`、`approval_node` 定义）

### 3.7 辅助业务记录（17）
`energy_record`、`cleaning_record`、`clean_inspection_record`、`disinfection_record`、`maintenance_record`、`maintenance_record_detail`、`temperature_humidity_record`、`pressure_difference_record`、`production_process_record`、`equipment_maintenance`、`training_record`、`verification_record`、`check_order`、`check_order_detail`、`transfer_order`、`transfer_order_detail`、`supplier_audit`

### 3.8 业务附件与日志（7）
- `business_file`：按 `biz_type` 删除业务单据附件，保留主数据类附件
- `file_info`：删除业务类附件记录（`business_type` 属于上述业务类型）
- `business_file_migration_conflict`
- `sys_data_change_log`、`sys_login_log`、`sys_operation_log`、`file_operation_log`

执行方式：`SET FOREIGN_KEY_CHECKS=0` 后逐表 `TRUNCATE`，结束后恢复；附件表按条件 `DELETE`。保留表的自增与数据不动。

---

## 4. 数据填充范围（建议）

| 编号 | 数据链 | 生产库参考 | 覆盖场景 |
|---|---|---|---|
| A | 活血通络丸 7000 盒 / 批号 260901 | 计划 `Plan202610190002`、工单 `GD202610190001`、采购 `CGDH202610190006/007`、放行 `FX-20261019-001`、销售 `CPCK-20261020-001` | 采购→验收→入库→生产→领料→检验→成品入库→放行→成品出库 **完整链路** |
| B | 疏肝养胃丸 7000 盒 / 批号 260901 | 计划 `Plan202610190001`、工单 `GD202610200001`、采购 `CGDH202610190001/003`、放行 `FX-20261020-001`、销售 `CPCK-20261020-002` | 放行**缺件拦截**、文件上传、拒绝后重新放行 |
| C | 益肺平喘丸 7000 盒 / 批号 261001 | 计划 `Plan202610190005`、工单 `GD202610200004`、采购 `CGDH202610190003/009`、放行 `FX-20261104-001`、销售 `CPCK-20261104-001` | 跨月数据、成品库存预警、首页看板 |
| D | 桂附潜阳丸 7000 盒 | 计划 `Plan202610190008`、工单 `GD202610200005` | **生产中**状态、工序执行、设备使用记录 |
| E | 护肾排毒丸 3500 瓶 | 计划 `Plan202610190004`（待生产）、采购 `CGJH202610190006/007` | **待生产**计划、采购计划审批与进度 |
| F | 蜂蜜 500kg（辅料） | 采购计划 `CGJH202610190002`、订单 `CGDH202610190009` | 辅料采购→验收→入库 |

> 生产库共 8 个工单、10 个生产计划；如要求全部复制，可按同一方法扩展到全部，但预计前端录入时间会从约 1 个工作日增加到 3 个工作日以上。

---

## 5. 实施步骤

### 阶段 0：准备与备份（约 30 分钟）
1. 确认 8.134 当前无人使用，记录维护窗口。
2. 确认 `http://8.134.48.54:8080` 已部署**最新后端**（含产品汇总、检验任务、审核放行、统一附件）。若未部署，先手工部署，否则新页面无法联调。
3. 用 Python + PyMySQL 导出 8.134 全库：
   - `SHOW CREATE TABLE` + 全量 `INSERT`，生成可恢复 SQL；
   - 备份文件：`_analysis/db-backup/erp_db_20261111_HHmm.sql`；
   - 备份后校验表数量与关键表行数。
4. 生成生产库参考清单（只读 SELECT）：品种、批号、供应商、采购明细、检验项目、放行记录，输出 Excel/Markdown 对照表，作为前端录入依据。

### 阶段 1：清空测试库（约 15 分钟）
1. 写操作脚本连接前断言目标为 `8.134.48.54/erp_db`。
2. 按第 3 节清单关闭外键检查后 `TRUNCATE` 业务表；附件按类型 `DELETE`。
3. 校验所有业务表行数为 0，保留表行数不变。
4. 记录清空前后行数快照。

### 阶段 2：前端录入（主体工作，约 1 个工作日）
按依赖顺序逐条链路录入，每条链路完成后截图并记录单号：

1. **生产计划**：创建计划（品种、批量、计划生产时间、交付时间）→ 审批/下达。
2. **采购计划**：按计划生成原料/包材/辅料采购计划 → 审批（自动生成采购订单）。
3. **采购订单**：核对供应商、数量、价格、到货日期。
4. **货物验收**：到货确认 → 外包装/性状检查（合格）→ 初验合格 → 检验合格 → 入库。
5. **生产任务/工单**：配置批号、批量、工序、设备 → 配置完成 → 工序执行（开始/结束）。
6. **生产领料**：按工单生成领料单 → 发料 → 生成领料出库单；必要时做退库。
7. **请检与检验**：中间产品/成品请检（自动生成检验任务）→ 计划 → 取样 → 检验记录 → 留样 → 销毁（可跳过）。
8. **成品入库**：工单成品入库。
9. **审核放行**：保存工序符合性检查 → 上传必传文件 → 通过放行；批次 B 先验证缺件拦截，再补齐文件放行。
10. **成品出库**：销售/成品出库台账出库。
11. **辅助记录**：设备使用记录、清洁/养护记录各 1 条，用于页面和冲突校验。

### 阶段 3：验证（约 2 小时）
1. SQL 行数与单号核对。
2. 页面验证：产品信息汇总 10 节点、检验看板、审核放行状态、采购进度、成品库存预警、首页看板。
3. 导出验证：批次 A 的 ZIP/CSV 集中导出。
4. 日志验证：登录、操作、字段变更日志有新增记录。

### 阶段 4：交付
- 数据填充清单（单号、品种、批号、状态、页面路径）。
- 关键页面截图。
- 遗留问题与后续补量清单。

---

## 6. 安全与回滚

1. **生产库零写入**：所有生产库连接只执行 `SELECT`；脚本对写连接做 host/database 断言。
2. **全库备份**：清空前完成 8.134 全库 SQL 备份，备份文件不放入 Git。
3. **回滚**：如清空或填充中断，执行恢复脚本（关闭外键检查 → 按备份重建/回填）恢复原状。
4. **分批执行**：清空、附件清理、日志清理分三步执行，每步完成后校验。
5. **不动磁盘文件**：只删数据库附件记录，不删除服务器文件存储，避免误删主数据文件。

---

## 7. 主要风险

| 风险 | 影响 | 应对 |
|---|---|---|
| 8.134 后端不是最新版本 | 新页面接口 404 或字段不一致 | 阶段 0 先验证接口；必要时手工部署 |
| 浏览器自动化不稳定 | 录入中断、重复单号 | 每条链路分步保存；失败重试；保留中间截图 |
| 放行必传文件多（35 条配置） | 放行无法通过 | 先验证缺件拦截，再按清单批量上传占位 PDF |
| 其他同事同时使用测试库 | 数据被覆盖或清空影响他人 | 确认维护窗口，操作前通知 |
| 生产库结构较旧 | 新流程数据无法直接复制 | 新流程全部通过前端重新生成，生产库仅作参考 |
| 编号生成依赖日期 | 清空后新单号与历史不同 | 属预期行为，测试以新单号为准 |

---

## 8. 需要确认的事项

1. **保留范围**：是否同意保留全部主数据（制剂、物料、供应商、房间、设备、工序模板等），只清业务单据？
2. **填充范围**：按建议的 A–F 六条数据链，还是要求复制生产库全部 8 个工单 / 10 个生产计划？
3. **维护窗口**：8.134 测试库是否可以立即清空，期间是否有其他人使用？
4. **后端版本**：8.134:8080 是否已部署最新代码？如未部署，是否授权我先部署再执行？
5. **录入方式**：以浏览器自动化操作前端页面为主；若个别页面无法自动化（如文件上传），是否允许改用后端接口补录（仅测试库）？
6. **登录账号**：默认使用 `root / root`，如已修改请提供测试账号。

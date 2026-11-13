# 8.134 测试库清空与生产数据复制记录

> 执行时间：2026-11-11 00:33 — 01:06  
> 目标库：`8.134.48.54:3306/erp_db`（测试库）  
> 参考库：`8.148.27.237:3306/erp_db1`（生产库，全程只读）  
> 前端：`http://192.168.5.80:3006`（代理到 `http://8.134.48.54:8080`）  
> 登录账号：`root / root`

## 1. 备份

清空前已对 8.134 做全库逻辑备份：

- 文件：`_analysis/db-backup/erp_db_20261111_003300.sql`
- 内容：95 张表的 `DROP TABLE` + `CREATE TABLE` + 全量数据
- 大小：约 1.6 MB
- 恢复方式：在 8.134 执行该 SQL（会先删表再重建），执行前请再次确认目标库。

## 2. 保留的基础数据

账号权限、组织人员、主数据、配置与流程定义均保留：

| 类别 | 主要表 | 保留行数 |
|---|---|---|
| 账号权限 | `user`、`role`、`permission`、`role_perm`、`user_role` | 22 / 13 / 191 / 741 / 50 |
| 组织人员 | `department`、`position`、`personnel_file` | 3 / 6 / 11 |
| 生产主数据 | `preparation`、`preparation_formula`、`preparation_process_template` | 28 / 509 / 131 |
| 物料与供应商 | `material`、`purchase_suppliers` | 2109 / 5 |
| 房间设备 | `room_info`、`equipment`、`production_unit` | 26 / 86 / 6 |
| 配置流程 | `system_config`、`approval_workflow`、`approval_node`、`file_requirement_config` | 19 / 6 / 27 / 35 |

## 3. 清空与恢复的业务数据

清空 60 余张业务表后，从 8.148 按快照恢复（89 张共同表，字段兼容；测试库新增可空字段已回填日期）：

| 模块 | 表 | 恢复行数 |
|---|---|---|
| 生产计划 | `production_plan` | 11（10 条生产库 + 1 条前端验证后删除；活动 7、软删 4） |
| 生产任务 | `work_order` | 8（活动 7、软删 1） |
| 工序执行 | `work_order_process_execution` | 87 |
| 采购计划 | `purchase_plan` / `purchase_plan_detail` | 13 / 120 |
| 采购订单 | `purchase_orders` / `purchase_order_items` | 13 / 120 |
| 货物验收 | `acceptance_order` / `acceptance_detail` | 15 / 152 |
| 入库 | `stock_in` / `stock_in_detail` | 17 / 124 |
| 出库 | `stock_out` / `stock_out_detail` | 21（含 1 张新流程草稿）/ 128 |
| 库存 | `stock` / `stock_transaction` | 124 / 263 |
| 退库 | `return_order` / `return_order_detail` | 4 / 11 |
| 销售 | `sales_order` | 4 |
| 请检/检验 | `inspection_request` / `inspection_record` | 18 / 18（含补录与去重后的结果） |
| 审核放行 | `release_review` | 7（4 条已放行 + 3 条草稿） |
| 审批实例 | `approval_instance` / `approval_record` | 38 / 323 |

## 4. 新流程数据补录

生产库没有新流程表，以下数据在测试库通过前端页面/业务接口补录：

| 数据 | 数量 | 说明 |
|---|---|---|
| 请检单 | 18 | 生产库 12 条 + 为 467/468/469 三个生产中工单补录 6 条中间产品请检 |
| 检验任务 | 18 | 全部关联请检单并已走到“已完成” |
| 检验计划 | 18 | 每个检验任务均已生成检验计划（`JH-20261111-xxx`） |
| 取样记录 | 18 | 每个任务均有取样记录 |
| 检验记录 | 18 | 每个任务对应 1 条检验记录，结论全部“合格”；已清理补录产生的重复记录，4 个已完成批次复用生产库历史记录 |
| 留样记录 | 4 | 4 个成品检验任务完成留样 |
| 销毁记录 | 4 | 4 个成品留样完成任务完成销毁 |
| 工序符合性检查 | 77 | 7 个活动批次 × 11 道工序，全部“符合” |
| 生产领料单 | 17 | 16 张由历史“生产领料出库”回填（共 124 条明细），另有 1 张 `SCLL-20261111-001` 为前端创建、已发料；均不新增/修改出库与库存 |
| 业务附件 | 1 | 批次 `GD202611090001` 上传“批生产记录-测试.pdf” |
| 退库/验收新增字段 | 4 / 15 | 回填退库人、验收人、审核人、退库时间、外包装/性状检查 |
| 审核放行草稿 | 3 | 467/468/469 三个生产中批次保存“待补充”草稿，待成品检验与批记录补齐后放行 |

## 4.1 业务附件恢复

从清空前的全库备份中恢复了 23 条业务附件关系（文件实体仍在测试服务器存储，已实测可下载）：

| 业务类型 | 文件槽位 | 数量 | 覆盖范围 |
|---|---|---|---|
| 货物验收（ACCEPTANCE_ORDER） | 随货清单 WAYBILL | 11 | 验收单 2029—2038 |
| 货物验收（ACCEPTANCE_ORDER） | 检验报告 INSPECTION_REPORT | 10 | 验收单 2029—2038 |
| 生产任务（WORK_ORDER） | 批生产记录 BATCH_RECORD | 1 | 工单 462（另有 469 为前端上传测试文件） |
| 审核放行（RELEASE_REVIEW） | 放行附件 DEFAULT | 1 | 放行记录 91 |

恢复后 `business_file` 共 104 条、`file_info` 共 81 条；放行批次 466 的“验收检验报告”“随货清单”“放行审核附件”均已达到 1/1 或 2/2。

**未恢复**：验收单 2045—2047 的 6 个附件（随货清单/检验报告）不在清空前的备份中，生产文件接口返回 404，无法下载到本地；需要从生产文件存储直接导出这 6 个文件后补传。

### 4.2 疏肝养胃丸占位文件

为工单 463（疏肝养胃丸 260901）的**必传/条件必传**槽位上传了 22 个占位 PDF：

- 品种资料：批记录模板、工艺规程、质量标准、验证方案
- 工单：批生产记录
- 放行：放行审核附件
- 检验：检验记录/图谱 3 份、请检单附件 3 份、取样记录附件 9 份
- 计划：生产计划附件
- 采购：采购订单附件 2 份
- 验收：验收检验报告 3 份、随货清单 3 份

按业务要求，可选槽位（成品入库/出库、工艺流程图、工序模板、其他文件、工序附件、领料单、发票）的占位文件已删除。验证结果：批次缺件 0 项，放行合规五项全部通过（`filePass=true`），可直接放行。

## 5. 页面验证结果

| 页面 | 结果 |
|---|---|
| 产品信息汇总 | 6 个制剂、7 个批次；链路、缺件、文件完整度正常展示 |
| 检验管理（新） | 任务总数 18，全部已完成；7 个批次均检验合格 |
| 审核放行（新） | 7 个批次；已放行 4、待审核/待补充 3，工序检查 11/11 |
| 生产领料申请 | 1 条已发料记录，关联出库单草稿 |
| 生产计划 / 生产任务 | 活动计划 7 条、活动任务 7 条，状态与生产用时正常 |
| 货物验收 / 入库 / 出库 / 退库 / 库存查询 | 数据量与生产库一致，新增字段可显示 |
| 成品库存预警 | 28 个品种、4 个预警，月均出库与可支撑月数正常 |
| 管理驾驶舱 | 成品库存预警 4 条、待确认出库 1 条 |

关键接口冒烟：`product-summary/overview`、`inspection/batches/stats`、`release-review/batches/stats`、`finished-stock-warning/summary`、`dashboard/todos`、`production-pick` 均返回 200。

## 6. 已知差异与说明

1. **软删除数据**：生产库有 3 个生产计划和 1 个工单为软删除状态，已按原状态恢复，页面默认不显示；数据库中共有 10 个生产计划、8 个工单。
2. **历史附件未复制**：生产库附件路径指向生产文件服务器，未复制到测试库；放行页面显示缺件属于预期，可继续用于测试文件上传与缺件拦截。已为 1 个批次上传测试附件验证上传链路。
3. **原材料库存为 0**：生产库快照中原料已被领用耗尽，因此生产领料测试使用有库存的包材明细发料；如需测试完整原料领料，先通过采购→验收→入库补一批原料。
4. **统计图当前月为 0**：生产数据集中在 2026 年 9—10 月，统计页默认“本月”，切换到对应月份或本年即可看到数据。
5. **统计图生产天数**：`GD202610190001` 原采购下达时间（2026-10-19）晚于成品入库时间（2026-09-28），导致生产天数为空。已将对应采购计划/采购订单时间调整为 2026-09-01，生产天数现显示 28 天。
6. **检验批次统计为 7 个批次**：已为 467/468/469 补录中间产品请检，所有 7 个活动工单均有检验任务和记录；4 个已入库批次已完成成品检验并放行，3 个生产中批次保存放行草稿。
7. **批号补齐**：467/468/469 原批号为空，已补为 `261002` / `261003` / `261101`，并同步检验请检、任务、计划、取样、记录和放行草稿。
8. **编号与日期对齐**：检验计划、检验任务、取样、留样编号已按业务日期重排，避免历史批次统一显示 20261111。

## 7. 操作脚本

| 脚本 | 用途 |
|---|---|
| `_analysis/db-tools/backup.py` | 全库备份 |
| `_analysis/db-tools/clear_business_data.py --dry-run/--execute` | 清空业务数据 |
| `_analysis/db-tools/copy_prod_business.py` | 从生产库复制历史业务表 |
| `_analysis/db-tools/fill_process_checks.py` | 通过接口补齐工序检查 |
| `_analysis/db-tools/fill_history_process_checks.py` | 为已放行批次补历史工序检查 |
| `_analysis/db-tools/fill_new_fields.py` | 回填退库/验收新增字段 |
| `_analysis/db-tools/fill_inspection_flow.py` | 补请检、检验计划并完成全部检验任务 |
| `_analysis/db-tools/clean_duplicate_inspection_records.py` | 清理重复检验记录并复用历史记录 |
| `_analysis/db-tools/fix_plan307_purchase_dates.py` | 修复生产天数为空的采购日期倒挂 |
| `_analysis/db-tools/align_inspection_plan_dates.py` | 检验计划周期/日期与请检单对齐 |
| `_analysis/db-tools/align_sampling_dates.py` | 取样/留样日期与检验记录对齐 |
| `_analysis/db-tools/assign_batch_numbers.py` | 为生产中工单补批号并同步检验表 |
| `_analysis/db-tools/sync_release_batch_numbers.py` | 同步放行草稿批号 |
| `_analysis/db-tools/renumber_inspection_plans.py` | 按日期重排检验计划编号 |
| `_analysis/db-tools/renumber_inspection_tasks.py` | 按请检日期重排检验任务号 |
| `_analysis/db-tools/renumber_quality_codes.py` | 重排取样/留样/销毁编号 |
| `_analysis/db-tools/restore_attachments_from_backup.py` | 从清空前备份恢复业务附件关系 |
| `_analysis/db-tools/restore_prod_file_paths.py` | 尝试挂载生产附件路径（2045—2047，测试服务器无文件） |
| `_analysis/db-tools/copy_production_attachments.py` | 生产附件下载并上传到测试库（需生产 Token） |
| `_analysis/db-tools/backfill_production_picks.py` | 为历史生产领料出库回填领料单及明细 |
| `_analysis/db-tools/fill_placeholder_files.py` | 为疏肝养胃丸必传/条件必传槽位上传占位 PDF |
| `_analysis/db-tools/delete_optional_placeholder_files.py` | 删除可选槽位占位文件 |
| `_analysis/db-tools/clear_business_data.py` | 清空脚本（含安全断言） |

-- ===================================
-- 业务附件数据迁移：file_info -> business_file
-- 说明：
--   1. 脚本可重复执行，所有插入均带 NOT EXISTS；
--   2. 只迁移 file_info.is_deleted = 0 且 business_id > 0 的记录；
--   3. 无法自动判定归属的记录写入 business_file_migration_conflict；
--   4. 执行前请备份 file_info；
--   5. 测试环境使用 8.134.48.54，禁止向 8.148.27.237 写测试数据。
-- ===================================

-- -----------------------------------
-- 一、简单一对一类型
-- -----------------------------------

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ACCEPTANCE_ORDER', fi.business_id, 0, fi.file_id, 'WAYBILL', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'GOODS_ACCEPTANCE_WAYBILL'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM acceptance_order a WHERE a.acceptance_id = fi.business_id AND a.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ACCEPTANCE_ORDER'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ACCEPTANCE_ORDER', fi.business_id, 0, fi.file_id, 'INVOICE', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'GOODS_ACCEPTANCE_INVOICE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM acceptance_order a WHERE a.acceptance_id = fi.business_id AND a.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ACCEPTANCE_ORDER'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ACCEPTANCE_ORDER', fi.business_id, 0, fi.file_id, 'INSPECTION_REPORT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'GOODS_ACCEPTANCE_INSPECTION_REPORT'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM acceptance_order a WHERE a.acceptance_id = fi.business_id AND a.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ACCEPTANCE_ORDER'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'DESTRUCTION_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'QUALITY_DESTRUCTION_RECORD'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM destruction_record d WHERE d.id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'DESTRUCTION_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'DEVIATION_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'QUALITY_DEVIATION_RECORD'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM deviation_record d WHERE d.id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'DEVIATION_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'INSPECTION_REQUEST', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'QUALITY_INSPECTION_RECORD'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM inspection_request r WHERE r.id = fi.business_id AND r.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'INSPECTION_REQUEST'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'SAMPLING_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'QUALITY_SAMPLING_RECORD'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM sampling_record r WHERE r.id = fi.business_id AND r.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'SAMPLING_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'RELEASE_REVIEW', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('AUDIT_RELEASE_FILE', 'AUDIT_RELEASE_RECORD')
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM release_review r WHERE r.id = fi.business_id AND r.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'RELEASE_REVIEW'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'RETAINED_SAMPLE', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('SAMPLE_RETENTION_FILE', 'SAMPLE_RETENTION_RECORD')
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM retained_sample r WHERE r.id = fi.business_id AND r.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'RETAINED_SAMPLE'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'VERIFICATION_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'VERIFICATION_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM verification_record r WHERE r.id = fi.business_id AND r.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'VERIFICATION_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- -----------------------------------
-- 二、生产计划、工单、工序、生产单位
-- -----------------------------------

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PRODUCTION_PLAN', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PRODUCTION_PLAN'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM production_plan p WHERE p.id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PRODUCTION_PLAN'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'WORK_ORDER', fi.business_id, 0, fi.file_id,
       CASE
           WHEN fi.custom_path LIKE '生产批记录上传/%' THEN 'BATCH_RECORD'
           WHEN fi.custom_path LIKE '其他文件上传/%' THEN 'OTHER'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PRODUCTION_TASK'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM work_order w WHERE w.work_order_id = fi.business_id AND w.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'WORK_ORDER'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'WORK_ORDER_PROCESS_EXECUTION',
       CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED),
       0, fi.file_id, 'PROCESS_ATTACHMENT',
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PRODUCTION_PROCESS'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[0-9]+/'
  AND EXISTS (
      SELECT 1 FROM work_order_process_execution w
      WHERE w.id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND w.work_order_id = fi.business_id
        AND w.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'WORK_ORDER_PROCESS_EXECUTION'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

-- 兼容旧版本 PRODUCTION_PROCESS 的 custom_path 仅存工序行 ID（没有 /工单ID 后缀）
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'WORK_ORDER_PROCESS_EXECUTION',
       CAST(fi.custom_path AS UNSIGNED),
       0, fi.file_id, 'PROCESS_ATTACHMENT',
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PRODUCTION_PROCESS'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[0-9]+$'
  AND EXISTS (
      SELECT 1 FROM work_order_process_execution w
      WHERE w.id = CAST(fi.custom_path AS UNSIGNED)
        AND w.work_order_id = fi.business_id
        AND w.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'WORK_ORDER_PROCESS_EXECUTION'
        AND bf.biz_id = CAST(fi.custom_path AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PRODUCTION_UNIT', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'GENERAL_ATTACHMENT'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM production_unit p WHERE p.prod_unit_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PRODUCTION_UNIT'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- -----------------------------------
-- 三、设备、能耗、养护
-- -----------------------------------

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'EQUIPMENT', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'EQUIPMENT_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM equipment e WHERE e.equipment_id = fi.business_id AND e.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'EQUIPMENT'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'EQUIPMENT_MAINTENANCE', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'EQUIPMENT_MAINTENANCE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM equipment_maintenance m WHERE m.maintenance_id = fi.business_id AND m.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'EQUIPMENT_MAINTENANCE'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ENERGY_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('ENERGY_FILE', 'ENERGY_RECORD')
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM energy_record e WHERE e.record_id = fi.business_id AND e.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ENERGY_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'MAINTENANCE_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('MAINTENANCE_RECORD', 'STOCK_MAINTENANCE_RECORD')
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM maintenance_record m WHERE m.id = fi.business_id AND m.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'MAINTENANCE_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'MAINTENANCE_RECORD_DETAIL', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('MAINTENANCE_DETAIL', 'STOCK_MAINTENANCE_DETAIL')
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM maintenance_record_detail d WHERE d.id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'MAINTENANCE_RECORD_DETAIL'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- -----------------------------------
-- 四、采购、供应商、培训
-- -----------------------------------

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PURCHASE_ORDER', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PURCHASE_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM purchase_orders p WHERE p.id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PURCHASE_ORDER'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PURCHASE_SUPPLIER', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'SUPPLIER_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM purchase_suppliers s WHERE s.id = fi.business_id AND s.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PURCHASE_SUPPLIER'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'SUPPLIER_AUDIT', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'SUPPLIER_ATTACHMENT'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM supplier_audit a WHERE a.id = fi.business_id AND a.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'SUPPLIER_AUDIT'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'TRAINING_RECORD', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'TRAINING_ATTACHMENT'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM training_record t WHERE t.id = fi.business_id AND t.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'TRAINING_RECORD'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- -----------------------------------
-- 五、岗位、职务、人员
-- -----------------------------------

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'POSITION', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'POSITION_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM position p WHERE p.position_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'POSITION'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'DUTY', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'DUTY_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM duty d WHERE d.duty_id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'DUTY'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PERSONNEL_FILE', fi.business_id, 0, fi.file_id, 'ATTACHMENT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PERSONNEL_ATTACHMENT'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM personnel_file p WHERE p.personnel_file_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PERSONNEL_FILE'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id
                  AND bf.file_kind = 'ATTACHMENT');

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PERSONNEL_FILE', fi.business_id, 0, fi.file_id, 'HEALTH_FILE', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PERSONNEL_HEALTH_FILE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM personnel_file p WHERE p.personnel_file_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PERSONNEL_FILE'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id
                  AND bf.file_kind = 'HEALTH_FILE');

-- 人员证书：custom_path 形如 “证书类型_证书ID/personnelFileId”
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PERSONNEL_CERTIFICATE',
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '/', 1), '_', -1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PERSONNEL_CERTIFICATE'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[^/]+_[0-9]+/'
  AND EXISTS (
      SELECT 1 FROM personnel_certificate c
      WHERE c.certificate_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '/', 1), '_', -1) AS UNSIGNED)
        AND c.personnel_file_id = fi.business_id
        AND c.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'PERSONNEL_CERTIFICATE'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '/', 1), '_', -1) AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

-- 解析失败的证书文件保留在人员档案下
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PERSONNEL_FILE', fi.business_id, 0, fi.file_id, 'CERTIFICATE_FILE', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PERSONNEL_CERTIFICATE'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND NOT (fi.custom_path REGEXP '^[^/]+_[0-9]+/')
  AND EXISTS (SELECT 1 FROM personnel_file p WHERE p.personnel_file_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'PERSONNEL_FILE'
        AND bf.biz_id = fi.business_id AND bf.ref_id = 0
        AND bf.file_id = fi.file_id AND bf.file_kind = 'CERTIFICATE_FILE'
  );

-- -----------------------------------
-- 六、机构 / 机构证书
-- -----------------------------------

-- 许可证扫描件
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ORGANIZATION', fi.business_id, 0, fi.file_id, 'LICENSE_SCAN', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('ORGANIZATION_CERTIFICATE', 'ORGANIZATION_LICENSE')
  AND fi.is_deleted = 0
  AND fi.custom_path LIKE '许可证扫描件/%'
  AND EXISTS (SELECT 1 FROM organization o WHERE o.id = fi.business_id AND o.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ORGANIZATION'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id
                  AND bf.file_kind = 'LICENSE_SCAN');

-- 机构证书
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ORGANIZATION_CERTIFICATE', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ORGANIZATION_CERTIFICATE'
  AND fi.is_deleted = 0
  AND fi.custom_path NOT LIKE '许可证扫描件/%'
  AND EXISTS (SELECT 1 FROM organization_certificate c WHERE c.id = fi.business_id AND c.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ORGANIZATION_CERTIFICATE'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- 机构照片
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ORGANIZATION', fi.business_id, 0, fi.file_id, 'PHOTO', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ORGANIZATION_PHOTO'
  AND fi.is_deleted = 0
  AND EXISTS (SELECT 1 FROM organization o WHERE o.id = fi.business_id AND o.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ORGANIZATION'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id
                  AND bf.file_kind = 'PHOTO');

-- -----------------------------------
-- 七、房间与房间日志
-- -----------------------------------

-- 房间日志：custom_path 形如 “【记录ID】/房间ID”
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'CLEANING_RECORD',
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ROOM_CLEANING_RECORD'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^【[0-9]+】/'
  AND EXISTS (
      SELECT 1 FROM cleaning_record c
      WHERE c.id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED)
        AND c.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'CLEANING_RECORD'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'CLEAN_INSPECTION_RECORD',
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ROOM_CLEAN_INSPECTION'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^【[0-9]+】/'
  AND EXISTS (
      SELECT 1 FROM clean_inspection_record c
      WHERE c.id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED)
        AND c.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'CLEAN_INSPECTION_RECORD'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

-- 房间页面上的规程附件
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ROOM_INFO', fi.business_id, 0, fi.file_id,
       CASE fi.business_type
           WHEN 'ROOM_DISINFECTION' THEN 'DISINFECTION'
           WHEN 'ROOM_CLEAN_INSPECTION' THEN 'CLEAN_INSPECTION'
           WHEN 'ROOM_CLEANING_RECORD' THEN 'CLEANING_RECORD'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('ROOM_DISINFECTION', 'ROOM_CLEAN_INSPECTION', 'ROOM_CLEANING_RECORD')
  AND fi.is_deleted = 0
  AND fi.custom_path NOT REGEXP '^【[0-9]+】/'
  AND EXISTS (SELECT 1 FROM room_info r WHERE r.room_id = fi.business_id AND r.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'ROOM_INFO'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id
                  AND bf.file_kind = CASE fi.business_type
                                         WHEN 'ROOM_DISINFECTION' THEN 'DISINFECTION'
                                         WHEN 'ROOM_CLEAN_INSPECTION' THEN 'CLEAN_INSPECTION'
                                         WHEN 'ROOM_CLEANING_RECORD' THEN 'CLEANING_RECORD'
                                         ELSE 'DEFAULT'
                                     END);

-- -----------------------------------
-- 八、剂型 / 制剂（重点冲突）
-- -----------------------------------

-- 只存在于 dosage_form 的 ID -> DOSAGE_FORM
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'DOSAGE_FORM', fi.business_id, 0, fi.file_id,
       CASE
           WHEN fi.custom_path LIKE '批记录模板/%' THEN 'BATCH_RECORD_TEMPLATE'
           WHEN fi.custom_path LIKE '验证方案/%' THEN 'VERIFICATION_TEMPLATE'
           WHEN fi.custom_path LIKE '检验模板/%' THEN 'INSPECTION_TEMPLATE'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_DOCUMENT'
  AND fi.is_deleted = 0
  AND EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'DOSAGE_FORM'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- 只存在于 preparation 的 ID -> PREPARATION
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PREPARATION', fi.business_id, 0, fi.file_id,
       CASE
           WHEN fi.custom_path LIKE '工艺规程/%' THEN 'PROCESS_SPEC'
           WHEN fi.custom_path LIKE '批记录模板/%' THEN 'BATCH_RECORD_TEMPLATE'
           WHEN fi.custom_path LIKE '验证方案/%' THEN 'VERIFICATION_PLAN'
           WHEN fi.custom_path LIKE '质量标准/%' THEN 'QUALITY_STANDARD'
           WHEN fi.custom_path LIKE '工艺规程流程图/%' THEN 'PROCESS_FLOWCHART'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_DOCUMENT'
  AND fi.is_deleted = 0
  AND EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.biz_type = 'PREPARATION'
                  AND bf.biz_id = fi.business_id AND bf.ref_id = 0 AND bf.file_id = fi.file_id);

-- 制剂工序模板：custom_path 形如 “模板ID/制剂ID”
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PREPARATION_PROCESS_TEMPLATE',
       CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_PROCESS'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[0-9]+/'
  AND EXISTS (
      SELECT 1 FROM preparation_process_template t
      WHERE t.template_id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND t.preparation_id = fi.business_id
        AND t.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'PREPARATION_PROCESS_TEMPLATE'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

-- 兼容旧版本 PREPARATION_PROCESS 的 custom_path 仅存模板 ID（没有 /制剂ID 后缀）
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PREPARATION_PROCESS_TEMPLATE',
       CAST(fi.custom_path AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_PROCESS'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[0-9]+$'
  AND EXISTS (
      SELECT 1 FROM preparation_process_template t
      WHERE t.template_id = CAST(fi.custom_path AS UNSIGNED)
        AND t.preparation_id = fi.business_id
        AND t.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'PREPARATION_PROCESS_TEMPLATE'
        AND bf.biz_id = CAST(fi.custom_path AS UNSIGNED)
        AND bf.ref_id = 0 AND bf.file_id = fi.file_id
  );

-- 冲突：PREPARATION_DOCUMENT 的 business_id 同时命中剂型和制剂，或两边都不存在
INSERT INTO business_file_migration_conflict (file_id, business_type, business_id, custom_path, reason)
SELECT fi.file_id, fi.business_type, fi.business_id, fi.custom_path,
       'PREPARATION_DOCUMENT 的 business_id 同时命中剂型和制剂，或两边都不存在'
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_DOCUMENT'
  AND fi.is_deleted = 0
  AND (
      (EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
       AND EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0))
      OR
      (NOT EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
       AND NOT EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0))
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file_migration_conflict c
      WHERE c.file_id = fi.file_id AND c.business_type = fi.business_type
        AND c.reason = 'PREPARATION_DOCUMENT 的 business_id 同时命中剂型和制剂，或两边都不存在'
  );

-- -----------------------------------
-- 九、审批附件（business_type 为数字节点ID）
-- -----------------------------------

INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'APPROVAL_INSTANCE', fi.business_id, CAST(fi.business_type AS UNSIGNED),
       fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type REGEXP '^[0-9]+$'
  AND fi.is_deleted = 0 AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM approval_instance i WHERE i.id = fi.business_id AND i.is_deleted = 0)
  AND EXISTS (
      SELECT 1 FROM approval_node n
      JOIN approval_instance i2 ON i2.workflow_id = n.workflow_id
      WHERE n.id = CAST(fi.business_type AS UNSIGNED)
        AND i2.id = fi.business_id
        AND n.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'APPROVAL_INSTANCE'
        AND bf.biz_id = fi.business_id
        AND bf.ref_id = CAST(fi.business_type AS UNSIGNED)
        AND bf.file_id = fi.file_id
  );

-- -----------------------------------
-- 十、兜底：仍未匹配到任何规则/父记录不存在的旧文件写入冲突清单
-- -----------------------------------
INSERT INTO business_file_migration_conflict (file_id, business_type, business_id, custom_path, reason)
SELECT fi.file_id, fi.business_type, fi.business_id, fi.custom_path,
       '未匹配到迁移规则或父记录不存在'
FROM file_info fi
WHERE fi.is_deleted = 0
  AND fi.business_type IS NOT NULL
  AND fi.business_id > 0
  AND NOT EXISTS (SELECT 1 FROM business_file bf WHERE bf.file_id = fi.file_id)
  AND NOT EXISTS (
      SELECT 1 FROM business_file_migration_conflict c
      WHERE c.file_id = fi.file_id
        AND c.reason = '未匹配到迁移规则或父记录不存在'
  );

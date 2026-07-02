-- ===================================
-- V5: 车间信息模块 — 补充缺失字段 + 清洁记录表
-- ===================================

-- 1. RoomInfo 主表：补充 5 个周期/规程字段
ALTER TABLE room_info ADD COLUMN disinfection_cycle INT DEFAULT 7 COMMENT '消毒周期(天)';
ALTER TABLE room_info ADD COLUMN disinfection_procedure VARCHAR(255) DEFAULT NULL COMMENT '消毒规程文件名';
ALTER TABLE room_info ADD COLUMN clean_inspection_cycle INT DEFAULT 30 COMMENT '洁净检测周期(天)';
ALTER TABLE room_info ADD COLUMN clean_inspection_procedure VARCHAR(255) DEFAULT NULL COMMENT '洁净检测规程文件名';
ALTER TABLE room_info ADD COLUMN cleaning_cycle INT DEFAULT 7 COMMENT '清洁周期(天)';

-- 2. DisinfectionRecord：补充附件字段
ALTER TABLE disinfection_record ADD COLUMN attachment VARCHAR(500) DEFAULT NULL COMMENT '附件文件名';

-- 3. CleanInspectionRecord：补充下次检测时间、备注
ALTER TABLE clean_inspection_record ADD COLUMN next_inspection_date DATE DEFAULT NULL COMMENT '下次检测时间';
ALTER TABLE clean_inspection_record ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '备注';

-- 4. TemperatureHumidityRecord：补充备注
ALTER TABLE temperature_humidity_record ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '备注';

-- 5. PressureDifferenceRecord：补充备注
ALTER TABLE pressure_difference_record ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '备注';

-- 6. CleaningRecord：新建清洁记录表
CREATE TABLE IF NOT EXISTS cleaning_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    room_id BIGINT NOT NULL COMMENT '关联车间ID',
    cleaning_date DATE NOT NULL COMMENT '清洁日期',
    cleaning_area VARCHAR(255) NOT NULL COMMENT '清洁区域',
    cleaning_method VARCHAR(100) NOT NULL COMMENT '清洁方式',
    cleaning_person VARCHAR(100) NOT NULL COMMENT '清洁执行人',
    cleaning_cycle INT NOT NULL DEFAULT 7 COMMENT '清洁周期(天)',
    next_cleaning_date DATE DEFAULT NULL COMMENT '下次清洁时间',
    attachment VARCHAR(500) DEFAULT NULL COMMENT '附件文件名',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    created_time DATETIME DEFAULT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT NULL COMMENT '更新时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='清洁记录表';

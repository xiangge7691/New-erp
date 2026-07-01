-- 车间信息模块扩展 + 库存单价字段
-- 用于支持车间设计文档中的5个新字段和库存资金占用图表

-- 1. room_info 表新增字段
ALTER TABLE room_info ADD COLUMN production_type VARCHAR(200) COMMENT '生产制剂类型，多选：内服/外用/眼用';
ALTER TABLE room_info ADD COLUMN dust_room TINYINT(1) DEFAULT 0 COMMENT '是否为产尘操作间';
ALTER TABLE room_info ADD COLUMN clean_area TINYINT(1) DEFAULT 0 COMMENT '是否为洁净区';
ALTER TABLE room_info ADD COLUMN clean_grade VARCHAR(10) COMMENT '洁净等级：A级/B级/C级/D级';
ALTER TABLE room_info ADD COLUMN clean_procedure_file_id BIGINT COMMENT '洁净规程文件ID，关联file_info';

-- 2. stock 表新增单价字段（用于库存资金占用环形图计算）
ALTER TABLE stock ADD COLUMN unit_price DECIMAL(12,2) COMMENT '单价';

-- 3. 新建消毒管理记录表
CREATE TABLE IF NOT EXISTS disinfection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL COMMENT '关联房间ID',
    disinfection_date DATE NOT NULL COMMENT '消毒日期',
    disinfection_method VARCHAR(100) NOT NULL COMMENT '消毒方式',
    disinfection_person VARCHAR(50) NOT NULL COMMENT '消毒人',
    disinfection_cycle INT NOT NULL DEFAULT 7 COMMENT '消毒周期（天）',
    next_disinfection_date DATE COMMENT '下次消毒时间',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    INDEX idx_room_id (room_id),
    INDEX idx_next_date (next_disinfection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消毒管理记录表';

-- 4. 新建洁净检测记录表
CREATE TABLE IF NOT EXISTS clean_inspection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL COMMENT '关联房间ID',
    inspection_date DATE NOT NULL COMMENT '检测日期',
    inspection_area VARCHAR(100) NOT NULL COMMENT '检测区域',
    inspection_item VARCHAR(100) NOT NULL COMMENT '检测项目',
    inspection_result VARCHAR(20) NOT NULL COMMENT '检测结果：合格/不合格',
    inspector VARCHAR(50) NOT NULL COMMENT '检测人',
    report_file_id BIGINT COMMENT '检测报告书文件ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洁净检测记录表';

-- 5. 新建温湿度记录表（硬件预留）
CREATE TABLE IF NOT EXISTS temperature_humidity_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL COMMENT '关联房间ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    inspection_area VARCHAR(100) NOT NULL COMMENT '检测区域',
    temperature DECIMAL(5,2) COMMENT '温度（℃）',
    humidity DECIMAL(5,2) COMMENT '湿度（%）',
    recorder VARCHAR(50) COMMENT '记录人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温湿度记录表';

-- 6. 新建压差记录表（硬件预留）
CREATE TABLE IF NOT EXISTS pressure_difference_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL COMMENT '关联房间ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    inspection_area VARCHAR(100) NOT NULL COMMENT '检测区域',
    pressure_value DECIMAL(8,2) COMMENT '压差值（Pa）',
    recorder VARCHAR(50) COMMENT '记录人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压差记录表';

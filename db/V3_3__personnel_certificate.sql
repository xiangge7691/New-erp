-- 人员证书子表
CREATE TABLE IF NOT EXISTS `personnel_certificate` (
    `certificate_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '证书唯一标识',
    `personnel_file_id`  BIGINT       NOT NULL COMMENT '关联人员档案ID',
    `certificate_type`   VARCHAR(50)  DEFAULT NULL COMMENT '证书类型（执业资格/职称证书/健康证/培训证书/其他）',
    `certificate_name`   VARCHAR(200) NOT NULL COMMENT '证书名称',
    `certificate_no`     VARCHAR(100) DEFAULT NULL COMMENT '证书编号',
    `issuing_authority`  VARCHAR(200) DEFAULT NULL COMMENT '发证机构',
    `issue_date`         DATE         DEFAULT NULL COMMENT '发证日期',
    `expiry_date`        DATE         DEFAULT NULL COMMENT '到期日期',
    `certificate_level`  VARCHAR(50)  DEFAULT NULL COMMENT '证书等级/级别',
    `applicable_scope`   VARCHAR(500) DEFAULT NULL COMMENT '适用范围',
    `original_status`    VARCHAR(20)  DEFAULT NULL COMMENT '原件/复印件',
    `reexamine_date`     DATE         DEFAULT NULL COMMENT '复审日期',
    `training_record`    VARCHAR(500) DEFAULT NULL COMMENT '培训记录',
    `file_id`            BIGINT       DEFAULT NULL COMMENT '附件（关联file_info.file_id）',
    `status`             INT          DEFAULT 1 COMMENT '状态：0失效/1有效',
    `remark`             VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_by`         BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `updated_by`         BIGINT       DEFAULT NULL COMMENT '更新人ID',
    `created_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         INT          DEFAULT 0 COMMENT '是否已删除：0否/1是',
    `version`            INT          DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`certificate_id`),
    KEY `idx_personnel_file_id` (`personnel_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员证书子表';

-- 移除人员档案表中的旧证书字段
ALTER TABLE `personnel_file`
    DROP COLUMN `certificate_name`,
    DROP COLUMN `certificate_no`;

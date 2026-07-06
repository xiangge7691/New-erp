-- 机构信息表
CREATE TABLE IF NOT EXISTS `organization` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `license_no` VARCHAR(50) NOT NULL COMMENT '许可证证号（如：湘20260001）',
    `org_name` VARCHAR(200) NOT NULL COMMENT '医疗机构名称',
    `org_category` VARCHAR(20) NOT NULL COMMENT '医疗机构类别（医院/卫生院/诊所/其他）',
    `unified_social_credit_code` VARCHAR(18) NOT NULL COMMENT '统一社会信用代码',
    `practice_license_no` VARCHAR(50) NOT NULL COMMENT '医疗机构执业许可证号',
    `legal_representative` VARCHAR(50) NOT NULL COMMENT '法定代表人',
    `enterprise_leader` VARCHAR(50) NOT NULL COMMENT '企业负责人',
    `prep_room_leader` VARCHAR(50) NOT NULL COMMENT '制剂室负责人',
    `preparation_address` VARCHAR(200) NOT NULL COMMENT '配制地址',
    `preparation_scope` VARCHAR(500) NOT NULL COMMENT '配制范围',
    `issuing_authority` VARCHAR(100) NOT NULL COMMENT '发证机关',
    `issue_date` DATE NOT NULL COMMENT '发证日期',
    `expiry_date` DATE NOT NULL COMMENT '有效期至（自动计算：发证日期+5年）',
    `license_status` VARCHAR(20) NOT NULL DEFAULT '有效' COMMENT '许可证状态（有效/即将到期/已过期/注销/吊销）',
    `remark` VARCHAR(500) COMMENT '备注',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态（1启用/0停用）',
    `is_deleted` INT NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_by` BIGINT COMMENT '创建人',
    `updated_by` BIGINT COMMENT '更新人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='机构信息表';

-- 机构证书子表
CREATE TABLE IF NOT EXISTS `organization_certificate` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `organization_id` BIGINT NOT NULL COMMENT '关联机构ID',
    `certificate_name` VARCHAR(100) NOT NULL COMMENT '证书名称',
    `certificate_no` VARCHAR(50) COMMENT '证书编号',
    `certificate_type` VARCHAR(20) NOT NULL COMMENT '证书类型（机构资质/设备认证/消防许可/环保验收/其他）',
    `issuing_authority` VARCHAR(100) COMMENT '发证机构',
    `issue_date` DATE COMMENT '发证日期',
    `expiry_date` DATE NOT NULL COMMENT '有效期至',
    `remark` VARCHAR(200) COMMENT '备注',
    `is_deleted` INT NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_by` BIGINT COMMENT '创建人',
    `updated_by` BIGINT COMMENT '更新人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_organization_id` (`organization_id`)
) COMMENT='机构证书表';

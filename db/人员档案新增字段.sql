-- 人员档案表新增字段：姓名、所属部门、资格认定、职称、任职简历、培训经历、注册证书、体检时间、健康档案、附件
ALTER TABLE personnel_file
    ADD COLUMN `name`               VARCHAR(50)  DEFAULT NULL COMMENT '姓名' AFTER `employee_no`,
    ADD COLUMN `department_id`      BIGINT       DEFAULT NULL COMMENT '所属部门ID' AFTER `name`,
    ADD COLUMN `qualification`      VARCHAR(255) DEFAULT NULL COMMENT '人员资格认定' AFTER `department_id`,
    ADD COLUMN `title_name`         VARCHAR(100) DEFAULT NULL COMMENT '职称名' AFTER `qualification`,
    ADD COLUMN `title_level`        VARCHAR(50)  DEFAULT NULL COMMENT '职称等级' AFTER `title_name`,
    ADD COLUMN `work_experience`    VARCHAR(500) DEFAULT NULL COMMENT '任职简历' AFTER `entry_date`,
    ADD COLUMN `education_training` VARCHAR(500) DEFAULT NULL COMMENT '教育-培训经历' AFTER `work_experience`,
    ADD COLUMN `certificate_name`   VARCHAR(100) DEFAULT NULL COMMENT '注册证书名' AFTER `education_training`,
    ADD COLUMN `certificate_no`     VARCHAR(50)  DEFAULT NULL COMMENT '注册证书号' AFTER `certificate_name`,
    ADD COLUMN `last_checkup_date`  DATE         DEFAULT NULL COMMENT '上次体检时间' AFTER `health_cert_expire`,
    ADD COLUMN `health_file`        BIGINT       DEFAULT NULL COMMENT '健康档案（关联file_info.file_id）' AFTER `last_checkup_date`,
    ADD COLUMN `attachments`        BIGINT       DEFAULT NULL COMMENT '附件（关联file_info.file_id）' AFTER `health_file`;

-- 添加部门外键约束（如果不存在）
ALTER TABLE personnel_file
    ADD CONSTRAINT `pf_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`);

-- 检验记录表批号改为可空
-- 背景：新增检验记录时批号非必填，原表定义 NOT NULL 无默认值导致插入失败
ALTER TABLE `inspection_record`
MODIFY COLUMN `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批号（选填，可空）';

-- ===================================
-- 物料库存触发器
-- ===================================

-- 当插入新物料时，自动为每个生产单位创建库存基础记录
DELIMITER //

CREATE TRIGGER `tr_material_after_insert`
AFTER INSERT ON `material`
FOR EACH ROW
BEGIN
    -- 遍历所有启用的生产单位，为每个单位创建该物料的库存记录
    INSERT INTO `stock` (
        `prod_unit_id`,
        `item_type`,
        `item_id`,
        `item_code`,
        `item_name`,
        `category_name`,
        `unit_name`,
        `quantity`,
        `stock_status`,
        `is_deleted`,
        `version`,
        `created_by`,
        `updated_by`,
        `created_time`,
        `updated_time`
    )
    SELECT
        `prod_unit_id`,                -- 生产单位ID
        'material',                    -- 物品类型：物料
        NEW.`material_id`,             -- 物料ID
        NEW.`material_code`,           -- 物料编码
        NEW.`material_name`,           -- 物料名称
        NEW.`category_name`,           -- 分类名称
        NEW.`unit_name`,               -- 计量单位
        0,                             -- 初始库存数量为0
        1,                             -- 库存状态：启用
        0,                             -- 未删除
        1,                             -- 版本号
        NEW.`created_by`,              -- 创建人
        NEW.`updated_by`,              -- 更新人
        NOW(),                         -- 创建时间
        NOW()                          -- 更新时间
    FROM `production_unit`
    WHERE `prod_unit_status` = 1       -- 只为启用的生产单位创建
      AND `is_deleted` = 0;            -- 排除已删除的生产单位
END //

DELIMITER ;

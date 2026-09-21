-- 出库明细表新增"来源入库单号"字段
ALTER TABLE stock_out_detail
ADD COLUMN related_in_code VARCHAR(50) DEFAULT NULL
COMMENT '来源入库单号' AFTER stock_id;

-- 回填历史数据：通过 stock_id 关联 stock → stock_in 获取入库单号
UPDATE stock_out_detail sod
INNER JOIN stock s ON sod.stock_id = s.stock_id
INNER JOIN stock_in si ON s.stock_in_id = si.in_id
SET sod.related_in_code = si.in_code
WHERE sod.related_in_code IS NULL AND sod.stock_id IS NOT NULL;

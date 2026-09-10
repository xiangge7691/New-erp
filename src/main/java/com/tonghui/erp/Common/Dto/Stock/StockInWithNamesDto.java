package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockIn;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入库单返回结果数据传输对象
 * <p>
 * 在入库单主表基础上扩展操作人姓名与仓库名，用于验收合格自动入库后
 * 向前端回显入库单时携带可读的名称信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInWithNamesDto extends StockIn {

    /**
     * 操作人姓名（创建人）
     */
    private String createdByName;

    /**
     * 仓库名称（入库生产单位名称）
     */
    private String warehouseName;
}
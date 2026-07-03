package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 入库单包含明细的扩展数据传输对象
 * <p>
 * 在入库单基础上扩展了入库明细列表，用于展示完整的入库单及其明细信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInWithDetailsDto extends StockIn {

    /**
     * 入库明细列表
     */
    private List<StockInDetail> details;
}

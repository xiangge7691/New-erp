package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 库存包含交易记录和出库明细的扩展数据传输对象
 * <p>
 * 在库存基础上扩展了库存交易记录和出库明细列表，用于展示完整的库存变动历史
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockWithDetailsDto extends Stock {

    /**
     * 库存交易记录列表（含绑定的入库单与验收单信息）
     */
    private List<StockTransactionDto> transactions;

    /**
     * 出库明细列表
     */
    private List<StockOutDetail> outDetails;
}

package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 出库单包含明细的扩展数据传输对象
 * <p>
 * 在出库单基础上扩展了出库明细列表，用于展示完整的出库单及其明细信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockOutWithDetailsDto extends StockOut {

    /**
     * 出库明细列表
     */
    private List<StockOutDetail> details;

    /**
     * 本次出库成功的明细条数（接口返回时填充）
     */
    private Integer successCount;
}

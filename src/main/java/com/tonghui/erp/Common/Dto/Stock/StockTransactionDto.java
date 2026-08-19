package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockTransaction;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存流水扩展数据传输对象
 * <p>
 * 在库存交易记录基础上扩展来源入库单与验收单（检验单）的绑定信息，
 * 用于库存查询页面查看流水时回显对应的入库单号与验收单号
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockTransactionDto extends StockTransaction {

    /**
     * 来源入库单ID（仅入库流水有值）
     */
    private Long inId;

    /**
     * 来源入库单号（仅入库流水有值，如 IN202608190001）
     */
    private String inCode;

    /**
     * 来源验收单/检验单号（仅验收合格自动入库的流水有值，如 YS-20260819-001）
     */
    private String acceptanceCode;
}

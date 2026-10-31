package com.tonghui.erp.Common.Dto.ProductionPick;

import com.tonghui.erp.Common.Dto.Stock.AvailableBatchDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产领料明细可用库存批次视图数据传输对象
 * <p>
 * 用于发料前查询每条明细物料可选的合格库存批次（FIFO 排序）
 * </p>
 */
@Data
public class ProductionPickBatchViewDto {

    /**
     * 生产领料明细ID
     */
    private Long detailId;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 请领数量
     */
    private BigDecimal applyQty;

    /**
     * 可用库存批次列表
     */
    private List<AvailableBatchDto> availableBatches;
}

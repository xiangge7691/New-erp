package com.tonghui.erp.Common.Dto.ProductionPick;

import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 生产领料单包含明细的扩展数据传输对象
 * <p>
 * 在主表基础上扩展明细列表与关联出库单状态，用于列表/详情/发起预填接口返回
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionPickWithDetailsDto extends ProductionPick {

    /**
     * 生产领料明细列表
     */
    private List<ProductionPickDetail> details;

    /**
     * 关联出库单状态（查询时实时从 stock_out 回填，如：草稿/已出库/已取消）
     */
    private String stockOutStatus;
}

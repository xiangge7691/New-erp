package com.tonghui.erp.Common.Dto.MaterialReturn;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物料退货管理「重新发货」请求DTO
 * <p>勾选同一验收单的多条已退货明细，逐条输入重新发货数量后合并生成一条新验收单</p>
 */
@Data
public class MaterialResendRequestDto {

    // region 请求字段
    // ===================================
    // 请求字段
    // ===================================

    /**
     * 来源验收单ID（所选明细必须同属一张验收单）
     */
    private Long acceptanceId;

    /**
     * 重新发货明细列表（每条含明细ID与重新发货数量）
     */
    private List<ResendItem> items;

    // endregion

    /**
     * 重新发货明细项
     */
    @Data
    public static class ResendItem {

        /**
         * 验收明细ID（须为该验收单下的已退货明细）
         */
        private Long detailId;

        /**
         * 重新发货数量（默认带出退货数量，必须大于0）
         */
        private BigDecimal resendQty;
    }
}

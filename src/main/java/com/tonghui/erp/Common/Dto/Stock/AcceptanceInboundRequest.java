package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.util.List;

/**
 * 货物验收整单入库请求数据传输对象
 * <p>
 * 用于整单入库接口：为每条「待入库」明细独立指定入库仓库（支持不同仓库）；
 * 未指定的明细回退验收单级默认仓库
 * </p>
 */
@Data
public class AcceptanceInboundRequest {

    // region 请求字段
    // ===================================
    // 请求字段
    // ===================================

    /**
     * 待入库明细的仓库指定列表
     */
    private List<Item> items;

    // endregion

    // region 明细项
    // ===================================
    // 明细项
    // ===================================

    /**
     * 单条待入库明细的仓库指定
     */
    @Data
    public static class Item {

        /**
         * 验收明细ID
         */
        private Long detailId;

        /**
         * 入库仓库（生产单位ID）
         */
        private Long prodUnitId;
    }

    // endregion
}

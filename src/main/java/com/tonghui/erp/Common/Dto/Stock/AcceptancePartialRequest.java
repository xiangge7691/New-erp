package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 部分验收（拆行）请求数据传输对象
 * <p>
 * 用于明细行在初验或检验环节的部分验收：将一行物料拆为
 * 「验收子行 + 退货子行」两条明细，两段数量之和必须等于原行采购数量
 * </p>
 */
@Data
public class AcceptancePartialRequest {

    // region 请求字段
    // ===================================
    // 请求字段
    // ===================================

    /**
     * 验收明细ID（被拆分的原明细行）
     */
    private Long detailId;

    /**
     * 拆分环节（初验 / 检验），对应 AcceptanceStatusPolicy.STAGE_INITIAL / STAGE_QUALITY
     */
    private String stage;

    /**
     * 验收数量（进入验收子行，保留3位小数）
     */
    private BigDecimal acceptQty;

    /**
     * 退货数量（进入退货子行，保留3位小数）
     */
    private BigDecimal returnQty;

    /**
     * 备注说明（追加到验收单备注）
     */
    private String remark;

    // endregion
}

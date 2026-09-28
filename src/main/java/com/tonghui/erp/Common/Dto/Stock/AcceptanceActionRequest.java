package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.util.List;

/**
 * 货物验收操作请求数据传输对象
 * <p>
 * 用于验收行级状态流转接口（逐行初验/逐行检验）的请求参数；
 * detailIds 为空时表示自动推进该单全部符合前置状态的明细行（批量推进）
 * </p>
 */
@Data
public class AcceptanceActionRequest {

    // region 请求字段
    // ===================================
    // 请求字段
    // ===================================

    /**
     * 是否合格（true-合格，false-不合格）
     */
    private Boolean pass;

    /**
     * 目标明细行ID列表（勾选的行），空/null表示全部待初验/待检验行
     */
    private List<Long> detailIds;

    /**
     * 备注说明（追加到验收单备注）
     */
    private String remark;

    // endregion
}

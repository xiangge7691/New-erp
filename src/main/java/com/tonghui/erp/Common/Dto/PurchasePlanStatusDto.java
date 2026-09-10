package com.tonghui.erp.Common.Dto;

import lombok.Data;

/**
 * 采购计划状态更新请求 DTO
 * <p>
 * 用于封装修改采购计划状态接口（PUT /api/purchase-plan/{id}/status）的请求参数。
 * 支持同时传递目标状态、审批意见等多个字段，便于后续扩展其他状态相关参数。
 * </p>
 */
@Data
public class PurchasePlanStatusDto {

    // region 状态更新字段
    // ===================================
    // 状态更新字段
    // ===================================

    /**
     * 目标状态（草稿/待审批/已审批/已驳回），必填。
     */
    private String status;

    /**
     * 审批意见（可选），修改状态时填写的审批备注，如驳回原因、审批说明等。
     */
    private String approvalOpinion;

    // endregion
}

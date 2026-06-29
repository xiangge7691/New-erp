package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 审批统计DTO
 */
@Data
public class ApprovalStatsDto {
    /**
     * 待审批总数
     */
    private long pendingApproval;

    /**
     * 我的待审批数量
     */
    private long myPending;
}

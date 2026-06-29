package com.tonghui.erp.Common.Dto.Approval;

import lombok.Data;

/**
 * 作废审批实例请求 DTO
 */
@Data
public class CancelRequest {
    /**
     * 作废人用户 ID
     */
    private Long userId;
    
    /**
     * 作废原因
     */
    private String cancelReason;
}

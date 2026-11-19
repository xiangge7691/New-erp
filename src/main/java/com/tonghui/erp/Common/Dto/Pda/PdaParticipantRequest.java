package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

/**
 * PDA 工序参与人请求
 */
@Data
public class PdaParticipantRequest {

    /** 人员档案ID */
    private Long personnelId;

    /** 个人完成数量 */
    private java.math.BigDecimal completedQty;

    /** 是否主操作：0否/1是 */
    private Integer isPrimary;
}

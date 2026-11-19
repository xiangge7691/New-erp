package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;

/**
 * PDA 工序参与人
 */
@Data
public class PdaParticipantDto {

    /** 人员档案ID */
    private Long personnelId;

    /** 人员姓名 */
    private String personnelName;

    /** 个人完成数量 */
    private BigDecimal completedQty;

    /** 是否主操作：0否/1是 */
    private Integer isPrimary;
}

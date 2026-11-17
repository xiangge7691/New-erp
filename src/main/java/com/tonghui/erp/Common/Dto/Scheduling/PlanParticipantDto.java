package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

/**
 * 计划参与人
 */
@Data
public class PlanParticipantDto {

    /** 人员档案ID */
    private Long personnelId;

    /** 人员姓名 */
    private String personnelName;

    /** 是否主操作 */
    private Integer isPrimary;
}

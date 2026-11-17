package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 保存排产的工序行请求
 */
@Data
public class ProcessPlanSaveDto {

    /** 计划行ID，新增时为空 */
    private Long planId;

    /** 工序类型ID */
    private Long processTypeId;

    /** 工序顺序 */
    private Integer stepOrder;

    /** 制剂工序模板ID */
    private Long templateId;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 计划房间ID */
    private Integer roomId;

    /** 计划设备ID */
    private Integer equipmentId;

    /** 计划班组ID */
    private Long teamId;

    /** 备注 */
    private String remark;

    /** 批次 */
    private List<PlanBatchDto> batches;

    /** 参与人 */
    private List<PlanParticipantDto> participants;
}

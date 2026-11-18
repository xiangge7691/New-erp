package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序排产行（计划 + 模板 + 实际）
 */
@Data
public class ProcessScheduleDto {

    /** 排产计划行ID，未落库时为空 */
    private Long planId;

    /** 工单ID */
    private Long workOrderId;

    /** 工序类型ID */
    private Long processTypeId;

    /** 工序名称 */
    private String processName;

    /** 工序顺序 */
    private Integer stepOrder;

    /** 制剂工序模板ID */
    private Long templateId;

    /** 模板标准工时（分钟） */
    private Integer standardDuration;

    /** 模板标准数量 */
    private BigDecimal standardQty;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 计划用时（分钟） */
    private Integer planDurationMinutes;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 计划房间ID */
    private Integer roomId;

    /** 计划房间名称 */
    private String roomName;

    /** 计划设备ID */
    private Integer equipmentId;

    /** 计划设备名称 */
    private String equipmentName;

    /** 计划班组ID */
    private Long teamId;

    /** 计划班组名称 */
    private String teamName;

    /** 计划状态：草稿/已保存/需重排 */
    private String status;

    /** 是否已排产：计划起止均已填写（为空即未排产，不影响流程） */
    private Boolean scheduled;

    /** 备注 */
    private String remark;

    /** 批次列表 */
    private List<PlanBatchDto> batches;

    /** 计划参与人 */
    private List<PlanParticipantDto> participants;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    private LocalDateTime actualEndTime;

    /** 实际状态 */
    private String actualStatus;

    /** 实际数量 */
    private BigDecimal actualQty;

    /** 实际执行记录ID */
    private Long executionId;

    /** 当前行冲突 */
    private List<ScheduleConflictDto> conflicts;
}

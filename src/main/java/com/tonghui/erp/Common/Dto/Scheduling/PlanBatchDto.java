package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排产批次
 */
@Data
public class PlanBatchDto {

    /** 批次ID */
    private Long id;

    /** 批次序号 */
    private Integer batchNo;

    /** 批次名称 */
    private String batchName;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 计划用时（分钟） */
    private Integer planDurationMinutes;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 计量单位ID */
    private Long unitId;

    /** 计量单位名称（如：千克） */
    private String unitName;

    /** 计量单位符号（如：kg） */
    private String unitSymbol;

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

    /** 状态 */
    private String status;

    /** 是否已排产：1是/0否（保存时由前端传参，后端不自动计算） */
    private Boolean scheduled;

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

    /** 是否已完工锁定：0否/1是 */
    private Integer locked;

    /** 参与人 */
    private List<PlanParticipantDto> participants;
}

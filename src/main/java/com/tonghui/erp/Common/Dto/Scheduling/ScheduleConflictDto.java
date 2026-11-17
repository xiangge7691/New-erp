package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排产冲突项
 */
@Data
public class ScheduleConflictDto {

    /** 冲突类型：设备占用/房间占用/人员班次/工序顺序/批次重叠 */
    private String type;

    /** 当前工单ID */
    private Long workOrderId;

    /** 当前工单编号 */
    private String workOrderCode;

    /** 计划行ID */
    private Long planId;

    /** 批次ID */
    private Long batchId;

    /** 工序顺序 */
    private Integer stepOrder;

    /** 工序名称 */
    private String processName;

    /** 冲突开始时间 */
    private LocalDateTime startTime;

    /** 冲突结束时间 */
    private LocalDateTime endTime;

    /** 冲突描述 */
    private String message;

    /** 冲突对方工单ID */
    private Long otherWorkOrderId;

    /** 冲突对方工单编号 */
    private String otherWorkOrderCode;

    /** 推荐开始时间 */
    private LocalDateTime recommendedStartTime;

    /** 推荐结束时间 */
    private LocalDateTime recommendedEndTime;
}

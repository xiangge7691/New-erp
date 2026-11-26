package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个生产任务的排产详情
 */
@Data
public class ScheduleDetailDto {

    /** 工单ID */
    private Long workOrderId;

    /** 工单编号 */
    private String workOrderCode;

    /** 工单名称 */
    private String workOrderName;

    /** 制剂名称 */
    private String preparationName;

    /** 生产计划ID */
    private Long planId;

    /** 生产计划名称 */
    private String planName;

    /** 生产计划编号 */
    private String planNumber;

    /** 批号 */
    private String batchNumber;

    /** 批量 */
    private BigDecimal batchQty;

    /** 工单状态 */
    private String currentStatus;

    /** 生产状态（按执行记录推导：待生产/生产中/已完工） */
    private String productionStatus;

    /** 配置日期 */
    private LocalDateTime configDate;

    /** 配置完成时间（为 null 表示任务尚未配置完成） */
    private LocalDateTime configCompleteTime;

    /** 任务计划开始时间 */
    private LocalDateTime planStartTime;

    /** 工序计划 */
    private List<ProcessScheduleDto> processes;

    /** 当前冲突 */
    private List<ScheduleConflictDto> conflicts;
}

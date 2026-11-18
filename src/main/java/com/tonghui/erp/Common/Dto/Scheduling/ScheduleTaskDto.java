package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 排产任务列表项
 */
@Data
public class ScheduleTaskDto {

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

    /** 生产用时（小时，已完工执行记录时长合计） */
    private BigDecimal productionDurationHours;

    /** 配置日期 */
    private LocalDateTime configDate;

    /** 任务计划开始时间 */
    private LocalDateTime planStartTime;

    /** 工序数 */
    private Integer processCount;

    /** 已排产工序数（计划起止均已填写） */
    private Integer scheduledProcessCount;

    /** 冲突数 */
    private Integer conflictCount;

    /** 是否已保存排产 */
    private Boolean saved;
}

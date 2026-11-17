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

    /** 制剂名称 */
    private String preparationName;

    /** 批号 */
    private String batchNumber;

    /** 批量 */
    private BigDecimal batchQty;

    /** 工单状态 */
    private String currentStatus;

    /** 任务计划开始时间 */
    private LocalDateTime planStartTime;

    /** 工序计划 */
    private List<ProcessScheduleDto> processes;

    /** 当前冲突 */
    private List<ScheduleConflictDto> conflicts;
}

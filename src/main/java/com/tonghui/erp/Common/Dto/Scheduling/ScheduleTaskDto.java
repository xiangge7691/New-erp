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

    /** 工序数 */
    private Integer processCount;

    /** 冲突数 */
    private Integer conflictCount;

    /** 是否已保存排产 */
    private Boolean saved;
}

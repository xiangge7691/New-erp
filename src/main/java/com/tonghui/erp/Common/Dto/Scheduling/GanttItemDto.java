package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 甘特图条目
 */
@Data
public class GanttItemDto {

    /** 视图：task/equipment/room */
    private String view;

    /** 分组键（任务编号、设备名称或房间名称） */
    private String groupKey;

    /** 工单ID */
    private Long workOrderId;

    /** 工单编号 */
    private String workOrderCode;

    /** 工序名称 */
    private String processName;

    /** 工序顺序 */
    private Integer stepOrder;

    /** 批次序号 */
    private Integer batchNo;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 设备名称 */
    private String equipmentName;

    /** 房间名称 */
    private String roomName;

    /** 是否冲突 */
    private Boolean conflict;
}

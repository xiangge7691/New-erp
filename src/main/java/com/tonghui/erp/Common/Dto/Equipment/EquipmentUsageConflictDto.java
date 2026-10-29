package com.tonghui.erp.Common.Dto.Equipment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备使用时间冲突明细
 * <p>
 * 保存工序执行记录时，用于描述"同一设备在目标时间段内已被占用"的既有记录，
 * 便于向前端返回可读的冲突信息（工单、批号、工序、时间段）。
 * </p>
 */
@Data
public class EquipmentUsageConflictDto {

    // region 既有占用记录信息
    // ===================================
    // 既有占用记录信息
    // ===================================

    /**
     * 既有工序执行记录ID
     */
    private Long recordId;

    /**
     * 设备ID
     */
    private Integer equipmentId;

    /**
     * 设备名称
     */
    private String equipmentName;

    /**
     * 固定资产编号
     */
    private String fixedAssetCode;

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 工单编号
     */
    private String workOrderCode;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 工序名称
     */
    private String processName;

    /**
     * 占用开始时间
     */
    private LocalDateTime startTime;

    /**
     * 占用结束时间
     */
    private LocalDateTime endTime;

    // endregion
}

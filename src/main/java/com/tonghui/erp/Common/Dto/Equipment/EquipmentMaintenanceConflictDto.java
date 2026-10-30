package com.tonghui.erp.Common.Dto.Equipment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备维保时间冲突明细
 * <p>
 * 保存工序执行记录时，用于描述设备使用时间与设备维保时间区间
 * （equipment_maintenance.maintenance_start_time ~ maintenance_end_time）重叠的记录。
 * </p>
 */
@Data
public class EquipmentMaintenanceConflictDto {

    // region 维保记录信息
    // ===================================
    // 维保记录信息
    // ===================================

    /**
     * 维保记录ID
     */
    private Long maintenanceId;

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
     * 维保类型（日常保养/定期检修/故障维修）
     */
    private String maintenanceType;

    /**
     * 维保开始时间
     */
    private LocalDateTime maintenanceStartTime;

    /**
     * 维保结束时间
     */
    private LocalDateTime maintenanceEndTime;

    /**
     * 维保内容
     */
    private String maintenanceContent;

    // endregion
}

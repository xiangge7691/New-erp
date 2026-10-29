package com.tonghui.erp.Common.Dto.Equipment;

import lombok.Data;

import java.time.LocalDate;

/**
 * 设备维保时间冲突明细
 * <p>
 * 保存工序执行记录时，用于描述设备使用日期与设备维保日期（equipment_maintenance.maintenance_date）
 * 重叠的记录。由于维保表仅记录日期，按"维保当天整日不可用"处理。
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
     * 维保日期
     */
    private LocalDate maintenanceDate;

    /**
     * 维保内容
     */
    private String maintenanceContent;

    // endregion
}

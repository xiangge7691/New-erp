package com.tonghui.erp.Common.Dto.Equipment;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备维保记录导出数据传输对象
 * <p>
 * 用于"设备使用记录"导出时，按设备附带的维保记录明细（维保时间段、类型、内容等），
 * 便于对照查看设备在什么时间处于维保状态。
 * </p>
 */
@Data
public class EquipmentMaintenanceRecordDto {

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
     * 维保时长（分钟，由开始/结束时间计算）
     */
    private Long durationMinutes;

    /**
     * 维保内容
     */
    private String maintenanceContent;

    /**
     * 维保结果
     */
    private String maintenanceResult;

    /**
     * 维保人员
     */
    private String maintainer;

    /**
     * 维保公司
     */
    private String maintenanceCompany;

    /**
     * 下次维保日期
     */
    private LocalDate nextMaintenanceDate;

    // endregion
}

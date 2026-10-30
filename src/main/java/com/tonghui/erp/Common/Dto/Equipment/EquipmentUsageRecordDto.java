package com.tonghui.erp.Common.Dto.Equipment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备使用记录数据传输对象
 * <p>
 * 由工单工序执行记录（work_order_process_execution）关联设备、工单、工序类型、房间后组装，
 * 用于"设备使用记录"列表展示与 Excel 导出（按设备导出）。
 * </p>
 */
@Data
public class EquipmentUsageRecordDto {

    // region 设备信息
    // ===================================
    // 设备信息
    // ===================================

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
     * 设备型号
     */
    private String equipmentModel;

    /**
     * 设备类型（生产设备/检验设备/环境设备/计量器具/压力容器/其他设备）
     */
    private String equipmentType;

    /**
     * 所在房间名称
     */
    private String roomName;

    // endregion

    // region 使用记录信息
    // ===================================
    // 使用记录信息
    // ===================================

    /**
     * 工序执行记录ID（work_order_process_execution.id）
     */
    private Long recordId;

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 工单编号
     */
    private String workOrderCode;

    /**
     * 生产计划名称
     */
    private String planName;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 工序类型ID
     */
    private Long processTypeId;

    /**
     * 工序名称
     */
    private String processName;

    /**
     * 工序编码
     */
    private String processCode;

    /**
     * 工序顺序
     */
    private Integer stepOrder;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 使用开始时间
     */
    private LocalDateTime startTime;

    /**
     * 使用结束时间
     */
    private LocalDateTime endTime;

    /**
     * 使用时长（分钟，由开始/结束时间计算）
     */
    private Long durationMinutes;

    /**
     * 执行状态（仅两种：待执行/已完成）
     * <p>
     * 不取库中 status（该列历史值恒为"待执行"），按使用时间推导：
     * 未填写开始/结束时间=待执行，已填写=已完成。
     * </p>
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    // endregion
}

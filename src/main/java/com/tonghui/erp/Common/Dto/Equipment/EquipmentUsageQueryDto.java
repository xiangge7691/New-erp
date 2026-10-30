package com.tonghui.erp.Common.Dto.Equipment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备使用记录查询条件
 * <p>
 * 用于"设备使用记录"分页查询与 Excel 导出，时间范围按"与使用区间有交集"过滤。
 * </p>
 */
@Data
public class EquipmentUsageQueryDto {

    // region 查询条件
    // ===================================
    // 查询条件
    // ===================================

    /**
     * 设备ID（可选，按设备导出时必填）
     */
    private Integer equipmentId;

    /**
     * 设备类型（可选，精确匹配）
     */
    private String equipmentType;

    /**
     * 关键字（可选，模糊匹配工单编号/批号/制剂名称/工序名称/设备名称/固定资产编号）
     */
    private String keyword;

    /**
     * 执行状态（可选，精确匹配；与列表展示口径一致：
     * 库中明确状态优先，否则按使用时间推导 待执行/执行中/已完成）
     */
    private String status;

    /**
     * 使用开始时间下界（含，由 startDate 解析）
     */
    private LocalDateTime startTime;

    /**
     * 使用结束时间上界（不含，由 endDate + 1 天解析）
     */
    private LocalDateTime endTime;

    // endregion
}

package com.tonghui.erp.Common.Dto.Quality;

import lombok.Data;

/**
 * 偏差记录关联生产任务下拉选项数据传输对象
 * <p>
 * 用于偏差登记时生产任务选择下拉框，仅返回选择所需的精简字段
 * </p>
 */
@Data
public class WorkOrderOptionDto {

    // region 下拉选项字段
    // ===================================
    // 下拉选项字段
    // ===================================

    /**
     * 生产任务ID（保存时回传）
     */
    private Long workOrderId;

    /**
     * 生产任务编号
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

    // endregion
}

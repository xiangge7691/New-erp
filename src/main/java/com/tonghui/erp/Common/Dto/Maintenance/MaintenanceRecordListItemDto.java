package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 养护记录列表项数据传输对象
 * <p>
 * 列表页展示一行养护记录单：编号、检查时间、仓库、检查类型、检查人员、
 * 明细数、异常明细数（异常数>0 时前端标红提示）及关联复查编号
 * </p>
 */
@Data
public class MaintenanceRecordListItemDto {

    /**
     * 养护记录唯一标识
     */
    private Long id;

    /**
     * 养护记录编号（YH-YYYYMMDD-NNN）
     */
    private String maintenanceCode;

    /**
     * 实际检查时间
     */
    private LocalDateTime checkTime;

    /**
     * 检查类型
     */
    private String checkType;

    /**
     * 仓库名称（快照）
     */
    private String warehouseName;

    /**
     * 检查人员姓名（快照）
     */
    private String checkerName;

    /**
     * 检查明细数
     */
    private int detailCount;

    /**
     * 异常明细数（任一检查项为发现异常或填写了异常描述的明细数）
     */
    private int abnormalCount;

    /**
     * 关联复查记录编号（快照）
     */
    private String relatedMaintenanceCode;

    /**
     * 创建人姓名（登记人）
     */
    private String createdByName;

    /**
     * 创建时间（登记时间）
     */
    private LocalDateTime createdTime;

    /**
     * 最后更新人姓名
     */
    private String updatedByName;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedTime;
}

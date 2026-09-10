package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点单列表项DTO
 * <p>
 * 盘点记录列表展示用，包含盘点单主表关键信息
 * </p>
 */
@Data
public class CheckOrderListItemDto {

    /**
     * 盘点单ID
     */
    private Long id;

    /**
     * 盘点单号
     */
    private String checkNo;

    /**
     * 盘点仓库名称
     */
    private String warehouse;

    /**
     * 物料种数（盘点条目数）
     */
    private Integer materialCount;

    /**
     * 盘盈条目数
     */
    private Integer profitCount;

    /**
     * 盘亏条目数
     */
    private Integer lossCount;

    /**
     * 盘平条目数
     */
    private Integer matchCount;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime createdAt;
}
package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 盘点单详情DTO
 * <p>
 * 盘点单主表信息 + 盘点明细列表，用于盘点单详情展示
 * </p>
 */
@Data
public class CheckOrderDetailDto {

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

    /**
     * 盘点明细列表
     */
    private List<CheckOrderDetailItemDto> items;
}
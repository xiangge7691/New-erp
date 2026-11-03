package com.tonghui.erp.Common.Dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关联单据通用传输对象
 * <p>
 * 用于采购流程节点跳转和“采购/计划/生产/检验/入库”五个子页面展示。
 * type 标识单据类型，id/code 用于定位单据，detailPath 为后端详情接口相对路径。
 * </p>
 */
@Data
public class RelatedDocumentDto {

    // region 单据标识
    // ===================================
    // 单据标识
    // ===================================

    /** 单据类型：PURCHASE_PLAN/PURCHASE_ORDER/ACCEPTANCE/STOCK_IN/PRODUCTION_PLAN/WORK_ORDER/INSPECTION_REQUEST/INSPECTION_RECORD/PLAN_STATUS_LOG */
    private String type;

    /** 单据主键 */
    private Long id;

    /** 单据编号 */
    private String code;

    /** 单据标题或名称 */
    private String title;

    /** 单据状态 */
    private String status;

    /** 单据关键时间 */
    private LocalDateTime time;

    /** 详情接口相对路径 */
    private String detailPath;

    /** 所属子页面：PURCHASE/PLAN/PRODUCTION/INSPECTION/INBOUND */
    private String category;

    // endregion
}

package com.tonghui.erp.Common.Dto;

import lombok.Data;

import java.util.List;

/**
 * 生产计划关联单据分组传输对象
 * <p>
 * 按前端“采购、计划、生产、检验、入库”五个子页面分组返回关联单据，
 * 每组内的单据均可通过 detailPath 打开详情。
 * </p>
 */
@Data
public class ProductionPlanRelatedDocumentsDto {

    // region 分组单据
    // ===================================
    // 分组单据
    // ===================================

    /** 采购：采购计划、采购订单、验收单 */
    private List<RelatedDocumentDto> purchase;

    /** 计划：生产计划、计划状态流水 */
    private List<RelatedDocumentDto> plan;

    /** 生产：生产任务（工单） */
    private List<RelatedDocumentDto> production;

    /** 检验：请检记录、检验记录 */
    private List<RelatedDocumentDto> inspection;

    /** 入库：采购入库单、成品入库单 */
    private List<RelatedDocumentDto> inbound;

    // endregion
}

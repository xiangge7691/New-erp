package com.tonghui.erp.Common.Dto.MaterialReturn;

import lombok.Data;

/**
 * 物料退货管理列表查询DTO
 * <p>筛选条件：状态、退货原因、日期、供应商、关键字（物料名称/编码/验收单号）</p>
 */
@Data
public class MaterialReturnQueryDto {

    // region 筛选字段
    // ===================================
    // 筛选字段
    // ===================================

    /**
     * 明细状态（待退货/已退货/已重发/已取消），空则查询全部退货相关状态
     */
    private String status;

    /**
     * 退货原因（初验不合格/检验不合格），空则不限
     */
    private String returnReason;

    /**
     * 供应商，空则不限
     */
    private String supplier;

    /**
     * 关键字（对物料名称、物料编码、验收单号模糊匹配），空则不限
     */
    private String keyword;

    /**
     * 开始日期（按明细最近更新时间过滤，yyyy-MM-dd），空则不限
     */
    private String startDate;

    /**
     * 结束日期（按明细最近更新时间过滤，yyyy-MM-dd），空则不限
     */
    private String endDate;

    // endregion

    // region 分页字段
    // ===================================
    // 分页字段
    // ===================================

    /**
     * 页码，从0开始
     */
    private int pageIndex = 0;

    /**
     * 每页数量
     */
    private int pageSize = 20;

    // endregion
}

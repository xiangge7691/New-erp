package com.tonghui.erp.Common.Dto.Quality;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销毁记录明细保存请求数据传输对象
 * <p>
 * 用于新增/修改销毁记录时的明细行请求体
 * </p>
 */
@Data
public class DestructionRecordDetailSaveDto {

    // region 明细字段
    // ===================================
    // 明细字段
    // ===================================

    /**
     * 品名/物料名称（必填，最长100字符）
     */
    private String materialName;

    /**
     * 规格（可选，最长100字符）
     */
    private String spec;

    /**
     * 批号（可选，最长50字符）
     */
    private String batchNo;

    /**
     * 数量（可选，默认0，服务端按明细求和写入主表合计）
     */
    private BigDecimal quantity;

    /**
     * 关联留样记录ID（可空）
     * <p>
     * 留样及稳定性考察产品销毁类型：会反写留样状态为已销毁并写入销毁日期；
     * 不合格品销毁类型：仅作追溯引用，不联动留样状态
     * </p>
     */
    private Long retainedSampleId;

    // endregion
}

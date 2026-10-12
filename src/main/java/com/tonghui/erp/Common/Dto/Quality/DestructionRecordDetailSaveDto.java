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
     * 关联留样记录ID（可空，仅作选人回填与追溯引用，保存销毁记录不联动留样状态）
     */
    private Long retainedSampleId;

    // endregion
}

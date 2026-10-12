package com.tonghui.erp.Common.Dto.Quality;

import lombok.Data;

/**
 * 销毁记录关联留样下拉选项数据传输对象
 * <p>
 * 用于销毁登记（留样及稳定性考察产品销毁）时留样记录选择下拉框，
 * 仅返回状态为"留样中"的记录，选择后回填明细行的品名/规格/批号并记录留样ID引用
 * </p>
 */
@Data
public class RetainedSampleOptionDto {

    // region 下拉选项字段
    // ===================================
    // 下拉选项字段
    // ===================================

    /**
     * 留样记录ID（保存明细时回传 retainedSampleId）
     */
    private Long id;

    /**
     * 留样编号
     */
    private String retainedCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 批号
     */
    private String batchNo;

    /**
     * 留样数量
     */
    private String retainedQuantity;

    /**
     * 存放位置
     */
    private String storageLocation;

    // endregion
}

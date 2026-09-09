package com.tonghui.erp.Common.Dto.MaterialRequisitionSlip;

import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 物料领料单包含明细的扩展数据传输对象
 * <p>
 * 在领料单主表基础上扩展了明细列表与关联验收单号，用于展示完整的领料单及其明细信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialRequisitionSlipWithDetailsDto extends MaterialRequisitionSlip {

    /**
     * 领料明细列表
     */
    private List<MaterialRequisitionSlipDetail> details;

    /**
     * 关联验收单号（查询时从acceptance_order表回填）
     */
    @TableField(exist = false)
    private String acceptanceCode;
}

package com.tonghui.erp.Common.Dto.Material;

import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 物料包含配方和采购明细的扩展数据传输对象
 * <p>
 * 在物料基础上扩展了关联的配方和采购订单明细列表，用于展示完整的物料详情及业务关联
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialWithDetailsDto extends Material {

    /**
     * 该物料关联的配方列表
     */
    private List<PreparationFormula> formulas;

    /**
     * 该物料关联的采购订单明细列表
     */
    private List<PurchaseOrderItems> items;
}

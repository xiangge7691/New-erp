package com.tonghui.erp.Common.Dto.Material;

import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialWithDetailsDto extends Material {
    private List<PreparationFormula> formulas;
    private List<PurchaseOrderItems> items;
}

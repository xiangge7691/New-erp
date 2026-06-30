package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionPlanWithRecordsDto extends ProductionPlan {
    private List<ProductionProcessRecord> records;
}

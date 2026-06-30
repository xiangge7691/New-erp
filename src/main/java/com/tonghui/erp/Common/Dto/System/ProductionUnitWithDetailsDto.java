package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.Entity.ProdUnitMaterialFile;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionUnitWithDetailsDto extends ProductionUnit {
    private List<ProdUnitInvoice> invoices;
    private List<ProdUnitMaterialFile> materialFiles;
}

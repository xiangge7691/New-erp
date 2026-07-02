package com.tonghui.erp.Common.Dto.Purchase;

import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;
import com.tonghui.erp.Data.Entity.StockIn;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseSuppliersWithDetailsDto extends PurchaseSuppliers {
    private List<PurchaseOrders> orders;
    private List<StockIn> stockIns;
}

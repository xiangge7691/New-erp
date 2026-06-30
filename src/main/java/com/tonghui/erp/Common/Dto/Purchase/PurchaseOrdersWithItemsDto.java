package com.tonghui.erp.Common.Dto.Purchase;

import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrdersWithItemsDto extends PurchaseOrders {
    private List<PurchaseOrderItems> items;
}

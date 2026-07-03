package com.tonghui.erp.Common.Dto.Purchase;

import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 采购订单包含明细项的扩展数据传输对象
 * <p>
 * 在采购订单基础上扩展了采购明细列表，用于展示完整的采购订单及其明细信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrdersWithItemsDto extends PurchaseOrders {

    /**
     * 采购订单明细列表
     */
    private List<PurchaseOrderItems> items;
}

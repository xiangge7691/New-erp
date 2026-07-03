package com.tonghui.erp.Common.Dto.Purchase;

import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;
import com.tonghui.erp.Data.Entity.StockIn;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 供应商包含关联订单和入库单的扩展数据传输对象
 * <p>
 * 在供应商基础上扩展了关联的采购订单和入库单列表，用于展示完整的供应商详情及业务关联
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseSuppliersWithDetailsDto extends PurchaseSuppliers {

    /**
     * 该供应商的采购订单列表
     */
    private List<PurchaseOrders> orders;

    /**
     * 该供应商的入库单列表
     */
    private List<StockIn> stockIns;
}

package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;

import java.util.List;

/**
 * 采购订单明细表业务接口
 */
public interface PurchaseOrderItemsService extends IService<PurchaseOrderItems> {

    // #region 基础操作

    /**
     * 新增采购订单明细
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return 是否成功
     */
    boolean addPurchaseOrderItem(PurchaseOrderItems purchaseOrderItems);

    /**
     * 更新采购订单明细
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return 是否成功
     */
    boolean updatePurchaseOrderItem(PurchaseOrderItems purchaseOrderItems);

    /**
     * 删除采购订单明细
     *
     * @param itemId 采购订单明细ID
     * @return 是否成功
     */
    boolean deletePurchaseOrderItem(Long itemId);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询采购订单明细
     *
     * @param itemId 采购订单明细ID
     * @return 采购订单明细实体
     */
    PurchaseOrderItems getPurchaseOrderItemById(Long itemId);

    /**
     * 根据采购订单ID查询所有明细
     *
     * @param orderId 采购订单ID
     * @return 采购订单明细集合
     */
    List<PurchaseOrderItems> getPurchaseOrderItemsByOrderId(Long orderId);

    // #endregion
}

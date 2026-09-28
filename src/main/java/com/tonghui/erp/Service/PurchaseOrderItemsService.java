package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;

import java.util.List;

/**
 * 采购订单明细表业务接口
 */
public interface PurchaseOrderItemsService extends IService<PurchaseOrderItems> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

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
     * @param purchaseOrderItems 采购订单明细实体，包含要更新的字段信息
     * @return 是否成功
     */
    boolean updatePurchaseOrderItem(PurchaseOrderItems purchaseOrderItems);

    /**
     * 批量更新采购订单明细（用于原型"批量写入统一发票号/统一供应商"）
     *
     * @param items 采购订单明细列表（每条必须包含 id，可仅携带需修改字段）
     * @return 是否成功
     */
    boolean batchUpdatePurchaseOrderItems(List<PurchaseOrderItems> items);

    /**
     * 删除采购订单明细
     *
     * @param itemId 采购订单明细ID
     * @return 是否成功
     */
    boolean deletePurchaseOrderItem(Long itemId);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

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

    // endregion
}

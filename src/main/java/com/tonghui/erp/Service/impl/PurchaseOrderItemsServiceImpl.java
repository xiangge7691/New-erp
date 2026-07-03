package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Service.PurchaseOrderItemsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 采购订单明细服务实现类
 * <p>
 * 实现PurchaseOrderItemsService接口，提供采购订单明细相关的业务逻辑处理，
 * 包括明细的增删改查等功能的具体实现
 * </p>
 *
 */
@Service
public class PurchaseOrderItemsServiceImpl extends ServiceImpl<PurchaseOrderItemsMapper, PurchaseOrderItems>
        implements PurchaseOrderItemsService {

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增采购订单明细
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean addPurchaseOrderItem(PurchaseOrderItems purchaseOrderItems) {
        return this.save(purchaseOrderItems);
    }

    /**
     * 更新采购订单明细
     *
     * @param purchaseOrderItems 采购订单明细实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updatePurchaseOrderItem(PurchaseOrderItems purchaseOrderItems) {
        return this.updateById(purchaseOrderItems);
    }

    /**
     * 删除采购订单明细
     *
     * @param itemId 采购订单明细ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deletePurchaseOrderItem(Long itemId) {
        return this.removeById(itemId);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询采购订单明细
     *
     * @param itemId 采购订单明细ID
     * @return 采购订单明细实体，不存在则返回null
     */
    @Override
    public PurchaseOrderItems getPurchaseOrderItemById(Long itemId) {
        return this.getById(itemId);
    }

    /**
     * 根据订单ID查询所有关联的采购订单明细
     *
     * @param orderId 采购订单ID
     * @return 该订单下所有明细的集合
     */
    @Override
    public List<PurchaseOrderItems> getPurchaseOrderItemsByOrderId(Long orderId) {
        QueryWrapper<PurchaseOrderItems> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return this.list(wrapper);
    }

    // endregion
}

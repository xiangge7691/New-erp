package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Service.PurchaseOrderItemsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
     * <p>金额自动按 实际到货数量 × 单价 计算（未填实际到货数量时回退采购数量）</p>
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean addPurchaseOrderItem(PurchaseOrderItems purchaseOrderItems) {
        calculateAmount(purchaseOrderItems);
        return this.save(purchaseOrderItems);
    }

    /**
     * 更新采购订单明细
     * <p>金额自动按 实际到货数量 × 单价 计算（未填实际到货数量时回退采购数量）</p>
     *
     * @param purchaseOrderItems 采购订单明细实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updatePurchaseOrderItem(PurchaseOrderItems purchaseOrderItems) {
        calculateAmount(purchaseOrderItems);
        return this.updateById(purchaseOrderItems);
    }

    /**
     * 批量更新采购订单明细
     * <p>逐条校验存在性并复用单条更新逻辑；金额规则与单条更新一致（amount 为 null 时按 实际到货数量 × 单价 计算）</p>
     *
     * @param items 采购订单明细列表（每条必须包含 id）
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean batchUpdatePurchaseOrderItems(List<PurchaseOrderItems> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("明细列表不能为空");
        }
        for (PurchaseOrderItems item : items) {
            if (item == null || item.getId() == null) {
                throw new RuntimeException("明细ID不能为空");
            }
            PurchaseOrderItems existing = this.getById(item.getId());
            if (existing == null) {
                throw new RuntimeException("采购订单明细不存在(ID=" + item.getId() + ")");
            }
            calculateAmount(item, existing);
            this.updateById(item);
        }
        return true;
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

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 计算明细金额（实际到货数量 × 单价，未填实际到货数量时回退采购数量）
     *
     * @param purchaseOrderItems 采购订单明细实体
     */
    private void calculateAmount(PurchaseOrderItems purchaseOrderItems) {
        if (purchaseOrderItems == null || purchaseOrderItems.getAmount() != null) {
            return;
        }
        BigDecimal qty = purchaseOrderItems.getActualArrivalQty() != null
                ? purchaseOrderItems.getActualArrivalQty()
                : purchaseOrderItems.getPurchaseQuantity();
        if (qty != null) {
            BigDecimal price = purchaseOrderItems.getUnitPrice() != null ? purchaseOrderItems.getUnitPrice() : BigDecimal.ZERO;
            purchaseOrderItems.setAmount(qty.multiply(price));
        }
    }

    /**
     * 计算明细金额（批量更新场景：仅携带部分字段时，数量/单价回退原记录）
     * <p>金额为空时按 (传入的实际到货数量 或 原记录的实际到货数量 或 原记录的采购数量) × (传入单价 或 原记录单价) 计算</p>
     *
     * @param purchaseOrderItems 采购订单明细实体（仅携带待修改字段）
     * @param existing           数据库原记录
     */
    private void calculateAmount(PurchaseOrderItems purchaseOrderItems, PurchaseOrderItems existing) {
        if (purchaseOrderItems == null || purchaseOrderItems.getAmount() != null || existing == null) {
            return;
        }
        BigDecimal qty = purchaseOrderItems.getActualArrivalQty() != null
                ? purchaseOrderItems.getActualArrivalQty()
                : existing.getActualArrivalQty() != null
                ? existing.getActualArrivalQty()
                : existing.getPurchaseQuantity();
        BigDecimal price = purchaseOrderItems.getUnitPrice() != null
                ? purchaseOrderItems.getUnitPrice()
                : existing.getUnitPrice();
        if (qty != null) {
            purchaseOrderItems.setAmount(qty.multiply(price != null ? price : BigDecimal.ZERO));
        }
    }

    // endregion
}

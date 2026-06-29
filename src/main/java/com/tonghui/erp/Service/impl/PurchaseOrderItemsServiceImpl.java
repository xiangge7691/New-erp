package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Service.PurchaseOrderItemsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PurchaseOrderItemsServiceImpl extends ServiceImpl<PurchaseOrderItemsMapper, PurchaseOrderItems>
        implements PurchaseOrderItemsService {

    @Override
    @Transactional
    public boolean addPurchaseOrderItem(PurchaseOrderItems purchaseOrderItems) {
        return this.save(purchaseOrderItems);
    }

    @Override
    @Transactional
    public boolean updatePurchaseOrderItem(PurchaseOrderItems purchaseOrderItems) {
        return this.updateById(purchaseOrderItems);
    }

    @Override
    @Transactional
    public boolean deletePurchaseOrderItem(Long itemId) {
        return this.removeById(itemId);
    }

    @Override
    public PurchaseOrderItems getPurchaseOrderItemById(Long itemId) {
        return this.getById(itemId);
    }

    @Override
    public List<PurchaseOrderItems> getPurchaseOrderItemsByOrderId(Long orderId) {
        QueryWrapper<PurchaseOrderItems> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return this.list(wrapper);
    }
}





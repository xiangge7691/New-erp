package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Service.PurchaseOrdersService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PurchaseOrdersServiceImpl extends ServiceImpl<PurchaseOrdersMapper, PurchaseOrders>
        implements PurchaseOrdersService {

    @Override
    public PagedResult<PurchaseOrders> getPurchaseOrderList(PageRequestDto pageRequestDto) {
        Page<PurchaseOrders> page = new Page<>(pageRequestDto.getPageIndex(), pageRequestDto.getPageSize());
        Page<PurchaseOrders> purchaseOrdersPage = this.page(page);

        PagedResult<PurchaseOrders> pagedResult = new PagedResult<>();
        pagedResult.setItems(purchaseOrdersPage.getRecords());
        pagedResult.setTotalCount(purchaseOrdersPage.getTotal());
        pagedResult.setPageIndex(pageRequestDto.getPageIndex());
        pagedResult.setPageSize(pageRequestDto.getPageSize());

        return pagedResult;
    }

    //#region 基础操作

    @Override
    @Transactional
    public boolean addPurchaseOrder(PurchaseOrders purchaseOrders) {
        return this.save(purchaseOrders);
    }

    @Override
    @Transactional
    public boolean updatePurchaseOrder(PurchaseOrders purchaseOrders) {
        return this.updateById(purchaseOrders);
    }

    @Override
    @Transactional
    public boolean deletePurchaseOrder(Long orderId) {
        return this.removeById(orderId);
    }

    //#endregion

    //#region 查询操作

    @Override
    public PurchaseOrders getPurchaseOrderById(Long orderId) {
        return this.getById(orderId);
    }

    //#endregion

    //#region 高级查询

    @Override
    public Page<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<PurchaseOrders> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<PurchaseOrders> wrapper = new QueryWrapper<>();

        if (purchaseOrders.getId() != null) {
            wrapper.eq("id", purchaseOrders.getId());
        }
        if (StringUtils.hasText(purchaseOrders.getPurchaseNumber())) {
            wrapper.like("purchase_number", purchaseOrders.getPurchaseNumber());
        }
        if (StringUtils.hasText(purchaseOrders.getWarehouse())) {
            wrapper.like("warehouse", purchaseOrders.getWarehouse());
        }
        if (purchaseOrders.getStatus() != null) {
            wrapper.eq("status", purchaseOrders.getStatus());
        }
        if (purchaseOrders.getProcessingDate() != null) {
            wrapper.ge("processing_date", purchaseOrders.getProcessingDate());
        }
        if (purchaseOrders.getExpectedDeliveryDate() != null) {
            wrapper.le("expected_delivery_date", purchaseOrders.getExpectedDeliveryDate());
        }
        // 添加缺失的模糊查询字段
        if (StringUtils.hasText(purchaseOrders.getInvoiceInfo())) {
            wrapper.like("invoice_info", purchaseOrders.getInvoiceInfo());
        }
        if (StringUtils.hasText(purchaseOrders.getReceivingInfo())) {
            wrapper.like("receiving_info", purchaseOrders.getReceivingInfo());
        }
        if (StringUtils.hasText(purchaseOrders.getUnit())) {
            wrapper.like("unit", purchaseOrders.getUnit());
        }
        if (StringUtils.hasText(purchaseOrders.getTitle())) {
            wrapper.like("title", purchaseOrders.getTitle());
        }
        if (StringUtils.hasText(purchaseOrders.getRemark())) {
            wrapper.like("remark", purchaseOrders.getRemark());
        }
        // 添加数字类型字段查询
        if (purchaseOrders.getPrescriptionMultiple() != null) {
            wrapper.eq("prescription_multiple", purchaseOrders.getPrescriptionMultiple());
        }
        if (purchaseOrders.getGenerateProductionPlan() != null) {
            wrapper.eq("generate_production_plan", purchaseOrders.getGenerateProductionPlan());
        }
        
        // 按编号倒序排列
        wrapper.orderByDesc("purchase_number");

        return this.page(page, wrapper);
    }

    //#endregion
}





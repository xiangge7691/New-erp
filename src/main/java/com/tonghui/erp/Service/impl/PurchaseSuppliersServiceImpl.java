package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseSuppliersWithDetailsDto;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchaseSuppliersMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Service.PurchaseSuppliersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 87954
* @description 针对表【purchase_suppliers(采购供应商信息表)】的数据库操作Service实现
* @createDate 2025-10-30 10:16:11
*/
@Service
public class PurchaseSuppliersServiceImpl extends ServiceImpl<PurchaseSuppliersMapper, PurchaseSuppliers>
    implements PurchaseSuppliersService{
    
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    @Autowired
    private StockInMapper stockInMapper;

    @Override
    public PagedResult<PurchaseSuppliers> getPurchaseSupplierList(PageRequestDto pageRequestDto) {
        Page<PurchaseSuppliers> page = new Page<>(pageRequestDto.getPageIndex(), pageRequestDto.getPageSize());
        Page<PurchaseSuppliers> purchaseSuppliersPage = this.page(page);

        PagedResult<PurchaseSuppliers> pagedResult = new PagedResult<>();
        pagedResult.setItems(purchaseSuppliersPage.getRecords());
        pagedResult.setTotalCount(purchaseSuppliersPage.getTotal());
        pagedResult.setPageIndex(pageRequestDto.getPageIndex());
        pagedResult.setPageSize(pageRequestDto.getPageSize());

        return pagedResult;
    }

    //#region 基础操作

    @Override
    @Transactional
    public boolean addPurchaseSupplier(PurchaseSuppliers purchaseSuppliers) {
        return this.save(purchaseSuppliers);
    }

    @Override
    @Transactional
    public boolean updatePurchaseSupplier(PurchaseSuppliers purchaseSuppliers) {
        return this.updateById(purchaseSuppliers);
    }

    @Override
    @Transactional
    public boolean deletePurchaseSupplier(Long id) {
        return this.removeById(id);
    }

    //#endregion

    //#region 查询操作

    @Override
    public PurchaseSuppliers getPurchaseSupplierById(Long id) {
        return this.getById(id);
    }

    @Override
    public PurchaseSuppliers getPurchaseSupplierByNumber(String supplierNumber) {
        QueryWrapper<PurchaseSuppliers> wrapper = new QueryWrapper<>();
        wrapper.eq("supplier_number", supplierNumber);
        return this.getOne(wrapper);
    }

    @Override
    public List<PurchaseSuppliers> getEnabledPurchaseSuppliers() {
        QueryWrapper<PurchaseSuppliers> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return this.list(wrapper);
    }

    @Override
    public List<PurchaseSuppliers> getAllPurchaseSuppliers() {
        return this.list();
    }

    //#endregion

    //#region 高级查询

    @Override
    public Page<PurchaseSuppliers> queryPurchaseSuppliers(PurchaseSuppliers purchaseSuppliers, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<PurchaseSuppliers> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<PurchaseSuppliers> wrapper = new QueryWrapper<>();

        if (purchaseSuppliers.getId() != null) {
            wrapper.eq("id", purchaseSuppliers.getId());
        }
        if (StringUtils.hasText(purchaseSuppliers.getSupplierNumber())) {
            wrapper.like("supplier_number", purchaseSuppliers.getSupplierNumber());
        }
        if (StringUtils.hasText(purchaseSuppliers.getSupplierName())) {
            wrapper.like("supplier_name", purchaseSuppliers.getSupplierName());
        }
        if (StringUtils.hasText(purchaseSuppliers.getCategory())) {
            wrapper.like("category", purchaseSuppliers.getCategory());
        }
        if (StringUtils.hasText(purchaseSuppliers.getContactPerson())) {
            wrapper.like("contact_person", purchaseSuppliers.getContactPerson());
        }
        if (StringUtils.hasText(purchaseSuppliers.getPhone())) {
            wrapper.like("phone", purchaseSuppliers.getPhone());
        }
        if (StringUtils.hasText(purchaseSuppliers.getEmail())) {
            wrapper.like("email", purchaseSuppliers.getEmail());
        }
        if (StringUtils.hasText(purchaseSuppliers.getAddress())) {
            wrapper.like("address", purchaseSuppliers.getAddress());
        }
        if (StringUtils.hasText(purchaseSuppliers.getBankAccount())) {
            wrapper.like("bank_account", purchaseSuppliers.getBankAccount());
        }
        if (StringUtils.hasText(purchaseSuppliers.getBankName())) {
            wrapper.like("bank_name", purchaseSuppliers.getBankName());
        }
        if (purchaseSuppliers.getStatus() != null) {
            wrapper.eq("status", purchaseSuppliers.getStatus());
        }

        return this.page(page, wrapper);
    }

    //#endregion

    //#region 带子表查询

    @Override
    public PagedResult<PurchaseSuppliersWithDetailsDto> searchWithDetails(PurchaseSuppliers purchaseSuppliers, int pageNum, int pageSize) {
        Page<PurchaseSuppliers> parentPage = queryPurchaseSuppliers(purchaseSuppliers, pageNum, pageSize);
        List<PurchaseSuppliers> parents = parentPage.getRecords();

        PagedResult<PurchaseSuppliersWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        List<Long> supplierIds = parents.stream().map(PurchaseSuppliers::getId).collect(Collectors.toList());

        QueryWrapper<PurchaseOrders> orderWrapper = new QueryWrapper<>();
        orderWrapper.in("supplier_id", supplierIds);
        List<PurchaseOrders> allOrders = purchaseOrdersMapper.selectList(orderWrapper);
        Map<Long, List<PurchaseOrders>> ordersMap = allOrders.stream()
                .collect(Collectors.groupingBy(PurchaseOrders::getSupplierId));

        QueryWrapper<StockIn> stockInWrapper = new QueryWrapper<>();
        stockInWrapper.in("supplier_id", supplierIds);
        List<StockIn> allStockIns = stockInMapper.selectList(stockInWrapper);
        Map<Long, List<StockIn>> stockInsMap = allStockIns.stream()
                .collect(Collectors.groupingBy(StockIn::getSupplierId));

        List<PurchaseSuppliersWithDetailsDto> dtos = parents.stream().map(parent -> {
            PurchaseSuppliersWithDetailsDto dto = new PurchaseSuppliersWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setOrders(ordersMap.getOrDefault(parent.getId(), List.of()));
            dto.setStockIns(stockInsMap.getOrDefault(parent.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    //#endregion
}





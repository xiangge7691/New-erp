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
 * 采购供应商服务实现类
 * <p>
 * 实现PurchaseSuppliersService接口，提供采购供应商相关的业务逻辑处理，包括供应商的增删改查、
 * 高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class PurchaseSuppliersServiceImpl extends ServiceImpl<PurchaseSuppliersMapper, PurchaseSuppliers>
    implements PurchaseSuppliersService{
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 采购订单数据访问层，用于关联查询供应商关联的采购订单 */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    /** 入库单数据访问层，用于关联查询供应商关联的入库单 */
    @Autowired
    private StockInMapper stockInMapper;

    // endregion

    // region 分页查询方法
    // ===================================
    // 分页查询方法
    // ===================================

    /**
     * 分页查询采购供应商列表
     *
     * @param pageRequestDto 分页请求参数，包含页码和每页数量
     * @return 采购供应商分页结果
     */
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

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增采购供应商
     *
     * @param purchaseSuppliers 采购供应商实体
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean addPurchaseSupplier(PurchaseSuppliers purchaseSuppliers) {
        return this.save(purchaseSuppliers);
    }

    /**
     * 更新采购供应商
     *
     * @param purchaseSuppliers 采购供应商实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updatePurchaseSupplier(PurchaseSuppliers purchaseSuppliers) {
        return this.updateById(purchaseSuppliers);
    }

    /**
     * 删除采购供应商
     *
     * @param id 采购供应商ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deletePurchaseSupplier(Long id) {
        return this.removeById(id);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询采购供应商
     *
     * @param id 采购供应商ID
     * @return 采购供应商实体，不存在则返回null
     */
    @Override
    public PurchaseSuppliers getPurchaseSupplierById(Long id) {
        return this.getById(id);
    }

    /**
     * 根据供应商编号查询采购供应商
     *
     * @param supplierNumber 供应商编号
     * @return 采购供应商实体，不存在则返回null
     */
    @Override
    public PurchaseSuppliers getPurchaseSupplierByNumber(String supplierNumber) {
        QueryWrapper<PurchaseSuppliers> wrapper = new QueryWrapper<>();
        wrapper.eq("supplier_number", supplierNumber);
        return this.getOne(wrapper);
    }

    /**
     * 查询所有启用状态的采购供应商
     *
     * @return 启用状态的采购供应商集合
     */
    @Override
    public List<PurchaseSuppliers> getEnabledPurchaseSuppliers() {
        QueryWrapper<PurchaseSuppliers> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return this.list(wrapper);
    }

    /**
     * 查询所有采购供应商
     *
     * @return 全部采购供应商集合
     */
    @Override
    public List<PurchaseSuppliers> getAllPurchaseSuppliers() {
        return this.list();
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询采购供应商（支持多条件组合查询）
     * <p>支持按供应商编号、名称、分类、联系人、电话、邮箱、地址、银行信息、状态等条件筛选</p>
     *
     * @param purchaseSuppliers 查询条件实体，非null字段将作为等值或模糊查询条件
     * @param keyword           关键字（对供应商编号、供应商名称进行模糊匹配，可选）
     * @param pageNum           页码，从0开始
     * @param pageSize          每页数量
     * @return 采购供应商分页结果
     */
    @Override
    public Page<PurchaseSuppliers> queryPurchaseSuppliers(PurchaseSuppliers purchaseSuppliers, String keyword, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<PurchaseSuppliers> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<PurchaseSuppliers> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对供应商编号、供应商名称进行模糊匹配
            wrapper.and(w -> w.like("supplier_number", keyword).or().like("supplier_name", keyword));
        }
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

        // 数据列表按创建时间倒序排列（最新创建在前）
        wrapper.orderByDesc("created_time");

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询采购供应商列表并关联采购订单和入库单信息
     * <p>先分页查询供应商主表数据，再批量查询关联的采购订单和入库单</p>
     *
     * @param purchaseSuppliers 查询条件实体
     * @param keyword           关键字（对供应商编号、供应商名称进行模糊匹配，可选）
     * @param pageNum           页码，从0开始
     * @param pageSize          每页数量
     * @return 带子表关联数据的采购供应商分页结果
     */
    @Override
    public PagedResult<PurchaseSuppliersWithDetailsDto> searchWithDetails(PurchaseSuppliers purchaseSuppliers, String keyword, int pageNum, int pageSize) {
        // 查询供应商主表分页数据
        Page<PurchaseSuppliers> parentPage = queryPurchaseSuppliers(purchaseSuppliers, keyword, pageNum, pageSize);
        List<PurchaseSuppliers> parents = parentPage.getRecords();

        PagedResult<PurchaseSuppliersWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的采购订单
        List<Long> supplierIds = parents.stream().map(PurchaseSuppliers::getId).collect(Collectors.toList());

        QueryWrapper<PurchaseOrders> orderWrapper = new QueryWrapper<>();
        orderWrapper.in("supplier_id", supplierIds);
        List<PurchaseOrders> allOrders = purchaseOrdersMapper.selectList(orderWrapper);
        Map<Long, List<PurchaseOrders>> ordersMap = allOrders.stream()
                .collect(Collectors.groupingBy(PurchaseOrders::getSupplierId));

        // 批量查询关联的入库单
        QueryWrapper<StockIn> stockInWrapper = new QueryWrapper<>();
        stockInWrapper.in("supplier_id", supplierIds);
        List<StockIn> allStockIns = stockInMapper.selectList(stockInWrapper);
        Map<Long, List<StockIn>> stockInsMap = allStockIns.stream()
                .collect(Collectors.groupingBy(StockIn::getSupplierId));

        // 组装带子表数据的DTO
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

    // endregion
}

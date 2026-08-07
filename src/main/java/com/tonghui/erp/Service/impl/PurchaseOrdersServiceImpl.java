package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseOrdersWithItemsDto;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Service.PurchaseOrdersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购订单服务实现类
 * <p>
 * 实现PurchaseOrdersService接口，提供采购订单相关的业务逻辑处理，包括订单的增删改查、
 * 高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class PurchaseOrdersServiceImpl extends ServiceImpl<PurchaseOrdersMapper, PurchaseOrders>
        implements PurchaseOrdersService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 采购订单明细数据访问层，用于关联查询订单明细信息 */
    @Autowired
    private PurchaseOrderItemsMapper purchaseOrderItemsMapper;

    // endregion

    // region 分页查询方法
    // ===================================
    // 分页查询方法
    // ===================================

    /**
     * 分页查询采购订单列表
     *
     * @param pageRequestDto 分页请求参数，包含页码和每页数量
     * @return 采购订单分页结果
     */
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

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /** 采购订单编号生成最大重试次数（处理并发编号冲突） */
    private static final int MAX_RETRY = 3;

    /**
     * 生成采购订单编号（CG + yyyyMMdd + 4位流水号）
     * <p>原数据库触发器 trg_auto_generate_purchase_number 逻辑迁移至后端实现，
     * 查询最大编号时绕过全局软删除过滤，避免与已软删除订单编号冲突</p>
     *
     * @return 采购订单编号
     */
    @Override
    public String generateOrderNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CG" + dateStr;

        // 使用原生SQL查询当天最大编号，绕过软删除过滤
        String lastNumber = this.baseMapper.selectMaxPurchaseNumberByPrefix(prefix);

        int sequence = 1;
        if (StringUtils.hasText(lastNumber) && lastNumber.length() > prefix.length()) {
            try {
                sequence = Integer.parseInt(lastNumber.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }

    /**
     * 新增采购订单
     *
     * @param purchaseOrders 采购订单实体
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean addPurchaseOrder(PurchaseOrders purchaseOrders) {
        // 重试机制：处理采购订单编号并发冲突
        for (int i = 0; i < MAX_RETRY; i++) {
            // 未提供编号时自动生成（替代原数据库触发器逻辑）
            if (!StringUtils.hasText(purchaseOrders.getPurchaseNumber())) {
                purchaseOrders.setPurchaseNumber(generateOrderNumber());
            }

            try {
                return this.save(purchaseOrders);
            } catch (DuplicateKeyException e) {
                // 编号冲突，清空编号后重试
                purchaseOrders.setPurchaseNumber(null);
                if (i == MAX_RETRY - 1) {
                    throw new RuntimeException("创建失败: 采购订单编号生成冲突，请稍后重试", e);
                }
            }
        }

        return false;
    }

    /**
     * 更新采购订单
     *
     * @param purchaseOrders 采购订单实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updatePurchaseOrder(PurchaseOrders purchaseOrders) {
        return this.updateById(purchaseOrders);
    }

    /**
     * 删除采购订单
     *
     * @param orderId 采购订单ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deletePurchaseOrder(Long orderId) {
        return this.removeById(orderId);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询采购订单
     *
     * @param orderId 采购订单ID
     * @return 采购订单实体，不存在则返回null
     */
    @Override
    public PurchaseOrders getPurchaseOrderById(Long orderId) {
        return this.getById(orderId);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询采购订单（支持多条件组合查询）
     * <p>支持按订单号、仓库、状态、供应商、标题、备注、日期等条件筛选，默认按编号倒序</p>
     *
     * @param purchaseOrders 查询条件实体，非null字段将作为等值或模糊查询条件
     * @param keyword        关键字（对采购编号、采购标题进行模糊匹配，可选）
     * @param pageNum        页码，从0开始
     * @param pageSize       每页数量
     * @return 采购订单分页结果
     */
    @Override
    public Page<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders, String keyword, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<PurchaseOrders> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<PurchaseOrders> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对采购编号、采购标题进行模糊匹配
            wrapper.and(w -> w.like("purchase_number", keyword).or().like("title", keyword));
        }
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
        if (purchaseOrders.getDesiredDeliveryDate() != null) {
            wrapper.le("desired_delivery_date", purchaseOrders.getDesiredDeliveryDate());
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

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询采购订单列表并关联订单明细信息
     * <p>先分页查询订单主表数据，再批量查询关联的订单明细</p>
     *
     * @param purchaseOrders 查询条件实体
     * @param keyword        关键字（对采购编号、采购标题进行模糊匹配，可选）
     * @param pageNum        页码，从0开始
     * @param pageSize       每页数量
     * @return 带子表关联数据的采购订单分页结果
     */
    @Override
    public PagedResult<PurchaseOrdersWithItemsDto> searchWithDetails(PurchaseOrders purchaseOrders, String keyword, int pageNum, int pageSize) {
        // 查询采购订单主表分页数据
        Page<PurchaseOrders> parentPage = queryPurchaseOrders(purchaseOrders, keyword, pageNum, pageSize);
        List<PurchaseOrders> parents = parentPage.getRecords();

        PagedResult<PurchaseOrdersWithItemsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的订单明细
        List<Long> parentIds = parents.stream().map(PurchaseOrders::getId).collect(Collectors.toList());
        QueryWrapper<PurchaseOrderItems> wrapper = new QueryWrapper<>();
        wrapper.in("order_id", parentIds);
        List<PurchaseOrderItems> allItems = purchaseOrderItemsMapper.selectList(wrapper);
        Map<Long, List<PurchaseOrderItems>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(PurchaseOrderItems::getOrderId));

        // 组装带子表数据的DTO
        List<PurchaseOrdersWithItemsDto> dtos = parents.stream().map(parent -> {
            PurchaseOrdersWithItemsDto dto = new PurchaseOrdersWithItemsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setItems(itemsMap.getOrDefault(parent.getId(), List.of()));
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

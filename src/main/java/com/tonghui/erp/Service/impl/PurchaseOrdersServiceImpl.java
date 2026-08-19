package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseOrdersWithItemsDto;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Service.PurchaseOrdersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    /** 验收单数据访问层，用于自动生成货物验收单 */
    @Autowired
    private AcceptanceOrderMapper acceptanceOrderMapper;

    /** 验收单明细数据访问层，用于自动生成验收明细 */
    @Autowired
    private AcceptanceDetailMapper acceptanceDetailMapper;

    /** 序列号生成服务，用于自动生成验收单号 */
    @Autowired
    private SequenceServiceImpl sequenceService;

    /** 物料数据访问层，自动生成验收单时按物料主数据校正物料名称 */
    @Autowired
    private MaterialMapper materialMapper;

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
     * 生成采购订单编号（CGDH + yyyyMMdd + 4位流水号）
     * <p>原数据库触发器 trg_auto_generate_purchase_number 逻辑迁移至后端实现，
     * 查询最大编号时绕过全局软删除过滤，避免与已软删除订单编号冲突</p>
     *
     * @return 采购订单编号
     */
    @Override
    public String generateOrderNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CGDH" + dateStr;

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
     * <p>
     * 当状态更新为"运输中"时，自动生成对应的货物验收单（含明细，从采购订单明细复制），
     * 同一采购订单仅生成一次（幂等）
     * </p>
     *
     * @param purchaseOrders 采购订单实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updatePurchaseOrder(PurchaseOrders purchaseOrders) {
        boolean updated = this.updateById(purchaseOrders);

        // 触发式逻辑：状态更新为"运输中"时自动生成货物验收单
        if (updated && StringUtils.hasText(String.valueOf(purchaseOrders.getStatus()))
                && "运输中".equals(String.valueOf(purchaseOrders.getStatus()))) {
            PurchaseOrders order = this.getById(purchaseOrders.getId());
            if (order != null) {
                createAcceptanceFromOrder(order);
            }
        }

        return updated;
    }

    /**
     * 根据采购订单自动生成货物验收单（含明细）
     * <p>
     * 幂等：同一采购订单号已存在验收单时不重复生成；
     * 验收单初始状态为"运输中"，明细从采购订单明细复制，批号/效期留待检验阶段填写
     * </p>
     *
     * @param order 采购订单
     */
    private void createAcceptanceFromOrder(PurchaseOrders order) {
        // 幂等校验：按采购订单号查询是否已生成验收单
        Long count = acceptanceOrderMapper.selectCount(new QueryWrapper<AcceptanceOrder>()
                .eq("purchase_number", order.getPurchaseNumber()));
        if (count != null && count > 0) {
            return;
        }

        // 构造验收单主表（状态为"运输中"，走通确认到货流程）
        AcceptanceOrder acceptance = new AcceptanceOrder();
        acceptance.setAcceptanceCode(sequenceService.generateAcceptanceCode());
        acceptance.setSourceType("采购入库");
        acceptance.setRelatedOrder(order.getPurchaseNumber());
        acceptance.setPurchaseNumber(order.getPurchaseNumber());
        acceptance.setPlanCode(order.getPlanCode());
        acceptance.setTitle(order.getTitle());
        acceptance.setUnitName(order.getUnit());
        acceptance.setPreparationCode(order.getPreparationCode());
        acceptance.setPreparationName(order.getPreparationName());
        acceptance.setSpec(order.getSpec());
        acceptance.setBatchQty(order.getBatchQty());
        acceptance.setPrescriptionMultiple(order.getPrescriptionMultiple());
        acceptance.setProdUnitId(order.getProdUnitId());
        acceptance.setStatus("运输中");
        acceptanceOrderMapper.insert(acceptance);

        // 从采购订单明细复制生成验收明细
        List<PurchaseOrderItems> items = purchaseOrderItemsMapper.selectList(
                new QueryWrapper<PurchaseOrderItems>().eq("order_id", order.getId()));

        // 批量加载物料主数据，用于校正物料名称（物料表为名称唯一权威来源，避免复制字段陈旧/错位）
        List<Long> materialIds = items.stream()
                .map(PurchaseOrderItems::getMaterialId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of() : materialMapper.selectBatchIds(materialIds)
                .stream().collect(Collectors.toMap(Material::getMaterialId, m -> m, (a, b) -> a));

        List<AcceptanceDetail> details = new ArrayList<>();
        for (PurchaseOrderItems item : items) {
            AcceptanceDetail detail = new AcceptanceDetail();
            detail.setAcceptanceId(acceptance.getAcceptanceId());
            detail.setSeq(item.getSequenceNumber() != null ? item.getSequenceNumber() : 0);
            // 物料类型固定为material（stock_in_detail.item_type为枚举，现有数据均为此值），原料/辅料/包材由分类区分
            detail.setItemType("material");
            detail.setItemId(item.getMaterialId());
            detail.setMaterialCode(item.getMaterialCode());
            // 物料名称：优先取物料主数据（material表），其次原药材品名，最后回退制剂名称
            // （product_name 字段在计划复制时存的是"原料/辅料/包材"分类，绝不能作为物料名称）
            detail.setMaterialName(resolveMaterialName(item, materialMap));
            detail.setMaterialCategory(item.getProcessingProperty());
            detail.setUnitName(item.getUnit());
            detail.setStandardDosage(item.getStandardDosage());
            detail.setQuantity(item.getPurchaseQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setAmount(item.getAmount());
            acceptanceDetailMapper.insert(detail);
            details.add(detail);
        }
        if (details.isEmpty()) {
            throw new RuntimeException("采购订单没有明细，无法自动生成验收单");
        }
    }

    /**
     * 解析验收明细物料名称
     * <p>物料主数据（material 表）为名称唯一权威来源，其次原药材品名，最后回退制剂名称</p>
     *
     * @param item       采购订单明细
     * @param materialMap 已加载的物料主数据（按物料ID索引）
     * @return 物料名称
     */
    private String resolveMaterialName(PurchaseOrderItems item, Map<Long, Material> materialMap) {
        // 优先：物料主数据名称
        if (item.getMaterialId() != null) {
            Material material = materialMap.get(item.getMaterialId());
            if (material != null && StringUtils.hasText(material.getMaterialName())) {
                return material.getMaterialName();
            }
        }
        // 其次：原药材品名（真实物料名称）
        if (StringUtils.hasText(item.getRawMaterialName())) {
            return item.getRawMaterialName();
        }
        // 最后：制剂名称
        return item.getProductName();
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
     * @param processingDateStart 处理开始日期（可选，格式：yyyy-MM-dd）
     * @param processingDateEnd 处理结束日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateStart 期望到货开始日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateEnd 期望到货结束日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateStart 预计到货开始日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateEnd 预计到货结束日期（可选，格式：yyyy-MM-dd）
     * @param pageNum        页码，从0开始
     * @param pageSize       每页数量
     * @return 采购订单分页结果
     */
    @Override
    public Page<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders, String keyword,
            String processingDateStart, String processingDateEnd,
            String desiredDeliveryDateStart, String desiredDeliveryDateEnd,
            String expectedDeliveryDateStart, String expectedDeliveryDateEnd,
            int pageNum, int pageSize) {
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
        
        // 处理日期范围查询
        if (StringUtils.hasText(processingDateStart)) {
            wrapper.ge("processing_date", processingDateStart);
        }
        if (StringUtils.hasText(processingDateEnd)) {
            wrapper.le("processing_date", processingDateEnd);
        }
        
        // 期望到货日期范围查询
        if (StringUtils.hasText(desiredDeliveryDateStart)) {
            wrapper.ge("desired_delivery_date", desiredDeliveryDateStart);
        }
        if (StringUtils.hasText(desiredDeliveryDateEnd)) {
            wrapper.le("desired_delivery_date", desiredDeliveryDateEnd);
        }
        
        // 预计到货日期范围查询
        if (StringUtils.hasText(expectedDeliveryDateStart)) {
            wrapper.ge("expected_delivery_date", expectedDeliveryDateStart);
        }
        if (StringUtils.hasText(expectedDeliveryDateEnd)) {
            wrapper.le("expected_delivery_date", expectedDeliveryDateEnd);
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
     * @param processingDateStart 处理开始日期（可选，格式：yyyy-MM-dd）
     * @param processingDateEnd 处理结束日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateStart 期望到货开始日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateEnd 期望到货结束日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateStart 预计到货开始日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateEnd 预计到货结束日期（可选，格式：yyyy-MM-dd）
     * @param pageNum        页码，从0开始
     * @param pageSize       每页数量
     * @return 带子表关联数据的采购订单分页结果
     */
    @Override
    public PagedResult<PurchaseOrdersWithItemsDto> searchWithDetails(PurchaseOrders purchaseOrders, String keyword,
            String processingDateStart, String processingDateEnd,
            String desiredDeliveryDateStart, String desiredDeliveryDateEnd,
            String expectedDeliveryDateStart, String expectedDeliveryDateEnd,
            int pageNum, int pageSize) {
        // 查询采购订单主表分页数据
        Page<PurchaseOrders> parentPage = queryPurchaseOrders(purchaseOrders, keyword, 
                processingDateStart, processingDateEnd, desiredDeliveryDateStart, desiredDeliveryDateEnd,
                expectedDeliveryDateStart, expectedDeliveryDateEnd, pageNum, pageSize);
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

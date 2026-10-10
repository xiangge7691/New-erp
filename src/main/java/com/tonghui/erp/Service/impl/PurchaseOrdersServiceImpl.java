package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseExportRowDto;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseOrdersWithItemsDto;
import com.tonghui.erp.Common.utils.AcceptanceStatusPolicy;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchaseSuppliersMapper;
import com.tonghui.erp.Service.PurchaseOrdersService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /** 供应商数据访问层，采购数据导出时回填供应商名称 */
    @Autowired
    private PurchaseSuppliersMapper purchaseSuppliersMapper;

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
        acceptance.setPlanCode(order.getProductionPlanCode());
        acceptance.setTitle(order.getTitle());
        acceptance.setUnitName(order.getUnit());
        acceptance.setPreparationCode(order.getPreparationCode());
        acceptance.setPreparationName(order.getPreparationName());
        acceptance.setSpec(order.getSpec());
        acceptance.setBatchQty(order.getBatchQty());
        acceptance.setPrescriptionMultiple(order.getPrescriptionMultiple());
        acceptance.setProdUnitId(order.getProdUnitId());
        acceptance.setDeliveryDate(order.getExpectedDeliveryDate());
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
            // 明细状态初始化为"待初验"，并携带采购订单明细ID与供应商（退货/同步回写依赖）
            detail.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL);
            detail.setPurchaseItemId(item.getId());
            detail.setSupplier(item.getSupplier());
            acceptanceDetailMapper.insert(detail);

            // 采购订单明细状态同步为"待初验"（验收明细带动订单明细）
            item.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL);
            purchaseOrderItemsMapper.updateById(item);
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

    /**
     * 确认采购（待采购 → 运输中）
     * <p>
     * 前置校验每条明细供应商必填，确认后主单状态改为"运输中"，
     * 并自动生成一条对应验收单（状态"运输中"，等待货物验收页确认到货）
     * </p>
     *
     * @param orderId 采购订单ID
     */
    @Override
    @Transactional
    public void confirmPurchase(Long orderId) {
        PurchaseOrders order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!AcceptanceStatusPolicy.ORDER_PENDING.equals(String.valueOf(order.getStatus()))) {
            throw new RuntimeException("仅「待采购」状态的订单可确认采购，当前状态: " + order.getStatus());
        }

        // 前置校验：每条明细必须已填写供应商（文档：确认采购前必填，否则无法点击采购）
        List<PurchaseOrderItems> items = purchaseOrderItemsMapper.selectList(
                new QueryWrapper<PurchaseOrderItems>().eq("order_id", orderId));
        if (items.isEmpty()) {
            throw new RuntimeException("采购订单没有明细，无法确认采购");
        }
        for (PurchaseOrderItems item : items) {
            if (!StringUtils.hasText(item.getSupplier())) {
                throw new RuntimeException("存在未填写供应商的物料，无法确认采购: "
                        + (StringUtils.hasText(item.getRawMaterialName()) ? item.getRawMaterialName() : item.getMaterialCode()));
            }
        }

        // 主单状态 → 运输中，并自动生成验收单（幂等）
        order.setStatus(AcceptanceStatusPolicy.MAIN_TRANSPORTING);
        this.updateById(order);
        createAcceptanceFromOrder(order);
    }

    /**
     * 作废采购订单（整单 → 已作废，终态）
     * <p>
     * 仅待采购/运输中/验收中/已结束状态可作废；已入库（已产生库存）与已作废订单不可作废；
     * 作废后联动将关联验收单（未入库/未作废）一并置为"已作废"，避免验收单继续流转
     * </p>
     *
     * @param orderId 采购订单ID
     */
    @Override
    @Transactional
    public void voidPurchase(Long orderId) {
        PurchaseOrders order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        String status = String.valueOf(order.getStatus());
        if (AcceptanceStatusPolicy.MAIN_VOIDED.equals(status)) {
            throw new RuntimeException("订单已作废，不可重复作废");
        }
        if (AcceptanceStatusPolicy.MAIN_INBOUND.equals(status)) {
            throw new RuntimeException("已入库订单不可作废（已产生库存记录）");
        }

        order.setStatus(AcceptanceStatusPolicy.MAIN_VOIDED);
        this.updateById(order);

        // 联动作废关联验收单（已入库/已作废验收单保持不动）
        List<AcceptanceOrder> acceptances = acceptanceOrderMapper.selectList(
                new QueryWrapper<AcceptanceOrder>().eq("purchase_number", order.getPurchaseNumber()));
        for (AcceptanceOrder acceptance : acceptances) {
            String accStatus = String.valueOf(acceptance.getStatus());
            if (!AcceptanceStatusPolicy.MAIN_VOIDED.equals(accStatus)
                    && !AcceptanceStatusPolicy.MAIN_INBOUND.equals(accStatus)) {
                acceptance.setStatus(AcceptanceStatusPolicy.MAIN_VOIDED);
                acceptanceOrderMapper.updateById(acceptance);
            }
        }
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

    // region 采购数据导出
    // ===================================
    // 采购数据导出
    // ===================================

    /**
     * 导出采购数据 Excel（仅采购数据，不含领料数据）
     * <p>
     * 流程：日期参数校验 → 按筛选条件查询采购订单主表 → 批量查询订单明细 →
     * 批量加载物料/供应商名称 → 组装为行（每行对应一条订单明细）→ POI 生成单 sheet xlsx 写流
     * </p>
     *
     * @param keyword             关键字（对采购订单编号、工单标题模糊匹配，可选）
     * @param status              采购订单状态（精确匹配，可选）
     * @param processingDateStart 处理日期起始（yyyy-MM-dd，可选）
     * @param processingDateEnd   处理日期结束（yyyy-MM-dd，可选）
     * @param out                 输出流
     * @throws IllegalArgumentException 日期格式非法或开始日期晚于结束日期
     * @throws IOException              写流失败
     */
    @Override
    public void exportPurchaseData(String keyword, String status,
                                   String processingDateStart, String processingDateEnd,
                                   OutputStream out) throws IOException {
        // 1. 参数校验：日期可选，若提供需合法且起止有序
        LocalDate start = parseOptionalDate(processingDateStart, "开始日期");
        LocalDate end = parseOptionalDate(processingDateEnd, "结束日期");
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        // 2. 查询采购订单主表（含筛选条件）
        QueryWrapper<PurchaseOrders> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("purchase_number", kw).or().like("title", kw));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (start != null) {
            wrapper.ge("processing_date", start);
        }
        if (end != null) {
            wrapper.le("processing_date", end);
        }
        wrapper.orderByAsc("purchase_number");
        List<PurchaseOrders> orders = this.baseMapper.selectList(wrapper);

        List<PurchaseExportRowDto> rows = new ArrayList<>();
        if (!orders.isEmpty()) {
            // 3. 按订单ID批量查询明细，按订单+序号排序
            List<Long> orderIds = orders.stream().map(PurchaseOrders::getId).collect(Collectors.toList());
            List<PurchaseOrderItems> items = purchaseOrderItemsMapper.selectList(
                    new QueryWrapper<PurchaseOrderItems>()
                            .in("order_id", orderIds)
                            .orderByAsc("order_id")
                            .orderByAsc("sequence_number"));

            // 4. 批量加载物料名称与供应商名称
            List<Long> materialIds = items.stream().map(PurchaseOrderItems::getMaterialId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of()
                    : materialMapper.selectBatchIds(materialIds).stream()
                            .collect(Collectors.toMap(Material::getMaterialId, m -> m, (a, b) -> a));

            List<Long> supplierIds = orders.stream().map(PurchaseOrders::getSupplierId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, String> supplierNameMap = supplierIds.isEmpty() ? Map.of()
                    : purchaseSuppliersMapper.selectBatchIds(supplierIds).stream()
                            .filter(s -> s.getId() != null && StringUtils.hasText(s.getSupplierName()))
                            .collect(Collectors.toMap(PurchaseSuppliers::getId,
                                    PurchaseSuppliers::getSupplierName, (a, b) -> a));

            Map<Long, PurchaseOrders> orderMap = orders.stream()
                    .collect(Collectors.toMap(PurchaseOrders::getId, o -> o, (a, b) -> a));

            // 5. 组装导出行
            for (PurchaseOrderItems item : items) {
                PurchaseOrders order = orderMap.get(item.getOrderId());
                if (order == null) {
                    continue;
                }
                PurchaseExportRowDto row = new PurchaseExportRowDto();
                row.setPurchaseNumber(order.getPurchaseNumber());
                row.setProductionPlanCode(order.getProductionPlanCode());
                row.setTitle(order.getTitle());
                row.setPreparationName(order.getPreparationName());
                row.setEntrustUnit(order.getUnit());
                row.setBatchQty(order.getBatchQty());
                row.setPrescriptionMultiple(order.getPrescriptionMultiple());
                row.setOrderStatus(Objects.toString(order.getStatus(), ""));
                row.setCreatedTime(order.getCreatedTime());

                row.setMaterialCode(item.getMaterialCode());
                // 物料名称：物料主数据优先，回退明细原药材品名
                Material material = item.getMaterialId() != null ? materialMap.get(item.getMaterialId()) : null;
                row.setMaterialName(material != null && StringUtils.hasText(material.getMaterialName())
                        ? material.getMaterialName() : item.getRawMaterialName());
                row.setMaterialCategory(material != null ? material.getCategoryName() : null);
                // 规格：取物料主数据的规格
                row.setSpec(material != null ? material.getSpec() : null);
                row.setUnit(item.getUnit());
                row.setStandardDosage(item.getStandardDosage());
                row.setPurchaseQuantity(item.getPurchaseQuantity());
                row.setUnitPrice(item.getUnitPrice());
                row.setAmount(item.getAmount());
                row.setDifference(item.getDifference());
                row.setInvoiceNo(item.getInvoiceNo());
                // 供应商：明细优先，回退主表供应商
                String supplier = StringUtils.hasText(item.getSupplier())
                        ? item.getSupplier()
                        : (order.getSupplierId() != null ? supplierNameMap.get(order.getSupplierId()) : null);
                row.setSupplier(supplier);
                rows.add(row);
            }
        }

        // 6. 生成 xlsx
        writePurchaseWorkbook(rows, out);
    }

    /**
     * 解析可选日期参数（为空返回null）
     *
     * @param value 参数值
     * @param label 参数中文名（用于错误消息）
     * @return 解析后的日期，未提供时返回null
     * @throws IllegalArgumentException 格式非法
     */
    private LocalDate parseOptionalDate(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + "格式非法，应为 yyyy-MM-dd");
        }
    }

    /**
     * POI 生成采购数据 workbook 并写入输出流
     * <p>
     * 单 sheet「采购数据」：Row0 表头（加粗灰底居中），Row1+ 数据行；
     * 单价、总价列格式 0.00
     * </p>
     *
     * @param rows 导出行列表
     * @param out  输出流
     * @throws IOException 写流失败
     */
    private void writePurchaseWorkbook(List<PurchaseExportRowDto> rows, OutputStream out) throws IOException {
        String[] headers = {
                "采购订单编号", "生产计划编号", "工单标题", "制剂名称", "委托单位", "批量", "处方倍数",
                "物料编码", "物料名称", "物料分类", "规格", "计量单位", "标准处方", "采购数量",
                "单价", "总价", "标准量差值", "采购订单状态", "发票号", "供应商", "创建时间（采购时间）"
        };

        try (Workbook workbook = new XSSFWorkbook()) {
            // 样式：表头（加粗灰底居中）、金额（0.00）
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            Sheet sheet = workbook.createSheet("采购数据");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (PurchaseExportRowDto row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(Objects.toString(row.getPurchaseNumber(), ""));
                dataRow.createCell(1).setCellValue(Objects.toString(row.getProductionPlanCode(), ""));
                dataRow.createCell(2).setCellValue(Objects.toString(row.getTitle(), ""));
                dataRow.createCell(3).setCellValue(Objects.toString(row.getPreparationName(), ""));
                dataRow.createCell(4).setCellValue(Objects.toString(row.getEntrustUnit(), ""));
                setNumberCell(dataRow.createCell(5), row.getBatchQty());
                setNumberCell(dataRow.createCell(6), row.getPrescriptionMultiple());
                dataRow.createCell(7).setCellValue(Objects.toString(row.getMaterialCode(), ""));
                dataRow.createCell(8).setCellValue(Objects.toString(row.getMaterialName(), ""));
                dataRow.createCell(9).setCellValue(Objects.toString(row.getMaterialCategory(), ""));
                dataRow.createCell(10).setCellValue(Objects.toString(row.getSpec(), ""));
                dataRow.createCell(11).setCellValue(Objects.toString(row.getUnit(), ""));
                setNumberCell(dataRow.createCell(12), row.getStandardDosage());
                setNumberCell(dataRow.createCell(13), row.getPurchaseQuantity());
                setMoneyCell(dataRow.createCell(14), row.getUnitPrice(), moneyStyle);
                setMoneyCell(dataRow.createCell(15), row.getAmount(), moneyStyle);
                setNumberCell(dataRow.createCell(16), row.getDifference());
                dataRow.createCell(17).setCellValue(Objects.toString(row.getOrderStatus(), ""));
                dataRow.createCell(18).setCellValue(Objects.toString(row.getInvoiceNo(), ""));
                dataRow.createCell(19).setCellValue(Objects.toString(row.getSupplier(), ""));
                dataRow.createCell(20).setCellValue(row.getCreatedTime() != null
                        ? row.getCreatedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            }

            // 列宽
            int[] widths = {22, 20, 20, 18, 18, 12, 12, 18, 20, 12, 18, 10, 12, 12, 12, 12, 12, 14, 18, 18, 22};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(out);
        }
    }

    /**
     * 写入数值单元格（空值按0）
     *
     * @param cell  单元格
     * @param value 数值
     */
    private void setNumberCell(Cell cell, BigDecimal value) {
        cell.setCellValue(nullToZero(value).doubleValue());
    }

    /**
     * 写入金额单元格（空值按0，应用金额格式）
     *
     * @param cell       单元格
     * @param value      金额
     * @param moneyStyle 金额样式
     */
    private void setMoneyCell(Cell cell, BigDecimal value, CellStyle moneyStyle) {
        cell.setCellValue(nullToZero(value).doubleValue());
        cell.setCellStyle(moneyStyle);
    }

    /**
     * 空值安全转零
     *
     * @param value 数值
     * @return 非空原值，否则0
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // endregion
}

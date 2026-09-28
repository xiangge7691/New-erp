package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialResendRequestDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnItemDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnQueryDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.AcceptanceStatusPolicy;
import com.tonghui.erp.Common.utils.MaterialReturnPolicy;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.AcceptanceResendLog;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.AcceptanceResendLogMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Service.AcceptanceOrderService;
import com.tonghui.erp.Service.MaterialReturnService;
import com.tonghui.erp.Service.impl.SequenceServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 物料退货管理服务实现类
 * <p>
 * 实现MaterialReturnService接口，提供退货明细行查询、退货、取消、
 * 重新发货（同验收单多条已退货明细合并生成新验收单并写留痕）能力。
 * 所有状态词表统一来自 AcceptanceStatusPolicy，状态变更后回写采购订单明细状态，
 * 重新发货生成的新验收单通过验收服务的同步入口联动采购订单主单状态
 * </p>
 */
@Service
public class MaterialReturnServiceImpl implements MaterialReturnService {

    // region 依赖与常量
    // ===================================
    // 依赖与常量
    // ===================================

    /** 验收明细数据访问层 */
    private final AcceptanceDetailMapper acceptanceDetailMapper;

    /** 验收单数据访问层 */
    private final AcceptanceOrderMapper acceptanceOrderMapper;

    /** 采购订单数据访问层 */
    private final PurchaseOrdersMapper purchaseOrdersMapper;

    /** 采购订单明细数据访问层（退货状态回写） */
    private final PurchaseOrderItemsMapper purchaseOrderItemsMapper;

    /** 重新发货留痕数据访问层 */
    private final AcceptanceResendLogMapper acceptanceResendLogMapper;

    /** 序列号生成服务（新验收单号） */
    private final SequenceServiceImpl sequenceService;

    /** 验收单服务（新增验收单、同步采购订单状态） */
    private final AcceptanceOrderService acceptanceOrderService;

    /** 退货相关明细状态集合（列表页数据范围） */
    private static final List<String> RETURN_STATUSES = List.of(
            AcceptanceStatusPolicy.DETAIL_PENDING_RETURN,
            AcceptanceStatusPolicy.DETAIL_RETURNED,
            AcceptanceStatusPolicy.DETAIL_RESENT,
            AcceptanceStatusPolicy.DETAIL_CANCELLED);

    /**
     * 构造函数注入依赖
     *
     * @param acceptanceDetailMapper   验收明细数据访问层
     * @param acceptanceOrderMapper    验收单数据访问层
     * @param purchaseOrdersMapper     采购订单数据访问层
     * @param purchaseOrderItemsMapper 采购订单明细数据访问层
     * @param acceptanceResendLogMapper 重新发货留痕数据访问层
     * @param sequenceService          序列号生成服务
     * @param acceptanceOrderService   验收单服务
     */
    @Autowired
    public MaterialReturnServiceImpl(AcceptanceDetailMapper acceptanceDetailMapper,
                                     AcceptanceOrderMapper acceptanceOrderMapper,
                                     PurchaseOrdersMapper purchaseOrdersMapper,
                                     PurchaseOrderItemsMapper purchaseOrderItemsMapper,
                                     AcceptanceResendLogMapper acceptanceResendLogMapper,
                                     SequenceServiceImpl sequenceService,
                                     AcceptanceOrderService acceptanceOrderService) {
        this.acceptanceDetailMapper = acceptanceDetailMapper;
        this.acceptanceOrderMapper = acceptanceOrderMapper;
        this.purchaseOrdersMapper = purchaseOrdersMapper;
        this.purchaseOrderItemsMapper = purchaseOrderItemsMapper;
        this.acceptanceResendLogMapper = acceptanceResendLogMapper;
        this.sequenceService = sequenceService;
        this.acceptanceOrderService = acceptanceOrderService;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 分页查询退货明细行
     * <p>
     * 以验收明细为行、状态限定在退货相关四个状态；支持按退货来源（采购退货/领料退货）筛选，
     * 来源由所属验收单的来源类型派生；批量组装验收单号/采购订单编号/制剂名称/供应商与最近重发时间，
     * 避免逐行查询
     * </p>
     *
     * @param query 查询条件（退货来源/状态/退货原因/供应商/关键字/日期）
     * @return 退货明细分页结果
     */
    @Override
    public PagedResult<MaterialReturnItemDto> searchItems(MaterialReturnQueryDto query) {
        if (query == null) {
            query = new MaterialReturnQueryDto();
        }
        int pageSize = query.getPageSize() <= 0 ? 20 : query.getPageSize();
        int pageIndex = Math.max(query.getPageIndex(), 0);
        Page<AcceptanceDetail> page = new Page<>(pageIndex + 1, pageSize);

        QueryWrapper<AcceptanceDetail> wrapper = new QueryWrapper<>();
        // 数据范围：退货相关状态；指定了状态则进一步限定
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq("status", query.getStatus());
        } else {
            wrapper.in("status", RETURN_STATUSES);
        }
        // 退货来源筛选：按所属验收单的来源类型派生（采购入库/退货重发→采购退货，领料入库→领料退货）
        if (StringUtils.hasText(query.getReturnSource())) {
            List<Long> sourceAcceptanceIds = queryAcceptanceIdsBySource(query.getReturnSource());
            wrapper.in("acceptance_id", sourceAcceptanceIds);
        }
        if (StringUtils.hasText(query.getReturnReason())) {
            wrapper.eq("return_reason", query.getReturnReason());
        }
        if (StringUtils.hasText(query.getSupplier())) {
            wrapper.eq("supplier", query.getSupplier());
        }
        // 关键字：物料名称/物料编码/验收单号 模糊匹配
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword();
            List<Long> matchedAcceptanceIds = acceptanceOrderMapper.selectList(
                            new QueryWrapper<AcceptanceOrder>()
                                    .like("acceptance_code", keyword)
                                    .select("acceptance_id"))
                    .stream().map(AcceptanceOrder::getAcceptanceId).collect(Collectors.toList());
            List<Long> scope = matchedAcceptanceIds.isEmpty() ? List.of(-1L) : matchedAcceptanceIds;
            wrapper.and(w -> w.like("material_name", keyword)
                    .or().like("material_code", keyword)
                    .or().in("acceptance_id", scope));
        }
        // 日期：按明细最近更新时间范围过滤
        if (StringUtils.hasText(query.getStartDate())) {
            wrapper.ge("updated_time", parseDateStart(query.getStartDate(), "开始日期"));
        }
        if (StringUtils.hasText(query.getEndDate())) {
            wrapper.le("updated_time", parseDateEnd(query.getEndDate(), "结束日期"));
        }
        wrapper.orderByDesc("updated_time").orderByDesc("detail_id");

        Page<AcceptanceDetail> result = acceptanceDetailMapper.selectPage(page, wrapper);
        PagedResult<MaterialReturnItemDto> paged = new PagedResult<>();
        paged.setTotalCount(result.getTotal());
        paged.setPageIndex(pageIndex);
        paged.setPageSize(pageSize);
        if (result.getRecords().isEmpty()) {
            paged.setItems(List.of());
            return paged;
        }
        paged.setItems(assembleItems(result.getRecords()));
        return paged;
    }

    /**
     * 批量组装退货明细行的关联展示字段
     *
     * @param details 明细列表（当前页）
     * @return 组装后的列表行DTO
     */
    private List<MaterialReturnItemDto> assembleItems(List<AcceptanceDetail> details) {
        // 批量加载验收单
        List<Long> acceptanceIds = details.stream()
                .map(AcceptanceDetail::getAcceptanceId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, AcceptanceOrder> acceptanceMap = acceptanceIds.isEmpty() ? Map.of()
                : acceptanceOrderMapper.selectBatchIds(acceptanceIds).stream()
                        .collect(Collectors.toMap(AcceptanceOrder::getAcceptanceId, a -> a, (a, b) -> a));

        // 批量加载采购订单（按采购订单号）
        List<String> purchaseNumbers = acceptanceMap.values().stream()
                .map(a -> StringUtils.hasText(a.getPurchaseNumber()) ? a.getPurchaseNumber() : a.getRelatedOrder())
                .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        Map<String, PurchaseOrders> orderMap = purchaseNumbers.isEmpty() ? Map.of()
                : purchaseOrdersMapper.selectList(new QueryWrapper<PurchaseOrders>()
                        .in("purchase_number", purchaseNumbers)).stream()
                        .collect(Collectors.toMap(o -> String.valueOf(o.getPurchaseNumber()), o -> o, (a, b) -> a));

        // 批量加载最近一次重新发货留痕时间
        List<Long> detailIds = details.stream().map(AcceptanceDetail::getDetailId).collect(Collectors.toList());
        Map<Long, LocalDateTime> resendTimeMap = acceptanceResendLogMapper.selectList(
                        new QueryWrapper<AcceptanceResendLog>().in("detail_id", detailIds))
                .stream()
                .filter(l -> l.getOperationTime() != null)
                .collect(Collectors.groupingBy(AcceptanceResendLog::getDetailId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(AcceptanceResendLog::getOperationTime)),
                                m -> m.map(AcceptanceResendLog::getOperationTime).orElse(null))));

        List<MaterialReturnItemDto> items = new ArrayList<>();
        for (AcceptanceDetail detail : details) {
            MaterialReturnItemDto dto = new MaterialReturnItemDto();
            dto.setDetailId(detail.getDetailId());
            dto.setAcceptanceId(detail.getAcceptanceId());
            dto.setSeq(detail.getSeq());
            dto.setMaterialCode(detail.getMaterialCode());
            dto.setMaterialName(detail.getMaterialName());
            dto.setBatchNumber(detail.getBatchNumber());
            dto.setReturnQty(detail.getQuantity());
            dto.setSupplier(detail.getSupplier());
            dto.setReturnReason(detail.getReturnReason());
            dto.setReturnRemark(detail.getReturnRemark());
            dto.setStatus(detail.getStatus());
            dto.setResendTime(resendTimeMap.get(detail.getDetailId()));

            AcceptanceOrder acceptance = acceptanceMap.get(detail.getAcceptanceId());
            if (acceptance != null) {
                dto.setAcceptanceCode(acceptance.getAcceptanceCode());
                // 退货来源：由所属验收单来源类型派生（领料入库→领料退货，其余→采购退货）
                dto.setReturnSource(MaterialReturnPolicy.isRequisitionSource(acceptance.getSourceType())
                        ? MaterialReturnPolicy.RETURN_SOURCE_REQUISITION
                        : MaterialReturnPolicy.RETURN_SOURCE_PURCHASE);
                String purchaseNumber = StringUtils.hasText(acceptance.getPurchaseNumber())
                        ? acceptance.getPurchaseNumber() : acceptance.getRelatedOrder();
                dto.setPurchaseNumber(purchaseNumber);
                // 关联制剂名称：取验收单制剂，缺失时回退采购订单制剂
                if (StringUtils.hasText(acceptance.getPreparationName())) {
                    dto.setPreparationName(acceptance.getPreparationName());
                } else if (StringUtils.hasText(purchaseNumber)) {
                    PurchaseOrders order = orderMap.get(purchaseNumber);
                    if (order != null) {
                        dto.setPreparationName(order.getPreparationName());
                    }
                }
            }
            items.add(dto);
        }
        return items;
    }

    /**
     * 解析开始日期（含当日 00:00:00）
     *
     * @param date  yyyy-MM-dd 字符串
     * @param field 字段名（错误提示用）
     * @return 日期时间
     */
    private LocalDateTime parseDateStart(String date, String field) {
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new RuntimeException(field + "格式错误，应为 yyyy-MM-dd");
        }
    }

    /**
     * 查询指定退货来源对应的验收单ID集合
     * <p>
     * 采购退货 → 来源类型 采购入库/退货重发；领料退货 → 来源类型含"领料"（如 领料入库）
     * </p>
     *
     * @param returnSource 退货来源（采购退货/领料退货）
     * @return 匹配的验收单ID集合
     */
    private List<Long> queryAcceptanceIdsBySource(String returnSource) {
        if (MaterialReturnPolicy.RETURN_SOURCE_REQUISITION.equals(returnSource)) {
            return acceptanceOrderMapper.selectList(
                            new QueryWrapper<AcceptanceOrder>().like("source_type", "领料").select("acceptance_id"))
                    .stream().map(AcceptanceOrder::getAcceptanceId).collect(Collectors.toList());
        }
        // 采购退货（含采购入库与退货重发）
        return acceptanceOrderMapper.selectList(
                        new QueryWrapper<AcceptanceOrder>()
                                .in("source_type", "采购入库", "退货重发")
                                .select("acceptance_id"))
                .stream().map(AcceptanceOrder::getAcceptanceId).collect(Collectors.toList());
    }

    /**
     * 解析结束日期（含当日 23:59:59）
     *
     * @param date  yyyy-MM-dd 字符串
     * @param field 字段名（错误提示用）
     * @return 日期时间
     */
    private LocalDateTime parseDateEnd(String date, String field) {
        try {
            return LocalDate.parse(date).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new RuntimeException(field + "格式错误，应为 yyyy-MM-dd");
        }
    }

    // endregion

    // region 状态操作
    // ===================================
    // 状态操作
    // ===================================

    /**
     * 退货：明细 待退货 → 已退货
     *
     * @param detailId 验收明细ID
     */
    @Override
    @Transactional
    public void returnDetail(Long detailId) {
        AcceptanceDetail detail = getDetailOrThrow(detailId);
        if (!AcceptanceStatusPolicy.DETAIL_PENDING_RETURN.equals(detail.getStatus())) {
            throw new RuntimeException("仅待退货状态的明细可执行退货");
        }
        detail.setStatus(AcceptanceStatusPolicy.DETAIL_RETURNED);
        acceptanceDetailMapper.updateById(detail);
        // 回写采购订单明细状态
        syncPurchaseItemStatus(detail);
    }

    /**
     * 取消：明细 已退货 → 已取消（该物料取消采购，不影响整单其他明细）
     *
     * @param detailId 验收明细ID
     */
    @Override
    @Transactional
    public void cancelDetail(Long detailId) {
        AcceptanceDetail detail = getDetailOrThrow(detailId);
        if (!AcceptanceStatusPolicy.DETAIL_RETURNED.equals(detail.getStatus())) {
            throw new RuntimeException("仅已退货状态的明细可取消");
        }
        detail.setStatus(AcceptanceStatusPolicy.DETAIL_CANCELLED);
        acceptanceDetailMapper.updateById(detail);
        // 回写采购订单明细状态
        syncPurchaseItemStatus(detail);
    }

    /**
     * 重新发货：同一验收单的多条已退货明细合并生成一条新验收单
     * <p>
     * 新单来源类型=退货重发、关联单号=原采购订单编号、记录原验收单号、初始运输中；
     * 所选明细状态→已重发，逐条写入重新发货留痕，最后同步采购订单（主单+明细）
     * </p>
     *
     * @param request 重新发货请求（验收单ID + 明细ID与重新发货数量列表）
     * @return 新生成的验收单
     */
    @Override
    @Transactional
    public AcceptanceOrder resend(MaterialResendRequestDto request) {
        // 请求校验
        if (request == null || request.getAcceptanceId() == null) {
            throw new RuntimeException("请指定来源验收单");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("请至少选择一条已退货物料");
        }
        AcceptanceOrder original = acceptanceOrderMapper.selectById(request.getAcceptanceId());
        if (original == null) {
            throw new RuntimeException("验收单不存在");
        }

        // 校验所选明细：同属该验收单、状态=已退货、重新发货数量>0
        List<AcceptanceDetail> originalDetails = getDetailsByAcceptanceId(original.getAcceptanceId());
        Map<Long, AcceptanceDetail> detailMap = originalDetails.stream()
                .collect(Collectors.toMap(AcceptanceDetail::getDetailId, d -> d, (a, b) -> a));
        List<AcceptanceDetail> selected = new ArrayList<>();
        List<BigDecimal> selectedQtys = new ArrayList<>();
        for (MaterialResendRequestDto.ResendItem item : request.getItems()) {
            if (item == null || item.getDetailId() == null) {
                throw new RuntimeException("重新发货明细ID不能为空");
            }
            AcceptanceDetail detail = detailMap.get(item.getDetailId());
            if (detail == null) {
                throw new RuntimeException("明细不属于该验收单，无法重新发货");
            }
            if (!AcceptanceStatusPolicy.DETAIL_RETURNED.equals(detail.getStatus())) {
                throw new RuntimeException("仅已退货状态的明细可重新发货: " + detail.getMaterialName());
            }
            if (item.getResendQty() == null || item.getResendQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("重新发货数量必须大于0: " + detail.getMaterialName());
            }
            selected.add(detail);
            selectedQtys.add(item.getResendQty());
        }

        // 构造新验收单：来源类型=退货重发，关联单号=原采购订单编号，记录原验收单号
        String purchaseNumber = StringUtils.hasText(original.getPurchaseNumber())
                ? original.getPurchaseNumber() : original.getRelatedOrder();
        AcceptanceOrder newAcceptance = new AcceptanceOrder();
        BeanUtils.copyProperties(original, newAcceptance, "acceptanceId", "acceptanceCode", "status",
                "prodUnitId", "remark", "originalAcceptanceCode", "createdBy", "createdTime");
        newAcceptance.setAcceptanceCode(sequenceService.generateAcceptanceCode());
        newAcceptance.setSourceType("退货重发");
        newAcceptance.setRelatedOrder(purchaseNumber);
        newAcceptance.setPurchaseNumber(purchaseNumber);
        newAcceptance.setOriginalAcceptanceCode(original.getAcceptanceCode());
        newAcceptance.setStatus(AcceptanceStatusPolicy.MAIN_TRANSPORTING);
        newAcceptance.setProdUnitId(null);
        newAcceptance.setRemark("由 " + original.getAcceptanceCode() + " 退货后重新发货");

        // 构造新明细：采购数量=输入的重新发货数量，批号/实际到货/金额清空待验收环节重算
        List<AcceptanceDetail> newDetails = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            AcceptanceDetail src = selected.get(i);
            AcceptanceDetail nd = new AcceptanceDetail();
            BeanUtils.copyProperties(src, nd, "detailId", "acceptanceId", "seq", "batchNumber",
                    "actualArrivalQty", "inboundQty", "amount", "diffQuantity",
                    "status", "returnReason", "returnRemark");
            nd.setQuantity(selectedQtys.get(i));
            nd.setBatchNumber("");
            nd.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL);
            newDetails.add(nd);
            // 备注追加「发货数量 X.XXX」便于追溯
            appendRemark(newAcceptance, "发货数量 " + selectedQtys.get(i).toPlainString());
        }
        // 通过验收服务新增（自动生成序号、金额与标准量差值重算）
        acceptanceOrderService.addAcceptance(newAcceptance, newDetails);

        // 所选原明细状态 → 已重发（处理完结，防止重复重新发货）
        LocalDateTime operationTime = LocalDateTime.now();
        for (AcceptanceDetail detail : selected) {
            detail.setStatus(AcceptanceStatusPolicy.DETAIL_RESENT);
            acceptanceDetailMapper.updateById(detail);
        }

        // 操作留痕：逐条记录重新发货数量、新验收单号、操作时间
        for (int i = 0; i < selected.size(); i++) {
            AcceptanceDetail detail = selected.get(i);
            AcceptanceResendLog log = new AcceptanceResendLog();
            log.setAcceptanceId(original.getAcceptanceId());
            log.setDetailId(detail.getDetailId());
            log.setMaterialName(detail.getMaterialName());
            log.setResendQty(selectedQtys.get(i));
            log.setNewAcceptanceCode(newAcceptance.getAcceptanceCode());
            log.setOperationTime(operationTime);
            acceptanceResendLogMapper.insert(log);
        }

        // 原单备注记录重新发货去向
        appendRemark(original, "重新发货: " + newAcceptance.getAcceptanceCode());
        acceptanceOrderMapper.updateById(original);

        // 采购订单状态跟随新验收单（运输中/待初验），明细级与主单级一并同步
        acceptanceOrderService.syncPurchaseStatus(newAcceptance);

        return newAcceptance;
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 查询验收明细，不存在时抛出异常
     *
     * @param detailId 明细ID
     * @return 验收明细
     */
    private AcceptanceDetail getDetailOrThrow(Long detailId) {
        if (detailId == null) {
            throw new RuntimeException("明细ID不能为空");
        }
        AcceptanceDetail detail = acceptanceDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new RuntimeException("验收明细不存在");
        }
        return detail;
    }

    /**
     * 查询验收单下全部明细（按序号升序）
     *
     * @param acceptanceId 验收单ID
     * @return 明细列表
     */
    private List<AcceptanceDetail> getDetailsByAcceptanceId(Long acceptanceId) {
        QueryWrapper<AcceptanceDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("acceptance_id", acceptanceId);
        wrapper.orderByAsc("seq");
        return acceptanceDetailMapper.selectList(wrapper);
    }

    /**
     * 回写采购订单明细状态（验收明细状态变化带动订单明细）
     *
     * @param detail 状态已变更的验收明细
     */
    private void syncPurchaseItemStatus(AcceptanceDetail detail) {
        if (detail.getPurchaseItemId() == null || !StringUtils.hasText(detail.getStatus())) {
            return;
        }
        PurchaseOrderItems item = purchaseOrderItemsMapper.selectById(detail.getPurchaseItemId());
        if (item != null && !detail.getStatus().equals(String.valueOf(item.getStatus()))) {
            item.setStatus(detail.getStatus());
            purchaseOrderItemsMapper.updateById(item);
        }
    }

    /**
     * 追加单据备注（管道分隔）
     *
     * @param entity  验收单实体
     * @param content 追加内容
     */
    private void appendRemark(AcceptanceOrder entity, String content) {
        String oldRemark = entity.getRemark();
        entity.setRemark(StringUtils.hasText(oldRemark) ? oldRemark + " | " + content : content);
    }

    // endregion
}

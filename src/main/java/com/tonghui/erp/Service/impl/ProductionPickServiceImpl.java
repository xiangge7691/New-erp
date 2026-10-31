package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickBatchViewDto;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickIssueDetailRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickIssueRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.AvailableBatchDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.ProductionPickDetailMapper;
import com.tonghui.erp.Data.mapper.ProductionPickMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.ProductionPickService;
import com.tonghui.erp.Service.StockOutService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产领料单业务实现类
 * <p>
 * 实现生产领料单的查询、按生产任务预填、新增/修改/删除、发料与作废。
 * 发料时按库存批次生成“生产领料出库”草稿出库单（不扣库存），
 * 仓库在出库管理确认后由现有出库逻辑扣减库存；作废时根据出库单状态删除草稿单或回滚库存。
 * </p>
 */
@Service
public class ProductionPickServiceImpl extends ServiceImpl<ProductionPickMapper, ProductionPick>
        implements ProductionPickService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 生产领料单主表数据访问层 */
    private final ProductionPickMapper pickMapper;

    /** 生产领料单明细数据访问层 */
    private final ProductionPickDetailMapper detailMapper;

    /** 序列号生成服务 */
    private final SequenceServiceImpl sequenceService;

    /** 出库单服务（发料时生成草稿出库单，作废时删除/取消） */
    private final StockOutService stockOutService;

    /** 生产任务数据访问层 */
    private final WorkOrderMapper workOrderMapper;

    /** 生产计划数据访问层 */
    private final ProductionPlanMapper productionPlanMapper;

    /** 制剂处方数据访问层 */
    private final PreparationFormulaMapper preparationFormulaMapper;

    /** 物料主数据访问层（补全明细物料信息） */
    private final MaterialMapper materialMapper;

    /** 库存数据访问层（发料时分配库存批次） */
    private final StockMapper stockMapper;

    /**
     * 构造函数注入依赖
     */
    @Autowired
    public ProductionPickServiceImpl(ProductionPickMapper pickMapper,
                                     ProductionPickDetailMapper detailMapper,
                                     SequenceServiceImpl sequenceService,
                                     StockOutService stockOutService,
                                     WorkOrderMapper workOrderMapper,
                                     ProductionPlanMapper productionPlanMapper,
                                     PreparationFormulaMapper preparationFormulaMapper,
                                     MaterialMapper materialMapper,
                                     StockMapper stockMapper) {
        this.pickMapper = pickMapper;
        this.detailMapper = detailMapper;
        this.sequenceService = sequenceService;
        this.stockOutService = stockOutService;
        this.workOrderMapper = workOrderMapper;
        this.productionPlanMapper = productionPlanMapper;
        this.preparationFormulaMapper = preparationFormulaMapper;
        this.materialMapper = materialMapper;
        this.stockMapper = stockMapper;
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询生产领料单（含明细）
     *
     * @param keyword      关键字
     * @param status       状态
     * @param workOrderId  生产任务ID
     * @param stockOutCode 关联出库单号
     * @param startDate    领料时间起始日期
     * @param endDate      领料时间结束日期
     * @param pageIndex    页码（从0开始）
     * @param pageSize     每页数量
     * @return 分页结果
     */
    @Override
    public PagedResult<ProductionPickWithDetailsDto> queryPicks(String keyword, String status, Long workOrderId,
                                                                String stockOutCode, LocalDate startDate, LocalDate endDate,
                                                                int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : pageSize;
        Page<ProductionPick> page = new Page<>(safePageIndex + 1L, safePageSize);
        QueryWrapper<ProductionPick> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("pick_code", keyword)
                    .or().like("work_order_code", keyword)
                    .or().like("work_order_name", keyword)
                    .or().like("applicant", keyword)
                    .or().like("purpose", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (workOrderId != null) {
            wrapper.eq("work_order_id", workOrderId);
        }
        if (StringUtils.hasText(stockOutCode)) {
            wrapper.like("stock_out_code", stockOutCode);
        }
        if (startDate != null) {
            wrapper.ge("pick_time", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le("pick_time", endDate.atTime(23, 59, 59));
        }
        wrapper.orderByDesc("created_time");
        wrapper.orderByDesc("id");

        Page<ProductionPick> result = pickMapper.selectPage(page, wrapper);
        List<ProductionPick> records = result.getRecords();
        Map<Long, List<ProductionPickDetail>> detailsMap = loadDetailsMap(
                records.stream().map(ProductionPick::getId).filter(Objects::nonNull).collect(Collectors.toList()));
        Map<Long, String> stockOutStatusMap = loadStockOutStatusMap(records);

        List<ProductionPickWithDetailsDto> items = records.stream()
                .map(record -> toDto(record,
                        detailsMap.getOrDefault(record.getId(), List.of()),
                        stockOutStatusMap.get(record.getStockOutId())))
                .collect(Collectors.toList());

        PagedResult<ProductionPickWithDetailsDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(items);
        pagedResult.setTotalCount(result.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize(safePageSize);
        return pagedResult;
    }

    /**
     * 根据ID查询生产领料单详情（含明细）
     *
     * @param id 生产领料单ID
     * @return 领料单详情
     */
    @Override
    public ProductionPickWithDetailsDto getPickWithDetails(Long id) {
        if (id == null) {
            return null;
        }
        ProductionPick pick = pickMapper.selectById(id);
        if (pick == null) {
            return null;
        }
        List<ProductionPickDetail> details = getDetailsByPickId(id);
        String stockOutStatus = null;
        if (pick.getStockOutId() != null) {
            StockOut stockOut = stockOutService.getStockOutById(pick.getStockOutId());
            if (stockOut != null) {
                stockOutStatus = stockOut.getOutStatus();
            }
        }
        return toDto(pick, details, stockOutStatus);
    }

    /**
     * 根据生产任务生成领料预填数据（不落库）
     *
     * @param workOrderId 生产任务ID
     * @param multiplier  处方倍数
     * @return 预填数据
     */
    @Override
    public ProductionPickWithDetailsDto buildPrefillFromWorkOrder(Long workOrderId, BigDecimal multiplier) {
        if (workOrderId == null) {
            throw new IllegalArgumentException("生产任务ID不能为空");
        }
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new IllegalArgumentException("生产任务不存在");
        }
        BigDecimal safeMultiplier = multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : multiplier;

        ProductionPick pick = new ProductionPick();
        applyWorkOrderSnapshot(pick, workOrder);
        if (pick.getPlanId() != null) {
            applyPlanSnapshot(pick, productionPlanMapper.selectById(pick.getPlanId()));
        }
        pick.setPickTime(LocalDateTime.now());
        pick.setMultiplier(safeMultiplier);
        pick.setStatus("待发料");
        pick.setPurpose(buildDefaultPurpose(workOrder.getPreparationName(), workOrder.getBatchNumber()));

        QueryWrapper<PreparationFormula> formulaWrapper = new QueryWrapper<>();
        if (workOrder.getPreparationId() != null) {
            formulaWrapper.eq("preparation_id", workOrder.getPreparationId());
        } else if (StringUtils.hasText(workOrder.getPreparationCode())) {
            formulaWrapper.eq("preparation_code", workOrder.getPreparationCode());
        } else {
            throw new IllegalArgumentException("生产任务缺少制剂信息，无法带出处方");
        }
        formulaWrapper.orderByAsc("formula_id");
        List<PreparationFormula> formulas = preparationFormulaMapper.selectList(formulaWrapper);
        if (formulas.isEmpty()) {
            throw new IllegalArgumentException("该制剂未配置处方信息");
        }

        List<ProductionPickDetail> details = new ArrayList<>();
        int seq = 1;
        for (PreparationFormula formula : formulas) {
            ProductionPickDetail detail = new ProductionPickDetail();
            detail.setSeq(seq++);
            detail.setMaterialId(formula.getMaterialId());
            detail.setMaterialCode(formula.getMaterialCode());
            detail.setMaterialName(formula.getMaterialName());
            detail.setCategoryName(formula.getMaterialCategory());
            detail.setUnitName(formula.getUnitName());
            BigDecimal dosage = formula.getDosage() != null ? formula.getDosage() : BigDecimal.ZERO;
            detail.setApplyQty(dosage.multiply(safeMultiplier).setScale(4, RoundingMode.HALF_UP));
            detail.setIsDeleted(0);
            detail.setVersion(1);
            details.add(detail);
        }
        return toDto(pick, details, null);
    }

    /**
     * 查询某张领料单每条明细的可用库存批次
     *
     * @param pickId 生产领料单ID
     * @return 明细可用批次列表
     */
    @Override
    public List<ProductionPickBatchViewDto> getAvailableBatches(Long pickId) {
        ProductionPick pick = pickMapper.selectById(pickId);
        if (pick == null) {
            throw new IllegalArgumentException("生产领料单不存在");
        }
        List<ProductionPickDetail> details = getDetailsByPickId(pickId);
        List<ProductionPickBatchViewDto> result = new ArrayList<>();
        for (ProductionPickDetail detail : details) {
            ProductionPickBatchViewDto view = new ProductionPickBatchViewDto();
            view.setDetailId(detail.getId());
            view.setMaterialCode(detail.getMaterialCode());
            view.setMaterialName(detail.getMaterialName());
            view.setUnitName(detail.getUnitName());
            view.setApplyQty(detail.getApplyQty());
            List<AvailableBatchDto> batches = stockOutService.getAvailableBatchesByMaterialCode(
                    detail.getMaterialCode(), detail.getApplyQty());
            // 过滤已过期批次（与发料分配规则保持一致）
            LocalDate today = LocalDate.now();
            view.setAvailableBatches(batches.stream()
                    .filter(b -> b.getExpiryDate() == null || !b.getExpiryDate().isBefore(today))
                    .collect(Collectors.toList()));
            result.add(view);
        }
        return result;
    }

    // endregion

    // region 新增与修改
    // ===================================
    // 新增与修改
    // ===================================

    /**
     * 新增生产领料单（含明细）
     *
     * @param pick    领料单主表信息
     * @param details 领料明细列表
     * @return 保存后的领料单
     */
    @Override
    @Transactional
    public ProductionPick createPick(ProductionPick pick, List<ProductionPickDetail> details) {
        if (pick == null) {
            throw new IllegalArgumentException("领料单信息不能为空");
        }
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("领料明细不能为空");
        }
        if (!StringUtils.hasText(pick.getApplicant())) {
            throw new IllegalArgumentException("领料人不能为空");
        }

        WorkOrder workOrder = resolveWorkOrder(pick.getWorkOrderId(), pick.getWorkOrderCode());
        applyWorkOrderSnapshot(pick, workOrder);
        if (pick.getPlanId() != null) {
            applyPlanSnapshot(pick, productionPlanMapper.selectById(pick.getPlanId()));
        }
        if (!StringUtils.hasText(pick.getPurpose())) {
            pick.setPurpose(buildDefaultPurpose(pick.getPreparationName(), pick.getBatchNumber()));
        }
        if (pick.getPickTime() == null) {
            pick.setPickTime(LocalDateTime.now());
        }
        if (pick.getMultiplier() == null || pick.getMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            pick.setMultiplier(BigDecimal.ONE);
        }

        pick.setId(null);
        pick.setStatus("待发料");
        pick.setStockOutId(null);
        pick.setStockOutCode(null);
        pick.setIsDeleted(0);
        pick.setVersion(1);

        // 单号由“当天最大值+1”生成，并发下可能重复，捕获唯一键冲突后重试
        int maxRetry = 3;
        for (int attempt = 1; ; attempt++) {
            pick.setPickCode(sequenceService.generateProductionPickCode());
            try {
                pickMapper.insert(pick);
                break;
            } catch (DuplicateKeyException ex) {
                pick.setId(null);
                if (attempt >= maxRetry) {
                    throw new IllegalArgumentException("领料单号生成冲突，请稍后重试");
                }
            }
        }

        saveDetails(pick.getId(), details);
        return pick;
    }

    /**
     * 修改生产领料单（含明细，仅待发料状态可修改）
     *
     * @param id      领料单ID
     * @param pick    领料单主表信息
     * @param details 领料明细列表
     */
    @Override
    @Transactional
    public void updatePick(Long id, ProductionPick pick, List<ProductionPickDetail> details) {
        ProductionPick existing = pickMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("生产领料单不存在");
        }
        if (!"待发料".equals(existing.getStatus())) {
            throw new IllegalArgumentException("仅待发料状态的领料单可编辑");
        }
        if (pick == null) {
            throw new IllegalArgumentException("领料单信息不能为空");
        }

        // 生产任务变更时重新快照任务/计划/制剂信息
        Long targetWorkOrderId = pick.getWorkOrderId() != null ? pick.getWorkOrderId() : existing.getWorkOrderId();
        String targetWorkOrderCode = StringUtils.hasText(pick.getWorkOrderCode())
                ? pick.getWorkOrderCode() : existing.getWorkOrderCode();
        WorkOrder workOrder = resolveWorkOrder(targetWorkOrderId, targetWorkOrderCode);
        boolean workOrderChanged = !Objects.equals(existing.getWorkOrderId(), workOrder.getWorkOrderId());
        if (workOrderChanged) {
            existing.setPlanId(null);
            existing.setPlanNumber(null);
            existing.setPreparationId(null);
            existing.setPreparationCode(null);
            existing.setPreparationName(null);
            existing.setBatchNumber(null);
        }
        applyWorkOrderSnapshot(existing, workOrder);
        if (workOrderChanged || existing.getPlanId() != null) {
            applyPlanSnapshot(existing, existing.getPlanId() != null
                    ? productionPlanMapper.selectById(existing.getPlanId()) : null);
        }

        // 只覆盖客户端显式传入的字段
        if (StringUtils.hasText(pick.getBatchNumber())) {
            existing.setBatchNumber(pick.getBatchNumber());
        }
        if (StringUtils.hasText(pick.getWorkshop())) {
            existing.setWorkshop(pick.getWorkshop());
        }
        if (pick.getPickTime() != null) {
            existing.setPickTime(pick.getPickTime());
        }
        if (StringUtils.hasText(pick.getPurpose())) {
            existing.setPurpose(pick.getPurpose());
        }
        if (StringUtils.hasText(pick.getApplicant())) {
            existing.setApplicant(pick.getApplicant());
        }
        if (StringUtils.hasText(pick.getReviewer())) {
            existing.setReviewer(pick.getReviewer());
        }
        if (pick.getMultiplier() != null && pick.getMultiplier().compareTo(BigDecimal.ZERO) > 0) {
            existing.setMultiplier(pick.getMultiplier());
        }
        if (pick.getRemark() != null) {
            existing.setRemark(pick.getRemark());
        }
        if (!StringUtils.hasText(existing.getPurpose())) {
            existing.setPurpose(buildDefaultPurpose(existing.getPreparationName(), existing.getBatchNumber()));
        }
        touchForUpdate(existing);
        pickMapper.updateById(existing);

        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("领料明细不能为空");
        }
        detailMapper.physicalDeleteByPickId(id);
        saveDetails(id, details);
    }

    // endregion

    // region 发料与作废
    // ===================================
    // 发料与作废
    // ===================================

    /**
     * 发料确认（生成草稿出库单，不扣库存）
     *
     * @param id      领料单ID
     * @param request 发料请求
     * @return 发料后的领料单
     */
    @Override
    @Transactional
    public ProductionPick issuePick(Long id, ProductionPickIssueRequest request) {
        ProductionPick pick = pickMapper.selectById(id);
        if (pick == null) {
            throw new IllegalArgumentException("生产领料单不存在");
        }
        if (!"待发料".equals(pick.getStatus())) {
            throw new IllegalArgumentException("仅待发料状态的领料单可发料");
        }
        if (request == null) {
            throw new IllegalArgumentException("发料信息不能为空");
        }
        if (!StringUtils.hasText(request.getReviewer())) {
            throw new IllegalArgumentException("审核人不能为空");
        }
        if (!StringUtils.hasText(request.getIssuer())) {
            throw new IllegalArgumentException("发料人不能为空");
        }
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new IllegalArgumentException("发料明细不能为空");
        }

        List<ProductionPickDetail> details = getDetailsByPickId(id);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("领料单没有明细，无法发料");
        }
        Map<Long, ProductionPickIssueDetailRequest> requestMap = new HashMap<>();
        for (ProductionPickIssueDetailRequest item : request.getDetails()) {
            if (item == null || item.getDetailId() == null) {
                throw new IllegalArgumentException("发料明细缺少明细ID");
            }
            if (requestMap.put(item.getDetailId(), item) != null) {
                throw new IllegalArgumentException("发料明细存在重复的明细ID: " + item.getDetailId());
            }
        }
        // 拒绝不属于当前领料单的明细ID
        Set<Long> currentDetailIds = details.stream()
                .map(ProductionPickDetail::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Long detailId : requestMap.keySet()) {
            if (!currentDetailIds.contains(detailId)) {
                throw new IllegalArgumentException("发料明细ID不属于当前领料单: " + detailId);
            }
        }

        List<StockOutDetail> outboundDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        // 同一张领料单内可能存在相同物料的多行明细，需记录已占用批次数量，避免重复分配同一批次
        Map<Long, BigDecimal> reservedStockQty = new HashMap<>();
        for (ProductionPickDetail detail : details) {
            ProductionPickIssueDetailRequest item = requestMap.get(detail.getId());
            if (item == null) {
                throw new IllegalArgumentException("请填写物料[" + detail.getMaterialName() + "]的实发数量");
            }
            BigDecimal actualQty = item.getActualQty();
            if (actualQty == null || actualQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]实发数量必须大于0");
            }
            if (detail.getApplyQty() != null && actualQty.compareTo(detail.getApplyQty()) > 0) {
                throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]实发数量不能超过请领数量");
            }

            List<StockAllocation> allocations = allocateStock(detail, actualQty, item.getStockId(), reservedStockQty);
            for (StockAllocation allocation : allocations) {
                StockOutDetail outboundDetail = buildStockOutDetail(detail, allocation);
                outboundDetails.add(outboundDetail);
                if (outboundDetail.getAmount() != null) {
                    totalAmount = totalAmount.add(outboundDetail.getAmount());
                }
            }

            // 回写明细实发信息
            detail.setActualQty(actualQty);
            detail.setStockId(allocations.get(0).getStock().getStockId());
            detail.setBatchNumber(joinBatchNumbers(allocations));
            detail.setUnitPrice(calculateWeightedPrice(allocations));
            detailMapper.updateById(detail);
        }

        // 生成草稿出库单（不扣库存）
        StockOut stockOut = new StockOut();
        stockOut.setOutType("生产领料出库");
        stockOut.setRelatedOrder(pick.getPickCode());
        stockOut.setPlanId(pick.getPlanId());
        stockOut.setPlanNumber(pick.getPlanNumber());
        stockOut.setOutDate(LocalDateTime.now());
        stockOut.setTotalAmount(totalAmount);
        stockOut.setRemark(buildOutboundRemark(pick));
        StockOut savedStockOut = stockOutService.createDraftOutboundWithDetails(stockOut, outboundDetails);

        // 回写领料单发料信息
        pick.setStatus("已发料");
        pick.setReviewer(request.getReviewer());
        pick.setReviewerTime(LocalDateTime.now());
        pick.setIssuer(request.getIssuer());
        pick.setIssueTime(LocalDateTime.now());
        pick.setStockOutId(savedStockOut.getOutId());
        pick.setStockOutCode(savedStockOut.getOutCode());
        touchForUpdate(pick);
        pickMapper.updateById(pick);
        return pick;
    }

    /**
     * 作废生产领料单
     *
     * @param id 领料单ID
     */
    @Override
    @Transactional
    public void cancelPick(Long id) {
        ProductionPick pick = pickMapper.selectById(id);
        if (pick == null) {
            throw new IllegalArgumentException("生产领料单不存在");
        }
        if ("已作废".equals(pick.getStatus())) {
            throw new IllegalArgumentException("领料单已作废，无需重复操作");
        }

        if (pick.getStockOutId() != null) {
            StockOut stockOut = stockOutService.getStockOutById(pick.getStockOutId());
            if (stockOut != null) {
                String outStatus = stockOut.getOutStatus();
                if ("草稿".equals(outStatus)) {
                    // 草稿出库单未扣库存，直接删除
                    stockOutService.deleteStockOut(stockOut.getOutId());
                } else if ("已出库".equals(outStatus)) {
                    // 已出库需回滚库存
                    stockOutService.cancelStockOut(stockOut.getOutId());
                } else if (!"已取消".equals(outStatus)) {
                    throw new IllegalArgumentException("关联出库单状态为[" + outStatus + "]，不允许作废领料单");
                }
            }
        }

        pick.setStatus("已作废");
        touchForUpdate(pick);
        pickMapper.updateById(pick);
    }

    /**
     * 删除生产领料单（仅待发料状态，物理删除）
     *
     * @param id 领料单ID
     */
    @Override
    @Transactional
    public void deletePick(Long id) {
        ProductionPick pick = pickMapper.selectById(id);
        if (pick == null) {
            throw new IllegalArgumentException("生产领料单不存在");
        }
        if (!"待发料".equals(pick.getStatus())) {
            throw new IllegalArgumentException("仅待发料状态的领料单可删除");
        }
        detailMapper.physicalDeleteByPickId(id);
        pickMapper.physicalDeleteById(id);
    }

    /**
     * 生成生产领料单号
     *
     * @return 领料单号
     */
    @Override
    public String generatePickCode() {
        return sequenceService.generateProductionPickCode();
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 根据ID或编码定位生产任务并校验状态
     *
     * @param workOrderId   生产任务ID（优先）
     * @param workOrderCode 生产任务编号（ID为空时使用）
     * @return 生产任务
     */
    private WorkOrder resolveWorkOrder(Long workOrderId, String workOrderCode) {
        WorkOrder workOrder = null;
        if (workOrderId != null) {
            workOrder = workOrderMapper.selectById(workOrderId);
        }
        if (workOrder == null && StringUtils.hasText(workOrderCode)) {
            workOrder = workOrderMapper.selectOne(new QueryWrapper<WorkOrder>()
                    .eq("work_order_code", workOrderCode));
        }
        if (workOrder == null) {
            throw new IllegalArgumentException("生产任务不存在");
        }
        if ("已作废".equals(workOrder.getCurrentStatus()) || "已归档".equals(workOrder.getCurrentStatus())) {
            throw new IllegalArgumentException("生产任务已作废或已归档，不能发起领料");
        }
        return workOrder;
    }

    /**
     * 将生产任务信息快照到领料单
     *
     * @param pick      领料单
     * @param workOrder 生产任务
     */
    private void applyWorkOrderSnapshot(ProductionPick pick, WorkOrder workOrder) {
        pick.setWorkOrderId(workOrder.getWorkOrderId());
        pick.setWorkOrderCode(workOrder.getWorkOrderCode());
        pick.setWorkOrderName(workOrder.getWorkOrderName());
        // 生产任务为权威来源：计划/制剂信息始终以任务为准
        pick.setPlanId(workOrder.getPlanId());
        pick.setPreparationId(workOrder.getPreparationId());
        pick.setPreparationCode(workOrder.getPreparationCode());
        pick.setPreparationName(workOrder.getPreparationName());
        if (!StringUtils.hasText(pick.getBatchNumber())) {
            pick.setBatchNumber(workOrder.getBatchNumber());
        }
    }

    /**
     * 将生产计划信息快照到领料单
     *
     * @param pick 领料单
     * @param plan 生产计划（可为空）
     */
    private void applyPlanSnapshot(ProductionPick pick, ProductionPlan plan) {
        if (plan == null) {
            return;
        }
        pick.setPlanId(plan.getId() != null ? plan.getId().longValue() : pick.getPlanId());
        if (!StringUtils.hasText(pick.getPlanNumber())) {
            pick.setPlanNumber(plan.getPlanNumber());
        }
        if (pick.getPreparationId() == null) {
            pick.setPreparationId(plan.getPreparationId());
        }
        if (!StringUtils.hasText(pick.getPreparationCode())) {
            pick.setPreparationCode(plan.getPreparationCode());
        }
        if (!StringUtils.hasText(pick.getPreparationName())) {
            pick.setPreparationName(plan.getPreparationName());
        }
    }

    /**
     * 构造默认用途（制剂名称+批号）
     *
     * @param preparationName 制剂名称
     * @param batchNumber     批号
     * @return 用途文本
     */
    private String buildDefaultPurpose(String preparationName, String batchNumber) {
        if (!StringUtils.hasText(preparationName)) {
            return null;
        }
        return StringUtils.hasText(batchNumber)
                ? preparationName + "（" + batchNumber + "）"
                : preparationName;
    }

    /**
     * 保存领料明细（校验物料信息并补全缺失字段）
     *
     * @param pickId  领料单ID
     * @param details 明细列表
     */
    private void saveDetails(Long pickId, List<ProductionPickDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("领料明细不能为空");
        }
        fillMaterialInfo(details);
        int seq = 1;
        for (ProductionPickDetail detail : details) {
            if (detail.getApplyQty() == null || detail.getApplyQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]请领数量必须大于0");
            }
            if (!StringUtils.hasText(detail.getMaterialCode())) {
                throw new IllegalArgumentException("领料明细缺少物料编码");
            }
            if (!StringUtils.hasText(detail.getMaterialName())) {
                throw new IllegalArgumentException("领料明细缺少物料名称");
            }
            if (!StringUtils.hasText(detail.getUnitName())) {
                throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]缺少计量单位");
            }
            detail.setId(null);
            detail.setPickId(pickId);
            detail.setSeq(seq++);
            detail.setIsDeleted(0);
            if (detail.getVersion() == null) {
                detail.setVersion(1);
            }
            detailMapper.insert(detail);
        }
    }

    /**
     * 按物料编码/ID批量补全明细的物料名称、分类与单位
     *
     * @param details 明细列表（原地修改）
     */
    private void fillMaterialInfo(List<ProductionPickDetail> details) {
        Set<String> codes = details.stream()
                .filter(d -> StringUtils.hasText(d.getMaterialCode()))
                .map(ProductionPickDetail::getMaterialCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Material> codeMap = new HashMap<>();
        if (!codes.isEmpty()) {
            codeMap = materialMapper.selectList(new QueryWrapper<Material>().in("material_code", codes)).stream()
                    .collect(Collectors.toMap(Material::getMaterialCode, m -> m, (a, b) -> a));
        }
        for (ProductionPickDetail detail : details) {
            Material material = null;
            if (StringUtils.hasText(detail.getMaterialCode())) {
                material = codeMap.get(detail.getMaterialCode());
            }
            if (material == null && detail.getMaterialId() != null) {
                material = materialMapper.selectById(detail.getMaterialId());
            }
            if (material == null) {
                continue;
            }
            if (detail.getMaterialId() == null) {
                detail.setMaterialId(material.getMaterialId());
            }
            if (!StringUtils.hasText(detail.getMaterialCode())) {
                detail.setMaterialCode(material.getMaterialCode());
            }
            if (!StringUtils.hasText(detail.getMaterialName())) {
                detail.setMaterialName(material.getMaterialName());
            }
            if (!StringUtils.hasText(detail.getCategoryName())) {
                detail.setCategoryName(material.getCategoryName());
            }
            if (!StringUtils.hasText(detail.getUnitName())) {
                detail.setUnitName(material.getUnitName());
            }
        }
    }

    /**
     * 查询领料单明细
     *
     * @param pickId 领料单ID
     * @return 明细列表
     */
    private List<ProductionPickDetail> getDetailsByPickId(Long pickId) {
        QueryWrapper<ProductionPickDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("pick_id", pickId);
        wrapper.orderByAsc("seq");
        wrapper.orderByAsc("id");
        return detailMapper.selectList(wrapper);
    }

    /**
     * 批量加载领料单明细
     *
     * @param pickIds 领料单ID集合
     * @return 领料单ID → 明细列表
     */
    private Map<Long, List<ProductionPickDetail>> loadDetailsMap(List<Long> pickIds) {
        if (pickIds == null || pickIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<ProductionPickDetail> wrapper = new QueryWrapper<>();
        wrapper.in("pick_id", pickIds);
        wrapper.orderByAsc("seq");
        wrapper.orderByAsc("id");
        return detailMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(ProductionPickDetail::getPickId));
    }

    /**
     * 批量加载关联出库单状态
     *
     * @param picks 领料单列表
     * @return 出库单ID → 出库单状态
     */
    private Map<Long, String> loadStockOutStatusMap(List<ProductionPick> picks) {
        List<Long> stockOutIds = picks.stream()
                .map(ProductionPick::getStockOutId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (stockOutIds.isEmpty()) {
            return Map.of();
        }
        return stockOutService.listByIds(stockOutIds).stream()
                .filter(s -> s.getOutId() != null)
                .collect(Collectors.toMap(StockOut::getOutId, StockOut::getOutStatus, (a, b) -> a));
    }

    /**
     * 分配库存批次
     * <p>
     * 指定 stockId 时校验该批次；未指定时按物料编码查询合格、未过期、有库存的批次，
     * 按入库时间 FIFO 依次分配，不足时抛出异常。
     * </p>
     *
     * @param detail           领料明细
     * @param actualQty        实发数量
     * @param stockId          指定库存批次ID（可空）
     * @param reservedStockQty 已占用批次数量（同一单据内累计）
     * @return 批次分配结果
     */
    private List<StockAllocation> allocateStock(ProductionPickDetail detail, BigDecimal actualQty, Long stockId,
                                                Map<Long, BigDecimal> reservedStockQty) {
        List<StockAllocation> allocations = new ArrayList<>();
        if (stockId != null) {
            Stock stock = stockMapper.selectById(stockId);
            BigDecimal reserved = reservedStockQty.getOrDefault(stockId, BigDecimal.ZERO);
            validateStock(stock, detail, actualQty, reserved);
            allocations.add(new StockAllocation(stock, actualQty));
            reservedStockQty.merge(stockId, actualQty, BigDecimal::add);
            return allocations;
        }

        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("item_code", detail.getMaterialCode());
        wrapper.eq("stock_status", "合格");
        wrapper.gt("quantity", 0);
        wrapper.and(w -> w.isNull("expiry_date").or().ge("expiry_date", LocalDate.now()));
        wrapper.orderByAsc("created_time");
        wrapper.orderByAsc("stock_id");
        List<Stock> stocks = stockMapper.selectList(wrapper);

        BigDecimal remaining = actualQty;
        for (Stock stock : stocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal reserved = reservedStockQty.getOrDefault(stock.getStockId(), BigDecimal.ZERO);
            BigDecimal available = stock.getQuantity() == null
                    ? BigDecimal.ZERO : stock.getQuantity().subtract(reserved);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal take = available.min(remaining);
            allocations.add(new StockAllocation(stock, take));
            reservedStockQty.merge(stock.getStockId(), take, BigDecimal::add);
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]库存不足，仍有 "
                    + remaining.stripTrailingZeros().toPlainString() + " 未分配");
        }
        return allocations;
    }

    /**
     * 校验指定库存批次是否可用于发料
     *
     * @param stock     库存批次
     * @param detail    领料明细
     * @param actualQty 实发数量
     * @param reserved  同一单据内该批次已占用数量
     */
    private void validateStock(Stock stock, ProductionPickDetail detail, BigDecimal actualQty, BigDecimal reserved) {
        if (stock == null) {
            throw new IllegalArgumentException("库存批次不存在");
        }
        if (!StringUtils.hasText(detail.getMaterialCode())
                || !detail.getMaterialCode().equalsIgnoreCase(stock.getItemCode())) {
            throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]与所选库存批次不匹配");
        }
        if (!"合格".equals(stock.getStockStatus() == null ? null : String.valueOf(stock.getStockStatus()))) {
            throw new IllegalArgumentException("库存批次状态不是合格，不能发料: " + stock.getBatchNumber());
        }
        if (stock.getExpiryDate() != null && stock.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("库存批次已过期，不能发料: " + stock.getBatchNumber());
        }
        BigDecimal available = stock.getQuantity() == null
                ? BigDecimal.ZERO : stock.getQuantity().subtract(reserved);
        if (available.compareTo(actualQty) < 0) {
            throw new IllegalArgumentException("物料[" + detail.getMaterialName() + "]所选批次库存不足（当前 "
                    + available.toPlainString() + "，需发料 " + actualQty.toPlainString() + "）");
        }
    }

    /**
     * 构造出库明细
     *
     * @param detail     领料明细
     * @param allocation 批次分配结果
     * @return 出库明细
     */
    private StockOutDetail buildStockOutDetail(ProductionPickDetail detail, StockAllocation allocation) {
        Stock stock = allocation.getStock();
        StockOutDetail outboundDetail = new StockOutDetail();
        outboundDetail.setStockId(stock.getStockId());
        outboundDetail.setProdUnitId(stock.getProdUnitId());
        outboundDetail.setItemType("material");
        outboundDetail.setItemId(detail.getMaterialId());
        outboundDetail.setItemCode(detail.getMaterialCode());
        outboundDetail.setItemName(detail.getMaterialName());
        outboundDetail.setCategoryName(StringUtils.hasText(stock.getCategoryName())
                ? stock.getCategoryName()
                : (StringUtils.hasText(detail.getCategoryName()) ? detail.getCategoryName() : "物料"));
        outboundDetail.setUnitName(StringUtils.hasText(stock.getUnitName()) ? stock.getUnitName() : detail.getUnitName());
        outboundDetail.setBatchNumber(stock.getBatchNumber());
        outboundDetail.setQuantity(allocation.getQuantity());
        outboundDetail.setUnitPrice(stock.getUnitPrice());
        if (stock.getUnitPrice() != null) {
            outboundDetail.setAmount(allocation.getQuantity()
                    .multiply(stock.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        outboundDetail.setRemark(detail.getRemark());
        outboundDetail.setIsDeleted(0);
        outboundDetail.setVersion(1);
        return outboundDetail;
    }

    /**
     * 拼接实发批次号（多批次逗号分隔）
     *
     * @param allocations 批次分配结果
     * @return 批次号文本
     */
    private String joinBatchNumbers(List<StockAllocation> allocations) {
        return allocations.stream()
                .map(a -> a.getStock().getBatchNumber())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /**
     * 计算加权平均单价
     *
     * @param allocations 批次分配结果
     * @return 加权平均单价，无单价时返回 null
     */
    private BigDecimal calculateWeightedPrice(List<StockAllocation> allocations) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (StockAllocation allocation : allocations) {
            BigDecimal price = allocation.getStock().getUnitPrice();
            if (price == null) {
                continue;
            }
            totalAmount = totalAmount.add(allocation.getQuantity().multiply(price));
            totalQty = totalQty.add(allocation.getQuantity());
        }
        return totalQty.compareTo(BigDecimal.ZERO) > 0
                ? totalAmount.divide(totalQty, 4, RoundingMode.HALF_UP)
                : null;
    }

    /**
     * 构造出库单备注
     *
     * @param pick 领料单
     * @return 备注文本
     */
    private String buildOutboundRemark(ProductionPick pick) {
        StringBuilder remark = new StringBuilder("生产领料单[").append(pick.getPickCode()).append("]");
        if (StringUtils.hasText(pick.getWorkOrderCode())) {
            remark.append(" 生产任务[").append(pick.getWorkOrderCode());
            if (StringUtils.hasText(pick.getWorkOrderName())) {
                remark.append(" ").append(pick.getWorkOrderName());
            }
            remark.append("]");
        }
        if (StringUtils.hasText(pick.getPurpose())) {
            remark.append(" 用途:").append(pick.getPurpose());
        }
        return remark.toString();
    }

    /**
     * 更新时手动补充审计字段（DB加载实体 strictUpdateFill 不会覆盖非null值）
     *
     * @param pick 领料单
     */
    private void touchForUpdate(ProductionPick pick) {
        pick.setUpdatedTime(LocalDateTime.now());
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            pick.setUpdatedBy(currentUserId);
        }
    }

    /**
     * 转换为带明细的DTO
     *
     * @param pick            领料单主表
     * @param details         明细列表
     * @param stockOutStatus  出库单状态
     * @return DTO
     */
    private ProductionPickWithDetailsDto toDto(ProductionPick pick, List<ProductionPickDetail> details,
                                               String stockOutStatus) {
        ProductionPickWithDetailsDto dto = new ProductionPickWithDetailsDto();
        BeanUtils.copyProperties(pick, dto);
        dto.setDetails(details == null ? List.of() : details);
        dto.setStockOutStatus(stockOutStatus);
        return dto;
    }

    /**
     * 库存批次分配内部类
     */
    private static class StockAllocation {

        /** 库存批次 */
        private final Stock stock;

        /** 本批次分配数量 */
        private final BigDecimal quantity;

        StockAllocation(Stock stock, BigDecimal quantity) {
            this.stock = stock;
            this.quantity = quantity;
        }

        Stock getStock() {
            return stock;
        }

        BigDecimal getQuantity() {
            return quantity;
        }
    }

    // endregion
}

package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.BatchContext;
import com.tonghui.erp.Common.Dto.QualityPages.ReleaseBatchDtos;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileRequirementConfig;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.InspectionTask;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.ReturnOrderDetail;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessCheck;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessCheckMapper;
import com.tonghui.erp.Service.BatchQueryService;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileRequirementService;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Service.ReleaseBatchService;
import com.tonghui.erp.Service.ReleaseReviewService;
import com.tonghui.erp.Service.UserService;
import com.tonghui.erp.Service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 批次审核放行服务实现
 */
@Service
public class ReleaseBatchServiceImpl implements ReleaseBatchService {

    private static final String RESULT_PASS = "符合";
    private static final String RESULT_FAIL = "不符合";
    private static final String RESULT_PENDING = "待查";
    private static final String CONCLUSION_RELEASE = "放行";
    private static final String CONCLUSION_REJECT = "拒绝放行";
    private static final String CONCLUSION_DRAFT = "待补充";
    private static final BigDecimal YIELD_THRESHOLD = new BigDecimal("98");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 放行页文件清单只统计批次相关业务类型，品种级资料不计入放行缺件 */
    private static final Set<String> RELEASE_FILE_BIZ_TYPES = Set.of(
            "PRODUCTION_PLAN", "PURCHASE_ORDER", "ACCEPTANCE_ORDER", "WORK_ORDER",
            "WORK_ORDER_PROCESS_EXECUTION", "STOCK_OUT_PRODUCTION", "STOCK_IN_PRODUCT",
            "STOCK_OUT_SALES", "INSPECTION_REQUEST", "SAMPLING_RECORD", "INSPECTION_RECORD",
            "RELEASE_REVIEW");

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private BatchQueryService batchQueryService;
    @Autowired
    private BusinessFileService businessFileService;
    @Autowired
    private FileRequirementService fileRequirementService;
    @Autowired
    private WorkOrderProcessCheckMapper processCheckMapper;
    @Autowired
    private ReleaseReviewService releaseReviewService;
    @Autowired
    private SequenceServiceImpl sequenceService;
    @Autowired
    private ProductionPlanService productionPlanService;
    @Autowired
    private UserService userService;
    @Autowired
    private WorkOrderService workOrderService;

    // region 列表
    // ===================================
    // 列表
    // ===================================

    @Override
    public PagedResult<ReleaseBatchDtos.BatchSummary> queryBatches(String varietyCode, String batchNo,
                                                                   String status, String keyword,
                                                                   int pageIndex, int pageSize) {
        List<ReleaseBatchDtos.BatchSummary> all = loadSummaries(varietyCode, batchNo, status, keyword);
        int safePageSize = Math.max(pageSize, 1);
        int from = Math.min(Math.max(pageIndex, 0) * safePageSize, all.size());
        int to = Math.min(from + safePageSize, all.size());

        PagedResult<ReleaseBatchDtos.BatchSummary> result = new PagedResult<>();
        result.setItems(all.subList(from, to));
        result.setTotalCount(all.size());
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public ReleaseBatchDtos.BoardStats queryBoardStats(String varietyCode, String batchNo,
                                                       String status, String keyword) {
        // 看板统计基于全量结果，不跟随列表的状态筛选变化
        List<ReleaseBatchDtos.BatchSummary> all = loadSummaries(varietyCode, batchNo, null, keyword);
        ReleaseBatchDtos.BoardStats stats = new ReleaseBatchDtos.BoardStats();
        stats.setTotal(all.size());
        for (ReleaseBatchDtos.BatchSummary item : all) {
            String releaseStatus = item.getReleaseStatus();
            if ("待审核".equals(releaseStatus)) {
                stats.setPendingReview(stats.getPendingReview() + 1);
            }
            if ("待放行".equals(releaseStatus) && item.isCanRelease()) {
                stats.setReleasable(stats.getReleasable() + 1);
            }
            if (!"已放行".equals(releaseStatus) && !item.isCanRelease()) {
                stats.setBlocked(stats.getBlocked() + 1);
            }
            if ("已放行".equals(releaseStatus)) {
                stats.setReleased(stats.getReleased() + 1);
            }
            if ("已拒绝".equals(releaseStatus)) {
                stats.setRejected(stats.getRejected() + 1);
            }
        }
        return stats;
    }

    /**
     * 加载批次审核汇总，列表与看板共用
     */
    private List<ReleaseBatchDtos.BatchSummary> loadSummaries(String varietyCode, String batchNo,
                                                              String status, String keyword) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(varietyCode)) {
            wrapper.eq("preparation_code", varietyCode);
        }
        if (StringUtils.hasText(batchNo)) {
            List<Long> planIds = findPlanIds(batchNo);
            wrapper.and(w -> {
                w.like("batch_number", batchNo).or().like("work_order_code", batchNo);
                if (!planIds.isEmpty()) {
                    w.or().in("plan_id", planIds);
                }
            });
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> planIds = findPlanIds(keyword);
            wrapper.and(w -> {
                w.like("work_order_code", keyword)
                        .or().like("batch_number", keyword)
                        .or().like("preparation_name", keyword);
                if (!planIds.isEmpty()) {
                    w.or().in("plan_id", planIds);
                }
            });
        }
        wrapper.orderByDesc("created_time", "work_order_id");
        List<WorkOrder> workOrders = workOrderMapper.selectList(wrapper);

        List<ReleaseBatchDtos.BatchSummary> all = new ArrayList<>();
        for (BatchContext ctx : batchQueryService.loadAll(workOrders)) {
            ReleaseBatchDtos.BatchSummary dto = toSummary(ctx);
            if (!StringUtils.hasText(status) || status.equals(dto.getReleaseStatus())) {
                all.add(dto);
            }
        }
        return all;
    }

    /**
     * 按计划号模糊查询生产计划ID
     * <p>work_order.plan_number 不是表字段，计划号搜索需通过 production_plan 反查。</p>
     *
     * @param text 计划号关键字
     * @return 匹配的计划ID列表
     */
    private List<Long> findPlanIds(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        return productionPlanService.list(new QueryWrapper<ProductionPlan>().like("plan_number", text))
                .stream()
                .map(plan -> plan.getId() == null ? null : plan.getId().longValue())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ReleaseBatchDtos.BatchSummary toSummary(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        ReleaseBatchDtos.BatchSummary dto = new ReleaseBatchDtos.BatchSummary();
        dto.setWorkOrderId(wo.getWorkOrderId());
        dto.setWorkOrderCode(wo.getWorkOrderCode());
        dto.setPlanNumber(ctx.getPlan() == null ? null : ctx.getPlan().getPlanNumber());
        dto.setBatchNo(wo.getBatchNumber());
        dto.setPreparationCode(wo.getPreparationCode());
        dto.setPreparationName(wo.getPreparationName());
        dto.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        dto.setUnitName(unitName(ctx));
        dto.setPlanQty(wo.getBatchQty());
        dto.setFinishedQty(wo.getFinishedQty());
        dto.setPlanDate(planDate(ctx));
        dto.setActualStart(actualStart(ctx));
        dto.setActualEnd(actualEnd(ctx));
        dto.setYieldRate(yieldRate(ctx));
        dto.setInspectionConclusion(inspectionConclusion(ctx));

        ProcessStat process = processStat(ctx);
        dto.setProcessTotal(process.total);
        dto.setProcessPass(process.pass);
        dto.setProcessFail(process.fail);
        dto.setProcessPending(process.pending);

        FileStat file = fileStat(ctx);
        dto.setFileUploaded(file.uploaded);
        dto.setFileRequired(file.required);
        dto.setFileMissing(Math.max(0, file.required - file.uploaded));

        dto.setDeviationTotal(ctx.getDeviations().size());
        int openDeviations = (int) ctx.getDeviations().stream().filter(this::deviationOpen).count();
        dto.setDeviationOpen(openDeviations);
        dto.setReleaseStatus(releaseStatus(ctx));
        dto.setReleaseApprover(releaseApprover(ctx));
        dto.setReleaseDate(releaseDate(ctx));
        dto.setCanRelease(compliance(ctx).isCanRelease());
        dto.setBlockers(buildBlockers(dto, openDeviations));
        return dto;
    }

    /**
     * 生成列表用阻塞项，避免前端为每个批次再查一次详情
     */
    private List<String> buildBlockers(ReleaseBatchDtos.BatchSummary dto, int openDeviations) {
        List<String> blockers = new ArrayList<>();
        if (!"合格".equals(dto.getInspectionConclusion())) {
            blockers.add("检验" + (StringUtils.hasText(dto.getInspectionConclusion())
                    ? " " + dto.getInspectionConclusion() : "未完成"));
        }
        if (dto.getProcessFail() > 0 || dto.getProcessPending() > 0) {
            blockers.add("工序 " + dto.getProcessPass() + "/" + dto.getProcessTotal());
        }
        if (dto.getFileMissing() > 0) {
            blockers.add("文件缺 " + dto.getFileMissing());
        }
        if (openDeviations > 0) {
            blockers.add("偏差 " + openDeviations + " 未关闭");
        }
        if (dto.getYieldRate() == null) {
            blockers.add("收率未录入");
        } else if (dto.getYieldRate().compareTo(YIELD_THRESHOLD) < 0) {
            blockers.add("收率 " + dto.getYieldRate().stripTrailingZeros().toPlainString() + "%");
        }
        return blockers;
    }

    // endregion

    // region 详情
    // ===================================
    // 详情
    // ===================================

    @Override
    public ReleaseBatchDtos.BatchDetail getBatchDetail(Long workOrderId) {
        WorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        BatchContext ctx = batchQueryService.load(wo);
        return toDetail(ctx);
    }

    private ReleaseBatchDtos.BatchDetail toDetail(BatchContext ctx) {
        batchQueryService.loadDetail(ctx);
        WorkOrder wo = ctx.getWorkOrder();
        ReleaseBatchDtos.BatchDetail detail = new ReleaseBatchDtos.BatchDetail();

        ReleaseBatchDtos.BatchInfo batch = new ReleaseBatchDtos.BatchInfo();
        batch.setWorkOrderId(wo.getWorkOrderId());
        batch.setWorkOrderCode(wo.getWorkOrderCode());
        batch.setPlanNumber(ctx.getPlan() == null ? null : ctx.getPlan().getPlanNumber());
        batch.setBatchNo(wo.getBatchNumber());
        batch.setPreparationCode(wo.getPreparationCode());
        batch.setPreparationName(wo.getPreparationName());
        batch.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        batch.setUnitName(unitName(ctx));
        batch.setPlanQty(wo.getBatchQty());
        batch.setFinishedQty(wo.getFinishedQty());
        batch.setPlanDate(planDate(ctx));
        batch.setActualStart(actualStart(ctx));
        batch.setActualEnd(actualEnd(ctx));
        batch.setYieldRate(yieldRate(ctx));
        batch.setInspectionConclusion(inspectionConclusion(ctx));
        batch.setWorkOrderStatus(wo.getCurrentStatus());
        batch.setReleaseStatus(releaseStatus(ctx));
        detail.setBatch(batch);

        detail.setPurchases(ctx.getPurchases().stream().map(p -> toPurchase(p, ctx)).collect(Collectors.toList()));
        detail.setHospitalRequisitions(ctx.getHospitalRequisitions().stream()
                .map(s -> toHospitalRequisition(s, ctx)).collect(Collectors.toList()));
        detail.setAcceptances(ctx.getAcceptances().stream().map(a -> toAcceptance(a, ctx)).collect(Collectors.toList()));
        detail.setInstructions(instruction(ctx));
        detail.setRequisitions(ctx.getPicks().stream().map(p -> toRequisition(p, ctx)).collect(Collectors.toList()));
        detail.setReturns(ctx.getReturns().stream().map(r -> toReturn(r, ctx)).collect(Collectors.toList()));
        detail.setProcesses(processItems(ctx));
        detail.setRequests(ctx.getRequests().stream().map(r -> toInspectionItem(r, "请检单", "INSPECTION_REQUEST", ctx)).collect(Collectors.toList()));
        detail.setSamplings(ctx.getSamplings().stream().map(s -> toInspectionItem(s, "取样记录", "SAMPLING_RECORD", ctx)).collect(Collectors.toList()));
        detail.setInspections(ctx.getInspections().stream().map(i -> toInspectionItem(i, "检验记录", "INSPECTION_RECORD", ctx)).collect(Collectors.toList()));
        detail.setRooms(roomItems(ctx));
        detail.setYieldInfo(yieldInfo(ctx));
        detail.setDeviations(ctx.getDeviations().stream().map(this::toDeviation).collect(Collectors.toList()));
        detail.setFileChecklist(fileChecklist(ctx));
        detail.setCompliance(compliance(ctx));
        detail.setRelease(releaseInfo(ctx));
        return detail;
    }

    private ReleaseBatchDtos.PurchaseItem toPurchase(PurchaseOrders po, BatchContext ctx) {
        ReleaseBatchDtos.PurchaseItem item = new ReleaseBatchDtos.PurchaseItem();
        item.setId(po.getId());
        item.setCode(po.getPurchaseNumber());
        item.setMaterialType(po.getMaterialType());
        item.setStatus(po.getStatus() == null ? null : String.valueOf(po.getStatus()));
        item.setDate(po.getOrderTime() == null ? null : po.getOrderTime().toLocalDate());
        item.setOrderTime(po.getOrderTime());
        item.setInvoiceNo(po.getInvoiceInfo());
        item.setRemark(po.getRemark());
        List<PurchaseOrderItems> lines = ctx.getPurchaseItems() == null ? Collections.emptyList()
                : ctx.getPurchaseItems().getOrDefault(po.getId(), Collections.emptyList());
        if (!lines.isEmpty()) {
            item.setSupplier(lines.stream().map(PurchaseOrderItems::getSupplier)
                    .filter(StringUtils::hasText).findFirst().orElse(null));
        }
        item.setLines(lines.stream().map(line -> toPurchaseLine(line, ctx)).collect(Collectors.toList()));
        List<BusinessFileDto> files = businessFileService.list("PURCHASE_ORDER", po.getId(), null, null);
        item.setFileCount(files == null ? 0 : files.size());
        item.setFiles(toFiles(files));
        return item;
    }

    /**
     * 组装采购明细行
     *
     * @param line 采购明细实体
     * @param ctx  批次上下文
     * @return 采购明细 DTO
     */
    private ReleaseBatchDtos.PurchaseLineItem toPurchaseLine(PurchaseOrderItems line, BatchContext ctx) {
        ReleaseBatchDtos.PurchaseLineItem item = new ReleaseBatchDtos.PurchaseLineItem();
        item.setMaterialCode(line.getMaterialCode());
        String materialName = line.getMaterialId() != null && ctx.getPurchaseMaterialNames() != null
                ? ctx.getPurchaseMaterialNames().get(line.getMaterialId()) : null;
        if (!StringUtils.hasText(materialName)) {
            materialName = StringUtils.hasText(line.getRawMaterialName())
                    ? line.getRawMaterialName() : line.getProductName();
        }
        item.setMaterialName(materialName);
        item.setSpec(line.getProcessingProperty());
        item.setQuantity(line.getPurchaseQuantity());
        item.setUnit(line.getUnit());
        item.setUnitPrice(line.getUnitPrice());
        item.setAmount(line.getAmount());
        item.setActualArrivalQty(line.getActualArrivalQty());
        item.setInvoiceNo(line.getInvoiceNo());
        item.setSupplier(line.getSupplier());
        return item;
    }

    /**
     * 组装医院领料单（与采购同级展示）
     *
     * @param slip 医院领料单实体
     * @param ctx  批次上下文
     * @return 医院领料单 DTO
     */
    private ReleaseBatchDtos.HospitalRequisitionItem toHospitalRequisition(MaterialRequisitionSlip slip,
                                                                           BatchContext ctx) {
        ReleaseBatchDtos.HospitalRequisitionItem item = new ReleaseBatchDtos.HospitalRequisitionItem();
        item.setId(slip.getId());
        item.setCode(slip.getSlipCode());
        item.setPlanCode(slip.getProductionPlanCode());
        item.setPlanTitle(slip.getProductionPlanTitle());
        item.setFromDept(slip.getFromDept());
        item.setToDept(slip.getToDept());
        item.setApplicant(slip.getApplicant());
        item.setApplyTime(slip.getApplyTime());
        item.setDate(slip.getApplyTime() == null ? null : slip.getApplyTime().toLocalDate());
        item.setMaterialType(slip.getMaterialType());
        item.setMultiplier(slip.getMultiplier());
        item.setStatus(slip.getStatus());
        item.setAcceptanceOrderId(slip.getAcceptanceOrderId());
        item.setRemark(slip.getRemark());
        List<MaterialRequisitionSlipDetail> lines = ctx.getHospitalRequisitionDetails() == null
                ? Collections.emptyList()
                : ctx.getHospitalRequisitionDetails().getOrDefault(slip.getId(), Collections.emptyList());
        item.setLines(lines.stream().map(this::toHospitalRequisitionLine).collect(Collectors.toList()));
        item.setFiles(toFiles(businessFileService.list("MATERIAL_REQUISITION_SLIP", slip.getId(), null, null)));
        return item;
    }

    /**
     * 组装医院领料明细行
     *
     * @param line 医院领料明细实体
     * @return 明细 DTO
     */
    private ReleaseBatchDtos.DocumentLineItem toHospitalRequisitionLine(MaterialRequisitionSlipDetail line) {
        ReleaseBatchDtos.DocumentLineItem item = new ReleaseBatchDtos.DocumentLineItem();
        item.setMaterialCode(line.getMaterialCode());
        item.setMaterialName(line.getMaterialName());
        item.setSpec(line.getCategoryName());
        item.setQuantity(line.getActualQty() != null ? line.getActualQty() : line.getApplyQty());
        item.setUnit(line.getUnitName());
        item.setBatchNumber(line.getBatchNumber());
        item.setUnitPrice(line.getUnitPrice());
        if (item.getQuantity() != null && line.getUnitPrice() != null) {
            item.setAmount(item.getQuantity().multiply(line.getUnitPrice()));
        }
        return item;
    }

    private ReleaseBatchDtos.AcceptanceItem toAcceptance(AcceptanceOrder acceptance, BatchContext ctx) {
        ReleaseBatchDtos.AcceptanceItem item = new ReleaseBatchDtos.AcceptanceItem();
        item.setId(acceptance.getAcceptanceId());
        item.setCode(acceptance.getAcceptanceCode());
        item.setStatus(acceptance.getStatus());
        item.setPurchaseNumber(acceptance.getPurchaseNumber());
        item.setSourceType(acceptance.getSourceType());
        item.setArrivalTime(acceptance.getArrivalTime());
        item.setAcceptanceTime(acceptance.getAcceptanceTime());
        item.setAcceptor(acceptance.getAcceptorName());
        item.setReviewer(acceptance.getReviewerName());
        item.setRemark(acceptance.getRemark());
        List<AcceptanceDetail> details = ctx.getAcceptanceDetails() == null
                ? Collections.emptyList()
                : ctx.getAcceptanceDetails().getOrDefault(acceptance.getAcceptanceId(), Collections.emptyList());
        item.setDetails(details.stream().map(this::toAcceptanceLine).collect(Collectors.toList()));
        List<BusinessFileDto> all = new ArrayList<>();
        for (String kind : List.of("WAYBILL", "INSPECTION_REPORT", "INVOICE")) {
            List<BusinessFileDto> files = businessFileService.list("ACCEPTANCE_ORDER", acceptance.getAcceptanceId(), null, kind);
            if (files != null && !files.isEmpty()) {
                item.getUploadedKinds().add(kind);
                item.getUploadedKindLabels().add(acceptanceKindLabel(kind));
                all.addAll(files);
            } else {
                item.getMissingKinds().add(kind);
                item.getMissingKindLabels().add(acceptanceKindLabel(kind));
            }
        }
        item.setFiles(toFiles(all));
        return item;
    }

    /**
     * 组装验收明细行
     *
     * @param detail 验收明细实体
     * @return 验收明细 DTO
     */
    private ReleaseBatchDtos.AcceptanceLineItem toAcceptanceLine(AcceptanceDetail detail) {
        ReleaseBatchDtos.AcceptanceLineItem item = new ReleaseBatchDtos.AcceptanceLineItem();
        item.setMaterialCode(detail.getMaterialCode());
        item.setMaterialName(detail.getMaterialName());
        item.setSpec(detail.getMaterialCategory());
        item.setQuantity(detail.getQuantity());
        item.setUnit(detail.getUnitName());
        item.setBatchNumber(detail.getBatchNumber());
        item.setExpiryDate(detail.getExpiryDate());
        item.setAppearanceCheck(detail.getAppearanceCheck());
        item.setOuterPackageCheck(detail.getOuterPackageCheck());
        item.setConclusion(detail.getStatus());
        item.setSupplier(detail.getSupplier());
        item.setDiffQuantity(detail.getDiffQuantity());
        return item;
    }

    /**
     * 验收文件槽位中文名
     *
     * @param kind 文件槽位编码
     * @return 中文名
     */
    private String acceptanceKindLabel(String kind) {
        return switch (kind) {
            case "WAYBILL" -> "随货清单";
            case "INSPECTION_REPORT" -> "验收检验报告";
            case "INVOICE" -> "发票";
            default -> kind;
        };
    }

    private List<ReleaseBatchDtos.DocumentItem> instruction(BatchContext ctx) {
        if (ctx.getPlan() == null) {
            return Collections.emptyList();
        }
        ProductionPlan plan = ctx.getPlan();
        ReleaseBatchDtos.DocumentItem item = new ReleaseBatchDtos.DocumentItem();
        item.setId(plan.getId().longValue());
        item.setCode(plan.getPlanNumber());
        item.setType("生产指令");
        item.setDate(planDate(ctx));
        item.setStatus(plan.getCurrentStatus());
        item.setPreparation(plan.getPreparationName());
        item.setPreparationCode(plan.getPreparationCode());
        item.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        item.setBatchNo(ctx.getWorkOrder() == null ? null : ctx.getWorkOrder().getBatchNumber());
        item.setQty(plan.getPlanQuantity() != null ? plan.getPlanQuantity()
                : ctx.getWorkOrder() == null ? null : ctx.getWorkOrder().getBatchQty());
        item.setPlanStart(plan.getProductionStartTime());
        item.setPlanEnd(plan.getProductionEndTime());
        item.setProcessRoute(processRoute(ctx));
        item.setRemark(plan.getRemark());
        item.setFiles(toFiles(businessFileService.list("PRODUCTION_PLAN", plan.getId().longValue(), null, null)));
        return List.of(item);
    }

    /**
     * 生成工序路线文案
     *
     * @param ctx 批次上下文
     * @return 形如「粉碎 → 干燥（丸） → 总混」的工序路线
     */
    private String processRoute(BatchContext ctx) {
        if (ctx.getExecutions() == null || ctx.getExecutions().isEmpty()) {
            return null;
        }
        Map<Long, String> names = ctx.getProcessNames() == null ? Collections.emptyMap() : ctx.getProcessNames();
        return ctx.getExecutions().stream()
                .map(execution -> processName(ctx, execution, names))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" → "));
    }

    /**
     * 获取工序显示名称（可传入已加载的名称映射）
     *
     * @param ctx   批次上下文
     * @param execution 工序执行记录
     * @param names 工序类型名称映射
     * @return 工序名称
     */
    private String processName(BatchContext ctx, WorkOrderProcessExecution execution, Map<Long, String> names) {
        String name = names.get(execution.getProcessTypeId());
        if (!StringUtils.hasText(name)) {
            name = processName(ctx, execution);
        }
        return name;
    }

    private ReleaseBatchDtos.DocumentItem toRequisition(ProductionPick pick, BatchContext ctx) {
        ReleaseBatchDtos.DocumentItem item = new ReleaseBatchDtos.DocumentItem();
        item.setId(pick.getId());
        item.setCode(pick.getPickCode());
        item.setType("领料单");
        item.setDate(date(pick.getPickTime()));
        item.setStatus(pick.getStatus());
        item.setOperator(pick.getApplicant());
        item.setPurpose(pick.getPurpose());
        item.setWarehouse(pick.getWorkshop());
        item.setStockOutCode(pick.getStockOutCode());
        item.setRemark(pick.getRemark());
        List<ProductionPickDetail> details = ctx.getPickDetails() == null
                ? Collections.emptyList()
                : ctx.getPickDetails().getOrDefault(pick.getId(), Collections.emptyList());
        item.setLines(details.stream().map(this::toDocumentLine).collect(Collectors.toList()));
        if (pick.getStockOutId() != null) {
            item.setFiles(toFiles(businessFileService.list("STOCK_OUT_PRODUCTION", pick.getStockOutId(), null, null)));
        }
        return item;
    }

    private ReleaseBatchDtos.DocumentItem toReturn(ReturnOrder returnOrder, BatchContext ctx) {
        ReleaseBatchDtos.DocumentItem item = new ReleaseBatchDtos.DocumentItem();
        item.setId(returnOrder.getId());
        item.setCode(returnOrder.getReturnNo());
        item.setType("退库单");
        item.setDate(date(returnOrder.getReturnTime()));
        item.setOperator(returnOrder.getReturnPersonName());
        item.setStatus(returnOrder.getReturnTime() != null ? "已退库" : "待退库");
        item.setStockOutCode(returnOrder.getOutOrderNo());
        item.setRemark(returnOrder.getRemark());
        List<ReturnOrderDetail> details = ctx.getReturnDetails() == null
                ? Collections.emptyList()
                : ctx.getReturnDetails().getOrDefault(returnOrder.getId(), Collections.emptyList());
        item.setLines(details.stream().map(this::toDocumentLine).collect(Collectors.toList()));
        item.setFiles(toFiles(businessFileService.list("RETURN_ORDER", returnOrder.getId(), null, null)));
        return item;
    }

    /**
     * 组装领料/退库明细行
     *
     * @param detail 明细实体
     * @return 明细 DTO
     */
    private ReleaseBatchDtos.DocumentLineItem toDocumentLine(Object detail) {
        ReleaseBatchDtos.DocumentLineItem item = new ReleaseBatchDtos.DocumentLineItem();
        if (detail instanceof ProductionPickDetail pick) {
            item.setMaterialCode(pick.getMaterialCode());
            item.setMaterialName(pick.getMaterialName());
            item.setSpec(pick.getCategoryName());
            item.setQuantity(pick.getActualQty() != null ? pick.getActualQty() : pick.getApplyQty());
            item.setUnit(pick.getUnitName());
            item.setBatchNumber(pick.getBatchNumber());
            item.setUnitPrice(pick.getUnitPrice());
        } else if (detail instanceof ReturnOrderDetail returned) {
            item.setMaterialCode(returned.getMaterialCode());
            item.setMaterialName(returned.getMaterialName());
            item.setSpec(returned.getCategory());
            item.setQuantity(returned.getReturnQuantity());
            item.setUnit(returned.getUnit());
            item.setBatchNumber(returned.getBatchNo());
            item.setUnitPrice(returned.getUnitPrice());
            item.setAmount(returned.getAmount());
        }
        return item;
    }

    private List<ReleaseBatchDtos.ProcessItem> processItems(BatchContext ctx) {
        Map<Long, WorkOrderProcessCheck> checkMap = ctx.getProcessChecks().stream()
                .filter(c -> c.getExecutionId() != null)
                .collect(Collectors.toMap(WorkOrderProcessCheck::getExecutionId, c -> c, (a, b) -> a));
        List<ReleaseBatchDtos.ProcessItem> items = new ArrayList<>();
        for (WorkOrderProcessExecution execution : ctx.getExecutions()) {
            ReleaseBatchDtos.ProcessItem item = new ReleaseBatchDtos.ProcessItem();
            item.setExecutionId(execution.getId());
            item.setProcessTypeId(execution.getProcessTypeId());
            item.setProcessName(processName(ctx, execution));
            String requirement = ctx.getProcessRequirements() == null ? null
                    : ctx.getProcessRequirements().get(execution.getProcessTypeId());
            item.setRequirement(StringUtils.hasText(requirement) ? requirement : execution.getKeyProcessParams());
            WorkOrderProcessCheck check = checkMap.get(execution.getId());
            item.setResult(check == null ? RESULT_PENDING : check.getCheckResult());
            item.setInspector(check == null ? null : check.getInspectorName());
            item.setCheckDate(check == null ? null : check.getCheckDate());
            item.setStartTime(execution.getStartTime());
            item.setEndTime(execution.getEndTime());
            item.setOperator(execution.getOperatorName());
            item.setStatus(execution.getStatus());
            if (execution.getRoomId() != null && ctx.getRooms() != null && ctx.getRooms().get(execution.getRoomId()) != null) {
                item.setRoomName(ctx.getRooms().get(execution.getRoomId()).getRoomName());
            }
            item.setEquipmentName(execution.getEquipmentName());
            item.setRemark(execution.getRemark());
            item.setFiles(toFiles(businessFileService.list("WORK_ORDER_PROCESS_EXECUTION",
                    execution.getId(), null, null)));
            items.add(item);
        }
        return items;
    }

    /**
     * 获取工序显示名称
     * <p>优先使用批次上下文已加载的工序类型名称，避免依赖未持久化的 execution.processTypeName。</p>
     *
     * @param ctx       批次上下文
     * @param execution 工序执行记录
     * @return 工序名称
     */
    private String processName(BatchContext ctx, WorkOrderProcessExecution execution) {
        if (execution.getProcessTypeId() != null && ctx.getProcessNames() != null) {
            String name = ctx.getProcessNames().get(execution.getProcessTypeId());
            if (StringUtils.hasText(name)) {
                return name;
            }
        }
        if (StringUtils.hasText(execution.getProcessTypeName())) {
            return execution.getProcessTypeName();
        }
        return "工序" + execution.getStepOrder();
    }

    private ReleaseBatchDtos.InspectionItem toInspectionItem(Object entity, String type, String bizType, BatchContext ctx) {
        ReleaseBatchDtos.InspectionItem item = new ReleaseBatchDtos.InspectionItem();
        item.setType(type);
        Long id = null;
        InspectionTask task = null;
        if (entity instanceof InspectionRequest r) {
            id = r.getId();
            item.setCode(r.getInspectionCode());
            item.setItemCategory(r.getItemCategory());
            item.setProcess(r.getProcessName());
            item.setTime(r.getRequestTime());
            item.setOperator(r.getRequester());
            item.setRequester(r.getRequester());
            item.setRequestDepartment(r.getRequestDepartment());
            item.setRequestTime(r.getRequestTime());
            item.setInspectionItems(r.getInspectionItems());
            item.setConfigQuantity(r.getConfigQuantity());
            item.setRemark(r.getRemark());
            item.setRelatedPlanCode(r.getInspectionPlanCode());
            task = findInspectionTask(ctx, "request", id);
            item.setStatus(requestStatus(task));
        } else if (entity instanceof SamplingRecord s) {
            id = s.getId();
            item.setCode(s.getSamplingCode());
            item.setItemCategory(s.getItemCategory());
            item.setTime(s.getSamplingTime());
            item.setOperator(s.getSampler());
            item.setSampler(s.getSampler());
            item.setSamplingTime(s.getSamplingTime());
            item.setSamplingQuantity(s.getSamplingQuantity());
            item.setSamplingCount(s.getSamplingCount());
            item.setSamplingMethod(s.getSamplingMethod());
            item.setSamplingLocation(s.getSamplingLocation());
            item.setRemark(s.getRemark());
            item.setRelatedPlanCode(s.getRelatedPlanCode());
            task = findInspectionTask(ctx, "sampling", id);
            item.setStatus("已取样");
            if (task != null) {
                item.setRelatedInspectionRequestCode(requestCodeById(ctx, task.getRequestRefId()));
            }
        } else if (entity instanceof InspectionRecord r) {
            id = r.getId();
            item.setCode(r.getInspectionCode());
            item.setItemCategory(r.getItemCategory());
            item.setConclusion(r.getConclusion());
            item.setTime(r.getEndTime());
            item.setOperator(r.getInspector());
            item.setInspectionBasis(r.getInspectionBasis());
            item.setInspectionItem(r.getInspectionItem());
            item.setInspector(r.getInspector());
            item.setReviewer(r.getReviewer());
            item.setStartTime(r.getStartTime());
            item.setEndTime(r.getEndTime());
            item.setRemark(r.getRemark());
            item.setRelatedInspectionRequestCode(r.getRelatedInspectionRequestCode());
            item.setRelatedSamplingCode(r.getRelatedSamplingCode());
            task = findInspectionTask(ctx, "inspection", id);
            item.setStatus(StringUtils.hasText(r.getConclusion()) && r.getEndTime() != null ? "已检验" : "检验中");
        }
        item.setId(id);
        if (task != null) {
            item.setRelatedTaskCode(task.getTaskCode());
            if (!StringUtils.hasText(item.getRelatedPlanCode())) {
                item.setRelatedPlanCode(task.getPlanNumber());
            }
        }
        item.setFiles(id == null ? Collections.emptyList() : toFiles(businessFileService.list(bizType, id, null, null)));
        return item;
    }

    /**
     * 按类型和单据ID查找检验任务
     *
     * @param ctx   批次上下文
     * @param kind  单据类型：request/sampling/inspection
     * @param refId 单据ID
     * @return 检验任务，未找到时返回 null
     */
    private InspectionTask findInspectionTask(BatchContext ctx, String kind, Long refId) {
        if (refId == null || ctx.getInspectionTasks() == null) {
            return null;
        }
        return ctx.getInspectionTasks().stream()
                .filter(task -> switch (kind) {
                    case "request" -> refId.equals(task.getRequestRefId());
                    case "sampling" -> refId.equals(task.getSamplingRefId());
                    case "inspection" -> refId.equals(task.getInspectionRefId());
                    default -> false;
                })
                .findFirst().orElse(null);
    }

    /**
     * 推导请检单状态
     *
     * @param task 检验任务
     * @return 待取样/检验中/已检验
     */
    private String requestStatus(InspectionTask task) {
        if (task == null) {
            return "待检验";
        }
        if (task.getInspectionRefId() != null) {
            return "已检验";
        }
        return task.getSamplingRefId() != null ? "检验中" : "待取样";
    }

    /**
     * 按请检单ID获取请检单号
     *
     * @param ctx 批次上下文
     * @param id  请检单ID
     * @return 请检单号
     */
    private String requestCodeById(BatchContext ctx, Long id) {
        if (id == null || ctx.getRequests() == null) {
            return null;
        }
        return ctx.getRequests().stream()
                .filter(request -> id.equals(request.getId()))
                .map(InspectionRequest::getInspectionCode)
                .findFirst().orElse(null);
    }

    private List<ReleaseBatchDtos.RoomItem> roomItems(BatchContext ctx) {
        Map<Integer, String> names = ctx.getRooms() == null ? Collections.emptyMap()
                : ctx.getRooms().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().getRoomName() == null ? String.valueOf(e.getKey()) : e.getValue().getRoomName()));
        Map<Integer, String> result = new HashMap<>();
        for (WorkOrderProcessExecution execution : ctx.getExecutions()) {
            if (execution.getRoomId() != null) {
                result.put(execution.getRoomId(), StringUtils.hasText(execution.getStatus())
                        ? execution.getStatus() : RESULT_PENDING);
            }
        }
        return result.entrySet().stream().map(e -> {
            ReleaseBatchDtos.RoomItem item = new ReleaseBatchDtos.RoomItem();
            item.setRoomId(e.getKey().longValue());
            item.setRoomName(names.getOrDefault(e.getKey(), "房间" + e.getKey()));
            item.setRecord("工序执行区域");
            item.setResult(e.getValue());
            return item;
        }).collect(Collectors.toList());
    }

    private ReleaseBatchDtos.YieldInfo yieldInfo(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        ReleaseBatchDtos.YieldInfo info = new ReleaseBatchDtos.YieldInfo();
        info.setPlanQty(wo.getBatchQty());
        info.setFinishedQty(wo.getFinishedQty());
        BigDecimal rate = yieldRate(ctx);
        info.setYieldRate(rate);
        info.setThreshold(YIELD_THRESHOLD);
        info.setPass(rate != null && rate.compareTo(YIELD_THRESHOLD) >= 0);
        return info;
    }

    private ReleaseBatchDtos.DeviationItem toDeviation(DeviationRecord d) {
        ReleaseBatchDtos.DeviationItem item = new ReleaseBatchDtos.DeviationItem();
        item.setId(d.getId());
        item.setCode(d.getDeviationCode());
        item.setTitle(d.getDeviationTitle());
        item.setConclusion(d.getResultDesc());
        item.setHandler(d.getHandlerName());
        item.setHandleDate(date(d.getHandleTime()));
        item.setStatus(deviationOpen(d) ? "未关闭" : "已关闭");
        item.setOccurTime(d.getOccurTime());
        item.setProcessName(d.getProcessName());
        item.setDescription(d.getDescription());
        item.setAction(d.getActionDesc());
        item.setRemark(d.getRemark());
        item.setFiles(toFiles(businessFileService.list("DEVIATION_RECORD", d.getId(), null, null)));
        return item;
    }

    // endregion

    // region 文件与合规
    // ===================================
    // 文件与合规
    // ===================================

    private List<ReleaseBatchDtos.FileChecklistItem> fileChecklist(BatchContext ctx) {
        List<FileRequirementConfig> configs = resolveReleaseFileConfigs(ctx);
        List<ReleaseBatchDtos.FileChecklistItem> result = new ArrayList<>();
        for (FileRequirementConfig config : configs) {
            int expected = expectedCount(config, ctx);
            if (expected <= 0) {
                continue;
            }
            int uploaded = uploadedCount(config, ctx);
            ReleaseBatchDtos.FileChecklistItem item = new ReleaseBatchDtos.FileChecklistItem();
            item.setStage(config.getStage());
            item.setLabel(StringUtils.hasText(config.getDisplayName())
                    ? config.getDisplayName() : config.getBizType() + " / " + config.getFileKind());
            item.setBizType(config.getBizType());
            item.setFileKind(config.getFileKind());
            item.setRequirement(config.getRequirement());
            item.setUploaded(uploaded);
            item.setTotal(expected);
            List<String> missingDetails = missingDetails(config, ctx);
            item.setMissingDetails(missingDetails);
            if (uploaded < expected && !"OPTIONAL".equals(config.getRequirement())) {
                if (!missingDetails.isEmpty()) {
                    String detailText = missingDetails.stream().limit(3).collect(Collectors.joining("、"));
                    item.setNote("缺 " + detailText
                            + (missingDetails.size() > 3 ? " 等 " + missingDetails.size() + " 项" : ""));
                } else {
                    item.setNote("缺 " + (expected - uploaded));
                }
            } else if ("OPTIONAL".equals(config.getRequirement())) {
                item.setNote("可空缺");
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 解析放行页使用的文件要求配置
     * <p>
     * 只保留批次相关 bizType，排除品种级资料；同一 bizType + fileKind 多级配置时
     * 按 品种级 > 被检品属性级 > GLOBAL 取优先级最高的一条。
     * </p>
     *
     * @param ctx 批次上下文
     * @return 去重后的配置列表
     */
    private List<FileRequirementConfig> resolveReleaseFileConfigs(BatchContext ctx) {
        String preparationCode = ctx.getPreparation() == null ? null : ctx.getPreparation().getPreparationCode();
        if (!StringUtils.hasText(preparationCode)) {
            preparationCode = ctx.getWorkOrder().getPreparationCode();
        }
        Map<String, FileRequirementConfig> best = new LinkedHashMap<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            String bizType = config.getBizType();
            if (bizType == null || !RELEASE_FILE_BIZ_TYPES.contains(bizType)) {
                continue;
            }
            String key = bizType + "|" + config.getFileKind();
            FileRequirementConfig existing = best.get(key);
            if (existing == null || scopePriority(config, preparationCode) > scopePriority(existing, preparationCode)) {
                best.put(key, config);
            }
        }
        return new ArrayList<>(best.values());
    }

    /**
     * 文件要求配置的适用范围优先级
     *
     * @param config          配置
     * @param preparationCode 当前批次品种编码
     * @return 优先级，数值越大越优先
     */
    private int scopePriority(FileRequirementConfig config, String preparationCode) {
        if ("PREPARATION".equals(config.getScope())
                && StringUtils.hasText(preparationCode)
                && Objects.equals(config.getScopeKey(), preparationCode)) {
            return 3;
        }
        if ("ITEM_CATEGORY".equals(config.getScope()) && "成品".equals(config.getScopeKey())) {
            return 2;
        }
        if ("GLOBAL".equals(config.getScope())) {
            return 1;
        }
        return 0;
    }

    private int expectedCount(FileRequirementConfig config, BatchContext ctx) {
        return switch (config.getBizType()) {
            case "PRODUCTION_PLAN" -> ctx.getPlan() == null ? 0 : 1;
            case "PURCHASE_ORDER" -> ctx.getPurchases().size();
            case "ACCEPTANCE_ORDER" -> ctx.getAcceptances().size();
            case "WORK_ORDER" -> 1;
            case "WORK_ORDER_PROCESS_EXECUTION" -> ctx.getExecutions().size();
            case "INSPECTION_REQUEST" -> ctx.getRequests().size();
            case "SAMPLING_RECORD" -> ctx.getSamplings().size();
            case "INSPECTION_RECORD" -> ctx.getInspections().size();
            case "RELEASE_REVIEW" -> ctx.getRelease() == null ? 0 : 1;
            case "STOCK_OUT_PRODUCTION" -> ctx.getPicks().size();
            case "STOCK_IN_PRODUCT" -> ctx.getStockIns().size();
            case "STOCK_OUT_SALES" -> ctx.getStockOuts().size();
            default -> 0;
        };
    }

    private int uploadedCount(FileRequirementConfig config, BatchContext ctx) {
        String bizType = config.getBizType();
        String kind = config.getFileKind();
        return switch (bizType) {
            case "PRODUCTION_PLAN" -> ctx.getPlan() != null
                    && hasFile(bizType, ctx.getPlan().getId().longValue(), kind) ? 1 : 0;
            case "PURCHASE_ORDER" -> (int) ctx.getPurchases().stream()
                    .filter(p -> hasFile(bizType, p.getId(), kind)).count();
            case "ACCEPTANCE_ORDER" -> (int) ctx.getAcceptances().stream()
                    .filter(a -> hasFile(bizType, a.getAcceptanceId(), kind)).count();
            case "WORK_ORDER" -> hasFile(bizType, ctx.getWorkOrder().getWorkOrderId(), kind) ? 1 : 0;
            case "WORK_ORDER_PROCESS_EXECUTION" -> (int) ctx.getExecutions().stream()
                    .filter(e -> hasFile(bizType, e.getId(), kind)).count();
            case "INSPECTION_REQUEST" -> (int) ctx.getRequests().stream()
                    .filter(r -> hasFile(bizType, r.getId(), kind)).count();
            case "SAMPLING_RECORD" -> (int) ctx.getSamplings().stream()
                    .filter(s -> hasFile(bizType, s.getId(), kind)).count();
            case "INSPECTION_RECORD" -> (int) ctx.getInspections().stream()
                    .filter(r -> hasFile(bizType, r.getId(), kind)).count();
            // 放行记录本身即放行审核单据，存在记录即视为已传 1
            case "RELEASE_REVIEW" -> ctx.getRelease() == null ? 0 : 1;
            case "STOCK_OUT_PRODUCTION" -> (int) ctx.getPicks().stream()
                    .filter(p -> p.getStockOutId() != null && hasFile(bizType, p.getStockOutId(), kind)).count();
            case "STOCK_IN_PRODUCT" -> (int) ctx.getStockIns().stream()
                    .filter(s -> hasFile(bizType, s.getInId(), kind)).count();
            case "STOCK_OUT_SALES" -> (int) ctx.getStockOuts().stream()
                    .filter(s -> hasFile(bizType, s.getOutId(), kind)).count();
            default -> 0;
        };
    }

    /**
     * 生成文件清单的缺失明细，供页面显示“缺哪一份”
     *
     * @param config 文件要求配置
     * @param ctx    批次上下文
     * @return 缺失明细列表
     */
    private List<String> missingDetails(FileRequirementConfig config, BatchContext ctx) {
        String bizType = config.getBizType();
        String kind = config.getFileKind();
        List<String> details = new ArrayList<>();
        switch (bizType) {
            case "PRODUCTION_PLAN" -> {
                if (ctx.getPlan() != null && !hasFile(bizType, ctx.getPlan().getId().longValue(), kind)) {
                    details.add("生产计划 " + ctx.getPlan().getPlanNumber());
                }
            }
            case "PURCHASE_ORDER" -> ctx.getPurchases().stream()
                    .filter(p -> !hasFile(bizType, p.getId(), kind))
                    .map(p -> "采购单 " + p.getPurchaseNumber())
                    .forEach(details::add);
            case "ACCEPTANCE_ORDER" -> ctx.getAcceptances().stream()
                    .filter(a -> !hasFile(bizType, a.getAcceptanceId(), kind))
                    .map(a -> "验收单 " + a.getAcceptanceCode() + " 缺 " + acceptanceKindLabel(kind))
                    .forEach(details::add);
            case "WORK_ORDER" -> {
                if (!hasFile(bizType, ctx.getWorkOrder().getWorkOrderId(), kind)) {
                    details.add("批生产记录");
                }
            }
            case "WORK_ORDER_PROCESS_EXECUTION" -> ctx.getExecutions().stream()
                    .filter(e -> !hasFile(bizType, e.getId(), kind))
                    .map(e -> processName(ctx, e) + "工序记录")
                    .forEach(details::add);
            case "INSPECTION_REQUEST" -> ctx.getRequests().stream()
                    .filter(r -> !hasFile(bizType, r.getId(), kind))
                    .map(r -> "请检单 " + r.getInspectionCode())
                    .forEach(details::add);
            case "SAMPLING_RECORD" -> ctx.getSamplings().stream()
                    .filter(s -> !hasFile(bizType, s.getId(), kind))
                    .map(s -> "取样记录 " + s.getSamplingCode())
                    .forEach(details::add);
            case "INSPECTION_RECORD" -> ctx.getInspections().stream()
                    .filter(r -> !hasFile(bizType, r.getId(), kind))
                    .map(r -> "检验记录 " + r.getInspectionCode())
                    .forEach(details::add);
            case "RELEASE_REVIEW" -> {
                if (ctx.getRelease() == null) {
                    details.add("放行审核记录");
                }
            }
            case "STOCK_OUT_PRODUCTION" -> ctx.getPicks().stream()
                    .filter(p -> p.getStockOutId() == null || !hasFile(bizType, p.getStockOutId(), kind))
                    .map(p -> "领料单 " + p.getPickCode())
                    .forEach(details::add);
            case "STOCK_IN_PRODUCT" -> ctx.getStockIns().stream()
                    .filter(s -> !hasFile(bizType, s.getInId(), kind))
                    .map(s -> "成品入库单 " + s.getInCode())
                    .forEach(details::add);
            case "STOCK_OUT_SALES" -> ctx.getStockOuts().stream()
                    .filter(s -> !hasFile(bizType, s.getOutId(), kind))
                    .map(s -> "成品出库单 " + s.getOutCode())
                    .forEach(details::add);
            default -> {
                // 其他类型不生成明细
            }
        }
        return details;
    }

    private boolean hasFile(String bizType, Long bizId, String kind) {
        if (bizId == null) return false;
        List<BusinessFileDto> files = businessFileService.list(bizType, bizId, null, kind);
        return files != null && !files.isEmpty();
    }

    private FileStat fileStat(BatchContext ctx) {
        FileStat stat = new FileStat();
        for (ReleaseBatchDtos.FileChecklistItem item : fileChecklist(ctx)) {
            if ("OPTIONAL".equals(item.getRequirement())) {
                continue;
            }
            stat.required += item.getTotal();
            stat.uploaded += item.getUploaded();
        }
        return stat;
    }

    private ProcessStat processStat(BatchContext ctx) {
        ProcessStat stat = new ProcessStat();
        stat.total = ctx.getExecutions().size();
        Map<Long, WorkOrderProcessCheck> checkMap = ctx.getProcessChecks().stream()
                .filter(c -> c.getExecutionId() != null)
                .collect(Collectors.toMap(WorkOrderProcessCheck::getExecutionId, c -> c, (a, b) -> a));
        for (WorkOrderProcessExecution execution : ctx.getExecutions()) {
            WorkOrderProcessCheck check = checkMap.get(execution.getId());
            if (check == null || !StringUtils.hasText(check.getCheckResult())) {
                stat.pending++;
            } else if (RESULT_PASS.equals(check.getCheckResult())) {
                stat.pass++;
            } else if (RESULT_FAIL.equals(check.getCheckResult())) {
                stat.fail++;
            } else {
                stat.pending++;
            }
        }
        return stat;
    }

    private ReleaseBatchDtos.Compliance compliance(BatchContext ctx) {
        ReleaseBatchDtos.Compliance c = new ReleaseBatchDtos.Compliance();
        c.setInspectionPass("合格".equals(inspectionConclusion(ctx)));
        ProcessStat process = processStat(ctx);
        c.setProcessPass(process.total == 0 || (process.fail == 0 && process.pending == 0));
        FileStat file = fileStat(ctx);
        c.setFilePass(file.uploaded >= file.required);
        c.setDeviationPass(ctx.getDeviations().stream().noneMatch(this::deviationOpen));
        c.setYieldPass(yieldRate(ctx) != null && yieldRate(ctx).compareTo(YIELD_THRESHOLD) >= 0);
        c.setCanRelease(c.isInspectionPass() && c.isProcessPass() && c.isFilePass()
                && c.isDeviationPass() && c.isYieldPass());
        c.setSummary(c.isCanRelease() ? "全部合规，可以放行" : "存在不符合项，暂不能放行");
        return c;
    }

    private boolean deviationOpen(DeviationRecord d) {
        return d.getHandleTime() == null && !StringUtils.hasText(d.getResultDesc());
    }

    private String inspectionConclusion(BatchContext ctx) {
        List<InspectionRecord> inspections = finishedInspections(ctx);
        if (inspections.isEmpty()) {
            String workOrderStatus = ctx.getWorkOrder().getCurrentStatus();
            if ("检验中".equals(workOrderStatus) || "已检验".equals(workOrderStatus)) {
                return workOrderStatus;
            }
            return "未检验";
        }
        String conclusion = inspections.get(0).getConclusion();
        if (!StringUtils.hasText(conclusion) || "待检".equals(conclusion)) {
            return "检验中";
        }
        return conclusion;
    }

    /**
     * 取成品检验记录；没有标记被检品属性时兼容返回全部记录
     *
     * @param ctx 批次上下文
     * @return 按结束时间倒序的成品检验记录
     */
    private List<InspectionRecord> finishedInspections(BatchContext ctx) {
        List<InspectionRecord> all = ctx.getInspections();
        List<InspectionRecord> finished = all.stream()
                .filter(record -> "成品".equals(record.getItemCategory()))
                .collect(Collectors.toList());
        if (!finished.isEmpty()) {
            return finished;
        }
        // 历史数据可能没有标记被检品属性；只有全部为空时才兼容返回全部
        boolean hasCategory = all.stream().anyMatch(record -> StringUtils.hasText(record.getItemCategory()));
        return hasCategory ? Collections.emptyList() : all;
    }

    private BigDecimal yieldRate(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        if (wo.getYieldRate() != null) {
            return wo.getYieldRate();
        }
        if (wo.getBatchQty() != null && wo.getBatchQty().compareTo(BigDecimal.ZERO) > 0
                && wo.getFinishedQty() != null) {
            return wo.getFinishedQty().multiply(new BigDecimal("100"))
                    .divide(wo.getBatchQty(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private String unitName(BatchContext ctx) {
        if (ctx.getPlan() != null && StringUtils.hasText(ctx.getPlan().getUnitName())) {
            return ctx.getPlan().getUnitName();
        }
        if (ctx.getPreparation() != null && StringUtils.hasText(ctx.getPreparation().getUnitName())) {
            return ctx.getPreparation().getUnitName();
        }
        return "";
    }

    private LocalDate planDate(BatchContext ctx) {
        if (ctx.getPlan() == null) {
            return null;
        }
        LocalDateTime time = ctx.getPlan().getPlanProductionTime() != null
                ? ctx.getPlan().getPlanProductionTime() : ctx.getPlan().getCreatedTime();
        return date(time);
    }

    private String releaseStatus(BatchContext ctx) {
        if (ctx.getRelease() != null && StringUtils.hasText(ctx.getRelease().getReleaseConclusion())) {
            String conclusion = ctx.getRelease().getReleaseConclusion();
            // 兼容历史值“同意放行”；“拒绝放行”必须先判断
            if (conclusion.contains("拒绝")) return "已拒绝";
            if (conclusion.contains("放行")) return "已放行";
        }
        // 草稿（待补充）不强制“待审核”，是否可放行只看合规五项
        return compliance(ctx).isCanRelease() ? "待放行" : "待审核";
    }

    /**
     * 放行批准人：取放行审核记录的审核人
     * <p>草稿（待补充）也会记录审核人，列表与详情页口径保持一致，未生成放行记录时返回 null。</p>
     *
     * @param ctx 批次上下文
     * @return 审核人姓名
     */
    private String releaseApprover(BatchContext ctx) {
        return ctx.getRelease() == null ? null : ctx.getRelease().getReviewer();
    }

    /**
     * 放行日期：取放行审核记录的审核时间所在日期
     *
     * @param ctx 批次上下文
     * @return 审核日期，未生成放行记录时返回 null
     */
    private LocalDate releaseDate(BatchContext ctx) {
        return ctx.getRelease() == null ? null : date(ctx.getRelease().getReviewTime());
    }

    private ReleaseBatchDtos.ReleaseInfo releaseInfo(BatchContext ctx) {
        ReleaseBatchDtos.ReleaseInfo info = new ReleaseBatchDtos.ReleaseInfo();
        if (ctx.getRelease() == null) {
            return info;
        }
        info.setId(ctx.getRelease().getId());
        info.setReleaseCode(ctx.getRelease().getReleaseCode());
        info.setConclusion(ctx.getRelease().getReleaseConclusion());
        info.setApprover(ctx.getRelease().getReviewer());
        info.setReviewTime(ctx.getRelease().getReviewTime());
        info.setOpinion(ctx.getRelease().getReviewOpinion());
        return info;
    }

    // endregion

    // region 工序检查 / 放行
    // ===================================
    // 工序检查 / 放行
    // ===================================

    @Override
    @Transactional
    public void saveProcessChecks(Long workOrderId, ReleaseBatchDtos.ProcessCheckSaveRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }
        BatchContext ctx = loadContext(workOrderId);
        if (isReleased(ctx)) {
            throw new IllegalArgumentException("该批次已放行，不能修改工序检查");
        }
        Set<Long> executionIds = ctx.getExecutions().stream()
                .map(WorkOrderProcessExecution::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> validResults = Set.of(RESULT_PASS, RESULT_FAIL, RESULT_PENDING);
        for (ReleaseBatchDtos.ProcessCheckItem item : request.getItems()) {
            if (item.getExecutionId() == null || !executionIds.contains(item.getExecutionId())) {
                throw new IllegalArgumentException("工序执行记录不属于该工单：" + item.getExecutionId());
            }
            String checkResult = item.getCheckResult();
            if (!StringUtils.hasText(checkResult) || !validResults.contains(checkResult)) {
                throw new IllegalArgumentException("工序检查结果不正确：" + checkResult);
            }
            WorkOrderProcessCheck check = processCheckMapper.selectOne(new QueryWrapper<WorkOrderProcessCheck>()
                    .eq("work_order_id", workOrderId)
                    .eq("execution_id", item.getExecutionId())
                    .last("LIMIT 1"));
            if (check == null) {
                check = new WorkOrderProcessCheck();
                check.setWorkOrderId(workOrderId);
                check.setIsDeleted(0);
                check.setVersion(1);
            }
            check.setExecutionId(item.getExecutionId());
            check.setProcessTypeId(item.getProcessTypeId());
            check.setProcessName(item.getProcessName());
            check.setRequirementSnapshot(item.getRequirementSnapshot());
            check.setCheckResult(item.getCheckResult());
            check.setInspectorId(item.getInspectorId());
            check.setInspectorName(item.getInspectorName());
            check.setCheckDate(item.getCheckDate());
            check.setRemark(item.getRemark());
            if (check.getId() == null) {
                processCheckMapper.insert(check);
            } else {
                processCheckMapper.updateById(check);
            }
        }
    }

    @Override
    @Transactional
    public ReleaseBatchDtos.BatchDetail approve(Long workOrderId, ReleaseBatchDtos.ApproveRequest request) {
        BatchContext ctx = loadContext(workOrderId);
        if (isReleased(ctx)) {
            throw new IllegalArgumentException("该批次已放行，不能重复放行");
        }
        ReleaseBatchDtos.Compliance compliance = compliance(ctx);
        if (!compliance.isCanRelease()) {
            throw new IllegalArgumentException("存在不符合项，不能通过放行");
        }
        LocalDateTime reviewTime = resolveReviewTime(
                request == null ? null : request.getReviewTime(),
                request == null ? null : request.getReviewDate());
        String opinion = request == null || !StringUtils.hasText(request.getOpinion())
                ? "合规检查通过，同意放行。" : request.getOpinion();
        saveRelease(ctx, CONCLUSION_RELEASE, request == null ? null : request.getApprover(),
                reviewTime, opinion);
        // 走工单服务同步放行时间，并联动重算工单/生产计划状态
        workOrderService.syncWorkOrderTime(workOrderId, "auditReleaseTime", reviewTime);
        return getBatchDetail(workOrderId);
    }

    @Override
    @Transactional
    public ReleaseBatchDtos.BatchDetail reject(Long workOrderId, ReleaseBatchDtos.RejectRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("拒绝原因必填");
        }
        BatchContext ctx = loadContext(workOrderId);
        if (isReleased(ctx)) {
            throw new IllegalArgumentException("该批次已放行，不能拒绝放行");
        }
        LocalDateTime reviewTime = resolveReviewTime(request.getReviewTime(), request.getReviewDate());
        saveRelease(ctx, CONCLUSION_REJECT, request.getReviewer(), reviewTime, request.getReason());
        return getBatchDetail(workOrderId);
    }

    @Override
    @Transactional
    public ReleaseBatchDtos.BatchDetail saveDraft(Long workOrderId, ReleaseBatchDtos.DraftRequest request) {
        BatchContext ctx = loadContext(workOrderId);
        if (isReleased(ctx)) {
            throw new IllegalArgumentException("该批次已放行，不能修改审核意见");
        }
        String conclusion = request == null || !StringUtils.hasText(request.getConclusion())
                ? CONCLUSION_DRAFT : request.getConclusion();
        if (!CONCLUSION_DRAFT.equals(conclusion)) {
            throw new IllegalArgumentException("草稿结论只能为待补充");
        }
        LocalDateTime reviewTime = resolveReviewTime(
                request == null ? null : request.getReviewTime(),
                request == null ? null : request.getReviewDate());
        saveRelease(ctx, conclusion, request == null ? null : request.getApprover(),
                reviewTime, request == null ? null : request.getOpinion());
        return getBatchDetail(workOrderId);
    }

    @Override
    public List<ReleaseBatchDtos.VarietyOption> listVarieties() {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.select("preparation_code", "preparation_name")
                .isNotNull("batch_number").ne("batch_number", "")
                .isNotNull("preparation_code").ne("preparation_code", "");
        Map<String, ReleaseBatchDtos.VarietyOption> options = new LinkedHashMap<>();
        for (WorkOrder wo : workOrderMapper.selectList(wrapper)) {
            if (!StringUtils.hasText(wo.getPreparationCode())) {
                continue;
            }
            options.computeIfAbsent(wo.getPreparationCode(), code -> {
                ReleaseBatchDtos.VarietyOption option = new ReleaseBatchDtos.VarietyOption();
                option.setPreparationCode(code);
                option.setPreparationName(wo.getPreparationName());
                return option;
            });
        }
        return options.values().stream()
                .sorted(Comparator.comparing(option -> option.getPreparationName() == null
                        ? "" : option.getPreparationName()))
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportCsv(ReleaseBatchDtos.ExportRequest request) {
        ReleaseBatchDtos.ExportRequest query = request == null
                ? new ReleaseBatchDtos.ExportRequest() : request;
        String status = StringUtils.hasText(query.getStatus()) ? query.getStatus() : "待审核";
        List<ReleaseBatchDtos.BatchSummary> items = loadSummaries(query.getVarietyCode(),
                query.getBatchNo(), status, query.getKeyword());

        StringBuilder content = new StringBuilder("﻿");
        content.append("品种编码,品种名称,规格,批号,工单号,计划号,批量,成品数,收率(%),检验结论,工序符合,文件完整,偏差,放行状态,阻塞项\n");
        for (ReleaseBatchDtos.BatchSummary item : items) {
            content.append(csv(item.getPreparationCode())).append(',')
                    .append(csv(item.getPreparationName())).append(',')
                    .append(csv(item.getSpec())).append(',')
                    .append(csv(item.getBatchNo())).append(',')
                    .append(csv(item.getWorkOrderCode())).append(',')
                    .append(csv(item.getPlanNumber())).append(',')
                    .append(csv(num(item.getPlanQty()))).append(',')
                    .append(csv(num(item.getFinishedQty()))).append(',')
                    .append(csv(num(item.getYieldRate()))).append(',')
                    .append(csv(item.getInspectionConclusion())).append(',')
                    .append(csv(item.getProcessPass() + "/" + item.getProcessTotal()
                            + "（不符合" + item.getProcessFail() + "/待查" + item.getProcessPending() + "）")).append(',')
                    .append(csv(item.getFileUploaded() + "/" + item.getFileRequired()
                            + "（缺" + item.getFileMissing() + "）")).append(',')
                    .append(csv(item.getDeviationTotal() + "条/未关闭" + item.getDeviationOpen())).append(',')
                    .append(csv(item.getReleaseStatus())).append(',')
                    .append(csv(item.getBlockers().isEmpty() ? "无" : String.join("；", item.getBlockers())))
                    .append('\n');
        }
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BatchContext loadContext(Long workOrderId) {
        WorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return batchQueryService.load(wo);
    }

    /**
     * 判断批次是否已放行
     *
     * @param ctx 批次上下文
     * @return 已放行返回 true
     */
    private boolean isReleased(BatchContext ctx) {
        if (ctx.getRelease() == null || !StringUtils.hasText(ctx.getRelease().getReleaseConclusion())) {
            return false;
        }
        String conclusion = ctx.getRelease().getReleaseConclusion();
        return !conclusion.contains("拒绝") && conclusion.contains("放行");
    }

    /**
     * 解析审核时间：reviewTime 优先，其次 reviewDate（取当天当前时间），否则当前时间
     *
     * @param reviewTime 审核时间
     * @param reviewDate 审核日期
     * @return 审核时间
     */
    private LocalDateTime resolveReviewTime(LocalDateTime reviewTime, LocalDate reviewDate) {
        if (reviewTime != null) {
            return reviewTime;
        }
        if (reviewDate != null) {
            return reviewDate.atTime(LocalTime.now().withNano(0));
        }
        return LocalDateTime.now().withNano(0);
    }

    /**
     * 解析审核人：优先请求值，否则当前登录用户姓名
     *
     * @param reviewer 请求中的审核人
     * @return 审核人姓名
     */
    private String resolveReviewer(String reviewer) {
        if (StringUtils.hasText(reviewer)) {
            return reviewer;
        }
        Long userId = EntityUtils.getCurrentUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null && StringUtils.hasText(user.getUserName())) {
                return user.getUserName();
            }
        }
        return "系统";
    }

    private void saveRelease(BatchContext ctx, String conclusion, String reviewer,
                             LocalDateTime reviewTime, String opinion) {
        WorkOrder wo = ctx.getWorkOrder();
        ReleaseReview release = ctx.getRelease();
        if (release == null) {
            release = new ReleaseReview();
            release.setReleaseCode(sequenceService.generateReleaseCode());
            release.setIsDeleted(0);
        }
        release.setWorkOrderId(wo.getWorkOrderId());
        release.setWorkOrderCode(wo.getWorkOrderCode());
        release.setObjectName(wo.getPreparationName());
        release.setItemCategory("成品");
        release.setPreparationName(wo.getPreparationName());
        release.setPreparationCode(wo.getPreparationCode());
        release.setBatchNo(wo.getBatchNumber());
        release.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        List<InspectionRecord> inspections = finishedInspections(ctx);
        if (!inspections.isEmpty()) {
            release.setRelatedInspectionCode(inspections.get(0).getInspectionCode());
            release.setRelatedInspectionRecordCode(inspections.get(0).getInspectionCode());
        }
        release.setReleaseConclusion(conclusion);
        release.setReviewOpinion(opinion);
        release.setReviewer(resolveReviewer(reviewer));
        release.setReviewTime(reviewTime == null ? LocalDateTime.now().withNano(0) : reviewTime);
        if (release.getId() == null) {
            releaseReviewService.save(release);
        } else {
            // DB 加载的实体在更新时 strictUpdateFill 不会覆盖非 null 值，需手动更新审计字段
            release.setUpdatedBy(EntityUtils.getCurrentUserId());
            release.setUpdatedTime(LocalDateTime.now());
            releaseReviewService.updateById(release);
        }
    }

    /**
     * CSV 字段转义
     *
     * @param value 原始值
     * @return 加引号并转义双引号后的值
     */
    private String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    /**
     * 数值转字符串
     *
     * @param value 数值
     * @return 去掉多余 0 的字符串，空值返回空串
     */
    private String num(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    // endregion

    // region 工具
    // ===================================
    // 工具
    // ===================================

    private List<ReleaseBatchDtos.FileItem> toFiles(List<BusinessFileDto> files) {
        if (files == null) return Collections.emptyList();
        return files.stream().map(f -> {
            ReleaseBatchDtos.FileItem item = new ReleaseBatchDtos.FileItem();
            item.setFileId(f.getFileId());
            item.setName(f.getOriginalName());
            item.setSize(f.getFileSize());
            item.setTime(f.getCreatedTime() == null ? null
                    : f.getCreatedTime().format(FILE_TIME_FORMATTER));
            item.setUploader("");
            return item;
        }).collect(Collectors.toList());
    }

    private LocalDate date(LocalDateTime time) {
        return time == null ? null : time.toLocalDate();
    }

    /**
     * 实际生产开始日期：生产计划开始时间 > 最早工序开始时间 > 工单配置日期
     *
     * @param ctx 批次上下文
     * @return 实际开始日期
     */
    private LocalDate actualStart(BatchContext ctx) {
        if (ctx.getPlan() != null && ctx.getPlan().getProductionStartTime() != null) {
            return date(ctx.getPlan().getProductionStartTime());
        }
        LocalDateTime earliest = ctx.getExecutions().stream()
                .map(WorkOrderProcessExecution::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        if (earliest != null) {
            return date(earliest);
        }
        return date(ctx.getWorkOrder().getConfigDate());
    }

    private LocalDate actualEnd(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        if (wo.getProductionCompleteTime() != null) return date(wo.getProductionCompleteTime());
        if (wo.getConfigCompleteTime() != null) return date(wo.getConfigCompleteTime());
        if (ctx.getPlan() != null && ctx.getPlan().getProductionEndTime() != null) {
            return date(ctx.getPlan().getProductionEndTime());
        }
        return null;
    }

    private static class FileStat {
        int required;
        int uploaded;
    }

    private static class ProcessStat {
        int total;
        int pass;
        int fail;
        int pending;
    }

    // endregion
}

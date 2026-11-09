package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.BatchContext;
import com.tonghui.erp.Common.Dto.QualityPages.ReleaseBatchDtos;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileRequirementConfig;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessCheck;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessCheckMapper;
import com.tonghui.erp.Service.BatchQueryService;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileRequirementService;
import com.tonghui.erp.Service.ReleaseBatchService;
import com.tonghui.erp.Service.ReleaseReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 批次审核放行服务实现
 */
@Service
public class ReleaseBatchServiceImpl implements ReleaseBatchService {

    private static final String RESULT_PASS = "符合";
    private static final String RESULT_FAIL = "不符合";
    private static final String RESULT_PENDING = "待查";

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
        List<ReleaseBatchDtos.BatchSummary> all = loadSummaries(varietyCode, batchNo, status, keyword);
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
            wrapper.and(w -> w.like("batch_number", batchNo).or().like("work_order_code", batchNo));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("work_order_code", keyword)
                    .or().like("batch_number", keyword)
                    .or().like("preparation_name", keyword));
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

    private ReleaseBatchDtos.BatchSummary toSummary(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        ReleaseBatchDtos.BatchSummary dto = new ReleaseBatchDtos.BatchSummary();
        dto.setWorkOrderId(wo.getWorkOrderId());
        dto.setWorkOrderCode(wo.getWorkOrderCode());
        dto.setBatchNo(wo.getBatchNumber());
        dto.setPreparationCode(wo.getPreparationCode());
        dto.setPreparationName(wo.getPreparationName());
        dto.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        dto.setUnitName(unitName(ctx));
        dto.setPlanQty(wo.getBatchQty());
        dto.setFinishedQty(wo.getFinishedQty());
        dto.setPlanDate(planDate(ctx));
        dto.setActualStart(date(wo.getConfigDate()));
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
        dto.setFileMissing(file.required - file.uploaded);

        dto.setDeviationTotal(ctx.getDeviations().size());
        int openDeviations = (int) ctx.getDeviations().stream().filter(this::deviationOpen).count();
        dto.setDeviationOpen(openDeviations);
        dto.setReleaseStatus(releaseStatus(ctx));
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
        if (dto.getYieldRate() != null && dto.getYieldRate().compareTo(new BigDecimal("98")) < 0) {
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
        WorkOrder wo = ctx.getWorkOrder();
        ReleaseBatchDtos.BatchDetail detail = new ReleaseBatchDtos.BatchDetail();

        ReleaseBatchDtos.BatchInfo batch = new ReleaseBatchDtos.BatchInfo();
        batch.setWorkOrderId(wo.getWorkOrderId());
        batch.setWorkOrderCode(wo.getWorkOrderCode());
        batch.setBatchNo(wo.getBatchNumber());
        batch.setPreparationCode(wo.getPreparationCode());
        batch.setPreparationName(wo.getPreparationName());
        batch.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        batch.setUnitName(unitName(ctx));
        batch.setPlanQty(wo.getBatchQty());
        batch.setFinishedQty(wo.getFinishedQty());
        batch.setPlanDate(planDate(ctx));
        batch.setActualStart(date(wo.getConfigDate()));
        batch.setActualEnd(actualEnd(ctx));
        batch.setYieldRate(yieldRate(ctx));
        batch.setInspectionConclusion(inspectionConclusion(ctx));
        batch.setWorkOrderStatus(wo.getCurrentStatus());
        detail.setBatch(batch);

        detail.setPurchases(ctx.getPurchases().stream().map(p -> toPurchase(p, ctx)).collect(Collectors.toList()));
        detail.setAcceptances(ctx.getAcceptances().stream().map(this::toAcceptance).collect(Collectors.toList()));
        detail.setInstructions(instruction(ctx));
        detail.setRequisitions(ctx.getPicks().stream().map(this::toRequisition).collect(Collectors.toList()));
        detail.setReturns(ctx.getReturns().stream().map(this::toReturn).collect(Collectors.toList()));
        detail.setProcesses(processItems(ctx));
        detail.setRequests(ctx.getRequests().stream().map(r -> toInspectionItem(r, "请检单", "INSPECTION_REQUEST")).collect(Collectors.toList()));
        detail.setSamplings(ctx.getSamplings().stream().map(s -> toInspectionItem(s, "取样记录", "SAMPLING_RECORD")).collect(Collectors.toList()));
        detail.setInspections(ctx.getInspections().stream().map(i -> toInspectionItem(i, "检验记录", "INSPECTION_RECORD")).collect(Collectors.toList()));
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
        List<PurchaseOrderItems> lines = ctx.getPurchaseItems() == null ? Collections.emptyList()
                : ctx.getPurchaseItems().getOrDefault(po.getId(), Collections.emptyList());
        if (!lines.isEmpty()) {
            item.setSupplier(lines.stream().map(PurchaseOrderItems::getSupplier)
                    .filter(StringUtils::hasText).findFirst().orElse(null));
        }
        List<BusinessFileDto> files = businessFileService.list("PURCHASE_ORDER", po.getId(), null, null);
        item.setFileCount(files == null ? 0 : files.size());
        item.setFiles(toFiles(files));
        return item;
    }

    private ReleaseBatchDtos.AcceptanceItem toAcceptance(AcceptanceOrder acceptance) {
        ReleaseBatchDtos.AcceptanceItem item = new ReleaseBatchDtos.AcceptanceItem();
        item.setId(acceptance.getAcceptanceId());
        item.setCode(acceptance.getAcceptanceCode());
        item.setStatus(acceptance.getStatus());
        item.setPurchaseNumber(acceptance.getPurchaseNumber());
        List<BusinessFileDto> all = new ArrayList<>();
        for (String kind : List.of("WAYBILL", "INSPECTION_REPORT", "INVOICE")) {
            List<BusinessFileDto> files = businessFileService.list("ACCEPTANCE_ORDER", acceptance.getAcceptanceId(), null, kind);
            if (files != null && !files.isEmpty()) {
                item.getUploadedKinds().add(kind);
                all.addAll(files);
            } else {
                item.getMissingKinds().add(kind);
            }
        }
        item.setFiles(toFiles(all));
        return item;
    }

    private List<ReleaseBatchDtos.DocumentItem> instruction(BatchContext ctx) {
        if (ctx.getPlan() == null) {
            return Collections.emptyList();
        }
        ReleaseBatchDtos.DocumentItem item = new ReleaseBatchDtos.DocumentItem();
        item.setId(ctx.getPlan().getId().longValue());
        item.setCode(ctx.getPlan().getPlanNumber());
        item.setType("生产指令");
        item.setDate(planDate(ctx));
        item.setStatus(ctx.getPlan().getCurrentStatus());
        item.setFiles(toFiles(businessFileService.list("PRODUCTION_PLAN", ctx.getPlan().getId().longValue(), null, null)));
        return List.of(item);
    }

    private ReleaseBatchDtos.DocumentItem toRequisition(ProductionPick pick) {
        ReleaseBatchDtos.DocumentItem item = new ReleaseBatchDtos.DocumentItem();
        item.setId(pick.getId());
        item.setCode(pick.getPickCode());
        item.setType("领料单");
        item.setDate(date(pick.getPickTime()));
        item.setStatus(pick.getStatus());
        item.setOperator(pick.getApplicant());
        if (pick.getStockOutId() != null) {
            item.setFiles(toFiles(businessFileService.list("STOCK_OUT_PRODUCTION", pick.getStockOutId(), null, null)));
        }
        return item;
    }

    private ReleaseBatchDtos.DocumentItem toReturn(ReturnOrder returnOrder) {
        ReleaseBatchDtos.DocumentItem item = new ReleaseBatchDtos.DocumentItem();
        item.setId(returnOrder.getId());
        item.setCode(returnOrder.getReturnNo());
        item.setType("退库单");
        item.setDate(date(returnOrder.getReturnTime()));
        item.setOperator(returnOrder.getReturnPersonName());
        item.setStatus(returnOrder.getOuterPackageCheck());
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
            item.setProcessName(execution.getProcessTypeName() == null
                    ? "工序" + execution.getStepOrder() : execution.getProcessTypeName());
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
            item.setFiles(toFiles(businessFileService.list("WORK_ORDER_PROCESS_EXECUTION",
                    execution.getId(), null, null)));
            items.add(item);
        }
        return items;
    }

    private ReleaseBatchDtos.InspectionItem toInspectionItem(Object entity, String type, String bizType) {
        ReleaseBatchDtos.InspectionItem item = new ReleaseBatchDtos.InspectionItem();
        item.setType(type);
        Long id = null;
        if (entity instanceof InspectionRequest r) {
            id = r.getId();
            item.setCode(r.getInspectionCode());
            item.setProcess(r.getProcessName());
            item.setTime(r.getRequestTime());
            item.setOperator(r.getRequester());
        } else if (entity instanceof SamplingRecord s) {
            id = s.getId();
            item.setCode(s.getSamplingCode());
            item.setTime(s.getSamplingTime());
            item.setOperator(s.getSampler());
        } else if (entity instanceof InspectionRecord r) {
            id = r.getId();
            item.setCode(r.getInspectionCode());
            item.setConclusion(r.getConclusion());
            item.setTime(r.getEndTime());
            item.setOperator(r.getInspector());
        }
        item.setId(id);
        item.setFiles(id == null ? Collections.emptyList() : toFiles(businessFileService.list(bizType, id, null, null)));
        return item;
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
        info.setThreshold(new BigDecimal("98"));
        info.setPass(rate != null && rate.compareTo(new BigDecimal("98")) >= 0);
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
        item.setFiles(toFiles(businessFileService.list("DEVIATION_RECORD", d.getId(), null, null)));
        return item;
    }

    // endregion

    // region 文件与合规
    // ===================================
    // 文件与合规
    // ===================================

    private List<ReleaseBatchDtos.FileChecklistItem> fileChecklist(BatchContext ctx) {
        List<FileRequirementConfig> configs = fileRequirementService.listAll();
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
            if (uploaded < expected && "REQUIRED".equals(config.getRequirement())) {
                item.setNote("缺 " + (expected - uploaded));
            } else if ("OPTIONAL".equals(config.getRequirement())) {
                item.setNote("可空缺");
            }
            result.add(item);
        }
        return result;
    }

    private int expectedCount(FileRequirementConfig config, BatchContext ctx) {
        return switch (config.getBizType()) {
            case "PREPARATION" -> ctx.getPreparation() == null ? 0 : 1;
            case "PREPARATION_PROCESS_TEMPLATE" -> ctx.getPreparation() == null ? 0 : 1;
            case "PRODUCTION_PLAN" -> ctx.getPlan() == null ? 0 : 1;
            case "PURCHASE_ORDER" -> ctx.getPurchases().size();
            case "ACCEPTANCE_ORDER" -> ctx.getAcceptances().size();
            case "WORK_ORDER" -> 1;
            case "INSPECTION_REQUEST" -> ctx.getRequests().size();
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
            case "PREPARATION" -> ctx.getPreparation() != null
                    && hasFile(bizType, ctx.getPreparation().getPreparationId(), kind) ? 1 : 0;
            case "PREPARATION_PROCESS_TEMPLATE" -> 0;
            case "PRODUCTION_PLAN" -> ctx.getPlan() != null
                    && hasFile(bizType, ctx.getPlan().getId().longValue(), kind) ? 1 : 0;
            case "PURCHASE_ORDER" -> (int) ctx.getPurchases().stream()
                    .filter(p -> hasFile(bizType, p.getId(), kind)).count();
            case "ACCEPTANCE_ORDER" -> (int) ctx.getAcceptances().stream()
                    .filter(a -> hasFile(bizType, a.getAcceptanceId(), kind)).count();
            case "WORK_ORDER" -> hasFile(bizType, ctx.getWorkOrder().getWorkOrderId(), kind) ? 1 : 0;
            case "INSPECTION_REQUEST" -> (int) ctx.getRequests().stream()
                    .filter(r -> hasFile(bizType, r.getId(), kind)).count();
            case "INSPECTION_RECORD" -> (int) ctx.getInspections().stream()
                    .filter(r -> hasFile(bizType, r.getId(), kind)).count();
            case "RELEASE_REVIEW" -> ctx.getRelease() != null
                    && hasFile(bizType, ctx.getRelease().getId(), kind) ? 1 : 0;
            case "STOCK_OUT_PRODUCTION" -> (int) ctx.getPicks().stream()
                    .filter(p -> p.getStockOutId() != null && hasFile(bizType, p.getStockOutId(), kind)).count();
            case "STOCK_IN_PRODUCT" -> (int) ctx.getStockIns().stream()
                    .filter(s -> hasFile(bizType, s.getInId(), kind)).count();
            case "STOCK_OUT_SALES" -> (int) ctx.getStockOuts().stream()
                    .filter(s -> hasFile(bizType, s.getOutId(), kind)).count();
            default -> 0;
        };
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
        c.setYieldPass(yieldRate(ctx) != null && yieldRate(ctx).compareTo(new BigDecimal("98")) >= 0);
        c.setCanRelease(c.isInspectionPass() && c.isProcessPass() && c.isFilePass()
                && c.isDeviationPass() && c.isYieldPass());
        c.setSummary(c.isCanRelease() ? "全部合规，可以放行" : "存在不符合项，暂不能放行");
        return c;
    }

    private boolean deviationOpen(DeviationRecord d) {
        return d.getHandleTime() == null && !StringUtils.hasText(d.getResultDesc());
    }

    private String inspectionConclusion(BatchContext ctx) {
        return ctx.getInspections().isEmpty() ? "未检验" : ctx.getInspections().get(0).getConclusion();
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
            if ("放行".equals(conclusion)) return "已放行";
            if (conclusion.contains("拒绝")) return "已拒绝";
            return "待审核";
        }
        return compliance(ctx).isCanRelease() ? "待放行" : "待审核";
    }

    private ReleaseBatchDtos.ReleaseInfo releaseInfo(BatchContext ctx) {
        if (ctx.getRelease() == null) {
            return null;
        }
        ReleaseBatchDtos.ReleaseInfo info = new ReleaseBatchDtos.ReleaseInfo();
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
        if (request == null || request.getItems() == null) {
            return;
        }
        for (ReleaseBatchDtos.ProcessCheckItem item : request.getItems()) {
            WorkOrderProcessCheck check = processCheckMapper.selectOne(new QueryWrapper<WorkOrderProcessCheck>()
                    .eq("work_order_id", workOrderId)
                    .eq(item.getExecutionId() != null, "execution_id", item.getExecutionId())
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
        ReleaseBatchDtos.Compliance compliance = compliance(ctx);
        if (!compliance.isCanRelease()) {
            throw new IllegalArgumentException("存在不符合项，不能通过放行");
        }
        saveRelease(ctx, "放行", request == null ? null : request.getApprover(),
                request == null ? null : request.getReviewTime(),
                request == null ? "合规检查通过，同意放行。" : request.getOpinion());
        WorkOrder wo = ctx.getWorkOrder();
        wo.setAuditReleaseTime(LocalDateTime.now());
        workOrderMapper.updateById(wo);
        return getBatchDetail(workOrderId);
    }

    @Override
    @Transactional
    public ReleaseBatchDtos.BatchDetail reject(Long workOrderId, ReleaseBatchDtos.RejectRequest request) {
        BatchContext ctx = loadContext(workOrderId);
        saveRelease(ctx, "拒绝放行", request == null ? null : request.getReviewer(),
                request == null ? null : request.getReviewTime(),
                request == null ? "审核不通过，拒绝放行。" : request.getReason());
        return getBatchDetail(workOrderId);
    }

    private BatchContext loadContext(Long workOrderId) {
        WorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return batchQueryService.load(wo);
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
        if (!ctx.getInspections().isEmpty()) {
            release.setRelatedInspectionCode(ctx.getInspections().get(0).getInspectionCode());
            release.setRelatedInspectionRecordCode(ctx.getInspections().get(0).getInspectionCode());
        }
        release.setReleaseConclusion(conclusion);
        release.setReviewOpinion(opinion);
        release.setReviewer(StringUtils.hasText(reviewer) ? reviewer : "赵磊");
        release.setReviewTime(reviewTime == null ? LocalDateTime.now() : reviewTime);
        if (release.getId() == null) {
            releaseReviewService.save(release);
        } else {
            releaseReviewService.updateById(release);
        }
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
            item.setTime(f.getCreatedTime() == null ? null : String.valueOf(f.getCreatedTime()));
            item.setUploader("");
            return item;
        }).collect(Collectors.toList());
    }

    private LocalDate date(LocalDateTime time) {
        return time == null ? null : time.toLocalDate();
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

package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.BatchContext;
import com.tonghui.erp.Common.Dto.QualityPages.ProductSummaryDtos;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.FileRequirementConfig;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.BatchQueryService;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileRequirementService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.ProductSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 产品信息汇总服务实现
 */
@Service
public class ProductSummaryServiceImpl implements ProductSummaryService {

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private PreparationService preparationService;
    @Autowired
    private BatchQueryService batchQueryService;
    @Autowired
    private BusinessFileService businessFileService;
    @Autowired
    private FileRequirementService fileRequirementService;
    @Autowired
    private FileInfoService fileInfoService;

    // region 列表
    // ===================================
    // 列表
    // ===================================

    @Override
    public PagedResult<ProductSummaryDtos.BatchSummary> queryBatches(String varietyCode, String dateType,
                                                                     String startDate, String endDate,
                                                                     String keyword, boolean onlyMissing,
                                                                     int pageIndex, int pageSize) {
        LoadedData data = loadData(varietyCode, dateType, startDate, endDate, keyword, onlyMissing);
        List<ProductSummaryDtos.BatchSummary> all = data.summaries;
        int safeSize = Math.max(pageSize, 1);
        int from = Math.min(Math.max(pageIndex, 0) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        PagedResult<ProductSummaryDtos.BatchSummary> result = new PagedResult<>();
        result.setItems(all.subList(from, to));
        result.setTotalCount(all.size());
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public java.util.List<ProductSummaryDtos.BatchSummary> queryCards(String varietyCode, String dateType,
                                                                      String startDate, String endDate,
                                                                      String keyword, boolean onlyMissing) {
        return loadData(varietyCode, dateType, startDate, endDate, keyword, onlyMissing).summaries;
    }

    /**
     * 加载筛选范围内的批次和未下达计划，列表、卡片、概览共用
     */
    private LoadedData loadData(String varietyCode, String dateType, String startDate, String endDate,
                                String keyword, boolean onlyMissing) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        LoadedData data = new LoadedData();

        QueryWrapper<WorkOrder> allWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(varietyCode)) {
            allWrapper.eq("preparation_code", varietyCode);
        }
        List<WorkOrder> allWorkOrders = workOrderMapper.selectList(allWrapper);
        Set<Long> planIdsWithWorkOrders = allWorkOrders.stream()
                .map(WorkOrder::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(varietyCode)) {
            wrapper.eq("preparation_code", varietyCode);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("work_order_code", keyword)
                    .or().like("batch_number", keyword)
                    .or().like("preparation_name", keyword));
        }
        wrapper.orderByDesc("created_time", "work_order_id");
        List<WorkOrder> workOrders = workOrderMapper.selectList(wrapper);

        for (BatchContext ctx : batchQueryService.loadAll(workOrders)) {
            ProductSummaryDtos.BatchSummary summary = toSummary(ctx);
            if (!inDateRange(rowDate(ctx, dateType), start, end)) continue;
            if (onlyMissing && summary.getMissingCount() == 0) continue;
            data.summaries.add(summary);
            data.contexts.add(ctx);
        }

        QueryWrapper<ProductionPlan> planWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(varietyCode)) {
            planWrapper.eq("preparation_code", varietyCode);
        }
        if (StringUtils.hasText(keyword)) {
            planWrapper.and(w -> w.like("plan_number", keyword)
                    .or().like("preparation_name", keyword)
                    .or().like("preparation_code", keyword));
        }
        planWrapper.orderByDesc("plan_production_time", "id");
        List<ProductionPlan> plans = productionPlanMapper.selectList(planWrapper);
        for (ProductionPlan plan : plans) {
            if (plan.getId() == null || planIdsWithWorkOrders.contains(plan.getId().longValue())) {
                continue;
            }
            // 未下达计划只有计划日期；选择工单/入库/检验日期口径时不纳入
            if (StringUtils.hasText(dateType) && !"planDate".equals(dateType)) {
                continue;
            }
            BatchContext planContext = batchQueryService.loadByPlan(plan);
            ProductSummaryDtos.BatchSummary summary = toPlanSummary(plan, planContext);
            if (!inDateRange(planDate(plan), start, end)) continue;
            if (onlyMissing && summary.getMissingCount() == 0) continue;
            data.summaries.add(summary);
            data.planOnlyPlans.add(plan);
        }

        data.summaries.sort(Comparator.comparing(ProductSummaryDtos.BatchSummary::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return data;
    }

    @Override
    public ProductSummaryDtos.Overview getOverview(String varietyCode, String dateType,
                                                    String startDate, String endDate,
                                                    String keyword, boolean onlyMissing) {
        LoadedData data = loadData(varietyCode, dateType, startDate, endDate, keyword, onlyMissing);
        List<ProductSummaryDtos.BatchSummary> rows = data.summaries;

        ProductSummaryDtos.Overview overview = new ProductSummaryDtos.Overview();
        overview.setGeneratedAt(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        int batchCount = (int) rows.stream().filter(r -> r.getWorkOrderId() != null).count();
        int missingRecords = (int) rows.stream().filter(r -> r.getMissingCount() > 0).count();
        int missingItems = rows.stream().mapToInt(ProductSummaryDtos.BatchSummary::getMissingCount).sum();
        int varietyCount = (int) rows.stream().map(ProductSummaryDtos.BatchSummary::getPreparationCode)
                .filter(StringUtils::hasText).distinct().count();

        List<ProductSummaryDtos.TodoItem> todo = rows.stream()
                .filter(r -> r.getMissingCount() > 0)
                .sorted(Comparator.comparingInt(ProductSummaryDtos.BatchSummary::getMissingCount).reversed())
                .limit(5)
                .map(this::toTodo)
                .collect(Collectors.toList());

        ProductSummaryDtos.Verdict verdict = new ProductSummaryDtos.Verdict();
        verdict.setVarietyCount(varietyCount);
        verdict.setRecordCount(rows.size());
        verdict.setBatchCount(batchCount);
        verdict.setMissingRecords(missingRecords);
        verdict.setMissingItems(missingItems);
        verdict.setTopPriority(todo.isEmpty() ? null : todo.get(0));
        overview.setVerdict(verdict);
        overview.setTodo(todo);

        List<ProductSummaryDtos.SlotState> slots = new ArrayList<>();
        for (BatchContext ctx : data.contexts) {
            slots.addAll(slotStates(ctx));
        }
        for (ProductionPlan plan : data.planOnlyPlans) {
            slots.addAll(planOnlySlots(plan));
        }
        slots = slots.stream()
                .filter(s -> s.getExpected() > 0 && !"OPTIONAL".equals(s.getRequirement()))
                .collect(Collectors.toList());

        Map<String, ProductSummaryDtos.StageHealth> stageMap = new LinkedHashMap<>();
        Map<String, ProductSummaryDtos.SlotHealth> slotMap = new LinkedHashMap<>();
        for (ProductSummaryDtos.SlotState slot : slots) {
            ProductSummaryDtos.StageHealth stage = stageMap.computeIfAbsent(slot.getStage(), key -> {
                ProductSummaryDtos.StageHealth item = new ProductSummaryDtos.StageHealth();
                item.setStage(key);
                return item;
            });
            stage.setExpected(stage.getExpected() + slot.getExpected());
            stage.setUploaded(stage.getUploaded() + slot.getUploaded());
            stage.setMissing(stage.getMissing() + slot.getMissing());

            String slotKey = slot.getStage() + "|" + slot.getLabel() + "|" + slot.getRequirement();
            ProductSummaryDtos.SlotHealth slotHealth = slotMap.computeIfAbsent(slotKey, key -> {
                ProductSummaryDtos.SlotHealth item = new ProductSummaryDtos.SlotHealth();
                item.setStage(slot.getStage());
                item.setLabel(slot.getLabel());
                item.setRequirement(slot.getRequirement());
                return item;
            });
            slotHealth.setExpected(slotHealth.getExpected() + slot.getExpected());
            slotHealth.setUploaded(slotHealth.getUploaded() + slot.getUploaded());
            slotHealth.setMissing(slotHealth.getMissing() + slot.getMissing());
        }

        List<ProductSummaryDtos.StageHealth> stages = stageMap.values().stream()
                .sorted(Comparator.comparingInt(s -> stageOrder(s.getStage())))
                .collect(Collectors.toList());
        List<ProductSummaryDtos.SlotHealth> allSlots = slotMap.values().stream()
                .sorted(Comparator.comparingInt((ProductSummaryDtos.SlotHealth s) -> stageOrder(s.getStage()))
                        .thenComparing(ProductSummaryDtos.SlotHealth::getLabel))
                .collect(Collectors.toList());
        List<ProductSummaryDtos.SlotHealth> topSlots = allSlots.stream()
                .filter(s -> s.getMissing() > 0)
                .sorted(Comparator.comparingInt(ProductSummaryDtos.SlotHealth::getMissing).reversed())
                .limit(5)
                .collect(Collectors.toList());

        ProductSummaryDtos.Health health = new ProductSummaryDtos.Health();
        health.setStages(stages);
        health.setTopSlots(topSlots);
        health.setAllSlots(allSlots);
        overview.setHealth(health);

        int expected = allSlots.stream().mapToInt(ProductSummaryDtos.SlotHealth::getExpected).sum();
        int uploaded = allSlots.stream().mapToInt(ProductSummaryDtos.SlotHealth::getUploaded).sum();
        int qualified = (int) rows.stream().filter(r -> "合格".equals(r.getInspectionConclusion())).count();
        int inspectionTotal = (int) rows.stream()
                .filter(r -> StringUtils.hasText(r.getInspectionConclusion())
                        && !"未检验".equals(r.getInspectionConclusion()))
                .count();
        ProductSummaryDtos.Kpi kpi = new ProductSummaryDtos.Kpi();
        kpi.setTotalBatches(batchCount);
        kpi.setRunningBatches((int) rows.stream()
                .filter(r -> r.getWorkOrderId() != null && !isDoneStatus(r.getStatus()))
                .count());
        kpi.setMissingItems(missingItems);
        kpi.setMissingRecords(missingRecords);
        kpi.setFileExpected(expected);
        kpi.setFileUploaded(uploaded);
        kpi.setCompletenessRate(expected == 0 ? 100 : (int) Math.round(uploaded * 100.0 / expected));
        kpi.setInspectionTotal(inspectionTotal);
        kpi.setInspectionQualified(qualified);
        kpi.setInspectionRate(inspectionTotal == 0 ? null : (int) Math.round(qualified * 100.0 / inspectionTotal));
        overview.setKpis(kpi);

        Map<String, ProductSummaryDtos.PreparationSummary> prepMap = new LinkedHashMap<>();
        for (ProductSummaryDtos.BatchSummary row : rows) {
            if (!StringUtils.hasText(row.getPreparationCode())) continue;
            ProductSummaryDtos.PreparationSummary prep = prepMap.computeIfAbsent(row.getPreparationCode(), key -> {
                ProductSummaryDtos.PreparationSummary p = new ProductSummaryDtos.PreparationSummary();
                p.setPreparationCode(key);
                p.setPreparationName(row.getPreparationName());
                return p;
            });
            if (row.getWorkOrderId() != null) prep.setBatchCount(prep.getBatchCount() + 1);
            if (row.getMissingCount() > 0) prep.setMissingCount(prep.getMissingCount() + 1);
        }
        overview.setPreparations(new ArrayList<>(prepMap.values()));
        return overview;
    }

    private ProductSummaryDtos.TodoItem toTodo(ProductSummaryDtos.BatchSummary row) {
        ProductSummaryDtos.TodoItem item = new ProductSummaryDtos.TodoItem();
        item.setRecordKey(row.getRecordKey());
        item.setPlanId(row.getPlanId());
        item.setWorkOrderId(row.getWorkOrderId());
        item.setPreparationName(row.getPreparationName());
        item.setCode(row.getWorkOrderCode() != null ? row.getWorkOrderCode() : row.getPlanNumber());
        item.setBatchNo(row.getBatchNo());
        item.setMissingCount(row.getMissingCount());
        item.setStageBrief(row.getMissingSummary());
        return item;
    }

    @Override
    public ProductSummaryDtos.BatchDetail getPlanDetail(Long planId) {
        ProductionPlan plan = productionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("生产计划不存在");
        }
        BatchContext ctx = batchQueryService.loadByPlan(plan);
        ProductSummaryDtos.BatchDetail detail = new ProductSummaryDtos.BatchDetail();
        detail.setBatch(toPlanSummary(plan, ctx));
        detail.setSlots(planOnlySlots(plan));
        detail.setFiles(planOnlyFileGroups(plan));
        detail.setMissingItems(planOnlyMissing(plan));
        detail.setStages(planOnlyStages(ctx));
        return detail;
    }

    /**
     * 未下达工单的计划节点：计划、采购、验收入库有数据时展示，其余节点固定为待处理
     *
     * @param ctx 计划上下文
     * @return 固定顺序的链路节点
     */
    private List<ProductSummaryDtos.StageItem> planOnlyStages(BatchContext ctx) {
        ProductionPlan plan = ctx.getPlan();
        List<ProductSummaryDtos.StageItem> stages = new ArrayList<>();
        stages.add(stage("plan", "计划", "done", plan.getPlanNumber(), planMeta(ctx),
                List.of(planDocument(plan))));
        addPurchaseStage(stages, ctx);
        addAcceptanceStage(stages, ctx);
        stages.add(pendingStage("workOrder", "生产任务"));
        stages.add(pendingStage("pick", "生产领料"));
        stages.add(pendingStage("production", "生产过程"));
        stages.add(pendingStage("inspection", "检验"));
        stages.add(pendingStage("finishedInbound", "成品入库"));
        stages.add(pendingStage("release", "审核放行"));
        stages.add(pendingStage("finishedOutbound", "成品出库"));
        return stages;
    }

    private ProductSummaryDtos.DocumentItem planDocument(ProductionPlan plan) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("生产计划");
        doc.setCode(plan.getPlanNumber());
        doc.setDate(planDate(plan));
        doc.setStatus(plan.getCurrentStatus());
        doc.setFiles(toFiles(businessFileService.list("PRODUCTION_PLAN", plan.getId().longValue(), null, null)));
        return doc;
    }

    private List<ProductSummaryDtos.SlotState> planOnlySlots(ProductionPlan plan) {
        List<ProductSummaryDtos.SlotState> result = new ArrayList<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            List<Long> ids = planSlotDocumentIds(config, plan);
            if (ids.isEmpty()) continue;
            ProductSummaryDtos.SlotState slot = new ProductSummaryDtos.SlotState();
            slot.setStage(config.getStage());
            slot.setNodeKey(nodeKeyOfStage(config.getStage()));
            slot.setLabel(StringUtils.hasText(config.getDisplayName())
                    ? config.getDisplayName() : config.getBizType() + " / " + config.getFileKind());
            slot.setBizType(config.getBizType());
            slot.setFileKind(config.getFileKind());
            slot.setRequirement(config.getRequirement());
            slot.setExpected(ids.size());
            slot.setUploaded((int) ids.stream().filter(id -> !files(config.getBizType(), id, config.getFileKind()).isEmpty()).count());
            slot.setMissing(Math.max(0, slot.getExpected() - slot.getUploaded()));
            result.add(slot);
        }
        return result;
    }

    private List<ProductSummaryDtos.FileGroup> planOnlyFileGroups(ProductionPlan plan) {
        List<ProductSummaryDtos.FileGroup> result = new ArrayList<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            List<Long> ids = planSlotDocumentIds(config, plan);
            if (ids.isEmpty()) continue;
            List<BusinessFileDto> files = new ArrayList<>();
            for (Long id : ids) {
                files.addAll(files(config.getBizType(), id, config.getFileKind()));
            }
            if (files.isEmpty()) continue;
            ProductSummaryDtos.FileGroup group = new ProductSummaryDtos.FileGroup();
            group.setStage(config.getStage());
            group.setNodeKey(nodeKeyOfStage(config.getStage()));
            group.setLabel(StringUtils.hasText(config.getDisplayName())
                    ? config.getDisplayName() : config.getBizType() + " / " + config.getFileKind());
            group.setBizType(config.getBizType());
            group.setFileKind(config.getFileKind());
            group.setRequirement(config.getRequirement());
            group.setSlotCode(config.getBizType() + " / " + config.getFileKind());
            group.setFiles(toFiles(files));
            result.add(group);
        }
        return result;
    }

    private List<ProductSummaryDtos.MissingItem> planOnlyMissing(ProductionPlan plan) {
        List<ProductSummaryDtos.MissingItem> result = new ArrayList<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            if ("OPTIONAL".equals(config.getRequirement())) continue;
            List<Long> ids = planSlotDocumentIds(config, plan);
            for (Long id : ids) {
                if (files(config.getBizType(), id, config.getFileKind()).isEmpty()) {
                    ProductSummaryDtos.MissingItem item = new ProductSummaryDtos.MissingItem();
                    item.setStage(config.getStage());
                    item.setNodeKey(nodeKeyOfStage(config.getStage()));
                    item.setLabel(StringUtils.hasText(config.getDisplayName())
                            ? config.getDisplayName() : config.getBizType());
                    item.setDoc(plan.getPlanNumber());
                    item.setSlotCode(config.getBizType() + " / " + config.getFileKind());
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<Long> planSlotDocumentIds(FileRequirementConfig config, ProductionPlan plan) {
        return switch (config.getBizType()) {
            case "PREPARATION", "PREPARATION_PROCESS_TEMPLATE" -> plan.getPreparationId() == null
                    ? Collections.emptyList() : List.of(plan.getPreparationId());
            case "PRODUCTION_PLAN" -> plan.getId() == null ? Collections.emptyList() : List.of(plan.getId().longValue());
            default -> Collections.emptyList();
        };
    }

    private ProductSummaryDtos.BatchSummary toPlanSummary(ProductionPlan plan, BatchContext ctx) {
        Preparation preparation = ctx.getPreparation() != null ? ctx.getPreparation()
                : (plan.getPreparationId() == null ? null : preparationService.getById(plan.getPreparationId()));
        ProductSummaryDtos.BatchSummary dto = new ProductSummaryDtos.BatchSummary();
        dto.setRecordKey(plan.getId() + "__none");
        dto.setPlanId(plan.getId() == null ? null : plan.getId().longValue());
        dto.setPlanNumber(plan.getPlanNumber());
        dto.setPreparationCode(plan.getPreparationCode());
        dto.setPreparationName(plan.getPreparationName());
        dto.setSpec(preparation == null ? null : preparation.getSpec());
        dto.setUnitName(StringUtils.hasText(plan.getUnitName()) ? plan.getUnitName()
                : (preparation == null ? "" : preparation.getUnitName()));
        dto.setBatchNo(null);
        dto.setPlanQty(plan.getPlanQuantity());
        dto.setFinishedQty(plan.getFinishedQuantity());
        dto.setInboundQty(BigDecimal.ZERO);
        dto.setOutboundQty(BigDecimal.ZERO);
        dto.setPlanDate(planDate(plan));
        dto.setProductionStart(date(plan.getProductionStartTime()));
        dto.setProductionEnd(date(plan.getProductionEndTime()));
        dto.setStatus(plan.getCurrentStatus());
        dto.setProcessTotal(0);
        dto.setProcessCompleted(0);
        dto.setFileCount(planOnlyFileGroups(plan).stream().mapToInt(g -> g.getFiles().size()).sum());
        List<ProductSummaryDtos.MissingItem> missing = planOnlyMissing(plan);
        dto.setMissingCount(missing.size());
        dto.setMissingSummary(missingSummary(missing));
        List<ProductSummaryDtos.SlotState> slots = planOnlySlots(plan).stream()
                .filter(s -> !"OPTIONAL".equals(s.getRequirement()))
                .collect(Collectors.toList());
        dto.setFileExpected(slots.stream().mapToInt(ProductSummaryDtos.SlotState::getExpected).sum());
        dto.setFileUploaded(slots.stream().mapToInt(ProductSummaryDtos.SlotState::getUploaded).sum());
        dto.setStageStates(planOnlyStageStates(ctx));
        return dto;
    }

    /**
     * 未下达工单计划的节点状态
     *
     * @param ctx 计划上下文
     * @return 固定顺序的节点状态
     */
    private List<ProductSummaryDtos.StageState> planOnlyStageStates(BatchContext ctx) {
        List<ProductSummaryDtos.StageState> states = new ArrayList<>();
        states.add(stageState("plan", "计划", ctx.getPlan() == null ? "skipped" : "done"));
        states.add(purchaseStageState(ctx));
        states.add(stageState("acceptance", "验收入库", acceptanceState(ctx)));
        states.add(stageState("workOrder", "生产任务", "pending"));
        states.add(stageState("pick", "生产领料", "pending"));
        states.add(stageState("production", "生产过程", "pending"));
        states.add(stageState("inspection", "检验", "pending"));
        states.add(stageState("finishedInbound", "成品入库", "pending"));
        states.add(stageState("release", "审核放行", "pending"));
        states.add(stageState("finishedOutbound", "成品出库", "pending"));
        return states;
    }

    /**
     * 批次节点状态：与详情节点使用同一套 key、标签和顺序
     *
     * @param ctx 批次上下文
     * @return 固定顺序的节点状态
     */
    private List<ProductSummaryDtos.StageState> stageStates(BatchContext ctx) {
        List<ProductSummaryDtos.StageState> states = new ArrayList<>();
        states.add(stageState("plan", "计划", ctx.getPlan() == null ? "skipped" : "done"));
        states.add(purchaseStageState(ctx));
        states.add(stageState("acceptance", "验收入库", acceptanceState(ctx)));
        states.add(stageState("workOrder", "生产任务", workOrderState(ctx)));
        states.add(stageState("pick", "生产领料", pickState(ctx)));
        states.add(stageState("production", "生产过程", productionState(ctx)));
        states.add(stageState("inspection", "检验", inspectionState(ctx)));
        states.add(stageState("finishedInbound", "成品入库", finishedInboundState(ctx)));
        states.add(stageState("release", "审核放行", releaseState(ctx)));
        states.add(stageState("finishedOutbound", "成品出库", finishedOutboundState(ctx)));
        return states;
    }

    /**
     * 采购节点状态：无采购但存在验收/物料入库时标记为不适用
     *
     * @param ctx 批次上下文
     * @return 节点状态
     */
    private ProductSummaryDtos.StageState purchaseStageState(BatchContext ctx) {
        String state = purchaseState(ctx);
        return stageState("purchase", "采购", state,
                "skipped".equals(state) ? "无采购，物料直接入库" : null);
    }

    private String purchaseState(BatchContext ctx) {
        List<PurchaseOrders> purchases = ctx.getPurchases();
        if (purchases.isEmpty()) {
            return hasMaterialInboundEvidence(ctx) ? "skipped" : "pending";
        }
        boolean allDone = purchases.stream().allMatch(purchase -> isPurchaseDone(purchase.getStatus()));
        return allDone ? "done" : "active";
    }

    private boolean isPurchaseDone(Object status) {
        String value = status == null ? "" : String.valueOf(status);
        return "已入库".equals(value) || "已结束".equals(value)
                || "已归档".equals(value) || "已完成".equals(value);
    }

    private boolean hasMaterialInboundEvidence(BatchContext ctx) {
        boolean hasAcceptance = materialAcceptances(ctx).stream()
                .anyMatch(acceptance -> !"已作废".equals(acceptance.getStatus()));
        return hasAcceptance || hasEffectiveStockIn(ctx.getMaterialStockIns());
    }

    /**
     * 有效入库单：排除草稿和已取消
     */
    private boolean isEffectiveStockIn(StockIn stockIn) {
        String status = stockIn.getInStatus();
        return status == null || (!"草稿".equals(status) && !"已取消".equals(status));
    }

    private boolean hasEffectiveStockIn(List<StockIn> stockIns) {
        return stockIns.stream().anyMatch(this::isEffectiveStockIn);
    }

    /**
     * 有效出库单：排除草稿和已取消
     */
    private boolean isEffectiveStockOut(StockOut stockOut) {
        String status = stockOut.getOutStatus();
        return status == null || (!"草稿".equals(status) && !"已取消".equals(status));
    }

    private boolean hasEffectiveStockOut(List<StockOut> stockOuts) {
        return stockOuts.stream().anyMatch(this::isEffectiveStockOut);
    }

    private String acceptanceState(BatchContext ctx) {
        if (hasEffectiveStockIn(ctx.getMaterialStockIns())) {
            return "done";
        }
        boolean active = materialAcceptances(ctx).stream()
                .anyMatch(acceptance -> !"已作废".equals(acceptance.getStatus()));
        return active ? "active" : "pending";
    }

    private String workOrderState(BatchContext ctx) {
        WorkOrder workOrder = ctx.getWorkOrder();
        if (workOrder == null) {
            return "pending";
        }
        if (workOrder.getConfigCompleteTime() != null || workOrder.getFinishedQty() != null) {
            return "done";
        }
        return workOrder.getConfigDate() == null ? "pending" : "active";
    }

    private String pickState(BatchContext ctx) {
        List<ProductionPick> picks = ctx.getPicks().stream()
                .filter(pick -> !"已作废".equals(pick.getStatus()))
                .collect(Collectors.toList());
        if (picks.isEmpty()) {
            return "pending";
        }
        return picks.stream().allMatch(pick -> "已发料".equals(pick.getStatus())) ? "done" : "active";
    }

    private String productionState(BatchContext ctx) {
        List<WorkOrderProcessExecution> executions = ctx.getExecutions();
        if (executions.isEmpty()) {
            return "pending";
        }
        if (executions.stream().allMatch(execution -> execution.getEndTime() != null)) {
            return "done";
        }
        return executions.stream().anyMatch(execution -> execution.getStartTime() != null) ? "active" : "pending";
    }

    private String inspectionState(BatchContext ctx) {
        List<InspectionRecord> finished = finishedInspections(ctx);
        if (finished.isEmpty()) {
            return !ctx.getRequests().isEmpty() || !ctx.getSamplings().isEmpty() ? "active" : "pending";
        }
        String conclusion = inspectionConclusion(ctx);
        if ("合格".equals(conclusion)) {
            return "done";
        }
        if ("不合格".equals(conclusion)) {
            return "error";
        }
        return "active";
    }

    private String finishedInboundState(BatchContext ctx) {
        if (hasEffectiveStockIn(ctx.getStockIns())) {
            return "done";
        }
        return "合格".equals(inspectionConclusion(ctx)) ? "active" : "pending";
    }

    private String releaseState(BatchContext ctx) {
        if (ctx.getRelease() == null) {
            return "done".equals(finishedInboundState(ctx)) ? "active" : "pending";
        }
        return "拒绝放行".equals(ctx.getRelease().getReleaseConclusion()) ? "error" : "done";
    }

    private String finishedOutboundState(BatchContext ctx) {
        if (hasEffectiveStockOut(ctx.getStockOuts())) {
            return "done";
        }
        return "done".equals(releaseState(ctx)) ? "active" : "pending";
    }

    private ProductSummaryDtos.StageState stageState(String key, String label, String state) {
        return stageState(key, label, state, null);
    }

    private ProductSummaryDtos.StageState stageState(String key, String label, String state, String skipReason) {
        ProductSummaryDtos.StageState item = new ProductSummaryDtos.StageState();
        item.setKey(key);
        item.setLabel(label);
        item.setState(state);
        item.setSkipReason(skipReason);
        return item;
    }

    private BigDecimal inboundQty(BatchContext ctx) {
        return ctx.getStockIns().stream()
                .filter(this::isEffectiveStockIn)
                .flatMap(s -> ctx.getStockInDetails() == null ? java.util.stream.Stream.empty()
                        : ctx.getStockInDetails().getOrDefault(s.getInId(), Collections.emptyList()).stream())
                .map(StockInDetail::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal outboundQty(BatchContext ctx) {
        return ctx.getStockOuts().stream()
                .filter(this::isEffectiveStockOut)
                .flatMap(s -> ctx.getStockOutDetails() == null ? java.util.stream.Stream.empty()
                        : ctx.getStockOutDetails().getOrDefault(s.getOutId(), Collections.emptyList()).stream())
                .map(StockOutDetail::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean inDateRange(LocalDate day, LocalDate start, LocalDate end) {
        if (start != null && (day == null || day.isBefore(start))) return false;
        return end == null || (day != null && !day.isAfter(end));
    }

    private boolean isDoneStatus(String status) {
        return "已完成".equals(status) || "异常".equals(status) || "不合格".equals(status);
    }

    private int stageOrder(String stage) {
        return switch (stage == null ? "" : stage) {
            case "品种资料" -> 0;
            case "计划" -> 1;
            case "采购" -> 2;
            case "验收" -> 3;
            case "工单" -> 4;
            case "领料" -> 5;
            case "工序" -> 6;
            case "检验" -> 7;
            case "入库" -> 8;
            case "放行" -> 9;
            case "出库" -> 10;
            default -> 99;
        };
    }

    /**
     * 文件配置环节映射到链路节点编码
     *
     * @param stage 文件配置环节
     * @return 节点编码
     */
    private String nodeKeyOfStage(String stage) {
        return switch (stage == null ? "" : stage) {
            case "品种资料", "计划" -> "plan";
            case "采购" -> "purchase";
            case "验收" -> "acceptance";
            case "工单" -> "workOrder";
            case "领料" -> "pick";
            case "工序" -> "production";
            case "检验" -> "inspection";
            case "入库" -> "finishedInbound";
            case "放行" -> "release";
            case "出库" -> "finishedOutbound";
            default -> null;
        };
    }

    private LocalDate planDate(ProductionPlan plan) {
        if (plan == null) return null;
        LocalDateTime time = plan.getPlanProductionTime() != null ? plan.getPlanProductionTime() : plan.getCreatedTime();
        return date(time);
    }

    private static class LoadedData {
        private final List<ProductSummaryDtos.BatchSummary> summaries = new ArrayList<>();
        private final List<BatchContext> contexts = new ArrayList<>();
        private final List<ProductionPlan> planOnlyPlans = new ArrayList<>();
    }

    private ProductSummaryDtos.BatchSummary toSummary(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        ProductSummaryDtos.BatchSummary dto = new ProductSummaryDtos.BatchSummary();
        Long planId = ctx.getPlan() == null ? null : ctx.getPlan().getId().longValue();
        dto.setRecordKey((planId == null ? "0" : planId) + "__" + wo.getWorkOrderId());
        dto.setWorkOrderId(wo.getWorkOrderId());
        dto.setWorkOrderCode(wo.getWorkOrderCode());
        dto.setPlanId(planId);
        dto.setPlanNumber(ctx.getPlan() == null ? wo.getPlanName() : ctx.getPlan().getPlanNumber());
        dto.setPreparationCode(wo.getPreparationCode());
        dto.setPreparationName(wo.getPreparationName());
        dto.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        dto.setUnitName(unitName(ctx));
        dto.setBatchNo(wo.getBatchNumber());
        dto.setPlanQty(wo.getBatchQty());
        dto.setFinishedQty(wo.getFinishedQty());
        dto.setInboundQty(inboundQty(ctx));
        dto.setOutboundQty(outboundQty(ctx));
        dto.setMaterialSource(materialSource(ctx));
        dto.setPlanDate(planDate(ctx));
        dto.setWorkOrderDate(date(wo.getConfigDate()));
        dto.setInboundDate(ctx.getStockIns().stream().filter(this::isEffectiveStockIn).findFirst()
                .map(stockIn -> date(stockIn.getInDate())).orElse(null));
        dto.setOutboundDate(ctx.getStockOuts().stream().filter(this::isEffectiveStockOut).findFirst()
                .map(stockOut -> date(stockOut.getOutDate())).orElse(null));
        InspectionRecord latestInspection = latestFinishedInspection(ctx);
        dto.setInspectionDate(latestInspection == null ? null : date(latestInspection.getEndTime()));
        dto.setProductionStart(date(wo.getConfigDate()));
        dto.setProductionEnd(actualEnd(ctx));
        dto.setYieldRate(yieldRate(ctx));
        dto.setInspectionConclusion(latestInspection == null ? null : latestInspection.getConclusion());
        dto.setLatestInspectionCode(latestInspection == null ? null : latestInspection.getInspectionCode());
        dto.setProcessTotal(ctx.getExecutions().size());
        dto.setProcessCompleted((int) ctx.getExecutions().stream().filter(e -> e.getEndTime() != null).count());
        dto.setStatus(wo.getCurrentStatus());

        List<ProductSummaryDtos.FileGroup> groups = fileGroups(ctx);
        dto.setFileCount(groups.stream().mapToInt(g -> g.getFiles().size()).sum());
        List<ProductSummaryDtos.SlotState> slots = slotStates(ctx).stream()
                .filter(s -> !"OPTIONAL".equals(s.getRequirement()))
                .collect(Collectors.toList());
        dto.setFileExpected(slots.stream().mapToInt(ProductSummaryDtos.SlotState::getExpected).sum());
        dto.setFileUploaded(slots.stream().mapToInt(ProductSummaryDtos.SlotState::getUploaded).sum());
        List<ProductSummaryDtos.MissingItem> missing = missingItems(ctx);
        dto.setMissingCount(missing.size());
        dto.setMissingSummary(missingSummary(missing));
        dto.setStageStates(stageStates(ctx));
        return dto;
    }

    // endregion

    // region 详情
    // ===================================
    // 详情
    // ===================================

    @Override
    public ProductSummaryDtos.BatchDetail getBatchDetail(Long workOrderId) {
        WorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        BatchContext ctx = batchQueryService.load(wo);
        ProductSummaryDtos.BatchDetail detail = new ProductSummaryDtos.BatchDetail();
        detail.setBatch(toSummary(ctx));
        detail.setSlots(slotStates(ctx));
        detail.setFiles(fileGroups(ctx));
        detail.setMissingItems(missingItems(ctx));
        detail.setStages(stages(ctx));
        return detail;
    }

    /**
     * 批次全链路节点：固定返回计划、采购、验收入库、生产任务、生产领料、
     * 生产过程、检验、成品入库、审核放行、成品出库 10 个节点。
     *
     * @param ctx 批次上下文
     * @return 固定顺序的链路节点
     */
    private List<ProductSummaryDtos.StageItem> stages(BatchContext ctx) {
        List<ProductSummaryDtos.StageItem> stages = new ArrayList<>();
        WorkOrder wo = ctx.getWorkOrder();

        // 计划
        stages.add(ctx.getPlan() == null
                ? skippedStage("plan", "计划", "无生产计划")
                : stage("plan", "计划", "done", planCode(ctx), planMeta(ctx), List.of(planDoc(ctx))));

        // 采购
        addPurchaseStage(stages, ctx);

        // 验收入库
        addAcceptanceStage(stages, ctx);

        // 生产任务
        stages.add(stage("workOrder", "生产任务", workOrderState(ctx),
                wo.getWorkOrderCode(), "批号 " + safe(wo.getBatchNumber()) + " · " + safe(wo.getCurrentStatus()),
                List.of(workOrderDoc(ctx))));

        // 生产领料
        List<ProductSummaryDtos.DocumentItem> pickDocs = new ArrayList<>();
        ctx.getPicks().forEach(pick -> pickDocs.add(pickDoc(pick, ctx)));
        ctx.getMaterialStockOuts().forEach(stockOut -> pickDocs.add(pickStockOutDoc(stockOut, ctx)));
        stages.add(stage("pick", "生产领料", pickState(ctx),
                ctx.getPicks().isEmpty() ? "暂无领料" : ctx.getPicks().size() + " 张领料单",
                pickMeta(ctx), pickDocs));

        // 生产过程
        stages.add(stage("production", "生产过程", productionState(ctx),
                ctx.getExecutions().isEmpty() ? "暂无工序" : ctx.getExecutions().size() + " 道工序",
                yieldRate(ctx) == null ? "" : "收率 " + yieldRate(ctx) + "%",
                ctx.getExecutions().stream().map(execution -> processDoc(execution, ctx)).collect(Collectors.toList())));

        // 检验
        List<ProductSummaryDtos.DocumentItem> inspectionDocs = new ArrayList<>();
        ctx.getRequests().forEach(request -> inspectionDocs.add(inspectionRequestDoc(request)));
        ctx.getSamplings().forEach(sampling -> inspectionDocs.add(samplingDoc(sampling)));
        ctx.getInspections().forEach(record -> inspectionDocs.add(inspectionRecordDoc(record)));
        List<InspectionRecord> finished = finishedInspections(ctx);
        String conclusion = inspectionConclusion(ctx);
        stages.add(stage("inspection", "检验", inspectionState(ctx),
                conclusion == null ? "暂无检验" : conclusion,
                finished.size() + " 条成品检验记录", inspectionDocs));

        // 成品入库
        stages.add(stage("finishedInbound", "成品入库", finishedInboundState(ctx),
                ctx.getStockIns().isEmpty() ? "未入库" : ctx.getStockIns().size() + " 张入库单", inboundMeta(ctx),
                ctx.getStockIns().stream().map(stockIn -> stockInDoc(stockIn, ctx)).collect(Collectors.toList())));

        // 审核放行
        stages.add(stage("release", "审核放行", releaseState(ctx),
                ctx.getRelease() == null ? "未放行" : ctx.getRelease().getReleaseCode(),
                ctx.getRelease() == null ? "" : safe(ctx.getRelease().getReleaseConclusion()),
                ctx.getRelease() == null ? Collections.emptyList() : List.of(releaseDoc(ctx.getRelease()))));

        // 成品出库
        stages.add(stage("finishedOutbound", "成品出库", finishedOutboundState(ctx),
                ctx.getStockOuts().isEmpty() ? "未出库" : ctx.getStockOuts().size() + " 张出库单",
                outboundMeta(ctx),
                ctx.getStockOuts().stream().map(stockOut -> stockOutDoc(stockOut, ctx)).collect(Collectors.toList())));
        return stages;
    }

    /**
     * 追加采购节点：无采购但有验收/物料入库时标记为不适用
     *
     * @param stages 节点列表
     * @param ctx    批次上下文
     */
    private void addPurchaseStage(List<ProductSummaryDtos.StageItem> stages, BatchContext ctx) {
        if (ctx.getPurchases().isEmpty() && hasMaterialInboundEvidence(ctx)) {
            stages.add(skippedStage("purchase", "采购", "无采购，物料直接入库"));
            return;
        }
        stages.add(stage("purchase", "采购", purchaseState(ctx),
                ctx.getPurchases().isEmpty() ? "无采购" : ctx.getPurchases().size() + " 张采购单",
                purchaseMeta(ctx), purchaseDocuments(ctx)));
    }

    /**
     * 追加验收入库节点：验收单与物料入库单在同一节点展示
     *
     * @param stages 节点列表
     * @param ctx    批次上下文
     */
    private void addAcceptanceStage(List<ProductSummaryDtos.StageItem> stages, BatchContext ctx) {
        stages.add(stage("acceptance", "验收入库", acceptanceState(ctx),
                acceptanceMain(ctx), acceptanceMeta(ctx), acceptanceDocuments(ctx)));
    }

    private List<ProductSummaryDtos.DocumentItem> purchaseDocuments(BatchContext ctx) {
        List<ProductSummaryDtos.DocumentItem> documents = new ArrayList<>();
        ctx.getPurchases().forEach(purchase -> documents.add(purchaseDoc(purchase, ctx)));
        return documents;
    }

    private String purchaseMeta(BatchContext ctx) {
        if (ctx.getPurchases().isEmpty()) {
            return "";
        }
        return ctx.getPurchases().stream()
                .map(purchase -> purchase.getStatus() == null ? "" : String.valueOf(purchase.getStatus()))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(" / "));
    }

    private List<ProductSummaryDtos.DocumentItem> acceptanceDocuments(BatchContext ctx) {
        List<ProductSummaryDtos.DocumentItem> documents = new ArrayList<>();
        materialAcceptances(ctx).forEach(acceptance -> documents.add(acceptanceDoc(acceptance)));
        ctx.getMaterialStockIns().forEach(stockIn -> documents.add(materialStockInDoc(stockIn, ctx)));
        return documents;
    }

    private String acceptanceMain(BatchContext ctx) {
        int acceptanceCount = materialAcceptances(ctx).size();
        int stockInCount = ctx.getMaterialStockIns().size();
        if (acceptanceCount == 0 && stockInCount == 0) {
            return "暂无验收";
        }
        return acceptanceCount + " 张验收单 / " + stockInCount + " 张入库单";
    }

    private String acceptanceMeta(BatchContext ctx) {
        LocalDate latest = null;
        for (AcceptanceOrder acceptance : materialAcceptances(ctx)) {
            LocalDate day = date(acceptance.getAcceptanceTime());
            if (day != null && (latest == null || day.isAfter(latest))) {
                latest = day;
            }
        }
        for (StockIn stockIn : ctx.getMaterialStockIns()) {
            LocalDate day = date(stockIn.getInDate());
            if (day != null && (latest == null || day.isAfter(latest))) {
                latest = day;
            }
        }
        return latest == null ? "" : latest.toString();
    }

    private String pickMeta(BatchContext ctx) {
        if (ctx.getPicks().isEmpty()) {
            return "";
        }
        ProductionPick first = ctx.getPicks().get(0);
        String meta = safe(first.getStatus()) + " · " + safe(first.getApplicant());
        if (!ctx.getMaterialStockOuts().isEmpty()) {
            meta += " · 出库 " + safe(ctx.getMaterialStockOuts().get(0).getOutCode());
        }
        return meta;
    }

    private String inboundMeta(BatchContext ctx) {
        return ctx.getStockIns().stream().filter(this::isEffectiveStockIn).findFirst()
                .map(stockIn -> date(stockIn.getInDate()))
                .map(day -> day == null ? "" : day.toString())
                .orElse("");
    }

    private String outboundMeta(BatchContext ctx) {
        return ctx.getStockOuts().stream().filter(this::isEffectiveStockOut).findFirst()
                .map(stockOut -> date(stockOut.getOutDate()))
                .map(day -> day == null ? "" : day.toString())
                .orElse("");
    }

    /**
     * 物料验收单：排除成品入库来源
     *
     * @param ctx 批次上下文
     * @return 验收单列表
     */
    private List<AcceptanceOrder> materialAcceptances(BatchContext ctx) {
        if (ctx.getAcceptances() == null) {
            return Collections.emptyList();
        }
        return ctx.getAcceptances().stream()
                .filter(acceptance -> !"成品入库".equals(acceptance.getSourceType()))
                .collect(Collectors.toList());
    }

    private ProductSummaryDtos.StageItem stage(String key, String label, String state, String main,
                                               String meta, List<ProductSummaryDtos.DocumentItem> documents) {
        ProductSummaryDtos.StageItem stage = new ProductSummaryDtos.StageItem();
        stage.setKey(key);
        stage.setLabel(label);
        stage.setState(state);
        stage.setSkipped(false);
        stage.setMain(main);
        stage.setMeta(meta);
        stage.setDocuments(documents);
        return stage;
    }

    /**
     * 不适用节点：业务上确实不存在的环节，如无采购直接入库
     *
     * @param key          节点编码
     * @param label        显示名称
     * @param skipReason   不适用说明
     * @return 节点
     */
    private ProductSummaryDtos.StageItem skippedStage(String key, String label, String skipReason) {
        ProductSummaryDtos.StageItem stage = stage(key, label, "skipped", "不适用", "",
                Collections.emptyList());
        stage.setSkipped(true);
        stage.setSkipReason(skipReason);
        return stage;
    }

    /**
     * 待处理节点：未到达该环节
     *
     * @param key   节点编码
     * @param label 显示名称
     * @return 节点
     */
    private ProductSummaryDtos.StageItem pendingStage(String key, String label) {
        return stage(key, label, "pending", "", "", Collections.emptyList());
    }

    private ProductSummaryDtos.DocumentItem planDoc(BatchContext ctx) {
        ProductionPlan plan = ctx.getPlan();
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("生产计划");
        doc.setCode(plan.getPlanNumber());
        doc.setDate(planDate(ctx));
        doc.setStatus(plan.getCurrentStatus());
        doc.setFiles(toFiles(businessFileService.list("PRODUCTION_PLAN", plan.getId().longValue(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem purchaseDoc(PurchaseOrders po, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("采购订单");
        doc.setCode(po.getPurchaseNumber());
        doc.setDate(po.getOrderTime() == null ? null : po.getOrderTime().toLocalDate());
        doc.setStatus(po.getStatus() == null ? null : String.valueOf(po.getStatus()));
        List<PurchaseOrderItems> items = ctx.getPurchaseItems() == null ? Collections.emptyList()
                : ctx.getPurchaseItems().getOrDefault(po.getId(), Collections.emptyList());
        for (PurchaseOrderItems item : items) {
            ProductSummaryDtos.DocumentLine line = new ProductSummaryDtos.DocumentLine();
            line.setCode(item.getMaterialCode());
            line.setName(resolvePurchaseLineName(item, ctx));
            line.setQty(item.getPurchaseQuantity());
            line.setUnit(item.getUnit());
            line.setPrice(item.getUnitPrice());
            line.setAmount(item.getAmount());
            line.setSupplier(item.getSupplier());
            line.setStatus(item.getStatus());
            doc.getLines().add(line);
        }
        doc.setFiles(toFiles(businessFileService.list("PURCHASE_ORDER", po.getId(), null, null)));
        return doc;
    }

    /**
     * 解析采购明细展示名称
     * <p>
     * 优先级与采购/验收一致：物料主数据名称 &gt; 原药材品名 &gt; 制剂名称。
     * 采购明细的 product_name 可能存的是物料分类，不能作为首选。
     * </p>
     *
     * @param item 采购明细
     * @param ctx  批次上下文
     * @return 物料名称
     */
    private String resolvePurchaseLineName(PurchaseOrderItems item, BatchContext ctx) {
        if (item.getMaterialId() != null && ctx.getPurchaseMaterialNames() != null) {
            String materialName = ctx.getPurchaseMaterialNames().get(item.getMaterialId());
            if (StringUtils.hasText(materialName)) {
                return materialName;
            }
        }
        if (StringUtils.hasText(item.getRawMaterialName())) {
            return item.getRawMaterialName();
        }
        return item.getProductName();
    }

    private ProductSummaryDtos.DocumentItem acceptanceDoc(AcceptanceOrder acceptance) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("货物验收");
        doc.setCode(acceptance.getAcceptanceCode());
        doc.setDate(acceptance.getAcceptanceTime() == null ? null : acceptance.getAcceptanceTime().toLocalDate());
        doc.setStatus(acceptance.getStatus());
        doc.setFiles(toFiles(businessFileService.list("ACCEPTANCE_ORDER", acceptance.getAcceptanceId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem pickDoc(ProductionPick pick, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("领料单");
        doc.setCode(pick.getPickCode());
        doc.setDate(date(pick.getPickTime()));
        doc.setStatus(pick.getStatus());
        doc.setOperator(pick.getApplicant());
        List<ProductionPickDetail> details = ctx.getPickDetails() == null ? Collections.emptyList()
                : ctx.getPickDetails().getOrDefault(pick.getId(), Collections.emptyList());
        for (ProductionPickDetail d : details) {
            ProductSummaryDtos.DocumentLine line = new ProductSummaryDtos.DocumentLine();
            line.setCode(d.getMaterialCode());
            line.setName(d.getMaterialName());
            line.setCategory(d.getCategoryName());
            line.setQty(d.getActualQty() == null ? d.getApplyQty() : d.getActualQty());
            line.setUnit(d.getUnitName());
            line.setBatchNo(d.getBatchNumber());
            doc.getLines().add(line);
        }
        return doc;
    }

    private ProductSummaryDtos.DocumentItem workOrderDoc(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("工单");
        doc.setCode(wo.getWorkOrderCode());
        doc.setDate(date(wo.getConfigDate()));
        doc.setStatus(wo.getCurrentStatus());
        doc.setFiles(toFiles(businessFileService.list("WORK_ORDER", wo.getWorkOrderId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem processDoc(WorkOrderProcessExecution execution, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("工序记录");
        String name = ctx.getProcessNames() == null ? null : ctx.getProcessNames().get(execution.getProcessTypeId());
        doc.setCode(StringUtils.hasText(name) ? name : "工序" + execution.getStepOrder());
        doc.setTime(execution.getStartTime());
        doc.setDate(date(execution.getStartTime()));
        doc.setStatus(execution.getStatus());
        doc.setOperator(execution.getOperatorName());
        doc.setFiles(toFiles(businessFileService.list("WORK_ORDER_PROCESS_EXECUTION", execution.getId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem stockInDoc(StockIn stockIn, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("成品入库单");
        doc.setCode(stockIn.getInCode());
        doc.setDate(date(stockIn.getInDate()));
        doc.setStatus(stockIn.getInStatus());
        doc.setAmount(stockIn.getTotalAmount());
        List<StockInDetail> details = ctx.getStockInDetails() == null ? Collections.emptyList()
                : ctx.getStockInDetails().getOrDefault(stockIn.getInId(), Collections.emptyList());
        for (StockInDetail d : details) {
            ProductSummaryDtos.DocumentLine line = new ProductSummaryDtos.DocumentLine();
            line.setCode(d.getItemCode());
            line.setName(d.getItemName());
            line.setCategory(d.getCategoryName());
            line.setQty(d.getQuantity());
            line.setUnit(d.getUnitName());
            line.setBatchNo(d.getBatchNumber());
            line.setAmount(d.getAmount());
            doc.getLines().add(line);
        }
        doc.setFiles(toFiles(businessFileService.list("STOCK_IN_PRODUCT", stockIn.getInId(), null, null)));
        return doc;
    }

    /**
     * 物料入库单：验收入库节点展示，附件随验收单归档
     *
     * @param stockIn 物料入库单
     * @param ctx     批次上下文
     * @return 单据项
     */
    private ProductSummaryDtos.DocumentItem materialStockInDoc(StockIn stockIn, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("物料入库单");
        doc.setCode(stockIn.getInCode());
        doc.setDate(date(stockIn.getInDate()));
        doc.setStatus(stockIn.getInStatus());
        doc.setAmount(stockIn.getTotalAmount());
        List<StockInDetail> details = ctx.getMaterialStockInDetails() == null ? Collections.emptyList()
                : ctx.getMaterialStockInDetails().getOrDefault(stockIn.getInId(), Collections.emptyList());
        for (StockInDetail d : details) {
            ProductSummaryDtos.DocumentLine line = new ProductSummaryDtos.DocumentLine();
            line.setCode(d.getItemCode());
            line.setName(d.getItemName());
            line.setCategory(d.getCategoryName());
            line.setQty(d.getQuantity());
            line.setUnit(d.getUnitName());
            line.setBatchNo(d.getBatchNumber());
            line.setAmount(d.getAmount());
            doc.getLines().add(line);
        }
        return doc;
    }

    private ProductSummaryDtos.DocumentItem stockOutDoc(StockOut stockOut, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("成品出库单");
        doc.setCode(stockOut.getOutCode());
        doc.setDate(date(stockOut.getOutDate()));
        doc.setStatus(stockOut.getOutStatus());
        doc.setAmount(stockOut.getTotalAmount());
        List<StockOutDetail> details = ctx.getStockOutDetails() == null ? Collections.emptyList()
                : ctx.getStockOutDetails().getOrDefault(stockOut.getOutId(), Collections.emptyList());
        for (StockOutDetail d : details) {
            ProductSummaryDtos.DocumentLine line = new ProductSummaryDtos.DocumentLine();
            line.setCode(d.getItemCode());
            line.setName(d.getItemName());
            line.setCategory(d.getCategoryName());
            line.setQty(d.getQuantity());
            line.setUnit(d.getUnitName());
            line.setBatchNo(d.getBatchNumber());
            line.setAmount(d.getAmount());
            doc.getLines().add(line);
        }
        doc.setFiles(toFiles(businessFileService.list("STOCK_OUT_SALES", stockOut.getOutId(), null, null)));
        return doc;
    }

    /**
     * 生产领料出库单：与领料单同属“生产领料”节点
     *
     * @param stockOut 领料出库单
     * @param ctx      批次上下文
     * @return 单据项
     */
    private ProductSummaryDtos.DocumentItem pickStockOutDoc(StockOut stockOut, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("领料出库单");
        doc.setCode(stockOut.getOutCode());
        doc.setDate(date(stockOut.getOutDate()));
        doc.setStatus(stockOut.getOutStatus());
        doc.setAmount(stockOut.getTotalAmount());
        List<StockOutDetail> details = ctx.getMaterialStockOutDetails() == null ? Collections.emptyList()
                : ctx.getMaterialStockOutDetails().getOrDefault(stockOut.getOutId(), Collections.emptyList());
        for (StockOutDetail d : details) {
            ProductSummaryDtos.DocumentLine line = new ProductSummaryDtos.DocumentLine();
            line.setCode(d.getItemCode());
            line.setName(d.getItemName());
            line.setCategory(d.getCategoryName());
            line.setQty(d.getQuantity());
            line.setUnit(d.getUnitName());
            line.setBatchNo(d.getBatchNumber());
            line.setAmount(d.getAmount());
            doc.getLines().add(line);
        }
        doc.setFiles(toFiles(businessFileService.list("STOCK_OUT_PRODUCTION", stockOut.getOutId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem inspectionRequestDoc(InspectionRequest request) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("请检单");
        doc.setCode(request.getInspectionCode());
        doc.setTime(request.getRequestTime());
        doc.setDate(date(request.getRequestTime()));
        doc.setOperator(request.getRequester());
        doc.setStatus(request.getProcessName());
        doc.setFiles(toFiles(businessFileService.list("INSPECTION_REQUEST", request.getId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem samplingDoc(SamplingRecord sampling) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("取样记录");
        doc.setCode(sampling.getSamplingCode());
        doc.setTime(sampling.getSamplingTime());
        doc.setDate(date(sampling.getSamplingTime()));
        doc.setOperator(sampling.getSampler());
        doc.setStatus(sampling.getSamplingMethod());
        doc.setFiles(toFiles(businessFileService.list("SAMPLING_RECORD", sampling.getId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem inspectionRecordDoc(InspectionRecord record) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("检验记录");
        doc.setItemCategory(record.getItemCategory());
        doc.setCode(record.getInspectionCode());
        doc.setTime(record.getEndTime());
        doc.setDate(date(record.getEndTime()));
        doc.setOperator(record.getInspector());
        doc.setStatus(record.getConclusion());
        doc.setFiles(toFiles(businessFileService.list("INSPECTION_RECORD", record.getId(), null, null)));
        return doc;
    }

    private ProductSummaryDtos.DocumentItem releaseDoc(ReleaseReview release) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("放行记录");
        doc.setCode(release.getReleaseCode());
        doc.setTime(release.getReviewTime());
        doc.setDate(date(release.getReviewTime()));
        doc.setOperator(release.getReviewer());
        doc.setStatus(release.getReleaseConclusion());
        doc.setFiles(toFiles(businessFileService.list("RELEASE_REVIEW", release.getId(), null, null)));
        return doc;
    }

    // endregion

    // region 槽位与文件
    // ===================================
    // 槽位与文件
    // ===================================

    private List<ProductSummaryDtos.SlotState> slotStates(BatchContext ctx) {
        List<ProductSummaryDtos.SlotState> result = new ArrayList<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            List<Long> ids = slotDocumentIds(config, ctx);
            if (ids.isEmpty()) continue;
            ProductSummaryDtos.SlotState slot = new ProductSummaryDtos.SlotState();
            slot.setStage(config.getStage());
            slot.setNodeKey(nodeKeyOfStage(config.getStage()));
            slot.setLabel(StringUtils.hasText(config.getDisplayName())
                    ? config.getDisplayName() : config.getBizType() + " / " + config.getFileKind());
            slot.setBizType(config.getBizType());
            slot.setFileKind(config.getFileKind());
            slot.setRequirement(config.getRequirement());
            slot.setExpected(ids.size());
            slot.setUploaded(uploadedDocCount(config, ctx, ids));
            slot.setMissing(Math.max(0, slot.getExpected() - slot.getUploaded()));
            result.add(slot);
        }
        return result;
    }

    private List<ProductSummaryDtos.FileGroup> fileGroups(BatchContext ctx) {
        List<ProductSummaryDtos.FileGroup> result = new ArrayList<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            List<Long> ids = slotDocumentIds(config, ctx);
            if (ids.isEmpty()) continue;
            List<BusinessFileDto> files = new ArrayList<>();
            for (Long id : ids) {
                files.addAll(files(config.getBizType(), id, config.getFileKind()));
            }
            if (files.isEmpty()) continue;
            ProductSummaryDtos.FileGroup group = new ProductSummaryDtos.FileGroup();
            group.setStage(config.getStage());
            group.setNodeKey(nodeKeyOfStage(config.getStage()));
            group.setLabel(StringUtils.hasText(config.getDisplayName())
                    ? config.getDisplayName() : config.getBizType() + " / " + config.getFileKind());
            group.setBizType(config.getBizType());
            group.setFileKind(config.getFileKind());
            group.setRequirement(config.getRequirement());
            group.setSlotCode(config.getBizType() + " / " + config.getFileKind());
            group.setFiles(toFiles(files));
            result.add(group);
        }
        return result;
    }

    private List<ProductSummaryDtos.MissingItem> missingItems(BatchContext ctx) {
        List<ProductSummaryDtos.MissingItem> result = new ArrayList<>();
        for (FileRequirementConfig config : fileRequirementService.listAll()) {
            if ("OPTIONAL".equals(config.getRequirement())) continue;
            List<Long> ids = slotDocumentIds(config, ctx);
            for (Long id : ids) {
                if (files(config.getBizType(), id, config.getFileKind()).isEmpty()) {
                    ProductSummaryDtos.MissingItem item = new ProductSummaryDtos.MissingItem();
                    item.setStage(config.getStage());
                    item.setNodeKey(nodeKeyOfStage(config.getStage()));
                    item.setLabel(StringUtils.hasText(config.getDisplayName())
                            ? config.getDisplayName() : config.getBizType());
                    item.setDoc(docCode(config.getBizType(), id, ctx));
                    item.setSlotCode(config.getBizType() + " / " + config.getFileKind());
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<Long> slotDocumentIds(FileRequirementConfig config, BatchContext ctx) {
        String type = config.getBizType();
        return switch (type) {
            case "PREPARATION", "PREPARATION_PROCESS_TEMPLATE" -> ctx.getPreparation() == null
                    ? Collections.emptyList() : List.of(ctx.getPreparation().getPreparationId());
            case "PRODUCTION_PLAN" -> ctx.getPlan() == null ? Collections.emptyList()
                    : List.of(ctx.getPlan().getId().longValue());
            case "PURCHASE_ORDER" -> ctx.getPurchases().stream().map(PurchaseOrders::getId).collect(Collectors.toList());
            case "ACCEPTANCE_ORDER" -> ctx.getAcceptances().stream().map(AcceptanceOrder::getAcceptanceId).collect(Collectors.toList());
            case "WORK_ORDER" -> List.of(ctx.getWorkOrder().getWorkOrderId());
            case "INSPECTION_REQUEST" -> ctx.getRequests().stream().map(InspectionRequest::getId).collect(Collectors.toList());
            case "SAMPLING_RECORD" -> ctx.getSamplings().stream().map(SamplingRecord::getId).collect(Collectors.toList());
            case "INSPECTION_RECORD" -> ctx.getInspections().stream().map(InspectionRecord::getId).collect(Collectors.toList());
            case "RELEASE_REVIEW" -> ctx.getRelease() == null ? Collections.emptyList() : List.of(ctx.getRelease().getId());
            case "DEVIATION_RECORD" -> ctx.getDeviations().stream().map(DeviationRecord::getId).collect(Collectors.toList());
            case "STOCK_OUT_PRODUCTION" -> ctx.getPicks().stream().map(ProductionPick::getStockOutId)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            case "STOCK_IN_PRODUCT" -> ctx.getStockIns().stream().map(StockIn::getInId).collect(Collectors.toList());
            case "STOCK_OUT_SALES" -> ctx.getStockOuts().stream().map(StockOut::getOutId).collect(Collectors.toList());
            default -> Collections.emptyList();
        };
    }

    private int uploadedDocCount(FileRequirementConfig config, BatchContext ctx, List<Long> ids) {
        return (int) ids.stream().filter(id -> !files(config.getBizType(), id, config.getFileKind()).isEmpty()).count();
    }

    private List<BusinessFileDto> files(String bizType, Long bizId, String kind) {
        if (bizId == null) return Collections.emptyList();
        List<BusinessFileDto> list = businessFileService.list(bizType, bizId, null, kind);
        return list == null ? Collections.emptyList() : list;
    }

    private String docCode(String bizType, Long id, BatchContext ctx) {
        return switch (bizType) {
            case "PREPARATION", "PREPARATION_PROCESS_TEMPLATE" -> ctx.getPreparation() == null ? "" : ctx.getPreparation().getPreparationCode();
            case "PRODUCTION_PLAN" -> ctx.getPlan() == null ? "" : ctx.getPlan().getPlanNumber();
            case "PURCHASE_ORDER" -> ctx.getPurchases().stream().filter(p -> Objects.equals(p.getId(), id))
                    .map(PurchaseOrders::getPurchaseNumber).findFirst().orElse("");
            case "ACCEPTANCE_ORDER" -> ctx.getAcceptances().stream().filter(a -> Objects.equals(a.getAcceptanceId(), id))
                    .map(AcceptanceOrder::getAcceptanceCode).findFirst().orElse("");
            case "WORK_ORDER" -> ctx.getWorkOrder().getWorkOrderCode();
            case "INSPECTION_REQUEST" -> ctx.getRequests().stream().filter(r -> Objects.equals(r.getId(), id))
                    .map(InspectionRequest::getInspectionCode).findFirst().orElse("");
            case "SAMPLING_RECORD" -> ctx.getSamplings().stream().filter(s -> Objects.equals(s.getId(), id))
                    .map(SamplingRecord::getSamplingCode).findFirst().orElse("");
            case "INSPECTION_RECORD" -> ctx.getInspections().stream().filter(r -> Objects.equals(r.getId(), id))
                    .map(InspectionRecord::getInspectionCode).findFirst().orElse("");
            case "RELEASE_REVIEW" -> ctx.getRelease() == null ? "" : ctx.getRelease().getReleaseCode();
            case "DEVIATION_RECORD" -> ctx.getDeviations().stream().filter(d -> Objects.equals(d.getId(), id))
                    .map(DeviationRecord::getDeviationCode).findFirst().orElse("");
            default -> "";
        };
    }

    private String missingSummary(List<ProductSummaryDtos.MissingItem> missing) {
        if (missing.isEmpty()) return "";
        Map<String, Integer> counts = new HashMap<>();
        for (ProductSummaryDtos.MissingItem item : missing) {
            counts.merge(item.getStage() == null ? "其他" : item.getStage(), 1, Integer::sum);
        }
        return counts.entrySet().stream().limit(3)
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining("、"))
                + (counts.size() > 3 ? " 等 " + counts.size() + " 个环节" : "");
    }

    // endregion

    // region 导出
    // ===================================
    // 导出
    // ===================================

    @Override
    public byte[] export(ProductSummaryDtos.ExportRequest request) {
        ProductSummaryDtos.ExportRequest req = request == null ? new ProductSummaryDtos.ExportRequest() : request;
        boolean hasSelectedIds = (req.getWorkOrderIds() != null && !req.getWorkOrderIds().isEmpty())
                || (req.getPlanIds() != null && !req.getPlanIds().isEmpty());
        String scope = StringUtils.hasText(req.getScope()) ? req.getScope().toUpperCase()
                : (hasSelectedIds ? "SELECTED" : "FILTERED");
        String format = StringUtils.hasText(req.getFormat()) ? req.getFormat().toUpperCase() : "ZIP";
        boolean includeProduct = !Boolean.FALSE.equals(req.getIncludeProductFiles());
        boolean includeManifest = !Boolean.FALSE.equals(req.getIncludeManifest());
        Set<String> requirementFilter = req.getRequirements() == null || req.getRequirements().isEmpty()
                ? new LinkedHashSet<>(List.of("REQUIRED", "CONDITIONAL"))
                : new LinkedHashSet<>(req.getRequirements());

        List<BatchContext> contexts = new ArrayList<>();
        List<ProductionPlan> planOnlyPlans = new ArrayList<>();

        if ("SELECTED".equals(scope)) {
            if (req.getWorkOrderIds() != null && !req.getWorkOrderIds().isEmpty()) {
                contexts = batchQueryService.loadAll(workOrderMapper.selectBatchIds(req.getWorkOrderIds()));
            }
            Set<Long> selectedPlanIds = new HashSet<>();
            if (req.getPlanIds() != null) {
                selectedPlanIds.addAll(req.getPlanIds());
            }
            if (!selectedPlanIds.isEmpty()) {
                Set<Long> planIdsWithWorkOrders = contexts.stream()
                        .map(ctx -> ctx.getWorkOrder().getPlanId())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                for (ProductionPlan plan : productionPlanMapper.selectBatchIds(selectedPlanIds)) {
                    if (plan.getId() != null && !planIdsWithWorkOrders.contains(plan.getId().longValue())) {
                        planOnlyPlans.add(plan);
                    }
                }
            }
        } else if ("ALL".equals(scope)) {
            List<WorkOrder> allWorkOrders = workOrderMapper.selectList(null);
            contexts = batchQueryService.loadAll(allWorkOrders);
            Set<Long> planIdsWithWorkOrders = allWorkOrders.stream()
                    .map(WorkOrder::getPlanId).filter(Objects::nonNull).collect(Collectors.toSet());
            planOnlyPlans = productionPlanMapper.selectList(null).stream()
                    .filter(p -> p.getId() != null && !planIdsWithWorkOrders.contains(p.getId().longValue()))
                    .collect(Collectors.toList());
        } else {
            ProductSummaryDtos.ExportFilters filters = req.getFilters();
            String variety = filters == null ? req.getVarietyCode() : filters.getVarietyCode();
            String dateType = filters == null ? null : filters.getDateType();
            String start = filters == null ? null : filters.getStartDate();
            String end = filters == null ? null : filters.getEndDate();
            String keyword = filters == null ? null : filters.getKeyword();
            boolean onlyMissing = filters != null && Boolean.TRUE.equals(filters.getOnlyMissing());
            LoadedData data = loadData(variety, dateType, start, end, keyword, onlyMissing);
            contexts = data.contexts;
            planOnlyPlans = data.planOnlyPlans;
        }

        List<String[]> manifest = new ArrayList<>();
        manifest.add(new String[]{"品种编码", "品种名称", "规格", "计划编号", "工单号", "批号",
                "计划日期", "工单日期", "入库日期", "出库日期", "检验日期",
                "环节", "槽位", "文件名", "大小(字节)", "上传人", "上传时间", "状态", "说明"});

        if ("CSV".equals(format)) {
            return manifestCsv(manifest);
        }

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            Set<String> addedEntries = new HashSet<>();
            Set<String> addedProductEntries = new HashSet<>();
            int[] fileCounter = {0};
            long[] sizeCounter = {0L};

            for (BatchContext ctx : contexts) {
                ProductSummaryDtos.BatchSummary summary = toSummary(ctx);
                String varietyFolder = safe(summary.getPreparationCode()) + "_" + safe(summary.getPreparationName());
                String batchFolder = StringUtils.hasText(summary.getBatchNo())
                        ? summary.getBatchNo() : safe(summary.getWorkOrderCode());
                for (ProductSummaryDtos.FileGroup group : fileGroups(ctx)) {
                    if (!requirementFilter.contains(group.getRequirement())) continue;
                    boolean productGroup = isProductGroup(group);
                    if (productGroup && !includeProduct) continue;
                    String folder = productGroup
                            ? varietyFolder + "/品种资料/" + sanitize(group.getStage()) + "_" + sanitize(group.getLabel())
                            : varietyFolder + "/" + sanitize(batchFolder) + "/" + sanitize(group.getStage()) + "_" + sanitize(group.getLabel());
                    for (ProductSummaryDtos.FileItem file : group.getFiles()) {
                        writeFile(zip, summary, group, file, folder, addedEntries, addedProductEntries,
                                productGroup, manifest, fileCounter, sizeCounter);
                    }
                }
                for (ProductSummaryDtos.MissingItem missing : missingItems(ctx)) {
                    if (!requirementFilter.contains("REQUIRED") && !requirementFilter.contains("CONDITIONAL")) continue;
                    addManifest(manifest, summary, missing.getStage(), missing.getLabel(), "", "",
                            "缺失", missing.getDoc());
                }
            }

            for (ProductionPlan plan : planOnlyPlans) {
                ProductSummaryDtos.BatchSummary summary = toPlanSummary(plan, batchQueryService.loadByPlan(plan));
                String varietyFolder = safe(summary.getPreparationCode()) + "_" + safe(summary.getPreparationName());
                String planFolder = safe(summary.getPlanNumber()) + "_未下达工单";
                for (ProductSummaryDtos.FileGroup group : planOnlyFileGroups(plan)) {
                    if (!requirementFilter.contains(group.getRequirement())) continue;
                    boolean productGroup = isProductGroup(group);
                    if (productGroup && !includeProduct) continue;
                    String folder = productGroup
                            ? varietyFolder + "/品种资料/" + sanitize(group.getStage()) + "_" + sanitize(group.getLabel())
                            : planFolder + "/" + sanitize(group.getStage()) + "_" + sanitize(group.getLabel());
                    for (ProductSummaryDtos.FileItem file : group.getFiles()) {
                        writeFile(zip, summary, group, file, folder, addedEntries, addedProductEntries,
                                productGroup, manifest, fileCounter, sizeCounter);
                    }
                }
                for (ProductSummaryDtos.MissingItem missing : planOnlyMissing(plan)) {
                    addManifest(manifest, summary, missing.getStage(), missing.getLabel(), "", "",
                            "缺失", missing.getDoc());
                }
            }

            if (includeManifest) {
                zip.putNextEntry(new ZipEntry("文件清单.csv"));
                zip.write(manifestCsvBytes(manifest));
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出文件失败: " + e.getMessage(), e);
        }
    }

    private boolean isProductGroup(ProductSummaryDtos.FileGroup group) {
        return "PREPARATION".equals(group.getBizType()) || "PREPARATION_PROCESS_TEMPLATE".equals(group.getBizType());
    }

    private void writeFile(ZipOutputStream zip, ProductSummaryDtos.BatchSummary summary,
                           ProductSummaryDtos.FileGroup group, ProductSummaryDtos.FileItem file,
                           String folder, Set<String> addedEntries, Set<String> addedProductEntries,
                           boolean productGroup, List<String[]> manifest, int[] fileCounter, long[] sizeCounter)
            throws IOException {
        FileInfo info = file.getFileId() == null ? null : fileInfoService.getById(file.getFileId());
        if (info == null || !StringUtils.hasText(info.getFilePath())) {
            addManifest(manifest, summary, group.getStage(), group.getLabel(), file.getName(),
                    String.valueOf(file.getSize()), "文件不存在", "");
            return;
        }
        Path path = Paths.get(info.getFilePath());
        if (!Files.exists(path)) {
            addManifest(manifest, summary, group.getStage(), group.getLabel(), file.getName(),
                    String.valueOf(file.getSize()), "文件缺失", "业务关系存在，磁盘文件不存在");
            return;
        }
        String entryName = folder + "/" + sanitize(file.getName());
        if (productGroup && !addedProductEntries.add(entryName)) {
            return;
        }
        if (!productGroup) {
            entryName = uniqueName(entryName, addedEntries);
        } else if (!addedEntries.add(entryName)) {
            return;
        }
        if (fileCounter[0] >= 300) {
            throw new IllegalArgumentException("导出文件超过 300 个，请缩小筛选范围");
        }
        sizeCounter[0] += Files.size(path);
        if (sizeCounter[0] > 500L * 1024 * 1024) {
            throw new IllegalArgumentException("导出文件超过 500MB，请缩小筛选范围");
        }
        fileCounter[0]++;
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(path, zip);
        zip.closeEntry();
        addManifest(manifest, summary, group.getStage(), group.getLabel(), file.getName(),
                String.valueOf(file.getSize()), "已导出", "");
    }

    private void addManifest(List<String[]> manifest, ProductSummaryDtos.BatchSummary summary,
                             String stage, String slot, String fileName, String size,
                             String status, String note) {
        manifest.add(new String[]{
                safe(summary.getPreparationCode()), safe(summary.getPreparationName()), safe(summary.getSpec()),
                safe(summary.getPlanNumber()), safe(summary.getWorkOrderCode()), safe(summary.getBatchNo()),
                dateText(summary.getPlanDate()), dateText(summary.getWorkOrderDate()),
                dateText(summary.getInboundDate()), dateText(summary.getOutboundDate()),
                dateText(summary.getInspectionDate()),
                safe(stage), safe(slot), safe(fileName), safe(size), "", "", safe(status), safe(note)
        });
    }

    private String dateText(java.time.LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private byte[] manifestCsv(List<String[]> lines) {
        StringBuilder csv = new StringBuilder("﻿");
        for (String[] row : lines) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) csv.append(',');
                csv.append(csvCell(row[i]));
            }
            csv.append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] manifestCsvBytes(List<String[]> lines) {
        return manifestCsv(lines);
    }

    private String csvCell(String value) {
        return "\"" + safe(value).replace("\"", "\"\"") + "\"";
    }

    private String uniqueName(String name, Set<String> added) {
        if (added.add(name)) return name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 2;
        while (!added.add(base + "_" + i + ext)) {
            i++;
        }
        return base + "_" + i + ext;
    }

    private String sanitize(String name) {
        return safe(name).replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String csv(String value) {
        return "\"" + safe(value).replace("\"", "\"\"") + "\"";
    }

    // endregion

    // region 工具
    // ===================================
    // 工具
    // ===================================

    private LocalDate rowDate(BatchContext ctx, String dateType) {
        return switch (dateType == null ? "" : dateType) {
            case "workOrderDate" -> date(ctx.getWorkOrder().getConfigDate());
            case "inboundDate" -> ctx.getStockIns().stream().filter(this::isEffectiveStockIn).findFirst()
                    .map(stockIn -> date(stockIn.getInDate())).orElse(null);
            case "inspectionDate" -> latestFinishedInspection(ctx) == null ? null : date(latestFinishedInspection(ctx).getEndTime());
            default -> planDate(ctx);
        };
    }

    private LocalDate planDate(BatchContext ctx) {
        if (ctx.getPlan() == null) return null;
        LocalDateTime time = ctx.getPlan().getPlanProductionTime() != null
                ? ctx.getPlan().getPlanProductionTime() : ctx.getPlan().getCreatedTime();
        return date(time);
    }

    private String planCode(BatchContext ctx) {
        return ctx.getPlan() == null ? "未关联计划" : ctx.getPlan().getPlanNumber();
    }

    private String planMeta(BatchContext ctx) {
        if (ctx.getPlan() == null) return "";
        return safe(ctx.getPlan().getCurrentStatus()) + (planDate(ctx) == null ? "" : " · " + planDate(ctx));
    }

    private String materialSource(BatchContext ctx) {
        if (!ctx.getPicks().isEmpty()) {
            return "REQUISITION";
        }
        if (!ctx.getPurchases().isEmpty() || !materialAcceptances(ctx).isEmpty()) {
            return "PURCHASE";
        }
        return hasEffectiveStockIn(ctx.getMaterialStockIns()) ? "DIRECT" : null;
    }

    private String materialMeta(BatchContext ctx) {
        if (!ctx.getPicks().isEmpty()) {
            ProductionPick first = ctx.getPicks().get(0);
            return safe(first.getStatus()) + " · " + safe(first.getApplicant());
        }
        if (!ctx.getPurchases().isEmpty()) {
            return safe(String.valueOf(ctx.getPurchases().get(0).getStatus()));
        }
        if (hasEffectiveStockIn(ctx.getMaterialStockIns())) {
            return safe(ctx.getMaterialStockIns().get(0).getInStatus());
        }
        return "";
    }

    /**
     * 产品汇总只认成品检验，中间产品/原料检验不参与成品结论
     */
    private List<InspectionRecord> finishedInspections(BatchContext ctx) {
        if (ctx.getInspections() == null) return Collections.emptyList();
        return ctx.getInspections().stream()
                .filter(item -> "成品".equals(item.getItemCategory()))
                .collect(Collectors.toList());
    }

    private InspectionRecord latestFinishedInspection(BatchContext ctx) {
        List<InspectionRecord> list = finishedInspections(ctx);
        return list.isEmpty() ? null : list.get(0);
    }

    private String inspectionConclusion(BatchContext ctx) {
        InspectionRecord inspection = latestFinishedInspection(ctx);
        return inspection == null ? null : inspection.getConclusion();
    }

    private BigDecimal yieldRate(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        if (wo.getYieldRate() != null) return wo.getYieldRate();
        if (wo.getBatchQty() != null && wo.getBatchQty().compareTo(BigDecimal.ZERO) > 0 && wo.getFinishedQty() != null) {
            return wo.getFinishedQty().multiply(new BigDecimal("100"))
                    .divide(wo.getBatchQty(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private String unitName(BatchContext ctx) {
        if (ctx.getPlan() != null && StringUtils.hasText(ctx.getPlan().getUnitName())) return ctx.getPlan().getUnitName();
        if (ctx.getPreparation() != null && StringUtils.hasText(ctx.getPreparation().getUnitName())) return ctx.getPreparation().getUnitName();
        return "";
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<ProductSummaryDtos.FileItem> toFiles(List<BusinessFileDto> files) {
        if (files == null) return Collections.emptyList();
        return files.stream().map(f -> {
            ProductSummaryDtos.FileItem item = new ProductSummaryDtos.FileItem();
            item.setFileId(f.getFileId());
            item.setName(f.getOriginalName());
            item.setSize(f.getFileSize());
            item.setTime(f.getCreatedTime());
            item.setUploader("");
            return item;
        }).collect(Collectors.toList());
    }

    // endregion
}

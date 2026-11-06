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
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.BatchQueryService;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileRequirementService;
import com.tonghui.erp.Service.FileInfoService;
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
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        List<ProductSummaryDtos.BatchSummary> all = new ArrayList<>();
        for (BatchContext ctx : batchQueryService.loadAll(workOrders)) {
            ProductSummaryDtos.BatchSummary dto = toSummary(ctx);
            LocalDate day = rowDate(ctx, dateType);
            if (start != null && (day == null || day.isBefore(start))) continue;
            if (end != null && (day == null || day.isAfter(end))) continue;
            if (onlyMissing && dto.getMissingCount() == 0) continue;
            all.add(dto);
        }
        all.sort(Comparator.comparing(ProductSummaryDtos.BatchSummary::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

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

    private ProductSummaryDtos.BatchSummary toSummary(BatchContext ctx) {
        WorkOrder wo = ctx.getWorkOrder();
        ProductSummaryDtos.BatchSummary dto = new ProductSummaryDtos.BatchSummary();
        dto.setWorkOrderId(wo.getWorkOrderId());
        dto.setWorkOrderCode(wo.getWorkOrderCode());
        dto.setPlanId(ctx.getPlan() == null ? null : ctx.getPlan().getId().longValue());
        dto.setPlanNumber(ctx.getPlan() == null ? wo.getPlanName() : ctx.getPlan().getPlanNumber());
        dto.setPreparationCode(wo.getPreparationCode());
        dto.setPreparationName(wo.getPreparationName());
        dto.setSpec(ctx.getPreparation() == null ? null : ctx.getPreparation().getSpec());
        dto.setUnitName(unitName(ctx));
        dto.setBatchNo(wo.getBatchNumber());
        dto.setPlanQty(wo.getBatchQty());
        dto.setFinishedQty(wo.getFinishedQty());
        dto.setMaterialSource(materialSource(ctx));
        dto.setPlanDate(planDate(ctx));
        dto.setProductionStart(date(wo.getConfigDate()));
        dto.setProductionEnd(actualEnd(ctx));
        dto.setYieldRate(yieldRate(ctx));
        dto.setInspectionConclusion(inspectionConclusion(ctx));
        dto.setStatus(wo.getCurrentStatus());

        List<ProductSummaryDtos.FileGroup> groups = fileGroups(ctx);
        dto.setFileCount(groups.stream().mapToInt(g -> g.getFiles().size()).sum());
        List<ProductSummaryDtos.MissingItem> missing = missingItems(ctx);
        dto.setMissingCount(missing.size());
        dto.setMissingSummary(missingSummary(missing));
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

    private List<ProductSummaryDtos.StageItem> stages(BatchContext ctx) {
        List<ProductSummaryDtos.StageItem> stages = new ArrayList<>();
        WorkOrder wo = ctx.getWorkOrder();

        stages.add(stage("plan", "计划", "done", planCode(ctx), planMeta(ctx),
                ctx.getPlan() == null ? Collections.emptyList() : List.of(planDoc(ctx))));

        List<ProductSummaryDtos.DocumentItem> materialDocs = new ArrayList<>();
        if (!ctx.getPicks().isEmpty()) {
            ctx.getPicks().forEach(p -> materialDocs.add(pickDoc(p, ctx)));
        } else {
            ctx.getPurchases().forEach(p -> materialDocs.add(purchaseDoc(p, ctx)));
            ctx.getAcceptances().forEach(a -> materialDocs.add(acceptanceDoc(a)));
        }
        stages.add(stage("material", ctx.getPicks().isEmpty() ? "采购 / 验收" : "领料",
                materialDocs.isEmpty() ? "pending" : "done",
                ctx.getPicks().isEmpty() ? ctx.getPurchases().size() + " 张采购单" : ctx.getPicks().size() + " 张领料单",
                materialMeta(ctx), materialDocs));

        stages.add(stage("workOrder", "工单", wo.getFinishedQty() != null ? "done" : "active",
                wo.getWorkOrderCode(), "批号 " + safe(wo.getBatchNumber()) + " · " + safe(wo.getCurrentStatus()),
                List.of(workOrderDoc(ctx))));

        stages.add(stage("production", "生产", ctx.getExecutions().isEmpty() ? "pending" : "done",
                ctx.getExecutions().size() + " 道工序",
                yieldRate(ctx) == null ? "" : "收率 " + yieldRate(ctx) + "%",
                ctx.getExecutions().stream().map(e -> processDoc(e, ctx)).collect(Collectors.toList())));

        stages.add(stage("inbound", "入库", ctx.getStockIns().isEmpty() ? "pending" : "done",
                ctx.getStockIns().size() + " 张入库单", "",
                ctx.getStockIns().stream().map(s -> stockInDoc(s, ctx)).collect(Collectors.toList())));

        stages.add(stage("outbound", "出库", ctx.getStockOuts().isEmpty() ? "pending" : "done",
                ctx.getStockOuts().size() + " 张出库单", "",
                ctx.getStockOuts().stream().map(s -> stockOutDoc(s, ctx)).collect(Collectors.toList())));

        List<ProductSummaryDtos.DocumentItem> inspectionDocs = new ArrayList<>();
        ctx.getRequests().forEach(r -> inspectionDocs.add(inspectionRequestDoc(r)));
        ctx.getSamplings().forEach(s -> inspectionDocs.add(samplingDoc(s)));
        ctx.getInspections().forEach(i -> inspectionDocs.add(inspectionRecordDoc(i)));
        stages.add(stage("inspection", "检验", inspectionDocs.isEmpty() ? "pending"
                        : ("合格".equals(inspectionConclusion(ctx)) ? "done"
                        : ("不合格".equals(inspectionConclusion(ctx)) ? "error" : "active")),
                inspectionConclusion(ctx), ctx.getInspections().size() + " 条检验记录", inspectionDocs));

        stages.add(stage("release", "放行", ctx.getRelease() == null ? "pending"
                        : ("拒绝放行".equals(ctx.getRelease().getReleaseConclusion()) ? "error" : "done"),
                ctx.getRelease() == null ? "未放行" : ctx.getRelease().getReleaseCode(),
                ctx.getRelease() == null ? "" : safe(ctx.getRelease().getReleaseConclusion()),
                ctx.getRelease() == null ? Collections.emptyList() : List.of(releaseDoc(ctx.getRelease()))));
        return stages;
    }

    private ProductSummaryDtos.StageItem stage(String key, String label, String state, String main,
                                               String meta, List<ProductSummaryDtos.DocumentItem> documents) {
        ProductSummaryDtos.StageItem stage = new ProductSummaryDtos.StageItem();
        stage.setKey(key);
        stage.setLabel(label);
        stage.setState(state);
        stage.setMain(main);
        stage.setMeta(meta);
        stage.setDocuments(documents);
        return stage;
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
            line.setName(StringUtils.hasText(item.getProductName()) ? item.getProductName() : item.getRawMaterialName());
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
        if (pick.getStockOutId() != null) {
            doc.setFiles(toFiles(businessFileService.list("STOCK_OUT_PRODUCTION", pick.getStockOutId(), null, null)));
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
        doc.setType("入库单");
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

    private ProductSummaryDtos.DocumentItem stockOutDoc(StockOut stockOut, BatchContext ctx) {
        ProductSummaryDtos.DocumentItem doc = new ProductSummaryDtos.DocumentItem();
        doc.setType("出库单");
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
            group.setLabel(StringUtils.hasText(config.getDisplayName())
                    ? config.getDisplayName() : config.getBizType() + " / " + config.getFileKind());
            group.setBizType(config.getBizType());
            group.setFileKind(config.getFileKind());
            group.setRequirement(config.getRequirement());
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
        if (request == null) {
            request = new ProductSummaryDtos.ExportRequest();
        }
        List<WorkOrder> workOrders;
        if (request.getWorkOrderIds() != null && !request.getWorkOrderIds().isEmpty()) {
            workOrders = workOrderMapper.selectBatchIds(request.getWorkOrderIds());
        } else {
            QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
            if (StringUtils.hasText(request.getVarietyCode())) {
                wrapper.eq("preparation_code", request.getVarietyCode());
            }
            workOrders = workOrderMapper.selectList(wrapper);
        }
        List<BatchContext> contexts = batchQueryService.loadAll(workOrders);
        Set<String> requirementFilter = request.getRequirements() == null ? Collections.emptySet()
                : new HashSet<>(request.getRequirements());
        boolean includeProduct = !Boolean.FALSE.equals(request.getIncludeProductFiles());

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            Set<String> added = new HashSet<>();
            StringBuilder csv = new StringBuilder("﻿文件夹,环节,文件槽位,级别,文件名,大小,状态\n");
            for (BatchContext ctx : contexts) {
                String variety = safe(ctx.getWorkOrder().getPreparationName());
                String batch = StringUtils.hasText(ctx.getWorkOrder().getBatchNumber())
                        ? ctx.getWorkOrder().getBatchNumber() : ctx.getWorkOrder().getWorkOrderCode();
                for (ProductSummaryDtos.FileGroup group : fileGroups(ctx)) {
                    if (!requirementFilter.isEmpty() && !requirementFilter.contains(group.getRequirement())) {
                        continue;
                    }
                    boolean productGroup = "PREPARATION".equals(group.getBizType())
                            || "PREPARATION_PROCESS_TEMPLATE".equals(group.getBizType());
                    String folder;
                    if (productGroup && !includeProduct) {
                        folder = "00_产品资料/" + safe(ctx.getWorkOrder().getPreparationCode());
                    } else {
                        folder = variety + "/" + batch + "/" + safe(group.getStage()) + "_" + safe(group.getLabel());
                    }
                    for (ProductSummaryDtos.FileItem file : group.getFiles()) {
                        FileInfo info = fileInfoService.getById(file.getFileId());
                        if (info == null || !StringUtils.hasText(info.getFilePath())) {
                            csv.append(csv(folder)).append(',').append(csv(group.getStage())).append(',')
                                    .append(csv(group.getLabel())).append(',').append(csv(group.getRequirement())).append(',')
                                    .append(csv(file.getName())).append(',')
                                    .append(csv(String.valueOf(file.getSize()))).append(',').append(csv("文件不存在")).append('\n');
                            continue;
                        }
                        Path path = Paths.get(info.getFilePath());
                        if (!Files.exists(path)) {
                            csv.append(csv(folder)).append(',').append(csv(group.getStage())).append(',')
                                    .append(csv(group.getLabel())).append(',').append(csv(group.getRequirement())).append(',')
                                    .append(csv(file.getName())).append(',')
                                    .append(csv(String.valueOf(file.getSize()))).append(',').append(csv("文件缺失")).append('\n');
                            continue;
                        }
                        String entryName = folder + "/" + sanitize(file.getName());
                        entryName = uniqueName(entryName, added);
                        zip.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zip);
                        zip.closeEntry();
                        csv.append(csv(folder)).append(',').append(csv(group.getStage())).append(',')
                                .append(csv(group.getLabel())).append(',').append(csv(group.getRequirement())).append(',')
                                .append(csv(file.getName())).append(',')
                                .append(csv(String.valueOf(file.getSize()))).append(',').append(csv("已导出")).append('\n');
                    }
                }
            }
            zip.putNextEntry(new ZipEntry("文件清单.csv"));
            zip.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出文件失败: " + e.getMessage(), e);
        }
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
            case "inboundDate" -> ctx.getStockIns().isEmpty() ? null : date(ctx.getStockIns().get(0).getInDate());
            case "inspectionDate" -> ctx.getInspections().isEmpty() ? null : date(ctx.getInspections().get(0).getEndTime());
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
        return !ctx.getPicks().isEmpty() ? "REQUISITION" : (!ctx.getPurchases().isEmpty() ? "PURCHASE" : null);
    }

    private String materialMeta(BatchContext ctx) {
        if (!ctx.getPicks().isEmpty()) {
            ProductionPick first = ctx.getPicks().get(0);
            return safe(first.getStatus()) + " · " + safe(first.getApplicant());
        }
        if (ctx.getPurchases().isEmpty()) return "";
        return safe(String.valueOf(ctx.getPurchases().get(0).getStatus()));
    }

    private String inspectionConclusion(BatchContext ctx) {
        return ctx.getInspections().isEmpty() ? "未检验" : ctx.getInspections().get(0).getConclusion();
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

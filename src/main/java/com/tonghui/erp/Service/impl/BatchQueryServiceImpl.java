package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.QualityPages.BatchContext;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.SalesOrder;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessCheck;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ProductionPickDetailMapper;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessCheckMapper;
import com.tonghui.erp.Service.AcceptanceOrderService;
import com.tonghui.erp.Service.BatchQueryService;
import com.tonghui.erp.Service.DeviationRecordService;
import com.tonghui.erp.Service.InspectionRecordService;
import com.tonghui.erp.Service.InspectionRequestService;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.ProcessTypeService;
import com.tonghui.erp.Service.ProductionPickService;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Service.PurchaseOrderItemsService;
import com.tonghui.erp.Service.PurchaseOrdersService;
import com.tonghui.erp.Service.ReleaseReviewService;
import com.tonghui.erp.Service.ReturnOrderService;
import com.tonghui.erp.Service.RoomInfoService;
import com.tonghui.erp.Service.SalesOrderService;
import com.tonghui.erp.Service.SamplingRecordService;
import com.tonghui.erp.Service.StockInService;
import com.tonghui.erp.Service.StockOutService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
 * 批次聚合查询服务实现
 */
@Service
public class BatchQueryServiceImpl implements BatchQueryService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Autowired
    private ProductionPlanService productionPlanService;
    @Autowired
    private PreparationService preparationService;
    @Autowired
    private PurchaseOrdersService purchaseOrdersService;
    @Autowired
    private PurchaseOrderItemsService purchaseOrderItemsService;
    @Autowired
    private AcceptanceOrderService acceptanceOrderService;
    @Autowired
    private ProductionPickService productionPickService;
    @Autowired
    private ProductionPickDetailMapper productionPickDetailMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private ReturnOrderService returnOrderService;
    @Autowired
    private StockInService stockInService;
    @Autowired
    private StockInDetailMapper stockInDetailMapper;
    @Autowired
    private StockOutService stockOutService;
    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;
    @Autowired
    private InspectionRequestService inspectionRequestService;
    @Autowired
    private SamplingRecordService samplingRecordService;
    @Autowired
    private InspectionRecordService inspectionRecordService;
    @Autowired
    private ReleaseReviewService releaseReviewService;
    @Autowired
    private SalesOrderService salesOrderService;
    @Autowired
    private DeviationRecordService deviationRecordService;
    @Autowired
    private WorkOrderProcessExecutionService workOrderProcessExecutionService;
    @Autowired
    private WorkOrderProcessCheckMapper workOrderProcessCheckMapper;
    @Autowired
    private PreparationProcessTemplateService preparationProcessTemplateService;
    @Autowired
    private ProcessTypeService processTypeService;
    @Autowired
    private RoomInfoService roomInfoService;

    // endregion

    @Override
    public List<BatchContext> loadAll(List<WorkOrder> workOrders) {
        if (workOrders == null || workOrders.isEmpty()) {
            return Collections.emptyList();
        }
        return workOrders.stream().map(this::load).collect(Collectors.toList());
    }

    @Override
    public BatchContext loadByPlan(ProductionPlan plan) {
        BatchContext ctx = new BatchContext();
        ctx.setPlan(plan);
        if (plan == null) {
            return ctx;
        }
        Preparation preparation = plan.getPreparationId() == null ? null : preparationService.getById(plan.getPreparationId());
        ctx.setPreparation(preparation);

        Long planId = plan.getId() == null ? null : plan.getId().longValue();
        String planNumber = plan.getPlanNumber();

        // 未下达工单的计划仍需展示采购、验收入库节点
        loadPurchases(ctx, planId, planNumber);
        loadAcceptances(ctx, null, planNumber);
        loadMaterialStockIns(ctx, planNumber);
        return ctx;
    }

    @Override
    public BatchContext load(WorkOrder workOrder) {
        BatchContext ctx = new BatchContext();
        ctx.setWorkOrder(workOrder);
        if (workOrder == null) {
            return ctx;
        }

        // 生产计划 / 制剂
        ProductionPlan plan = workOrder.getPlanId() == null ? null : productionPlanService.getById(workOrder.getPlanId());
        ctx.setPlan(plan);
        Preparation preparation = workOrder.getPreparationId() == null ? null
                : preparationService.getById(workOrder.getPreparationId());
        if (preparation == null && StringUtils.hasText(workOrder.getPreparationCode())) {
            preparation = preparationService.getOne(new QueryWrapper<Preparation>()
                    .eq("preparation_code", workOrder.getPreparationCode())
                    .last("LIMIT 1"), false);
        }
        ctx.setPreparation(preparation);

        String planNumber = plan == null ? workOrder.getPlanNumber() : plan.getPlanNumber();
        Long planId = plan == null ? workOrder.getPlanId() == null ? null : workOrder.getPlanId().longValue()
                : plan.getId().longValue();

        // 采购与明细
        loadPurchases(ctx, planId, planNumber);

        // 验收
        loadAcceptances(ctx, workOrder.getWorkOrderCode(), planNumber);

        // 物料入库（验收合格自动生成或按计划直接入库，不含成品入库）
        loadMaterialStockIns(ctx, planNumber);

        // 领料
        List<ProductionPick> picks = productionPickService.list(new QueryWrapper<ProductionPick>()
                .eq("work_order_id", workOrder.getWorkOrderId()));
        ctx.setPicks(picks);
        if (!picks.isEmpty()) {
            List<Long> ids = picks.stream().map(ProductionPick::getId).collect(Collectors.toList());
            ctx.setPickDetails(productionPickDetailMapper.selectList(new QueryWrapper<ProductionPickDetail>()
                            .in("pick_id", ids))
                    .stream().collect(Collectors.groupingBy(ProductionPickDetail::getPickId)));
        }

        // 退库
        if (StringUtils.hasText(planNumber)) {
            ctx.setReturns(returnOrderService.list(new QueryWrapper<ReturnOrder>()
                    .eq("production_plan_no", planNumber)));
        }

        // 成品入库
        loadFinishedStockIns(ctx, workOrder.getWorkOrderId());

        // 生产领料出库
        loadMaterialStockOuts(ctx, planId, planNumber);

        // 成品出库（销售台账/批号关联）
        loadFinishedStockOuts(ctx, workOrder);

        // 检验
        ctx.setRequests(inspectionRequestService.list(new QueryWrapper<InspectionRequest>()
                .eq("work_order_id", workOrder.getWorkOrderId())));
        if (StringUtils.hasText(workOrder.getBatchNumber())) {
            ctx.setSamplings(samplingRecordService.list(new QueryWrapper<SamplingRecord>()
                    .eq("batch_no", workOrder.getBatchNumber())));
        }
        ctx.setInspections(inspectionRecordService.list(new QueryWrapper<InspectionRecord>()
                .eq("work_order_id", workOrder.getWorkOrderId())
                .orderByDesc("end_time", "id")));

        // 放行
        ctx.setRelease(releaseReviewService.getOne(new QueryWrapper<ReleaseReview>()
                .eq("work_order_id", workOrder.getWorkOrderId())
                .orderByDesc("review_time", "id")
                .last("LIMIT 1"), false));

        // 偏差
        ctx.setDeviations(deviationRecordService.list(new QueryWrapper<DeviationRecord>()
                .eq("work_order_id", workOrder.getWorkOrderId())));

        // 工序执行
        List<WorkOrderProcessExecution> executions = workOrderProcessExecutionService.list(
                new QueryWrapper<WorkOrderProcessExecution>()
                        .eq("work_order_id", workOrder.getWorkOrderId())
                        .orderByAsc("step_order", "id"));
        ctx.setExecutions(executions);
        ctx.setProcessChecks(workOrderProcessCheckMapper.selectList(new QueryWrapper<WorkOrderProcessCheck>()
                .eq("work_order_id", workOrder.getWorkOrderId())));

        // 工序要求：品种工序模板优先，process_type 描述兜底
        Map<Long, String> requirements = new HashMap<>();
        List<ProcessType> processTypes = processTypeService.list();
        Map<Integer, String> processDesc = processTypes.stream()
                .collect(Collectors.toMap(ProcessType::getProcessId,
                        p -> p.getProcessDescription() == null ? "" : p.getProcessDescription(), (a, b) -> a));
        Map<Long, String> processNames = processTypes.stream()
                .collect(Collectors.toMap(p -> p.getProcessId().longValue(),
                        p -> p.getProcessName() == null ? "" : p.getProcessName(), (a, b) -> a));
        ctx.setProcessNames(processNames);
        if (preparation != null) {
            List<PreparationProcessTemplate> templates = preparationProcessTemplateService.list(
                    new QueryWrapper<PreparationProcessTemplate>()
                            .eq("preparation_id", preparation.getPreparationId())
                            .orderByAsc("step_order"));
            for (PreparationProcessTemplate template : templates) {
                String requirement = StringUtils.hasText(template.getKeyProcessParams())
                        ? template.getKeyProcessParams() : processDesc.get(template.getProcessTypeId() == null ? null : template.getProcessTypeId().intValue());
                if (template.getProcessTypeId() != null) {
                    requirements.put(template.getProcessTypeId(), requirement);
                }
            }
        }
        ctx.setProcessRequirements(requirements);

        // 房间
        Set<Integer> roomIds = executions.stream().map(WorkOrderProcessExecution::getRoomId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!roomIds.isEmpty()) {
            ctx.setRooms(roomInfoService.listByIds(roomIds).stream()
                    .collect(Collectors.toMap(RoomInfo::getRoomId, r -> r, (a, b) -> a)));
        }
        return ctx;
    }

    // region 数据加载辅助
    // ===================================
    // 数据加载辅助
    // ===================================

    /**
     * 加载采购订单及明细
     *
     * @param ctx        批次上下文
     * @param planId     生产计划ID
     * @param planNumber 生产计划编号
     */
    private void loadPurchases(BatchContext ctx, Long planId, String planNumber) {
        if (planId == null && !StringUtils.hasText(planNumber)) {
            return;
        }
        QueryWrapper<PurchaseOrders> wrapper = new QueryWrapper<>();
        if (planId != null) {
            wrapper.eq("production_plan_id", planId);
        } else {
            wrapper.eq("production_plan_code", planNumber);
        }
        List<PurchaseOrders> purchases = purchaseOrdersService.list(wrapper);
        ctx.setPurchases(purchases);
        if (purchases.isEmpty()) {
            return;
        }
        List<Long> ids = purchases.stream().map(PurchaseOrders::getId).collect(Collectors.toList());
        List<PurchaseOrderItems> items = purchaseOrderItemsService.list(
                new QueryWrapper<PurchaseOrderItems>().in("order_id", ids));
        ctx.setPurchaseItems(items.stream().collect(Collectors.groupingBy(PurchaseOrderItems::getOrderId)));
        loadPurchaseMaterialNames(ctx, items);
    }

    /**
     * 加载采购明细的物料主数据名称
     * <p>
     * 采购明细的 product_name 可能存的是物料分类，真实名称以物料主数据为准，
     * 缺失时再回退 raw_material_name / product_name。
     * </p>
     *
     * @param ctx   批次上下文
     * @param items 采购明细
     */
    private void loadPurchaseMaterialNames(BatchContext ctx, List<PurchaseOrderItems> items) {
        List<Long> materialIds = items.stream()
                .map(PurchaseOrderItems::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (materialIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = materialMapper.selectList(new QueryWrapper<Material>()
                        .in("material_id", materialIds))
                .stream()
                .filter(material -> StringUtils.hasText(material.getMaterialName()))
                .collect(Collectors.toMap(Material::getMaterialId, Material::getMaterialName, (a, b) -> a));
        ctx.setPurchaseMaterialNames(names);
    }

    /**
     * 加载验收单
     *
     * @param ctx           批次上下文
     * @param workOrderCode 工单编号（可空）
     * @param planNumber    生产计划编号（可空）
     */
    private void loadAcceptances(BatchContext ctx, String workOrderCode, String planNumber) {
        List<AcceptanceOrder> acceptances = new ArrayList<>();
        if (StringUtils.hasText(workOrderCode) || StringUtils.hasText(planNumber)) {
            QueryWrapper<AcceptanceOrder> wrapper = new QueryWrapper<>();
            wrapper.and(w -> {
                if (StringUtils.hasText(workOrderCode)) {
                    w.eq("work_order_code", workOrderCode);
                }
                if (StringUtils.hasText(planNumber)) {
                    if (StringUtils.hasText(workOrderCode)) {
                        w.or();
                    }
                    w.eq("plan_code", planNumber);
                }
            });
            acceptances = acceptanceOrderService.list(wrapper);
        }
        if (acceptances.isEmpty() && !ctx.getPurchases().isEmpty()) {
            List<String> purchaseNumbers = ctx.getPurchases().stream()
                    .map(PurchaseOrders::getPurchaseNumber)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
            if (!purchaseNumbers.isEmpty()) {
                acceptances = acceptanceOrderService.list(new QueryWrapper<AcceptanceOrder>()
                        .in("purchase_number", purchaseNumbers));
            }
        }
        ctx.setAcceptances(acceptances);
    }

    /**
     * 加载物料入库单及明细
     * <p>
     * 优先按验收单号关联（验收合格自动生成的入库单），其次按计划编号回退；
     * 显式排除成品入库，避免与成品入库节点混用。
     * </p>
     *
     * @param ctx        批次上下文
     * @param planNumber 生产计划编号
     */
    private void loadMaterialStockIns(BatchContext ctx, String planNumber) {
        Map<Long, StockIn> result = new LinkedHashMap<>();
        List<String> acceptanceCodes = ctx.getAcceptances().stream()
                .map(AcceptanceOrder::getAcceptanceCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (!acceptanceCodes.isEmpty()) {
            stockInService.list(new QueryWrapper<StockIn>()
                            .in("related_order", acceptanceCodes)
                            .and(w -> w.ne("in_type", "成品入库").or().isNull("in_type")))
                    .forEach(stockIn -> result.put(stockIn.getInId(), stockIn));
        }
        if (StringUtils.hasText(planNumber)) {
            stockInService.list(new QueryWrapper<StockIn>()
                            .eq("plan_number", planNumber)
                            .and(w -> w.in("in_type", "采购入库", "直接入库", "领料入库").or().isNull("in_type")))
                    .forEach(stockIn -> result.put(stockIn.getInId(), stockIn));
        }
        List<StockIn> stockIns = new ArrayList<>(result.values());
        stockIns.sort(Comparator.comparing(StockIn::getInDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        ctx.setMaterialStockIns(stockIns);
        if (!stockIns.isEmpty()) {
            List<Long> ids = stockIns.stream().map(StockIn::getInId).collect(Collectors.toList());
            ctx.setMaterialStockInDetails(stockInDetailMapper.selectList(new QueryWrapper<StockInDetail>()
                            .in("in_id", ids))
                    .stream().collect(Collectors.groupingBy(StockInDetail::getInId)));
        }
    }

    /**
     * 加载成品入库单及明细
     *
     * @param ctx         批次上下文
     * @param workOrderId 工单ID
     */
    private void loadFinishedStockIns(BatchContext ctx, Long workOrderId) {
        List<StockIn> stockIns = stockInService.list(new QueryWrapper<StockIn>()
                .eq("work_order_id", workOrderId)
                .and(w -> w.eq("in_type", "成品入库").or().isNull("in_type"))
                .orderByAsc("in_date", "in_id"));
        ctx.setStockIns(stockIns);
        if (!stockIns.isEmpty()) {
            List<Long> ids = stockIns.stream().map(StockIn::getInId).collect(Collectors.toList());
            ctx.setStockInDetails(stockInDetailMapper.selectList(new QueryWrapper<StockInDetail>()
                            .in("in_id", ids))
                    .stream().collect(Collectors.groupingBy(StockInDetail::getInId)));
        }
    }

    /**
     * 加载生产领料出库单及明细
     *
     * @param ctx        批次上下文
     * @param planId     生产计划ID
     * @param planNumber 生产计划编号
     */
    private void loadMaterialStockOuts(BatchContext ctx, Long planId, String planNumber) {
        Map<Long, StockOut> result = new LinkedHashMap<>();
        List<Long> pickOutIds = ctx.getPicks().stream()
                .map(ProductionPick::getStockOutId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!pickOutIds.isEmpty()) {
            stockOutService.listByIds(pickOutIds)
                    .forEach(stockOut -> result.put(stockOut.getOutId(), stockOut));
        }
        List<String> pickCodes = ctx.getPicks().stream()
                .map(ProductionPick::getPickCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (!pickCodes.isEmpty() && (planId != null || StringUtils.hasText(planNumber))) {
            QueryWrapper<StockOut> wrapper = new QueryWrapper<StockOut>()
                    .in("out_type", "生产领料出库", "production")
                    .in("related_order", pickCodes);
            if (planId != null) {
                wrapper.eq("plan_id", planId);
            } else {
                wrapper.eq("plan_number", planNumber);
            }
            stockOutService.list(wrapper)
                    .forEach(stockOut -> result.put(stockOut.getOutId(), stockOut));
        }
        List<StockOut> stockOuts = new ArrayList<>(result.values());
        stockOuts.sort(Comparator.comparing(StockOut::getOutDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        ctx.setMaterialStockOuts(stockOuts);
        if (!stockOuts.isEmpty()) {
            List<Long> ids = stockOuts.stream().map(StockOut::getOutId).collect(Collectors.toList());
            ctx.setMaterialStockOutDetails(stockOutDetailMapper.selectList(new QueryWrapper<StockOutDetail>()
                            .in("out_id", ids))
                    .stream().collect(Collectors.groupingBy(StockOutDetail::getOutId)));
        }
    }

    /**
     * 加载成品出库单及明细
     * <p>
     * 关联顺序：销售台账（制剂编码 + 批号）→ 销售单号；工单号手工关联；
     * 批号明细兜底。不按生产计划关联，避免同一计划多批次时出库单被重复归属。
     * </p>
     *
     * @param ctx       批次上下文
     * @param workOrder 工单
     */
    private void loadFinishedStockOuts(BatchContext ctx, WorkOrder workOrder) {
        Map<Long, StockOut> result = new LinkedHashMap<>();

        // 1. 销售台账关联：同一制剂的同一批号可能对应多张销售台账
        List<SalesOrder> salesOrders = new ArrayList<>();
        if (StringUtils.hasText(workOrder.getPreparationCode())
                && StringUtils.hasText(workOrder.getBatchNumber())) {
            salesOrders = salesOrderService.list(new QueryWrapper<SalesOrder>()
                    .eq("preparation_code", workOrder.getPreparationCode())
                    .eq("batch_number", workOrder.getBatchNumber()));
        }
        ctx.setSalesOrders(salesOrders);
        List<String> salesOrderCodes = salesOrders.stream()
                .map(SalesOrder::getSalesOrderCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (!salesOrderCodes.isEmpty()) {
            stockOutService.list(finishedOutWrapper().in("related_order", salesOrderCodes))
                    .forEach(stockOut -> result.put(stockOut.getOutId(), stockOut));
        }

        // 2. 手工出库单直接关联工单号
        if (StringUtils.hasText(workOrder.getWorkOrderCode())) {
            stockOutService.list(finishedOutWrapper().eq("related_order", workOrder.getWorkOrderCode()))
                    .forEach(stockOut -> result.put(stockOut.getOutId(), stockOut));
        }

        // 3. 批号明细兜底（含制剂编码限定，避免与物料批号混淆）
        if (StringUtils.hasText(workOrder.getBatchNumber())) {
            QueryWrapper<StockOutDetail> detailWrapper = new QueryWrapper<StockOutDetail>()
                    .eq("batch_number", workOrder.getBatchNumber());
            if (StringUtils.hasText(workOrder.getPreparationCode())) {
                detailWrapper.eq("item_code", workOrder.getPreparationCode());
            }
            List<Long> outIds = stockOutDetailMapper.selectList(detailWrapper).stream()
                    .map(StockOutDetail::getOutId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!outIds.isEmpty()) {
                stockOutService.list(finishedOutWrapper().in("out_id", outIds))
                        .forEach(stockOut -> result.put(stockOut.getOutId(), stockOut));
            }
        }

        List<StockOut> stockOuts = new ArrayList<>(result.values());
        stockOuts.sort(Comparator.comparing(StockOut::getOutDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        ctx.setStockOuts(stockOuts);
        if (!stockOuts.isEmpty()) {
            List<Long> ids = stockOuts.stream().map(StockOut::getOutId).collect(Collectors.toList());
            ctx.setStockOutDetails(stockOutDetailMapper.selectList(new QueryWrapper<StockOutDetail>()
                            .in("out_id", ids))
                    .stream().collect(Collectors.groupingBy(StockOutDetail::getOutId)));
        }
    }

    /**
     * 成品出库查询条件：兼容历史“成品出库”与现行“销售出库”，并容纳未写类型的旧数据
     *
     * @return 出库单查询条件
     */
    private QueryWrapper<StockOut> finishedOutWrapper() {
        QueryWrapper<StockOut> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.in("out_type", "销售出库", "成品出库").or().isNull("out_type"));
        return wrapper;
    }

    // endregion
}

package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.QualityPages.BatchContext;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
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
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessCheck;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
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
import com.tonghui.erp.Service.SamplingRecordService;
import com.tonghui.erp.Service.StockInService;
import com.tonghui.erp.Service.StockOutService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        if (planId != null || StringUtils.hasText(planNumber)) {
            QueryWrapper<PurchaseOrders> purchaseWrapper = new QueryWrapper<>();
            if (planId != null) {
                purchaseWrapper.eq("production_plan_id", planId);
            } else {
                purchaseWrapper.eq("production_plan_code", planNumber);
            }
            List<PurchaseOrders> purchases = purchaseOrdersService.list(purchaseWrapper);
            ctx.setPurchases(purchases);
            if (!purchases.isEmpty()) {
                List<Long> ids = purchases.stream().map(PurchaseOrders::getId).collect(Collectors.toList());
                Map<Long, List<PurchaseOrderItems>> items = purchaseOrderItemsService.list(
                                new QueryWrapper<PurchaseOrderItems>().in("order_id", ids))
                        .stream().collect(Collectors.groupingBy(PurchaseOrderItems::getOrderId));
                ctx.setPurchaseItems(items);
            }
        }

        // 验收
        List<AcceptanceOrder> acceptances = new ArrayList<>();
        if (StringUtils.hasText(workOrder.getWorkOrderCode()) || StringUtils.hasText(planNumber)) {
            QueryWrapper<AcceptanceOrder> acceptanceWrapper = new QueryWrapper<>();
            acceptanceWrapper.and(w -> {
                if (StringUtils.hasText(workOrder.getWorkOrderCode())) {
                    w.eq("work_order_code", workOrder.getWorkOrderCode());
                }
                if (StringUtils.hasText(planNumber)) {
                    w.or().eq("plan_code", planNumber);
                }
            });
            acceptances = acceptanceOrderService.list(acceptanceWrapper);
        }
        if (acceptances.isEmpty() && !ctx.getPurchases().isEmpty()) {
            List<String> purchaseNumbers = ctx.getPurchases().stream()
                    .map(PurchaseOrders::getPurchaseNumber).filter(Objects::nonNull).collect(Collectors.toList());
            if (!purchaseNumbers.isEmpty()) {
                acceptances = acceptanceOrderService.list(new QueryWrapper<AcceptanceOrder>()
                        .in("purchase_number", purchaseNumbers));
            }
        }
        ctx.setAcceptances(acceptances);

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

        // 入库
        List<StockIn> stockIns = stockInService.list(new QueryWrapper<StockIn>()
                .eq("work_order_id", workOrder.getWorkOrderId()));
        ctx.setStockIns(stockIns);
        if (!stockIns.isEmpty()) {
            List<Long> ids = stockIns.stream().map(StockIn::getInId).collect(Collectors.toList());
            ctx.setStockInDetails(stockInDetailMapper.selectList(new QueryWrapper<StockInDetail>()
                            .in("in_id", ids))
                    .stream().collect(Collectors.groupingBy(StockInDetail::getInId)));
        }

        // 出库
        QueryWrapper<StockOut> stockOutWrapper = new QueryWrapper<>();
        if (planId != null) {
            stockOutWrapper.eq("plan_id", planId);
        } else if (StringUtils.hasText(planNumber)) {
            stockOutWrapper.eq("plan_number", planNumber);
        } else {
            stockOutWrapper.eq("out_id", -1);
        }
        List<StockOut> stockOuts = stockOutService.list(stockOutWrapper);
        ctx.setStockOuts(stockOuts);
        if (!stockOuts.isEmpty()) {
            List<Long> ids = stockOuts.stream().map(StockOut::getOutId).collect(Collectors.toList());
            ctx.setStockOutDetails(stockOutDetailMapper.selectList(new QueryWrapper<StockOutDetail>()
                            .in("out_id", ids))
                    .stream().collect(Collectors.groupingBy(StockOutDetail::getOutId)));
        }

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
}

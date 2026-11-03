package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Constants.PurchaseProcessNode;
import com.tonghui.erp.Common.Dto.ProductionPlanOverdueDto;
import com.tonghui.erp.Common.Dto.ProductionPlanPurchaseProgressDto;
import com.tonghui.erp.Common.Dto.ProductionPlanPurchaseSummaryDto;
import com.tonghui.erp.Common.Dto.ProductionPlanRelatedDocumentsDto;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Common.Dto.PurchaseChainDto;
import com.tonghui.erp.Common.Dto.PurchaseNodeDto;
import com.tonghui.erp.Common.Dto.RelatedDocumentDto;
import com.tonghui.erp.Common.utils.AcceptanceStatusPolicy;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.PlanStatusLog;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.InspectionRecordMapper;
import com.tonghui.erp.Data.mapper.InspectionRequestMapper;
import com.tonghui.erp.Data.mapper.PlanStatusLogMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.ProductionPlanProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产计划采购进度服务实现类
 * <p>
 * 基于采购计划、采购订单、验收单、入库单上的业务时间字段，实时组装生产计划关联采购的
 * 六节点流程进度。一个生产计划可关联多条采购计划/采购订单，列表摘要按“最慢链路”计算，
 * 详情接口返回每条链路的节点时间、节点间耗时以及采购/计划/生产/检验/入库关联单据。
 * </p>
 */
@Service
public class ProductionPlanProgressServiceImpl implements ProductionPlanProgressService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 生产计划数据访问层 */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    /** 采购计划数据访问层 */
    @Autowired
    private PurchasePlanMapper purchasePlanMapper;

    /** 采购订单数据访问层 */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    /** 验收单数据访问层 */
    @Autowired
    private AcceptanceOrderMapper acceptanceOrderMapper;

    /** 验收明细数据访问层 */
    @Autowired
    private AcceptanceDetailMapper acceptanceDetailMapper;

    /** 入库单数据访问层 */
    @Autowired
    private StockInMapper stockInMapper;

    /** 工单数据访问层 */
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /** 请检记录数据访问层 */
    @Autowired
    private InspectionRequestMapper inspectionRequestMapper;

    /** 检验记录数据访问层 */
    @Autowired
    private InspectionRecordMapper inspectionRecordMapper;

    /** 计划状态流水数据访问层 */
    @Autowired
    private PlanStatusLogMapper planStatusLogMapper;

    // endregion

    // region 列表摘要
    // ===================================
    // 列表摘要
    // ===================================

    /**
     * 批量为生产计划列表填充采购进度摘要和逾期信息
     *
     * @param plans 生产计划列表
     */
    @Override
    public void fillPurchaseSummaries(List<ProductionPlanWithRecordsDto> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }

        // 生产计划编号 -> 生产计划ID，用于历史数据缺少 production_plan_id 时按编号兜底
        Map<String, Long> planNumberToId = new HashMap<>();
        for (ProductionPlanWithRecordsDto plan : plans) {
            if (plan.getId() != null && StringUtils.hasText(plan.getPlanNumber())) {
                planNumberToId.put(plan.getPlanNumber(), plan.getId().longValue());
            }
        }

        Map<Long, List<PurchasePlan>> purchasePlansByPlanId = loadPurchasePlans(plans, planNumberToId);
        Map<Long, List<PurchaseOrders>> ordersByPlanId = loadPurchaseOrders(plans, planNumberToId, purchasePlansByPlanId);

        for (ProductionPlanWithRecordsDto plan : plans) {
            Long planId = plan.getId() == null ? null : plan.getId().longValue();
            List<PurchasePlan> purchasePlans = planId == null
                    ? List.of() : purchasePlansByPlanId.getOrDefault(planId, List.of());
            List<PurchaseOrders> orders = planId == null
                    ? List.of() : ordersByPlanId.getOrDefault(planId, List.of());
            plan.setPurchaseProgress(buildSummary(purchasePlans, orders));
            plan.setOverdue(calculateOverdue(plan));
        }
    }

    /**
     * 批量加载当前页生产计划关联的采购计划
     *
     * @param plans          生产计划列表
     * @param planNumberToId 生产计划编号到ID的映射
     * @return 生产计划ID -> 采购计划列表
     */
    private Map<Long, List<PurchasePlan>> loadPurchasePlans(List<ProductionPlanWithRecordsDto> plans,
                                                            Map<String, Long> planNumberToId) {
        List<Long> planIds = plans.stream()
                .map(plan -> plan.getId() == null ? null : plan.getId().longValue())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> planNumbers = plans.stream()
                .map(ProductionPlan::getPlanNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        List<PurchasePlan> all = new ArrayList<>();
        if (!planIds.isEmpty()) {
            all.addAll(purchasePlanMapper.selectList(new QueryWrapper<PurchasePlan>()
                    .in("production_plan_id", planIds)));
        }
        if (!planNumbers.isEmpty()) {
            // 历史数据可能只写了生产计划编号，未写 production_plan_id
            all.addAll(purchasePlanMapper.selectList(new QueryWrapper<PurchasePlan>()
                    .in("production_plan_code", planNumbers)
                    .isNull("production_plan_id")));
        }

        Map<Long, PurchasePlan> byId = new LinkedHashMap<>();
        for (PurchasePlan purchasePlan : all) {
            if (purchasePlan.getId() != null) {
                byId.putIfAbsent(purchasePlan.getId(), purchasePlan);
            }
        }

        Map<Long, List<PurchasePlan>> result = new HashMap<>();
        for (PurchasePlan purchasePlan : byId.values()) {
            Long planId = purchasePlan.getProductionPlanId();
            if (planId == null && StringUtils.hasText(purchasePlan.getProductionPlanCode())) {
                planId = planNumberToId.get(purchasePlan.getProductionPlanCode());
            }
            if (planId != null) {
                result.computeIfAbsent(planId, key -> new ArrayList<>()).add(purchasePlan);
            }
        }
        return result;
    }

    /**
     * 批量加载当前页生产计划关联的采购订单
     *
     * @param plans              生产计划列表
     * @param planNumberToId     生产计划编号到ID的映射
     * @param purchasePlansByPlanId 已加载的采购计划
     * @return 生产计划ID -> 采购订单列表
     */
    private Map<Long, List<PurchaseOrders>> loadPurchaseOrders(List<ProductionPlanWithRecordsDto> plans,
                                                               Map<String, Long> planNumberToId,
                                                               Map<Long, List<PurchasePlan>> purchasePlansByPlanId) {
        List<Long> planIds = plans.stream()
                .map(plan -> plan.getId() == null ? null : plan.getId().longValue())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> planNumbers = plans.stream()
                .map(ProductionPlan::getPlanNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, PurchasePlan> purchasePlanById = new HashMap<>();
        List<Long> purchasePlanIds = new ArrayList<>();
        for (List<PurchasePlan> list : purchasePlansByPlanId.values()) {
            for (PurchasePlan purchasePlan : list) {
                if (purchasePlan.getId() != null) {
                    purchasePlanById.putIfAbsent(purchasePlan.getId(), purchasePlan);
                    purchasePlanIds.add(purchasePlan.getId());
                }
            }
        }
        purchasePlanIds = purchasePlanIds.stream().distinct().collect(Collectors.toList());

        List<PurchaseOrders> all = new ArrayList<>();
        if (!planIds.isEmpty()) {
            all.addAll(purchaseOrdersMapper.selectList(new QueryWrapper<PurchaseOrders>()
                    .in("production_plan_id", planIds)));
        }
        if (!purchasePlanIds.isEmpty()) {
            all.addAll(purchaseOrdersMapper.selectList(new QueryWrapper<PurchaseOrders>()
                    .in("plan_id", purchasePlanIds)));
        }
        if (!planNumbers.isEmpty()) {
            all.addAll(purchaseOrdersMapper.selectList(new QueryWrapper<PurchaseOrders>()
                    .in("production_plan_code", planNumbers)));
        }

        Map<Long, PurchaseOrders> byId = new LinkedHashMap<>();
        for (PurchaseOrders order : all) {
            if (order.getId() != null) {
                byId.putIfAbsent(order.getId(), order);
            }
        }

        Map<Long, List<PurchaseOrders>> result = new HashMap<>();
        for (PurchaseOrders order : byId.values()) {
            Long planId = order.getProductionPlanId();
            if (planId == null && order.getPlanId() != null) {
                PurchasePlan purchasePlan = purchasePlanById.get(order.getPlanId());
                if (purchasePlan != null) {
                    planId = purchasePlan.getProductionPlanId();
                    if (planId == null && StringUtils.hasText(purchasePlan.getProductionPlanCode())) {
                        planId = planNumberToId.get(purchasePlan.getProductionPlanCode());
                    }
                }
            }
            if (planId == null && StringUtils.hasText(order.getProductionPlanCode())) {
                planId = planNumberToId.get(order.getProductionPlanCode());
            }
            if (planId != null) {
                result.computeIfAbsent(planId, key -> new ArrayList<>()).add(order);
            }
        }
        return result;
    }

    /**
     * 构建生产计划采购进度摘要
     *
     * @param plans  采购计划列表
     * @param orders 采购订单列表
     * @return 采购进度摘要
     */
    private ProductionPlanPurchaseSummaryDto buildSummary(List<PurchasePlan> plans, List<PurchaseOrders> orders) {
        ProductionPlanPurchaseSummaryDto summary = new ProductionPlanPurchaseSummaryDto();
        summary.setHasPurchasePlan(!plans.isEmpty() || !orders.isEmpty());
        summary.setPurchasePlanCount(plans.size());
        summary.setPurchaseOrderCount(orders.size());
        summary.setCompletedOrderCount((int) orders.stream().filter(this::isCompletedOrder).count());
        summary.setClosedOrderCount((int) orders.stream().filter(this::isClosedOrder).count());

        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (PurchaseProcessNode node : PurchaseProcessNode.values()) {
            distribution.put(node.getCode(), 0);
        }
        summary.setStageDistribution(distribution);

        if (plans.isEmpty() && orders.isEmpty()) {
            summary.setCurrentStageCode(null);
            summary.setCurrentStageName(null);
            summary.setCurrentStageState(null);
            summary.setProgressPercent(0);
            summary.setAbnormalCount(0);
            summary.setDataInconsistent(false);
            summary.setLatestNodeTime(null);
            summary.setSummaryText("未采购");
            return summary;
        }

        List<ChainStage> stages = new ArrayList<>();
        Set<Long> matchedOrderIds = new HashSet<>();
        for (PurchaseOrders order : orders) {
            PurchasePlan purchasePlan = findPurchasePlan(order, plans);
            ChainStage stage = resolveStage(purchasePlan, order);
            if (stage != null) {
                stages.add(stage);
            }
            matchedOrderIds.add(order.getId());
        }
        for (PurchasePlan purchasePlan : plans) {
            boolean hasOrder = orders.stream().anyMatch(order ->
                    Objects.equals(order.getPlanId(), purchasePlan.getId())
                            || Objects.equals(order.getId(), purchasePlan.getPurchaseOrderId())
                            || (order.getPlanId() == null
                            && StringUtils.hasText(order.getProductionPlanCode())
                            && order.getProductionPlanCode().equals(purchasePlan.getProductionPlanCode())));
            if (!hasOrder && !matchedOrderIds.contains(purchasePlan.getPurchaseOrderId())) {
                ChainStage stage = resolveStage(purchasePlan, null);
                if (stage != null) {
                    stages.add(stage);
                }
            }
        }

        int abnormalCount = (int) stages.stream().filter(stage -> "ABNORMAL".equals(stage.state)).count();
        summary.setAbnormalCount(abnormalCount);
        summary.setDataInconsistent(stages.stream().anyMatch(stage -> stage.dataInconsistent));
        for (ChainStage stage : stages) {
            distribution.merge(stage.node.getCode(), 1, Integer::sum);
        }

        List<ChainStage> activeStages = stages.stream()
                .filter(stage -> !"ABNORMAL".equals(stage.state))
                .collect(Collectors.toList());
        if (activeStages.isEmpty()) {
            summary.setCurrentStageCode(PurchaseProcessNode.ABNORMAL.getCode());
            summary.setCurrentStageName(PurchaseProcessNode.ABNORMAL.getName());
            summary.setCurrentStageState("ABNORMAL");
            summary.setProgressPercent(0);
        } else {
            // 瓶颈取“未完成链路”中阶段最靠前的一条；全部完成时统一展示已完成
            List<ChainStage> unfinished = activeStages.stream()
                    .filter(stage -> !"DONE".equals(stage.state))
                    .collect(Collectors.toList());
            if (unfinished.isEmpty()) {
                summary.setCurrentStageCode(PurchaseProcessNode.INBOUND.getCode());
                summary.setCurrentStageName("已完成");
                summary.setCurrentStageState("DONE");
                summary.setProgressPercent(100);
            } else {
                ChainStage bottleneck = unfinished.stream()
                        .min(Comparator.comparingInt(stage -> stage.node.getIndex()))
                        .orElse(unfinished.get(0));
                summary.setCurrentStageCode(bottleneck.node.getCode());
                summary.setCurrentStageName(bottleneck.node.getName());
                summary.setCurrentStageState("CURRENT");
                summary.setProgressPercent(activeStages.stream()
                        .mapToInt(this::stageProgressPercent)
                        .min()
                        .orElse(0));
            }
        }

        summary.setLatestNodeTime(resolveLatestNodeTime(plans, orders));
        summary.setSummaryText(buildSummaryText(summary, stages));
        return summary;
    }

    /**
     * 根据采购订单状态解析流程阶段
     *
     * @param purchasePlan 采购计划
     * @param order        采购订单
     * @return 链路阶段
     */
    private ChainStage resolveStage(PurchasePlan purchasePlan, PurchaseOrders order) {
        // 采购计划驳回优先级最高，即使存在异常订单也按驳回展示
        if (purchasePlan != null && "已驳回".equals(purchasePlan.getStatus())) {
            return ChainStage.abnormal(PurchaseProcessNode.APPROVE, 1, "采购计划已驳回");
        }

        if (order != null) {
            String status = stringValue(order.getStatus());
            if ("已作废".equals(status)) {
                return ChainStage.abnormal(PurchaseProcessNode.ORDER, 2, "采购订单已作废");
            }
            if ("待采购".equals(status)) {
                return ChainStage.current(PurchaseProcessNode.ORDER);
            }
            if ("运输中".equals(status)) {
                return ChainStage.current(PurchaseProcessNode.ARRIVE);
            }
            if ("验收中".equals(status)) {
                return ChainStage.current(PurchaseProcessNode.ACCEPT);
            }
            if ("已入库".equals(status) || "已归档".equals(status)) {
                return ChainStage.done(PurchaseProcessNode.INBOUND);
            }
            if ("已结束".equals(status)) {
                return ChainStage.done(PurchaseProcessNode.ACCEPT);
            }
            return ChainStage.current(PurchaseProcessNode.ORDER);
        }

        if (purchasePlan != null) {
            String status = purchasePlan.getStatus();
            if ("草稿".equals(status) || "待审批".equals(status)) {
                return ChainStage.current(PurchaseProcessNode.APPROVE);
            }
            if ("已审批".equals(status)) {
                // 已审批但未生成订单，标记数据不一致，方便前端提示
                return ChainStage.current(PurchaseProcessNode.ORDER, true);
            }
            if ("已驳回".equals(status)) {
                return ChainStage.abnormal(PurchaseProcessNode.APPROVE, 1, "采购计划已驳回");
            }
            return ChainStage.current(PurchaseProcessNode.APPROVE);
        }
        return null;
    }

    /**
     * 为采购订单匹配采购计划
     *
     * @param order  采购订单
     * @param plans  采购计划列表
     * @return 匹配的采购计划，无匹配返回 null
     */
    private PurchasePlan findPurchasePlan(PurchaseOrders order, List<PurchasePlan> plans) {
        if (order.getPlanId() != null) {
            for (PurchasePlan plan : plans) {
                if (Objects.equals(plan.getId(), order.getPlanId())) {
                    return plan;
                }
            }
        }
        for (PurchasePlan plan : plans) {
            if (Objects.equals(plan.getPurchaseOrderId(), order.getId())) {
                return plan;
            }
        }
        if (StringUtils.hasText(order.getPlanCode())) {
            for (PurchasePlan plan : plans) {
                if (order.getPlanCode().equals(plan.getPlanCode())) {
                    return plan;
                }
            }
        }
        return null;
    }

    /**
     * 判断订单是否已完成（已入库/已归档）
     *
     * @param order 采购订单
     * @return true-已完成
     */
    private boolean isCompletedOrder(PurchaseOrders order) {
        String status = stringValue(order.getStatus());
        return "已入库".equals(status) || "已归档".equals(status);
    }

    /**
     * 判断订单是否已结束（如全部退货）
     *
     * @param order 采购订单
     * @return true-已结束
     */
    private boolean isClosedOrder(PurchaseOrders order) {
        return "已结束".equals(stringValue(order.getStatus()));
    }

    /**
     * 计算链路进度百分比
     *
     * @param stage 链路阶段
     * @return 进度百分比
     */
    private int stageProgressPercent(ChainStage stage) {
        if ("ABNORMAL".equals(stage.state)) {
            return 0;
        }
        return PurchaseProcessNode.progressPercent(stage.completedCount);
    }

    /**
     * 构建采购进度展示文案
     *
     * @param summary        摘要
     * @param stages         链路阶段列表
     * @return 展示文案
     */
    private String buildSummaryText(ProductionPlanPurchaseSummaryDto summary, List<ChainStage> stages) {
        int total = stages.size();
        if (total == 0) {
            return "未采购";
        }
        int completed = summary.getCompletedOrderCount() + summary.getClosedOrderCount();
        List<String> parts = new ArrayList<>();
        if (summary.getCompletedOrderCount() > 0) {
            parts.add(summary.getCompletedOrderCount() + " 已入库");
        }
        if (summary.getClosedOrderCount() > 0) {
            parts.add(summary.getClosedOrderCount() + " 已结束");
        }
        long approving = stages.stream()
                .filter(stage -> stage.node == PurchaseProcessNode.APPROVE && "CURRENT".equals(stage.state))
                .count();
        if (approving > 0) {
            parts.add(approving + " 待审批");
        }
        long ordering = stages.stream()
                .filter(stage -> stage.node == PurchaseProcessNode.ORDER && "CURRENT".equals(stage.state))
                .count();
        if (ordering > 0) {
            parts.add(ordering + " 待下单");
        }
        long transporting = stages.stream()
                .filter(stage -> stage.node == PurchaseProcessNode.ARRIVE && "CURRENT".equals(stage.state))
                .count();
        if (transporting > 0) {
            parts.add(transporting + " 运输中");
        }
        long inspecting = stages.stream()
                .filter(stage -> stage.node == PurchaseProcessNode.ACCEPT && "CURRENT".equals(stage.state))
                .count();
        if (inspecting > 0) {
            parts.add(inspecting + " 验收中");
        }
        if (summary.getAbnormalCount() != null && summary.getAbnormalCount() > 0) {
            parts.add(summary.getAbnormalCount() + " 异常");
        }
        if (parts.isEmpty()) {
            return completed + "/" + total;
        }
        if (completed == 0) {
            return String.join("，", parts);
        }
        return completed + "/" + total + " " + String.join("，", parts);
    }

    /**
     * 解析最近一次采购节点时间
     *
     * @param plans  采购计划列表
     * @param orders 采购订单列表
     * @return 最近节点时间
     */
    private LocalDateTime resolveLatestNodeTime(List<PurchasePlan> plans, List<PurchaseOrders> orders) {
        LocalDateTime latest = null;
        for (PurchasePlan plan : plans) {
            latest = maxTime(latest, plan.getApprovalTime());
            latest = maxTime(latest, plan.getCreatedTime());
        }
        for (PurchaseOrders order : orders) {
            latest = maxTime(latest, order.getOrderTime());
            latest = maxTime(latest, order.getUpdatedTime());
        }
        return latest;
    }

    // endregion

    // region 详情时间线
    // ===================================
    // 详情时间线
    // ===================================

    /**
     * 查询生产计划采购流程详情
     *
     * @param planId 生产计划ID
     * @return 采购流程详情
     */
    @Override
    public ProductionPlanPurchaseProgressDto getPurchaseProgress(Integer planId) {
        if (planId == null) {
            throw new RuntimeException("生产计划ID不能为空");
        }
        ProductionPlan plan = productionPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }

        List<PurchasePlan> purchasePlans = loadPurchasePlansForDetail(plan);
        List<PurchaseOrders> orders = loadOrdersForDetail(plan, purchasePlans);
        List<AcceptanceOrder> acceptances = loadAcceptancesForDetail(plan, orders);
        List<StockIn> stockIns = loadStockInsForDetail(plan, acceptances);
        List<WorkOrder> workOrders = workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
                .eq("plan_id", planId));
        List<Long> workOrderIds = workOrders.stream()
                .map(WorkOrder::getWorkOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<InspectionRequest> inspectionRequests = workOrderIds.isEmpty()
                ? List.of()
                : inspectionRequestMapper.selectList(new QueryWrapper<InspectionRequest>()
                .in("work_order_id", workOrderIds));
        List<InspectionRecord> inspectionRecords = workOrderIds.isEmpty()
                ? List.of()
                : inspectionRecordMapper.selectList(new QueryWrapper<InspectionRecord>()
                .in("work_order_id", workOrderIds));
        List<PlanStatusLog> statusLogs = planStatusLogMapper.selectList(new QueryWrapper<PlanStatusLog>()
                .eq("plan_id", planId));

        Map<Long, List<AcceptanceDetail>> acceptanceDetailsMap = loadAcceptanceDetails(acceptances);
        List<PurchaseChainDto> chains = buildChains(plan, purchasePlans, orders, acceptances,
                stockIns, acceptanceDetailsMap);

        ProductionPlanPurchaseProgressDto detail = new ProductionPlanPurchaseProgressDto();
        detail.setProductionPlanId(plan.getId());
        detail.setPlanNumber(plan.getPlanNumber());
        detail.setPlanName(plan.getPlanName());
        detail.setCurrentStatus(plan.getCurrentStatus());
        detail.setPlanProductionTime(plan.getPlanProductionTime());
        detail.setDeliveryTime(plan.getDeliveryTime());
        detail.setOverdue(calculateOverdue(plan));
        detail.setPurchaseSummary(buildSummary(purchasePlans, orders));
        detail.setChains(chains);
        detail.setRelatedDocuments(buildRelatedDocuments(plan, purchasePlans, orders, acceptances,
                stockIns, workOrders, inspectionRequests, inspectionRecords, statusLogs));
        return detail;
    }

    /**
     * 加载详情所需的采购计划
     *
     * @param plan 生产计划
     * @return 采购计划列表
     */
    private List<PurchasePlan> loadPurchasePlansForDetail(ProductionPlan plan) {
        QueryWrapper<PurchasePlan> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.eq("production_plan_id", plan.getId().longValue());
            if (StringUtils.hasText(plan.getPlanNumber())) {
                w.or().eq("production_plan_code", plan.getPlanNumber());
            }
        });
        return purchasePlanMapper.selectList(wrapper);
    }

    /**
     * 加载详情所需的采购订单
     *
     * @param plan         生产计划
     * @param purchasePlans 采购计划列表
     * @return 采购订单列表
     */
    private List<PurchaseOrders> loadOrdersForDetail(ProductionPlan plan, List<PurchasePlan> purchasePlans) {
        List<Long> purchasePlanIds = purchasePlans.stream()
                .map(PurchasePlan::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        QueryWrapper<PurchaseOrders> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.eq("production_plan_id", plan.getId().longValue());
            if (!purchasePlanIds.isEmpty()) {
                w.or().in("plan_id", purchasePlanIds);
            }
            if (StringUtils.hasText(plan.getPlanNumber())) {
                w.or().eq("production_plan_code", plan.getPlanNumber());
            }
        });
        return purchaseOrdersMapper.selectList(wrapper);
    }

    /**
     * 加载详情所需的验收单
     *
     * @param plan  生产计划
     * @param orders 采购订单列表
     * @return 验收单列表
     */
    private List<AcceptanceOrder> loadAcceptancesForDetail(ProductionPlan plan, List<PurchaseOrders> orders) {
        List<String> orderNumbers = orders.stream()
                .map(PurchaseOrders::getPurchaseNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        List<AcceptanceOrder> all = new ArrayList<>();
        if (!orderNumbers.isEmpty()) {
            all.addAll(acceptanceOrderMapper.selectList(new QueryWrapper<AcceptanceOrder>()
                    .and(w -> w.in("purchase_number", orderNumbers)
                            .or().in("related_order", orderNumbers))));
        }
        if (StringUtils.hasText(plan.getPlanNumber())) {
            all.addAll(acceptanceOrderMapper.selectList(new QueryWrapper<AcceptanceOrder>()
                    .eq("plan_code", plan.getPlanNumber())
                    .eq("source_type", "采购入库")));
        }
        return dedupeAcceptances(all);
    }

    /**
     * 加载详情所需的入库单
     *
     * @param plan        生产计划
     * @param acceptances 验收单列表
     * @return 入库单列表
     */
    private List<StockIn> loadStockInsForDetail(ProductionPlan plan, List<AcceptanceOrder> acceptances) {
        List<String> acceptanceCodes = acceptances.stream()
                .map(AcceptanceOrder::getAcceptanceCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        List<StockIn> all = new ArrayList<>();
        if (!acceptanceCodes.isEmpty()) {
            all.addAll(stockInMapper.selectList(new QueryWrapper<StockIn>()
                    .in("related_order", acceptanceCodes)));
        }
        if (StringUtils.hasText(plan.getPlanNumber())) {
            all.addAll(stockInMapper.selectList(new QueryWrapper<StockIn>()
                    .eq("plan_number", plan.getPlanNumber())
                    .eq("in_type", "采购入库")));
        }
        return dedupeStockIns(all);
    }

    /**
     * 批量加载验收明细，用于计算验收节点进度
     *
     * @param acceptances 验收单列表
     * @return 验收单ID -> 验收明细列表
     */
    private Map<Long, List<AcceptanceDetail>> loadAcceptanceDetails(List<AcceptanceOrder> acceptances) {
        List<Long> acceptanceIds = acceptances.stream()
                .map(AcceptanceOrder::getAcceptanceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (acceptanceIds.isEmpty()) {
            return Map.of();
        }
        List<AcceptanceDetail> details = acceptanceDetailMapper.selectList(
                new QueryWrapper<AcceptanceDetail>().in("acceptance_id", acceptanceIds));
        return details.stream().collect(Collectors.groupingBy(AcceptanceDetail::getAcceptanceId));
    }

    /**
     * 组装采购链路
     *
     * @param plan                 生产计划
     * @param purchasePlans        采购计划列表
     * @param orders               采购订单列表
     * @param acceptances          验收单列表
     * @param stockIns             入库单列表
     * @param acceptanceDetailsMap 验收明细
     * @return 采购链路列表
     */
    private List<PurchaseChainDto> buildChains(ProductionPlan plan,
                                               List<PurchasePlan> purchasePlans,
                                               List<PurchaseOrders> orders,
                                               List<AcceptanceOrder> acceptances,
                                               List<StockIn> stockIns,
                                               Map<Long, List<AcceptanceDetail>> acceptanceDetailsMap) {
        List<PurchaseChainDto> chains = new ArrayList<>();
        Set<Long> matchedOrderIds = new HashSet<>();

        for (PurchasePlan purchasePlan : purchasePlans) {
            List<PurchaseOrders> planOrders = orders.stream()
                    .filter(order -> Objects.equals(order.getPlanId(), purchasePlan.getId())
                            || Objects.equals(order.getId(), purchasePlan.getPurchaseOrderId())
                            || (order.getPlanId() == null
                            && StringUtils.hasText(order.getPlanCode())
                            && order.getPlanCode().equals(purchasePlan.getPlanCode())))
                    .collect(Collectors.toList());
            if (planOrders.isEmpty()) {
                chains.add(buildChain(plan, purchasePlan, null, acceptances, stockIns, acceptanceDetailsMap));
            } else {
                for (PurchaseOrders order : planOrders) {
                    chains.add(buildChain(plan, purchasePlan, order, acceptances, stockIns, acceptanceDetailsMap));
                    matchedOrderIds.add(order.getId());
                }
            }
        }

        // 没有采购计划的手工订单，也单独形成一条链路
        for (PurchaseOrders order : orders) {
            if (!matchedOrderIds.contains(order.getId())) {
                chains.add(buildChain(plan, null, order, acceptances, stockIns, acceptanceDetailsMap));
            }
        }

        chains.sort(Comparator.comparing(PurchaseChainDto::getChainId,
                Comparator.nullsLast(String::compareTo)));
        return chains;
    }

    /**
     * 组装单条采购链路
     *
     * @param plan                 生产计划
     * @param purchasePlan         采购计划
     * @param order                采购订单
     * @param allAcceptances       全部验收单
     * @param allStockIns          全部入库单
     * @param acceptanceDetailsMap 验收明细
     * @return 采购链路
     */
    private PurchaseChainDto buildChain(ProductionPlan plan,
                                        PurchasePlan purchasePlan,
                                        PurchaseOrders order,
                                        List<AcceptanceOrder> allAcceptances,
                                        List<StockIn> allStockIns,
                                        Map<Long, List<AcceptanceDetail>> acceptanceDetailsMap) {
        List<AcceptanceOrder> acceptances = filterAcceptances(order, purchasePlan, allAcceptances);
        AcceptanceOrder currentAcceptance = selectCurrentAcceptance(acceptances);
        List<StockIn> stockIns = filterStockIns(plan, order, acceptances, allStockIns);
        StockIn currentStockIn = selectCurrentStockIn(stockIns);

        ChainStage stage = resolveDetailStage(purchasePlan, order, currentAcceptance);
        List<PurchaseNodeDto> nodes = buildNodes(purchasePlan, order, acceptances,
                currentAcceptance, currentStockIn, stage, acceptanceDetailsMap);

        PurchaseChainDto chain = new PurchaseChainDto();
        chain.setChainId(order != null ? "ORDER-" + order.getId()
                : (purchasePlan != null ? "PLAN-" + purchasePlan.getId() : "PLAN-" + plan.getId()));
        chain.setPurchasePlan(purchasePlan);
        chain.setPurchaseOrder(order);
        chain.setAcceptances(acceptances);
        chain.setStockIns(stockIns);
        chain.setCurrentStageCode(stage == null ? null : stage.node.getCode());
        chain.setCurrentStageName(stage == null ? null : stage.node.getName());
        chain.setCurrentStageState(stage == null ? null : stage.state);
        chain.setProgressPercent(stage == null ? 0 : stageProgressPercent(stage));
        chain.setAbnormal(stage != null && "ABNORMAL".equals(stage.state));
        chain.setAbnormalReason(stage == null ? null : stage.abnormalReason);
        chain.setNodes(nodes);
        chain.setTotalDurationMinutes(calculateTotalDuration(nodes));
        chain.setElapsedMinutes(calculateElapsedMinutes(nodes,
                chain.getCurrentStageState()));
        return chain;
    }

    /**
     * 筛选采购订单关联的验收单
     *
     * @param order        采购订单
     * @param purchasePlan 采购计划
     * @param all          全部验收单
     * @return 验收单列表
     */
    private List<AcceptanceOrder> filterAcceptances(PurchaseOrders order,
                                                    PurchasePlan purchasePlan,
                                                    List<AcceptanceOrder> all) {
        if (order == null) {
            if (purchasePlan == null || !StringUtils.hasText(purchasePlan.getProductionPlanCode())) {
                return List.of();
            }
            return all.stream()
                    .filter(acceptance -> purchasePlan.getProductionPlanCode().equals(acceptance.getPlanCode())
                            && "采购入库".equals(acceptance.getSourceType()))
                    .sorted(Comparator.comparing(AcceptanceOrder::getCreatedTime,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }
        String orderNumber = order.getPurchaseNumber();
        if (!StringUtils.hasText(orderNumber)) {
            return List.of();
        }
        return all.stream()
                .filter(acceptance -> orderNumber.equals(acceptance.getPurchaseNumber())
                        || orderNumber.equals(acceptance.getRelatedOrder()))
                .sorted(Comparator.comparing(AcceptanceOrder::getCreatedTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 选择当前验收单：优先最新一张未作废验收单
     *
     * @param acceptances 验收单列表
     * @return 当前验收单
     */
    private AcceptanceOrder selectCurrentAcceptance(List<AcceptanceOrder> acceptances) {
        if (acceptances == null || acceptances.isEmpty()) {
            return null;
        }
        return acceptances.stream()
                .filter(acceptance -> !"已作废".equals(acceptance.getStatus()))
                .max(Comparator.comparing(AcceptanceOrder::getCreatedTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseGet(() -> acceptances.stream()
                        .max(Comparator.comparing(AcceptanceOrder::getCreatedTime,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(acceptances.get(0)));
    }

    /**
     * 筛选采购订单关联的入库单
     *
     * @param plan        生产计划
     * @param order       采购订单
     * @param acceptances 验收单列表
     * @param all         全部入库单
     * @return 入库单列表
     */
    private List<StockIn> filterStockIns(ProductionPlan plan,
                                         PurchaseOrders order,
                                         List<AcceptanceOrder> acceptances,
                                         List<StockIn> all) {
        Set<String> acceptanceCodes = acceptances.stream()
                .map(AcceptanceOrder::getAcceptanceCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<StockIn> result = all.stream()
                .filter(stockIn -> StringUtils.hasText(stockIn.getRelatedOrder())
                        && acceptanceCodes.contains(stockIn.getRelatedOrder()))
                .sorted(Comparator.comparing(StockIn::getInDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        if (!result.isEmpty()) {
            return result;
        }
        String planNumber = order != null && StringUtils.hasText(order.getProductionPlanCode())
                ? order.getProductionPlanCode() : plan.getPlanNumber();
        if (!StringUtils.hasText(planNumber)) {
            return List.of();
        }
        return all.stream()
                .filter(stockIn -> planNumber.equals(stockIn.getPlanNumber())
                        && "采购入库".equals(stockIn.getInType()))
                .sorted(Comparator.comparing(StockIn::getInDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 选择当前入库单（取最新入库时间）
     *
     * @param stockIns 入库单列表
     * @return 当前入库单
     */
    private StockIn selectCurrentStockIn(List<StockIn> stockIns) {
        if (stockIns == null || stockIns.isEmpty()) {
            return null;
        }
        return stockIns.stream()
                .max(Comparator.comparing(StockIn::getInDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(stockIns.get(stockIns.size() - 1));
    }

    /**
     * 详情阶段解析：在订单状态基础上补充验收单作废判断
     *
     * @param purchasePlan     采购计划
     * @param order            采购订单
     * @param currentAcceptance 当前验收单
     * @return 链路阶段
     */
    private ChainStage resolveDetailStage(PurchasePlan purchasePlan,
                                          PurchaseOrders order,
                                          AcceptanceOrder currentAcceptance) {
        ChainStage stage = resolveStage(purchasePlan, order);
        if (currentAcceptance != null && "已作废".equals(currentAcceptance.getStatus())
                && (order == null || !"已作废".equals(stringValue(order.getStatus())))) {
            return ChainStage.abnormal(PurchaseProcessNode.ARRIVE, 3, "验收单已作废");
        }
        // 验收明细已全部终结但订单仍是验收中时，下一节点应为入库
        if (stage != null && stage.node == PurchaseProcessNode.ACCEPT && "CURRENT".equals(stage.state)
                && currentAcceptance != null && currentAcceptance.getAcceptanceTime() != null) {
            return ChainStage.current(PurchaseProcessNode.INBOUND);
        }
        return stage;
    }

    /**
     * 构建六个流程节点
     *
     * @param plan                 生产计划
     * @param purchasePlan         采购计划
     * @param order                采购订单
     * @param currentAcceptance    当前验收单
     * @param currentStockIn       当前入库单
     * @param stage                链路阶段
     * @param acceptanceDetailsMap 验收明细
     * @return 节点列表
     */
    private List<PurchaseNodeDto> buildNodes(PurchasePlan purchasePlan,
                                             PurchaseOrders order,
                                             List<AcceptanceOrder> acceptances,
                                             AcceptanceOrder currentAcceptance,
                                             StockIn currentStockIn,
                                             ChainStage stage,
                                             Map<Long, List<AcceptanceDetail>> acceptanceDetailsMap) {
        List<PurchaseNodeDto> nodes = new ArrayList<>();
        for (PurchaseProcessNode node : PurchaseProcessNode.values()) {
            if (node == PurchaseProcessNode.ABNORMAL) {
                continue;
            }
            NodeTime nodeTime = resolveNodeTime(node, purchasePlan, order, acceptances,
                    currentAcceptance, currentStockIn);
            String state = resolveNodeState(node, stage);
            RelatedDocumentDto doc = buildNodeDoc(node, purchasePlan, order, acceptances,
                    currentAcceptance, currentStockIn);
            // 未开始/跳过的节点不返回时间和单据，避免历史兜底时间造成误导
            if ("PENDING".equals(state) || "SKIPPED".equals(state)) {
                nodeTime = NodeTime.empty();
                doc = null;
            }
            PurchaseNodeDto dto = new PurchaseNodeDto();
            dto.setCode(node.getCode());
            dto.setName(node.getName());
            dto.setState(state);
            dto.setTime(nodeTime.time);
            dto.setEstimated(nodeTime.estimated);
            dto.setDoc(doc);
            if (node == PurchaseProcessNode.APPROVE && purchasePlan != null) {
                dto.setRemark(purchasePlan.getApprovalOpinion());
            }
            if (node == PurchaseProcessNode.ACCEPT && currentAcceptance != null
                    && !"PENDING".equals(state) && !"SKIPPED".equals(state)) {
                List<AcceptanceDetail> details = acceptanceDetailsMap.getOrDefault(
                        currentAcceptance.getAcceptanceId(), List.of());
                dto.setTotalDetails(details.size());
                dto.setCompletedDetails((int) details.stream()
                        .filter(detail -> AcceptanceStatusPolicy.isDetailTerminal(detail.getStatus()))
                        .count());
            }
            nodes.add(dto);
        }
        fillNodeDurations(nodes);
        return nodes;
    }

    /**
     * 解析节点时间，字段为空时按历史数据兜底并标记 estimated
     *
     * @param node              节点
     * @param purchasePlan      采购计划
     * @param order             采购订单
     * @param currentAcceptance 当前验收单
     * @param currentStockIn    当前入库单
     * @return 节点时间
     */
    private NodeTime resolveNodeTime(PurchaseProcessNode node,
                                     PurchasePlan purchasePlan,
                                     PurchaseOrders order,
                                     List<AcceptanceOrder> acceptances,
                                     AcceptanceOrder currentAcceptance,
                                     StockIn currentStockIn) {
        switch (node) {
            case APPLY:
                return NodeTime.exact(purchasePlan == null ? null : purchasePlan.getCreatedTime());
            case APPROVE:
                if (purchasePlan != null && purchasePlan.getApprovalTime() != null) {
                    return NodeTime.exact(purchasePlan.getApprovalTime());
                }
                // 历史数据：订单由审批通过时自动生成，用订单创建时间近似审批时间
                if (order != null && order.getCreatedTime() != null) {
                    return NodeTime.estimated(order.getCreatedTime());
                }
                if (purchasePlan != null && purchasePlan.getUpdatedTime() != null
                        && ("已审批".equals(purchasePlan.getStatus()) || "已驳回".equals(purchasePlan.getStatus()))) {
                    return NodeTime.estimated(purchasePlan.getUpdatedTime());
                }
                return NodeTime.empty();
            case ORDER:
                if (order != null && order.getOrderTime() != null) {
                    return NodeTime.exact(order.getOrderTime());
                }
                // 历史数据：验收单在确认采购时生成，取最早一张验收单的创建时间近似下单时间
                if (order != null && !"待采购".equals(stringValue(order.getStatus()))) {
                    LocalDateTime firstAcceptanceTime = acceptances.stream()
                            .map(AcceptanceOrder::getCreatedTime)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(null);
                    if (firstAcceptanceTime != null) {
                        return NodeTime.estimated(firstAcceptanceTime);
                    }
                    if (order.getUpdatedTime() != null) {
                        return NodeTime.estimated(order.getUpdatedTime());
                    }
                }
                return NodeTime.empty();
            case ARRIVE:
                // 退货重发可能有多张验收单，到货时间取最早一次确认到货时间
                LocalDateTime firstArrival = acceptances.stream()
                        .map(AcceptanceOrder::getArrivalTime)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                if (firstArrival != null) {
                    return NodeTime.exact(firstArrival);
                }
                if (currentAcceptance != null && "验收中".equals(currentAcceptance.getStatus())
                        && currentAcceptance.getUpdatedTime() != null) {
                    return NodeTime.estimated(currentAcceptance.getUpdatedTime());
                }
                return NodeTime.empty();
            case ACCEPT:
                // 退货重发可能有多张验收单，验收完成时间取最后一次完成时间
                LocalDateTime lastAcceptance = acceptances.stream()
                        .map(AcceptanceOrder::getAcceptanceTime)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
                if (lastAcceptance != null) {
                    return NodeTime.exact(lastAcceptance);
                }
                LocalDateTime fallbackAcceptance = acceptances.stream()
                        .filter(acceptance -> "已入库".equals(acceptance.getStatus())
                                || "已结束".equals(acceptance.getStatus())
                                || "已归档".equals(acceptance.getStatus()))
                        .map(AcceptanceOrder::getUpdatedTime)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
                if (fallbackAcceptance != null) {
                    return NodeTime.estimated(fallbackAcceptance);
                }
                return NodeTime.empty();
            case INBOUND:
                if (currentStockIn != null && currentStockIn.getInDate() != null) {
                    return NodeTime.exact(currentStockIn.getInDate());
                }
                if (currentStockIn != null && currentStockIn.getCreatedTime() != null) {
                    return NodeTime.estimated(currentStockIn.getCreatedTime());
                }
                return NodeTime.empty();
            default:
                return NodeTime.empty();
        }
    }

    /**
     * 解析节点状态
     *
     * @param node  节点
     * @param stage 链路阶段
     * @return 节点状态
     */
    private String resolveNodeState(PurchaseProcessNode node, ChainStage stage) {
        if (stage == null) {
            return "PENDING";
        }
        if ("ABNORMAL".equals(stage.state)) {
            if (node.getIndex() < stage.node.getIndex()) {
                return "DONE";
            }
            if (node == stage.node) {
                return "ABNORMAL";
            }
            return "PENDING";
        }
        if (node.getIndex() < stage.node.getIndex()) {
            return "DONE";
        }
        if (node == stage.node) {
            return "DONE".equals(stage.state) ? "DONE" : "CURRENT";
        }
        // 已结束（全部退货）时无需入库
        if (node == PurchaseProcessNode.INBOUND
                && stage.node == PurchaseProcessNode.ACCEPT
                && "DONE".equals(stage.state)) {
            return "SKIPPED";
        }
        return "PENDING";
    }

    /**
     * 构建节点对应单据
     *
     * @param node              节点
     * @param purchasePlan      采购计划
     * @param order             采购订单
     * @param currentAcceptance 当前验收单
     * @param currentStockIn    当前入库单
     * @return 单据信息
     */
    private RelatedDocumentDto buildNodeDoc(PurchaseProcessNode node,
                                            PurchasePlan purchasePlan,
                                            PurchaseOrders order,
                                            List<AcceptanceOrder> acceptances,
                                            AcceptanceOrder currentAcceptance,
                                            StockIn currentStockIn) {
        switch (node) {
            case APPLY:
            case APPROVE:
                return buildPurchasePlanDoc(purchasePlan);
            case ORDER:
                return buildPurchaseOrderDoc(order);
            case ARRIVE:
                AcceptanceOrder arrivalAcceptance = acceptances.stream()
                        .filter(acceptance -> acceptance.getArrivalTime() != null)
                        .min(Comparator.comparing(AcceptanceOrder::getArrivalTime))
                        .orElse(currentAcceptance);
                return buildAcceptanceDoc(arrivalAcceptance);
            case ACCEPT:
                AcceptanceOrder completedAcceptance = acceptances.stream()
                        .filter(acceptance -> acceptance.getAcceptanceTime() != null)
                        .max(Comparator.comparing(AcceptanceOrder::getAcceptanceTime))
                        .orElse(currentAcceptance);
                return buildAcceptanceDoc(completedAcceptance);
            case INBOUND:
                return buildStockInDoc(currentStockIn);
            default:
                return null;
        }
    }

    /**
     * 填充节点间耗时
     *
     * @param nodes 节点列表
     */
    private void fillNodeDurations(List<PurchaseNodeDto> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            PurchaseNodeDto current = nodes.get(i);
            if (i == 0 || current.getTime() == null) {
                continue;
            }
            PurchaseNodeDto previous = nodes.get(i - 1);
            if (previous.getTime() == null) {
                continue;
            }
            long minutes = Duration.between(previous.getTime(), current.getTime()).toMinutes();
            if (minutes < 0) {
                // 时间倒挂属于数据异常，不展示负耗时
                current.setDurationEstimated(true);
                continue;
            }
            current.setDurationMinutes(minutes);
            current.setDurationText(formatDuration(minutes));
            current.setDurationEstimated(Boolean.TRUE.equals(current.getEstimated())
                    || Boolean.TRUE.equals(previous.getEstimated()));
        }
    }

    /**
     * 计算链路总耗时（第一个节点到最后一个已完成节点）
     *
     * @param nodes 节点列表
     * @return 总耗时分钟数
     */
    private Long calculateTotalDuration(List<PurchaseNodeDto> nodes) {
        LocalDateTime first = null;
        LocalDateTime last = null;
        for (PurchaseNodeDto node : nodes) {
            if (node.getTime() == null) {
                continue;
            }
            if (first == null || node.getTime().isBefore(first)) {
                first = node.getTime();
            }
            if (last == null || node.getTime().isAfter(last)) {
                last = node.getTime();
            }
        }
        if (first == null || last == null) {
            return null;
        }
        return Duration.between(first, last).toMinutes();
    }

    /**
     * 计算链路已耗时
     *
     * @param nodes 节点列表
     * @param state 当前阶段状态
     * @return 已耗时分钟数
     */
    private Long calculateElapsedMinutes(List<PurchaseNodeDto> nodes, String state) {
        if ("DONE".equals(state) || "ABNORMAL".equals(state)) {
            return null;
        }
        LocalDateTime first = null;
        for (PurchaseNodeDto node : nodes) {
            if (node.getTime() != null) {
                first = node.getTime();
                break;
            }
        }
        if (first == null) {
            return null;
        }
        return Duration.between(first, LocalDateTime.now()).toMinutes();
    }

    // endregion

    // region 逾期计算
    // ===================================
    // 逾期计算
    // ===================================

    /**
     * 计算生产计划逾期信息
     *
     * @param plan 生产计划
     * @return 逾期信息
     */
    @Override
    public ProductionPlanOverdueDto calculateOverdue(ProductionPlan plan) {
        ProductionPlanOverdueDto dto = new ProductionPlanOverdueDto();
        if (plan == null) {
            dto.setPlanProductionTime(false);
            dto.setPlanProductionOverdueMinutes(0L);
            dto.setDeliveryTime(false);
            dto.setDeliveryOverdueMinutes(0L);
            dto.setLevel("NONE");
            dto.setText(null);
            return dto;
        }
        boolean finished = "已完成".equals(plan.getCurrentStatus())
                || "已归档".equals(plan.getCurrentStatus())
                || Integer.valueOf(1).equals(plan.getIsArchived());
        long planMinutes = overdueMinutes(plan.getPlanProductionTime(), finished);
        long deliveryMinutes = overdueMinutes(plan.getDeliveryTime(), finished);

        dto.setPlanProductionTime(planMinutes > 0);
        dto.setPlanProductionOverdueMinutes(planMinutes);
        dto.setDeliveryTime(deliveryMinutes > 0);
        dto.setDeliveryOverdueMinutes(deliveryMinutes);
        if (planMinutes > 0 && deliveryMinutes > 0) {
            dto.setLevel("BOTH");
        } else if (planMinutes > 0) {
            dto.setLevel("PLAN");
        } else if (deliveryMinutes > 0) {
            dto.setLevel("DELIVERY");
        } else {
            dto.setLevel("NONE");
        }
        dto.setText(buildOverdueText(planMinutes, deliveryMinutes));
        return dto;
    }

    /**
     * 计算单个时间字段的逾期分钟数
     *
     * @param target   目标时间
     * @param finished 计划是否已完成/已归档
     * @return 逾期分钟数，未逾期返回 0
     */
    private long overdueMinutes(LocalDateTime target, boolean finished) {
        if (target == null || finished) {
            return 0L;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(target)) {
            return 0L;
        }
        return Duration.between(target, now).toMinutes();
    }

    /**
     * 构建逾期展示文案
     *
     * @param planMinutes     计划生产时间逾期分钟数
     * @param deliveryMinutes 需交付时间逾期分钟数
     * @return 展示文案
     */
    private String buildOverdueText(long planMinutes, long deliveryMinutes) {
        List<String> parts = new ArrayList<>();
        if (planMinutes > 0) {
            parts.add("计划生产时间逾期" + formatDuration(planMinutes));
        }
        if (deliveryMinutes > 0) {
            parts.add("需交付时间逾期" + formatDuration(deliveryMinutes));
        }
        return parts.isEmpty() ? null : String.join("，", parts);
    }

    // endregion

    // region 关联单据
    // ===================================
    // 关联单据
    // ===================================

    /**
     * 构建五个子页面的关联单据
     *
     * @param plan               生产计划
     * @param purchasePlans      采购计划列表
     * @param orders             采购订单列表
     * @param acceptances        验收单列表
     * @param stockIns           入库单列表
     * @param workOrders         工单列表
     * @param inspectionRequests 请检记录列表
     * @param inspectionRecords  检验记录列表
     * @param statusLogs         计划状态流水
     * @return 关联单据分组
     */
    private ProductionPlanRelatedDocumentsDto buildRelatedDocuments(ProductionPlan plan,
                                                                    List<PurchasePlan> purchasePlans,
                                                                    List<PurchaseOrders> orders,
                                                                    List<AcceptanceOrder> acceptances,
                                                                    List<StockIn> stockIns,
                                                                    List<WorkOrder> workOrders,
                                                                    List<InspectionRequest> inspectionRequests,
                                                                    List<InspectionRecord> inspectionRecords,
                                                                    List<PlanStatusLog> statusLogs) {
        ProductionPlanRelatedDocumentsDto documents = new ProductionPlanRelatedDocumentsDto();

        List<RelatedDocumentDto> purchase = new ArrayList<>();
        for (PurchasePlan purchasePlan : purchasePlans) {
            purchase.add(buildPurchasePlanDoc(purchasePlan));
        }
        for (PurchaseOrders order : orders) {
            purchase.add(buildPurchaseOrderDoc(order));
        }
        for (AcceptanceOrder acceptance : acceptances) {
            purchase.add(buildAcceptanceDoc(acceptance));
        }
        documents.setPurchase(sortDocuments(purchase));

        List<RelatedDocumentDto> planDocs = new ArrayList<>();
        planDocs.add(buildProductionPlanDoc(plan));
        for (PlanStatusLog statusLog : statusLogs) {
            planDocs.add(buildPlanStatusLogDoc(statusLog));
        }
        documents.setPlan(sortDocuments(planDocs));

        List<RelatedDocumentDto> production = new ArrayList<>();
        for (WorkOrder workOrder : workOrders) {
            production.add(buildWorkOrderDoc(workOrder));
        }
        documents.setProduction(sortDocuments(production));

        List<RelatedDocumentDto> inspection = new ArrayList<>();
        for (InspectionRequest request : inspectionRequests) {
            inspection.add(buildInspectionRequestDoc(request));
        }
        for (InspectionRecord record : inspectionRecords) {
            inspection.add(buildInspectionRecordDoc(record));
        }
        documents.setInspection(sortDocuments(inspection));

        List<RelatedDocumentDto> inbound = new ArrayList<>();
        for (StockIn stockIn : stockIns) {
            inbound.add(buildStockInDoc(stockIn));
        }
        documents.setInbound(sortDocuments(inbound));
        return documents;
    }

    /**
     * 构建采购计划单据
     *
     * @param purchasePlan 采购计划
     * @return 单据信息
     */
    private RelatedDocumentDto buildPurchasePlanDoc(PurchasePlan purchasePlan) {
        if (purchasePlan == null) {
            return null;
        }
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("PURCHASE_PLAN");
        doc.setId(purchasePlan.getId());
        doc.setCode(purchasePlan.getPlanCode());
        doc.setTitle(purchasePlan.getTitle());
        doc.setStatus(purchasePlan.getStatus());
        doc.setTime(purchasePlan.getCreatedTime());
        doc.setDetailPath("/api/purchase-plan/" + purchasePlan.getId());
        doc.setCategory("PURCHASE");
        return doc;
    }

    /**
     * 构建采购订单单据
     *
     * @param order 采购订单
     * @return 单据信息
     */
    private RelatedDocumentDto buildPurchaseOrderDoc(PurchaseOrders order) {
        if (order == null) {
            return null;
        }
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("PURCHASE_ORDER");
        doc.setId(order.getId());
        doc.setCode(order.getPurchaseNumber());
        doc.setTitle(order.getTitle());
        doc.setStatus(stringValue(order.getStatus()));
        doc.setTime(order.getCreatedTime());
        doc.setDetailPath("/api/purchase-orders/" + order.getId());
        doc.setCategory("PURCHASE");
        return doc;
    }

    /**
     * 构建验收单单据
     *
     * @param acceptance 验收单
     * @return 单据信息
     */
    private RelatedDocumentDto buildAcceptanceDoc(AcceptanceOrder acceptance) {
        if (acceptance == null) {
            return null;
        }
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("ACCEPTANCE");
        doc.setId(acceptance.getAcceptanceId());
        doc.setCode(acceptance.getAcceptanceCode());
        doc.setTitle(acceptance.getTitle());
        doc.setStatus(acceptance.getStatus());
        doc.setTime(acceptance.getCreatedTime());
        doc.setDetailPath("/api/acceptance/" + acceptance.getAcceptanceId());
        doc.setCategory("PURCHASE");
        return doc;
    }

    /**
     * 构建入库单单据
     *
     * @param stockIn 入库单
     * @return 单据信息
     */
    private RelatedDocumentDto buildStockInDoc(StockIn stockIn) {
        if (stockIn == null) {
            return null;
        }
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("STOCK_IN");
        doc.setId(stockIn.getInId());
        doc.setCode(stockIn.getInCode());
        doc.setTitle(StringUtils.hasText(stockIn.getRelatedOrderTitle())
                ? stockIn.getRelatedOrderTitle() : stockIn.getPlanTitle());
        doc.setStatus(stockIn.getInStatus());
        doc.setTime(stockIn.getInDate());
        doc.setDetailPath("/api/stockin/" + stockIn.getInId());
        doc.setCategory("INBOUND");
        return doc;
    }

    /**
     * 构建生产计划单据
     *
     * @param plan 生产计划
     * @return 单据信息
     */
    private RelatedDocumentDto buildProductionPlanDoc(ProductionPlan plan) {
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("PRODUCTION_PLAN");
        doc.setId(plan.getId() == null ? null : plan.getId().longValue());
        doc.setCode(plan.getPlanNumber());
        doc.setTitle(plan.getPlanName());
        doc.setStatus(plan.getCurrentStatus());
        doc.setTime(plan.getCreatedTime());
        doc.setDetailPath("/api/production-plans/" + plan.getId());
        doc.setCategory("PLAN");
        return doc;
    }

    /**
     * 构建计划状态流水单据
     *
     * @param statusLog 计划状态流水
     * @return 单据信息
     */
    private RelatedDocumentDto buildPlanStatusLogDoc(PlanStatusLog statusLog) {
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("PLAN_STATUS_LOG");
        doc.setId(statusLog.getId() == null ? null : statusLog.getId().longValue());
        doc.setCode(statusLog.getFromStatus() + " → " + statusLog.getToStatus());
        doc.setTitle(statusLog.getRemark());
        doc.setStatus(statusLog.getToStatus());
        doc.setTime(statusLog.getChangeTime());
        doc.setDetailPath("/api/plan-status-logs/" + statusLog.getId());
        doc.setCategory("PLAN");
        return doc;
    }

    /**
     * 构建工单单据
     *
     * @param workOrder 工单
     * @return 单据信息
     */
    private RelatedDocumentDto buildWorkOrderDoc(WorkOrder workOrder) {
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("WORK_ORDER");
        doc.setId(workOrder.getWorkOrderId());
        doc.setCode(workOrder.getWorkOrderCode());
        doc.setTitle(workOrder.getWorkOrderName());
        doc.setStatus(workOrder.getCurrentStatus());
        doc.setTime(workOrder.getConfigDate());
        doc.setDetailPath("/api/work-orders/" + workOrder.getWorkOrderId());
        doc.setCategory("PRODUCTION");
        return doc;
    }

    /**
     * 构建请检记录单据
     *
     * @param request 请检记录
     * @return 单据信息
     */
    private RelatedDocumentDto buildInspectionRequestDoc(InspectionRequest request) {
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("INSPECTION_REQUEST");
        doc.setId(request.getId());
        doc.setCode(request.getInspectionCode());
        doc.setTitle(request.getInspectionItemName());
        doc.setStatus(request.getWorkOrderCode());
        doc.setTime(request.getRequestTime());
        doc.setDetailPath("/api/inspection-request/" + request.getId());
        doc.setCategory("INSPECTION");
        return doc;
    }

    /**
     * 构建检验记录单据
     *
     * @param record 检验记录
     * @return 单据信息
     */
    private RelatedDocumentDto buildInspectionRecordDoc(InspectionRecord record) {
        RelatedDocumentDto doc = new RelatedDocumentDto();
        doc.setType("INSPECTION_RECORD");
        doc.setId(record.getId());
        doc.setCode(record.getInspectionCode());
        doc.setTitle(record.getObjectName());
        doc.setStatus(record.getConclusion());
        doc.setTime(record.getEndTime() != null ? record.getEndTime() : record.getStartTime());
        doc.setDetailPath("/api/inspectionRecord/" + record.getId());
        doc.setCategory("INSPECTION");
        return doc;
    }

    /**
     * 按时间倒序排列单据
     *
     * @param documents 单据列表
     * @return 排序后的列表
     */
    private List<RelatedDocumentDto> sortDocuments(List<RelatedDocumentDto> documents) {
        return documents.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RelatedDocumentDto::getTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // endregion

    // region 通用工具方法
    // ===================================
    // 通用工具方法
    // ===================================

    /**
     * 去除重复验收单
     *
     * @param acceptances 验收单列表
     * @return 去重后的列表
     */
    private List<AcceptanceOrder> dedupeAcceptances(List<AcceptanceOrder> acceptances) {
        Map<Long, AcceptanceOrder> byId = new LinkedHashMap<>();
        for (AcceptanceOrder acceptance : acceptances) {
            if (acceptance.getAcceptanceId() != null) {
                byId.putIfAbsent(acceptance.getAcceptanceId(), acceptance);
            }
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * 去除重复入库单
     *
     * @param stockIns 入库单列表
     * @return 去重后的列表
     */
    private List<StockIn> dedupeStockIns(List<StockIn> stockIns) {
        Map<Long, StockIn> byId = new LinkedHashMap<>();
        for (StockIn stockIn : stockIns) {
            if (stockIn.getInId() != null) {
                byId.putIfAbsent(stockIn.getInId(), stockIn);
            }
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * 安全获取对象字符串值
     *
     * @param value 对象
     * @return 字符串值，null 返回 null
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 取两个时间中的较晚者
     *
     * @param first  时间1
     * @param second 时间2
     * @return 较晚时间
     */
    private LocalDateTime maxTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    /**
     * 格式化分钟数为中文时长
     *
     * @param minutes 分钟数
     * @return 中文时长
     */
    private String formatDuration(long minutes) {
        if (minutes <= 0) {
            return "0分钟";
        }
        long days = minutes / (24 * 60);
        long hours = (minutes % (24 * 60)) / 60;
        long mins = minutes % 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("天");
        }
        if (hours > 0) {
            builder.append(hours).append("小时");
        }
        if (mins > 0 || builder.length() == 0) {
            builder.append(mins).append("分钟");
        }
        return builder.toString();
    }

    // endregion

    // region 内部阶段模型
    // ===================================
    // 内部阶段模型
    // ===================================

    /**
     * 链路阶段内部模型
     */
    private static final class ChainStage {

        /** 当前节点 */
        private final PurchaseProcessNode node;

        /** 当前状态：CURRENT/DONE/ABNORMAL */
        private final String state;

        /** 已完成节点数 */
        private final int completedCount;

        /** 是否存在数据不一致 */
        private final boolean dataInconsistent;

        /** 异常原因 */
        private final String abnormalReason;

        private ChainStage(PurchaseProcessNode node, String state, int completedCount,
                           boolean dataInconsistent, String abnormalReason) {
            this.node = node;
            this.state = state;
            this.completedCount = completedCount;
            this.dataInconsistent = dataInconsistent;
            this.abnormalReason = abnormalReason;
        }

        private static ChainStage current(PurchaseProcessNode node) {
            return new ChainStage(node, "CURRENT", node.getIndex(), false, null);
        }

        private static ChainStage current(PurchaseProcessNode node, boolean dataInconsistent) {
            return new ChainStage(node, "CURRENT", node.getIndex(), dataInconsistent, null);
        }

        private static ChainStage done(PurchaseProcessNode node) {
            return new ChainStage(node, "DONE", Math.min(6, node.getIndex() + 1), false, null);
        }

        private static ChainStage abnormal(PurchaseProcessNode node, int completedCount, String reason) {
            return new ChainStage(node, "ABNORMAL", completedCount, false, reason);
        }
    }

    /**
     * 节点时间内部模型
     */
    private static final class NodeTime {

        /** 节点时间 */
        private final LocalDateTime time;

        /** 是否历史近似值 */
        private final boolean estimated;

        private NodeTime(LocalDateTime time, boolean estimated) {
            this.time = time;
            this.estimated = estimated;
        }

        private static NodeTime exact(LocalDateTime time) {
            return new NodeTime(time, false);
        }

        private static NodeTime estimated(LocalDateTime time) {
            return new NodeTime(time, true);
        }

        private static NodeTime empty() {
            return new NodeTime(null, false);
        }
    }

    // endregion
}

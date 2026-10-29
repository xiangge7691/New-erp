package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.Dashboard.CardDetailDto;
import com.tonghui.erp.Common.Dto.Dashboard.ChartDataDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardDetailRowDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMetricsDto;
import com.tonghui.erp.Common.utils.DashboardCalcUtils;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.DashboardProductionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首页生产统计实现
 * <p>
 * 一次加载（{@link #load}）计算卡片、明细与图表所需数据，
 * 卡片主数值（单据数）与明细行数保持一致。
 * </p>
 */
@Service
public class DashboardProductionServiceImpl implements DashboardProductionService {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 作废状态 */
    private static final String STATUS_VOID = "作废";

    /** 时间展示格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 品种交付量 Top 数量 */
    private static final int TOP_PREPARATION_LIMIT = 5;

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 生产计划数据访问层 */
    private final ProductionPlanMapper productionPlanMapper;

    /** 工单数据访问层 */
    private final WorkOrderMapper workOrderMapper;

    /**
     * 构造函数注入依赖
     *
     * @param productionPlanMapper 生产计划数据访问层
     * @param workOrderMapper      工单数据访问层
     */
    public DashboardProductionServiceImpl(ProductionPlanMapper productionPlanMapper,
                                          WorkOrderMapper workOrderMapper) {
        this.productionPlanMapper = productionPlanMapper;
        this.workOrderMapper = workOrderMapper;
    }

    // endregion

    // region 卡片指标
    // ===================================
    // 卡片指标
    // ===================================

    /**
     * 构建生产三张卡片
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 生产指标
     */
    @Override
    public DashboardMetricsDto.ProductionMetricsDto getProductionMetrics(String startMonth, String endMonth) {
        ProductionData data = load(startMonth, endMonth);
        DashboardMetricsDto.ProductionMetricsDto metrics = new DashboardMetricsDto.ProductionMetricsDto();
        metrics.setTotalPlan(buildTotalPlanCard(data));
        metrics.setDelivery(buildDeliveryCard(data));
        metrics.setPending(buildPendingCard(data));
        return metrics;
    }

    // endregion

    // region 卡片明细
    // ===================================
    // 卡片明细
    // ===================================

    /**
     * 总计划量明细
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 明细
     */
    @Override
    public CardDetailDto<DashboardMetricsDto.TotalPlanCardDto, DashboardDetailRowDto>
    getTotalPlanDetails(String startMonth, String endMonth) {
        ProductionData data = load(startMonth, endMonth);
        return new CardDetailDto<>("totalPlan", buildTotalPlanCard(data), data.planRows);
    }

    /**
     * 总交付量明细
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 明细
     */
    @Override
    public CardDetailDto<DashboardMetricsDto.DeliveryCardDto, DashboardDetailRowDto>
    getDeliveryDetails(String startMonth, String endMonth) {
        ProductionData data = load(startMonth, endMonth);
        return new CardDetailDto<>("delivery", buildDeliveryCard(data), data.deliveryRows);
    }

    /**
     * 待生产明细
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 明细
     */
    @Override
    public CardDetailDto<DashboardMetricsDto.PendingCardDto, DashboardDetailRowDto>
    getPendingDetails(String startMonth, String endMonth) {
        ProductionData data = load(startMonth, endMonth);
        return new CardDetailDto<>("pending", buildPendingCard(data), data.pendingRows);
    }

    // endregion

    // region 图表数据
    // ===================================
    // 图表数据
    // ===================================

    /**
     * 生产分析图表
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 生产分析图表
     */
    @Override
    public ChartDataDto.ProductionCharts getProductionCharts(String startMonth, String endMonth) {
        ProductionData data = load(startMonth, endMonth);
        ChartDataDto.ProductionCharts charts = new ChartDataDto.ProductionCharts();

        // 计划量 vs 交付量
        List<ChartDataDto.PlanVsDeliveryItem> planVsDelivery = new ArrayList<>();
        for (String month : DashboardCalcUtils.mergeMonths(
            data.planQtyByMonth.keySet(), data.deliveryQtyByMonth.keySet())) {
            ChartDataDto.PlanVsDeliveryItem item = new ChartDataDto.PlanVsDeliveryItem();
            item.setMonth(month);
            item.setPlanQuantity(DashboardCalcUtils.money(data.planQtyByMonth.get(month)));
            item.setDeliveryQuantity(DashboardCalcUtils.money(data.deliveryQtyByMonth.get(month)));
            planVsDelivery.add(item);
        }
        charts.setPlanVsDelivery(planVsDelivery);

        // 月度待生产量
        List<ChartDataDto.PendingMonthItem> pendingByMonth = new ArrayList<>();
        for (String month : DashboardCalcUtils.mergeMonths(data.pendingQtyByMonth.keySet())) {
            ChartDataDto.PendingMonthItem item = new ChartDataDto.PendingMonthItem();
            item.setMonth(month);
            item.setPendingQuantity(DashboardCalcUtils.money(data.pendingQtyByMonth.get(month)));
            pendingByMonth.add(item);
        }
        charts.setPendingByMonth(pendingByMonth);

        // 品种交付量 Top5
        List<ChartDataDto.PreparationRank> ranks = new ArrayList<>();
        for (Map.Entry<String, PreparationDelivery> entry : data.deliveryByPreparation.entrySet()) {
            PreparationDelivery value = entry.getValue();
            ChartDataDto.PreparationRank rank = new ChartDataDto.PreparationRank();
            rank.setPreparationCode(value.preparationCode);
            rank.setPreparationName(value.preparationName);
            rank.setDeliveryQuantity(DashboardCalcUtils.money(value.quantity));
            ranks.add(rank);
        }
        ranks.sort(Comparator.comparing(ChartDataDto.PreparationRank::getDeliveryQuantity,
            Comparator.nullsLast(Comparator.reverseOrder())));
        charts.setTopPreparations(ranks.size() > TOP_PREPARATION_LIMIT
            ? new ArrayList<>(ranks.subList(0, TOP_PREPARATION_LIMIT)) : ranks);

        return charts;
    }

    // endregion

    // region 卡片组装
    // ===================================
    // 卡片组装
    // ===================================

    /**
     * 组装总计划量卡片
     *
     * @param data 生产数据
     * @return 总计划量卡片
     */
    private DashboardMetricsDto.TotalPlanCardDto buildTotalPlanCard(ProductionData data) {
        DashboardMetricsDto.TotalPlanCardDto card = new DashboardMetricsDto.TotalPlanCardDto();
        card.setPlanCount(data.plans.size());
        card.setPlanQuantity(DashboardCalcUtils.money(data.planQtyTotal));
        card.setDeliveredQuantity(DashboardCalcUtils.money(data.planDeliveredQtyTotal));
        card.setPendingQuantity(DashboardCalcUtils.money(data.planPendingQtyTotal));
        card.setAvgQuantity(data.plans.isEmpty() ? null
            : DashboardCalcUtils.money(data.planQtyTotal.divide(
                BigDecimal.valueOf(data.plans.size()), 2, RoundingMode.HALF_UP)));
        card.setPreparationCount(data.planPreparationCount);
        return card;
    }

    /**
     * 组装总交付量卡片
     *
     * @param data 生产数据
     * @return 总交付量卡片
     */
    private DashboardMetricsDto.DeliveryCardDto buildDeliveryCard(ProductionData data) {
        DashboardMetricsDto.DeliveryCardDto card = new DashboardMetricsDto.DeliveryCardDto();
        card.setWorkOrderCount(data.deliveredWorkOrders.size());
        card.setBatchQty(DashboardCalcUtils.money(data.deliveryQtyTotal));
        card.setDeliveryRate(DashboardCalcUtils.percent(data.deliveryQtyTotal, data.planQtyTotal));
        card.setFullyDeliveredPlanCount(data.fullyDeliveredPlanCount);
        return card;
    }

    /**
     * 组装待生产卡片
     *
     * @param data 生产数据
     * @return 待生产卡片
     */
    private DashboardMetricsDto.PendingCardDto buildPendingCard(ProductionData data) {
        DashboardMetricsDto.PendingCardDto card = new DashboardMetricsDto.PendingCardDto();
        card.setPlanCount(data.pendingPlans.size());
        card.setPlanQuantity(DashboardCalcUtils.money(data.pendingPlanQtyTotal));
        card.setDeliveredQuantity(DashboardCalcUtils.money(data.pendingDeliveredQtyTotal));
        card.setPendingQuantity(DashboardCalcUtils.money(data.pendingQtyTotal));
        card.setRatioOfPlan(DashboardCalcUtils.percent(data.pendingQtyTotal, data.planQtyTotal));
        card.setPreparationCount(data.pendingPreparationCount);
        card.setMaxOverdueDays(data.maxOverdueDays);
        return card;
    }

    // endregion

    // region 核心加载
    // ===================================
    // 核心加载
    // ===================================

    /**
     * 加载并计算生产数据
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 生产数据快照
     */
    private ProductionData load(String startMonth, String endMonth) {
        ProductionData data = new ProductionData();
        LocalDate today = LocalDate.now();

        // === 1. 区间内生产计划 ===
        for (ProductionPlan plan : productionPlanMapper.selectList(null)) {
            if (plan.getId() != null) {
                data.planNumberById.put(plan.getId().longValue(), plan.getPlanNumber());
            }
            String month = DashboardCalcUtils.monthOf(plan.getPlanProductionTime());
            if (DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                data.plans.add(plan);
            }
        }
        data.plans.sort(Comparator.comparing(ProductionPlan::getPlanProductionTime,
            Comparator.nullsLast(Comparator.naturalOrder())));

        // === 2. 已交付工单（全时段，用于计算每张计划的已交付量） ===
        List<WorkOrder> allDelivered = workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
            .isNotNull("config_complete_time")
            .ne("current_status", STATUS_VOID));
        Map<Long, BigDecimal> deliveredQtyByPlan = new HashMap<>();
        for (WorkOrder workOrder : allDelivered) {
            if (workOrder.getPlanId() != null) {
                deliveredQtyByPlan.merge(workOrder.getPlanId(),
                    DashboardCalcUtils.nz(workOrder.getBatchQty()), BigDecimal::add);
            }
            String month = DashboardCalcUtils.monthOf(workOrder.getConfigCompleteTime());
            if (DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                data.deliveredWorkOrders.add(workOrder);
            }
        }
        data.deliveredWorkOrders.sort(Comparator.comparing(WorkOrder::getConfigCompleteTime));

        // === 3. 计划统计 ===
        Set<String> planPreparations = new LinkedHashSet<>();
        for (ProductionPlan plan : data.plans) {
            BigDecimal planQty = DashboardCalcUtils.nz(plan.getPlanQuantity());
            BigDecimal deliveredQty = DashboardCalcUtils.nz(
                deliveredQtyByPlan.get(plan.getId() == null ? null : plan.getId().longValue()));
            BigDecimal pendingQty = planQty.subtract(deliveredQty);
            if (pendingQty.signum() < 0) {
                pendingQty = BigDecimal.ZERO;
            }

            data.planQtyTotal = data.planQtyTotal.add(planQty);
            data.planDeliveredQtyTotal = data.planDeliveredQtyTotal.add(deliveredQty);
            data.planPendingQtyTotal = data.planPendingQtyTotal.add(pendingQty);
            String month = DashboardCalcUtils.monthOf(plan.getPlanProductionTime());
            data.planQtyByMonth.merge(month, planQty, BigDecimal::add);
            if (StringUtils.hasText(plan.getPreparationCode())) {
                planPreparations.add(plan.getPreparationCode());
            }

            // 已全部交付的计划单
            if (planQty.signum() > 0 && pendingQty.signum() == 0) {
                data.fullyDeliveredPlanCount++;
            }

            // 计划明细行
            DashboardDetailRowDto planRow = new DashboardDetailRowDto();
            planRow.setMonth(month);
            planRow.setPlanNumber(plan.getPlanNumber());
            planRow.setPreparationCode(plan.getPreparationCode());
            planRow.setPreparationName(plan.getPreparationName());
            planRow.setPlanQuantity(DashboardCalcUtils.money(planQty));
            planRow.setDeliveredQuantity(DashboardCalcUtils.money(deliveredQty));
            planRow.setPendingQuantity(DashboardCalcUtils.money(pendingQty));
            planRow.setTime(formatDateTime(plan.getPlanProductionTime()));
            planRow.setStatus(plan.getCurrentStatus());
            planRow.setRowType("正常");
            data.planRows.add(planRow);

            // 待生产（差额大于 0）
            if (pendingQty.signum() > 0) {
                PendingPlan pendingPlan = new PendingPlan();
                pendingPlan.plan = plan;
                pendingPlan.deliveredQty = deliveredQty;
                pendingPlan.pendingQty = pendingQty;
                pendingPlan.overdueDays = calcOverdueDays(plan.getPlanProductionTime(), today);
                data.pendingPlans.add(pendingPlan);

                data.pendingQtyTotal = data.pendingQtyTotal.add(pendingQty);
                data.pendingPlanQtyTotal = data.pendingPlanQtyTotal.add(planQty);
                data.pendingDeliveredQtyTotal = data.pendingDeliveredQtyTotal.add(deliveredQty);
                data.pendingQtyByMonth.merge(month, pendingQty, BigDecimal::add);
                if (data.maxOverdueDays == null || pendingPlan.overdueDays > data.maxOverdueDays) {
                    data.maxOverdueDays = pendingPlan.overdueDays;
                }

                DashboardDetailRowDto pendingRow = new DashboardDetailRowDto();
                pendingRow.setMonth(month);
                pendingRow.setPlanNumber(plan.getPlanNumber());
                pendingRow.setPreparationCode(plan.getPreparationCode());
                pendingRow.setPreparationName(plan.getPreparationName());
                pendingRow.setPlanQuantity(DashboardCalcUtils.money(planQty));
                pendingRow.setDeliveredQuantity(DashboardCalcUtils.money(deliveredQty));
                pendingRow.setPendingQuantity(DashboardCalcUtils.money(pendingQty));
                pendingRow.setOverdueDays(pendingPlan.overdueDays);
                pendingRow.setTime(formatDateTime(plan.getPlanProductionTime()));
                pendingRow.setStatus(plan.getCurrentStatus());
                pendingRow.setRowType("正常");
                data.pendingRows.add(pendingRow);
            }
        }
        data.planPreparationCount = planPreparations.size();
        Set<String> pendingPreparations = new LinkedHashSet<>();
        for (PendingPlan pendingPlan : data.pendingPlans) {
            if (StringUtils.hasText(pendingPlan.plan.getPreparationCode())) {
                pendingPreparations.add(pendingPlan.plan.getPreparationCode());
            }
        }
        data.pendingPreparationCount = pendingPreparations.size();

        // === 4. 交付统计 ===
        for (WorkOrder workOrder : data.deliveredWorkOrders) {
            BigDecimal batchQty = DashboardCalcUtils.nz(workOrder.getBatchQty());
            String month = DashboardCalcUtils.monthOf(workOrder.getConfigCompleteTime());
            data.deliveryQtyTotal = data.deliveryQtyTotal.add(batchQty);
            data.deliveryQtyByMonth.merge(month, batchQty, BigDecimal::add);

            // 品种交付量
            String key = workOrder.getPreparationCode() == null ? "" : workOrder.getPreparationCode();
            PreparationDelivery delivery = data.deliveryByPreparation.computeIfAbsent(key,
                k -> new PreparationDelivery(workOrder.getPreparationCode(), workOrder.getPreparationName()));
            delivery.quantity = delivery.quantity.add(batchQty);

            // 交付明细行
            DashboardDetailRowDto row = new DashboardDetailRowDto();
            row.setMonth(month);
            row.setWorkOrderCode(workOrder.getWorkOrderCode());
            row.setBatchNumber(workOrder.getBatchNumber());
            row.setPreparationCode(workOrder.getPreparationCode());
            row.setPreparationName(workOrder.getPreparationName());
            row.setPlanNumber(resolvePlanNumber(workOrder, data));
            row.setBatchQty(DashboardCalcUtils.money(batchQty));
            row.setTime(formatDateTime(workOrder.getConfigCompleteTime()));
            row.setStatus(workOrder.getCurrentStatus());
            row.setRowType("正常");
            data.deliveryRows.add(row);
        }

        // 交付明细按配置完成时间排序
        data.deliveryRows.sort(Comparator.comparing(DashboardDetailRowDto::getTime,
            Comparator.nullsLast(Comparator.naturalOrder())));

        return data;
    }

    /**
     * 计算滞留天数（今天 − 计划生产时间），未来计划返回 0
     *
     * @param planTime 计划生产时间
     * @param today    今天
     * @return 滞留天数
     */
    private long calcOverdueDays(LocalDateTime planTime, LocalDate today) {
        if (planTime == null) {
            return 0L;
        }
        long days = ChronoUnit.DAYS.between(planTime.toLocalDate(), today);
        return Math.max(days, 0L);
    }

    /**
     * 格式化时间
     *
     * @param dateTime 时间
     * @return 字符串；空值返回 null
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 解析工单归属的计划编号
     * <p>工单表只有 plan_id，plan_number 为非表字段恒为空，需按计划ID回查。</p>
     *
     * @param workOrder 工单
     * @param data      生产数据（含计划编号映射）
     * @return 计划编号；无法解析返回 null
     */
    private String resolvePlanNumber(WorkOrder workOrder, ProductionData data) {
        if (StringUtils.hasText(workOrder.getPlanNumber())) {
            return workOrder.getPlanNumber();
        }
        return workOrder.getPlanId() == null ? null : data.planNumberById.get(workOrder.getPlanId());
    }

    // endregion

    // region 内部数据结构
    // ===================================
    // 内部数据结构
    // ===================================

    /**
     * 生产数据快照
     */
    private static class ProductionData {

        /** 区间内生产计划 */
        private final List<ProductionPlan> plans = new ArrayList<>();

        /** 区间内已交付工单 */
        private final List<WorkOrder> deliveredWorkOrders = new ArrayList<>();

        /** 待生产计划 */
        private final List<PendingPlan> pendingPlans = new ArrayList<>();

        /** 总计划量明细 */
        private final List<DashboardDetailRowDto> planRows = new ArrayList<>();

        /** 总交付量明细 */
        private final List<DashboardDetailRowDto> deliveryRows = new ArrayList<>();

        /** 待生产明细 */
        private final List<DashboardDetailRowDto> pendingRows = new ArrayList<>();

        /** 月度计划量 */
        private final Map<String, BigDecimal> planQtyByMonth = new LinkedHashMap<>();

        /** 月度交付量 */
        private final Map<String, BigDecimal> deliveryQtyByMonth = new LinkedHashMap<>();

        /** 月度待生产量 */
        private final Map<String, BigDecimal> pendingQtyByMonth = new LinkedHashMap<>();

        /** 品种交付量 */
        private final Map<String, PreparationDelivery> deliveryByPreparation = new LinkedHashMap<>();

        /** 计划量合计 */
        private BigDecimal planQtyTotal = BigDecimal.ZERO;

        /** 交付量合计 */
        private BigDecimal deliveryQtyTotal = BigDecimal.ZERO;

        /** 待生产量合计 */
        private BigDecimal pendingQtyTotal = BigDecimal.ZERO;

        /** 区间内计划的已交付量合计（对应总计划量卡片的 deliveredQuantity） */
        private BigDecimal planDeliveredQtyTotal = BigDecimal.ZERO;

        /** 区间内计划的待生产量合计（对应总计划量卡片的 pendingQuantity） */
        private BigDecimal planPendingQtyTotal = BigDecimal.ZERO;

        /** 待生产计划的计划量合计 */
        private BigDecimal pendingPlanQtyTotal = BigDecimal.ZERO;

        /** 待生产计划的已交付量合计 */
        private BigDecimal pendingDeliveredQtyTotal = BigDecimal.ZERO;

        /** 覆盖制剂品种数 */
        private int planPreparationCount;

        /** 待生产涉及制剂品种数 */
        private int pendingPreparationCount;

        /** 已全部交付计划单数 */
        private int fullyDeliveredPlanCount;

        /** 最长滞留天数 */
        private Long maxOverdueDays;

        /** 计划ID → 计划编号（工单表无 plan_number 字段，需回填） */
        private final Map<Long, String> planNumberById = new HashMap<>();
    }

    /**
     * 待生产计划
     */
    private static class PendingPlan {

        /** 计划单 */
        private ProductionPlan plan;

        /** 已交付量 */
        private BigDecimal deliveredQty;

        /** 待生产量 */
        private BigDecimal pendingQty;

        /** 滞留天数 */
        private long overdueDays;
    }

    /**
     * 品种交付量
     */
    private static class PreparationDelivery {

        /** 制剂编码 */
        private final String preparationCode;

        /** 制剂名称 */
        private final String preparationName;

        /** 交付量 */
        private BigDecimal quantity = BigDecimal.ZERO;

        /**
         * 构造方法
         *
         * @param preparationCode 制剂编码
         * @param preparationName 制剂名称
         */
        private PreparationDelivery(String preparationCode, String preparationName) {
            this.preparationCode = preparationCode;
            this.preparationName = preparationName;
        }
    }

    // endregion
}

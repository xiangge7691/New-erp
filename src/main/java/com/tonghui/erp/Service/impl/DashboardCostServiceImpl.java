package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.Dashboard.CardDetailDto;
import com.tonghui.erp.Common.Dto.Dashboard.ChartDataDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardDetailRowDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMetricsDto;
import com.tonghui.erp.Common.utils.DashboardCalcUtils;
import com.tonghui.erp.Data.Entity.EnergyRecord;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.ReturnOrderDetail;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.EnergyRecordMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderDetailMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.DashboardCostService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首页成本核算统计实现
 * <p>
 * 一次加载（{@link #load}）同时计算卡片、明细与图表所需数据，保证同一接口内
 * 卡片总值与明细合计一致。
 * </p>
 */
@Service
public class DashboardCostServiceImpl implements DashboardCostService {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 作废状态 */
    private static final String STATUS_VOID = "作废";

    /** 计入实际成本的物料分类 */
    private static final Set<String> MATERIAL_CATEGORIES = Set.of("原料", "辅料", "包材");

    /** 自来水能耗类型 */
    private static final String ENERGY_WATER = "自来水";

    /** 电能耗类型 */
    private static final String ENERGY_ELECTRICITY = "电";

    /** 燃气能耗类型 */
    private static final String ENERGY_GAS = "燃气";

    /** 其他成本类型（能耗记录中的「其他」） */
    private static final String ENERGY_OTHER = "其他";

    /** 物料分类：原料 */
    private static final String CATEGORY_RAW = "原料";

    /** 物料分类：辅料 */
    private static final String CATEGORY_AUXILIARY = "辅料";

    /** 物料分类：包材 */
    private static final String CATEGORY_PACKAGING = "包材";

    /** 成本偏差排行条数上限 */
    private static final int VARIANCE_RANK_LIMIT = 10;

    /** 行类型：正常 */
    private static final String ROW_NORMAL = "正常";

    /** 行类型：未关联制剂/计划 */
    private static final String ROW_UNLINKED = "未关联制剂";

    /** 行类型：未分摊水电气 */
    private static final String ROW_UNALLOCATED_ENERGY = "未分摊水电气";

    /** 时间展示格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 分摊计算保留位数（中间过程） */
    private static final int ALLOC_SCALE = 2;

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 工单数据访问层 */
    private final WorkOrderMapper workOrderMapper;

    /** 制剂数据访问层 */
    private final PreparationMapper preparationMapper;

    /** 生产计划数据访问层 */
    private final ProductionPlanMapper productionPlanMapper;

    /** 出库单数据访问层 */
    private final StockOutMapper stockOutMapper;

    /** 出库明细数据访问层 */
    private final StockOutDetailMapper stockOutDetailMapper;

    /** 退库明细数据访问层 */
    private final ReturnOrderDetailMapper returnOrderDetailMapper;

    /** 退库单数据访问层（用于校验退库单是否已删除） */
    private final ReturnOrderMapper returnOrderMapper;

    /** 能耗记录数据访问层 */
    private final EnergyRecordMapper energyRecordMapper;

    /** 采购单数据访问层（批次生产周期起点） */
    private final PurchaseOrdersMapper purchaseOrdersMapper;

    /** 成品入库单数据访问层（批次生产周期终点兜底） */
    private final StockInMapper stockInMapper;

    /**
     * 构造函数注入依赖
     *
     * @param workOrderMapper        工单数据访问层
     * @param preparationMapper      制剂数据访问层
     * @param productionPlanMapper   生产计划数据访问层
     * @param stockOutMapper         出库单数据访问层
     * @param stockOutDetailMapper   出库明细数据访问层
     * @param returnOrderDetailMapper 退库明细数据访问层
     * @param returnOrderMapper      退库单数据访问层
     * @param energyRecordMapper     能耗记录数据访问层
     * @param purchaseOrdersMapper   采购单数据访问层
     * @param stockInMapper          成品入库单数据访问层
     */
    public DashboardCostServiceImpl(WorkOrderMapper workOrderMapper,
                                    PreparationMapper preparationMapper,
                                    ProductionPlanMapper productionPlanMapper,
                                    StockOutMapper stockOutMapper,
                                    StockOutDetailMapper stockOutDetailMapper,
                                    ReturnOrderDetailMapper returnOrderDetailMapper,
                                    ReturnOrderMapper returnOrderMapper,
                                    EnergyRecordMapper energyRecordMapper,
                                    PurchaseOrdersMapper purchaseOrdersMapper,
                                    StockInMapper stockInMapper) {
        this.workOrderMapper = workOrderMapper;
        this.preparationMapper = preparationMapper;
        this.productionPlanMapper = productionPlanMapper;
        this.stockOutMapper = stockOutMapper;
        this.stockOutDetailMapper = stockOutDetailMapper;
        this.returnOrderDetailMapper = returnOrderDetailMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.energyRecordMapper = energyRecordMapper;
        this.purchaseOrdersMapper = purchaseOrdersMapper;
        this.stockInMapper = stockInMapper;
    }

    // endregion

    // region 卡片指标
    // ===================================
    // 卡片指标
    // ===================================

    /**
     * 构建成本核算三张卡片
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 成本核算指标
     */
    @Override
    public DashboardMetricsDto.CostMetricsDto getCostMetrics(String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);
        DashboardMetricsDto.CostMetricsDto metrics = new DashboardMetricsDto.CostMetricsDto();
        metrics.setEstimatedCost(buildEstimatedCostCard(data));
        metrics.setActualCost(buildActualCostCard(data));
        metrics.setOutputValue(buildOutputValueCard(data));
        return metrics;
    }

    // endregion

    // region 卡片明细
    // ===================================
    // 卡片明细
    // ===================================

    /**
     * 预估成本明细
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 明细
     */
    @Override
    public CardDetailDto<DashboardMetricsDto.EstimatedCostCardDto, DashboardDetailRowDto>
    getEstimatedCostDetails(String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);
        return new CardDetailDto<>("estimatedCost", buildEstimatedCostCard(data), data.estimatedRows);
    }

    /**
     * 实际成本明细
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 明细
     */
    @Override
    public CardDetailDto<DashboardMetricsDto.ActualCostCardDto, DashboardDetailRowDto>
    getActualCostDetails(String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);
        return new CardDetailDto<>("actualCost", buildActualCostCard(data), data.actualRows);
    }

    /**
     * 预估产值明细
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 明细
     */
    @Override
    public CardDetailDto<DashboardMetricsDto.OutputValueCardDto, DashboardDetailRowDto>
    getOutputValueDetails(String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);
        return new CardDetailDto<>("outputValue", buildOutputValueCard(data), data.outputRows);
    }

    // endregion

    // region 图表数据
    // ===================================
    // 图表数据
    // ===================================

    /**
     * 月度成本趋势
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 月度趋势
     */
    @Override
    public List<ChartDataDto.CostTrendItem> getCostTrend(String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);
        List<ChartDataDto.CostTrendItem> items = new ArrayList<>();
        for (String month : resolveMonths(data, startMonth, endMonth)) {
            ChartDataDto.CostTrendItem item = new ChartDataDto.CostTrendItem();
            BigDecimal estimated = DashboardCalcUtils.money(data.estimatedByMonth.get(month));
            BigDecimal actual = DashboardCalcUtils.money(data.actualByMonth.get(month));
            BigDecimal variance = DashboardCalcUtils.money(estimated.subtract(actual));
            item.setMonth(month);
            item.setOutputValue(DashboardCalcUtils.money(data.outputByMonth.get(month)));
            item.setEstimatedCost(estimated);
            item.setActualCost(actual);
            item.setCostVariance(variance);
            item.setCostVarianceRate(DashboardCalcUtils.percent(variance, estimated));
            items.add(item);
        }
        return items;
    }

    /**
     * 月度实际成本构成
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 月度构成
     */
    @Override
    public List<ChartDataDto.CostStructureItem> getCostStructure(String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);
        List<ChartDataDto.CostStructureItem> items = new ArrayList<>();
        for (String month : resolveMonths(data, startMonth, endMonth)) {
            ChartDataDto.CostStructureItem item = new ChartDataDto.CostStructureItem();
            item.setMonth(month);
            item.setMaterial(DashboardCalcUtils.money(data.materialByMonth.get(month)));
            item.setRawMaterial(DashboardCalcUtils.money(data.rawMaterialByMonth.get(month)));
            item.setAuxiliary(DashboardCalcUtils.money(data.auxiliaryByMonth.get(month)));
            item.setPackaging(DashboardCalcUtils.money(data.packagingByMonth.get(month)));
            item.setWater(DashboardCalcUtils.money(data.waterByMonth.get(month)));
            item.setElectricity(DashboardCalcUtils.money(data.electricityByMonth.get(month)));
            item.setGas(DashboardCalcUtils.money(data.gasByMonth.get(month)));
            item.setOther(DashboardCalcUtils.money(data.otherByMonth.get(month)));
            items.add(item);
        }
        return items;
    }

    /**
     * 批次成本偏差排行
     * <p>
     * 按工单汇总区间内的预估成本与实际成本，偏差 = 预估 − 实际；
     * 预计成本为 0（未维护预计成本价）的批次不参与排名，避免偏差率失真。
     * </p>
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 排行列表（按偏差率降序，最多 10 条）
     */
    @Override
    public List<ChartDataDto.BatchCostVarianceRankItem> getBatchCostVarianceRank(
            String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);

        // 工单号 → 排行聚合（同一工单可能因交付月与成本月不同出现多行，需合并）
        Map<String, ChartDataDto.BatchCostVarianceRankItem> rankMap = new LinkedHashMap<>();
        for (DashboardDetailRowDto row : data.actualRows) {
            if (!StringUtils.hasText(row.getWorkOrderCode())) {
                continue;
            }
            ChartDataDto.BatchCostVarianceRankItem item = rankMap.computeIfAbsent(
                row.getWorkOrderCode(), k -> {
                    ChartDataDto.BatchCostVarianceRankItem created =
                        new ChartDataDto.BatchCostVarianceRankItem();
                    created.setWorkOrderCode(row.getWorkOrderCode());
                    created.setBatchNumber(row.getBatchNumber());
                    created.setPreparationCode(row.getPreparationCode());
                    created.setPreparationName(row.getPreparationName());
                    created.setPlanNumber(row.getPlanNumber());
                    created.setBatchQty(row.getBatchQty());
                    created.setProductionDays(row.getProductionDays());
                    return created;
                });
            item.setActualCost(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getActualCost())
                    .add(DashboardCalcUtils.nz(row.getActualCost()))));
        }

        // 合并预估成本并计算偏差率
        List<ChartDataDto.BatchCostVarianceRankItem> ranks = new ArrayList<>();
        for (Map.Entry<String, ChartDataDto.BatchCostVarianceRankItem> entry : rankMap.entrySet()) {
            ChartDataDto.BatchCostVarianceRankItem item = entry.getValue();
            BigDecimal estimated = DashboardCalcUtils.money(
                data.estimatedCostByWorkOrder.get(entry.getKey()));
            if (estimated.signum() <= 0) {
                continue;
            }
            BigDecimal variance = DashboardCalcUtils.money(estimated.subtract(item.getActualCost()));
            item.setEstimatedCost(estimated);
            item.setCostVariance(variance);
            item.setCostVarianceRate(DashboardCalcUtils.percent(variance, estimated));
            ranks.add(item);
        }

        ranks.sort(Comparator.comparing(ChartDataDto.BatchCostVarianceRankItem::getCostVarianceRate,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return ranks.size() > VARIANCE_RANK_LIMIT
            ? new ArrayList<>(ranks.subList(0, VARIANCE_RANK_LIMIT)) : ranks;
    }

    /**
     * 按品种拆分的实际成本构成
     * <p>
     * 复用实际成本明细行（月份 + 工单），按制剂聚合并保留「未关联制剂」「未分摊水电气」伪品种。
     * 原料 / 辅料 / 包材为按出库分类比例分摊到行的结果，三者之和等于该行物料成本。
     * </p>
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 品种构成列表（按实际成本降序）
     */
    @Override
    public List<ChartDataDto.CostStructureByPreparationItem> getCostStructureByPreparation(
            String startMonth, String endMonth) {
        CostData data = load(startMonth, endMonth);

        Map<String, ChartDataDto.CostStructureByPreparationItem> itemMap = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DashboardDetailRowDto row : data.actualRows) {
            String key = StringUtils.hasText(row.getPreparationCode())
                ? "P|" + row.getPreparationCode()
                : "R|" + (row.getRowType() == null ? "" : row.getRowType());
            ChartDataDto.CostStructureByPreparationItem item = itemMap.computeIfAbsent(key, k -> {
                ChartDataDto.CostStructureByPreparationItem created =
                    new ChartDataDto.CostStructureByPreparationItem();
                created.setPreparationCode(row.getPreparationCode());
                created.setPreparationName(StringUtils.hasText(row.getPreparationName())
                    ? row.getPreparationName() : row.getRowType());
                created.setRowType(row.getRowType());
                return created;
            });
            item.setRawMaterialAmount(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getRawMaterialAmount())
                    .add(DashboardCalcUtils.nz(row.getRawMaterialAmount()))));
            item.setAuxiliaryMaterialAmount(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getAuxiliaryMaterialAmount())
                    .add(DashboardCalcUtils.nz(row.getAuxiliaryMaterialAmount()))));
            item.setPackagingAmount(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getPackagingAmount())
                    .add(DashboardCalcUtils.nz(row.getPackagingAmount()))));
            item.setUtilityAmount(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getUtilityAmount())
                    .add(DashboardCalcUtils.nz(row.getWaterAmount()))
                    .add(DashboardCalcUtils.nz(row.getElectricityAmount()))
                    .add(DashboardCalcUtils.nz(row.getGasAmount()))));
            item.setOtherAmount(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getOtherAmount())
                    .add(DashboardCalcUtils.nz(row.getOtherAmount()))));
            item.setTotalAmount(DashboardCalcUtils.money(
                DashboardCalcUtils.nz(item.getTotalAmount())
                    .add(DashboardCalcUtils.nz(row.getActualCost()))));
            total = total.add(DashboardCalcUtils.nz(row.getActualCost()));
        }

        List<ChartDataDto.CostStructureByPreparationItem> items = new ArrayList<>(itemMap.values());
        for (ChartDataDto.CostStructureByPreparationItem item : items) {
            item.setRatio(DashboardCalcUtils.percent(item.getTotalAmount(), total));
        }
        items.sort(Comparator.comparing(ChartDataDto.CostStructureByPreparationItem::getTotalAmount,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    // endregion

    // region 卡片组装
    // ===================================
    // 卡片组装
    // ===================================

    /**
     * 组装预估成本卡片
     *
     * @param data 成本数据
     * @return 预估成本卡片
     */
    private DashboardMetricsDto.EstimatedCostCardDto buildEstimatedCostCard(CostData data) {
        DashboardMetricsDto.EstimatedCostCardDto card = new DashboardMetricsDto.EstimatedCostCardDto();
        card.setEstimatedCost(DashboardCalcUtils.money(data.estimatedTotal));
        card.setBatchCount(data.inRangeWorkOrders.size());
        card.setBatchQty(DashboardCalcUtils.money(data.batchQtyTotal));
        card.setEstimatedCostPrice(data.batchQtyTotal.signum() == 0 ? null
            : DashboardCalcUtils.money(data.estimatedTotal.divide(data.batchQtyTotal, 2, RoundingMode.HALF_UP)));
        card.setRatioOfOutput(DashboardCalcUtils.percent(data.estimatedTotal, data.outputTotal));
        card.setMomRate(calcMomRate(data.estimatedByMonth));
        card.setMissingPriceBatchCount(data.missingPriceBatchCount);
        card.setMissingPriceBatchQty(DashboardCalcUtils.money(data.missingPriceBatchQty));
        return card;
    }

    /**
     * 组装实际成本卡片
     *
     * @param data 成本数据
     * @return 实际成本卡片
     */
    private DashboardMetricsDto.ActualCostCardDto buildActualCostCard(CostData data) {
        BigDecimal material = DashboardCalcUtils.money(data.materialTotal);
        BigDecimal water = DashboardCalcUtils.money(data.waterTotal);
        BigDecimal electricity = DashboardCalcUtils.money(data.electricityTotal);
        BigDecimal gas = DashboardCalcUtils.money(data.gasTotal);
        BigDecimal other = DashboardCalcUtils.money(data.otherTotal);
        BigDecimal actual = DashboardCalcUtils.money(material.add(water).add(electricity).add(gas).add(other));
        BigDecimal estimated = DashboardCalcUtils.money(data.estimatedTotal);

        DashboardMetricsDto.ActualCostCardDto card = new DashboardMetricsDto.ActualCostCardDto();
        card.setActualCost(actual);
        card.setMaterialAmount(material);
        card.setWaterAmount(water);
        card.setElectricityAmount(electricity);
        card.setGasAmount(gas);
        card.setOtherAmount(other);
        card.setBatchCount(data.inRangeWorkOrders.size());
        card.setRatioOfOutput(DashboardCalcUtils.percent(actual, data.outputTotal));
        BigDecimal variance = DashboardCalcUtils.money(estimated.subtract(actual));
        card.setVarianceToEstimated(variance);
        card.setVarianceRate(DashboardCalcUtils.percent(variance, estimated));
        return card;
    }

    /**
     * 组装预估产值卡片
     *
     * @param data 成本数据
     * @return 预估产值卡片
     */
    private DashboardMetricsDto.OutputValueCardDto buildOutputValueCard(CostData data) {
        BigDecimal estimated = DashboardCalcUtils.money(data.estimatedTotal);
        BigDecimal actual = DashboardCalcUtils.money(data.actualTotal);

        DashboardMetricsDto.OutputValueCardDto card = new DashboardMetricsDto.OutputValueCardDto();
        card.setOutputValue(DashboardCalcUtils.money(data.outputTotal));
        card.setBatchCount(data.inRangeWorkOrders.size());
        card.setBatchQty(DashboardCalcUtils.money(data.batchQtyTotal));
        card.setUnitValue(data.batchQtyTotal.signum() == 0 ? null
            : DashboardCalcUtils.money(data.outputTotal.divide(data.batchQtyTotal, 2, RoundingMode.HALF_UP)));
        card.setMomRate(calcMomRate(data.outputByMonth));
        card.setEstimatedCostRatio(DashboardCalcUtils.percent(estimated, data.outputTotal));
        card.setActualCostRatio(DashboardCalcUtils.percent(actual, data.outputTotal));
        return card;
    }

    /**
     * 计算月度序列的环比：最后一个有数据月份 vs 上一自然月
     * <p>上一自然月无数据（例如不在查询区间内）时返回 null。</p>
     *
     * @param monthly 月度金额
     * @return 环比百分比
     */
    private BigDecimal calcMomRate(Map<String, BigDecimal> monthly) {
        String latest = null;
        for (String month : monthly.keySet()) {
            if (latest == null || month.compareTo(latest) > 0) {
                latest = month;
            }
        }
        if (latest == null) {
            return null;
        }
        return DashboardCalcUtils.momRate(monthly.get(latest), monthly.get(DashboardCalcUtils.previousMonth(latest)));
    }

    // endregion

    // region 核心加载
    // ===================================
    // 核心加载
    // ===================================

    /**
     * 加载并计算成本核算数据
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 成本数据快照
     */
    private CostData load(String startMonth, String endMonth) {
        CostData data = new CostData();

        // === 1. 已交付工单（配置完成时间非空且非作废） ===
        List<WorkOrder> delivered = workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
            .isNotNull("config_complete_time")
            .ne("current_status", STATUS_VOID));
        for (WorkOrder workOrder : delivered) {
            String month = DashboardCalcUtils.monthOf(workOrder.getConfigCompleteTime());
            if (DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                data.inRangeWorkOrders.add(workOrder);
            }
        }
        data.inRangeWorkOrders.sort(Comparator.comparing(WorkOrder::getConfigCompleteTime));

        // === 2. 制剂预计成本价映射 ===
        Map<Long, BigDecimal> priceById = new HashMap<>();
        Map<String, BigDecimal> priceByCode = new HashMap<>();
        for (Preparation preparation : preparationMapper.selectList(null)) {
            priceById.put(preparation.getPreparationId(), preparation.getEstimatedCostPrice());
            if (StringUtils.hasText(preparation.getPreparationCode())) {
                priceByCode.put(preparation.getPreparationCode(), preparation.getEstimatedCostPrice());
            }
        }

        // === 2.1 生产计划映射：ID → 计划（工单表只有 plan_id，计划编号需回查） ===
        for (ProductionPlan plan : productionPlanMapper.selectList(null)) {
            if (plan.getId() != null) {
                data.planById.put(plan.getId().longValue(), plan);
                data.planNumberById.put(plan.getId().longValue(), plan.getPlanNumber());
            }
        }

        // === 2.2 批次生产周期（采购下达 → 成品入库），一次性预加载避免 N+1 ===
        loadBatchCycles(data);

        // === 3. 预估产值 / 预估成本（工单粒度） ===
        for (WorkOrder workOrder : data.inRangeWorkOrders) {
            String month = DashboardCalcUtils.monthOf(workOrder.getConfigCompleteTime());
            BigDecimal batchQty = DashboardCalcUtils.nz(workOrder.getBatchQty());
            BigDecimal output = DashboardCalcUtils.nz(workOrder.getTotalAmount());
            BigDecimal price = resolvePrice(workOrder, priceByCode, priceById);
            BigDecimal estimatedCost;
            if (price == null) {
                estimatedCost = BigDecimal.ZERO;
                data.missingPriceBatchCount++;
                data.missingPriceBatchQty = data.missingPriceBatchQty.add(batchQty);
            } else {
                estimatedCost = price.multiply(batchQty);
            }

            data.outputTotal = data.outputTotal.add(output);
            data.estimatedTotal = data.estimatedTotal.add(estimatedCost);
            data.batchQtyTotal = data.batchQtyTotal.add(batchQty);
            data.outputByMonth.merge(month, output, BigDecimal::add);
            data.estimatedByMonth.merge(month, estimatedCost, BigDecimal::add);

            data.outputRows.add(buildWorkOrderRow(workOrder, data, month, output, null, null));
            data.estimatedRows.add(buildWorkOrderRow(workOrder, data, month, null, price, estimatedCost));

            // 工单粒度预估成本（供批次偏差排行、单位成本偏差复用）
            if (StringUtils.hasText(workOrder.getWorkOrderCode())) {
                data.estimatedCostByWorkOrder.merge(workOrder.getWorkOrderCode(), estimatedCost,
                    BigDecimal::add);
                if (price != null) {
                    data.unitEstimatedByWorkOrder.putIfAbsent(workOrder.getWorkOrderCode(), price);
                }
            }
        }

        // === 4. 物料成本（出库净额，按计划分摊到工单） ===
        loadMaterialCost(data, startMonth, endMonth);

        // === 5. 水电气（按当月已交付工单数均摊） ===
        loadEnergyCost(data, startMonth, endMonth);

        // === 6. 合并实际成本明细（月份 + 工单） ===
        mergeActualRows(data);

        return data;
    }

    /**
     * 解析工单对应的预计成本价
     *
     * @param workOrder  工单
     * @param priceByCode 制剂编码 → 预计成本价
     * @param priceById   制剂ID → 预计成本价
     * @return 预计成本价；未维护返回 null
     */
    private BigDecimal resolvePrice(WorkOrder workOrder,
                                    Map<String, BigDecimal> priceByCode,
                                    Map<Long, BigDecimal> priceById) {
        // 优先使用工单创建时冻结的快照价，保证历史月份不随制剂改价漂移
        if (workOrder.getEstimatedCostPriceSnapshot() != null) {
            return workOrder.getEstimatedCostPriceSnapshot();
        }
        BigDecimal price = null;
        if (StringUtils.hasText(workOrder.getPreparationCode())) {
            price = priceByCode.get(workOrder.getPreparationCode());
        }
        if (price == null && workOrder.getPreparationId() != null) {
            price = priceById.get(workOrder.getPreparationId());
        }
        return price;
    }

    /**
     * 构建工单粒度的明细行
     *
     * @param workOrder     工单
     * @param data          成本数据（用于回填计划编号）
     * @param month         归属月份
     * @param outputValue   预估产值（非产值卡片传 null）
     * @param price         预计成本价（非预估成本卡片传 null）
     * @param estimatedCost 预估成本（非预估成本卡片传 null）
     * @return 明细行
     */
    private DashboardDetailRowDto buildWorkOrderRow(WorkOrder workOrder, CostData data, String month,
                                                    BigDecimal outputValue,
                                                    BigDecimal price,
                                                    BigDecimal estimatedCost) {
        DashboardDetailRowDto row = new DashboardDetailRowDto();
        row.setMonth(month);
        row.setPreparationCode(workOrder.getPreparationCode());
        row.setPreparationName(workOrder.getPreparationName());
        row.setWorkOrderCode(workOrder.getWorkOrderCode());
        row.setPlanNumber(resolvePlanNumber(workOrder.getPlanNumber(), workOrder.getPlanId(), data));
        row.setBatchNumber(workOrder.getBatchNumber());
        row.setBatchQty(DashboardCalcUtils.money(workOrder.getBatchQty()));
        row.setStatus(workOrder.getCurrentStatus());
        row.setTime(formatDateTime(workOrder.getConfigCompleteTime()));
        row.setRowType(ROW_NORMAL);
        // 单位预计成本（快照价）与批次生产周期对两个卡片明细都返回
        row.setUnitEstimatedCost(price == null ? null : DashboardCalcUtils.money(price));
        BatchCycle cycle = data.cycleByWorkOrder.get(workOrder.getWorkOrderCode());
        if (cycle != null) {
            row.setPurchaseDate(formatDateTime(cycle.purchaseTime));
            row.setInboundDate(formatDateTime(cycle.inboundTime));
            row.setProductionDays(cycle.productionDays);
        }
        if (outputValue != null) {
            row.setOutputValue(DashboardCalcUtils.money(outputValue));
            BigDecimal batchQty = DashboardCalcUtils.nz(workOrder.getBatchQty());
            row.setUnitValue(batchQty.signum() == 0 ? null
                : DashboardCalcUtils.money(outputValue.divide(batchQty, 2, RoundingMode.HALF_UP)));
        }
        if (estimatedCost != null) {
            row.setEstimatedCostPrice(price == null ? null : DashboardCalcUtils.money(price));
            row.setEstimatedCost(DashboardCalcUtils.money(estimatedCost));
        }
        return row;
    }

    /**
     * 加载物料成本：出库明细金额 − 退库冲减，按计划批量分摊到工单
     *
     * @param data       成本数据
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     */
    private void loadMaterialCost(CostData data, String startMonth, String endMonth) {
        // 已出库且出库时间在区间内的出库单
        List<StockOut> outs = new ArrayList<>();
        for (StockOut stockOut : stockOutMapper.selectList(new QueryWrapper<StockOut>().eq("out_status", "已出库"))) {
            String month = DashboardCalcUtils.monthOf(stockOut.getOutDate());
            if (DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                outs.add(stockOut);
            }
        }
        if (outs.isEmpty()) {
            return;
        }

        List<Long> outIds = new ArrayList<>();
        Map<Long, StockOut> outById = new HashMap<>();
        for (StockOut stockOut : outs) {
            outIds.add(stockOut.getOutId());
            outById.put(stockOut.getOutId(), stockOut);
        }

        List<StockOutDetail> details = stockOutDetailMapper.selectList(
            new QueryWrapper<StockOutDetail>().in("out_id", outIds));
        List<Long> detailIds = new ArrayList<>();
        Map<Long, BigDecimal> netByDetail = new LinkedHashMap<>();
        Map<Long, StockOutDetail> detailById = new HashMap<>();
        for (StockOutDetail detail : details) {
            if (!MATERIAL_CATEGORIES.contains(detail.getCategoryName())) {
                continue;
            }
            detailIds.add(detail.getOutDetailId());
            detailById.put(detail.getOutDetailId(), detail);
            netByDetail.put(detail.getOutDetailId(), DashboardCalcUtils.nz(detail.getAmount()));
        }

        // 退库冲减：通过 out_detail_id 回溯原出库明细
        if (!detailIds.isEmpty()) {
            List<ReturnOrderDetail> returnDetails = returnOrderDetailMapper.selectList(
                new QueryWrapper<ReturnOrderDetail>().in("out_detail_id", detailIds));
            if (!returnDetails.isEmpty()) {
                Set<Long> returnOrderIds = new HashSet<>();
                for (ReturnOrderDetail returnDetail : returnDetails) {
                    if (returnDetail.getReturnOrderId() != null) {
                        returnOrderIds.add(returnDetail.getReturnOrderId());
                    }
                }
                Set<Long> validOrderIds = new HashSet<>();
                if (!returnOrderIds.isEmpty()) {
                    for (ReturnOrder order : returnOrderMapper.selectBatchIds(returnOrderIds)) {
                        validOrderIds.add(order.getId());
                    }
                }
                for (ReturnOrderDetail returnDetail : returnDetails) {
                    if (returnDetail.getOutDetailId() == null
                        || !validOrderIds.contains(returnDetail.getReturnOrderId())
                        || !netByDetail.containsKey(returnDetail.getOutDetailId())) {
                        continue;
                    }
                    BigDecimal current = netByDetail.get(returnDetail.getOutDetailId());
                    netByDetail.put(returnDetail.getOutDetailId(),
                        current.subtract(DashboardCalcUtils.nz(returnDetail.getAmount())));
                }
            }
        }

        // 计划编号 → 计划ID（出库单可能只带编号），复用 load() 已加载的计划映射
        Map<String, Long> planIdByNumber = new HashMap<>();
        for (Map.Entry<Long, String> entry : data.planNumberById.entrySet()) {
            if (StringUtils.hasText(entry.getValue())) {
                planIdByNumber.put(entry.getValue(), entry.getKey());
            }
        }

        // 计划 → 工单（用于按批量分摊）
        Map<Long, List<WorkOrder>> workOrdersByPlan = new HashMap<>();
        for (WorkOrder workOrder : workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
            .isNotNull("plan_id")
            .ne("current_status", STATUS_VOID))) {
            workOrdersByPlan.computeIfAbsent(workOrder.getPlanId(), k -> new ArrayList<>()).add(workOrder);
        }

        // 按「月份 + 计划」汇总净额；无计划归属的按出库月份单独归集
        Map<String, Map<Long, BigDecimal>> amountByMonthPlan = new LinkedHashMap<>();
        // 「月份 + 计划」→ 分类金额（原料 / 辅料 / 包材），用于把物料成本按分类比例拆分到批次
        Map<String, Map<String, BigDecimal>> categoryByMonthPlan = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : netByDetail.entrySet()) {
            StockOutDetail detail = detailById.get(entry.getKey());
            StockOut stockOut = outById.get(detail.getOutId());
            if (stockOut == null) {
                continue;
            }
            String month = DashboardCalcUtils.monthOf(stockOut.getOutDate());
            BigDecimal amount = entry.getValue();
            if (amount.signum() == 0) {
                continue;
            }
            data.materialTotal = data.materialTotal.add(amount);
            data.materialByMonth.merge(month, amount, BigDecimal::add);
            recordMaterialCategory(data, detail.getCategoryName(), month, amount);

            Long planId = stockOut.getPlanId();
            if (planId == null && StringUtils.hasText(stockOut.getPlanNumber())) {
                planId = planIdByNumber.get(stockOut.getPlanNumber());
            }
            if (planId == null) {
                ActualRow row = data.actualRowMap.computeIfAbsent(unlinkedKey(month),
                    k -> newUnlinkedRow(month, stockOut));
                row.material = row.material.add(amount);
                addCategoryAmount(row, detail.getCategoryName(), amount);
            } else {
                amountByMonthPlan.computeIfAbsent(month, k -> new LinkedHashMap<>())
                    .merge(planId, amount, BigDecimal::add);
                categoryByMonthPlan.computeIfAbsent(categoryPlanKey(month, planId),
                    k -> new HashMap<>()).merge(detail.getCategoryName(), amount, BigDecimal::add);
            }
        }

        // 计划净额按批量分摊到工单
        for (Map.Entry<String, Map<Long, BigDecimal>> monthEntry : amountByMonthPlan.entrySet()) {
            String month = monthEntry.getKey();
            for (Map.Entry<Long, BigDecimal> planEntry : monthEntry.getValue().entrySet()) {
                Long planId = planEntry.getKey();
                BigDecimal amount = planEntry.getValue();
                Map<String, BigDecimal> categoryAmounts = categoryByMonthPlan.getOrDefault(
                    categoryPlanKey(month, planId), new HashMap<>());
                List<WorkOrder> planWorkOrders = workOrdersByPlan.getOrDefault(planId, new ArrayList<>());
                BigDecimal totalQty = BigDecimal.ZERO;
                for (WorkOrder workOrder : planWorkOrders) {
                    totalQty = totalQty.add(DashboardCalcUtils.nz(workOrder.getBatchQty()));
                }
                if (planWorkOrders.isEmpty() || totalQty.signum() <= 0) {
                    ProductionPlan plan = data.planById.get(planId);
                    ActualRow row = data.actualRowMap.computeIfAbsent(unlinkedKey(month),
                        k -> newActualRow(month, null,
                            plan == null ? null : plan.getPreparationName(), ROW_UNLINKED));
                    row.material = row.material.add(amount);
                    for (Map.Entry<String, BigDecimal> category : categoryAmounts.entrySet()) {
                        addCategoryAmount(row, category.getKey(), category.getValue());
                    }
                    continue;
                }
                BigDecimal allocated = BigDecimal.ZERO;
                for (int i = 0; i < planWorkOrders.size(); i++) {
                    WorkOrder workOrder = planWorkOrders.get(i);
                    BigDecimal share;
                    if (i == planWorkOrders.size() - 1) {
                        // 最后一张工单承接尾差，保证分摊合计与计划净额一致
                        share = amount.subtract(allocated);
                    } else {
                        share = amount.multiply(DashboardCalcUtils.nz(workOrder.getBatchQty()))
                            .divide(totalQty, ALLOC_SCALE, RoundingMode.HALF_UP);
                        allocated = allocated.add(share);
                    }
                    ActualRow row = data.actualRowMap.computeIfAbsent(actualKey(month, workOrder.getWorkOrderCode()),
                        k -> newActualRow(month, workOrder, workOrder.getPreparationName(), ROW_NORMAL));
                    row.material = row.material.add(share);
                    splitMaterialShare(row, share, categoryAmounts);
                }
            }
        }
    }

    /**
     * 归集月度物料分类金额（原料 / 辅料 / 包材）
     *
     * @param data         成本数据
     * @param categoryName 物料分类
     * @param month        月份
     * @param amount       净额
     */
    private void recordMaterialCategory(CostData data, String categoryName, String month, BigDecimal amount) {
        if (CATEGORY_RAW.equals(categoryName)) {
            data.rawMaterialTotal = data.rawMaterialTotal.add(amount);
            data.rawMaterialByMonth.merge(month, amount, BigDecimal::add);
        } else if (CATEGORY_AUXILIARY.equals(categoryName)) {
            data.auxiliaryTotal = data.auxiliaryTotal.add(amount);
            data.auxiliaryByMonth.merge(month, amount, BigDecimal::add);
        } else if (CATEGORY_PACKAGING.equals(categoryName)) {
            data.packagingTotal = data.packagingTotal.add(amount);
            data.packagingByMonth.merge(month, amount, BigDecimal::add);
        }
    }

    /**
     * 按分类把金额直接累加到行（未关联计划的出库行）
     *
     * @param row          实际成本行
     * @param categoryName 物料分类
     * @param amount       金额
     */
    private void addCategoryAmount(ActualRow row, String categoryName, BigDecimal amount) {
        if (CATEGORY_RAW.equals(categoryName)) {
            row.rawMaterial = row.rawMaterial.add(amount);
        } else if (CATEGORY_AUXILIARY.equals(categoryName)) {
            row.auxiliaryMaterial = row.auxiliaryMaterial.add(amount);
        } else if (CATEGORY_PACKAGING.equals(categoryName)) {
            row.packaging = row.packaging.add(amount);
        }
    }

    /**
     * 按「月份 + 计划」的分类比例拆分工单物料分摊额
     * <p>尾差计入包材，保证原料 + 辅料 + 包材 = 该行物料成本。</p>
     *
     * @param row             实际成本行
     * @param share           该工单分摊到的物料金额
     * @param categoryAmounts 该「月份 + 计划」的分类金额
     */
    private void splitMaterialShare(ActualRow row, BigDecimal share,
                                    Map<String, BigDecimal> categoryAmounts) {
        BigDecimal raw = DashboardCalcUtils.nz(categoryAmounts.get(CATEGORY_RAW));
        BigDecimal auxiliary = DashboardCalcUtils.nz(categoryAmounts.get(CATEGORY_AUXILIARY));
        BigDecimal packaging = DashboardCalcUtils.nz(categoryAmounts.get(CATEGORY_PACKAGING));
        BigDecimal total = raw.add(auxiliary).add(packaging);
        if (total.signum() <= 0) {
            // 分类缺失时全部计入原料，保证三分类之和等于物料成本
            row.rawMaterial = row.rawMaterial.add(share);
            return;
        }
        BigDecimal rawShare = share.multiply(raw).divide(total, ALLOC_SCALE, RoundingMode.HALF_UP);
        BigDecimal auxiliaryShare = share.multiply(auxiliary)
            .divide(total, ALLOC_SCALE, RoundingMode.HALF_UP);
        BigDecimal packagingShare = share.subtract(rawShare).subtract(auxiliaryShare);
        row.rawMaterial = row.rawMaterial.add(rawShare);
        row.auxiliaryMaterial = row.auxiliaryMaterial.add(auxiliaryShare);
        row.packaging = row.packaging.add(packagingShare);
    }

    /**
     * 分类归集键（月份 + 计划）
     *
     * @param month  月份
     * @param planId 计划ID
     * @return 归集键
     */
    private String categoryPlanKey(String month, Long planId) {
        return month + "|" + planId;
    }

    /**
     * 加载批次生产周期
     * <p>
     * 起点：同一生产计划下最早的采购单创建时间（{@code purchase_orders.created_time}）；
     * 终点：工单成品入库时间，缺失时回退最早的成品入库单日期。
     * 任一端缺失时批次天数返回 null，不参与统计。
     * </p>
     *
     * @param data 成本数据
     */
    private void loadBatchCycles(CostData data) {
        if (data.inRangeWorkOrders.isEmpty()) {
            return;
        }

        Set<Long> planIds = new HashSet<>();
        Set<Long> workOrderIds = new HashSet<>();
        for (WorkOrder workOrder : data.inRangeWorkOrders) {
            if (workOrder.getPlanId() != null) {
                planIds.add(workOrder.getPlanId());
            }
            if (workOrder.getWorkOrderId() != null) {
                workOrderIds.add(workOrder.getWorkOrderId());
            }
        }

        // 计划ID → 最早采购单创建时间
        Map<Long, LocalDateTime> purchaseByPlan = new HashMap<>();
        if (!planIds.isEmpty()) {
            // 采购单可能只填了生产计划编号（production_plan_code），补充编码 → 计划ID 映射
            Map<String, Long> planIdByNumber = new HashMap<>();
            Set<String> planNumbers = new LinkedHashSet<>();
            for (Map.Entry<Long, String> entry : data.planNumberById.entrySet()) {
                if (!StringUtils.hasText(entry.getValue())) {
                    continue;
                }
                planIdByNumber.put(entry.getValue(), entry.getKey());
                if (planIds.contains(entry.getKey())) {
                    planNumbers.add(entry.getValue());
                }
            }
            QueryWrapper<PurchaseOrders> wrapper = new QueryWrapper<>();
            wrapper.and(w -> {
                w.in("production_plan_id", planIds);
                if (!planNumbers.isEmpty()) {
                    w.or().in("production_plan_code", planNumbers);
                }
            });
            List<PurchaseOrders> orders = purchaseOrdersMapper.selectList(wrapper);
            for (PurchaseOrders order : orders) {
                Long planId = order.getProductionPlanId();
                if (planId == null && StringUtils.hasText(order.getProductionPlanCode())) {
                    planId = planIdByNumber.get(order.getProductionPlanCode());
                }
                if (planId == null || !planIds.contains(planId) || order.getCreatedTime() == null) {
                    continue;
                }
                purchaseByPlan.merge(planId, order.getCreatedTime(),
                    (a, b) -> a.isBefore(b) ? a : b);
            }
        }

        // 工单ID → 最早成品入库时间（工单未填入库时间时兜底）
        Map<Long, LocalDateTime> inboundByWorkOrder = new HashMap<>();
        if (!workOrderIds.isEmpty()) {
            List<StockIn> stockIns = stockInMapper.selectList(new QueryWrapper<StockIn>()
                .in("work_order_id", workOrderIds)
                .eq("in_status", "已入库"));
            for (StockIn stockIn : stockIns) {
                if (stockIn.getWorkOrderId() == null || stockIn.getInDate() == null) {
                    continue;
                }
                inboundByWorkOrder.merge(stockIn.getWorkOrderId(), stockIn.getInDate(),
                    (a, b) -> a.isBefore(b) ? a : b);
            }
        }

        for (WorkOrder workOrder : data.inRangeWorkOrders) {
            if (!StringUtils.hasText(workOrder.getWorkOrderCode())) {
                continue;
            }
            BatchCycle cycle = new BatchCycle();
            cycle.purchaseTime = workOrder.getPlanId() == null
                ? null : purchaseByPlan.get(workOrder.getPlanId());
            cycle.inboundTime = workOrder.getInboundTime() != null
                ? workOrder.getInboundTime()
                : (workOrder.getWorkOrderId() == null
                    ? null : inboundByWorkOrder.get(workOrder.getWorkOrderId()));
            cycle.productionDays = calcProductionDays(cycle.purchaseTime, cycle.inboundTime);
            data.cycleByWorkOrder.put(workOrder.getWorkOrderCode(), cycle);
        }
    }

    /**
     * 计算批次生产天数（向上取整）
     *
     * @param purchaseTime 采购下达时间
     * @param inboundTime  成品入库时间
     * @return 天数；任一端缺失或时间倒挂返回 null
     */
    private Long calcProductionDays(LocalDateTime purchaseTime, LocalDateTime inboundTime) {
        if (purchaseTime == null || inboundTime == null || inboundTime.isBefore(purchaseTime)) {
            return null;
        }
        long minutes = Duration.between(purchaseTime, inboundTime).toMinutes();
        return (minutes + 1439) / 1440;
    }

    /**
     * 加载水电气与其他成本，按当月已交付工单数均摊
     *
     * @param data       成本数据
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     */
    private void loadEnergyCost(CostData data, String startMonth, String endMonth) {
        List<EnergyRecord> records = energyRecordMapper.selectList(new QueryWrapper<EnergyRecord>()
            .in("energy_type", ENERGY_WATER, ENERGY_ELECTRICITY, ENERGY_GAS, ENERGY_OTHER));
        if (records.isEmpty()) {
            return;
        }

        // 月份 → 已交付工单
        Map<String, List<WorkOrder>> workOrdersByMonth = new HashMap<>();
        for (WorkOrder workOrder : data.inRangeWorkOrders) {
            String month = DashboardCalcUtils.monthOf(workOrder.getConfigCompleteTime());
            workOrdersByMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(workOrder);
        }

        // 月份 → 能耗金额
        Map<String, BigDecimal> waterByMonth = new HashMap<>();
        Map<String, BigDecimal> electricityByMonth = new HashMap<>();
        Map<String, BigDecimal> gasByMonth = new HashMap<>();
        Map<String, BigDecimal> otherByMonth = new HashMap<>();
        for (EnergyRecord record : records) {
            String month = record.getMonth();
            if (!DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                continue;
            }
            BigDecimal amount = DashboardCalcUtils.nz(record.getTotalAmount());
            if (ENERGY_WATER.equals(record.getEnergyType())) {
                waterByMonth.merge(month, amount, BigDecimal::add);
            } else if (ENERGY_ELECTRICITY.equals(record.getEnergyType())) {
                electricityByMonth.merge(month, amount, BigDecimal::add);
            } else if (ENERGY_GAS.equals(record.getEnergyType())) {
                gasByMonth.merge(month, amount, BigDecimal::add);
            } else if (ENERGY_OTHER.equals(record.getEnergyType())) {
                otherByMonth.merge(month, amount, BigDecimal::add);
            }
        }

        Set<String> months = new LinkedHashSet<>();
        months.addAll(waterByMonth.keySet());
        months.addAll(electricityByMonth.keySet());
        months.addAll(gasByMonth.keySet());
        months.addAll(otherByMonth.keySet());

        for (String month : months) {
            BigDecimal water = DashboardCalcUtils.nz(waterByMonth.get(month));
            BigDecimal electricity = DashboardCalcUtils.nz(electricityByMonth.get(month));
            BigDecimal gas = DashboardCalcUtils.nz(gasByMonth.get(month));
            BigDecimal other = DashboardCalcUtils.nz(otherByMonth.get(month));
            data.waterTotal = data.waterTotal.add(water);
            data.electricityTotal = data.electricityTotal.add(electricity);
            data.gasTotal = data.gasTotal.add(gas);
            data.otherTotal = data.otherTotal.add(other);
            data.waterByMonth.merge(month, water, BigDecimal::add);
            data.electricityByMonth.merge(month, electricity, BigDecimal::add);
            data.gasByMonth.merge(month, gas, BigDecimal::add);
            data.otherByMonth.merge(month, other, BigDecimal::add);

            List<WorkOrder> monthWorkOrders = workOrdersByMonth.getOrDefault(month, new ArrayList<>());
            if (monthWorkOrders.isEmpty()) {
                // 当月无已交付工单，能耗无法分摊到批次
                ActualRow row = data.actualRowMap.computeIfAbsent(unallocatedEnergyKey(month),
                    k -> newActualRow(month, null, null, ROW_UNALLOCATED_ENERGY));
                row.water = row.water.add(water);
                row.electricity = row.electricity.add(electricity);
                row.gas = row.gas.add(gas);
                row.other = row.other.add(other);
                continue;
            }

            int count = monthWorkOrders.size();
            int index = 0;
            BigDecimal waterAllocated = BigDecimal.ZERO;
            BigDecimal electricityAllocated = BigDecimal.ZERO;
            BigDecimal gasAllocated = BigDecimal.ZERO;
            BigDecimal otherAllocated = BigDecimal.ZERO;
            for (WorkOrder workOrder : monthWorkOrders) {
                index++;
                boolean last = index == count;
                BigDecimal waterShare = last ? water.subtract(waterAllocated)
                    : water.divide(BigDecimal.valueOf(count), ALLOC_SCALE, RoundingMode.HALF_UP);
                BigDecimal electricityShare = last ? electricity.subtract(electricityAllocated)
                    : electricity.divide(BigDecimal.valueOf(count), ALLOC_SCALE, RoundingMode.HALF_UP);
                BigDecimal gasShare = last ? gas.subtract(gasAllocated)
                    : gas.divide(BigDecimal.valueOf(count), ALLOC_SCALE, RoundingMode.HALF_UP);
                BigDecimal otherShare = last ? other.subtract(otherAllocated)
                    : other.divide(BigDecimal.valueOf(count), ALLOC_SCALE, RoundingMode.HALF_UP);
                waterAllocated = waterAllocated.add(waterShare);
                electricityAllocated = electricityAllocated.add(electricityShare);
                gasAllocated = gasAllocated.add(gasShare);
                otherAllocated = otherAllocated.add(otherShare);

                ActualRow row = data.actualRowMap.computeIfAbsent(actualKey(month, workOrder.getWorkOrderCode()),
                    k -> newActualRow(month, workOrder, workOrder.getPreparationName(), ROW_NORMAL));
                row.water = row.water.add(waterShare);
                row.electricity = row.electricity.add(electricityShare);
                row.gas = row.gas.add(gasShare);
                row.other = row.other.add(otherShare);
            }
        }
    }

    /**
     * 将实际成本行转换为明细 DTO，并汇总实际成本合计
     *
     * @param data 成本数据
     */
    private void mergeActualRows(CostData data) {
        List<ActualRow> rows = new ArrayList<>(data.actualRowMap.values());
        rows.sort(Comparator.comparing((ActualRow row) -> row.month)
            .thenComparing(row -> row.workOrderCode == null ? "" : row.workOrderCode));

        for (ActualRow actualRow : rows) {
            BigDecimal material = DashboardCalcUtils.money(actualRow.material);
            BigDecimal water = DashboardCalcUtils.money(actualRow.water);
            BigDecimal electricity = DashboardCalcUtils.money(actualRow.electricity);
            BigDecimal gas = DashboardCalcUtils.money(actualRow.gas);
            BigDecimal other = DashboardCalcUtils.money(actualRow.other);
            BigDecimal actual = DashboardCalcUtils.money(
                material.add(water).add(electricity).add(gas).add(other));
            data.actualTotal = data.actualTotal.add(actual);
            data.actualByMonth.merge(actualRow.month, actual, BigDecimal::add);

            DashboardDetailRowDto row = new DashboardDetailRowDto();
            row.setMonth(actualRow.month);
            row.setPreparationCode(actualRow.preparationCode);
            row.setPreparationName(actualRow.preparationName);
            row.setWorkOrderCode(actualRow.workOrderCode);
            row.setPlanNumber(resolvePlanNumber(actualRow.planNumber, actualRow.planId, data));
            row.setBatchNumber(actualRow.batchNumber);
            row.setStatus(actualRow.status);
            row.setTime(actualRow.time);
            row.setRowType(actualRow.rowType);
            row.setBatchQty(actualRow.batchQty);
            row.setMaterialAmount(material);
            row.setRawMaterialAmount(DashboardCalcUtils.money(actualRow.rawMaterial));
            row.setAuxiliaryMaterialAmount(DashboardCalcUtils.money(actualRow.auxiliaryMaterial));
            row.setPackagingAmount(DashboardCalcUtils.money(actualRow.packaging));
            row.setWaterAmount(water);
            row.setElectricityAmount(electricity);
            row.setGasAmount(gas);
            row.setOtherAmount(other);
            row.setActualCost(actual);

            // 单位成本与单位成本偏差（分母为工单批量）
            BigDecimal batchQty = DashboardCalcUtils.nz(actualRow.batchQty);
            BigDecimal unitActual = batchQty.signum() == 0 ? null
                : DashboardCalcUtils.money(actual.divide(batchQty, 2, RoundingMode.HALF_UP));
            row.setUnitActualCost(unitActual);
            BigDecimal unitEstimated = actualRow.workOrderCode == null ? null
                : data.unitEstimatedByWorkOrder.get(actualRow.workOrderCode);
            row.setUnitEstimatedCost(unitEstimated == null ? null
                : DashboardCalcUtils.money(unitEstimated));
            if (unitEstimated != null && unitActual != null) {
                BigDecimal unitVariance = DashboardCalcUtils.money(unitEstimated.subtract(unitActual));
                row.setUnitCostVariance(unitVariance);
                row.setUnitCostVarianceRate(DashboardCalcUtils.percent(unitVariance, unitEstimated));
            }

            // 批次生产周期（采购下达 → 成品入库）
            BatchCycle cycle = actualRow.workOrderCode == null ? null
                : data.cycleByWorkOrder.get(actualRow.workOrderCode);
            if (cycle != null) {
                row.setPurchaseDate(formatDateTime(cycle.purchaseTime));
                row.setInboundDate(formatDateTime(cycle.inboundTime));
                row.setProductionDays(cycle.productionDays);
            }

            data.actualRows.add(row);
        }
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 解析图表月份列表：优先按查询区间展开，否则取数据中实际存在的月份
     *
     * @param data       成本数据
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 月份列表（升序）
     */
    private List<String> resolveMonths(CostData data, String startMonth, String endMonth) {
        List<String> months = DashboardCalcUtils.expandMonths(startMonth, endMonth);
        if (!months.isEmpty()) {
            return months;
        }
        return DashboardCalcUtils.mergeMonths(
            data.outputByMonth.keySet(),
            data.estimatedByMonth.keySet(),
            data.actualByMonth.keySet(),
            data.materialByMonth.keySet());
    }

    /**
     * 创建实际成本行
     *
     * @param month             月份
     * @param workOrder         工单（可为空）
     * @param preparationName   制剂名称
     * @param rowType           行类型
     * @return 实际成本行
     */
    private ActualRow newActualRow(String month, WorkOrder workOrder, String preparationName, String rowType) {
        ActualRow row = new ActualRow();
        row.month = month;
        row.rowType = rowType;
        row.preparationName = preparationName;
        if (workOrder != null) {
            row.workOrderCode = workOrder.getWorkOrderCode();
            row.planId = workOrder.getPlanId();
            row.planNumber = workOrder.getPlanNumber();
            row.preparationCode = workOrder.getPreparationCode();
            row.preparationName = workOrder.getPreparationName();
            row.batchNumber = workOrder.getBatchNumber();
            row.batchQty = DashboardCalcUtils.money(workOrder.getBatchQty());
            row.status = workOrder.getCurrentStatus();
            row.time = formatDateTime(workOrder.getConfigCompleteTime());
        }
        return row;
    }

    /**
     * 创建未关联计划的出库行（展示出库单信息，便于追溯）
     *
     * @param month    月份
     * @param stockOut 出库单
     * @return 实际成本行
     */
    private ActualRow newUnlinkedRow(String month, StockOut stockOut) {
        ActualRow row = newActualRow(month, null, null, ROW_UNLINKED);
        row.planNumber = stockOut.getPlanNumber();
        row.status = stockOut.getOutStatus();
        row.time = formatDateTime(stockOut.getOutDate());
        return row;
    }

    /**
     * 正常行合并键（月份 + 工单号）
     *
     * @param month         月份
     * @param workOrderCode 工单号
     * @return 合并键
     */
    private String actualKey(String month, String workOrderCode) {
        return month + "|W|" + (workOrderCode == null ? "" : workOrderCode);
    }

    /**
     * 未关联制剂行合并键
     *
     * @param month 月份
     * @return 合并键
     */
    private String unlinkedKey(String month) {
        return month + "|U|";
    }

    /**
     * 未分摊水电气行合并键
     *
     * @param month 月份
     * @return 合并键
     */
    private String unallocatedEnergyKey(String month) {
        return month + "|E|";
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
     * 解析计划编号
     * <p>工单表只有 plan_id，plan_number 为非表字段恒为空，需按计划ID回查。</p>
     *
     * @param planNumber 已有计划编号（出库单等场景可直接带入）
     * @param planId     计划ID
     * @param data       成本数据（含计划编号映射）
     * @return 计划编号；无法解析返回 null
     */
    private String resolvePlanNumber(String planNumber, Long planId, CostData data) {
        if (StringUtils.hasText(planNumber)) {
            return planNumber;
        }
        return planId == null ? null : data.planNumberById.get(planId);
    }

    // endregion

    // region 内部数据结构
    // ===================================
    // 内部数据结构
    // ===================================

    /**
     * 成本数据快照
     */
    private static class CostData {

        /** 区间内已交付工单 */
        private final List<WorkOrder> inRangeWorkOrders = new ArrayList<>();

        /** 预估产值明细行 */
        private final List<DashboardDetailRowDto> outputRows = new ArrayList<>();

        /** 预估成本明细行 */
        private final List<DashboardDetailRowDto> estimatedRows = new ArrayList<>();

        /** 实际成本明细行 */
        private final List<DashboardDetailRowDto> actualRows = new ArrayList<>();

        /** 实际成本行合并容器 */
        private final Map<String, ActualRow> actualRowMap = new LinkedHashMap<>();

        /** 月度预估产值 */
        private final Map<String, BigDecimal> outputByMonth = new HashMap<>();

        /** 月度预估成本 */
        private final Map<String, BigDecimal> estimatedByMonth = new HashMap<>();

        /** 月度物料成本 */
        private final Map<String, BigDecimal> materialByMonth = new HashMap<>();

        /** 月度水费 */
        private final Map<String, BigDecimal> waterByMonth = new HashMap<>();

        /** 月度电费 */
        private final Map<String, BigDecimal> electricityByMonth = new HashMap<>();

        /** 月度燃气费 */
        private final Map<String, BigDecimal> gasByMonth = new HashMap<>();

        /** 月度其他成本 */
        private final Map<String, BigDecimal> otherByMonth = new HashMap<>();

        /** 月度原料成本 */
        private final Map<String, BigDecimal> rawMaterialByMonth = new HashMap<>();

        /** 月度辅料成本 */
        private final Map<String, BigDecimal> auxiliaryByMonth = new HashMap<>();

        /** 月度包材成本 */
        private final Map<String, BigDecimal> packagingByMonth = new HashMap<>();

        /** 月度实际成本 */
        private final Map<String, BigDecimal> actualByMonth = new HashMap<>();

        /** 预估产值合计 */
        private BigDecimal outputTotal = BigDecimal.ZERO;

        /** 预估成本合计 */
        private BigDecimal estimatedTotal = BigDecimal.ZERO;

        /** 实际成本合计 */
        private BigDecimal actualTotal = BigDecimal.ZERO;

        /** 物料成本合计 */
        private BigDecimal materialTotal = BigDecimal.ZERO;

        /** 水费合计 */
        private BigDecimal waterTotal = BigDecimal.ZERO;

        /** 电费合计 */
        private BigDecimal electricityTotal = BigDecimal.ZERO;

        /** 燃气费合计 */
        private BigDecimal gasTotal = BigDecimal.ZERO;

        /** 其他成本合计 */
        private BigDecimal otherTotal = BigDecimal.ZERO;

        /** 原料成本合计 */
        private BigDecimal rawMaterialTotal = BigDecimal.ZERO;

        /** 辅料成本合计 */
        private BigDecimal auxiliaryTotal = BigDecimal.ZERO;

        /** 包材成本合计 */
        private BigDecimal packagingTotal = BigDecimal.ZERO;

        /** 批量合计 */
        private BigDecimal batchQtyTotal = BigDecimal.ZERO;

        /** 未维护预计成本价的批次数 */
        private int missingPriceBatchCount;

        /** 未维护预计成本价的批量合计 */
        private BigDecimal missingPriceBatchQty = BigDecimal.ZERO;

        /** 计划ID → 计划编号（工单表无 plan_number 字段，需回填） */
        private final Map<Long, String> planNumberById = new HashMap<>();

        /** 计划ID → 计划（用于未关联工单时展示制剂名称） */
        private final Map<Long, ProductionPlan> planById = new HashMap<>();

        /** 工单号 → 预估成本（元），供批次偏差排行使用 */
        private final Map<String, BigDecimal> estimatedCostByWorkOrder = new HashMap<>();

        /** 工单号 → 单位预计成本（元/件，快照优先） */
        private final Map<String, BigDecimal> unitEstimatedByWorkOrder = new HashMap<>();

        /** 工单号 → 批次生产周期 */
        private final Map<String, BatchCycle> cycleByWorkOrder = new HashMap<>();
    }

    /**
     * 批次生产周期
     */
    private static class BatchCycle {

        /** 采购需求下达时间（同一计划最早采购单创建时间） */
        private LocalDateTime purchaseTime;

        /** 成品入库时间 */
        private LocalDateTime inboundTime;

        /** 生产天数（向上取整），任一端缺失为 null */
        private Long productionDays;
    }

    /**
     * 实际成本合并行
     */
    private static class ActualRow {

        /** 月份 */
        private String month;

        /** 工单号 */
        private String workOrderCode;

        /** 关联计划ID（用于回填计划编号） */
        private Long planId;

        /** 计划单号 */
        private String planNumber;

        /** 制剂编码 */
        private String preparationCode;

        /** 制剂名称 */
        private String preparationName;

        /** 批号 */
        private String batchNumber;

        /** 批量（件） */
        private BigDecimal batchQty;

        /** 工单状态 */
        private String status;

        /** 配置完成时间 */
        private String time;

        /** 行类型 */
        private String rowType;

        /** 物料成本 */
        private BigDecimal material = BigDecimal.ZERO;

        /** 原料成本 */
        private BigDecimal rawMaterial = BigDecimal.ZERO;

        /** 辅料成本 */
        private BigDecimal auxiliaryMaterial = BigDecimal.ZERO;

        /** 包材成本 */
        private BigDecimal packaging = BigDecimal.ZERO;

        /** 水费 */
        private BigDecimal water = BigDecimal.ZERO;

        /** 电费 */
        private BigDecimal electricity = BigDecimal.ZERO;

        /** 燃气费 */
        private BigDecimal gas = BigDecimal.ZERO;

        /** 其他成本 */
        private BigDecimal other = BigDecimal.ZERO;
    }

    // endregion
}

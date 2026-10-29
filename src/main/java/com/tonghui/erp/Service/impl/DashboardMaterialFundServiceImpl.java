package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.Dashboard.ChartDataDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMaterialFundDetailDto;
import com.tonghui.erp.Common.utils.DashboardCalcUtils;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.ReturnOrderDetail;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderDetailMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderMapper;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Service.DashboardMaterialFundService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
 * 首页原辅包资金统计实现
 * <p>
 * 入库、出库净额、库存三项并列统计，不跨项相加；数量单位按分类各自独立。
 * </p>
 */
@Service
public class DashboardMaterialFundServiceImpl implements DashboardMaterialFundService {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 计入统计的物料分类（按展示顺序） */
    private static final List<String> CATEGORIES = List.of("原料", "辅料", "包材");

    /** 入库状态 */
    private static final String IN_STATUS_DONE = "已入库";

    /** 出库状态 */
    private static final String OUT_STATUS_DONE = "已出库";

    /** 类型：入库 */
    private static final String TYPE_INBOUND = "inbound";

    /** 类型：出库 */
    private static final String TYPE_OUTBOUND = "outbound";

    /** 类型：库存 */
    private static final String TYPE_STOCK = "stock";

    /** 日期格式 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 入库单数据访问层 */
    private final StockInMapper stockInMapper;

    /** 入库明细数据访问层 */
    private final StockInDetailMapper stockInDetailMapper;

    /** 出库单数据访问层 */
    private final StockOutMapper stockOutMapper;

    /** 出库明细数据访问层 */
    private final StockOutDetailMapper stockOutDetailMapper;

    /** 退库明细数据访问层 */
    private final ReturnOrderDetailMapper returnOrderDetailMapper;

    /** 退库单数据访问层 */
    private final ReturnOrderMapper returnOrderMapper;

    /** 库存数据访问层 */
    private final StockMapper stockMapper;

    /** 物料档案数据访问层 */
    private final MaterialMapper materialMapper;

    /**
     * 构造函数注入依赖
     *
     * @param stockInMapper           入库单数据访问层
     * @param stockInDetailMapper     入库明细数据访问层
     * @param stockOutMapper          出库单数据访问层
     * @param stockOutDetailMapper    出库明细数据访问层
     * @param returnOrderDetailMapper 退库明细数据访问层
     * @param returnOrderMapper       退库单数据访问层
     * @param stockMapper             库存数据访问层
     * @param materialMapper          物料档案数据访问层
     */
    public DashboardMaterialFundServiceImpl(StockInMapper stockInMapper,
                                            StockInDetailMapper stockInDetailMapper,
                                            StockOutMapper stockOutMapper,
                                            StockOutDetailMapper stockOutDetailMapper,
                                            ReturnOrderDetailMapper returnOrderDetailMapper,
                                            ReturnOrderMapper returnOrderMapper,
                                            StockMapper stockMapper,
                                            MaterialMapper materialMapper) {
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.stockOutMapper = stockOutMapper;
        this.stockOutDetailMapper = stockOutDetailMapper;
        this.returnOrderDetailMapper = returnOrderDetailMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.stockMapper = stockMapper;
        this.materialMapper = materialMapper;
    }

    // endregion

    // region 图表数据
    // ===================================
    // 图表数据
    // ===================================

    /**
     * 原辅包资金三项图表
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 入库 / 出库净额 / 当前库存
     */
    @Override
    public ChartDataDto.MaterialFunds getMaterialFunds(String startMonth, String endMonth) {
        ChartDataDto.MaterialFunds funds = new ChartDataDto.MaterialFunds();
        funds.setInbound(buildInboundSeries(startMonth, endMonth));
        funds.setOutbound(buildOutboundSeries(startMonth, endMonth));
        funds.setStock(buildStockSeries());
        funds.setSnapshotDate(LocalDate.now().format(DATE_FORMATTER));
        return funds;
    }

    /**
     * 构建入库资金序列
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 入库序列
     */
    private ChartDataDto.MaterialFundSeries buildInboundSeries(String startMonth, String endMonth) {
        List<StockIn> stockIns = new ArrayList<>();
        for (StockIn stockIn : stockInMapper.selectList(
            new QueryWrapper<StockIn>().eq("in_status", IN_STATUS_DONE))) {
            String month = DashboardCalcUtils.monthOf(stockIn.getInDate());
            if (DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                stockIns.add(stockIn);
            }
        }

        Map<String, FundAccumulator> byCategory = new LinkedHashMap<>();
        Map<String, Map<String, FundAccumulator>> byMonth = new LinkedHashMap<>();
        if (!stockIns.isEmpty()) {
            Map<Long, StockIn> inById = new HashMap<>();
            List<Long> inIds = new ArrayList<>();
            for (StockIn stockIn : stockIns) {
                inById.put(stockIn.getInId(), stockIn);
                inIds.add(stockIn.getInId());
            }
            for (StockInDetail detail : stockInDetailMapper.selectList(
                new QueryWrapper<StockInDetail>().in("in_id", inIds))) {
                if (!CATEGORIES.contains(detail.getCategoryName())) {
                    continue;
                }
                StockIn stockIn = inById.get(detail.getInId());
                if (stockIn == null) {
                    continue;
                }
                String month = DashboardCalcUtils.monthOf(stockIn.getInDate());
                FundAccumulator categoryAcc = accumulator(byCategory, detail.getCategoryName());
                FundAccumulator monthAcc = accumulator(
                    byMonth.computeIfAbsent(month, k -> new LinkedHashMap<>()), detail.getCategoryName());
                accumulate(categoryAcc, detail.getAmount(), detail.getQuantity(),
                    detail.getBatchNumber(), detail.getUnitName(), null);
                accumulate(monthAcc, detail.getAmount(), detail.getQuantity(),
                    detail.getBatchNumber(), detail.getUnitName(), null);
            }
        }
        return toSeries(byCategory, byMonth, DashboardCalcUtils.expandMonths(startMonth, endMonth));
    }

    /**
     * 构建出库净额资金序列（出库金额 − 退库冲减）
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 出库序列
     */
    private ChartDataDto.MaterialFundSeries buildOutboundSeries(String startMonth, String endMonth) {
        List<StockOut> stockOuts = new ArrayList<>();
        for (StockOut stockOut : stockOutMapper.selectList(
            new QueryWrapper<StockOut>().eq("out_status", OUT_STATUS_DONE))) {
            String month = DashboardCalcUtils.monthOf(stockOut.getOutDate());
            if (DashboardCalcUtils.inRange(month, startMonth, endMonth)) {
                stockOuts.add(stockOut);
            }
        }

        Map<String, FundAccumulator> byCategory = new LinkedHashMap<>();
        Map<String, Map<String, FundAccumulator>> byMonth = new LinkedHashMap<>();
        if (!stockOuts.isEmpty()) {
            Map<Long, StockOut> outById = new HashMap<>();
            List<Long> outIds = new ArrayList<>();
            for (StockOut stockOut : stockOuts) {
                outById.put(stockOut.getOutId(), stockOut);
                outIds.add(stockOut.getOutId());
            }

            List<StockOutDetail> details = stockOutDetailMapper.selectList(
                new QueryWrapper<StockOutDetail>().in("out_id", outIds));
            Map<Long, ReturnAmount> returnByDetail = loadReturnAmounts(details);

            for (StockOutDetail detail : details) {
                if (!CATEGORIES.contains(detail.getCategoryName())) {
                    continue;
                }
                StockOut stockOut = outById.get(detail.getOutId());
                if (stockOut == null) {
                    continue;
                }
                String month = DashboardCalcUtils.monthOf(stockOut.getOutDate());
                ReturnAmount returnAmount = returnByDetail.getOrDefault(detail.getOutDetailId(),
                    new ReturnAmount(BigDecimal.ZERO, BigDecimal.ZERO));

                FundAccumulator categoryAcc = accumulator(byCategory, detail.getCategoryName());
                FundAccumulator monthAcc = accumulator(
                    byMonth.computeIfAbsent(month, k -> new LinkedHashMap<>()), detail.getCategoryName());
                applyOutbound(categoryAcc, detail, returnAmount);
                applyOutbound(monthAcc, detail, returnAmount);
            }
        }
        return toSeries(byCategory, byMonth, DashboardCalcUtils.expandMonths(startMonth, endMonth));
    }

    /**
     * 构建当前库存资金序列（时点快照，忽略月份）
     *
     * @return 库存序列
     */
    private ChartDataDto.MaterialFundSeries buildStockSeries() {
        Map<String, FundAccumulator> byCategory = new LinkedHashMap<>();
        for (Stock stock : stockMapper.selectList(new QueryWrapper<Stock>().gt("quantity", 0))) {
            if (!CATEGORIES.contains(stock.getCategoryName())) {
                continue;
            }
            BigDecimal amount = DashboardCalcUtils.nz(stock.getQuantity())
                .multiply(DashboardCalcUtils.nz(stock.getUnitPrice()));
            accumulate(accumulator(byCategory, stock.getCategoryName()), amount,
                stock.getQuantity(), stock.getBatchNumber(), stock.getUnitName(), null);
        }
        ChartDataDto.MaterialFundSeries series = toSeries(byCategory, new LinkedHashMap<>(), new ArrayList<>());
        series.setMonths(null);
        return series;
    }

    /**
     * 将分类与月度累计转换为资金序列
     *
     * @param byCategory 分类累计
     * @param byMonth    月度累计
     * @param months     月份列表
     * @return 资金序列
     */
    private ChartDataDto.MaterialFundSeries toSeries(Map<String, FundAccumulator> byCategory,
                                                     Map<String, Map<String, FundAccumulator>> byMonth,
                                                     List<String> months) {
        ChartDataDto.MaterialFundSeries series = new ChartDataDto.MaterialFundSeries();
        BigDecimal total = BigDecimal.ZERO;
        for (FundAccumulator acc : byCategory.values()) {
            total = total.add(netAmount(acc));
        }
        total = DashboardCalcUtils.money(total);

        List<ChartDataDto.MaterialFundCategory> categories = new ArrayList<>();
        for (String category : CATEGORIES) {
            FundAccumulator acc = byCategory.get(category);
            categories.add(toCategory(category, acc, total));
        }
        series.setTotalAmount(total);
        series.setCategories(categories);

        // 月份列表：查询区间优先，否则取数据中实际存在的月份
        List<String> monthList = months.isEmpty()
            ? DashboardCalcUtils.mergeMonths(byMonth.keySet()) : months;
        List<ChartDataDto.MaterialFundMonth> monthItems = new ArrayList<>();
        for (String month : monthList) {
            Map<String, FundAccumulator> monthData = byMonth.get(month);
            ChartDataDto.MaterialFundMonth monthItem = new ChartDataDto.MaterialFundMonth();
            monthItem.setMonth(month);
            BigDecimal monthTotal = BigDecimal.ZERO;
            if (monthData != null) {
                for (FundAccumulator acc : monthData.values()) {
                    monthTotal = monthTotal.add(netAmount(acc));
                }
            }
            List<ChartDataDto.MaterialFundCategory> monthCategories = new ArrayList<>();
            for (String category : CATEGORIES) {
                monthCategories.add(toCategory(category,
                    monthData == null ? null : monthData.get(category), monthTotal));
            }
            monthItem.setCategories(monthCategories);
            monthItems.add(monthItem);
        }
        series.setMonths(monthItems);
        return series;
    }

    /**
     * 转换分类汇总
     *
     * @param category 分类
     * @param acc      累计器（可为空）
     * @param total    合计金额
     * @return 分类汇总
     */
    private ChartDataDto.MaterialFundCategory toCategory(String category, FundAccumulator acc, BigDecimal total) {
        ChartDataDto.MaterialFundCategory item = new ChartDataDto.MaterialFundCategory();
        item.setCategory(category);
        BigDecimal net = acc == null ? BigDecimal.ZERO : netAmount(acc);
        item.setAmount(DashboardCalcUtils.money(net));
        item.setQuantity(DashboardCalcUtils.money(acc == null ? BigDecimal.ZERO : acc.quantity));
        item.setUnit(resolveUnit(category, acc));
        item.setRatio(DashboardCalcUtils.percent(net, total));
        item.setReturnAmount(DashboardCalcUtils.money(acc == null ? BigDecimal.ZERO : acc.returnAmount));
        item.setReturnQuantity(DashboardCalcUtils.money(acc == null ? BigDecimal.ZERO : acc.returnQuantity));
        return item;
    }

    // endregion

    // region 物料级下钻
    // ===================================
    // 物料级下钻
    // ===================================

    /**
     * 物料级明细（全量，不分页）
     *
     * @param type     类型：inbound / outbound / stock
     * @param category 分类（可空）
     * @param month    月份（仅 inbound/outbound 生效）
     * @return 物料级明细
     */
    @Override
    public DashboardMaterialFundDetailDto getMaterialFundDetails(String type, String category, String month) {
        String normalizedType = StringUtils.hasText(type) ? type.trim() : TYPE_INBOUND;
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        String normalizedMonth = DashboardCalcUtils.isMonth(month) ? month : null;

        DashboardMaterialFundDetailDto result = new DashboardMaterialFundDetailDto();
        result.setType(normalizedType);
        result.setCategory(normalizedCategory);
        // 库存为时点快照，忽略月份
        result.setMonth(TYPE_STOCK.equals(normalizedType) ? null : normalizedMonth);

        Map<String, MaterialItemAccumulator> items;
        if (TYPE_OUTBOUND.equals(normalizedType)) {
            items = loadOutboundDetails(normalizedCategory, normalizedMonth);
        } else if (TYPE_STOCK.equals(normalizedType)) {
            items = loadStockDetails(normalizedCategory);
            result.setSnapshotDate(LocalDate.now().format(DATE_FORMATTER));
        } else {
            items = loadInboundDetails(normalizedCategory, normalizedMonth);
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (MaterialItemAccumulator acc : items.values()) {
            total = total.add(acc.netAmount());
            totalQuantity = totalQuantity.add(acc.quantity);
        }
        total = DashboardCalcUtils.money(total);
        result.setTotalAmount(total);
        result.setTotalQuantity(DashboardCalcUtils.money(totalQuantity));

        Map<String, String> materialSpecs = loadMaterialSpecs();
        List<DashboardMaterialFundDetailDto.Item> rows = new ArrayList<>();
        for (MaterialItemAccumulator acc : items.values()) {
            DashboardMaterialFundDetailDto.Item item = new DashboardMaterialFundDetailDto.Item();
            item.setCategory(acc.category);
            item.setMaterialCode(acc.materialCode);
            item.setMaterialName(acc.materialName);
            item.setSpec(acc.materialCode == null ? null : materialSpecs.get(acc.materialCode));
            item.setQuantity(DashboardCalcUtils.money(acc.quantity));
            item.setUnit(acc.unit);
            item.setBatchCount(acc.batchNumbers.size());
            item.setAmount(DashboardCalcUtils.money(acc.amount));
            item.setReturnAmount(DashboardCalcUtils.money(acc.returnAmount));
            item.setReturnQuantity(DashboardCalcUtils.money(acc.returnQuantity));
            item.setNetAmount(DashboardCalcUtils.money(acc.netAmount()));
            item.setRatio(DashboardCalcUtils.percent(acc.netAmount(), total));
            item.setStockAgeDays(acc.stockAgeDays);
            rows.add(item);
        }
        rows.sort(Comparator.comparing(DashboardMaterialFundDetailDto.Item::getNetAmount,
            Comparator.nullsLast(Comparator.reverseOrder())));
        result.setItems(rows);
        return result;
    }

    /**
     * 加载入库物料明细
     *
     * @param category 分类（可空）
     * @param month    月份（可空）
     * @return 物料明细累计
     */
    private Map<String, MaterialItemAccumulator> loadInboundDetails(String category, String month) {
        Map<String, MaterialItemAccumulator> items = new LinkedHashMap<>();
        List<StockIn> stockIns = new ArrayList<>();
        for (StockIn stockIn : stockInMapper.selectList(
            new QueryWrapper<StockIn>().eq("in_status", IN_STATUS_DONE))) {
            String inMonth = DashboardCalcUtils.monthOf(stockIn.getInDate());
            if (month != null && !month.equals(inMonth)) {
                continue;
            }
            stockIns.add(stockIn);
        }
        if (stockIns.isEmpty()) {
            return items;
        }
        Map<Long, StockIn> inById = new HashMap<>();
        List<Long> inIds = new ArrayList<>();
        for (StockIn stockIn : stockIns) {
            inById.put(stockIn.getInId(), stockIn);
            inIds.add(stockIn.getInId());
        }
        for (StockInDetail detail : stockInDetailMapper.selectList(
            new QueryWrapper<StockInDetail>().in("in_id", inIds))) {
            if (!CATEGORIES.contains(detail.getCategoryName())
                || (category != null && !category.equals(detail.getCategoryName()))) {
                continue;
            }
            MaterialItemAccumulator acc = itemAccumulator(items, detail.getCategoryName(),
                detail.getItemCode(), detail.getItemName(), detail.getUnitName());
            acc.amount = acc.amount.add(DashboardCalcUtils.nz(detail.getAmount()));
            acc.quantity = acc.quantity.add(DashboardCalcUtils.nz(detail.getQuantity()));
            if (StringUtils.hasText(detail.getBatchNumber())) {
                acc.batchNumbers.add(detail.getBatchNumber());
            }
        }
        return items;
    }

    /**
     * 加载出库物料明细（含退库冲减）
     *
     * @param category 分类（可空）
     * @param month    月份（可空）
     * @return 物料明细累计
     */
    private Map<String, MaterialItemAccumulator> loadOutboundDetails(String category, String month) {
        Map<String, MaterialItemAccumulator> items = new LinkedHashMap<>();
        List<StockOut> stockOuts = new ArrayList<>();
        for (StockOut stockOut : stockOutMapper.selectList(
            new QueryWrapper<StockOut>().eq("out_status", OUT_STATUS_DONE))) {
            String outMonth = DashboardCalcUtils.monthOf(stockOut.getOutDate());
            if (month != null && !month.equals(outMonth)) {
                continue;
            }
            stockOuts.add(stockOut);
        }
        if (stockOuts.isEmpty()) {
            return items;
        }
        List<Long> outIds = new ArrayList<>();
        for (StockOut stockOut : stockOuts) {
            outIds.add(stockOut.getOutId());
        }
        List<StockOutDetail> details = stockOutDetailMapper.selectList(
            new QueryWrapper<StockOutDetail>().in("out_id", outIds));
        Map<Long, ReturnAmount> returnByDetail = loadReturnAmounts(details);

        for (StockOutDetail detail : details) {
            if (!CATEGORIES.contains(detail.getCategoryName())
                || (category != null && !category.equals(detail.getCategoryName()))) {
                continue;
            }
            ReturnAmount returnAmount = returnByDetail.getOrDefault(detail.getOutDetailId(),
                new ReturnAmount(BigDecimal.ZERO, BigDecimal.ZERO));
            MaterialItemAccumulator acc = itemAccumulator(items, detail.getCategoryName(),
                detail.getItemCode(), detail.getItemName(), detail.getUnitName());
            acc.amount = acc.amount.add(DashboardCalcUtils.nz(detail.getAmount()));
            acc.quantity = acc.quantity.add(DashboardCalcUtils.nz(detail.getQuantity()));
            acc.returnAmount = acc.returnAmount.add(returnAmount.amount);
            acc.returnQuantity = acc.returnQuantity.add(returnAmount.quantity);
            if (StringUtils.hasText(detail.getBatchNumber())) {
                acc.batchNumbers.add(detail.getBatchNumber());
            }
        }
        return items;
    }

    /**
     * 加载当前库存物料明细（含库龄）
     *
     * @param category 分类（可空）
     * @return 物料明细累计
     */
    private Map<String, MaterialItemAccumulator> loadStockDetails(String category) {
        Map<String, MaterialItemAccumulator> items = new LinkedHashMap<>();
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>().gt("quantity", 0));

        // 入库单日期映射（用于计算库龄）
        Set<Long> stockInIds = new HashSet<>();
        for (Stock stock : stocks) {
            if (stock.getStockInId() != null) {
                stockInIds.add(stock.getStockInId());
            }
        }
        Map<Long, LocalDate> inDateById = new HashMap<>();
        if (!stockInIds.isEmpty()) {
            for (StockIn stockIn : stockInMapper.selectBatchIds(stockInIds)) {
                if (stockIn.getInDate() != null) {
                    inDateById.put(stockIn.getInId(), stockIn.getInDate().toLocalDate());
                }
            }
        }

        LocalDate today = LocalDate.now();
        for (Stock stock : stocks) {
            if (!CATEGORIES.contains(stock.getCategoryName())
                || (category != null && !category.equals(stock.getCategoryName()))) {
                continue;
            }
            MaterialItemAccumulator acc = itemAccumulator(items, stock.getCategoryName(),
                stock.getItemCode(), stock.getItemName(), stock.getUnitName());
            BigDecimal amount = DashboardCalcUtils.nz(stock.getQuantity())
                .multiply(DashboardCalcUtils.nz(stock.getUnitPrice()));
            acc.amount = acc.amount.add(amount);
            acc.quantity = acc.quantity.add(DashboardCalcUtils.nz(stock.getQuantity()));
            if (StringUtils.hasText(stock.getBatchNumber())) {
                acc.batchNumbers.add(stock.getBatchNumber());
            }
            LocalDate inDate = stock.getStockInId() == null ? null : inDateById.get(stock.getStockInId());
            if (inDate != null) {
                long age = Math.max(ChronoUnit.DAYS.between(inDate, today), 0L);
                // 同一物料多个批次时取最老批次（库龄最大）
                if (acc.stockAgeDays == null || age > acc.stockAgeDays) {
                    acc.stockAgeDays = age;
                }
            }
        }
        return items;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 查询出库明细对应的退库冲减金额与数量
     *
     * @param details 出库明细
     * @return 出库明细ID → 退库冲减
     */
    private Map<Long, ReturnAmount> loadReturnAmounts(List<StockOutDetail> details) {
        Map<Long, ReturnAmount> result = new HashMap<>();
        List<Long> detailIds = new ArrayList<>();
        for (StockOutDetail detail : details) {
            if (CATEGORIES.contains(detail.getCategoryName()) && detail.getOutDetailId() != null) {
                detailIds.add(detail.getOutDetailId());
            }
        }
        if (detailIds.isEmpty()) {
            return result;
        }
        List<ReturnOrderDetail> returnDetails = returnOrderDetailMapper.selectList(
            new QueryWrapper<ReturnOrderDetail>().in("out_detail_id", detailIds));
        if (returnDetails.isEmpty()) {
            return result;
        }
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
                || !validOrderIds.contains(returnDetail.getReturnOrderId())) {
                continue;
            }
            ReturnAmount current = result.computeIfAbsent(returnDetail.getOutDetailId(),
                k -> new ReturnAmount(BigDecimal.ZERO, BigDecimal.ZERO));
            current.amount = current.amount.add(DashboardCalcUtils.nz(returnDetail.getAmount()));
            current.quantity = current.quantity.add(DashboardCalcUtils.nz(returnDetail.getReturnQuantity()));
        }
        return result;
    }

    /**
     * 获取分类累计器
     *
     * @param map      累计器集合
     * @param category 分类
     * @return 累计器
     */
    private FundAccumulator accumulator(Map<String, FundAccumulator> map, String category) {
        return map.computeIfAbsent(category, k -> {
            FundAccumulator acc = new FundAccumulator();
            acc.category = category;
            return acc;
        });
    }

    /**
     * 获取物料级累计器
     *
     * @param map          累计器集合
     * @param category     分类
     * @param materialCode 物料编码
     * @param materialName 物料名称
     * @param unit         单位
     * @return 累计器
     */
    private MaterialItemAccumulator itemAccumulator(Map<String, MaterialItemAccumulator> map,
                                                    String category,
                                                    String materialCode,
                                                    String materialName,
                                                    String unit) {
        String key = (category == null ? "" : category) + "|"
            + (materialCode == null ? "" : materialCode) + "|"
            + (materialName == null ? "" : materialName);
        return map.computeIfAbsent(key, k -> {
            MaterialItemAccumulator acc = new MaterialItemAccumulator();
            acc.category = category;
            acc.materialCode = materialCode;
            acc.materialName = materialName;
            acc.unit = StringUtils.hasText(unit) ? unit : defaultUnit(category);
            return acc;
        });
    }

    /**
     * 累计入库/库存金额与数量
     *
     * @param acc      累计器
     * @param amount   金额
     * @param quantity 数量
     * @param batchNo  批号
     * @param unit     单位
     * @param inDate   入库日期
     */
    private void accumulate(FundAccumulator acc, BigDecimal amount, BigDecimal quantity,
                            String batchNo, String unit, LocalDate inDate) {
        acc.amount = acc.amount.add(DashboardCalcUtils.nz(amount));
        acc.quantity = acc.quantity.add(DashboardCalcUtils.nz(quantity));
        if (!StringUtils.hasText(acc.unit) && StringUtils.hasText(unit)) {
            acc.unit = unit;
        }
        if (StringUtils.hasText(batchNo)) {
            acc.batchNumbers.add(batchNo);
        }
        if (inDate != null && (acc.oldestInDate == null || inDate.isBefore(acc.oldestInDate))) {
            acc.oldestInDate = inDate;
        }
    }

    /**
     * 累计出库金额（扣除退库冲减）
     *
     * @param acc          累计器
     * @param detail       出库明细
     * @param returnAmount 退库冲减
     */
    private void applyOutbound(FundAccumulator acc, StockOutDetail detail, ReturnAmount returnAmount) {
        acc.amount = acc.amount.add(DashboardCalcUtils.nz(detail.getAmount())
            .subtract(returnAmount.amount));
        acc.quantity = acc.quantity.add(DashboardCalcUtils.nz(detail.getQuantity())
            .subtract(returnAmount.quantity));
        acc.returnAmount = acc.returnAmount.add(returnAmount.amount);
        acc.returnQuantity = acc.returnQuantity.add(returnAmount.quantity);
        if (!StringUtils.hasText(acc.unit) && StringUtils.hasText(detail.getUnitName())) {
            acc.unit = detail.getUnitName();
        }
        if (StringUtils.hasText(detail.getBatchNumber())) {
            acc.batchNumbers.add(detail.getBatchNumber());
        }
    }

    /**
     * 计算累计器净金额（出库已内含退库冲减）
     *
     * @param acc 累计器
     * @return 净金额
     */
    private BigDecimal netAmount(FundAccumulator acc) {
        return acc.amount;
    }

    /**
     * 解析分类单位
     *
     * @param category 分类
     * @param acc      累计器
     * @return 单位
     */
    private String resolveUnit(String category, FundAccumulator acc) {
        if (acc != null && StringUtils.hasText(acc.unit)) {
            return acc.unit;
        }
        return defaultUnit(category);
    }

    /**
     * 分类默认单位
     *
     * @param category 分类
     * @return 默认单位
     */
    private String defaultUnit(String category) {
        if ("包材".equals(category)) {
            return "个";
        }
        return "kg";
    }

    /**
     * 加载物料规格映射
     *
     * @return 物料编码 → 规格
     */
    private Map<String, String> loadMaterialSpecs() {
        Map<String, String> specs = new HashMap<>();
        for (Material material : materialMapper.selectList(null)) {
            if (StringUtils.hasText(material.getMaterialCode())) {
                specs.put(material.getMaterialCode(), material.getSpec());
            }
        }
        return specs;
    }

    // endregion

    // region 内部数据结构
    // ===================================
    // 内部数据结构
    // ===================================

    /**
     * 分类累计器
     */
    private static class FundAccumulator {

        /** 分类 */
        private String category;

        /** 金额（出库为净额） */
        private BigDecimal amount = BigDecimal.ZERO;

        /** 数量（出库为净数量） */
        private BigDecimal quantity = BigDecimal.ZERO;

        /** 退库冲减金额 */
        private BigDecimal returnAmount = BigDecimal.ZERO;

        /** 退库冲减数量 */
        private BigDecimal returnQuantity = BigDecimal.ZERO;

        /** 单位 */
        private String unit;

        /** 涉及批号 */
        private final Set<String> batchNumbers = new LinkedHashSet<>();

        /** 最老入库日期 */
        private LocalDate oldestInDate;
    }

    /**
     * 物料级累计器
     */
    private static class MaterialItemAccumulator {

        /** 分类 */
        private String category;

        /** 物料编码 */
        private String materialCode;

        /** 物料名称 */
        private String materialName;

        /** 单位 */
        private String unit;

        /** 金额（出库为出库金额） */
        private BigDecimal amount = BigDecimal.ZERO;

        /** 数量（出库为出库数量） */
        private BigDecimal quantity = BigDecimal.ZERO;

        /** 退库冲减金额 */
        private BigDecimal returnAmount = BigDecimal.ZERO;

        /** 退库冲减数量 */
        private BigDecimal returnQuantity = BigDecimal.ZERO;

        /** 涉及批号 */
        private final Set<String> batchNumbers = new LinkedHashSet<>();

        /** 库龄（天），仅库存 */
        private Long stockAgeDays;

        /**
         * 净金额
         *
         * @return 出库为金额 − 退库冲减；其他类型等于金额
         */
        private BigDecimal netAmount() {
            return amount.subtract(returnAmount);
        }
    }

    /**
     * 退库冲减金额与数量
     */
    private static class ReturnAmount {

        /** 冲减金额 */
        private BigDecimal amount;

        /** 冲减数量 */
        private BigDecimal quantity;

        /**
         * 构造方法
         *
         * @param amount   冲减金额
         * @param quantity 冲减数量
         */
        private ReturnAmount(BigDecimal amount, BigDecimal quantity) {
            this.amount = amount;
            this.quantity = quantity;
        }
    }

    // endregion
}

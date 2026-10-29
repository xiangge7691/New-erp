package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Common.Dto.Dashboard.CardDetailDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardDetailRowDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMetricsDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardTreeDto;
import com.tonghui.erp.Common.utils.DashboardCalcUtils;
import com.tonghui.erp.Service.DashboardCostService;
import com.tonghui.erp.Service.DashboardTreeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 首页明细下钻实现（月份 → 制剂 → 批次）
 * <p>
 * 不重新查库，直接复用 {@link DashboardCostService} 的三份卡片明细：
 * 预估产值明细（交付月口径）、预估成本明细、实际成本明细（成本发生月口径），
 * 再按「月份 → 制剂 → 批次（工单）」聚合成树。
 * </p>
 * <p>
 * 归集口径：
 * <ul>
 *   <li>预估产值、预估成本、交付量：来自交付工单，按 {@code config_complete_time} 归月</li>
 *   <li>物料成本、水电气：按出库 / 能耗发生月份归月，可能与交付月不同</li>
 *   <li>「未关联制剂」「未分摊水电气」作为伪制剂节点挂在对应月份下，保证月合计不漏金额</li>
 * </ul>
 * </p>
 */
@Service
public class DashboardTreeServiceImpl implements DashboardTreeService {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 正常行类型 */
    private static final String ROW_NORMAL = "正常";

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 成本核算服务（提供三份卡片明细） */
    private final DashboardCostService dashboardCostService;

    /**
     * 构造函数注入依赖
     *
     * @param dashboardCostService 成本核算服务
     */
    public DashboardTreeServiceImpl(DashboardCostService dashboardCostService) {
        this.dashboardCostService = dashboardCostService;
    }

    // endregion

    // region 入口
    // ===================================
    // 入口
    // ===================================

    /**
     * 成本与生产明细树
     *
     * @param startMonth 起始月份（yyyy-MM，可空）
     * @param endMonth   结束月份（yyyy-MM，可空）
     * @return 明细树
     */
    @Override
    public DashboardTreeDto getDetailTree(String startMonth, String endMonth) {
        List<DashboardDetailRowDto> outputRows = detailsOf(
            dashboardCostService.getOutputValueDetails(startMonth, endMonth));
        List<DashboardDetailRowDto> estimatedRows = detailsOf(
            dashboardCostService.getEstimatedCostDetails(startMonth, endMonth));
        List<DashboardDetailRowDto> actualRows = detailsOf(
            dashboardCostService.getActualCostDetails(startMonth, endMonth));

        // 月份 → 批次（工单）节点；使用 TreeMap 保证月份升序
        Map<String, Map<String, DashboardTreeDto.Batch>> batchesByMonth = new LinkedHashMap<>();
        // 批次所属制剂信息：月份|批次键 → [制剂编码, 制剂名称]
        Map<String, String[]> preparationByBatch = new HashMap<>();
        // 交付量（仅交付月口径）：月份 → 件数、月份|制剂键 → 件数
        Map<String, BigDecimal> deliveryByMonth = new HashMap<>();
        Map<String, BigDecimal> deliveryByPreparation = new HashMap<>();

        // 1. 预估产值明细（交付工单，产值 + 批量 + 状态）
        for (DashboardDetailRowDto row : outputRows) {
            DashboardTreeDto.Batch batch = batchNode(batchesByMonth, row);
            mergeCommon(batch, row);
            batch.setOutputValue(DashboardCalcUtils.money(row.getOutputValue()));
            rememberPreparation(preparationByBatch, row);
            BigDecimal qty = DashboardCalcUtils.nz(row.getBatchQty());
            deliveryByMonth.merge(row.getMonth(), qty, BigDecimal::add);
            deliveryByPreparation.merge(row.getMonth() + "|" + preparationKey(row.getPreparationCode(),
                row.getPreparationName(), row.getRowType()), qty, BigDecimal::add);
        }

        // 2. 预估成本明细（交付工单，预计成本价 × 批量）
        for (DashboardDetailRowDto row : estimatedRows) {
            DashboardTreeDto.Batch batch = batchNode(batchesByMonth, row);
            mergeCommon(batch, row);
            batch.setEstimatedCost(DashboardCalcUtils.money(row.getEstimatedCost()));
            rememberPreparation(preparationByBatch, row);
        }

        // 3. 实际成本明细（成本发生月，物料 + 水电气；含两类特殊行）
        for (DashboardDetailRowDto row : actualRows) {
            DashboardTreeDto.Batch batch = batchNode(batchesByMonth, row);
            mergeCommon(batch, row);
            mergeActual(batch, row);
            rememberPreparation(preparationByBatch, row);
        }

        // 4. 组装月份 → 制剂 → 批次
        DashboardTreeDto tree = new DashboardTreeDto();
        tree.setStartMonth(startMonth);
        tree.setEndMonth(endMonth);
        tree.setMonths(buildMonths(batchesByMonth, preparationByBatch, deliveryByMonth,
            deliveryByPreparation));
        tree.setSummary(buildSummary(tree.getMonths()));
        return tree;
    }

    // endregion

    // region 树组装
    // ===================================
    // 树组装
    // ===================================

    /**
     * 组装月份节点列表
     *
     * @param batchesByMonth         月份 → 批次键 → 批次
     * @param preparationByBatch     批次所属制剂信息
     * @param deliveryByMonth        月份 → 交付量
     * @param deliveryByPreparation  月份|制剂键 → 交付量
     * @return 月份节点列表（升序）
     */
    private List<DashboardTreeDto.Month> buildMonths(
            Map<String, Map<String, DashboardTreeDto.Batch>> batchesByMonth,
            Map<String, String[]> preparationByBatch,
            Map<String, BigDecimal> deliveryByMonth,
            Map<String, BigDecimal> deliveryByPreparation) {

        List<String> monthKeys = new ArrayList<>(batchesByMonth.keySet());
        monthKeys.sort(Comparator.naturalOrder());

        List<DashboardTreeDto.Month> months = new ArrayList<>();
        for (String month : monthKeys) {
            DashboardTreeDto.Month monthNode = new DashboardTreeDto.Month();
            monthNode.setMonth(month);
            monthNode.setDeliveryQuantity(DashboardCalcUtils.money(
                deliveryByMonth.getOrDefault(month, BigDecimal.ZERO)));
            monthNode.setPreparations(buildPreparations(month, batchesByMonth.get(month),
                preparationByBatch, deliveryByPreparation));

            // 月合计由制剂节点汇总，保证层级间自洽
            BigDecimal output = BigDecimal.ZERO;
            BigDecimal estimated = BigDecimal.ZERO;
            BigDecimal actual = BigDecimal.ZERO;
            for (DashboardTreeDto.Preparation preparation : monthNode.getPreparations()) {
                output = output.add(DashboardCalcUtils.nz(preparation.getOutputValue()));
                estimated = estimated.add(DashboardCalcUtils.nz(preparation.getEstimatedCost()));
                actual = actual.add(DashboardCalcUtils.nz(preparation.getActualCost()));
            }
            monthNode.setOutputValue(DashboardCalcUtils.money(output));
            monthNode.setEstimatedCost(DashboardCalcUtils.money(estimated));
            monthNode.setActualCost(DashboardCalcUtils.money(actual));
            months.add(monthNode);
        }
        return months;
    }

    /**
     * 组装某月份的制剂节点列表
     *
     * @param month                  月份
     * @param batches                该月份的批次节点
     * @param preparationByBatch     批次所属制剂信息
     * @param deliveryByPreparation  月份|制剂键 → 交付量
     * @return 制剂节点列表（正常制剂在前，按预估产值降序）
     */
    private List<DashboardTreeDto.Preparation> buildPreparations(
            String month,
            Map<String, DashboardTreeDto.Batch> batches,
            Map<String, String[]> preparationByBatch,
            Map<String, BigDecimal> deliveryByPreparation) {

        Map<String, DashboardTreeDto.Preparation> preparationMap = new LinkedHashMap<>();

        List<DashboardTreeDto.Batch> batchList = batches == null
            ? new ArrayList<>() : new ArrayList<>(batches.values());
        batchList.sort(Comparator.comparing((DashboardTreeDto.Batch batch) ->
                batch.getTime() == null ? "" : batch.getTime())
            .thenComparing(batch -> batch.getBatchNumber() == null ? "" : batch.getBatchNumber()));

        for (DashboardTreeDto.Batch batch : batchList) {
            String[] preparationInfo = preparationByBatch.get(batchKey(month, batch));
            String preparationCode = preparationInfo == null ? null : preparationInfo[0];
            String preparationName = preparationInfo == null ? null : preparationInfo[1];
            String key = preparationKey(preparationCode, preparationName, batch.getRowType());

            DashboardTreeDto.Preparation preparation = preparationMap.computeIfAbsent(key, k -> {
                DashboardTreeDto.Preparation node = new DashboardTreeDto.Preparation();
                node.setBatches(new ArrayList<>());
                node.setRowType(batch.getRowType());
                // 伪节点（未关联制剂 / 未分摊水电气）没有制剂名称，直接用行类型作为展示名
                node.setPreparationName(preparationName != null ? preparationName
                    : (ROW_NORMAL.equals(batch.getRowType()) ? null : batch.getRowType()));
                return node;
            });
            if (preparation.getPreparationCode() == null) {
                preparation.setPreparationCode(preparationCode);
            }
            if (preparation.getPreparationName() == null) {
                preparation.setPreparationName(preparationName);
            }
            preparation.getBatches().add(batch);
        }

        // 汇总制剂级数值
        List<DashboardTreeDto.Preparation> preparations = new ArrayList<>(preparationMap.values());
        for (DashboardTreeDto.Preparation preparation : preparations) {
            BigDecimal output = BigDecimal.ZERO;
            BigDecimal estimated = BigDecimal.ZERO;
            BigDecimal actual = BigDecimal.ZERO;
            for (DashboardTreeDto.Batch batch : preparation.getBatches()) {
                output = output.add(DashboardCalcUtils.nz(batch.getOutputValue()));
                estimated = estimated.add(DashboardCalcUtils.nz(batch.getEstimatedCost()));
                actual = actual.add(DashboardCalcUtils.nz(batch.getActualCost()));
            }
            preparation.setOutputValue(DashboardCalcUtils.money(output));
            preparation.setEstimatedCost(DashboardCalcUtils.money(estimated));
            preparation.setActualCost(DashboardCalcUtils.money(actual));
            preparation.setDeliveryQuantity(DashboardCalcUtils.money(
                deliveryByPreparation.getOrDefault(
                    month + "|" + preparationKey(preparation.getPreparationCode(),
                        preparation.getPreparationName(), preparation.getRowType()),
                    BigDecimal.ZERO)));
        }

        // 正常制剂按预估产值降序，伪节点（未关联制剂 / 未分摊水电气）排最后
        preparations.sort(Comparator
            .comparing((DashboardTreeDto.Preparation preparation) ->
                ROW_NORMAL.equals(preparation.getRowType()) ? 0 : 1)
            .thenComparing(preparation ->
                DashboardCalcUtils.nz(preparation.getOutputValue()).negate()));
        return preparations;
    }

    /**
     * 组装区间合计
     *
     * @param months 月份节点列表
     * @return 区间合计
     */
    private DashboardTreeDto.Summary buildSummary(List<DashboardTreeDto.Month> months) {
        DashboardTreeDto.Summary summary = new DashboardTreeDto.Summary();
        BigDecimal output = BigDecimal.ZERO;
        BigDecimal estimated = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        BigDecimal delivery = BigDecimal.ZERO;
        Set<String> preparationCodes = new TreeSet<>();
        Set<String> workOrderCodes = new TreeSet<>();

        for (DashboardTreeDto.Month month : months) {
            output = output.add(DashboardCalcUtils.nz(month.getOutputValue()));
            estimated = estimated.add(DashboardCalcUtils.nz(month.getEstimatedCost()));
            actual = actual.add(DashboardCalcUtils.nz(month.getActualCost()));
            delivery = delivery.add(DashboardCalcUtils.nz(month.getDeliveryQuantity()));
            for (DashboardTreeDto.Preparation preparation : month.getPreparations()) {
                if (StringUtils.hasText(preparation.getPreparationCode())) {
                    preparationCodes.add(preparation.getPreparationCode());
                }
                for (DashboardTreeDto.Batch batch : preparation.getBatches()) {
                    if (StringUtils.hasText(batch.getWorkOrderCode())) {
                        workOrderCodes.add(batch.getWorkOrderCode());
                    }
                }
            }
        }

        summary.setMonthCount(months.size());
        summary.setPreparationCount(preparationCodes.size());
        summary.setBatchCount(workOrderCodes.size());
        summary.setOutputValue(DashboardCalcUtils.money(output));
        summary.setEstimatedCost(DashboardCalcUtils.money(estimated));
        summary.setActualCost(DashboardCalcUtils.money(actual));
        summary.setDeliveryQuantity(DashboardCalcUtils.money(delivery));
        return summary;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 取明细列表（空安全）
     *
     * @param detail 卡片明细
     * @param <S>    汇总值类型
     * @return 明细列表，无数据返回空列表
     */
    private <S> List<DashboardDetailRowDto> detailsOf(CardDetailDto<S, DashboardDetailRowDto> detail) {
        if (detail == null || detail.getDetails() == null) {
            return new ArrayList<>();
        }
        return detail.getDetails();
    }

    /**
     * 取或创建批次节点
     *
     * @param batchesByMonth 月份 → 批次键 → 批次
     * @param row            明细行
     * @return 批次节点
     */
    private DashboardTreeDto.Batch batchNode(
            Map<String, Map<String, DashboardTreeDto.Batch>> batchesByMonth,
            DashboardDetailRowDto row) {
        Map<String, DashboardTreeDto.Batch> batches = batchesByMonth.computeIfAbsent(
            row.getMonth(), k -> new LinkedHashMap<>());
        return batches.computeIfAbsent(batchKey(row), k -> {
            DashboardTreeDto.Batch batch = new DashboardTreeDto.Batch();
            batch.setRowType(row.getRowType());
            return batch;
        });
    }

    /**
     * 批次合并键：正常行按工单号，特殊行按行类型（同一月份内至多一条）
     *
     * @param row 明细行
     * @return 合并键
     */
    private String batchKey(DashboardDetailRowDto row) {
        if (StringUtils.hasText(row.getWorkOrderCode())) {
            return "W|" + row.getWorkOrderCode();
        }
        return "R|" + (row.getRowType() == null ? "" : row.getRowType());
    }

    /**
     * 批次合并键（基于已装配的批次节点）
     *
     * @param month 月份
     * @param batch 批次节点
     * @return 合并键
     */
    private String batchKey(String month, DashboardTreeDto.Batch batch) {
        if (StringUtils.hasText(batch.getWorkOrderCode())) {
            return month + "|W|" + batch.getWorkOrderCode();
        }
        return month + "|R|" + (batch.getRowType() == null ? "" : batch.getRowType());
    }

    /**
     * 记录批次所属制剂（先到先得，避免成本月行覆盖交付月行）
     *
     * @param preparationByBatch 批次所属制剂信息
     * @param row                明细行
     */
    private void rememberPreparation(Map<String, String[]> preparationByBatch,
                                     DashboardDetailRowDto row) {
        preparationByBatch.putIfAbsent(row.getMonth() + "|" + batchKey(row),
            new String[] {row.getPreparationCode(), row.getPreparationName()});
    }

    /**
     * 合并批次通用字段（仅在目标为空时填充）
     *
     * @param batch 批次节点
     * @param row   明细行
     */
    private void mergeCommon(DashboardTreeDto.Batch batch, DashboardDetailRowDto row) {
        if (batch.getWorkOrderCode() == null) {
            batch.setWorkOrderCode(row.getWorkOrderCode());
        }
        if (batch.getBatchNumber() == null) {
            batch.setBatchNumber(row.getBatchNumber());
        }
        if (batch.getPlanNumber() == null) {
            batch.setPlanNumber(row.getPlanNumber());
        }
        if (batch.getStatus() == null) {
            batch.setStatus(row.getStatus());
        }
        if (batch.getTime() == null) {
            batch.setTime(row.getTime());
        }
        if (batch.getBatchQty() == null) {
            batch.setBatchQty(row.getBatchQty());
        }
        if (batch.getRowType() == null) {
            batch.setRowType(row.getRowType());
        }
    }

    /**
     * 合并实际成本字段（同批次多行时累加）
     *
     * @param batch 批次节点
     * @param row   实际成本明细行
     */
    private void mergeActual(DashboardTreeDto.Batch batch, DashboardDetailRowDto row) {
        BigDecimal material = DashboardCalcUtils.nz(row.getMaterialAmount());
        BigDecimal utility = DashboardCalcUtils.nz(row.getWaterAmount())
            .add(DashboardCalcUtils.nz(row.getElectricityAmount()))
            .add(DashboardCalcUtils.nz(row.getGasAmount()));

        batch.setMaterialAmount(DashboardCalcUtils.money(
            DashboardCalcUtils.nz(batch.getMaterialAmount()).add(material)));
        batch.setUtilityAmount(DashboardCalcUtils.money(
            DashboardCalcUtils.nz(batch.getUtilityAmount()).add(utility)));
        batch.setActualCost(DashboardCalcUtils.money(
            DashboardCalcUtils.nz(batch.getActualCost()).add(DashboardCalcUtils.nz(row.getActualCost()))));
    }

    /**
     * 制剂分组键
     *
     * @param preparationCode 制剂编码
     * @param preparationName 制剂名称
     * @param rowType         行类型
     * @return 分组键
     */
    private String preparationKey(String preparationCode, String preparationName, String rowType) {
        if (StringUtils.hasText(preparationCode)) {
            return "P|" + preparationCode;
        }
        if (rowType != null && !ROW_NORMAL.equals(rowType)) {
            return "R|" + rowType;
        }
        return "N|" + (preparationName == null ? "" : preparationName);
    }

    // endregion
}

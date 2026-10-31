package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningDto;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningSummaryDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成品库存预警服务测试
 * <p>
 * 只读测试库数据，校验预警列表、汇总统计及核心公式：
 * 可支撑时间 = 可用库存 ÷ 月平均出库量，预警 = 可支撑时间 &lt; 预警月数
 * </p>
 */
@SpringBootTest
public class FinishedStockWarningServiceTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 成品库存预警服务
     */
    @Autowired
    private FinishedStockWarningService finishedStockWarningService;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 校验预警列表分页查询与统计口径
     */
    @Test
    public void testQueryWarnings() {
        PagedResult<FinishedStockWarningDto> page =
                finishedStockWarningService.queryWarnings(null, false, true, 0, 20);

        assertNotNull(page, "分页结果不能为空");
        assertNotNull(page.getItems(), "分页数据不能为空");
        assertTrue(page.getPageSize() > 0, "每页大小应大于0");
        assertTrue(page.getTotalCount() >= page.getItems().size(), "总记录数不应小于当前页数据量");

        for (FinishedStockWarningDto row : page.getItems()) {
            assertNotNull(row.getPreparationCode(), "品种编码不能为空");
            assertNotNull(row.getAutoMonthlyAvg(), "月平均出库量不能为空");
            assertNotNull(row.getAvailableStock(), "可用库存不能为空");
            assertNotNull(row.getWarningMonths(), "预警月数不能为空");
            assertNotNull(row.getOutboundMonths(), "月度出库明细不能为空");
            assertEquals(row.getAvgMonths(), row.getOutboundMonths().size(),
                    "月度出库明细应补齐统计月数");

            // 月均出库量大于0时，可支撑时间必须等于可用库存 ÷ 月均出库量（保留2位小数）
            if (row.getAutoMonthlyAvg().signum() > 0) {
                BigDecimal expected = row.getAvailableStock()
                        .divide(row.getAutoMonthlyAvg(), 2, RoundingMode.HALF_UP);
                assertEquals(0, expected.compareTo(row.getSupportMonths()),
                        "可支撑时间计算不一致: " + row.getPreparationCode());

                boolean expectedWarning = expected.compareTo(
                        BigDecimal.valueOf(row.getWarningMonths())) < 0;
                assertEquals(expectedWarning, row.isWarning(),
                        "预警状态计算不一致: " + row.getPreparationCode());
            } else {
                assertFalse(row.isWarning(), "无出库品种不应触发预警");
                assertEquals("noDemand", row.getWarningLevel(), "无出库品种级别应为 noDemand");
            }
        }
    }

    /**
     * 校验已出库台账聚合查询能取到测试库真实数据
     */
    @Test
    public void testOutboundAggregation() {
        PagedResult<FinishedStockWarningDto> page =
                finishedStockWarningService.queryWarnings("Z06-W13-001", false, true, 0, 20);
        Assumptions.assumeFalse(page.getItems().isEmpty(), "测试库无 Z06-W13-001，跳过出库聚合校验");

        FinishedStockWarningDto row = page.getItems().get(0);
        assertTrue(row.getTotalOutbound().signum() > 0, "测试库 Z06-W13-001 统计窗口内应有已出库数量");
        assertTrue(row.getAutoMonthlyAvg().signum() > 0, "有出库时月平均出库量应大于0");
        assertEquals(row.getAvgMonths(), row.getOutboundMonths().size(), "月度出库明细应补齐统计月数");
    }

    /**
     * 校验成品库存聚合查询能取到测试库真实数据
     */
    @Test
    public void testStockAggregation() {
        PagedResult<FinishedStockWarningDto> page =
                finishedStockWarningService.queryWarnings("Z01-001", false, true, 0, 20);
        Assumptions.assumeFalse(page.getItems().isEmpty(), "测试库无 Z01-001，跳过成品库存聚合校验");

        FinishedStockWarningDto row = page.getItems().get(0);
        assertTrue(row.getTotalStock().signum() > 0, "测试库 Z01-001 应有成品库存");
        assertTrue(row.getQualifiedStock().compareTo(row.getTotalStock()) <= 0,
                "合格库存不应大于总库存");
        assertTrue(row.getAvailableStock().compareTo(row.getQualifiedStock()) <= 0,
                "可用库存不应大于合格库存");
        // 计量单位取库存表，委托单位取制剂主数据，两者不能混用
        if (row.getCommissionedUnitName() != null) {
            assertNotEquals(row.getCommissionedUnitName(), row.getUnitName(),
                    "计量单位不应取到委托单位");
        }
    }

    /**
     * 校验预警汇总统计口径
     */
    @Test
    public void testSummary() {
        FinishedStockWarningSummaryDto summary = finishedStockWarningService.getSummary(true);

        assertNotNull(summary, "汇总结果不能为空");
        assertTrue(summary.getAvgMonths() > 0, "统计月数应大于0");
        assertTrue(summary.getWarningMonths() > 0, "预警月数应大于0");
        assertTrue(summary.getNearExpiryDays() >= 0, "近效期天数不能为负");
        assertNotNull(summary.getAvgStartMonth(), "统计起始月份不能为空");
        assertNotNull(summary.getAvgEndMonth(), "统计结束月份不能为空");
        assertTrue(summary.getTotalProducts() >= summary.getWarningCount(),
                "品种总数不应小于预警数量");
        assertTrue(summary.getWarningCount() >= summary.getDangerCount(),
                "预警数量不应小于危险数量");
    }

    /**
     * 校验首页待办使用的预警列表只返回触发预警的品种
     */
    @Test
    public void testGetWarnings() {
        List<FinishedStockWarningDto> warnings = finishedStockWarningService.getWarnings(true);

        assertNotNull(warnings, "预警列表不能为空");
        for (FinishedStockWarningDto row : warnings) {
            assertTrue(row.isWarning(), "待办预警列表只能包含触发预警的品种: " + row.getPreparationCode());
        }
    }

    // endregion
}

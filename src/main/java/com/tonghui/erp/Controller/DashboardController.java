package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Dashboard.*;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页仪表盘控制器
 * 提供首页统计数据查询
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    @Autowired
    private ProductionPlanService productionPlanService;

    @Autowired
    private StockService stockService;

    @Autowired
    private ApprovalInstanceService approvalInstanceService;

    /**
     * 获取首页汇总数据
     * 包含生产统计、库存预警、审批统计
     */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDto> getSummary() {
        try {
            DashboardSummaryDto summary = new DashboardSummaryDto();

            // 1. 生产计划统计
            ProductionStatsDto productionStats = new ProductionStatsDto();
            long totalPlans = productionPlanService.count();
            long inProgress = productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "生产中")
            );
            long completed = productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "已完成")
            );
            long pending = productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "待生产")
            );
            productionStats.setTotalPlans(totalPlans);
            productionStats.setInProgress(inProgress);
            productionStats.setCompleted(completed);
            productionStats.setPending(pending);
            summary.setProductionStats(productionStats);

            // 2. 库存预警统计
            StockWarningStatsDto stockWarnings = new StockWarningStatsDto();
            // 库存不足：有设置最低库存且当前库存 <= 最低库存（通过应用层过滤）
            List<Stock> allStocks = stockService.list(
                new QueryWrapper<Stock>().isNotNull("min_quantity")
            );
            long lowStock = allStocks.stream()
                .filter(s -> s.getQuantity() != null && s.getMinQuantity() != null 
                    && s.getQuantity().compareTo(s.getMinQuantity()) <= 0)
                .count();
            // 即将过期：30天内到期
            LocalDate today = LocalDate.now();
            LocalDate expiryDeadline = today.plusDays(30);
            long expiringSoon = stockService.count(
                new QueryWrapper<Stock>()
                    .ge("expiry_date", today)
                    .le("expiry_date", expiryDeadline)
            );
            // 已过期
            long expired = stockService.count(
                new QueryWrapper<Stock>()
                    .lt("expiry_date", today)
            );
            stockWarnings.setLowStock(lowStock);
            stockWarnings.setExpiringSoon(expiringSoon);
            stockWarnings.setExpired(expired);
            summary.setStockWarnings(stockWarnings);

            // 3. 审批统计
            ApprovalStatsDto approvalStats = new ApprovalStatsDto();
            long pendingApproval = approvalInstanceService.count(
                new QueryWrapper<ApprovalInstance>().eq("status", "PENDING")
            );
            approvalStats.setPendingApproval(pendingApproval);
            approvalStats.setMyPending(0); // TODO: 根据当前用户查询我的待审批
            summary.setApprovalStats(approvalStats);

            return success(summary);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }
}

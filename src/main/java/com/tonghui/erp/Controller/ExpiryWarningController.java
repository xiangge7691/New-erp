package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存有效期预警控制器
 * <p>
 * 提供库存有效期预警的查询、统计及高级筛选功能，用于库存管理中的过期预警和效期跟踪
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/expiry-warning/list             │ GET   │ 获取即将过期的库存列表              │
 * │ 2  │ /api/expiry-warning/stats            │ GET   │ 获取有效期预警统计                  │
 * │ 3  │ /api/expiry-warning/search           │ GET   │ 高级查询即将过期的库存（支持分页）  │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/expiry-warning")
public class ExpiryWarningController extends BaseController {
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 库存服务
     */
    private final StockService stockService;
    
    @Autowired
    public ExpiryWarningController(StockService stockService) {
        this.stockService = stockService;
    }
    
    // endregion

    // region 预警查询接口
    // ===================================
    // 预警查询接口
    // ===================================
    
    /**
     * 获取即将过期的库存列表
     * <p>
     * 查询在未来指定天数内即将过期的库存物品
     * </p>
     *
     * 示例请求：
     * GET /api/expiry-warning/list?days=30
     *
     * @param days 预警天数（默认30天），查询未来N天内即将过期的库存
     * @return ApiResponse&lt;List&lt;ExpiryWarningDTO&gt;&gt; 预警库存列表
     */
    @GetMapping("/list")
    public ApiResponse<List<ExpiryWarningDTO>> getExpiringStocks(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<ExpiryWarningDTO> warnings = stockService.getExpiringStocks(days);
            return success(warnings);
        } catch (Exception e) {
            return exception(e, "获取预警列表");
        }
    }
    
    /**
     * 获取有效期预警统计
     * <p>
     * 返回过期、即将过期、库存不足等各类预警的统计数据
     * </p>
     *
     * 示例请求：
     * GET /api/expiry-warning/stats
     *
     * @return ApiResponse&lt;ExpiryWarningStatsDTO&gt; 预警统计数据（包含过期、即将过期、库存不足等统计）
     */
    @GetMapping("/stats")
    public ApiResponse<ExpiryWarningStatsDTO> getStats() {
        try {
            ExpiryWarningStatsDTO stats = stockService.getExpiryWarningStats();
            return success(stats);
        } catch (Exception e) {
            return exception(e, "获取预警统计");
        }
    }
    
    /**
     * 高级查询即将过期的库存（支持分页和筛选）
     * <p>
     * 支持按预警天数、物品类型、生产单位、预警级别等条件进行筛选
     * </p>
     *
     * 示例请求：
     * GET /api/expiry-warning/search?days=90&itemType=material&prodUnitId=1&warningLevel=urgent&pageIndex=0&pageSize=20
     *
     * @param days 预警天数（默认90天）
     * @param itemType 物品类型（可选: material-物料，preparation-制剂）
     * @param prodUnitId 生产单位ID（可选）
     * @param warningLevel 预警级别（可选: urgent-紧急，warning-警告，info-提示）
     * @param pageIndex 页码（默认0）
     * @param pageSize 每页大小（默认20）
     * @return ApiResponse&lt;PagedResult&lt;ExpiryWarningDTO&gt;&gt; 分页结果，包含预警库存列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<ExpiryWarningDTO>> queryExpiringStocks(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Long prodUnitId,
            @RequestParam(required = false) String warningLevel,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Page<ExpiryWarningDTO> page = stockService.queryExpiringStocks(
                    days, itemType, prodUnitId, warningLevel, pageIndex, pageSize);
            
            PagedResult<ExpiryWarningDTO> result = new PagedResult<>();
            result.setItems(page.getRecords());
            result.setTotalCount(page.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询预警库存");
        }
    }

    // endregion
}

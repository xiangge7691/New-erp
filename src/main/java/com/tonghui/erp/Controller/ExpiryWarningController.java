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
 */
@RestController
@RequestMapping("/api/expiry-warning")
public class ExpiryWarningController extends BaseController {
    
    private final StockService stockService;
    
    @Autowired
    public ExpiryWarningController(StockService stockService) {
        this.stockService = stockService;
    }
    
    /**
     * 获取即将过期的库存列表
     *
     * @param days 预警天数（默认30天）
     * @return 预警库存列表
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
     *
     * @return 预警统计数据
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
     *
     * @param days 预警天数（默认90天）
     * @param itemType 物品类型（可选: material/preparation）
     * @param prodUnitId 生产单位ID（可选）
     * @param warningLevel 预警级别（可选: urgent/warning/info）
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return 分页结果
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
}

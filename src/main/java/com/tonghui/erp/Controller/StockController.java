package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 库存控制器
 */
@RestController
@RequestMapping("/api/stock")
public class StockController extends BaseController {

    private final StockService stockService;

    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // #region 高级查询

    /**
     * 高级查询库存（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - itemName：模糊匹配
     * - categoryName：精确匹配
     * - unitName：精确匹配
     * - quantity：大于等于
     * - prodUnitId：精确匹配
     * - createdTimeStart：创建时间起始（大于等于）
     * - createdTimeEnd：创建时间结束（小于等于）
     * - updatedTimeStart：更新时间起始（大于等于）
     * - updatedTimeEnd：更新时间结束（小于等于）
     *
     * 示例请求：
     * GET /stock/search?pageIndex=1&pageSize=20&itemName=瓶&categoryName=包材&unitName=个&quantity=100&prodUnitId=1&createdTimeStart=2025-01-01%2000:00:00&createdTimeEnd=2025-09-01%2023:59:59
     *
     * @param stock      查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex  页码
     * @param pageSize   每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<Stock>> queryStocks(Stock stock,
                                          @RequestParam(required = false) LocalDateTime createdTimeStart,
                                          @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                          @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                          @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                          @RequestParam int pageIndex,
                                          @RequestParam int pageSize) {
        try {
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : pageSize;
            if (safePageSize < 1) {
                safePageSize = 1;
            }

            // 获取分页结果
            Page<Stock> pageResult = stockService.queryStocks(stock, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);

            // 转换为统一的PagedResult格式
            PagedResult<Stock> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return error("查询失败：" + ex.getMessage());
        }
    }

    // #endregion

    // #region 带子表查询

    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<StockWithDetailsDto>> searchWithDetails(Stock stock,
                                                                           @RequestParam int pageIndex,
                                                                           @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<StockWithDetailsDto> result = stockService.searchWithDetails(stock, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // #endregion
}

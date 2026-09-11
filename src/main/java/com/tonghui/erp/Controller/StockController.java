package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockGroupedDto;
import com.tonghui.erp.Common.Dto.Stock.StockGroupedByBatchDto;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDetailDto;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存控制器
 * <p>
 * 提供库存的高级查询和带子表查询功能
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/stock/search            │ GET    │ 高级查询库存                 │
 * │ 2  │ /api/stock/search-with-details │ GET  │ 带子表查询库存               │
 * └────┴──────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/stock")
public class StockController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 库存服务
     */
    private final StockService stockService;

    /**
     * 构造方法注入库存服务
     *
     * @param stockService 库存服务
     */
    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

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
     * GET /api/stock/search?pageIndex=1&pageSize=20&keyword=瓶&categoryName=包材&unitName=个&quantity=100&prodUnitId=1&createdTimeStart=2025-01-01%2000:00:00&createdTimeEnd=2025-09-01%2023:59:59
     *
     * @param stock      查询条件（自动从query参数映射）
     * @param keyword    关键字（对物品编码、物品名称进行模糊匹配，可选）
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
                                          @RequestParam(required = false) String keyword,
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
            Page<Stock> pageResult = stockService.queryStocks(stock, keyword, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);

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

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 带子表查询库存（支持多条件 + 分页）
     *
     * 示例请求：
     * GET /api/stock/search-with-details?pageIndex=1&pageSize=20&keyword=瓶
     *
     * @param stock     查询条件（自动从query参数映射）
     * @param keyword   关键字（对物品编码、物品名称进行模糊匹配，可选）
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果（包含子表信息）
     */
// @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<StockWithDetailsDto>> searchWithDetails(Stock stock,
                                                                           @RequestParam(required = false) String keyword,
                                                                           @RequestParam int pageIndex,
                                                                           @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<StockWithDetailsDto> result = stockService.searchWithDetails(stock, keyword, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion

    // region 分组查询与流水
    // ===================================
    // 分组查询与流水
    // ===================================

    /**
     * 按批次号+制剂名称分组查询库存（含明细与仓库名称）
     * <p>
     * 用于库存查询页面"按批次+制剂分组 + 明细展开"的展示模式，
     * 每个分组下显示同批次+制剂的所有库存记录（含来源入库单号）
     * </p>
     *
     * 示例请求：
     * GET /api/stock/grouped-search?pageIndex=0&pageSize=20&batchNumber=A&preparationName=伸腿&stockStatus=合格
     *
     * @param itemCode        物料编码（模糊匹配，选填）
     * @param itemName        物料名称（模糊匹配，选填）
     * @param batchNumber     批次号（模糊匹配，选填）
     * @param preparationName 制剂名称（模糊匹配，选填）
     * @param categoryName    分类名称（等值匹配，选填）
     * @param prodUnitId      仓库（生产单位ID，选填）
     * @param stockStatus     库存状态（选填：合格/待检/不合格）
     * @param showZero        是否显示零库存（选填，默认false）
     * @param pageIndex       页码，从0开始
     * @param pageSize        每页大小
     * @return 分组分页结果（批次+制剂组列表，含明细）
     */
    @GetMapping("/grouped-search")
    public ApiResponse<PagedResult<StockGroupedByBatchDto>> groupedSearch(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String preparationName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long prodUnitId,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false, defaultValue = "false") boolean showZero,
            @RequestParam int pageIndex,
            @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<StockGroupedByBatchDto> result = stockService.groupedSearchByBatchAndPreparation(
                    itemCode, itemName, batchNumber, preparationName,
                    categoryName, prodUnitId, stockStatus, showZero,
                    safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "分组查询库存失败");
        }
    }

    /**
     * 根据库存ID查询库存流水列表
     * <p>
     * 用于库存查询页面展开批次后查看该批次的出入库流水
     * </p>
     *
     * 示例请求：
     * GET /api/stock/1/transactions
     *
     * @param id 库存ID
     * @return 库存流水列表
     */
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<StockTransactionDto>> getTransactions(@PathVariable Long id) {
        try {
            List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(id);
            return success(transactions);
        } catch (Exception ex) {
            return exception(ex, "查询库存流水失败");
        }
    }

    /**
     * 按批次号+制剂名称分组查询库存
     * <p>
     * 用于库存查询页面按"批次+制剂分组 + 明细展开"的展示模式，
     * 每个分组下显示同批次+制剂的所有库存记录（含来源入库单号）
     * </p>
     *
     * 示例请求：
     * GET /api/stock/grouped-by-batch?pageIndex=0&pageSize=20&batchNumber=A&preparationName=伸腿
     *
     * @param batchNumber     批次号（可选，模糊匹配）
     * @param preparationName 制剂名称（可选，模糊匹配）
     * @param itemCode        物料编码（可选，模糊匹配）
     * @param itemName        物料名称（可选，模糊匹配）
     * @param categoryName    分类名称（可选，精确匹配）
     * @param prodUnitId      仓库ID（可选，精确匹配）
     * @param stockStatus     库存状态（可选，精确匹配）
     * @param showZero        是否显示零库存
     * @param pageIndex       页码
     * @param pageSize        每页大小
     * @return 分组分页结果
     */
    @GetMapping("/grouped-by-batch")
    public ApiResponse<PagedResult<StockGroupedByBatchDto>> groupedSearchByBatch(
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String preparationName,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Long prodUnitId,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "false") boolean showZero,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            PagedResult<StockGroupedByBatchDto> result = stockService.groupedSearchByBatchAndPreparation(
                    itemCode, itemName, batchNumber, preparationName,
                    categoryName, prodUnitId, stockStatus, showZero,
                    pageIndex, pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "按批次查询库存");
        }
    }

    /**
     * 根据库存ID查询流水列表（增强版，含物品/仓库/关联单据信息）
     *
     * 示例请求：
     * GET /api/stock/1/transaction-details
     *
     * @param id 库存ID
     * @return 流水详细列表
     */
    @GetMapping("/{id}/transaction-details")
    public ApiResponse<List<StockTransactionDetailDto>> getTransactionDetails(@PathVariable Long id) {
        try {
            List<StockTransactionDetailDto> result = stockService.getTransactionDetailsByStockId(id);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询库存流水");
        }
    }

    /**
     * 根据批次号+制剂名称查询流水列表
     *
     * 示例请求：
     * GET /api/stock/transactions-by-batch?batchNumber=A&preparationName=伸腿
     *
     * @param batchNumber     批次号（可选，模糊匹配）
     * @param preparationName 制剂名称（可选，模糊匹配）
     * @param itemCode        物料编码（可选，模糊匹配）
     * @return 流水详细列表
     */
    @GetMapping("/transactions-by-batch")
    public ApiResponse<List<StockTransactionDetailDto>> getTransactionDetailsByBatch(
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String preparationName,
            @RequestParam(required = false) String itemCode) {
        try {
            List<StockTransactionDetailDto> result = stockService.getTransactionDetailsByBatch(
                    batchNumber, preparationName, itemCode);
            return success(result);
        } catch (Exception e) {
            return exception(e, "按批次查询流水");
        }
    }

    /**
     * 根据入库单ID查询流水列表
     *
     * 示例请求：
     * GET /api/stock/transactions-by-stock-in/1
     *
     * @param stockInId 入库单ID
     * @return 流水详细列表
     */
    @GetMapping("/transactions-by-stock-in/{stockInId}")
    public ApiResponse<List<StockTransactionDetailDto>> getTransactionDetailsByStockIn(
            @PathVariable Long stockInId) {
        try {
            List<StockTransactionDetailDto> result = stockService.getTransactionDetailsByStockInId(stockInId);
            return success(result);
        } catch (Exception e) {
            return exception(e, "按入库单查询流水");
        }
    }

    /**
     * 根据出库单ID查询流水列表
     *
     * 示例请求：
     * GET /api/stock/transactions-by-stock-out/1
     *
     * @param stockOutId 出库单ID
     * @return 流水详细列表
     */
    @GetMapping("/transactions-by-stock-out/{stockOutId}")
    public ApiResponse<List<StockTransactionDetailDto>> getTransactionDetailsByStockOut(
            @PathVariable Long stockOutId) {
        try {
            List<StockTransactionDetailDto> result = stockService.getTransactionDetailsByStockOutId(stockOutId);
            return success(result);
        } catch (Exception e) {
            return exception(e, "按出库单查询流水");
        }
    }

    // endregion
}

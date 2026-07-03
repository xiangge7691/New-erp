package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockOutWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Service.StockInService;
import com.tonghui.erp.Service.StockOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单控制器
 * <p>
 * 提供出库单的增删改查、高级查询、带明细子表操作以及出库单号自动生成等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                             │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────────┤
 * │ 1  │ /api/stockout                    │ GET    │ 分页查询所有出库单               │
 * │ 2  │ /api/stockout/{id}               │ GET    │ 获取出库单详情                   │
 * │ 3  │ /api/stockout                    │ POST   │ 新增出库单                       │
 * │ 4  │ /api/stockout/{id}               │ PUT    │ 修改出库单                       │
 * │ 5  │ /api/stockout/{id}               │ DELETE │ 删除出库单                       │
 * │ 6  │ /api/stockout/search             │ GET    │ 高级查询出库单（支持多条件+分页） │
 * │ 7  │ /api/stockout/search-with-details│ GET    │ 高级查询出库单（包含明细子表）    │
 * │ 8  │ /api/stockout/withDetails        │ POST   │ 创建出库单（包含明细）           │
 * │ 9  │ /api/stockout/{id}/withDetails   │ PUT    │ 更新出库单（包含明细）           │
 * │ 10 │ /api/stockout/{id}/details       │ GET    │ 获取出库明细列表                 │
 * │ 11 │ /api/stockout/detail             │ POST   │ 添加出库明细                     │
 * │ 12 │ /api/stockout/details            │ POST   │ 批量添加出库明细                 │
 * │ 13 │ /api/stockout/detail             │ PUT    │ 更新出库明细                     │
 * │ 14 │ /api/stockout/detail/{id}        │ DELETE │ 删除出库明细                     │
 * │ 15 │ /api/stockout/generateCode       │ GET    │ 生成出库单号                     │
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/stockout")
public class StockOutController extends BaseCrudController<StockOut, StockOut, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 出库单服务
     */
    @Autowired
    private StockOutService stockOutService;

    // endregion

    // region 基础CRUD实现
    // ===================================
    // 基础CRUD实现
    // ===================================

    @Override
    protected PagedResult<StockOut> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用StockOutService的queryStockOuts方法进行查询
        StockOut stockOut = new StockOut();
        Page<StockOut> pageResult = stockOutService.queryStockOuts(stockOut, null, null, null, null, null, null, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<StockOut> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected StockOut getDataById(Long id) {
        return stockOutService.getStockOutById(id);
    }

    @Override
    protected StockOut doCreate(StockOut stockOut) {
        // 创建时如果没有单号，则自动生成
        if (stockOut.getOutCode() == null || stockOut.getOutCode().isEmpty()) {
            stockOut.setOutCode(stockOutService.generateStockOutCode());
        }
        stockOutService.addStockOut(stockOut, null);
        return stockOut;
    }

    @Override
    protected StockOut doUpdate(Long id, StockOut stockOut) {
        stockOut.setOutId(id);
        // 只更新非 null 字段，支持部分更新
        stockOutService.partialUpdateStockOut(stockOut);
        return stockOut;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            stockOutService.deleteStockOut(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询出库单（支持多条件 + 分页 + 时间范围）
     * <p>
     * 可选查询条件：outCode（模糊匹配）、prodUnitId、customerId、relatedOrder（模糊匹配）、
     * outDate、outStatus、outType、createdBy、updatedBy 等精确匹配条件，
     * 以及 createdTimeStart/End、updatedTimeStart/End、startDate/endDate 时间范围条件
     * </p>
     *
     * 示例请求：
     * GET /api/stockout/search?pageIndex=1&pageSize=20&outCode=OUT2025&prodUnitId=1&createdTimeStart=2025-01-01%2000:00:00&createdTimeEnd=2025-09-01%2023:59:59
     *
     * @param stockOut  查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 出库开始日期
     * @param endDate   出库结束日期
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return PagedResult&lt;StockOut&gt; 分页结果
     */
    @GetMapping("/search")
    public PagedResult<StockOut> queryStockOuts(StockOut stockOut,
                                                @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                @RequestParam(required = false) LocalDate startDate,
                                                @RequestParam(required = false) LocalDate endDate,
                                                @RequestParam int pageIndex,
                                                @RequestParam int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 获取分页结果
        Page<StockOut> pageResult = stockOutService.queryStockOuts(stockOut, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, startDate, endDate, safePageIndex, safePageSize);

        // 转换为统一的PagedResult格式
        PagedResult<StockOut> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 高级查询出库单（包含明细子表）
     * <p>
     * 与 /search 接口类似，但返回结果中包含出库明细子表信息
     * </p>
     *
     * 示例请求：
     * GET /api/stockout/search-with-details?pageIndex=1&pageSize=20&outCode=OUT2025
     *
     * @param stockOut  查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 出库开始日期
     * @param endDate   出库结束日期
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return ApiResponse&lt;PagedResult&lt;StockOutWithDetailsDto&gt;&gt; 分页结果（包含明细）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<StockOutWithDetailsDto>> searchWithDetails(StockOut stockOut,
                                                                               @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                               @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                               @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                               @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                               @RequestParam(required = false) LocalDate startDate,
                                                                               @RequestParam(required = false) LocalDate endDate,
                                                                               @RequestParam int pageIndex,
                                                                               @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<StockOutWithDetailsDto> result = stockOutService.searchWithDetails(stockOut, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, startDate, endDate, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion

    // region 专门的出库单创建和更新接口（包含明细）
    // ===================================
    // 专门的出库单创建和更新接口（包含明细）
    // ===================================

    /**
     * 创建出库单（包含明细）
     * <p>
     * 如果未提供出库单号，系统将自动生成。可同时传入出库明细列表。
     * </p>
     *
     * 示例请求：
     * POST /api/stockout/withDetails
     * Content-Type: application/json
     * {
     *   "outType": "销售出库",
     *   "customerId": 1,
     *   "outDate": "2026-07-01",
     *   "remark": "正常出库"
     * }
     *
     * @param stockOut 出库单信息
     * @param details 出库明细列表（可选）
     * @return ApiResponse&lt;StockOut&gt; 出库单信息
     */
    @PostMapping("/withDetails")
    public ApiResponse<StockOut> createStockOutWithDetails(@RequestBody StockOut stockOut,
                                              @RequestParam(required = false) List<StockOutDetail> details) {
        if (stockOut.getOutCode() == null || stockOut.getOutCode().isEmpty()) {
            stockOut.setOutCode(stockOutService.generateStockOutCode());
        }
        stockOutService.addStockOut(stockOut, details);
        return success(stockOut, "出库单创建成功");
    }

    /**
     * 更新出库单（包含明细）
     *
     * 示例请求：
     * PUT /api/stockout/1/withDetails
     * Content-Type: application/json
     * {
     *   "outStatus": "已完成",
     *   "remark": "已更新"
     * }
     *
     * @param id 出库单ID（路径参数）
     * @param stockOut 出库单信息
     * @param details 出库明细列表（可选）
     * @return StockOut 更新后的出库单信息
     */
    @PutMapping("/{id}/withDetails")
    public StockOut updateStockOutWithDetails(@PathVariable Long id,
                                             @RequestBody StockOut stockOut,
                                             @RequestParam(required = false) List<StockOutDetail> details) {
        stockOut.setOutId(id);
        stockOutService.updateStockOut(stockOut, details);
        return stockOut;
    }

    // endregion

    // region 出库单明细相关接口
    // ===================================
    // 出库单明细相关接口
    // ===================================

    /**
     * 根据出库单ID获取明细列表
     *
     * 示例请求：
     * GET /api/stockout/1/details
     *
     * @param stockOutId 出库单ID（路径参数）
     * @return ApiResponse&lt;List&lt;StockOutDetail&gt;&gt; 明细列表
     */
    @GetMapping("/{id}/details")
    public ApiResponse<List<StockOutDetail>> getStockOutDetails(@PathVariable("id") Long stockOutId) {
        try {
            List<StockOutDetail> details = stockOutService.getStockOutDetailsByStockOutId(stockOutId);
            return success(details);
        } catch (Exception e) {
            return exception(e, "获取出库明细列表");
        }
    }

    /**
     * 添加出库明细
     *
     * 示例请求：
     * POST /api/stockout/detail
     * Content-Type: application/json
     * {
     *   "outId": 1,
     *   "prodUnitId": 10,
     *   "quantity": 100
     * }
     *
     * @param detail 出库明细信息
     * @return ApiResponse&lt;StockOutDetail&gt; 添加的出库明细
     */
    @PostMapping("/detail")
    public ApiResponse<StockOutDetail> addStockOutDetail(@RequestBody StockOutDetail detail) {
        try {
            stockOutService.addStockOutDetail(detail);
            return success(detail, "添加出库明细成功");
        } catch (Exception e) {
            return exception(e, "添加出库明细");
        }
    }

    /**
     * 批量添加出库明细
     *
     * 示例请求：
     * POST /api/stockout/details
     * Content-Type: application/json
     * [
     *   {"outId": 1, "prodUnitId": 10, "quantity": 100},
     *   {"outId": 1, "prodUnitId": 11, "quantity": 50}
     * ]
     *
     * @param details 出库明细列表
     * @return ApiResponse&lt;List&lt;StockOutDetail&gt;&gt; 添加的出库明细列表
     */
    @PostMapping("/details")
    public ApiResponse<List<StockOutDetail>> addStockOutDetails(@RequestBody List<StockOutDetail> details) {
        try {
            stockOutService.addStockOutDetails(details);
            return success(details, "批量添加出库明细成功");
        } catch (Exception e) {
            return exception(e, "批量添加出库明细");
        }
    }

    /**
     * 更新出库明细
     *
     * 示例请求：
     * PUT /api/stockout/detail
     * Content-Type: application/json
     * {
     *   "detailId": 1,
     *   "quantity": 200
     * }
     *
     * @param detail 出库明细信息
     * @return ApiResponse&lt;StockOutDetail&gt; 更新后的出库明细
     */
    @PutMapping("/detail")
    public ApiResponse<StockOutDetail> updateStockOutDetail(@RequestBody StockOutDetail detail) {
        try {
            stockOutService.updateStockOutDetail(detail);
            return success(detail, "更新出库明细成功");
        } catch (Exception e) {
            return exception(e, "更新出库明细");
        }
    }

    /**
     * 删除出库明细
     *
     * 示例请求：
     * DELETE /api/stockout/detail/1
     *
     * @param detailId 明细ID（路径参数）
     * @return ApiResponse&lt;Boolean&gt; 是否删除成功
     */
    @DeleteMapping("/detail/{id}")
    public ApiResponse<Boolean> deleteStockOutDetail(@PathVariable("id") Long detailId) {
        try {
            stockOutService.deleteStockOutDetail(detailId);
            return success(true, "删除出库明细成功");
        } catch (Exception e) {
            return exception(e, "删除出库明细");
        }
    }

    // endregion

    // region 单号生成接口
    // ===================================
    // 单号生成接口
    // ===================================

    /**
     * 生成出库单号
     * <p>
     * 根据系统规则自动生成唯一的出库单号
     * </p>
     *
     * 示例请求：
     * GET /api/stockout/generateCode
     *
     * @return ApiResponse&lt;String&gt; 出库单号
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateStockOutCode() {
        try {
            String code = stockOutService.generateStockOutCode();
            return success(code, "生成出库单号成功");
        } catch (Exception e) {
            return exception(e, "生成出库单号");
        }
    }

    // endregion
}

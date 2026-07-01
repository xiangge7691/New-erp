package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockInWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Service.StockInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库单控制器
 */
@RestController
@RequestMapping("/api/stockin")
public class StockInController extends BaseCrudController<StockIn, StockIn, Long> {

    @Autowired
    private StockInService stockInService;

    @Override
    protected PagedResult<StockIn> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用StockInService的queryStockIns方法进行查询
        StockIn stockIn = new StockIn();
        Page<StockIn> pageResult = stockInService.queryStockIns(stockIn, null, null, null, null, null, null, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<StockIn> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected StockIn getDataById(Long id) {
        return stockInService.getStockInById(id);
    }

    @Override
    protected StockIn doCreate(StockIn stockIn) {
        // 创建时如果没有单号，则自动生成
        if (stockIn.getInCode() == null || stockIn.getInCode().isEmpty()) {
            stockIn.setInCode(stockInService.generateStockInCode());
        }
        stockInService.addStockIn(stockIn, null);
        return stockIn;
    }

    @Override
    protected StockIn doUpdate(Long id, StockIn stockIn) {
        stockIn.setInId(id);
        // 只更新非 null 字段，支持部分更新
        stockInService.partialUpdateStockIn(stockIn);
        return stockIn;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            stockInService.deleteStockIn(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // #region 高级查询

    /**
     * 高级查询入库单（支持多条件 + 分页 + 时间范围）
     *
     * 可选查询条件：
     * - inCode：模糊匹配
     * - prodUnitId：精确匹配
     * - supplierId：精确匹配
     * - relatedOrder：模糊匹配
     * - inDate：精确匹配
     * - inStatus：状态过滤
     * - createdTimeStart：创建时间起始（大于等于）
     * - createdTimeEnd：创建时间结束（小于等于）
     * - updatedTimeStart：更新时间起始（大于等于）
     * - updatedTimeEnd：更新时间结束（小于等于）
     * - startDate：入库开始日期（大于等于）
     * - endDate：入库结束日期（小于等于）
     * - createdBy：创建人ID（精确匹配）
     * - updatedBy：更新人ID（精确匹配）
     * - inType：入库类型（精确匹配）
     *
     * 示例请求：
     * GET /stockin/search?pageIndex=1&pageSize=20&inCode=IN2025&prodUnitId=1&createdTimeStart=2025-01-01%2000:00:00&createdTimeEnd=2025-09-01%2023:59:59
     *
     * @param stockIn   查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 入库开始日期
     * @param endDate   入库结束日期
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public PagedResult<StockIn> queryStockIns(StockIn stockIn,
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
        Page<StockIn> pageResult = stockInService.queryStockIns(stockIn, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, startDate, endDate, safePageIndex, safePageSize);

        // 转换为统一的PagedResult格式
        PagedResult<StockIn> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    // #endregion

    // #region 带子表查询

    /**
     * 高级查询入库单（包含明细子表）
     *
     * @param stockIn   查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 入库开始日期
     * @param endDate   入库结束日期
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果（包含明细）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<StockInWithDetailsDto>> searchWithDetails(StockIn stockIn,
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
            PagedResult<StockInWithDetailsDto> result = stockInService.searchWithDetails(stockIn, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, startDate, endDate, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // #endregion

    // #region 专门的入库单创建和更新接口（包含明细）

    /**
     * 创建入库单（包含明细）
     *
     * @param stockIn 入库单信息
     * @param details 入库明细列表
     * @return 入库单信息
     */
    @PostMapping("/withDetails")
    public ApiResponse<StockIn> createStockInWithDetails(@RequestBody StockIn stockIn,
                                            @RequestParam(required = false) List<StockInDetail> details) {
        if (stockIn.getInCode() == null || stockIn.getInCode().isEmpty()) {
            stockIn.setInCode(stockInService.generateStockInCode());
        }
        stockInService.addStockIn(stockIn, details);
        return success(stockIn, "入库单创建成功");
    }

    /**
     * 更新入库单（包含明细）
     *
     * @param id 入库单ID
     * @param stockIn 入库单信息
     * @param details 入库明细列表
     * @return 入库单信息
     */
    @PutMapping("/{id}/withDetails")
    public StockIn updateStockInWithDetails(@PathVariable Long id,
                                           @RequestBody StockIn stockIn,
                                           @RequestParam(required = false) List<StockInDetail> details) {
        stockIn.setInId(id);
        stockInService.updateStockIn(stockIn, details);
        return stockIn;
    }

    // #endregion

    // #region 入库单明细相关接口

    /**
     * 根据入库单ID获取明细列表
     *
     * @param stockInId 入库单ID
     * @return 明细列表
     */
    @GetMapping("/{id}/details")
    public ApiResponse<List<StockInDetail>> getStockInDetails(@PathVariable("id") Long stockInId) {
        try {
            List<StockInDetail> details = stockInService.getStockInDetailsByStockInId(stockInId);
            return success(details);
        } catch (Exception e) {
            return exception(e, "获取入库明细列表");
        }
    }

    /**
     * 添加入库明细
     *
     * @param detail 入库明细
     * @return 入库明细
     */
    @PostMapping("/detail")
    public ApiResponse<StockInDetail> addStockInDetail(@RequestBody StockInDetail detail) {
        try {
            stockInService.addStockInDetail(detail);
            return success(detail, "添加入库明细成功");
        } catch (Exception e) {
            return exception(e, "添加入库明细");
        }
    }

    /**
     * 批量添加入库明细
     *
     * @param details 入库明细列表
     * @return 入库明细列表
     */
    @PostMapping("/details")
    public ApiResponse<List<StockInDetail>> addStockInDetails(@RequestBody List<StockInDetail> details) {
        try {
            stockInService.addStockInDetails(details);
            return success(details, "批量添加入库明细成功");
        } catch (Exception e) {
            return exception(e, "批量添加入库明细");
        }
    }

    /**
     * 更新入库明细
     *
     * @param detail 入库明细
     * @return 入库明细
     */
    @PutMapping("/detail")
    public ApiResponse<StockInDetail> updateStockInDetail(@RequestBody StockInDetail detail) {
        try {
            stockInService.updateStockInDetail(detail);
            return success(detail, "更新入库明细成功");
        } catch (Exception e) {
            return exception(e, "更新入库明细");
        }
    }

    /**
     * 删除入库明细
     *
     * @param detailId 明细ID
     * @return 是否删除成功
     */
    @DeleteMapping("/detail/{id}")
    public ApiResponse<Boolean> deleteStockInDetail(@PathVariable("id") Long detailId) {
        try {
            stockInService.deleteStockInDetail(detailId);
            return success(true, "删除入库明细成功");
        } catch (Exception e) {
            return exception(e, "删除入库明细");
        }
    }

    // #endregion

    // #region 单号生成接口

    /**
     * 生成入库单号
     *
     * @return 入库单号
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateStockInCode() {
        try {
            String code = stockInService.generateStockInCode();
            return success(code, "生成入库单号成功");
        } catch (Exception e) {
            return exception(e, "生成入库单号");
        }
    }

    // #endregion
}

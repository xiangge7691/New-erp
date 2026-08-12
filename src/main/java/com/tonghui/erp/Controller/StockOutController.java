package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.BatchOutboundRequest;
import com.tonghui.erp.Common.Dto.Stock.PlanDetailItemDto;
import com.tonghui.erp.Common.Dto.Stock.StockOutWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Service.StockInService;
import com.tonghui.erp.Service.StockOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
* │ 12 │ /api/stockout/details            │ POST   │ 批量添加出库明细                     │
 * │ 13 │ /api/stockout/detail             │ PUT    │ 更新出库明细                     │
 * │ 14 │ /api/stockout/detail/{id}        │ DELETE │ 删除出库明细                     │
 * │ 15 │ /api/stockout/generateCode       │ GET    │ 生成出库单号                     │
 * │ 16 │ /api/stockout/preparation-detail │ GET    │ 按制剂ID查处方明细（含物料库存批次）  │
 * │ 17 │ /api/stockout/direct-outbound    │ POST   │ 表单+明细直接批量出库（无需确认）     │
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
     * outStatus、outType、createdBy、updatedBy 等精确匹配条件，
     * 以及 createdTimeStart/End、updatedTimeStart/End、startDate/endDate 时间范围条件
     * </p>
     * <p>
     * outDate 出库日期精确到时分秒（格式：yyyy-MM-dd HH:mm:ss 或 ISO 格式）；
     * startDate/endDate 为出库日期范围（按整天含端点：起始 00:00:00，结束 23:59:59）
     * </p>
     *
     * 示例请求：
     * GET /api/stockout/search?pageIndex=1&pageSize=20&outCode=OUT2025&prodUnitId=1&createdTimeStart=2025-01-01%2000:00:00&createdTimeEnd=2025-09-01%2023:59:59&startDate=2026-08-01&endDate=2026-08-11
     *
     * @param stockOut  查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 出库开始日期（yyyy-MM-dd，精确到当日00:00:00）
     * @param endDate   出库结束日期（yyyy-MM-dd，精确到当日23:59:59）
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
     * @param startDate 出库开始日期（yyyy-MM-dd，精确到当日00:00:00）
     * @param endDate   出库结束日期（yyyy-MM-dd，精确到当日23:59:59）
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
     *   "outDate": "2026-07-01T10:30:00",
     *   "remark": "正常出库"
     * }
     *
     * @param stockOut 出库单信息
     * @param details 出库明细列表（可选）
     * @return ApiResponse&lt;StockOut&gt; 出库单信息
     */
// @PostMapping("/withDetails")
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
// @PutMapping("/{id}/withDetails")
    public StockOut updateStockOutWithDetails(@PathVariable Long id,
                                             @RequestBody StockOut stockOut,
                                             @RequestParam(required = false) List<StockOutDetail> details) {
        stockOut.setOutId(id);
        stockOutService.updateStockOut(stockOut, details);
        return stockOut;
    }

    // endregion

    // region 出库确认（库存联动）
    // ===================================
    // 出库确认（库存联动）
    // ===================================

    /**
     * 确认出库：草稿 → 已出库
     * <p>校验出库单为草稿状态且有明细，联动扣减库存批次并写入库存流水（库存不足整体回滚）</p>
     *
     * 示例请求：
     * POST /api/stockout/1/confirm
     *
     * @param id 出库单ID
     * @return 操作结果
     */
// @PostMapping("/{id}/confirm")
    public ApiResponse<Boolean> confirmStockOut(@PathVariable Long id) {
        try {
            stockOutService.confirmStockOut(id);
            return success(true, "出库确认成功，库存已扣减");
        } catch (Exception e) {
            return exception(e, "确认出库");
        }
    }

    /**
     * 取消出库：已出库 → 已取消
     * <p>校验出库单为已出库状态，随后回滚库存（恢复对应库存批次）并写入调整流水</p>
     *
     * 示例请求：
     * POST /api/stockout/1/cancel
     *
     * @param id 出库单ID
     * @return 操作结果
     */
// @PostMapping("/{id}/cancel")
    public ApiResponse<Boolean> cancelStockOut(@PathVariable Long id) {
        try {
            stockOutService.cancelStockOut(id);
            return success(true, "出库单已取消，库存已回滚");
        } catch (Exception e) {
            return exception(e, "取消出库");
        }
    }

    // endregion

    // region 批量出库（按制剂处方）
    // ===================================
    // 批量出库（按制剂处方）
    // ===================================

    /**
     * 按生产计划获取批量出库处方明细
     * <p>
     * 根据生产计划关联制剂处方，返回每个物料应出数量（处方量×生产倍数）及可用库存批次，
     * 用于批量出库弹窗自动生成出库明细
     * </p>
     *
     * 示例请求：
     * GET /api/stockout/plan-detail?planCode=JH-20260701001&multiplier=20
     *
     * @param planCode   生产计划编号（必填）
     * @param multiplier 生产倍数（选填，默认1倍）
     * @return 处方明细列表（含可用库存批次）
     */
// @GetMapping("/plan-detail")
    public ApiResponse<List<PlanDetailItemDto>> getPlanDetail(
            @RequestParam String planCode,
            @RequestParam(required = false) BigDecimal multiplier) {
        try {
            List<PlanDetailItemDto> items = stockOutService.getPlanDetail(planCode, multiplier);
            return success(items);
        } catch (Exception e) {
            return exception(e, "查询处方出库明细");
        }
    }

    /**
     * 批量出库确认：一次事务内创建出库单并确认生效（扣减库存+写流水）
     *
     * 示例请求：
     * POST /api/stockout/batch-confirm
     * Content-Type: application/json
     * {
     *   "outType": "生产领料出库",
     *   "relatedOrder": "JH-20260701001",
     *   "prodUnitId": 1,
     *   "remark": "批量出库-益肾壮骨丸",
     *   "items": [
     *     { "stockId": 1, "itemCode": "Y0084", "itemName": "甘草", "categoryName": "原料",
     *       "unitName": "kg", "batchNumber": "HG20260723", "quantity": 0.42, "unitPrice": 30 }
     *   ]
     * }
     *
     * @param request 批量出库请求
     * @return 已确认的出库单
     */
// @PostMapping("/batch-confirm")
    public ApiResponse<StockOut> batchConfirm(@RequestBody BatchOutboundRequest request) {
        try {
            StockOut stockOut = stockOutService.batchConfirm(request);
            return success(stockOut, "批量出库完成，出库单号: " + stockOut.getOutCode());
        } catch (Exception e) {
            return exception(e, "批量出库");
        }
    }

    // endregion

    // region 按制剂查处方出库明细与直接批量出库
    // ===================================
    // 按制剂查处方出库明细与直接批量出库
    // ===================================

    /**
     * 按制剂ID查询处方信息明细（含物料库存批次明细）
     * <p>
     * 根据制剂ID查询其处方明细（物料编码、品名、分类、单位、标准处方、应出数量），
     * 并带出每个物料可出库的合格库存批次明细（出库批次、可用库存、单价、金额、库存状态，FIFO排序）
     * </p>
     *
     * 示例请求：
     * GET /api/stockout/preparation-detail?preparationId=1&multiplier=1
     *
     * @param preparationId 制剂ID（必填）
     * @param multiplier    生产倍数（选填，默认1倍，应出数量=标准处方×倍数）
     * @return ApiResponse&lt;List&lt;PlanDetailItemDto&gt;&gt; 处方明细列表（含序号、可用库存批次、单价、金额、库存状态）
     */
    @GetMapping("/preparation-detail")
    public ApiResponse<List<PlanDetailItemDto>> getPreparationDetail(
            @RequestParam Long preparationId,
            @RequestParam(required = false) BigDecimal multiplier) {
        try {
            List<PlanDetailItemDto> items = stockOutService.getPreparationDetail(preparationId, multiplier);
            return success(items);
        } catch (Exception e) {
            return exception(e, "按制剂查处方出库明细");
        }
    }

    /**
     * 表单+物料明细形式直接批量出库（无需确认，直接扣减库存）
     * <p>
     * 请求体为出库单主表单（可携带 planId/planNumber 记录关联生产计划）+ 物料明细列表，
     * 一次事务内自动生成出库单号、保存主表与明细，并直接联动扣减库存批次、写入库存流水，
     * 任一明细库存不足时整体回滚（单据、明细、库存均不落库）
     * </p>
     *
     * 示例请求：
     * POST /api/stockout/direct-outbound
     * Content-Type: application/json
     * {
     *   "outType": "生产领料出库",
     *   "planId": 3,
     *   "planNumber": "Plan20260710001",
     *   "prodUnitId": 1,
     *   "outDate": "2026-08-11T10:30:00",
     *   "remark": "按制剂处方直接批量出库",
     *   "details": [
     *     { "stockId": 12, "itemCode": "Y0084", "itemName": "甘草", "categoryName": "原料",
     *       "unitName": "kg", "batchNumber": "HG20260723", "quantity": 0.42, "unitPrice": 30 },
     *     { "stockId": 15, "itemCode": "Y0085", "itemName": "黄芪", "categoryName": "原料",
     *       "unitName": "kg", "batchNumber": "HQ20260720", "quantity": 1.0, "unitPrice": 45 }
     *   ]
     * }
     *
     * @param request 出库单主表单+物料明细列表（明细须携带 stockId 定位库存批次）
     * @return ApiResponse&lt;StockOutWithDetailsDto&gt; 直接出库后的出库单（含明细与成功条数）
     */
    @PostMapping("/direct-outbound")
    public ApiResponse<StockOutWithDetailsDto> directOutbound(@RequestBody StockOutWithDetailsDto request) {
        try {
            // 校验出库类型与明细
            if (!StringUtils.hasText(request.getOutType())) {
                throw new RuntimeException("出库类型不能为空");
            }
            if (request.getDetails() == null || request.getDetails().isEmpty()) {
                throw new RuntimeException("出库明细不能为空");
            }
            // 单号为空时自动生成
            if (!StringUtils.hasText(request.getOutCode())) {
                request.setOutCode(stockOutService.generateStockOutCode());
            }
            // 直接出库：保存主表+明细并联动扣减库存写流水（无需确认）
            stockOutService.addStockOut(request, request.getDetails());
            request.setSuccessCount(request.getDetails().size());
            return success(request, "批量出库完成，出库单号: " + request.getOutCode());
        } catch (Exception e) {
            return exception(e, "直接批量出库");
        }
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

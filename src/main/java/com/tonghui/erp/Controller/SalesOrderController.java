package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.SalesOrderCreateDto;
import com.tonghui.erp.Data.Entity.SalesOrder;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Service.SalesOrderService;
import com.tonghui.erp.Service.StockOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 成品出库台账控制器
 * <p>
 * 提供成品出库台账的增删改查、高级查询、统计汇总以及作废等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                     │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/sales-order                         │ GET    │ 分页查询台账列表             │
 * │ 2  │ /api/sales-order/{id}                    │ GET    │ 获取台账详情                 │
 * │ 3  │ /api/sales-order                         │ POST   │ 新增台账（开单）             │
 * │ 4  │ /api/sales-order/{id}                    │ PUT    │ 修改台账（仅备注可改）       │
 * │ 5  │ /api/sales-order/{id}                    │ DELETE │ 删除台账                     │
 * │ 6  │ /api/sales-order/search                  │ GET    │ 高级查询台账（支持多条件）   │
 * │ 7  │ /api/sales-order/generate-code           │ GET    │ 自动生成台账单号             │
 * │ 8  │ /api/sales-order/statistics              │ GET    │ 统计汇总（总单数/总金额）    │
 * │ 9  │ /api/sales-order/{id}/void               │ PUT    │ 作废台账                     │
 * └────┴──────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/sales-order")
public class SalesOrderController extends BaseCrudController<SalesOrder, SalesOrder, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 成品出库台账服务
     */
    @Autowired
    private SalesOrderService salesOrderService;

    /**
     * 出库单服务（用于联动创建草稿出库单）
     */
    @Autowired
    private StockOutService stockOutService;

    // endregion

    // region 基础CRUD实现
    // ===================================
    // 基础CRUD实现
    // ===================================

    @Override
    protected PagedResult<SalesOrder> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Page<SalesOrder> pageResult = salesOrderService.querySalesOrders(null, null, null, null, null, safePageIndex, safePageSize);

        PagedResult<SalesOrder> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected SalesOrder getDataById(Long id) {
        return salesOrderService.getById(id);
    }

    @Override
    protected SalesOrder doCreate(SalesOrder salesOrder) {
        // 自动生成成品出库单号
        salesOrder.setSalesOrderCode(salesOrderService.generateCode());
        // 校验单号唯一性
        if (!salesOrderService.isCodeUnique(salesOrder.getSalesOrderCode(), null)) {
            throw new RuntimeException("成品出库单号已存在");
        }
        // 自动计算金额
        if (salesOrder.getQuantity() != null && salesOrder.getUnitPrice() != null) {
            salesOrder.setAmount(salesOrder.getUnitPrice().multiply(new BigDecimal(salesOrder.getQuantity())));
        }
        // 默认状态为已开单
        if (salesOrder.getStatus() == null) {
            salesOrder.setStatus("已开单");
        }
        salesOrderService.save(salesOrder);
        return salesOrder;
    }

    /**
     * 新增成品出库台账（联动创建草稿出库单）
     * <p>
     * 重写基类创建方法，接收台账信息和出库明细列表，
     * 创建台账后自动联动创建草稿出库单（状态为"草稿"，不扣库存）
     * </p>
     *
     * 示例请求：
     * POST /api/sales-order
     * Content-Type: application/json
     * {
     *   "salesOrderDate": "2026-09-21T15:00:00",
     *   "customerId": 1,
     *   "preparationName": "益肾壮骨丸",
     *   "preparationCode": "Z00347",
     *   "batchNumber": "P20260901",
     *   "quantity": 250,
     *   "unitPrice": 21.01,
     *   "stockOutDetails": [
     *     { "stockId": 1, "quantity": 250, "unitPrice": 21.01 }
     *   ]
     * }
     *
     * @param dto 台账创建请求（含出库明细）
     * @return ApiResponse&lt;SalesOrder&gt; 创建结果（含关联的出库单号）
     */
    @PostMapping
    public ApiResponse<SalesOrder> create(@RequestBody SalesOrderCreateDto dto) {
        try {
            if (dto == null) {
                return error("请求参数不能为空");
            }
            // 1. 创建 SalesOrder
            SalesOrder salesOrder = new SalesOrder();
            salesOrder.setSalesOrderDate(dto.getSalesOrderDate());
            salesOrder.setCustomerId(dto.getCustomerId());
            salesOrder.setPreparationName(dto.getPreparationName());
            salesOrder.setPreparationCode(dto.getPreparationCode());
            salesOrder.setBatchNumber(dto.getBatchNumber());
            salesOrder.setQuantity(dto.getQuantity());
            salesOrder.setUnitPrice(dto.getUnitPrice());
            salesOrder.setRemark(dto.getRemark());
            SalesOrder created = doCreate(salesOrder);

            // 2. 联动创建草稿出库单（如有出库明细）
            if (dto.getStockOutDetails() != null && !dto.getStockOutDetails().isEmpty()) {
                StockOut stockOut = new StockOut();
                stockOut.setOutType("成品出库");
                stockOut.setCustomerId(created.getCustomerId());
                stockOut.setRelatedOrder(created.getSalesOrderCode());
                stockOut.setOutDate(created.getSalesOrderDate());
                stockOut.setTotalAmount(created.getAmount());
                stockOut.setRemark("由成品出库台账自动创建");

                StockOut draft = stockOutService.createDraftOutbound(stockOut);

                // 保存出库明细
                for (StockOutDetail detail : dto.getStockOutDetails()) {
                    detail.setOutId(draft.getOutId());
                }
                stockOutService.addStockOutDetails(dto.getStockOutDetails());

                // 回写出库单号到 SalesOrder
                created.setRelatedOutCode(draft.getOutCode());
                salesOrderService.updateById(created);
            }

            return success(created, "创建成功");
        } catch (Exception ex) {
            return exception(ex, "创建成品出库台账");
        }
    }

    @Override
    protected SalesOrder doUpdate(Long id, SalesOrder salesOrder) {
        salesOrder.setSalesOrderId(id);
        salesOrderService.updateById(salesOrder);
        return salesOrder;
    }

    @Override
    protected boolean doDelete(Long id) {
        return salesOrderService.removeById(id);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询成品出库台账（支持多条件 + 分页）
     * <p>
     * 可选查询条件：keyword（模糊匹配单号/制剂）、status（已开单/已出库/已作废）、
     * customerId（客户ID）、startDate/endDate（出库日期区间）
     * </p>
     *
     * 示例请求：
     * GET /api/sales-order/search?pageIndex=0&pageSize=20&keyword=CPCK
     * GET /api/sales-order/search?pageIndex=0&pageSize=20&status=已开单
     * GET /api/sales-order/search?pageIndex=0&pageSize=20&customerId=1
     * GET /api/sales-order/search?pageIndex=0&pageSize=20&startDate=2026-09-01T00:00:00&endDate=2026-09-30T23:59:59
     *
     * @param keyword    关键字（模糊匹配单号/制剂，可选）
     * @param status     状态（已开单/已出库/已作废，可选）
     * @param customerId 客户ID（可选）
     * @param startDate  出库日期起始（可选）
     * @param endDate    出库日期结束（可选）
     * @param pageIndex  页码（从0开始）
     * @param pageSize   每页大小
     * @return ApiResponse&lt;PagedResult&lt;SalesOrder&gt;&gt; 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<SalesOrder>> searchSalesOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<SalesOrder> pageResult = salesOrderService.querySalesOrders(keyword, status, customerId, startDate, endDate, safePageIndex, safePageSize);

            PagedResult<SalesOrder> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "高级查询成品出库台账");
        }
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 自动生成成品出库单号
     * <p>
     * 根据系统规则自动生成唯一的成品出库单号（格式CPCK-YYYYMMDD-NNN）
     * </p>
     *
     * 示例请求：
     * GET /api/sales-order/generate-code
     *
     * @return ApiResponse&lt;String&gt; 成品出库单号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateSalesOrderCode() {
        try {
            String code = salesOrderService.generateCode();
            return success(code, "成品出库单号生成成功");
        } catch (Exception ex) {
            return exception(ex, "生成成品出库单号");
        }
    }

    // endregion

    // region 统计汇总
    // ===================================
    // 统计汇总
    // ===================================

    /**
     * 统计成品出库汇总数据
     * <p>
     * 按筛选条件汇总显示成品出库总单数、成品出库总金额
     * </p>
     *
     * 示例请求：
     * GET /api/sales-order/statistics
     * GET /api/sales-order/statistics?status=已出库
     * GET /api/sales-order/statistics?customerId=1&startDate=2026-09-01T00:00:00
     *
     * @param keyword    关键字（可选）
     * @param status     状态（可选）
     * @param customerId 客户ID（可选）
     * @param startDate  出库日期起始（可选）
     * @param endDate    出库日期结束（可选）
     * @return ApiResponse&lt;Map&gt; 统计结果（totalCount: 总单数, totalAmount: 总金额）
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            BigDecimal[] stats = salesOrderService.getStatistics(keyword, status, customerId, startDate, endDate);
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", stats[0]);
            result.put("totalAmount", stats[1]);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "统计成品出库汇总");
        }
    }

    // endregion

    // region 作废
    // ===================================
    // 作废
    // ===================================

    /**
     * 作废成品出库台账
     * <p>
     * 将台账状态设置为"已作废"，同时把出库管理中对应"待确认"的出库单置为"已取消"
     * 已作废的台账不能再次作废
     * </p>
     *
     * 示例请求：
     * PUT /api/sales-order/1/void
     *
     * @param id 台账ID
     * @return ApiResponse&lt;Boolean&gt; 作废结果
     */
    @PutMapping("/{id}/void")
    public ApiResponse<Boolean> voidSalesOrder(@PathVariable Long id) {
        try {
            boolean result = salesOrderService.voidOrder(id);
            if (result) {
                return success(true, "作废成功");
            } else {
                return error("作废失败，台账不存在或已作废");
            }
        } catch (Exception ex) {
            return exception(ex, "作废成品出库台账");
        }
    }

    // endregion
}

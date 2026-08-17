package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.StockDetailItemDto;
import com.tonghui.erp.Data.Entity.CheckOrder;
import com.tonghui.erp.Service.CheckOrderService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 盘点管理控制器
 * <p>
 * 提供盘点单列表查询、盘点仓库/库存明细查询、提交盘点
 * （自动计算差异、调整库存并生成盘点流水）及盘点单详情查询
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬────────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                               │
 * ├────┼──────────────────────────────────────────────┼────────┼────────────────────────────────────┤
 * │ 1  │ /api/warehouse/check-orders                  │ GET    │ 查询盘点单列表（分页）             │
 * │ 2  │ /api/warehouse/check-orders/warehouses       │ GET    │ 获取盘点仓库列表                   │
 * │ 3  │ /api/warehouse/check-orders/stock-details    │ GET    │ 获取仓库库存明细（盘点用）         │
 * │ 4  │ /api/warehouse/check-orders                  │ POST   │ 提交盘点                           │
 * │ 5  │ /api/warehouse/check-orders/{id}             │ GET    │ 查询盘点单详情                     │
 * └────┴──────────────────────────────────────────────┴────────┴────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/warehouse/check-orders")
public class CheckOrderController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 盘点单服务
     */
    @Autowired
    private CheckOrderService checkOrderService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 查询盘点单列表（分页，支持仓库筛选与关键词搜索）
     *
     * 示例请求：
     * GET /api/warehouse/check-orders?warehouse=原料仓&keyword=PD-20260817&pageIndex=0&pageSize=20
     *
     * @param warehouse 仓库名称筛选（可选）
     * @param keyword   搜索关键词（盘点单号/物料名称，可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量（默认20）
     * @return ApiResponse&lt;PagedResult&lt;CheckOrder&gt;&gt; 分页结果（主表信息列表）
     */
    @GetMapping
    public ApiResponse<PagedResult<CheckOrder>> list(
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<CheckOrder> page = checkOrderService.queryCheckOrders(
                    warehouse, keyword, safePageIndex, safePageSize);

            PagedResult<CheckOrder> result = new PagedResult<>();
            result.setItems(page.getRecords());
            result.setTotalCount(page.getTotal());
            result.setPageIndex(safePageIndex);
            result.setPageSize(safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询盘点单列表");
        }
    }

    /**
     * 获取盘点仓库列表
     *
     * 示例请求：
     * GET /api/warehouse/check-orders/warehouses
     *
     * @return ApiResponse&lt;List&lt;String&gt;&gt; 仓库名称列表
     */
    @GetMapping("/warehouses")
    public ApiResponse<List<String>> warehouses() {
        try {
            return success(checkOrderService.getWarehouseList());
        } catch (Exception ex) {
            return exception(ex, "获取仓库列表");
        }
    }

    /**
     * 获取仓库库存明细（盘点用）
     *
     * 示例请求：
     * GET /api/warehouse/check-orders/stock-details?warehouse=原料仓&showZero=false&keyword=甘草
     *
     * @param warehouse 仓库名称（必填）
     * @param showZero  是否显示零库存（可选，默认false）
     * @param keyword   搜索关键词（物料名称/批号/编码，可选）
     * @return ApiResponse&lt;List&lt;StockDetailItemDto&gt;&gt; 库存明细列表（含库存标识、系统库存）
     */
    @GetMapping("/stock-details")
    public ApiResponse<List<StockDetailItemDto>> stockDetails(
            @RequestParam String warehouse,
            @RequestParam(required = false) Boolean showZero,
            @RequestParam(required = false) String keyword) {
        try {
            return success(checkOrderService.getStockDetails(warehouse, showZero, keyword));
        } catch (Exception ex) {
            return exception(ex, "获取仓库库存明细");
        }
    }

    /**
     * 查询盘点单详情（含明细）
     *
     * 示例请求：
     * GET /api/warehouse/check-orders/1
     *
     * @param id 盘点单ID
     * @return ApiResponse&lt;CheckOrderDetailDto&gt; 盘点单详情（主表+明细）
     */
    @GetMapping("/{id}")
    public ApiResponse<CheckOrderDetailDto> detail(@PathVariable Long id) {
        try {
            return success(checkOrderService.getCheckOrderDetail(id));
        } catch (Exception ex) {
            return exception(ex, "查询盘点单详情");
        }
    }

    // endregion

    // region 提交盘点
    // ===================================
    // 提交盘点
    // ===================================

    /**
     * 提交盘点
     * <p>
     * 自动计算差异（实盘-系统）并生成盘点结果（盘盈/盘亏/盘平），
     * 对有差异的物料调整库存并生成盘点调整流水
     * </p>
     *
     * 示例请求：
     * POST /api/warehouse/check-orders
     * Content-Type: application/json
     * {
     *   "warehouse": "原料仓",
     *   "items": [
     *     {
     *       "inventoryKey": "Y0084_原料仓_HG20260501",
     *       "actualStock": 4.800
     *     }
     *   ]
     * }
     *
     * @param dto 盘点请求体：
     *            <ul>
     *              <li>warehouse：盘点仓库名称（必填）</li>
     *              <li>items：盘点明细（必填），每项含 inventoryKey（库存标识：编码_仓库_批号）与 actualStock（实盘数量，不能为负数）</li>
     *            </ul>
     * @return ApiResponse&lt;CheckOrder&gt; 创建后的盘点单（含单号与统计结果）
     */
    @PostMapping
    public ApiResponse<CheckOrder> create(@RequestBody CheckOrderCreateDto dto) {
        try {
            CheckOrder order = checkOrderService.createCheckOrder(dto);
            return success(order, "盘点成功");
        } catch (Exception ex) {
            return exception(ex, "提交盘点");
        }
    }

    // endregion
}
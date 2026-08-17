package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Warehouse.AvailableOutOrderDto;
import com.tonghui.erp.Common.Dto.Warehouse.OutOrderMaterialDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderDetailDto;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Service.ReturnOrderService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 退库管理控制器
 * <p>
 * 提供退库单列表查询、可退库出库单/出库明细查询、
 * 新增退库（校验可退额度、回增库存并生成退库流水）及退库单详情查询
 * </p>
 *
 * 接口清单：
 * ┌────┬───────────────────────────────────────────────────────┬────────┬─────────────────────────────────┐
 * │ #  │ 接口                                                  │ 方法   │ 说明                            │
 * ├────┼───────────────────────────────────────────────────────┼────────┼─────────────────────────────────┤
 * │ 1  │ /api/warehouse/return-orders                          │ GET    │ 查询退库单列表（分页）          │
 * │ 2  │ /api/warehouse/return-orders/available-out-orders     │ GET    │ 获取可退库的出库单列表          │
 * │ 3  │ /api/warehouse/return-orders/out-order-materials/{no} │ GET    │ 获取出库单物料明细（含可退数量）│
 * │ 4  │ /api/warehouse/return-orders                          │ POST   │ 新增退库单                      │
 * │ 5  │ /api/warehouse/return-orders/{id}                     │ GET    │ 查询退库单详情                  │
 * └────┴───────────────────────────────────────────────────────┴────────┴─────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/warehouse/return-orders")
public class ReturnOrderController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 退库单服务
     */
    @Autowired
    private ReturnOrderService returnOrderService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 查询退库单列表（分页，支持关键词搜索）
     *
     * 示例请求：
     * GET /api/warehouse/return-orders?keyword=TK-20260817&pageIndex=0&pageSize=20
     *
     * @param keyword   搜索关键词（退库单号/出库单号/物料名称，可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量（默认20）
     * @return ApiResponse&lt;PagedResult&lt;ReturnOrder&gt;&gt; 分页结果（主表信息列表）
     */
    @GetMapping
    public ApiResponse<PagedResult<ReturnOrder>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<ReturnOrder> page = returnOrderService.queryReturnOrders(
                    keyword, safePageIndex, safePageSize);

            PagedResult<ReturnOrder> result = new PagedResult<>();
            result.setItems(page.getRecords());
            result.setTotalCount(page.getTotal());
            result.setPageIndex(safePageIndex);
            result.setPageSize(safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询退库单列表");
        }
    }

    /**
     * 获取可退库的出库单列表
     * <p>
     * 仅展示"生产领料出库"且仍有可退额度的出库单
     * </p>
     *
     * 示例请求：
     * GET /api/warehouse/return-orders/available-out-orders
     *
     * @return ApiResponse&lt;List&lt;AvailableOutOrderDto&gt;&gt; 可退库出库单列表（含可退总量）
     */
    @GetMapping("/available-out-orders")
    public ApiResponse<List<AvailableOutOrderDto>> availableOutOrders() {
        try {
            return success(returnOrderService.getAvailableOutOrders());
        } catch (Exception ex) {
            return exception(ex, "获取可退库出库单");
        }
    }

    /**
     * 获取出库单物料明细（含可退数量）
     *
     * 示例请求：
     * GET /api/warehouse/return-orders/out-order-materials/CK-20260701-001
     *
     * @param outOrderNo 出库单号（路径参数）
     * @return ApiResponse&lt;List&lt;OutOrderMaterialDto&gt;&gt; 物料明细列表（含出库数量、已退数量、可退数量）
     */
    @GetMapping("/out-order-materials/{outOrderNo}")
    public ApiResponse<List<OutOrderMaterialDto>> outOrderMaterials(@PathVariable String outOrderNo) {
        try {
            return success(returnOrderService.getOutOrderMaterials(outOrderNo));
        } catch (Exception ex) {
            return exception(ex, "获取出库单物料明细");
        }
    }

    /**
     * 查询退库单详情（含明细）
     *
     * 示例请求：
     * GET /api/warehouse/return-orders/1
     *
     * @param id 退库单ID
     * @return ApiResponse&lt;ReturnOrderDetailDto&gt; 退库单详情（主表+明细）
     */
    @GetMapping("/{id}")
    public ApiResponse<ReturnOrderDetailDto> detail(@PathVariable Long id) {
        try {
            return success(returnOrderService.getReturnOrderDetail(id));
        } catch (Exception ex) {
            return exception(ex, "查询退库单详情");
        }
    }

    // endregion

    // region 新增退库
    // ===================================
    // 新增退库
    // ===================================

    /**
     * 新增退库单
     * <p>
     * 校验退库数量不超过可退数量（出库数量-已退数量）后回增库存
     * （原库存行不存在时按出库明细重建），并生成退库流水
     * </p>
     *
     * 示例请求：
     * POST /api/warehouse/return-orders
     * Content-Type: application/json
     * {
     *   "outOrderNo": "CK-20260701-001",
     *   "remark": "生产余料退回",
     *   "items": [
     *     {
     *       "inventoryKey": "Y0084_原料仓_HG20260501",
     *       "returnQuantity": 0.300
     *     }
     *   ]
     * }
     *
     * @param dto 退库请求体：
     *            <ul>
     *              <li>outOrderNo：关联出库单号（必填）</li>
     *              <li>remark：备注（可选）</li>
     *              <li>items：退库物料明细（必填），每项含 inventoryKey（库存标识：编码_仓库_批号）与 returnQuantity（本次退库数量，必须大于0且不超过可退数量）</li>
     *            </ul>
     * @return ApiResponse&lt;ReturnOrder&gt; 创建后的退库单（含单号与汇总数据）
     */
    @PostMapping
    public ApiResponse<ReturnOrder> create(@RequestBody ReturnOrderCreateDto dto) {
        try {
            ReturnOrder order = returnOrderService.createReturnOrder(dto);
            return success(order, "退库成功");
        } catch (Exception ex) {
            return exception(ex, "新增退库单");
        }
    }

    // endregion
}
package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Service.PurchaseOrderItemsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购订单明细控制器
 * <p>
 * 提供采购订单明细的增删改查操作，用于采购订单中物料明细的管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/purchase-order-items            │ POST  │ 新增采购订单明细                    │
 * │ 2  │ /api/purchase-order-items            │ PUT   │ 更新采购订单明细                    │
 * │ 3  │ /api/purchase-order-items/{itemId}   │ DELETE│ 删除采购订单明细                    │
 * │ 4  │ /api/purchase-order-items/{itemId}   │ GET   │ 根据ID查询采购订单明细              │
 * │ 5  │ /api/purchase-order-items/order/{orderId} │ GET │ 根据采购订单ID查询所有明细      │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/purchase-order-items")
public class PurchaseOrderItemsController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 采购订单明细服务
     */
    @Autowired
    private PurchaseOrderItemsService purchaseOrderItemsService;

    // endregion

    // region 采购订单明细CRUD接口
    // ===================================
    // 采购订单明细CRUD接口
    // ===================================

    /**
     * 新增采购订单明细
     *
     * 示例请求：
     * POST /api/purchase-order-items
     * Content-Type: application/json
     * {
     *   "orderId": 1,
     *   "itemId": 101,
     *   "quantity": 100,
     *   "purchaseQuantity": 100,
     *   "actualArrivalQty": 95,
     *   "unitPrice": 25.50,
     *   "amount": 2422.50,
     *   "invoiceNo": "FP20260701",
     *   "supplier": "某某药材公司",
     *   "standardDosage": 0.021
     * }
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return ApiResponse&lt;PurchaseOrderItems&gt; 新增的采购订单明细（含ID）
     */
    @PostMapping
    public ApiResponse<PurchaseOrderItems> addPurchaseOrderItem(@RequestBody PurchaseOrderItems purchaseOrderItems) {
        try {
            purchaseOrderItemsService.addPurchaseOrderItem(purchaseOrderItems);
            return success(purchaseOrderItems, "新增采购订单明细成功");
        } catch (Exception e) {
            return exception(e, "新增采购订单明细");
        }
    }

    /**
     * 更新采购订单明细
     *
     * 示例请求：
     * PUT /api/purchase-order-items
     * Content-Type: application/json
     * {
     *   "itemId": 101,
     *   "quantity": 200,
     *   "actualArrivalQty": 190,
     *   "unitPrice": 25.50,
     *   "amount": 4845.00,
     *   "invoiceNo": "FP20260702",
     *   "supplier": "某某药材公司"
     * }
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return ApiResponse&lt;Boolean&gt; 操作结果
     */
    @PutMapping
    public ApiResponse<Boolean> updatePurchaseOrderItem(@RequestBody PurchaseOrderItems purchaseOrderItems) {
        try {
            boolean result = purchaseOrderItemsService.updatePurchaseOrderItem(purchaseOrderItems);
            return success(result, "更新采购订单明细成功");
        } catch (Exception e) {
            return exception(e, "更新采购订单明细");
        }
    }

    /**
     * 删除采购订单明细
     *
     * 示例请求：
     * DELETE /api/purchase-order-items/101
     *
     * @param itemId 采购订单明细ID（路径参数）
     * @return ApiResponse&lt;Boolean&gt; 操作结果
     */
    @DeleteMapping("/{itemId}")
    public ApiResponse<Boolean> deletePurchaseOrderItem(@PathVariable("itemId") Long itemId) {
        try {
            boolean result = purchaseOrderItemsService.deletePurchaseOrderItem(itemId);
            return success(result, "删除采购订单明细成功");
        } catch (Exception e) {
            return exception(e, "删除采购订单明细");
        }
    }

    /**
     * 根据ID查询采购订单明细
     *
     * 示例请求：
     * GET /api/purchase-order-items/101
     *
     * @param itemId 采购订单明细ID（路径参数）
     * @return ApiResponse&lt;PurchaseOrderItems&gt; 采购订单明细实体
     */
    @GetMapping("/{itemId}")
    public ApiResponse<PurchaseOrderItems> getPurchaseOrderItemById(@PathVariable("itemId") Long itemId) {
        try {
            PurchaseOrderItems item = purchaseOrderItemsService.getPurchaseOrderItemById(itemId);
            if (item == null) {
                return error("未找到指定的采购订单明细");
            }
            return success(item);
        } catch (Exception e) {
            return exception(e, "查询采购订单明细");
        }
    }

    /**
     * 根据采购订单ID查询所有明细
     *
     * 示例请求：
     * GET /api/purchase-order-items/order/1
     *
     * @param orderId 采购订单ID（路径参数）
     * @return ApiResponse&lt;List&lt;PurchaseOrderItems&gt;&gt; 采购订单明细集合
     */
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<PurchaseOrderItems>> getPurchaseOrderItemsByOrderId(@PathVariable("orderId") Long orderId) {
        try {
            List<PurchaseOrderItems> items = purchaseOrderItemsService.getPurchaseOrderItemsByOrderId(orderId);
            return success(items);
        } catch (Exception e) {
            return exception(e, "查询采购订单明细列表");
        }
    }

    // endregion
}

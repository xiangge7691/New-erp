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
 */
@RestController
@RequestMapping("/api/purchase-order-items")
public class PurchaseOrderItemsController extends BaseController {

    @Autowired
    private PurchaseOrderItemsService purchaseOrderItemsService;

    /**
     * 新增采购订单明细
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return 操作结果
     */
    @PostMapping
    public ApiResponse<Boolean> addPurchaseOrderItem(@RequestBody PurchaseOrderItems purchaseOrderItems) {
        try {
            boolean result = purchaseOrderItemsService.addPurchaseOrderItem(purchaseOrderItems);
            return success(result, "新增采购订单明细成功");
        } catch (Exception e) {
            return exception(e, "新增采购订单明细");
        }
    }

    /**
     * 更新采购订单明细
     *
     * @param purchaseOrderItems 采购订单明细实体
     * @return 操作结果
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
     * @param itemId 采购订单明细ID
     * @return 操作结果
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
     * @param itemId 采购订单明细ID
     * @return 采购订单明细实体
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
     * @param orderId 采购订单ID
     * @return 采购订单明细集合
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
}

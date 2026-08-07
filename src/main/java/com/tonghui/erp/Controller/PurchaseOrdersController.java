package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseOrdersWithItemsDto;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Service.PurchaseOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购订单控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────┬────────┬─────────────────────────────────┐
 * │ #  │ 接口                                     │ 方法   │ 说明                            │
 * ├────┼──────────────────────────────────────────┼────────┼─────────────────────────────────┤
 * │ 1  │ /api/purchase-orders                     │ GET    │ 分页查询采购订单列表            │
 * │ 2  │ /api/purchase-orders/{id}                │ GET    │ 获取采购订单详情                │
 * │ 3  │ /api/purchase-orders                     │ POST   │ 新增采购订单                    │
 * │ 4  │ /api/purchase-orders/{id}                │ PUT    │ 修改采购订单                    │
 * │ 5  │ /api/purchase-orders/{id}                │ DELETE │ 删除采购订单                    │
 * │ 6  │ /api/purchase-orders/search              │ GET    │ 高级查询采购订单（多条件+分页）  │
 * │ 7  │ /api/purchase-orders/search-with-details │ GET    │ 高级查询采购订单（含明细子表）   │
 * │ 8  │ /api/purchase-orders/enabled             │ GET    │ 查询所有启用状态的采购订单       │
 * │ 9  │ /api/purchase-orders/{id}/status/{status}│ POST   │ 启用/停用采购订单               │
 * └────┴──────────────────────────────────────────┴────────┴─────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrdersController extends BaseCrudController<PurchaseOrders, PurchaseOrders, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 采购订单服务
     */
    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    // endregion

    // region CRUD基础方法实现
    // ===================================
    // CRUD基础方法实现
    // ===================================

    @Override
    protected PagedResult<PurchaseOrders> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        PurchaseOrders purchaseOrders = new PurchaseOrders();
        Page<PurchaseOrders> pageResult = purchaseOrdersService.queryPurchaseOrders(purchaseOrders, null, safePageIndex, safePageSize);

        PagedResult<PurchaseOrders> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected PurchaseOrders getDataById(Long id) {
        return purchaseOrdersService.getPurchaseOrderById(id);
    }

    @Override
    protected PurchaseOrders doCreate(PurchaseOrders purchaseOrders) {
        purchaseOrdersService.addPurchaseOrder(purchaseOrders);
        return purchaseOrders;
    }

    @Override
    protected PurchaseOrders doUpdate(Long id, PurchaseOrders purchaseOrders) {
        purchaseOrders.setId(id);
        purchaseOrdersService.updatePurchaseOrder(purchaseOrders);
        return purchaseOrders;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            purchaseOrdersService.deletePurchaseOrder(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询采购订单（支持多条件 + 分页）
     *
     * 示例请求：
     * GET /api/purchase-orders/search?pageIndex=1&pageSize=20&keyword=CG2025&warehouse=原料库&status=1
     *
     * @param purchaseOrders 查询条件（自动从query参数映射）
     * @param keyword 关键字（对采购编号、采购标题进行模糊匹配，可选）
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return PagedResult&lt;PurchaseOrders&gt; 分页查询结果
     */
    @GetMapping("/search")
    public PagedResult<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam int pageIndex,
                                                           @RequestParam int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 获取分页结果
        Page<PurchaseOrders> pageResult = purchaseOrdersService.queryPurchaseOrders(purchaseOrders, keyword, safePageIndex, safePageSize);

        // 转换为统一的PagedResult格式
        PagedResult<PurchaseOrders> pagedResult = new PagedResult<>();
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
     * 高级查询采购订单（包含明细子表）
     *
     * 示例请求：
     * GET /api/purchase-orders/search-with-details?pageIndex=1&pageSize=20&keyword=CG2025
     *
     * @param purchaseOrders 查询条件（自动从query参数映射）
     * @param keyword 关键字（对采购编号、采购标题进行模糊匹配，可选）
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;PurchaseOrdersWithItemsDto&gt;&gt; 分页结果（包含明细）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<PurchaseOrdersWithItemsDto>> searchWithDetails(PurchaseOrders purchaseOrders,
                                                                                    @RequestParam(required = false) String keyword,
                                                                                    @RequestParam int pageIndex,
                                                                                    @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<PurchaseOrdersWithItemsDto> result = purchaseOrdersService.searchWithDetails(purchaseOrders, keyword, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion

    // region 特殊业务方法
    // ===================================
    // 特殊业务方法
    // ===================================

    /**
     * 查询所有启用状态的采购订单
     *
     * 示例请求：
     * GET /api/purchase-orders/enabled
     *
     * @return PagedResult&lt;PurchaseOrders&gt; 采购订单集合
     */
    @GetMapping("/enabled")
    public PagedResult<PurchaseOrders> getEnabledPurchaseOrders() {
        // 这里使用分页查询，但返回所有启用状态的采购订单
        PurchaseOrders purchaseOrders = new PurchaseOrders();
        purchaseOrders.setStatus(1); // 启用状态

        Page<PurchaseOrders> pageResult = purchaseOrdersService.queryPurchaseOrders(purchaseOrders, null, 0, Integer.MAX_VALUE);

        PagedResult<PurchaseOrders> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(0);
        pagedResult.setPageSize(pageResult.getRecords().size());

        return pagedResult;
    }

    /**
     * 启用/停用采购订单
     *
     * 示例请求：
     * POST /api/purchase-orders/1/status/1
     *
     * @param id 采购订单ID
     * @param status 状态：1启用，0停用
     * @return ApiResponse&lt;Boolean&gt; 操作结果
     */
    @PostMapping("/{id}/status/{status}")
    public ApiResponse<Boolean> togglePurchaseOrderStatus(@PathVariable Long id, @PathVariable Object status) {
        try {
            PurchaseOrders purchaseOrders = purchaseOrdersService.getPurchaseOrderById(id);
            if (purchaseOrders != null) {
                purchaseOrders.setStatus(status);
                purchaseOrdersService.updatePurchaseOrder(purchaseOrders);
                return success(true, "状态更新成功");
            }
            return error("未找到对应的采购订单");
        } catch (Exception e) {
            return exception(e, "更新采购订单状态");
        }
    }

    // endregion
}

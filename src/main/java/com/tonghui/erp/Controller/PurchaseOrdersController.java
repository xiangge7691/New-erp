package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Service.PurchaseOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购订单控制器
 */
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrdersController extends BaseCrudController<PurchaseOrders, PurchaseOrders, Long> {

    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    @Override
    protected PagedResult<PurchaseOrders> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用PurchaseOrdersService的queryPurchaseOrders方法进行查询
        PurchaseOrders purchaseOrders = new PurchaseOrders();
        Page<PurchaseOrders> pageResult = purchaseOrdersService.queryPurchaseOrders(purchaseOrders, safePageIndex, safePageSize);

        // 转换为PagedResult
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
        purchaseOrders.setId(id.intValue()); // 转换为Integer类型
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

    // #region 高级查询

    /**
     * 高级查询采购订单（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - purchaseNumber：模糊匹配
     * - warehouse：模糊匹配
     * - status：状态过滤
     * - processingDate：处理日期（大于等于）
     * - expectedDeliveryDate：预计到货日期（小于等于）
     *
     * 示例请求：
     * GET /purchase-orders/search?pageIndex=1&pageSize=20&purchaseNumber=PO&warehouse=原料库&status=1
     *
     * @param purchaseOrders 查询条件（自动从query参数映射）
     * @param pageIndex      页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public PagedResult<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders,
                                                           @RequestParam int pageIndex,
                                                           @RequestParam int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 获取分页结果
        Page<PurchaseOrders> pageResult = purchaseOrdersService.queryPurchaseOrders(purchaseOrders, safePageIndex, safePageSize);

        // 转换为统一的PagedResult格式
        PagedResult<PurchaseOrders> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    // #endregion

    // #region 特殊业务方法

    /**
     * 查询所有启用状态的采购订单
     *
     * @return 采购订单集合
     */
    @GetMapping("/enabled")
    public PagedResult<PurchaseOrders> getEnabledPurchaseOrders() {
        // 这里使用分页查询，但返回所有启用状态的采购订单
        PurchaseOrders purchaseOrders = new PurchaseOrders();
        purchaseOrders.setStatus(1); // 启用状态

        Page<PurchaseOrders> pageResult = purchaseOrdersService.queryPurchaseOrders(purchaseOrders, 0, Integer.MAX_VALUE);

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
     * @param id     采购订单ID
     * @param status 状态：1启用，0停用
     * @return 操作结果
     */
    @PostMapping("/{id}/status/{status}")
    public boolean togglePurchaseOrderStatus(@PathVariable Long id, @PathVariable Object status) {
        PurchaseOrders purchaseOrders = purchaseOrdersService.getPurchaseOrderById(id);
        if (purchaseOrders != null) {
            purchaseOrders.setStatus(status);
            purchaseOrdersService.updatePurchaseOrder(purchaseOrders);
            return true;
        }
        return false;
    }

    // #endregion
}

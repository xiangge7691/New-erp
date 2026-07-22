package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseSuppliersWithDetailsDto;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;
import com.tonghui.erp.Service.PurchaseSuppliersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 采购供应商控制器
 * <p>
 * 提供采购供应商的CRUD操作、高级查询、按编号查询、启用状态查询及状态切换等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/purchase-suppliers            │ GET    │ 分页查询采购供应商列表       │
 * │ 2  │ /api/purchase-suppliers/{id}       │ GET    │ 获取采购供应商详情           │
 * │ 3  │ /api/purchase-suppliers            │ POST   │ 新增采购供应商               │
 * │ 4  │ /api/purchase-suppliers/{id}       │ PUT    │ 修改采购供应商               │
 * │ 5  │ /api/purchase-suppliers/{id}       │ DELETE │ 删除采购供应商               │
 * │ 6  │ /api/purchase-suppliers/search     │ GET    │ 高级查询采购供应商           │
 * │ 7  │ /api/purchase-suppliers/search-with-details │ GET │ 带子表查询采购供应商   │
 * │ 8  │ /api/purchase-suppliers/number/{supplierNumber} │ GET │ 根据编号查询采购供应商 │
 * │ 9  │ /api/purchase-suppliers/enabled    │ GET    │ 查询所有启用状态的采购供应商 │
 * │ 10 │ /api/purchase-suppliers/{id}/status/{status} │ POST │ 启用/停用采购供应商   │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/purchase-suppliers")
public class PurchaseSuppliersController extends BaseCrudController<PurchaseSuppliers, PurchaseSuppliers, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 采购供应商服务
     */
    @Autowired
    private PurchaseSuppliersService purchaseSuppliersService;

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    /**
     * 获取所有采购供应商数据（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    protected PagedResult<PurchaseSuppliers> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用PurchaseSuppliersService的queryPurchaseSuppliers方法进行查询
        PurchaseSuppliers purchaseSuppliers = new PurchaseSuppliers();
        Page<PurchaseSuppliers> pageResult = purchaseSuppliersService.queryPurchaseSuppliers(purchaseSuppliers, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<PurchaseSuppliers> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    /**
     * 根据ID获取采购供应商
     *
     * @param id 采购供应商ID
     * @return 采购供应商实体
     */
    @Override
    protected PurchaseSuppliers getDataById(Long id) {
        return purchaseSuppliersService.getPurchaseSupplierById(id);
    }

    /**
     * 创建采购供应商
     *
     * @param purchaseSuppliers 采购供应商信息
     * @return 创建后的采购供应商
     */
    @Override
    protected PurchaseSuppliers doCreate(PurchaseSuppliers purchaseSuppliers) {
        purchaseSuppliersService.addPurchaseSupplier(purchaseSuppliers);
        return purchaseSuppliers;
    }

    /**
     * 更新采购供应商
     *
     * @param id                 采购供应商ID
     * @param purchaseSuppliers  采购供应商信息
     * @return 更新后的采购供应商
     */
    @Override
    protected PurchaseSuppliers doUpdate(Long id, PurchaseSuppliers purchaseSuppliers) {
        purchaseSuppliers.setId(id);
        purchaseSuppliersService.updatePurchaseSupplier(purchaseSuppliers);
        return purchaseSuppliers;
    }

    /**
     * 删除采购供应商
     *
     * @param id 采购供应商ID
     * @return 是否删除成功
     */
    @Override
    protected boolean doDelete(Long id) {
        try {
            purchaseSuppliersService.deletePurchaseSupplier(id);
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
     * 高级查询采购供应商（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - supplierNumber：供应商编号（模糊匹配）
     * - supplierName：供应商名称（模糊匹配）
     * - category：供应商分类（模糊匹配）
     * - contactPerson：联系人（模糊匹配）
     * - phone：联系电话（模糊匹配）
     * - email：邮箱（模糊匹配）
     * - address：地址（模糊匹配）
     * - bankAccount：银行账号（模糊匹配）
     * - bankName：银行名称（模糊匹配）
     * - status：状态过滤（精确匹配）
     *
     * 示例请求：
     * GET /api/purchase-suppliers/search?pageIndex=1&pageSize=20&supplierName=原料&contactPerson=张&status=1
     *
     * @param purchaseSuppliers 查询条件（自动从query参数映射）
     * @param pageIndex         页码
     * @param pageSize          每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<PurchaseSuppliers>> queryPurchaseSuppliers(PurchaseSuppliers purchaseSuppliers,
                                                                 @RequestParam int pageIndex,
                                                                 @RequestParam int pageSize) {
        try {
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            // 获取分页结果
            Page<PurchaseSuppliers> pageResult = purchaseSuppliersService.queryPurchaseSuppliers(purchaseSuppliers, safePageIndex, safePageSize);

            // 转换为统一的PagedResult格式
            PagedResult<PurchaseSuppliers> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询采购供应商");
        }
    }

    // endregion

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 带子表查询采购供应商（支持多条件 + 分页）
     *
     * 示例请求：
     * GET /api/purchase-suppliers/search-with-details?pageIndex=1&pageSize=20&supplierName=原料
     *
     * @param purchaseSuppliers 查询条件（自动从query参数映射）
     * @param pageIndex         页码
     * @param pageSize          每页大小
     * @return 分页结果（包含子表信息）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<PurchaseSuppliersWithDetailsDto>> searchWithDetails(PurchaseSuppliers purchaseSuppliers,
                                                                                       @RequestParam int pageIndex,
                                                                                       @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<PurchaseSuppliersWithDetailsDto> result = purchaseSuppliersService.searchWithDetails(purchaseSuppliers, safePageIndex, safePageSize);
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
     * 根据编号查询采购供应商
     *
     * 示例请求：
     * GET /api/purchase-suppliers/number/CGYS001
     *
     * @param supplierNumber 采购供应商编号
     * @return 采购供应商实体
     */
    @GetMapping("/number/{supplierNumber}")
    public ApiResponse<PurchaseSuppliers> getPurchaseSupplierByNumber(@PathVariable String supplierNumber) {
        try {
            PurchaseSuppliers result = purchaseSuppliersService.getPurchaseSupplierByNumber(supplierNumber);
            if (result == null) {
                return error("未找到对应的采购供应商");
            }
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "根据编号查询采购供应商");
        }
    }

    /**
     * 查询所有启用状态的采购供应商
     *
     * 示例请求：
     * GET /api/purchase-suppliers/enabled
     *
     * @return 采购供应商集合
     */
    @GetMapping("/enabled")
    public ApiResponse<PagedResult<PurchaseSuppliers>> getEnabledPurchaseSuppliers() {
        try {
            // 这里使用分页查询，但返回所有启用状态的采购供应商
            PurchaseSuppliers purchaseSuppliers = new PurchaseSuppliers();
            purchaseSuppliers.setStatus(1); // 启用状态

            Page<PurchaseSuppliers> pageResult = purchaseSuppliersService.queryPurchaseSuppliers(purchaseSuppliers, 0, Integer.MAX_VALUE);

            PagedResult<PurchaseSuppliers> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(0);
            pagedResult.setPageSize(pageResult.getRecords().size());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询启用状态的采购供应商");
        }
    }

    /**
     * 启用/停用采购供应商
     *
     * 示例请求：
     * POST /api/purchase-suppliers/1/status/1
     *
     * @param id     采购供应商ID
     * @param status 状态：1启用，0停用
     * @return 操作结果
     */
    @PostMapping("/{id}/status/{status}")
    public ApiResponse<Boolean> togglePurchaseSupplierStatus(@PathVariable Long id, @PathVariable Object status) {
        try {
            PurchaseSuppliers purchaseSuppliers = purchaseSuppliersService.getPurchaseSupplierById(id);
            if (purchaseSuppliers != null) {
                purchaseSuppliers.setStatus(status);
                purchaseSuppliersService.updatePurchaseSupplier(purchaseSuppliers);
                return success(true, "状态更新成功");
            }
            return error("未找到对应的采购供应商");
        } catch (Exception ex) {
            return exception(ex, "更新采购供应商状态");
        }
    }

    // endregion
}

package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.ProductionUnitWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Service.ProductionUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产单位控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬────────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                               │
 * ├────┼──────────────────────────────────────────────┼────────┼────────────────────────────────────┤
 * │ 1  │ /api/production_unit                         │ GET    │ 分页查询生产单位列表               │
 * │ 2  │ /api/production_unit/{id}                    │ GET    │ 获取生产单位详情                   │
 * │ 3  │ /api/production_unit                         │ POST   │ 新增生产单位                       │
 * │ 4  │ /api/production_unit/{id}                    │ PUT    │ 修改生产单位                       │
 * │ 5  │ /api/production_unit/{id}                    │ DELETE │ 删除生产单位                       │
 * │ 6  │ /api/production_unit/search                  │ GET    │ 高级查询生产单位（多条件+分页）     │
 * │ 7  │ /api/production_unit/search-with-details     │ GET    │ 高级查询生产单位（含子表）          │
 * │ 8  │ /api/production_unit/{id}/invoice            │ POST   │ 添加发票信息                       │
 * │ 9  │ /api/production_unit/{id}/invoices           │ GET    │ 获取发票信息列表                   │
 * │ 10 │ /api/production_unit/invoice/{invoiceId}     │ DELETE │ 删除发票信息                       │
 * │ 11 │ /api/production_unit/code/{prodUnitCode}     │ GET    │ 根据编码查询生产单位               │
 * │ 12 │ /api/production_unit/enabled                 │ GET    │ 查询所有启用状态的生产单位          │
 * │ 13 │ /api/production_unit/{id}/status/{status}    │ POST   │ 启用/停用生产单位                  │
 * └────┴──────────────────────────────────────────────┴────────┴────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/production_unit")
public class ProductionUnitController extends BaseCrudController<ProductionUnit, ProductionUnit, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 生产单位服务
     */
    @Autowired
    private ProductionUnitService productionUnitService;

    // endregion

    // region CRUD基础方法实现
    // ===================================
    // CRUD基础方法实现
    // ===================================

    @Override
    protected PagedResult<ProductionUnit> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        ProductionUnit productionUnit = new ProductionUnit();
        Page<ProductionUnit> pageResult = productionUnitService.queryProductionUnits(productionUnit, safePageIndex, safePageSize);

        PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected ProductionUnit getDataById(Long id) {
        return productionUnitService.getProductionUnitById(id);
    }

    @Override
    protected ProductionUnit doCreate(ProductionUnit productionUnit) {
        productionUnitService.addProductionUnit(productionUnit);
        return productionUnit;
    }

    @Override
    protected ProductionUnit doUpdate(Long id, ProductionUnit productionUnit) {
        productionUnit.setProdUnitId(id);
        productionUnitService.updateProductionUnit(productionUnit);
        return productionUnit;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            productionUnitService.deleteProductionUnit(id);
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
     * 高级查询生产单位（支持多条件 + 分页）
     *
     * 示例请求：
     * GET /api/production_unit/search?pageIndex=1&pageSize=20&keyword=原料药&prodUnitManager=张&prodUnitStatus=1&createdTimeStart=2023-01-01T00:00:00&createdTimeEnd=2023-12-31T23:59:59
     *
     * @param productionUnit 查询条件（自动从query参数映射）
     * @param keyword 关键字（对生产单位编码、生产单位名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;ProductionUnit&gt;&gt; 分页查询结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<ProductionUnit>> queryProductionUnits(ProductionUnit productionUnit,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                         @RequestParam int pageIndex,
                                                                         @RequestParam int pageSize) {
        try {
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            // 获取分页结果，将时间参数传递给service方法
            Page<ProductionUnit> pageResult = productionUnitService.queryProductionUnits(productionUnit, keyword, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);

            // 转换为统一的pagedResult格式
            PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "搜索生产单位");
        }
    }

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 高级查询生产单位（包含发票和材料文件子表）
     *
     * 示例请求：
     * GET /api/production_unit/search-with-details?pageIndex=1&pageSize=20&keyword=原料药
     *
     * @param productionUnit 查询条件（自动从query参数映射）
     * @param keyword 关键字（对生产单位编码、生产单位名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;ProductionUnitWithDetailsDto&gt;&gt; 分页结果（包含子表）
     */
// @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ProductionUnitWithDetailsDto>> searchWithDetails(ProductionUnit productionUnit,
                                                                                     @RequestParam(required = false) String keyword,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                                     @RequestParam int pageIndex,
                                                                                     @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ProductionUnitWithDetailsDto> result = productionUnitService.searchWithDetails(productionUnit, keyword, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion

    // region 发票信息操作接口
    // ===================================
    // 发票信息操作接口
    // ===================================

    /**
     * 为生产单位添加发票信息（单条）
     *
     * 示例请求：
     * POST /api/production_unit/1/invoice
     * Content-Type: application/json
     * "发票内容"
     *
     * @param prodUnitId 生产单位ID
     * @param prodInvoiceInfo 发票信息
     * @return ApiResponse&lt;ProdUnitInvoice&gt; 添加的发票信息
     */
    @PostMapping("/{id}/invoice")
    public ApiResponse<ProdUnitInvoice> addProdUnitInvoice(@PathVariable("id")Long prodUnitId, @RequestBody String prodInvoiceInfo) {
        try {
            ProdUnitInvoice invoice = productionUnitService.addProdUnitInvoice(prodUnitId, prodInvoiceInfo);
            return success(invoice, "添加发票信息成功");
        } catch (Exception e) {
            return exception(e, "添加发票信息失败");
        }
    }

    /**
     * 批量为生产单位添加发票信息（先删除原有发票，再批量插入）
     *
     * 示例请求：
     * POST /api/production_unit/1/invoices
     * Content-Type: application/json
     * ["发票内容1", "发票内容2", "发票内容3"]
     *
     * @param prodUnitId 生产单位ID
     * @param prodInvoiceInfos 发票信息列表
     * @return ApiResponse&lt;List&lt;ProdUnitInvoice&gt;&gt; 添加的发票列表
     */
    @PostMapping("/{id}/invoices")
    public ApiResponse<List<ProdUnitInvoice>> addProdUnitInvoices(@PathVariable("id")Long prodUnitId, @RequestBody List<String> prodInvoiceInfos) {
        try {
            List<ProdUnitInvoice> invoices = productionUnitService.addProdUnitInvoices(prodUnitId, prodInvoiceInfos);
            return success(invoices, "批量添加发票信息成功");
        } catch (Exception e) {
            return exception(e, "批量添加发票信息失败");
        }
    }

    /**
     * 获取生产单位的发票信息列表
     *
     * 示例请求：
     * GET /api/production_unit/1/invoices
     *
     * @param prodUnitId 生产单位ID
     * @return ApiResponse&lt;List&lt;ProdUnitInvoice&gt;&gt; 发票信息列表
     */
    @GetMapping("/{id}/invoices")
    public ApiResponse<List<ProdUnitInvoice>> getProdUnitInvoices(@PathVariable("id")Long prodUnitId) {
        try {
            List<ProdUnitInvoice> invoices = productionUnitService.getProdUnitInvoices(prodUnitId);
            return success(invoices);
        } catch (Exception e) {
            return exception(e, "获取发票信息失败");
        }
    }

    /**
     * 删除发票信息
     *
     * 示例请求：
     * DELETE /api/production_unit/invoice/1
     *
     * @param prodInvoiceId 发票信息ID
     * @return ApiResponse&lt;Boolean&gt; 操作结果
     */
    @DeleteMapping("/invoice/{invoiceId}")
    public ApiResponse<Boolean> deleteProdUnitInvoice(@PathVariable("invoiceId")Long prodInvoiceId) {
        try {
            boolean result = productionUnitService.deleteProdUnitInvoice(prodInvoiceId);
            return success(result, "删除发票信息成功");
        } catch (Exception e) {
            return exception(e, "删除发票信息失败");
        }
    }

    // endregion

    // region 特殊业务方法
    // ===================================
    // 特殊业务方法
    // ===================================

    /**
     * 根据编码查询生产单位
     *
     * 示例请求：
     * GET /api/production_unit/code/PU001
     *
     * @param prodUnitCode 生产单位编码
     * @return ApiResponse&lt;ProductionUnit&gt; 生产单位实体
     */
    @GetMapping("/code/{prodUnitCode}")
    public ApiResponse<ProductionUnit> getProductionUnitByCode(@PathVariable String prodUnitCode) {
        try {
            ProductionUnit unit = productionUnitService.getProductionUnitByCode(prodUnitCode);
            if (unit == null) {
                return error("未找到对应生产单位");
            }
            return success(unit);
        } catch (Exception e) {
            return exception(e, "查询生产单位失败");
        }
    }

    /**
     * 查询所有启用状态的生产单位
     *
     * 示例请求：
     * GET /api/production_unit/enabled
     *
     * @return ApiResponse&lt;PagedResult&lt;ProductionUnit&gt;&gt; 生产单位集合
     */
// @GetMapping("/enabled")
    public ApiResponse<PagedResult<ProductionUnit>>getEnabledProductionUnits() {
        try {
            // 这里使用分页查询，但返回所有启用status的生产单位
            ProductionUnit productionUnit = new ProductionUnit();
            productionUnit.setProdUnitStatus(1); // 启用status

            Page<ProductionUnit> pageResult = productionUnitService.queryProductionUnits(productionUnit, 0,Integer.MAX_VALUE);

            PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(0);
            pagedResult.setPageSize(pageResult.getRecords().size());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询启用生产单位失败");
        }
    }

    /**
     * 启用/停用生产单位
     *
     * 示例请求：
     * POST /api/production_unit/1/status/1
     *
     * @param id 生产单位ID
     * @param status 状态：1启用，0停用
     * @return ApiResponse&lt;Boolean&gt; 操作结果
     */
// @PostMapping("/{id}/status/{status}")
    public ApiResponse<Boolean>toggleProductionUnitStatus(@PathVariable Long id, @PathVariable Integer status) {
        try {
            ProductionUnit productionUnit = productionUnitService.getProductionUnitById(id);
            if (productionUnit != null) {
                productionUnit.setProdUnitStatus(status);
                productionUnitService.updateProductionUnit(productionUnit);
                return success(true, "更新状态成功");
            }
            return error("未找到对应生产单位");
        } catch (Exception e) {
            return exception(e, "更新状态失败");
        }
    }

    // endregion
}
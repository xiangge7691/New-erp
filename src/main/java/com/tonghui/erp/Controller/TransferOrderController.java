package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Warehouse.MaterialBatchDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderListItemDto;
import com.tonghui.erp.Common.Dto.Warehouse.WarehouseMaterialDto;
import com.tonghui.erp.Data.Entity.TransferOrder;
import com.tonghui.erp.Service.TransferOrderService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调拨管理控制器
 * <p>
 * 提供仓库间物料调拨的调拨单列表查询、仓库/物料/批次信息查询、
 * 新增调拨（自动更新两端库存并生成调拨出入库流水）及调拨单详情查询
 * </p>
 *
 * 接口清单：
 * ┌────┬─────────────────────────────────────────────────┬────────┬──────────────────────────────────────┐
 * │ #  │ 接口                                            │ 方法   │ 说明                                 │
 * ├────┼─────────────────────────────────────────────────┼────────┼──────────────────────────────────────┤
 * │ 1  │ /api/warehouse/transfer-orders                  │ GET    │ 查询调拨单列表（分页）               │
 * │ 2  │ /api/warehouse/transfer-orders/warehouses       │ GET    │ 获取仓库列表                         │
 * │ 3  │ /api/warehouse/transfer-orders/materials        │ GET    │ 获取仓库可用物料列表（支持关键词）   │
 * │ 4  │ /api/warehouse/transfer-orders/material-batches │ GET    │ 获取物料批次库存详情                 │
 * │ 5  │ /api/warehouse/transfer-orders                  │ POST   │ 新增调拨单                           │
 * │ 6  │ /api/warehouse/transfer-orders/{id}             │ GET    │ 查询调拨单详情                       │
 * └────┴─────────────────────────────────────────────────┴────────┴──────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/warehouse/transfer-orders")
public class TransferOrderController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 调拨单服务
     */
    @Autowired
    private TransferOrderService transferOrderService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 查询调拨单列表（分页，支持类型筛选与关键词搜索）
     * <p>
     * 类型筛选：调拨出库（按调出仓库模糊匹配keyword）/调拨入库（按调入仓库模糊匹配keyword），
     * keyword 为空时 type 筛选不生效；keyword 支持调拨单号/物料名称模糊匹配
     * </p>
     *
     * 示例请求：
     * GET /api/warehouse/transfer-orders?type=调拨出库&keyword=DB-20260817&pageIndex=0&pageSize=20
     *
     * @param type      类型筛选（调拨出库/调拨入库，可选）
     * @param keyword   搜索关键词（调拨单号/物料名称，可选）
     * @param startTime 创建时间起始（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime   创建时间结束（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量（默认20）
     * @return ApiResponse&lt;PagedResult&lt;TransferOrder&gt;&gt; 分页结果（主表信息列表）
     */
    @GetMapping
    public ApiResponse<PagedResult<TransferOrder>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<TransferOrder> page = transferOrderService.queryTransferOrders(
                    type, keyword, startTime, endTime, safePageIndex, safePageSize);

            PagedResult<TransferOrder> result = new PagedResult<>();
            result.setItems(page.getRecords());
            result.setTotalCount(page.getTotal());
            result.setPageIndex(safePageIndex);
            result.setPageSize(safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询调拨单列表");
        }
    }

    /**
     * 获取仓库列表
     *
     * 示例请求：
     * GET /api/warehouse/transfer-orders/warehouses
     *
     * @return ApiResponse&lt;List&lt;String&gt;&gt; 仓库名称列表
     */
    @GetMapping("/warehouses")
    public ApiResponse<List<String>> warehouses() {
        try {
            return success(transferOrderService.getWarehouseList());
        } catch (Exception ex) {
            return exception(ex, "获取仓库列表");
        }
    }

    /**
     * 获取仓库可用物料列表（含批次数量）
     *
     * 示例请求：
     * GET /api/warehouse/transfer-orders/materials?warehouse=原料仓&keyword=当归
     *
     * @param warehouse 仓库名称（必填）
     * @param keyword   搜索关键词（按物料编码/物料名称模糊匹配，可选）
     * @return ApiResponse&lt;List&lt;WarehouseMaterialDto&gt;&gt; 物料列表（按物料编码分组，含批次数量）
     */
    @GetMapping("/materials")
    public ApiResponse<List<WarehouseMaterialDto>> materials(
            @RequestParam String warehouse,
            @RequestParam(required = false) String keyword) {
        try {
            return success(transferOrderService.getWarehouseMaterials(warehouse, keyword));
        } catch (Exception ex) {
            return exception(ex, "获取仓库可用物料");
        }
    }

    /**
     * 获取物料批次库存详情
     *
     * 示例请求：
     * GET /api/warehouse/transfer-orders/material-batches?warehouse=原料仓&materialCode=Y0084
     *
     * @param warehouse    仓库名称（必填）
     * @param materialCode 物料编码（必填）
     * @return ApiResponse&lt;List&lt;MaterialBatchDto&gt;&gt; 批次详情列表（含库存标识、库存数量、单价）
     */
    @GetMapping("/material-batches")
    public ApiResponse<List<MaterialBatchDto>> materialBatches(
            @RequestParam String warehouse,
            @RequestParam String materialCode) {
        try {
            return success(transferOrderService.getMaterialBatches(warehouse, materialCode));
        } catch (Exception ex) {
            return exception(ex, "获取物料批次详情");
        }
    }

    /**
     * 查询调拨单详情（含明细）
     *
     * 示例请求：
     * GET /api/warehouse/transfer-orders/1
     *
     * @param id 调拨单ID
     * @return ApiResponse&lt;TransferOrderDetailDto&gt; 调拨单详情（主表+明细）
     */
    @GetMapping("/{id}")
    public ApiResponse<TransferOrderDetailDto> detail(@PathVariable Long id) {
        try {
            return success(transferOrderService.getTransferOrderDetail(id));
        } catch (Exception ex) {
            return exception(ex, "查询调拨单详情");
        }
    }

    // endregion

    // region 新增调拨
    // ===================================
    // 新增调拨
    // ===================================

    /**
     * 新增调拨单
     * <p>
     * 校验仓库与库存后自动扣减调出库存、增加调入库存（无同批次则新增库存记录），
     * 生成调拨出库与调拨入库两条库存流水，并保存调拨单主表与明细
     * </p>
     *
     * 示例请求：
     * POST /api/warehouse/transfer-orders
     * Content-Type: application/json
     * {
     *   "fromWarehouse": "原料仓",
     *   "toWarehouse": "耒阳制剂室",
     *   "remark": "原料仓调拨至耒阳制剂室",
     *   "items": [
     *     {
     *       "inventoryKey": "Y0084_原料仓_HG20260501",
     *       "transferQuantity": 1.000
     *     }
     *   ]
     * }
     *
     * @param dto 调拨单请求体：
     *            <ul>
     *              <li>fromWarehouse：调出仓库名称（必填）</li>
     *              <li>toWarehouse：调入仓库名称（必填，不能与调出仓库相同）</li>
     *              <li>remark：备注（可选）</li>
     *              <li>items：调拨物料明细（必填），每项含 inventoryKey（库存标识：编码_仓库_批号）与 transferQuantity（调拨数量，必须大于0且不超过调出库存）</li>
     *            </ul>
     * @return ApiResponse&lt;TransferOrder&gt; 创建后的调拨单（含单号与汇总数据）
     */
    @PostMapping
    public ApiResponse<TransferOrder> create(@RequestBody TransferOrderCreateDto dto) {
        try {
            TransferOrder order = transferOrderService.createTransferOrder(dto);
            return success(order, "调拨成功");
        } catch (Exception ex) {
            return exception(ex, "新增调拨单");
        }
    }

    // endregion
}
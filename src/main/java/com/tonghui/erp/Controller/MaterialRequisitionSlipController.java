package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.MaterialRequisitionSlip.MaterialRequisitionSlipWithDetailsDto;
import com.tonghui.erp.Common.Dto.MaterialRequisitionSlip.MaterialRequisitionSlipWithDetailsRequest;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import com.tonghui.erp.Service.MaterialRequisitionSlipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物料领料单控制器
 * <p>
 * 提供领料单的CRUD操作、带子表查询、作废、生产计划联动、单号生成等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                           │ 方法   │ 说明                                │
 * ├────┼────────────────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/material-requisition-slip                 │ GET    │ 分页查询领料单列表                   │
 * │ 2  │ /api/material-requisition-slip/{id}            │ GET    │ 获取领料单详情（含明细）             │
 * │ 3  │ /api/material-requisition-slip/withDetails     │ POST   │ 新增领料单（含明细+自动生成验收单）  │
 * │ 4  │ /api/material-requisition-slip/{id}/withDetails│ PUT    │ 更新领料单（仅待发放状态可编辑）     │
 * │ 5  │ /api/material-requisition-slip/{id}            │ DELETE │ 删除领料单（仅待发放状态可删除）     │
 * │ 6  │ /api/material-requisition-slip/{id}/void       │ POST   │ 作废领料单（待发放→已作废）          │
 * │ 7  │ /api/material-requisition-slip/generate-code   │ GET    │ 生成领料单号（LL-YYYYMMDD-NNN）     │
 * │ 8  │ /api/material-requisition-slip/from-production-plan/{planId} │ GET │ 根据生产计划自动带出物料明细  │
 * └────┴────────────────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/material-requisition-slip")
public class MaterialRequisitionSlipController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 领料单服务
     */
    @Autowired
    private MaterialRequisitionSlipService materialRequisitionSlipService;

    // endregion

    // region 分页查询
    // ===================================
    // 分页查询
    // ===================================

    /**
     * 分页查询领料单列表（含明细）
     *
     * 示例请求：
     * GET /api/material-requisition-slip?pageIndex=0&pageSize=20&keyword=LL&status=待发放
     *
     * @param keyword   关键字（对领料单号/生产计划标题/领料人进行模糊匹配，可选）
     * @param status    状态筛选（可选：待发放/验收中/已入库/已作废）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 领料单分页结果（含明细）
     */
    @GetMapping
    public ApiResponse<PagedResult<MaterialRequisitionSlipWithDetailsDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            List<MaterialRequisitionSlipWithDetailsDto> items =
                    materialRequisitionSlipService.querySlipsWithDetails(keyword, status, safePageIndex, safePageSize);

            // 获取总数（用于分页）
            Page<MaterialRequisitionSlip> countPage = materialRequisitionSlipService.querySlips(keyword, status, safePageIndex, safePageSize);

            PagedResult<MaterialRequisitionSlipWithDetailsDto> pagedResult = new PagedResult<>();
            pagedResult.setItems(items);
            pagedResult.setTotalCount(countPage.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize(safePageSize);

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询领料单列表");
        }
    }

    // endregion

    // region 领料单详情
    // ===================================
    // 领料单详情
    // ===================================

    /**
     * 根据ID获取领料单详情（含明细）
     *
     * 示例请求：
     * GET /api/material-requisition-slip/1
     *
     * @param id 领料单ID
     * @return 领料单详情（含明细）
     */
    @GetMapping("/{id}")
    public ApiResponse<MaterialRequisitionSlipWithDetailsDto> getById(@PathVariable Long id) {
        try {
            MaterialRequisitionSlipWithDetailsDto dto = materialRequisitionSlipService.getSlipWithDetails(id);
            if (dto == null) {
                return error("领料单不存在");
            }
            return success(dto);
        } catch (Exception ex) {
            return exception(ex, "获取领料单详情");
        }
    }

    // endregion

    // region 领料单创建与更新
    // ===================================
    // 领料单创建与更新
    // ===================================

    /**
     * 新增领料单（包含明细，自动生成货物验收单）
     *
     * 示例请求：
     * POST /api/material-requisition-slip/withDetails
     * Content-Type: application/json
     * {
     *   "slip": {
     *     "productionPlanCode": "Plan202609080001",
     *     "productionPlanTitle": "益肾壮骨丸计划单",
     *     "applicant": "张生产",
     *     "applyTime": "2026-09-08T10:00:00",
     *     "remark": "益肾壮骨丸领料"
     *   },
     *   "details": [
     *     { "materialCode": "Y0084", "materialName": "甘草", "unitName": "kg", "applyQty": 5 },
     *     { "materialCode": "Y0436", "materialName": "木瓜", "unitName": "kg", "applyQty": 6 }
     *   ]
     * }
     *
     * @param request 保存请求（slip-领料单信息，details-明细列表）
     * @return 领料单信息
     */
    @PostMapping("/withDetails")
    public ApiResponse<MaterialRequisitionSlip> createWithDetails(
            @RequestBody MaterialRequisitionSlipWithDetailsRequest request) {
        try {
            if (request == null || request.getSlip() == null) {
                return error("请求参数不能为空");
            }
            if (request.getDetails() == null || request.getDetails().isEmpty()) {
                return error("领料明细不能为空");
            }

            // 校验明细：至少一条且请领数量>0
            boolean hasInvalidDetail = request.getDetails().stream().anyMatch(d ->
                    d.getApplyQty() == null || d.getApplyQty().compareTo(BigDecimal.ZERO) <= 0);
            if (hasInvalidDetail) {
                return error("每条明细的请领数量必须大于0");
            }

            MaterialRequisitionSlip slip = materialRequisitionSlipService.addSlip(
                    request.getSlip(), request.getDetails());
            return success(slip, "领料单创建成功");
        } catch (Exception ex) {
            return exception(ex, "创建领料单");
        }
    }

    /**
     * 更新领料单（包含明细，仅待发放状态可编辑）
     *
     * 示例请求：
     * PUT /api/material-requisition-slip/1/withDetails
     * Content-Type: application/json
     * {
     *   "slip": { "applicant": "李生产", "remark": "更新备注" },
     *   "details": [
     *     { "materialCode": "Y0084", "materialName": "甘草", "unitName": "kg", "applyQty": 10 }
     *   ]
     * }
     *
     * @param id      领料单ID
     * @param request 保存请求（slip-领料单信息，details-明细列表）
     * @return 操作结果
     */
    @PutMapping("/{id}/withDetails")
    public ApiResponse<Boolean> updateWithDetails(@PathVariable Long id,
                                                   @RequestBody MaterialRequisitionSlipWithDetailsRequest request) {
        try {
            if (request == null || request.getSlip() == null) {
                return error("请求参数不能为空");
            }
            materialRequisitionSlipService.updateSlip(id, request.getSlip(), request.getDetails());
            return success(true, "领料单更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新领料单");
        }
    }

    /**
     * 删除领料单（仅待发放状态可删除）
     *
     * 示例请求：
     * DELETE /api/material-requisition-slip/1
     *
     * @param id 领料单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            materialRequisitionSlipService.deleteSlip(id);
            return success(true, "领料单删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除领料单");
        }
    }

    // endregion

    // region 状态操作
    // ===================================
    // 状态操作
    // ===================================

    /**
     * 作废领料单（待发放→已作废，联动验收单→已退货）
     *
     * 示例请求：
     * POST /api/material-requisition-slip/1/void
     *
     * @param id 领料单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/void")
    public ApiResponse<Boolean> voidSlip(@PathVariable Long id) {
        try {
            materialRequisitionSlipService.voidSlip(id);
            return success(true, "领料单已作废");
        } catch (Exception ex) {
            return exception(ex, "作废领料单");
        }
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成领料单号（格式 LL-YYYYMMDD-NNN）
     *
     * 示例请求：
     * GET /api/material-requisition-slip/generate-code
     *
     * @return 领料单号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateCode() {
        try {
            String code = materialRequisitionSlipService.generateSlipCode();
            return success(code, "生成领料单号成功");
        } catch (Exception ex) {
            return exception(ex, "生成领料单号");
        }
    }

    // endregion

    // region 生产计划联动
    // ===================================
    // 生产计划联动
    // ===================================

    /**
     * 根据生产计划自动带出物料明细（按制剂处方计算请领数量）
     * <p>
     * 请领数量 = 处方量(dosage) × 倍数(multiplier) × 计划数量(planQuantity)
     * </p>
     *
     * 示例请求：
     * GET /api/material-requisition-slip/from-production-plan/1?multiplier=1
     *
     * @param planId     生产计划ID
     * @param multiplier 倍数（可选，默认1）
     * @return 领料明细列表（已计算请领数量，前端确认后调用addSlip保存）
     */
    @GetMapping("/from-production-plan/{planId}")
    public ApiResponse<List<MaterialRequisitionSlipDetail>> generateDetailsFromProductionPlan(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "1") BigDecimal multiplier) {
        try {
            List<MaterialRequisitionSlipDetail> details =
                    materialRequisitionSlipService.generateDetailsFromProductionPlan(planId, multiplier);
            return success(details, "物料明细生成成功");
        } catch (Exception ex) {
            return exception(ex, "根据生产计划生成物料明细");
        }
    }

    // endregion
}

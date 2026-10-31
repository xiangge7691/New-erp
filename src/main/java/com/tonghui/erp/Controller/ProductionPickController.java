package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickBatchViewDto;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickIssueRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickSaveRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Service.ProductionPickService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生产领料单控制器
 * <p>
 * 提供生产领料单的分页查询、详情、按生产任务预填、新增/修改/删除、
 * 发料（生成草稿出库单）与作废等接口。
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                                 │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/production-pick                                 │ GET    │ 分页查询生产领料单           │
 * │ 2  │ /api/production-pick/{id}                            │ GET    │ 获取领料单详情（含明细）     │
 * │ 3  │ /api/production-pick/from-work-order/{workOrderId}   │ GET    │ 按生产任务生成预填数据       │
 * │ 4  │ /api/production-pick/{id}/available-batches          │ GET    │ 查询明细可用库存批次         │
 * │ 5  │ /api/production-pick                                 │ POST   │ 新增领料单（含明细）         │
 * │ 6  │ /api/production-pick/{id}                            │ PUT    │ 修改领料单（仅待发料）       │
 * │ 7  │ /api/production-pick/{id}/issue                      │ POST   │ 发料（生成草稿出库单）       │
 * │ 8  │ /api/production-pick/{id}/cancel                     │ POST   │ 作废领料单                   │
 * │ 9  │ /api/production-pick/{id}                            │ DELETE │ 删除领料单（仅待发料）       │
 * │ 10 │ /api/production-pick/generate-code                   │ GET    │ 生成领料单号                 │
 * └────┴──────────────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/production-pick")
public class ProductionPickController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 生产领料单服务
     */
    @Autowired
    private ProductionPickService productionPickService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询生产领料单（含明细）
     *
     * 示例请求：
     * GET /api/production-pick?pageIndex=0&pageSize=20&keyword=SCLL&status=待发料&workOrderId=1&startDate=2026-10-01&endDate=2026-10-31
     *
     * @param keyword      关键字（领料单号/生产任务号/生产任务名/领料人/用途，可选）
     * @param status       状态（待发料/已发料/已作废，可选）
     * @param workOrderId  关联生产任务ID（可选）
     * @param stockOutCode 关联出库单号（可选）
     * @param startDate    领料时间起始日期（yyyy-MM-dd，可选）
     * @param endDate      领料时间结束日期（yyyy-MM-dd，可选）
     * @param pageIndex    页码（从0开始）
     * @param pageSize     每页数量
     * @return 分页结果（含明细与出库单状态）
     */
    @GetMapping
    public ApiResponse<PagedResult<ProductionPickWithDetailsDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) String stockOutCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : pageSize;
            PagedResult<ProductionPickWithDetailsDto> result = productionPickService.queryPicks(
                    keyword, status, workOrderId, stockOutCode, startDate, endDate, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询生产领料单列表");
        }
    }

    /**
     * 根据ID获取生产领料单详情（含明细）
     *
     * 示例请求：
     * GET /api/production-pick/1
     *
     * @param id 领料单ID
     * @return 领料单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductionPickWithDetailsDto> getById(@PathVariable Long id) {
        try {
            ProductionPickWithDetailsDto dto = productionPickService.getPickWithDetails(id);
            if (dto == null) {
                return error("生产领料单不存在");
            }
            return success(dto);
        } catch (Exception ex) {
            return exception(ex, "获取生产领料单详情");
        }
    }

    /**
     * 按生产任务生成领料预填数据（不落库）
     *
     * 示例请求：
     * GET /api/production-pick/from-work-order/1?multiplier=1
     *
     * @param workOrderId 生产任务ID
     * @param multiplier  处方倍数（可选，默认1）
     * @return 预填的领料单主表与明细
     */
    @GetMapping("/from-work-order/{workOrderId}")
    public ApiResponse<ProductionPickWithDetailsDto> getFromWorkOrder(
            @PathVariable Long workOrderId,
            @RequestParam(defaultValue = "1") BigDecimal multiplier) {
        try {
            ProductionPickWithDetailsDto dto = productionPickService.buildPrefillFromWorkOrder(workOrderId, multiplier);
            return success(dto, "生产领料预填数据生成成功");
        } catch (Exception ex) {
            return exception(ex, "按生产任务生成领料预填数据");
        }
    }

    /**
     * 查询某张领料单每条明细的可用库存批次
     *
     * 示例请求：
     * GET /api/production-pick/1/available-batches
     *
     * @param id 领料单ID
     * @return 明细可用批次列表
     */
    @GetMapping("/{id}/available-batches")
    public ApiResponse<List<ProductionPickBatchViewDto>> getAvailableBatches(@PathVariable Long id) {
        try {
            return success(productionPickService.getAvailableBatches(id));
        } catch (Exception ex) {
            return exception(ex, "查询生产领料可用库存批次");
        }
    }

    // endregion

    // region 写操作
    // ===================================
    // 写操作
    // ===================================

    /**
     * 新增生产领料单（含明细）
     *
     * 示例请求：
     * POST /api/production-pick
     * Content-Type: application/json
     * {
     *   "pick": {
     *     "workOrderId": 1,
     *     "applicant": "张生产",
     *     "pickTime": "2026-10-31T10:00:00",
     *     "purpose": "益肾壮骨丸（20261031）",
     *     "remark": "生产领料"
     *   },
     *   "details": [
     *     { "materialCode": "Y0084", "materialName": "甘草", "unitName": "kg", "applyQty": 5 },
     *     { "materialCode": "Y0436", "materialName": "木瓜", "unitName": "kg", "applyQty": 6 }
     *   ]
     * }
     *
     * @param request 保存请求（pick-主表，details-明细）
     * @return 保存后的领料单
     */
    @PostMapping
    public ApiResponse<ProductionPick> create(@RequestBody ProductionPickSaveRequest request) {
        try {
            if (request == null || request.getPick() == null) {
                return error("请求参数不能为空");
            }
            if (request.getDetails() == null || request.getDetails().isEmpty()) {
                return error("领料明细不能为空");
            }
            ProductionPick pick = productionPickService.createPick(request.getPick(), request.getDetails());
            return success(pick, "生产领料单创建成功");
        } catch (Exception ex) {
            return exception(ex, "创建生产领料单");
        }
    }

    /**
     * 修改生产领料单（含明细，仅待发料状态可修改）
     *
     * 示例请求：
     * PUT /api/production-pick/1
     * Content-Type: application/json
     * {
     *   "pick": { "applicant": "李生产", "remark": "更新备注" },
     *   "details": [
     *     { "materialCode": "Y0084", "materialName": "甘草", "unitName": "kg", "applyQty": 10 }
     *   ]
     * }
     *
     * @param id      领料单ID
     * @param request 保存请求
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody ProductionPickSaveRequest request) {
        try {
            if (request == null || request.getPick() == null) {
                return error("请求参数不能为空");
            }
            productionPickService.updatePick(id, request.getPick(), request.getDetails());
            return success(true, "生产领料单更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新生产领料单");
        }
    }

    /**
     * 发料确认（生成“生产领料出库”草稿出库单，不扣库存）
     *
     * 示例请求：
     * POST /api/production-pick/1/issue
     * Content-Type: application/json
     * {
     *   "reviewer": "张三",
     *   "issuer": "李四",
     *   "details": [
     *     { "detailId": 10, "actualQty": 5, "stockId": 100 },
     *     { "detailId": 11, "actualQty": 6 }
     *   ]
     * }
     *
     * @param id      领料单ID
     * @param request 发料请求
     * @return 发料后的领料单
     */
    @PostMapping("/{id}/issue")
    public ApiResponse<ProductionPick> issue(@PathVariable Long id, @RequestBody ProductionPickIssueRequest request) {
        try {
            ProductionPick pick = productionPickService.issuePick(id, request);
            return success(pick, "发料成功，已生成生产领料出库草稿单");
        } catch (Exception ex) {
            return exception(ex, "生产领料发料");
        }
    }

    /**
     * 作废生产领料单
     * <p>
     * 已发料时：关联出库单为草稿则删除草稿单；已出库则调用出库取消回滚库存
     * </p>
     *
     * 示例请求：
     * POST /api/production-pick/1/cancel
     *
     * @param id 领料单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<Boolean> cancel(@PathVariable Long id) {
        try {
            productionPickService.cancelPick(id);
            return success(true, "生产领料单已作废");
        } catch (Exception ex) {
            return exception(ex, "作废生产领料单");
        }
    }

    /**
     * 删除生产领料单（仅待发料状态）
     *
     * 示例请求：
     * DELETE /api/production-pick/1
     *
     * @param id 领料单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            productionPickService.deletePick(id);
            return success(true, "生产领料单删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除生产领料单");
        }
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成生产领料单号（格式 SCLL-YYYYMMDD-NNN）
     *
     * 示例请求：
     * GET /api/production-pick/generate-code
     *
     * @return 领料单号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateCode() {
        try {
            return success(productionPickService.generatePickCode(), "生成生产领料单号成功");
        } catch (Exception ex) {
            return exception(ex, "生成生产领料单号");
        }
    }

    // endregion
}

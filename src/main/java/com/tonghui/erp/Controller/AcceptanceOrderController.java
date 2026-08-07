package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceActionRequest;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsRequest;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Service.AcceptanceOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货物验收控制器
 * <p>
 * 提供验收单的CRUD操作、高级查询、带子表查询、明细管理、单号生成及状态流转
 * （确认到货/初验/检验/重新收货）等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────┬────────┬────────────────────────────────┐
 * │ #  │ 接口                                     │ 方法   │ 说明                           │
 * ├────┼──────────────────────────────────────────┼────────┼────────────────────────────────┤
 * │ 1  │ /api/acceptance                         │ GET    │ 分页查询验收单列表             │
 * │ 2  │ /api/acceptance/{id}                    │ GET    │ 获取验收单详情                 │
 * │ 3  │ /api/acceptance/withDetails             │ POST   │ 创建验收单（包含明细）         │
 * │ 4  │ /api/acceptance/{id}/withDetails        │ PUT    │ 更新验收单（包含明细）         │
 * │ 5  │ /api/acceptance/{id}                    │ DELETE │ 删除验收单（已入库禁止删除）   │
 * │ 6  │ /api/acceptance/search                  │ GET    │ 高级查询验收单（状态/来源筛选）│
 * │ 7  │ /api/acceptance/search-with-details     │ GET    │ 带子表查询验收单               │
 * │ 8  │ /api/acceptance/{id}/details            │ GET    │ 获取验收单明细列表             │
 * │ 9  │ /api/acceptance/detail                  │ PUT    │ 更新验收明细（批号/单价）      │
 * │ 10 │ /api/acceptance/detail/{id}             │ DELETE │ 删除验收明细                   │
 * │ 11 │ /api/acceptance/generateCode            │ GET    │ 生成验收单号（YS-YYYYMMDD-NNN）│
 * │ 12 │ /api/acceptance/{id}/confirm-arrival    │ POST   │ 确认到货：运输中→到货初验      │
 * │ 13 │ /api/acceptance/{id}/inspect            │ POST   │ 初验：合格→物料检验/不合格→待退货│
 * │ 14 │ /api/acceptance/{id}/quality-check      │ POST   │ 检验：合格→已入库(库存联动)/不合格→待退货│
 * │ 15 │ /api/acceptance/{id}/re-receive         │ POST   │ 重新收货：生成新单，原单已退换 │
 * └────┴──────────────────────────────────────────┴────────┴────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/acceptance")
public class AcceptanceOrderController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 验收单服务
     */
    @Autowired
    private AcceptanceOrderService acceptanceOrderService;

    // endregion

    // region 分页查询
    // ===================================
    // 分页查询
    // ===================================

    /**
     * 分页查询验收单列表
     *
     * 示例请求：
     * GET /api/acceptance?pageIndex=0&pageSize=20
     *
     * @param pageRequest 分页请求参数
     * @return 验收单分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<AcceptanceOrder>> getAll(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            int pageSize = pageRequest.getPageSize() <= 0 ? 20 : pageRequest.getPageSize();
            Page<AcceptanceOrder> pageResult = acceptanceOrderService.queryAcceptances(
                    new AcceptanceOrder(), null, pageRequest.getPageIndex(), pageSize);

            PagedResult<AcceptanceOrder> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(pageRequest.getPageIndex());
            pagedResult.setPageSize((int) pageResult.getSize());
            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询验收单列表");
        }
    }

    /**
     * 根据ID获取验收单详情
     *
     * 示例请求：
     * GET /api/acceptance/1
     *
     * @param id 验收单ID
     * @return 验收单实体
     */
    @GetMapping("/{id}")
    public ApiResponse<AcceptanceOrder> getById(@PathVariable Long id) {
        try {
            AcceptanceOrder acceptance = acceptanceOrderService.getAcceptanceById(id);
            if (acceptance == null) {
                return error("验收单不存在");
            }
            return success(acceptance);
        } catch (Exception ex) {
            return exception(ex, "获取验收单");
        }
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询验收单（支持状态、来源类型筛选）
     *
     * 示例请求：
     * GET /api/acceptance/search?pageIndex=0&pageSize=20&keyword=YS2025&status=物料检验&sourceType=采购入库
     *
     * @param acceptance 查询条件（自动从query参数映射：status/sourceType/acceptanceCode等）
     * @param keyword    关键字（对验收编号、验收标题进行模糊匹配，可选）
     * @param pageIndex  页码
     * @param pageSize   每页大小
     * @return 验收单分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<AcceptanceOrder>> search(AcceptanceOrder acceptance,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam int pageIndex,
                                                            @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            Page<AcceptanceOrder> pageResult = acceptanceOrderService.queryAcceptances(
                    acceptance, keyword, safePageIndex, safePageSize);

            PagedResult<AcceptanceOrder> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());
            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询验收单");
        }
    }

    /**
     * 带子表查询验收单（包含明细）
     *
     * 示例请求：
     * GET /api/acceptance/search-with-details?pageIndex=0&pageSize=20&keyword=YS2025&status=物料检验
     *
     * @param acceptance 查询条件（自动从query参数映射）
     * @param keyword    关键字（对验收编号、验收标题进行模糊匹配，可选）
     * @param pageIndex  页码
     * @param pageSize   每页大小
     * @return 分页结果（包含明细）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<AcceptanceWithDetailsDto>> searchWithDetails(AcceptanceOrder acceptance,
                                                                                @RequestParam(required = false) String keyword,
                                                                                @RequestParam int pageIndex,
                                                                                @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<AcceptanceWithDetailsDto> result = acceptanceOrderService.searchWithDetails(
                    acceptance, keyword, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询验收单");
        }
    }

    // endregion

    // region 验收单创建与更新（包含明细）
    // ===================================
    // 验收单创建与更新（包含明细）
    // ===================================

    /**
     * 创建验收单（包含明细），验收单号自动生成（YS-YYYYMMDD-NNN）
     *
     * 示例请求：
     * POST /api/acceptance/withDetails
     * Content-Type: application/json
     * {
     *   "acceptance": { "sourceType": "采购入库", "relatedOrder": "DD-20260723-001" },
     *   "details": [
     *     { "materialCode": "Y0084", "materialName": "甘草", "materialCategory": "原料",
     *       "unitName": "kg", "standardDosage": 0.021, "quantity": 0.42,
     *       "unitPrice": 30, "batchNumber": "HG20260723", "expiryDate": "2027-07-23" }
     *   ]
     * }
     *
     * @param request 保存请求（acceptance-验收单信息，details-明细列表）
     * @return 验收单信息
     */
    @PostMapping("/withDetails")
    public ApiResponse<AcceptanceOrder> createWithDetails(@RequestBody AcceptanceWithDetailsRequest request) {
        try {
            if (request == null || request.getAcceptance() == null) {
                return error("请求参数不能为空");
            }
            AcceptanceOrder acceptance = request.getAcceptance();
            if (acceptance.getAcceptanceCode() == null || acceptance.getAcceptanceCode().isEmpty()) {
                acceptance.setAcceptanceCode(acceptanceOrderService.generateAcceptanceCode());
            }
            acceptanceOrderService.addAcceptance(acceptance, request.getDetails());
            return success(acceptance, "验收单创建成功");
        } catch (Exception ex) {
            return exception(ex, "创建验收单");
        }
    }

    /**
     * 更新验收单（包含明细，已入库禁止修改）
     *
     * 示例请求：
     * PUT /api/acceptance/1/withDetails
     * Content-Type: application/json
     * { "acceptance": { "remark": "补充备注" }, "details": [...] }
     *
     * @param id      验收单ID
     * @param request 保存请求（acceptance-验收单信息，details-明细列表）
     * @return 验收单信息
     */
    @PutMapping("/{id}/withDetails")
    public ApiResponse<AcceptanceOrder> updateWithDetails(@PathVariable Long id,
                                                          @RequestBody AcceptanceWithDetailsRequest request) {
        try {
            if (request == null || request.getAcceptance() == null) {
                return error("请求参数不能为空");
            }
            AcceptanceOrder existing = acceptanceOrderService.getAcceptanceById(id);
            if (existing == null) {
                return error("验收单不存在");
            }
            AcceptanceOrder acceptance = request.getAcceptance();
            acceptance.setAcceptanceId(id);
            acceptanceOrderService.updateAcceptance(acceptance, request.getDetails());
            return success(acceptance, "验收单更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新验收单");
        }
    }

    /**
     * 删除验收单（已入库禁止删除）
     *
     * 示例请求：
     * DELETE /api/acceptance/1
     *
     * @param id 验收单ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            AcceptanceOrder existing = acceptanceOrderService.getAcceptanceById(id);
            if (existing == null) {
                return error("验收单不存在");
            }
            acceptanceOrderService.deleteAcceptance(id);
            return success(true, "验收单删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除验收单");
        }
    }

    // endregion

    // region 验收明细管理
    // ===================================
    // 验收明细管理
    // ===================================

    /**
     * 根据验收单ID获取明细列表
     *
     * 示例请求：
     * GET /api/acceptance/1/details
     *
     * @param id 验收单ID
     * @return 明细列表
     */
    @GetMapping("/{id}/details")
    public ApiResponse<List<AcceptanceDetail>> getDetails(@PathVariable Long id) {
        try {
            List<AcceptanceDetail> details = acceptanceOrderService.getDetailsByAcceptanceId(id);
            return success(details);
        } catch (Exception ex) {
            return exception(ex, "获取验收明细列表");
        }
    }

    /**
     * 更新验收明细（批号/单价等，已入库后锁定）
     *
     * 示例请求：
     * PUT /api/acceptance/detail
     * Content-Type: application/json
     * { "detailId": 1, "batchNumber": "HG20260723", "unitPrice": 30 }
     *
     * @param detail 验收明细
     * @return 验收明细
     */
    @PutMapping("/detail")
    public ApiResponse<AcceptanceDetail> updateDetail(@RequestBody AcceptanceDetail detail) {
        try {
            if (detail == null || detail.getDetailId() == null) {
                return error("请求参数不能为空");
            }
            acceptanceOrderService.updateAcceptanceDetail(detail);
            return success(detail, "更新验收明细成功");
        } catch (Exception ex) {
            return exception(ex, "更新验收明细");
        }
    }

    /**
     * 删除验收明细
     *
     * 示例请求：
     * DELETE /api/acceptance/detail/1
     *
     * @param id 明细ID
     * @return 是否删除成功
     */
    @DeleteMapping("/detail/{id}")
    public ApiResponse<Boolean> deleteDetail(@PathVariable Long id) {
        try {
            acceptanceOrderService.deleteAcceptanceDetail(id);
            return success(true, "删除验收明细成功");
        } catch (Exception ex) {
            return exception(ex, "删除验收明细");
        }
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成验收单号（格式 YS-YYYYMMDD-NNN）
     *
     * 示例请求：
     * GET /api/acceptance/generateCode
     *
     * @return 验收单号
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            String code = acceptanceOrderService.generateAcceptanceCode();
            return success(code, "生成验收单号成功");
        } catch (Exception ex) {
            return exception(ex, "生成验收单号");
        }
    }

    // endregion

    // region 状态流转
    // ===================================
    // 状态流转
    // ===================================

    /**
     * 确认到货：运输中 → 到货初验
     *
     * 示例请求：
     * POST /api/acceptance/1/confirm-arrival
     *
     * @param id 验收单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/confirm-arrival")
    public ApiResponse<Boolean> confirmArrival(@PathVariable Long id) {
        try {
            acceptanceOrderService.confirmArrival(id);
            return success(true, "已确认到货，进入初验环节");
        } catch (Exception ex) {
            return exception(ex, "确认到货");
        }
    }

    /**
     * 初验处理：合格 → 物料检验；不合格 → 待退货
     *
     * 示例请求：
     * POST /api/acceptance/1/inspect
     * Content-Type: application/json
     * { "pass": true, "remark": "无异常" }
     *
     * @param id      验收单ID
     * @param request 初验请求（pass-是否合格，remark-备注）
     * @return 操作结果
     */
    @PostMapping("/{id}/inspect")
    public ApiResponse<Boolean> inspect(@PathVariable Long id, @RequestBody(required = false) AcceptanceActionRequest request) {
        try {
            boolean pass = request != null && Boolean.TRUE.equals(request.getPass());
            String remark = request != null ? request.getRemark() : null;
            acceptanceOrderService.inspect(id, pass, remark);
            return success(true, pass ? "初验合格，进入物料检验环节" : "初验不合格，已标记为待退货");
        } catch (Exception ex) {
            return exception(ex, "初验处理");
        }
    }

    /**
     * 检验处理：合格 → 已入库（自动增加库存并写流水）；不合格 → 待退货
     *
     * 示例请求：
     * POST /api/acceptance/1/quality-check
     * Content-Type: application/json
     * { "pass": true, "prodUnitId": 1, "remark": "合格" }
     *
     * @param id      验收单ID
     * @param request 检验请求（pass-是否合格，prodUnitId-入库仓库，remark-备注）
     * @return 操作结果
     */
    @PostMapping("/{id}/quality-check")
    public ApiResponse<Boolean> qualityCheck(@PathVariable Long id, @RequestBody(required = false) AcceptanceActionRequest request) {
        try {
            boolean pass = request != null && Boolean.TRUE.equals(request.getPass());
            Long prodUnitId = request != null ? request.getProdUnitId() : null;
            String remark = request != null ? request.getRemark() : null;
            acceptanceOrderService.qualityCheck(id, pass, prodUnitId, remark);
            return success(true, pass ? "检验合格，已入库，库存已更新" : "检验不合格，已标记为待退货");
        } catch (Exception ex) {
            return exception(ex, "检验处理");
        }
    }

    /**
     * 重新收货：基于原单生成新验收单（明细沿用原单、批号清空），原单标记为已退换
     *
     * 示例请求：
     * POST /api/acceptance/1/re-receive
     *
     * @param id 验收单ID
     * @return 新生成的验收单
     */
    @PostMapping("/{id}/re-receive")
    public ApiResponse<AcceptanceOrder> reReceive(@PathVariable Long id) {
        try {
            AcceptanceOrder newAcceptance = acceptanceOrderService.reReceive(id);
            return success(newAcceptance, "已生成新验收单 " + newAcceptance.getAcceptanceCode() + "，原单标记为已退换");
        } catch (Exception ex) {
            return exception(ex, "重新收货");
        }
    }

    // endregion
}

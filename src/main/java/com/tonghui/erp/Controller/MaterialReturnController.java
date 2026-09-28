package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialResendRequestDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnItemDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnQueryDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Service.MaterialReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物料退货管理控制器
 * <p>
 * 采购部门集中处理不合格退货的唯一工作台接口：
 * 退货明细行查询（明细行级分页）、退货、取消、重新发货（合并生成新验收单）
 * </p>
 * <pre>
 * ┌────┬──────────────────────────────────┬────────┬────────────────────────────┐
 * │ 序号│ 接口路径                          │ 方法   │ 说明                        │
 * ├────┼──────────────────────────────────┼────────┼────────────────────────────┤
 * │ 1  │ /api/material-return/search      │ GET    │ 分页查询退货明细行           │
 * │ 2  │ /api/material-return/{id}/return │ POST   │ 退货：待退货→已退货          │
 * │ 3  │ /api/material-return/{id}/cancel │ POST   │ 取消：已退货→已取消          │
 * │ 4  │ /api/material-return/resend      │ POST   │ 重新发货：合并生成新验收单    │
 * └────┴──────────────────────────────────┴────────┴────────────────────────────┘
 * </pre>
 */
@RestController
@RequestMapping("/api/material-return")
public class MaterialReturnController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 物料退货管理服务
     */
    @Autowired
    private MaterialReturnService materialReturnService;

    // endregion

    // region 分页查询
    // ===================================
    // 分页查询
    // ===================================

    /**
     * 分页查询退货明细行
     *
     * 示例请求：
     * GET /api/material-return/search?pageIndex=0&pageSize=20&status=待退货&returnReason=初验不合格&keyword=黄芪&supplier=XX供应商&startDate=2026-01-01&endDate=2026-12-31
     *
     * @param query 查询条件（status-明细状态，returnReason-退货原因，supplier-供应商，
     *              keyword-物料名称/编码/验收单号关键字，startDate/endDate-日期范围，
     *              pageIndex-页码从0开始，pageSize-每页数量）
     * @return 退货明细分页结果（含验收单号/采购订单编号/制剂名称/批号/退货数量/供应商/原因/备注/状态/重发时间）
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<MaterialReturnItemDto>> search(@ModelAttribute MaterialReturnQueryDto query) {
        try {
            return success(materialReturnService.searchItems(query));
        } catch (Exception ex) {
            return exception(ex, "查询退货明细");
        }
    }

    // endregion

    // region 状态操作
    // ===================================
    // 状态操作
    // ===================================

    /**
     * 退货：明细 待退货 → 已退货（二次确认后调用）
     *
     * 示例请求：
     * POST /api/material-return/12/return
     *
     * @param detailId 验收明细ID
     * @return 操作结果
     */
    @PostMapping("/{detailId}/return")
    public ApiResponse<Boolean> returnDetail(@PathVariable Long detailId) {
        try {
            materialReturnService.returnDetail(detailId);
            return success(true, "已退货");
        } catch (Exception ex) {
            return exception(ex, "退货");
        }
    }

    /**
     * 取消：明细 已退货 → 已取消（该物料取消采购，不影响整单其他明细）
     *
     * 示例请求：
     * POST /api/material-return/12/cancel
     *
     * @param detailId 验收明细ID
     * @return 操作结果
     */
    @PostMapping("/{detailId}/cancel")
    public ApiResponse<Boolean> cancelDetail(@PathVariable Long detailId) {
        try {
            materialReturnService.cancelDetail(detailId);
            return success(true, "已取消");
        } catch (Exception ex) {
            return exception(ex, "取消");
        }
    }

    /**
     * 重新发货：同一验收单的多条已退货明细合并生成一条新验收单
     * <p>新验收单来源类型=退货重发、初始状态=运输中，所选明细状态→已重发并写入操作留痕</p>
     *
     * 示例请求：
     * POST /api/material-return/resend
     * Content-Type: application/json
     * { "acceptanceId": 1, "items": [ { "detailId": 12, "resendQty": 1.500 }, { "detailId": 13, "resendQty": 2.000 } ] }
     *
     * @param request 重新发货请求（acceptanceId-来源验收单ID，items-明细ID与重新发货数量列表）
     * @return 新生成的验收单
     */
    @PostMapping("/resend")
    public ApiResponse<AcceptanceOrder> resend(@RequestBody MaterialResendRequestDto request) {
        try {
            AcceptanceOrder newAcceptance = materialReturnService.resend(request);
            return success(newAcceptance, "已生成新验收单 " + newAcceptance.getAcceptanceCode());
        } catch (Exception ex) {
            return exception(ex, "重新发货");
        }
    }

    // endregion
}

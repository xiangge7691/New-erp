package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.PurchasePlanStatusDto;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.Entity.PurchasePlanDetail;
import com.tonghui.erp.Data.mapper.PurchasePlanDetailMapper;
import com.tonghui.erp.Service.PurchasePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购计划控制器
 */
@RestController
@RequestMapping("/api/purchase-plan")
public class PurchasePlanController extends BaseController {

    @Autowired
    private PurchasePlanService purchasePlanService;

    @Autowired
    private PurchasePlanDetailMapper purchasePlanDetailMapper;

    /**
     * 创建采购计划
     */
    @PostMapping
    public ApiResponse<PurchasePlan> create(@RequestBody PurchasePlan purchasePlan) {
        try {
            boolean result = purchasePlanService.addPurchasePlan(purchasePlan);
            if (result) {
                return success(purchasePlan, "创建成功");
            }
            return error("创建失败");
        } catch (Exception ex) {
            return exception(ex, "创建采购计划失败");
        }
    }

    /**
     * 根据ID获取采购计划详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PurchasePlan> getById(@PathVariable Long id) {
        try {
            PurchasePlan plan = purchasePlanService.getById(id);
            return success(plan);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    /**
     * 分页查询采购计划列表（支持多条件组合查询）
     * <p>查询条件自动从请求参数映射到实体，支持计划编号、标题、制剂、物料类型、仓库、状态等条件筛选，
     * 以及处理日期、期望到货日期、预计到货日期的时间范围筛选</p>
     *
     * 示例请求：
     * GET /api/purchase-plan?pageIndex=0&pageSize=20&status=草稿&warehouse=原料库&planCode=CGJH&processingDateStart=2025-01-01&processingDateEnd=2025-12-31&keyword=XX制剂
     *
     * @param purchasePlan             查询条件（自动从query参数映射）
     * @param keyword                  关键字（对计划编号、生产计划编号、标题、制剂名称模糊匹配，可选）
     * @param processingDateStart      处理日期起始（可选）
     * @param processingDateEnd        处理日期结束（可选）
     * @param desiredDeliveryDateStart 期望到货日期起始（可选）
     * @param desiredDeliveryDateEnd   期望到货日期结束（可选）
     * @param expectedDeliveryDateStart 预计到货日期起始（可选）
     * @param expectedDeliveryDateEnd   预计到货日期结束（可选）
     * @param pageIndex                页码（从0开始）
     * @param pageSize                 每页数量
     * @return 分页查询结果
     */
    @GetMapping
    public ApiResponse<PagedResult<PurchasePlan>> list(
            PurchasePlan purchasePlan,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate processingDateStart,
            @RequestParam(required = false) LocalDate processingDateEnd,
            @RequestParam(required = false) LocalDate desiredDeliveryDateStart,
            @RequestParam(required = false) LocalDate desiredDeliveryDateEnd,
            @RequestParam(required = false) LocalDate expectedDeliveryDateStart,
            @RequestParam(required = false) LocalDate expectedDeliveryDateEnd,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<PurchasePlan> page = purchasePlanService.queryPurchasePlans(purchasePlan, keyword,
                    processingDateStart, processingDateEnd,
                    desiredDeliveryDateStart, desiredDeliveryDateEnd,
                    expectedDeliveryDateStart, expectedDeliveryDateEnd,
                    safePageIndex, safePageSize);

            PagedResult<PurchasePlan> result = new PagedResult<>();
            result.setItems(page.getRecords());
            result.setTotalCount(page.getTotal());
            result.setPageIndex(safePageIndex);
            result.setPageSize(safePageSize);

            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    /**
     * 删除采购计划
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            boolean result = purchasePlanService.removeById(id);
            return success(result, "删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除失败");
        }
    }

    /**
     * 更新采购计划状态（通用状态变更接口）
     * <p>
     * 前端控制按钮显隐，后端只负责更新状态。
     * 当目标状态为"已审批"时，自动生成采购订单。
     * </p>
     *
     * 示例请求：
     * PUT /api/purchase-plan/1/status
     * 请求体：{"status": "已审批", "approvalOpinion": "同意"}
     *
     * @param id   采购计划ID
     * @param body 状态更新请求体，支持多个字段：
     *             <ul>
     *               <li>status：目标状态（必填），可选值：草稿/待审批/已审批/已驳回</li>
     *               <li>approvalOpinion：审批意见（可选），修改状态时填写的审批备注</li>
     *             </ul>
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Boolean> updateStatus(@PathVariable Long id, @RequestBody PurchasePlanStatusDto body) {
        try {
            boolean result = purchasePlanService.updateStatus(id, body.getStatus(), body.getApprovalOpinion());
            return success(result, "状态更新成功");
        } catch (Exception ex) {
            return exception(ex, "状态更新失败");
        }
    }

    /**
     * 获取采购计划明细
     */
    @GetMapping("/{id}/details")
    public ApiResponse<List<PurchasePlanDetail>> getDetails(@PathVariable Long id) {
        try {
            List<PurchasePlanDetail> details = purchasePlanDetailMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PurchasePlanDetail>()
                            .eq("plan_id", id)
                            .eq("is_deleted", 0)
            );
            return success(details);
        } catch (Exception ex) {
            return exception(ex, "查询明细失败");
        }
    }

    /**
     * 批量保存采购计划明细
     */
    @PostMapping("/{id}/details/batch")
    public ApiResponse<Boolean> batchSaveDetails(@PathVariable Long id,
                                                  @RequestBody List<PurchasePlanDetail> details) {
        try {
            // 先删除原有明细
            purchasePlanDetailMapper.physicalDeleteByPlanId(id);

            // 批量插入新明细
            for (int i = 0; i < details.size(); i++) {
                PurchasePlanDetail detail = details.get(i);
                detail.setId(null);
                detail.setPlanId(id);
                detail.setSequenceNumber(i + 1);
                detail.setIsDeleted(0);
                detail.setVersion(1);
                purchasePlanDetailMapper.insert(detail);
            }
            return success(true, "保存成功");
        } catch (Exception ex) {
            return exception(ex, "保存明细失败");
        }
    }
}

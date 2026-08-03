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
     * 分页查询采购计划列表
     */
    @GetMapping
    public ApiResponse<PagedResult<PurchasePlan>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Page<PurchasePlan> page = purchasePlanService.queryPurchasePlans(status, keyword, pageIndex, pageSize);

            PagedResult<PurchasePlan> result = new PagedResult<>();
            result.setItems(page.getRecords());
            result.setTotalCount(page.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);

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

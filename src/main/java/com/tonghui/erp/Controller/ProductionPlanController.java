package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Service.ProductionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产计划控制器
 */
@RestController
@RequestMapping("/api/production-plans")
public class ProductionPlanController extends BaseCrudController<ProductionPlan, ProductionPlan, Integer> {

    @Autowired
    private ProductionPlanService productionPlanService;

    @Override
    protected PagedResult<ProductionPlan> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用ProductionPlanService的queryProductionPlans方法进行查询
        ProductionPlan productionPlan = new ProductionPlan();
        Page<ProductionPlan> pageResult = productionPlanService.queryProductionPlans(productionPlan, null, null, null, null, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<ProductionPlan> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount((int) pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected ProductionPlan getDataById(Integer id) {
        return productionPlanService.getById(id);
    }

    @Override
    protected ProductionPlan doCreate(ProductionPlan entity) {
        // 设置创建人和更新人
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            entity.setCreateUser(currentUserId);
            entity.setUpdateUser(currentUserId);
        }
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        productionPlanService.save(entity);
        return entity;
    }

    @Override
    protected ProductionPlan doUpdate(Integer id, ProductionPlan entity) {
        entity.setId(id);
        // 设置更新人和更新时间
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            entity.setUpdateUser(currentUserId);
        }
        entity.setUpdateTime(LocalDateTime.now());
        productionPlanService.updateById(entity);
        return entity;
    }

    @Override
    protected boolean doDelete(Integer id) {
        return productionPlanService.removeById(id);
    }

    // #region 高级查询

    /**
     * 高级查询生产计划（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - planNumber：模糊匹配
     * - relatedOrder：模糊匹配
     * - preparationCode：模糊匹配
     * - preparationName：模糊匹配
     * - currentStatus：精确匹配
     * - isArchived：精确匹配
     * - createdTimeStart：创建时间起始（大于等于）
     * - createdTimeEnd：创建时间结束（小于等于）
     * - updatedTimeStart：更新时间起始（大于等于）
     * - updatedTimeEnd：更新时间结束（小于等于）
     *
     * 示例请求：
     * GET /api/production-plans/search?pageIndex=1&pageSize=20&planNumber=PP2025&currentStatus=active&createdTimeStart=2025-01-01T00:00:00&createdTimeEnd=2025-12-31T23:59:59
     *
     * @param productionPlan 查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<ProductionPlan>> queryProductionPlans(ProductionPlan productionPlan,
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

            // 获取分页结果
            Page<ProductionPlan> pageResult = productionPlanService.queryProductionPlans(productionPlan, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);

            // 转换为统一的PagedResult格式
            PagedResult<ProductionPlan> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount((int) pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return error("查询失败：" + ex.getMessage());
        }
    }

    // #endregion

    // #region 带子表查询

    /**
     * 高级查询生产计划（包含工序记录子表）
     *
     * @param productionPlan 查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果（包含工序记录）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ProductionPlanWithRecordsDto>> searchWithDetails(ProductionPlan productionPlan,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                                     @RequestParam int pageIndex,
                                                                                     @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ProductionPlanWithRecordsDto> result = productionPlanService.searchWithDetails(productionPlan, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // #endregion
    
    // #region 自动生成计划编号接口
    
    /**
     * 自动生成生产计划编号
     *
     * @return 生成的计划编号
     */
    @GetMapping("/generate-plan-number")
    public ApiResponse<String> generatePlanNumber() {
        try {
            // 生成计划编号格式：PP + 年月日 + 6位序列号
            String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String prefix = "Plan" + dateStr;
            
            // 查询当天已有的最大序列号
            QueryWrapper<ProductionPlan> queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("plan_number", prefix);
            queryWrapper.orderByDesc("plan_number");
            queryWrapper.last("LIMIT 1");
            
            ProductionPlan latestPlan = productionPlanService.getOne(queryWrapper);
            
            int sequence = 1;
            if (latestPlan != null && latestPlan.getPlanNumber() != null) {
                try {
                    // 从现有编号中提取序列号部分并加1
                    String latestNumber = latestPlan.getPlanNumber();
                    String sequenceStr = latestNumber.substring(Math.max(0, latestNumber.length() - 4));
                    sequence = Integer.parseInt(sequenceStr) + 1;
                } catch (Exception e) {
                    // 解析失败则使用默认序列号1
                    sequence = 1;
                }
            }
            
            // 格式化序列号为6位数字，不足补零
            String planNumber = prefix + String.format("%04d", sequence);
            
            return success(planNumber, "计划编号生成成功");
        } catch (Exception ex) {
            return error("生成计划编号异常：" + ex.getMessage());
        }
    }
    
    // #endregion
    
    // #region 状态管理接口

    /**
     * 更改生产计划状态
     *
     * @param planId 生产计划ID
     * @param newStatus 新状态
     * @param remark 备注
     * @param finishedQuantity 成品数量（仅在出库状态时使用）
     * @param productionCycle 生产周期（仅在出库状态时使用）
     * @param yieldRate 得率（仅在出库状态时使用）
     * @param unitPrice 单价（仅在出库状态时使用）
     * @return 操作结果
     */
    @PostMapping("/{planId}/status/change")
    public ApiResponse<Boolean> changePlanStatus(
            @PathVariable Integer planId,
            @RequestParam String newStatus,
            @RequestParam Long operatorId,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) BigDecimal finishedQuantity,
            @RequestParam(required = false) Integer productionCycle,
            @RequestParam(required = false) BigDecimal yieldRate,
            @RequestParam(required = false) BigDecimal unitPrice) {
        try {
            boolean result = productionPlanService.changePlanStatus(
                    planId, newStatus, operatorId, remark,
                    finishedQuantity, productionCycle, yieldRate, unitPrice);
            return success(result, "状态变更成功");
        } catch (Exception ex) {
            return error("状态变更失败：" + ex.getMessage());
        }
    }

    /**
     * 恢复暂停的生产计划状态
     *
     * @param planId 生产计划ID
     * @param operatorId 操作员ID
     * @param remark 备注
     * @return 操作结果
     */
    @PostMapping("/{planId}/status/resume")
    public ApiResponse<Boolean> resumePlanStatus(
            @PathVariable Integer planId,
            @RequestParam Long operatorId,
            @RequestParam String remark) {
        try {
            boolean result = productionPlanService.resumePlanStatus(planId, operatorId, remark);
            return success(result, "状态恢复成功");
        } catch (Exception ex) {
            return error("状态恢复失败：" + ex.getMessage());
        }
    }

    // #endregion
}

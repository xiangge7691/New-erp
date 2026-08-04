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

import java.time.LocalDateTime;

/**
 * 生产计划控制器
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────┬────────┬─────────────────────────────────┐
 * │ #  │ 接口                                       │ 方法   │ 说明                            │
 * ├────┼────────────────────────────────────────────┼────────┼─────────────────────────────────┤
 * │ 1  │ /api/production-plans                      │ GET    │ 分页查询生产计划列表            │
 * │ 2  │ /api/production-plans/{id}                 │ GET    │ 获取生产计划详情                │
 * │ 3  │ /api/production-plans                      │ POST   │ 新增生产计划                    │
 * │ 4  │ /api/production-plans/{id}                 │ PUT    │ 修改生产计划                    │
 * │ 5  │ /api/production-plans/{id}                 │ DELETE │ 删除生产计划                    │
 * │ 6  │ /api/production-plans/search               │ GET    │ 高级查询生产计划（多条件+分页）  │
 * │ 7  │ /api/production-plans/search-with-details  │ GET    │ 高级查询生产计划（含工序记录）   │
 * │ 8  │ /api/production-plans/generate-plan-number │ GET    │ 自动生成计划编号                │
 * └────┴────────────────────────────────────────────┴────────┴─────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/production-plans")
public class ProductionPlanController extends BaseCrudController<ProductionPlan, ProductionPlan, Integer> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 生产计划服务
     */
    @Autowired
    private ProductionPlanService productionPlanService;

    // endregion

    // region CRUD基础方法实现
    // ===================================
    // CRUD基础方法实现
    // ===================================

    @Override
    protected PagedResult<ProductionPlan> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        ProductionPlan productionPlan = new ProductionPlan();
        Page<ProductionPlan> pageResult = productionPlanService.queryProductionPlans(productionPlan,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                safePageIndex, safePageSize);

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
        // 自动生成计划编号
        if (entity.getPlanNumber() == null || entity.getPlanNumber().isEmpty()) {
            entity.setPlanNumber(generatePlanNumberInternal());
        }

        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            entity.setCreatedBy(currentUserId);
            entity.setUpdatedBy(currentUserId);
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);

        // 新建计划默认状态为「待生产」（未关联工单），后续由工单状态联动刷新
        entity.setCurrentStatus("待生产");
        entity.setCurrentStatusDate(now);

        productionPlanService.save(entity);
        return entity;
    }

    /**
     * 内部生成计划编号方法
     */
    private String generatePlanNumberInternal() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "Plan" + dateStr;

        QueryWrapper<ProductionPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("plan_number", prefix);
        queryWrapper.orderByDesc("plan_number");
        queryWrapper.last("LIMIT 1");

        ProductionPlan latestPlan = productionPlanService.getOne(queryWrapper);

        int sequence = 1;
        if (latestPlan != null && latestPlan.getPlanNumber() != null) {
            try {
                String latestNumber = latestPlan.getPlanNumber();
                String sequenceStr = latestNumber.substring(Math.max(0, latestNumber.length() - 4));
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }

    @Override
    protected ProductionPlan doUpdate(Integer id, ProductionPlan entity) {
        entity.setId(id);
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            entity.setUpdatedBy(currentUserId);
        }
        entity.setUpdatedTime(LocalDateTime.now());
        productionPlanService.updateById(entity);
        return entity;
    }

    @Override
    protected boolean doDelete(Integer id) {
        return productionPlanService.removeById(id);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询生产计划（支持多条件 + 分页）
     *
     * 示例请求：
     * GET /api/production-plans/search?pageIndex=1&pageSize=20&planNumber=PP2025&currentStatus=active&createdTimeStart=2025-01-01T00:00:00&createdTimeEnd=2025-12-31T23:59:59
     *
     * @param productionPlan 查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param productionStartTimeStart 生产开始时间起始
     * @param productionStartTimeEnd 生产开始时间结束
     * @param productionEndTimeStart 生产结束时间起始
     * @param productionEndTimeEnd 生产结束时间结束
     * @param inspectionStartTimeStart 检验开始时间起始
     * @param inspectionStartTimeEnd 检验开始时间结束
     * @param inspectionEndTimeStart 检验结束时间起始
     * @param inspectionEndTimeEnd 检验结束时间结束
     * @param outboundTimeStart 出库时间起始
     * @param outboundTimeEnd 出库时间结束
     * @param archiveTimeStart 归档时间起始
     * @param archiveTimeEnd 归档时间结束
     * @param timeFieldType 时间字段类型
     * @param timeStart 通用时间起始
     * @param timeEnd 通用时间结束
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;ProductionPlan&gt;&gt; 分页查询结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<ProductionPlan>> queryProductionPlans(ProductionPlan productionPlan,
                                                                         @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime productionStartTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime productionStartTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime productionEndTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime productionEndTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime inspectionStartTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime inspectionStartTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime inspectionEndTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime inspectionEndTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime outboundTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime outboundTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime archiveTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime archiveTimeEnd,
                                                                         @RequestParam(required = false) String timeFieldType,
                                                                         @RequestParam(required = false) LocalDateTime timeStart,
                                                                         @RequestParam(required = false) LocalDateTime timeEnd,
                                                                         @RequestParam int pageIndex,
                                                                         @RequestParam int pageSize) {
        try {
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            // 获取分页结果
            Page<ProductionPlan> pageResult = productionPlanService.queryProductionPlans(productionPlan,
                    null,
                    createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                    productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                    inspectionStartTimeStart, inspectionStartTimeEnd, inspectionEndTimeStart, inspectionEndTimeEnd,
                    outboundTimeStart, outboundTimeEnd, archiveTimeStart, archiveTimeEnd,
                    timeFieldType, timeStart, timeEnd,
                    safePageIndex, safePageSize);

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

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 高级查询生产计划（包含工序记录子表）
     * <p>支持通过 keyword 关键字对计划编号、计划名称进行模糊查询，同时支持多条件组合查询</p>
     *
     * 示例请求：
     * GET /api/production-plans/search-with-details?pageIndex=1&pageSize=20&keyword=PP2025
     *
     * @param productionPlan 查询条件（自动从query参数映射）
     * @param keyword 关键字（对计划编号、计划名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param productionStartTimeStart 生产开始时间起始
     * @param productionStartTimeEnd 生产开始时间结束
     * @param productionEndTimeStart 生产结束时间起始
     * @param productionEndTimeEnd 生产结束时间结束
     * @param inspectionStartTimeStart 检验开始时间起始
     * @param inspectionStartTimeEnd 检验开始时间结束
     * @param inspectionEndTimeStart 检验结束时间起始
     * @param inspectionEndTimeEnd 检验结束时间结束
     * @param outboundTimeStart 出库时间起始
     * @param outboundTimeEnd 出库时间结束
     * @param archiveTimeStart 归档时间起始
     * @param archiveTimeEnd 归档时间结束
     * @param timeFieldType 时间字段类型
     * @param timeStart 通用时间起始
     * @param timeEnd 通用时间结束
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;ProductionPlanWithRecordsDto&gt;&gt; 分页结果（包含工序记录）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ProductionPlanWithRecordsDto>> searchWithDetails(ProductionPlan productionPlan,
                                                                                     @RequestParam(required = false) String keyword,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime productionStartTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime productionStartTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime productionEndTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime productionEndTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime inspectionStartTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime inspectionStartTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime inspectionEndTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime inspectionEndTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime outboundTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime outboundTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime archiveTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime archiveTimeEnd,
                                                                                     @RequestParam(required = false) String timeFieldType,
                                                                                     @RequestParam(required = false) LocalDateTime timeStart,
                                                                                     @RequestParam(required = false) LocalDateTime timeEnd,
                                                                                     @RequestParam int pageIndex,
                                                                                     @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ProductionPlanWithRecordsDto> result = productionPlanService.searchWithDetails(productionPlan,
                    keyword,
                    createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                    productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                    inspectionStartTimeStart, inspectionStartTimeEnd, inspectionEndTimeStart, inspectionEndTimeEnd,
                    outboundTimeStart, outboundTimeEnd, archiveTimeStart, archiveTimeEnd,
                    timeFieldType, timeStart, timeEnd,
                    safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion
}

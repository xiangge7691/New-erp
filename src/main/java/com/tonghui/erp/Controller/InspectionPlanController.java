package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.InspectionPlan;
import com.tonghui.erp.Service.InspectionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验计划控制器
 * <p>
 * 提供月/周检验计划的CRUD操作、条件分页查询及计划编号生成，
 * 用于质检检验模块的检验计划手动排程管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/inspectionPlan              │ GET    │ 分页查询检验计划列表          │
 * │ 2  │ /api/inspectionPlan/list         │ GET    │ 查询检验计划列表（不分页）    │
 * │ 3  │ /api/inspectionPlan/generateCode │ GET    │ 获取自动生成的计划编号        │
 * │ 4  │ /api/inspectionPlan/{id}         │ GET    │ 查询检验计划详情              │
 * │ 5  │ /api/inspectionPlan              │ POST   │ 新增检验计划                  │
 * │ 6  │ /api/inspectionPlan/{id}         │ PUT    │ 修改检验计划                  │
 * │ 7  │ /api/inspectionPlan/{id}         │ DELETE │ 删除检验计划（软删除）        │
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/inspectionPlan")
public class InspectionPlanController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 检验计划服务
     */
    @Autowired
    private InspectionPlanService inspectionPlanService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询检验计划列表
     *
     * 示例请求：
     * GET /api/inspectionPlan?planCode=JH-20260720001&objectName=维生素C片&inspectionType=成品检验&status=待检验&startDate=2026-07-01&endDate=2026-07-31&pageIndex=0&pageSize=10
     *
     * @param planCode       计划编号（可选，模糊匹配）
     * @param objectName     检验对象名称（可选，模糊匹配）
     * @param batchNo        批号（可选，模糊匹配）
     * @param inspectionType 检验类型（可选，精确匹配）
     * @param status         状态（可选，精确匹配）
     * @param startDate      计划检验开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate        计划检验结束日期（可选，格式：yyyy-MM-dd）
     * @param pageIndex      页码索引，从0开始（默认0）
     * @param pageSize       每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;InspectionPlan&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<InspectionPlan>> getAll(
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String inspectionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<InspectionPlan> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<InspectionPlan> wrapper = buildQueryWrapper(planCode, objectName, batchNo, inspectionType, status, startDate, endDate);
            Page<InspectionPlan> pageResult = inspectionPlanService.page(page, wrapper);

            PagedResult<InspectionPlan> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询检验计划");
        }
    }

    /**
     * 查询检验计划列表（不分页）
     *
     * 示例请求：
     * GET /api/inspectionPlan/list?inspectionType=成品检验&status=待检验
     *
     * @param planCode       计划编号（可选，模糊匹配）
     * @param objectName     检验对象名称（可选，模糊匹配）
     * @param batchNo        批号（可选，模糊匹配）
     * @param inspectionType 检验类型（可选，精确匹配）
     * @param status         状态（可选，精确匹配）
     * @param startDate      计划检验开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate        计划检验结束日期（可选，格式：yyyy-MM-dd）
     * @return ApiResponse&lt;List&lt;InspectionPlan&gt;&gt; 检验计划列表
     */
    @GetMapping("/list")
    public ApiResponse<List<InspectionPlan>> getList(
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String inspectionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            QueryWrapper<InspectionPlan> wrapper = buildQueryWrapper(planCode, objectName, batchNo, inspectionType, status, startDate, endDate);
            wrapper.orderByDesc("plan_time");
            return success(inspectionPlanService.list(wrapper));
        } catch (Exception e) {
            return exception(e, "查询检验计划");
        }
    }

    /**
     * 查询检验计划详情
     *
     * 示例请求：
     * GET /api/inspectionPlan/1
     *
     * @param id 检验计划ID（路径参数）
     * @return ApiResponse&lt;InspectionPlan&gt; 检验计划详情
     */
    @GetMapping("/{id}")
    public ApiResponse<InspectionPlan> getById(@PathVariable Long id) {
        try {
            InspectionPlan plan = inspectionPlanService.getById(id);
            if (plan == null) {
                return error("检验计划不存在");
            }
            return success(plan);
        } catch (Exception e) {
            return exception(e, "查询检验计划详情");
        }
    }

    /**
     * 获取自动生成的计划编号
     *
     * 示例请求：
     * GET /api/inspectionPlan/generateCode
     *
     * @return ApiResponse&lt;String&gt; 自动生成的计划编号（格式JH-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            return success(inspectionPlanService.generateCode());
        } catch (Exception e) {
            return exception(e, "生成计划编号");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增检验计划
     *
     * 示例请求：
     * POST /api/inspectionPlan
     * Content-Type: application/json
     * {
     *   "planCode": "JH-20260720-001",
     *   "planPeriod": "2026-07-第3周",
     *   "inspectionType": "成品检验",
     *   "objectName": "维生素C片",
     *   "batchNo": "20260701",
     *   "spec": "100mg",
     *   "inspectionSummary": "含量测定、崩解时限",
     *   "planTime": "2026-07-20",
     *   "status": "待检验",
     *   "remark": "月检计划"
     * }
     *
     * @param record 检验计划信息（编号为空时系统自动生成）
     * @return ApiResponse&lt;InspectionPlan&gt; 新增的检验计划
     */
    @PostMapping
    public ApiResponse<InspectionPlan> create(@RequestBody InspectionPlan record) {
        try {
            if (StringUtils.hasText(record.getPlanCode())) {
                if (!inspectionPlanService.isCodeUnique(record.getPlanCode(), null)) {
                    return error("计划编号已存在：" + record.getPlanCode());
                }
            } else {
                record.setPlanCode(inspectionPlanService.generateCode());
            }
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            inspectionPlanService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增检验计划");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改检验计划
     *
     * 示例请求：
     * PUT /api/inspectionPlan/1
     * Content-Type: application/json
     * {
     *   "status": "已完成",
     *   "completeTime": "2026-07-22"
     * }
     *
     * @param id     检验计划ID（路径参数）
     * @param record 更新的检验计划信息
     * @return ApiResponse&lt;InspectionPlan&gt; 修改后的检验计划
     */
    @PutMapping("/{id}")
    public ApiResponse<InspectionPlan> update(
            @PathVariable Long id,
            @RequestBody InspectionPlan record) {
        try {
            InspectionPlan existing = inspectionPlanService.getById(id);
            if (existing == null) {
                return error("检验计划不存在");
            }
            if (StringUtils.hasText(record.getPlanCode())
                    && !record.getPlanCode().equals(existing.getPlanCode())
                    && !inspectionPlanService.isCodeUnique(record.getPlanCode(), id)) {
                return error("计划编号已存在：" + record.getPlanCode());
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            inspectionPlanService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改检验计划");
        }
    }

    /**
     * 删除检验计划（软删除）
     *
     * 示例请求：
     * DELETE /api/inspectionPlan/1
     *
     * @param id 检验计划ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            InspectionPlan existing = inspectionPlanService.getById(id);
            if (existing == null) {
                return error("检验计划不存在");
            }
            inspectionPlanService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除检验计划");
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建检验计划查询条件
     *
     * @param planCode       计划编号（模糊匹配）
     * @param objectName     检验对象名称（模糊匹配）
     * @param batchNo        批号（模糊匹配）
     * @param inspectionType 检验类型（精确匹配）
     * @param status         状态（精确匹配）
     * @param startDate      计划检验开始日期
     * @param endDate        计划检验结束日期
     * @return 查询条件Wrapper
     */
    private QueryWrapper<InspectionPlan> buildQueryWrapper(String planCode, String objectName, String batchNo,
            String inspectionType, String status, String startDate, String endDate) {
        QueryWrapper<InspectionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (StringUtils.hasText(planCode)) wrapper.like("plan_code", planCode);
        if (StringUtils.hasText(objectName)) wrapper.like("object_name", objectName);
        if (StringUtils.hasText(batchNo)) wrapper.like("batch_no", batchNo);
        if (StringUtils.hasText(inspectionType)) wrapper.eq("inspection_type", inspectionType);
        if (StringUtils.hasText(status)) wrapper.eq("status", status);
        if (StringUtils.hasText(startDate)) wrapper.ge("plan_time", startDate);
        if (StringUtils.hasText(endDate)) wrapper.le("plan_time", endDate);
        wrapper.orderByDesc("plan_time");
        return wrapper;
    }

    // endregion
}
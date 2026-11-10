package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.InspectionTaskDtos;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Service.InspectionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 检验任务控制器
 * <p>
 * 提供检验任务列表、详情、六步办理、跳过、作废和归档接口。
 * 一个任务对应一个工单/批次/被检品属性，六步顺序由服务层校验。
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明             │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────┤
 * │ 1  │ /api/inspection/tasks                        │ GET    │ 分页查询任务      │
 * │ 2  │ /api/inspection/tasks/{id}                   │ GET    │ 任务详情          │
 * │ 3  │ /api/inspection/tasks                        │ POST   │ 创建任务          │
 * │ 4  │ /api/inspection/tasks/{id}/request           │ POST   │ 办理请检          │
 * │ 5  │ /api/inspection/tasks/{id}/plan              │ POST   │ 办理检验计划      │
 * │ 6  │ /api/inspection/tasks/{id}/sampling          │ POST   │ 办理取样          │
 * │ 7  │ /api/inspection/tasks/{id}/inspection        │ POST   │ 办理检验记录      │
 * │ 8  │ /api/inspection/tasks/{id}/retention         │ POST   │ 办理留样          │
 * │ 9  │ /api/inspection/tasks/{id}/destruction       │ POST   │ 办理销毁          │
 * │ 10 │ /api/inspection/tasks/{id}/skip              │ POST   │ 跳过留样/销毁     │
 * │ 11 │ /api/inspection/tasks/{id}/cancel            │ POST   │ 作废任务          │
 * │ 12 │ /api/inspection/tasks/{id}/archive           │ POST   │ 归档任务          │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────┘
 */
@RestController
@RequestMapping("/api/inspection/tasks")
public class InspectionTaskController extends BaseController {

    /** 检验任务服务 */
    @Autowired
    private InspectionTaskService inspectionTaskService;

    /**
     * 分页查询检验任务
     *
     * 示例请求：
     * GET /api/inspection/tasks?varietyCode=Z04-011&batchNo=260901&itemCategory=成品&step=INSPECTION&status=进行中&keyword=QJ&pageIndex=0&pageSize=20
     *
     * @param varietyCode  品种编码（可选）
     * @param batchNo      批号（可选，模糊匹配）
     * @param itemCategory 被检品属性（可选）
     * @param step         当前环节（可选）
     * @param status       任务状态（可选；为空时默认不包含已归档任务，可选值：待开始/进行中/已完成/已作废/已归档）
     * @param keyword      关键字（任务号/工单号/品种/批号）
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页数量
     * @return 分页任务列表
     */
    @GetMapping
    public ApiResponse<PagedResult<InspectionTaskDtos.TaskSummary>> list(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String step,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return success(inspectionTaskService.queryTasks(varietyCode, batchNo, itemCategory,
                    step, status, keyword, pageIndex, pageSize));
        } catch (Exception e) {
            return exception(e, "查询检验任务");
        }
    }

    /**
     * 查询检验任务详情
     *
     * 示例请求：GET /api/inspection/tasks/1
     *
     * @param id 任务ID
     * @return 任务详情（含六步）
     */
    @GetMapping("/{id}")
    public ApiResponse<InspectionTaskDtos.TaskDetail> detail(@PathVariable Long id) {
        try {
            return success(inspectionTaskService.getTaskDetail(id));
        } catch (Exception e) {
            return exception(e, "查询检验任务详情");
        }
    }

    /**
     * 创建检验任务
     *
     * 示例请求：
     * POST /api/inspection/tasks
     * {"workOrderId":463,"itemCategory":"成品","retentionRequired":1,"destructionRequired":1}
     *
     * @param request 创建请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.CREATE, desc = "创建检验任务",
            businessType = "INSPECTION_TASK", businessId = "#result.data.id")
    @PostMapping
    public ApiResponse<InspectionTaskDtos.TaskDetail> create(@RequestBody InspectionTaskDtos.CreateTaskRequest request) {
        try {
            return success(inspectionTaskService.createTask(request), "检验任务已创建");
        } catch (Exception e) {
            return exception(e, "创建检验任务");
        }
    }

    /**
     * 办理检验计划
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/plan
     * {"planPeriod":"2026-11-第1周","inspectionSummary":"全检","planTime":"2026-11-06"}
     *
     * 说明：检验类型由任务的被检品属性自动推导（生产原料→生产原料检验，
     * 中间产品→中间产品检验，成品→成品检验），无需传 inspectionType。
     *
     * @param id      任务ID
     * @param request 计划请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "办理检验计划",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/plan")
    public ApiResponse<InspectionTaskDtos.TaskDetail> completePlan(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.PlanStepRequest request) {
        try {
            return success(inspectionTaskService.completePlan(id, request), "检验计划已办理");
        } catch (Exception e) {
            return exception(e, "办理检验计划");
        }
    }

    /**
     * 办理请检
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/request
     * {"requester":"丁圣芳","requestDepartment":"质量部","processName":"成品检验","inspectionItems":"性状、鉴别、含量测定"}
     *
     * @param id      任务ID
     * @param request 请检请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "办理请检",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/request")
    public ApiResponse<InspectionTaskDtos.TaskDetail> completeRequest(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.RequestStepRequest request) {
        try {
            return success(inspectionTaskService.completeRequest(id, request), "请检已办理");
        } catch (Exception e) {
            return exception(e, "办理请检");
        }
    }

    /**
     * 办理取样
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/sampling
     * {"sampler":"陈静","location":"成品库 A 取样间","quantity":"100g×3份","count":3,"method":"随机"}
     *
     * @param id      任务ID
     * @param request 取样请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "办理取样",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/sampling")
    public ApiResponse<InspectionTaskDtos.TaskDetail> completeSampling(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.SamplingStepRequest request) {
        try {
            return success(inspectionTaskService.completeSampling(id, request), "取样已办理");
        } catch (Exception e) {
            return exception(e, "办理取样");
        }
    }

    /**
     * 办理留样
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/retention
     * {"quantity":"100g×3份","retainedDate":"2026-11-06","expiryDate":"2028-11-05","storageLocation":"留样室 A-03"}
     *
     * @param id      任务ID
     * @param request 留样请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "办理留样",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/retention")
    public ApiResponse<InspectionTaskDtos.TaskDetail> completeRetention(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.RetentionStepRequest request) {
        try {
            return success(inspectionTaskService.completeRetention(id, request), "留样已办理");
        } catch (Exception e) {
            return exception(e, "办理留样");
        }
    }

    /**
     * 办理检验记录
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/inspection
     * {"inspector":"丁圣芳","reviewer":"赵磊","inspectionBasis":"中国药典2025版","conclusion":"合格"}
     *
     * @param id      任务ID
     * @param request 检验记录请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "办理检验",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/inspection")
    public ApiResponse<InspectionTaskDtos.TaskDetail> completeInspection(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.InspectionStepRequest request) {
        try {
            return success(inspectionTaskService.completeInspection(id, request), "检验记录已办理");
        } catch (Exception e) {
            return exception(e, "办理检验");
        }
    }

    /**
     * 办理销毁
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/destruction
     * {"destroyReason":"留样到期","destroyMethod":"焚烧","destroyerName":"张力","destroyDate":"2026-11-06"}
     *
     * @param id      任务ID
     * @param request 销毁请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "办理销毁",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/destruction")
    public ApiResponse<InspectionTaskDtos.TaskDetail> completeDestruction(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.DestructionStepRequest request) {
        try {
            return success(inspectionTaskService.completeDestruction(id, request), "销毁已办理");
        } catch (Exception e) {
            return exception(e, "办理销毁");
        }
    }

    /**
     * 跳过留样或销毁
     *
     * 示例请求：
     * POST /api/inspection/tasks/1/skip
     * {"stepCode":"RETENTION","reason":"该品种不要求留样"}
     *
     * @param id      任务ID
     * @param request 跳过请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "跳过检验步骤",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/skip")
    public ApiResponse<InspectionTaskDtos.TaskDetail> skip(
            @PathVariable Long id, @RequestBody InspectionTaskDtos.SkipStepRequest request) {
        try {
            return success(inspectionTaskService.skipStep(id, request), "环节已跳过");
        } catch (Exception e) {
            return exception(e, "跳过检验环节");
        }
    }

    /**
     * 作废检验任务
     *
     * 示例请求：POST /api/inspection/tasks/1/cancel {"reason":"重复创建"}
     *
     * @param id      任务ID
     * @param request 作废请求
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "作废检验任务",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/cancel")
    public ApiResponse<InspectionTaskDtos.TaskDetail> cancel(
            @PathVariable Long id, @RequestBody(required = false) InspectionTaskDtos.CancelRequest request) {
        try {
            return success(inspectionTaskService.cancelTask(id, request), "任务已作废");
        } catch (Exception e) {
            return exception(e, "作废检验任务");
        }
    }

    /**
     * 归档检验任务
     * <p>
     * 仅“待开始/进行中”的任务可归档；归档后任务只读，默认不在列表和看板中展示，且不可解除归档。
     * </p>
     *
     * 示例请求：POST /api/inspection/tasks/1/archive
     *
     * @param id 任务ID
     * @return 任务详情
     */
    @OperationLog(module = "检验任务", type = OperationType.UPDATE, desc = "归档检验任务",
            businessType = "INSPECTION_TASK", businessId = "#id")
    @PostMapping("/{id}/archive")
    public ApiResponse<InspectionTaskDtos.TaskDetail> archive(@PathVariable Long id) {
        try {
            return success(inspectionTaskService.archiveTask(id), "检验任务已归档");
        } catch (Exception e) {
            return exception(e, "归档检验任务");
        }
    }
}

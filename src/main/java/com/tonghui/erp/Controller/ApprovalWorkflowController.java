package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import com.tonghui.erp.Service.ApprovalWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流程定义控制器
 */
@RestController
@RequestMapping("/api/approval/workflow")
public class ApprovalWorkflowController extends BaseController {
    
    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;
    
    /**
     * 获取审批流程列表
     * GET /api/approval/workflow
     * 请求参数:
     * - pageIndex: 页码，默认-1(全量)
     * - pageSize: 每页数量，默认-1(全量)
     * 返回参数:
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": {
     *     "items": [审批流程对象列表],
     *     "totalCount": 总数,
     *     "pageIndex": 页码,
     *     "pageSize": 每页数量
     *   },
     *   "timestamp": 时间戳
     * }
     */
    @GetMapping
    public ApiResponse<PagedResult<ApprovalWorkflow>> listWorkflows(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ApprovalWorkflow> result = new PagedResult<>();
            List<ApprovalWorkflow> workflows = approvalWorkflowService.getAllWorkflows();
            result.setItems(workflows);
            result.setTotalCount(workflows.size());
            result.setPageIndex(pageRequest.getPageIndex());
            result.setPageSize(pageRequest.getPageSize());
            // 如果是全量数据，修正pageSize
            if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
                result.setPageSize(workflows.size());
            }
            return success(result);
        } catch (Exception e) {
            return exception(e, "获取审批流程列表");
        }
    }
    
    /**
     * 根据ID获取审批流程详情
     * GET /api/approval/workflow/{id}
     * 请求参数:
     * - id: 审批流程ID
     * 返回参数:
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": {
     *     "id": 主键ID,
     *     "workflowName": 流程名称,
     *     "workflowType": 流程类型,
     *     "createdAt": 创建时间,
     *     "updatedAt": 更新时间
     *   },
     *   "timestamp": 时间戳
     * }
     */
    @GetMapping("/{id}")
    public ApiResponse<ApprovalWorkflow> getWorkflowById(@PathVariable Long id) {
        try {
            ApprovalWorkflow workflow = approvalWorkflowService.getById(id);
            if (workflow == null) {
                return error("未找到指定的审批流程");
            }
            return success(workflow);
        } catch (Exception e) {
            return exception(e, "获取审批流程详情");
        }
    }
    
    /**
     * 根据流程类型获取审批流程
     * GET /api/approval/workflow/type/{workflowType}
     * 请求参数:
     * - workflowType: 流程类型
     * 返回参数:
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": {
     *     "id": 主键ID,
     *     "workflowName": 流程名称,
     *     "workflowType": 流程类型,
     *     "createdAt": 创建时间,
     *     "updatedAt": 更新时间
     *   },
     *   "timestamp": 时间戳
     * }
     */
    @GetMapping("/type/{workflowType}")
    public ApiResponse<ApprovalWorkflow> getWorkflowByType(@PathVariable String workflowType) {
        try {
            ApprovalWorkflow workflow = approvalWorkflowService.getByWorkflowType(workflowType);
            if (workflow == null) {
                return error("未找到指定类型的审批流程");
            }
            return success(workflow);
        } catch (Exception e) {
            return exception(e, "获取审批流程");
        }
    }
    
    /**
     * 创建审批流程
     * POST /api/approval/workflow
     * 请求体:
     * {
     *   "workflowName": "流程名称",
     *   "workflowType": "流程类型"
     * }
     * 返回参数:
     * {
     *   "code": 200,
     *   "message": "审批流程创建成功",
     *   "data": {
     *     "id": 主键ID,
     *     "workflowName": 流程名称,
     *     "workflowType": 流程类型,
     *     "createdAt": 创建时间,
     *     "updatedAt": 更新时间
     *   },
     *   "timestamp": 时间戳
     * }
     */
    @PostMapping
    public ApiResponse<ApprovalWorkflow> createWorkflow(@RequestBody ApprovalWorkflow workflow) {
        try {
            boolean saved = approvalWorkflowService.save(workflow);
            if (saved) {
                return success(workflow, "审批流程创建成功");
            } else {
                return error("审批流程创建失败");
            }
        } catch (Exception e) {
            return exception(e, "创建审批流程");
        }
    }
    
    /**
     * 更新审批流程
     * PUT /api/approval/workflow/{id}
     * 请求参数:
     * - id: 审批流程ID
     * 请求体:
     * {
     *   "workflowName": "流程名称",
     *   "workflowType": "流程类型"
     * }
     * 返回参数:
     * {
     *   "code": 200,
     *   "message": "审批流程更新成功",
     *   "data": {
     *     "id": 主键ID,
     *     "workflowName": 流程名称,
     *     "workflowType": 流程类型,
     *     "createdAt": 创建时间,
     *     "updatedAt": 更新时间
     *   },
     *   "timestamp": 时间戳
     * }
     */
    @PutMapping("/{id}")
    public ApiResponse<ApprovalWorkflow> updateWorkflow(@PathVariable Long id, @RequestBody ApprovalWorkflow workflow) {
        try {
            workflow.setId(id);
            boolean updated = approvalWorkflowService.updateById(workflow);
            if (updated) {
                return success(workflow, "审批流程更新成功");
            } else {
                return error("审批流程更新失败");
            }
        } catch (Exception e) {
            return exception(e, "更新审批流程");
        }
    }
    
    /**
     * 删除审批流程
     * DELETE /api/approval/workflow/{id}
     * 请求参数:
     * - id: 审批流程ID
     * 返回参数:
     * {
     *   "code": 200,
     *   "message": "审批流程删除成功",
     *   "data": null,
     *   "timestamp": 时间戳
     * }
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable Long id) {
        try {
            boolean removed = approvalWorkflowService.removeById(id);
            if (removed) {
                return success(null, "审批流程删除成功");
            } else {
                return error("审批流程删除失败");
            }
        } catch (Exception e) {
            return exception(e, "删除审批流程");
        }
    }
}

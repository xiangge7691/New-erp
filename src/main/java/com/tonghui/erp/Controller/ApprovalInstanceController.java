package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Approval.ApprovalInstanceWithRecordsDto;
import com.tonghui.erp.Common.Dto.Approval.CancelRequest;
import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.tonghui.erp.Service.ApprovalInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批实例控制器
 */
@RestController
@RequestMapping("/api/approval/instance")
public class ApprovalInstanceController extends BaseController {

    @Autowired
    private ApprovalInstanceService approvalInstanceService;

    /**
     * 获取审批实例列表
     */
    @GetMapping
    public ApiResponse<PagedResult<ApprovalInstance>> listInstances(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ApprovalInstance> result;
            if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
                result = approvalInstanceService.getInstances(-1, -1);
            } else {
                result = approvalInstanceService.getInstances(pageRequest.getPageIndex(), pageRequest.getPageSize());
            }
            return success(result);
        } catch (Exception e) {
            return exception(e, "获取审批实例列表");
        }
    }

    /**
     * 根据ID获取审批实例详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ApprovalInstance> getInstanceById(@PathVariable Long id) {
        try {
            ApprovalInstance instance = approvalInstanceService.getById(id);
            if (instance == null) {
                return error("未找到指定的审批实例");
            }
            return success(instance);
        } catch (Exception e) {
            return exception(e, "获取审批实例详情");
        }
    }

    /**
     * 根据关联业务获取审批实例
     */
    @GetMapping("/related/{relatedId}/{relatedType}")
    public ApiResponse<ApprovalInstance> getInstanceByRelated(@PathVariable Long relatedId,
                                                              @PathVariable String relatedType) {
        try {
            ApprovalInstance instance = approvalInstanceService.getInstanceByRelated(relatedId, relatedType);
            if (instance == null) {
                return error("未找到指定的审批实例");
            }
            return success(instance);
        } catch (Exception e) {
            return exception(e, "获取审批实例");
        }
    }

    /**
     * 根据流程ID获取审批实例列表
     */
    @GetMapping("/workflow/{workflowId}")
    public ApiResponse<List<ApprovalInstance>> getInstancesByWorkflowId(@PathVariable Long workflowId) {
        try {
            List<ApprovalInstance> instances = approvalInstanceService.getInstancesByWorkflowId(workflowId);
            return success(instances);
        } catch (Exception e) {
            return exception(e, "获取审批实例列表");
        }
    }

    /**
     * 根据状态获取审批实例列表
     */
    @GetMapping("/status/{status}")
    public ApiResponse<List<ApprovalInstance>> getInstancesByStatus(@PathVariable String status) {
        try {
            List<ApprovalInstance> instances = approvalInstanceService.getInstancesByStatus(status);
            return success(instances);
        } catch (Exception e) {
            return exception(e, "获取审批实例列表");
        }
    }

    /**
     * 创建审批实例
     */
    @PostMapping
    public ApiResponse<ApprovalInstance> createInstance(@RequestBody ApprovalInstance instance) {
        try {
            instance.setInitiatorId(EntityUtils.getCurrentUserId());
            boolean saved = approvalInstanceService.save(instance);
            if (saved) {
                return success(instance, "审批实例创建成功");
            } else {
                return error("审批实例创建失败");
            }
        } catch (Exception e) {
            return exception(e, "创建审批实例");
        }
    }

    /**
     * 创建审批实例并绑定业务
     *
     * @param relatedType 业务类型（如 PURCHASE_ORDER）
     * @param relatedId   业务ID
     * @param workflowId  流程ID
     */
    @PostMapping("/create-with-binding")
    public ApiResponse<ApprovalInstance> createWithBinding(
            @RequestParam String relatedType,
            @RequestParam Long relatedId,
            @RequestParam Long workflowId) {
        try {
            Long initiatorId = EntityUtils.getCurrentUserId();
            ApprovalInstance instance = approvalInstanceService.createWithBinding(
                    relatedType, relatedId, workflowId, initiatorId);
            return success(instance, "审批实例创建成功");
        } catch (Exception e) {
            return exception(e, "创建审批实例");
        }
    }

    /**
     * 更新审批实例
     */
    @PutMapping("/{id}")
    public ApiResponse<ApprovalInstance> updateInstance(@PathVariable Long id, @RequestBody ApprovalInstance instance) {
        try {
            instance.setId(id);
            boolean updated = approvalInstanceService.updateById(instance);
            if (updated) {
                return success(instance, "审批实例更新成功");
            } else {
                return error("审批实例更新失败");
            }
        } catch (Exception e) {
            return exception(e, "更新审批实例");
        }
    }

    /**
     * 获取审批实例的当前处理角色列表
     */
    @GetMapping("/{id}/current-handler-roles")
    public ApiResponse<List<CurrentHandlerRoleDto>> getCurrentHandlerRoles(@PathVariable Long id) {
        try {
            List<CurrentHandlerRoleDto> roles = approvalInstanceService.getCurrentHandlerRoles(id);
            return success(roles);
        } catch (Exception e) {
            return exception(e, "获取当前处理角色列表");
        }
    }

    /**
     * 检查用户是否为当前审批实例的处理人
     */
    @GetMapping("/{id}/check-handler/{userId}")
    public ApiResponse<Boolean> isCurrentUserHandler(@PathVariable Long id, @PathVariable Long userId) {
        try {
            boolean isHandler = approvalInstanceService.isCurrentUserHandler(id, userId);
            return success(isHandler);
        } catch (Exception e) {
            return exception(e, "检查用户处理权限");
        }
    }

    /**
     * 删除审批实例
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInstance(@PathVariable Long id) {
        try {
            boolean removed = approvalInstanceService.removeById(id);
            if (removed) {
                return success(null, "审批实例删除成功");
            } else {
                return error("审批实例删除失败");
            }
        } catch (Exception e) {
            return exception(e, "删除审批实例");
        }
    }

    /**
     * 作废审批实例
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelInstance(@PathVariable Long id,
                                            @RequestBody CancelRequest request) {
        try {
            if (request == null || request.getUserId() == null) {
                return error("作废人 ID 不能为空");
            }
            if (request.getCancelReason() == null || request.getCancelReason().trim().isEmpty()) {
                return error("作废原因不能为空");
            }
            boolean cancelled = approvalInstanceService.cancelInstance(id, request.getUserId(), request.getCancelReason());
            if (cancelled) {
                return success(null, "审批实例作废成功");
            } else {
                return error("审批实例作废失败，可能该实例不是待审批状态");
            }
        } catch (Exception e) {
            return exception(e, "作废审批实例");
        }
    }

    /**
     * 同意审批
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approveInstance(@PathVariable Long id,
                                             @RequestParam(required = false) String remark) {
        try {
            Long userId = EntityUtils.getCurrentUserId();
            approvalInstanceService.approve(id, userId, remark);
            return success(null, "审批同意成功");
        } catch (Exception e) {
            return exception(e, "同意审批");
        }
    }

    /**
     * 驳回审批
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> rejectInstance(@PathVariable Long id,
                                            @RequestParam(required = false) String remark) {
        try {
            Long userId = EntityUtils.getCurrentUserId();
            approvalInstanceService.reject(id, userId, remark);
            return success(null, "审批驳回成功");
        } catch (Exception e) {
            return exception(e, "驳回审批");
        }
    }

    /**
     * 转交审批
     */
    @PostMapping("/{id}/transfer")
    public ApiResponse<Void> transferInstance(@PathVariable Long id,
                                              @RequestParam(required = false) String remark) {
        try {
            Long userId = EntityUtils.getCurrentUserId();
            approvalInstanceService.transfer(id, userId, remark);
            return success(null, "审批转交成功");
        } catch (Exception e) {
            return exception(e, "转交审批");
        }
    }

    /**
     * 查询审批实例（包含审批记录子表）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ApprovalInstanceWithRecordsDto>> searchWithDetails(
            @RequestParam int pageIndex,
            @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ApprovalInstanceWithRecordsDto> result = approvalInstanceService.searchWithDetails(safePageIndex, safePageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询审批实例");
        }
    }
}

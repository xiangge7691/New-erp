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
 * <p>
 * 提供审批实例的全生命周期管理，包括创建、查询、审批流程操作（同意/驳回/转交）及作废
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────────┬────────┬──────────────────────────────────┐
 * │ #  │ 接口                                             │ 方法   │ 说明                             │
 * ├────┼──────────────────────────────────────────────────┼────────┼──────────────────────────────────┤
 * │ 1  │ /api/approval/instance                           │ GET    │ 获取审批实例列表（分页）          │
 * │ 2  │ /api/approval/instance/{id}                      │ GET    │ 根据ID获取审批实例详情            │
 * │ 3  │ /api/approval/instance/related/{id}/{type}       │ GET    │ 根据关联业务获取审批实例          │
 * │ 4  │ /api/approval/instance/workflow/{workflowId}     │ GET    │ 根据流程ID获取审批实例列表        │
 * │ 5  │ /api/approval/instance/status/{status}           │ GET    │ 根据状态获取审批实例列表          │
 * │ 6  │ /api/approval/instance                           │ POST   │ 创建审批实例                     │
 * │ 7  │ /api/approval/instance/create-with-binding       │ POST   │ 创建审批实例并绑定业务            │
 * │ 8  │ /api/approval/instance/{id}                      │ PUT    │ 更新审批实例                     │
 * │ 9  │ /api/approval/instance/{id}/current-handler-roles│ GET    │ 获取当前处理角色列表              │
 * │ 10 │ /api/approval/instance/{id}/check-handler/{uid}  │ GET    │ 检查用户是否为当前处理人          │
 * │ 11 │ /api/approval/instance/{id}                      │ DELETE │ 删除审批实例                     │
 * │ 12 │ /api/approval/instance/{id}/cancel               │ POST   │ 作废审批实例                     │
 * │ 13 │ /api/approval/instance/{id}/approve              │ POST   │ 同意审批                         │
 * │ 14 │ /api/approval/instance/{id}/reject               │ POST   │ 驳回审批                         │
 * │ 15 │ /api/approval/instance/{id}/transfer             │ POST   │ 转交审批                         │
 * │ 16 │ /api/approval/instance/search-with-details       │ GET    │ 查询审批实例（含审批记录子表）     │
 * └────┴──────────────────────────────────────────────────┴────────┴──────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/approval/instance")
public class ApprovalInstanceController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批实例服务
     */
    @Autowired
    private ApprovalInstanceService approvalInstanceService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 获取审批实例列表（分页）
     *
     * 示例请求：
     * GET /api/approval/instance?pageIndex=0&pageSize=20
     *
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;ApprovalInstance&gt;&gt; 审批实例分页结果
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
     *
     * 示例请求：
     * GET /api/approval/instance/1
     *
     * @param id 审批实例ID（路径参数）
     * @return ApiResponse&lt;ApprovalInstance&gt; 审批实例详情
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
     *
     * 示例请求：
     * GET /api/approval/instance/related/100/PURCHASE_ORDER
     *
     * @param relatedId   业务单据ID（路径参数）
     * @param relatedType 业务类型（路径参数），如：PURCHASE_ORDER
     * @return ApiResponse&lt;ApprovalInstance&gt; 审批实例
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
     *
     * 示例请求：
     * GET /api/approval/instance/workflow/1
     *
     * @param workflowId 审批流程ID（路径参数）
     * @return ApiResponse&lt;List&lt;ApprovalInstance&gt;&gt; 审批实例列表
     */
// @GetMapping("/workflow/{workflowId}")
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
     *
     * 示例请求：
     * GET /api/approval/instance/status/PENDING
     *
     * @param status 审批状态（路径参数）：PENDING/APPROVED/REJECTED/CANCELLED/TRANSFERRED
     * @return ApiResponse&lt;List&lt;ApprovalInstance&gt;&gt; 审批实例列表
     */
// @GetMapping("/status/{status}")
    public ApiResponse<List<ApprovalInstance>> getInstancesByStatus(@PathVariable String status) {
        try {
            List<ApprovalInstance> instances = approvalInstanceService.getInstancesByStatus(status);
            return success(instances);
        } catch (Exception e) {
            return exception(e, "获取审批实例列表");
        }
    }

    /**
     * 获取审批实例的当前处理角色列表
     *
     * 示例请求：
     * GET /api/approval/instance/1/current-handler-roles
     *
     * @param id 审批实例ID（路径参数）
     * @return ApiResponse&lt;List&lt;CurrentHandlerRoleDto&gt;&gt; 当前处理角色列表
     */
// @GetMapping("/{id}/current-handler-roles")
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
     *
     * 示例请求：
     * GET /api/approval/instance/1/check-handler/10
     *
     * @param id     审批实例ID（路径参数）
     * @param userId 用户ID（路径参数）
     * @return ApiResponse&lt;Boolean&gt; true表示是当前处理人
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
     * 查询审批实例（包含审批记录子表）
     *
     * 示例请求：
     * GET /api/approval/instance/search-with-details?pageIndex=0&pageSize=20
     *
     * @param pageIndex 页码索引（请求参数），从0开始
     * @param pageSize  每页数量（请求参数）
     * @return ApiResponse&lt;PagedResult&lt;ApprovalInstanceWithRecordsDto&gt;&gt; 包含审批记录的分页结果
     */
// @GetMapping("/search-with-details")
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

    // endregion

    // region 创建接口
    // ===================================
    // 创建接口
    // ===================================

    /**
     * 创建审批实例
     *
     * 示例请求：
     * POST /api/approval/instance
     * Content-Type: application/json
     * {
     *   "workflowId": 1,
     *   "relatedId": 100,
     *   "relatedType": "PURCHASE_ORDER"
     * }
     *
     * @param instance 审批实例信息
     * @return ApiResponse&lt;ApprovalInstance&gt; 创建的审批实例
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
     * <p>
     * 创建审批实例并关联业务单据，自动定位到流程的第一个审批节点
     * </p>
     *
     * 示例请求：
     * POST /api/approval/instance/create-with-binding?relatedType=PURCHASE_ORDER&relatedId=100&workflowId=1
     *
     * @param relatedType 业务类型（请求参数），如：PURCHASE_ORDER
     * @param relatedId   业务ID（请求参数）
     * @param workflowId  审批流程ID（请求参数）
     * @return ApiResponse&lt;ApprovalInstance&gt; 创建的审批实例
     */
// @PostMapping("/create-with-binding")
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

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 更新审批实例
     *
     * 示例请求：
     * PUT /api/approval/instance/1
     * Content-Type: application/json
     * {
     *   "status": "APPROVED"
     * }
     *
     * @param id       审批实例ID（路径参数）
     * @param instance 更新的审批实例信息
     * @return ApiResponse&lt;ApprovalInstance&gt; 更新后的审批实例
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
     * 删除审批实例
     *
     * 示例请求：
     * DELETE /api/approval/instance/1
     *
     * @param id 审批实例ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
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

    // endregion

    // region 审批流程操作接口
    // ===================================
    // 审批流程操作接口
    // ===================================

    /**
     * 作废审批实例
     * <p>
     * 仅PENDING状态的实例可作废
     * </p>
     *
     * 示例请求：
     * POST /api/approval/instance/1/cancel
     * Content-Type: application/json
     * {
     *   "userId": 10,
     *   "cancelReason": "业务变更，取消审批"
     * }
     *
     * @param id      审批实例ID（路径参数）
     * @param request 作废请求（包含作废人ID和作废原因）
     * @return ApiResponse&lt;Void&gt; 操作结果
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
     * <p>
     * 当前登录用户同意审批，流转到下一节点或完成审批
     * </p>
     *
     * 示例请求：
     * POST /api/approval/instance/1/approve?remark=同意
     *
     * @param id     审批实例ID（路径参数）
     * @param remark 审批意见（可选请求参数）
     * @return ApiResponse&lt;Void&gt; 操作结果
     */
// @PostMapping("/{id}/approve")
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
     * <p>
     * 当前登录用户驳回审批，回退到指定节点或审批驳回
     * </p>
     *
     * 示例请求：
     * POST /api/approval/instance/1/reject?remark=资料不完整，请补充
     *
     * @param id     审批实例ID（路径参数）
     * @param remark 驳回原因（可选请求参数）
     * @return ApiResponse&lt;Void&gt; 操作结果
     */
// @PostMapping("/{id}/reject")
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
     * <p>
     * 当前登录用户将审批转交给其他人处理
     * </p>
     *
     * 示例请求：
     * POST /api/approval/instance/1/transfer?remark=本人出差，转交处理
     *
     * @param id     审批实例ID（路径参数）
     * @param remark 转交说明（可选请求参数）
     * @return ApiResponse&lt;Void&gt; 操作结果
     */
// @PostMapping("/{id}/transfer")
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

    // endregion
}

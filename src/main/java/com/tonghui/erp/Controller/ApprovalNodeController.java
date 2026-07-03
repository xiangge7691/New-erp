package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Approval.ApprovalNodeWithRecordsDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Service.ApprovalNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批节点定义控制器
 * <p>
 * 提供审批节点的CRUD操作及带审批记录的联合查询
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/approval/node                           │ GET    │ 获取审批节点列表（分页）      │
 * │ 2  │ /api/approval/node/{id}                      │ GET    │ 根据ID获取审批节点详情        │
 * │ 3  │ /api/approval/node/workflow/{workflowId}     │ GET    │ 根据流程ID获取所有节点        │
 * │ 4  │ /api/approval/node                           │ POST   │ 创建审批节点                 │
 * │ 5  │ /api/approval/node/{id}                      │ PUT    │ 更新审批节点                 │
 * │ 6  │ /api/approval/node/{id}                      │ DELETE │ 删除审批节点                 │
 * │ 7  │ /api/approval/node/search-with-details       │ GET    │ 查询审批节点（含审批记录子表） │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/approval/node")
public class ApprovalNodeController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批节点服务
     */
    @Autowired
    private ApprovalNodeService approvalNodeService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 获取审批节点列表（分页）
     *
     * 示例请求：
     * GET /api/approval/node?pageIndex=0&pageSize=20
     *
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;ApprovalNode&gt;&gt; 审批节点分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<ApprovalNode>> listNodes(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ApprovalNode> result = approvalNodeService.getNodes(pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception e) {
            return exception(e, "获取审批节点列表");
        }
    }

    /**
     * 根据ID获取审批节点详情
     *
     * 示例请求：
     * GET /api/approval/node/1
     *
     * @param id 审批节点ID（路径参数）
     * @return ApiResponse&lt;ApprovalNode&gt; 审批节点详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ApprovalNode> getNodeById(@PathVariable Long id) {
        try {
            ApprovalNode node = approvalNodeService.getById(id);
            if (node == null) {
                return error("未找到指定的审批节点");
            }
            return success(node);
        } catch (Exception e) {
            return exception(e, "获取审批节点详情");
        }
    }

    /**
     * 根据流程ID获取所有节点
     *
     * 示例请求：
     * GET /api/approval/node/workflow/1
     *
     * @param workflowId 审批流程ID（路径参数）
     * @return ApiResponse&lt;List&lt;ApprovalNode&gt;&gt; 审批节点列表
     */
    @GetMapping("/workflow/{workflowId}")
    public ApiResponse<List<ApprovalNode>> getNodesByWorkflowId(@PathVariable Long workflowId) {
        try {
            List<ApprovalNode> nodes = approvalNodeService.getNodesByWorkflowId(workflowId);
            return success(nodes);
        } catch (Exception e) {
            return exception(e, "获取审批节点列表");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 创建审批节点
     *
     * 示例请求：
     * POST /api/approval/node
     * Content-Type: application/json
     * {
     *   "workflowId": 1,
     *   "nodeName": "部门经理审批",
     *   "nodeOrder": 1,
     *   "roleId": 2
     * }
     *
     * @param node 审批节点信息
     * @return ApiResponse&lt;ApprovalNode&gt; 创建的审批节点
     */
    @PostMapping
    public ApiResponse<ApprovalNode> createNode(@RequestBody ApprovalNode node) {
        try {
            boolean saved = approvalNodeService.save(node);
            if (saved) {
                return success(node, "审批节点创建成功");
            } else {
                return error("审批节点创建失败");
            }
        } catch (Exception e) {
            return exception(e, "创建审批节点");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 更新审批节点
     *
     * 示例请求：
     * PUT /api/approval/node/1
     * Content-Type: application/json
     * {
     *   "nodeName": "部门经理审批（已修改）",
     *   "roleId": 3
     * }
     *
     * @param id   审批节点ID（路径参数）
     * @param node 更新的审批节点信息
     * @return ApiResponse&lt;ApprovalNode&gt; 更新后的审批节点
     */
    @PutMapping("/{id}")
    public ApiResponse<ApprovalNode> updateNode(@PathVariable Long id, @RequestBody ApprovalNode node) {
        try {
            node.setId(id);
            boolean updated = approvalNodeService.updateById(node);
            if (updated) {
                return success(node, "审批节点更新成功");
            } else {
                return error("审批节点更新失败");
            }
        } catch (Exception e) {
            return exception(e, "更新审批节点");
        }
    }

    /**
     * 删除审批节点
     *
     * 示例请求：
     * DELETE /api/approval/node/1
     *
     * @param id 审批节点ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNode(@PathVariable Long id) {
        try {
            boolean removed = approvalNodeService.removeById(id);
            if (removed) {
                return success(null, "审批节点删除成功");
            } else {
                return error("审批节点删除失败");
            }
        } catch (Exception e) {
            return exception(e, "删除审批节点");
        }
    }

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 查询审批节点（包含审批记录子表）
     * <p>
     * 分页查询审批节点，并关联加载每个节点的审批记录列表
     * </p>
     *
     * 示例请求：
     * GET /api/approval/node/search-with-details?pageIndex=0&pageSize=20
     *
     * @param approvalNode 查询条件对象（可选字段：id、workflowId、nodeName、nodeOrder、roleId）
     * @param pageIndex    页码索引（请求参数），从0开始
     * @param pageSize     每页数量（请求参数）
     * @return ApiResponse&lt;PagedResult&lt;ApprovalNodeWithRecordsDto&gt;&gt; 包含审批记录的分页结果
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ApprovalNodeWithRecordsDto>> searchWithDetails(ApprovalNode approvalNode,
                                                                                  @RequestParam int pageIndex,
                                                                                  @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ApprovalNodeWithRecordsDto> result = approvalNodeService.searchWithDetails(approvalNode, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion
}

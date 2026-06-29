package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Service.ApprovalNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批节点定义控制器
 */
@RestController
@RequestMapping("/api/approval/node")
public class ApprovalNodeController extends BaseController {

    @Autowired
    private ApprovalNodeService approvalNodeService;

    /**
     * 获取审批节点列表
     */
    @GetMapping
    public ApiResponse<PagedResult<ApprovalNode>> listNodes(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            // 使用 MyBatis Plus 分页查询
            PagedResult<ApprovalNode> result = approvalNodeService.getNodes(pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception e) {
            return exception(e, "获取审批节点列表");
        }
    }

    /**
     * 根据ID获取审批节点详情
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

    /**
     * 创建审批节点
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

    /**
     * 更新审批节点
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
}

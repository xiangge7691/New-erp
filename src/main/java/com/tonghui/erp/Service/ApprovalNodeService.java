package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Approval.ApprovalNodeWithRecordsDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;

import java.util.List;

/**
 * 审批节点服务接口
 */
public interface ApprovalNodeService extends IService<ApprovalNode> {
    /**
     * 根据流程ID 获取审批节点列表
     * @param workflowId 流程ID
     * @return 审批节点列表
     */
    List<ApprovalNode> getNodesByWorkflowId(Long workflowId);
    
    /**
     * 根据流程ID 和节点顺序获取审批节点
     * @param workflowId 流程ID
     * @param nodeOrder 节点顺序
     * @return 审批节点
     */
    ApprovalNode getNodeByWorkflowIdAndOrder(Long workflowId, Integer nodeOrder);
    
    /**
     * 获取审批节点列表（分页）
     * @param pageIndex 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PagedResult<ApprovalNode> getNodes(int pageIndex, int pageSize);

    Page<ApprovalNode> queryApprovalNodes(ApprovalNode approvalNode, int pageNum, int pageSize);

    PagedResult<ApprovalNodeWithRecordsDto> searchWithDetails(ApprovalNode approvalNode, int pageNum, int pageSize);
}

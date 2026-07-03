package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Approval.ApprovalNodeWithRecordsDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;

import java.util.List;

/**
 * 审批节点服务接口
 * <p>
 * 提供审批节点的CRUD操作及带子表的联合查询功能。
 * 审批节点定义了审批流程中每个步骤的处理角色和顺序
 * </p>
 */
public interface ApprovalNodeService extends IService<ApprovalNode> {

    /**
     * 根据流程ID获取审批节点列表
     * <p>
     * 查询指定审批流程下的所有节点，按节点顺序升序排列
     * </p>
     *
     * @param workflowId 流程ID
     * @return 审批节点列表，按nodeOrder升序
     */
    List<ApprovalNode> getNodesByWorkflowId(Long workflowId);
    
    /**
     * 根据流程ID和节点顺序获取审批节点
     *
     * @param workflowId 流程ID
     * @param nodeOrder  节点顺序
     * @return 审批节点，不存在时返回null
     */
    ApprovalNode getNodeByWorkflowIdAndOrder(Long workflowId, Integer nodeOrder);
    
    /**
     * 获取审批节点列表（分页）
     *
     * @param pageIndex 页码索引，从0开始
     * @param pageSize  每页数量
     * @return 分页结果
     */
    PagedResult<ApprovalNode> getNodes(int pageIndex, int pageSize);

    /**
     * 条件查询审批节点（分页）
     * <p>
     * 支持按ID、流程ID、节点名称、节点顺序、角色ID等条件筛选
     * </p>
     *
     * @param approvalNode 查询条件对象
     * @param pageNum      页码（从0开始）
     * @param pageSize     每页数量
     * @return MyBatis Plus分页结果
     */
    Page<ApprovalNode> queryApprovalNodes(ApprovalNode approvalNode, int pageNum, int pageSize);

    /**
     * 查询审批节点（包含审批记录子表）
     * <p>
     * 分页查询审批节点，并关联加载每个节点的审批记录列表
     * </p>
     *
     * @param approvalNode 查询条件对象
     * @param pageNum      页码（从0开始）
     * @param pageSize     每页数量
     * @return 包含审批记录的审批节点分页结果
     */
    PagedResult<ApprovalNodeWithRecordsDto> searchWithDetails(ApprovalNode approvalNode, int pageNum, int pageSize);
}

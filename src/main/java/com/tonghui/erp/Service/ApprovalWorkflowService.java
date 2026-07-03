package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Approval.ApprovalWorkflowWithNodesDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * 审批流程服务接口
 * <p>
 * 提供审批流程定义的CRUD操作及带节点子表的联合查询功能。
 * 审批流程定义了审批的步骤序列，每个流程由多个审批节点组成
 * </p>
 */
public interface ApprovalWorkflowService extends IService<ApprovalWorkflow> {

    /**
     * 根据流程类型获取审批流程
     *
     * @param workflowType 流程类型（如：PURCHASE_ORDER、PRODUCTION_PLAN等）
     * @return 审批流程，不存在时返回null
     */
    ApprovalWorkflow getByWorkflowType(String workflowType);
    
    /**
     * 获取所有审批流程
     *
     * @return 审批流程列表
     */
    List<ApprovalWorkflow> getAllWorkflows();

    /**
     * 查询审批流程（包含节点子表）
     * <p>
     * 分页查询审批流程，并关联加载每个流程的审批节点列表
     * </p>
     *
     * @param pageIndex 页码索引，从0开始；-1表示全量查询
     * @param pageSize  每页数量；-1表示全量查询
     * @return 包含审批节点的审批流程分页结果
     */
    PagedResult<ApprovalWorkflowWithNodesDto> searchWithDetails(int pageIndex, int pageSize);
}

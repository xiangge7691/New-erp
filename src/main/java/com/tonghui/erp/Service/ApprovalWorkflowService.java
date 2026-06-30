package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Approval.ApprovalWorkflowWithNodesDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
* @author 87954
* @description 针对表【approval_workflow(审批流程定义)】的数据库操作Service
* @createDate 2025-12-18 09:50:00
*/
public interface ApprovalWorkflowService extends IService<ApprovalWorkflow> {
    /**
     * 根据流程类型获取审批流程
     * @param workflowType 流程类型
     * @return 审批流程
     */
    ApprovalWorkflow getByWorkflowType(String workflowType);
    
    /**
     * 获取所有审批流程
     * @return 审批流程列表
     */
    List<ApprovalWorkflow> getAllWorkflows();

    /**
     * 查询审批流程（包含节点子表）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果（包含节点）
     */
    PagedResult<ApprovalWorkflowWithNodesDto> searchWithDetails(int pageIndex, int pageSize);
}

package com.tonghui.erp.Common.Dto.Approval;

import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 审批流程包含审批节点的扩展数据传输对象
 * <p>
 * 在审批流程基础上扩展了审批节点列表，用于展示完整的审批流程定义及所有节点配置
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalWorkflowWithNodesDto extends ApprovalWorkflow {

    /**
     * 该流程的审批节点列表，按节点顺序排列
     */
    private List<ApprovalNode> nodes;
}

package com.tonghui.erp.Common.Dto.Approval;

import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalWorkflowWithNodesDto extends ApprovalWorkflow {
    private List<ApprovalNode> nodes;
}

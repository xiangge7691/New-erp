package com.tonghui.erp.Common.Dto.Approval;

import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 审批节点包含审批记录的扩展数据传输对象
 * <p>
 * 在审批节点基础上扩展了审批记录列表，用于展示完整的审批节点及该节点下的所有审批操作记录
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalNodeWithRecordsDto extends ApprovalNode {

    /**
     * 该节点的审批记录列表
     */
    private List<ApprovalRecord> records;
}

package com.tonghui.erp.Common.Dto.Approval;

import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 审批实例包含审批记录的扩展数据传输对象
 * <p>
 * 在审批实例基础上扩展了审批记录列表，用于展示完整的审批流程详情及所有审批操作历史
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalInstanceWithRecordsDto extends ApprovalInstance {

    /**
     * 该实例的审批记录列表
     */
    private List<ApprovalRecord> records;
}

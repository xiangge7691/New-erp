package com.tonghui.erp.Common.Dto.Approval;

import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalInstanceWithRecordsDto extends ApprovalInstance {
    private List<ApprovalRecord> records;
}

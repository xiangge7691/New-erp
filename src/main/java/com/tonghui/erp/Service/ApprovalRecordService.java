package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 87954
* @description 针对表【approval_record(审批记录)】的数据库操作Service
* @createDate 2025-12-18 09:50:00
*/
public interface ApprovalRecordService extends IService<ApprovalRecord> {
    /**
     * 根据实例ID获取审批记录列表
     * @param instanceId 实例ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> getRecordsByInstanceId(Long instanceId);
    
    /**
     * 根据节点ID获取审批记录列表
     * @param nodeId 节点ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> getRecordsByNodeId(Long nodeId);
}

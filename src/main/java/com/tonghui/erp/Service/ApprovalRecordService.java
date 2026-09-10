package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审批记录服务接口
 * <p>
 * 提供审批记录的CRUD操作及按实例/节点查询功能。
 * 审批记录记录了每个审批操作的详细信息，包括操作人、操作类型、审批意见等
 * </p>
 */
public interface ApprovalRecordService extends IService<ApprovalRecord> {

    /**
     * 根据实例ID获取审批记录列表
     * <p>
     * 查询指定审批实例下的所有审批操作记录，按操作时间倒序排列
     * </p>
     *
     * @param instanceId 审批实例ID
     * @return 审批记录列表，按createdTime倒序
     */
    List<ApprovalRecord> getRecordsByInstanceId(Long instanceId);
    
    /**
     * 根据节点ID获取审批记录列表
     * <p>
     * 查询指定审批节点的所有审批操作记录，按操作时间倒序排列
     * </p>
     *
     * @param nodeId 审批节点ID
     * @return 审批记录列表，按createdTime倒序
     */
    List<ApprovalRecord> getRecordsByNodeId(Long nodeId);
}

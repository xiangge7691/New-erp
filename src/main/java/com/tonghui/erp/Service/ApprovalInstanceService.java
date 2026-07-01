package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Approval.ApprovalInstanceWithRecordsDto;
import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审批实例服务接口
 */
public interface ApprovalInstanceService extends IService<ApprovalInstance> {

    /**
     * 根据关联业务获取审批实例
     */
    ApprovalInstance getInstanceByRelated(Long relatedId, String relatedType);

    /**
     * 根据流程ID获取审批实例列表
     */
    List<ApprovalInstance> getInstancesByWorkflowId(Long workflowId);

    /**
     * 根据状态获取审批实例列表
     */
    List<ApprovalInstance> getInstancesByStatus(String status);

    /**
     * 获取审批实例列表（分页）
     */
    PagedResult<ApprovalInstance> getInstances(int pageIndex, int pageSize);

    /**
     * 获取审批实例的当前处理角色列表
     */
    List<CurrentHandlerRoleDto> getCurrentHandlerRoles(Long instanceId);

    /**
     * 检查用户是否为当前审批实例的处理人
     */
    boolean isCurrentUserHandler(Long instanceId, Long userId);

    /**
     * 作废审批实例
     */
    boolean cancelInstance(Long instanceId, Long userId, String cancelReason);

    /**
     * 查询审批实例（包含审批记录子表）
     */
    PagedResult<ApprovalInstanceWithRecordsDto> searchWithDetails(int pageIndex, int pageSize);

    // ========== 审批流程引擎 ==========

    /**
     * 同意当前节点
     * @param instanceId 审批实例ID
     * @param userId 审批人ID
     * @param remark 审批意见
     */
    void approve(Long instanceId, Long userId, String remark);

    /**
     * 驳回
     * @param instanceId 审批实例ID
     * @param userId 审批人ID
     * @param remark 驳回原因
     */
    void reject(Long instanceId, Long userId, String remark);

    /**
     * 转交
     * @param instanceId 审批实例ID
     * @param userId 转交人ID
     * @param remark 转交说明
     */
    void transfer(Long instanceId, Long userId, String remark);

    /**
     * 创建审批实例并绑定业务
     * @param relatedType 业务类型
     * @param relatedId 业务ID
     * @param workflowId 流程ID
     * @param initiatorId 发起人ID
     * @return 审批实例
     */
    ApprovalInstance createWithBinding(String relatedType, Long relatedId, Long workflowId, Long initiatorId);
}

package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Approval.ApprovalInstanceWithRecordsDto;
import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 87954
* @description 针对表【approval_instance(审批实例)】的数据库操作Service
* @createDate 2025-12-18 09:50:00
*/
public interface ApprovalInstanceService extends IService<ApprovalInstance> {
    /**
     * 根据关联业务获取审批实例
     * @param relatedId 关联业务ID
     * @param relatedType 关联业务类型
     * @return 审批实例
     */
    ApprovalInstance getInstanceByRelated(Long relatedId, String relatedType);
    
    /**
     * 根据流程ID获取审批实例列表
     * @param workflowId 流程ID
     * @return 审批实例列表
     */
    List<ApprovalInstance> getInstancesByWorkflowId(Long workflowId);
    
    /**
     * 根据状态获取审批实例列表
     * @param status 状态
     * @return 审批实例列表
     */
    List<ApprovalInstance> getInstancesByStatus(String status);
    
    /**
     * 获取审批实例列表（分页）
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页数量，-1表示不分页返回所有结果
     * @return 审批实例列表的分页结果
     */
    PagedResult<ApprovalInstance> getInstances(int pageIndex, int pageSize);
    
    /**
     * 获取审批实例的当前处理角色列表
     * @param id 审批实例ID
     * @return 当前需要处理的角色列表
     */
    List<CurrentHandlerRoleDto> getCurrentHandlerRoles(Long instanceId);
    
    /**
     * 检查用户是否为当前审批实例的处理人
     * @param instanceId 审批实例 ID
     * @param userId 用户 ID
     * @return true 表示用户需要处理该实例，false 表示不需要
     */
    boolean isCurrentUserHandler(Long instanceId, Long userId);
    
    /**
     * 作废审批实例
     * @param instanceId 审批实例 ID
     * @param userId 作废人用户 ID
     * @param cancelReason 作废原因
     * @return 操作是否成功
     */
    boolean cancelInstance(Long instanceId, Long userId, String cancelReason);

    /**
     * 查询审批实例（包含审批记录子表）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果（包含审批记录）
     */
    PagedResult<ApprovalInstanceWithRecordsDto> searchWithDetails(int pageIndex, int pageSize);
}

package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Approval.ApprovalInstanceWithRecordsDto;
import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审批实例服务接口
 * <p>
 * 提供审批实例的生命周期管理，包括创建、查询、审批流程引擎（同意/驳回/转交）及作废操作。
 * 审批实例绑定业务单据，通过审批流程定义驱动多级审批流转
 * </p>
 */
public interface ApprovalInstanceService extends IService<ApprovalInstance> {

    /**
     * 根据关联业务获取审批实例
     * <p>
     * 通过业务ID和业务类型查询对应的审批实例，用于检查业务单据是否已有审批流程
     * </p>
     *
     * @param relatedId   业务单据ID
     * @param relatedType 业务类型（如：PURCHASE_ORDER、PRODUCTION_PLAN等）
     * @return 审批实例，不存在时返回null
     */
    ApprovalInstance getInstanceByRelated(Long relatedId, String relatedType);

    /**
     * 根据流程ID获取审批实例列表
     *
     * @param workflowId 审批流程ID
     * @return 该流程下的所有审批实例列表
     */
    List<ApprovalInstance> getInstancesByWorkflowId(Long workflowId);

    /**
     * 根据状态获取审批实例列表
     *
     * @param status 审批状态（PENDING/APPROVED/REJECTED/CANCELLED/TRANSFERRED）
     * @return 指定状态的审批实例列表
     */
    List<ApprovalInstance> getInstancesByStatus(String status);

    /**
     * 获取审批实例列表（分页）
     * <p>
     * 支持分页查询和全量查询（pageIndex和pageSize均为-1时返回全量数据）
     * </p>
     *
     * @param pageIndex 页码索引，从0开始；-1表示全量查询
     * @param pageSize  每页数量；-1表示全量查询
     * @return 分页结果
     */
    PagedResult<ApprovalInstance> getInstances(int pageIndex, int pageSize);

    /**
     * 获取审批实例的当前处理角色列表
     * <p>
     * 返回该实例审批流程中所有节点对应的角色信息，标记当前待处理节点
     * </p>
     *
     * @param instanceId 审批实例ID
     * @return 当前处理角色列表
     */
    List<CurrentHandlerRoleDto> getCurrentHandlerRoles(Long instanceId);

    /**
     * 检查用户是否为当前审批实例的处理人
     *
     * @param instanceId 审批实例ID
     * @param userId     用户ID
     * @return true表示是当前处理人，false表示不是
     */
    boolean isCurrentUserHandler(Long instanceId, Long userId);

    /**
     * 作废审批实例
     * <p>
     * 仅PENDING状态的实例可作废，作废后记录作废原因和操作人
     * </p>
     *
     * @param instanceId   审批实例ID
     * @param userId       作废操作人ID
     * @param cancelReason 作废原因
     * @return true表示作废成功，false表示失败（状态不满足或实例不存在）
     */
    boolean cancelInstance(Long instanceId, Long userId, String cancelReason);

    /**
     * 查询审批实例（包含审批记录子表）
     * <p>
     * 分页查询审批实例，并关联加载每个实例的审批记录列表
     * </p>
     *
     * @param pageIndex 页码索引，从0开始
     * @param pageSize  每页数量
     * @return 包含审批记录的审批实例分页结果
     */
    PagedResult<ApprovalInstanceWithRecordsDto> searchWithDetails(int pageIndex, int pageSize);

    // ========== 审批流程引擎 ==========

    /**
     * 同意当前节点
     * <p>
     * 验证操作人权限后，记录同意操作并流转到下一节点；若无下一节点则审批通过
     * </p>
     *
     * @param instanceId 审批实例ID
     * @param userId     审批人ID
     * @param remark     审批意见
     */
    void approve(Long instanceId, Long userId, String remark);

    /**
     * 驳回审批
     * <p>
     * 验证操作人权限后，记录驳回操作。若当前节点配置了驳回目标节点则回退到指定节点，否则审批驳回
     * </p>
     *
     * @param instanceId 审批实例ID
     * @param userId     审批人ID
     * @param remark     驳回原因
     */
    void reject(Long instanceId, Long userId, String remark);

    /**
     * 转交审批
     * <p>
     * 验证操作人权限后，将当前审批转交给其他人处理，实例状态变为TRANSFERRED
     * </p>
     *
     * @param instanceId 审批实例ID
     * @param userId     转交人ID
     * @param remark     转交说明
     */
    void transfer(Long instanceId, Long userId, String remark);

    /**
     * 创建审批实例并绑定业务
     * <p>
     * 创建新的审批实例，关联业务单据和审批流程，并自动定位到流程的第一个节点
     * </p>
     *
     * @param relatedType 业务类型
     * @param relatedId   业务ID
     * @param workflowId  审批流程ID
     * @param initiatorId 发起人ID
     * @return 创建的审批实例
     */
    ApprovalInstance createWithBinding(String relatedType, Long relatedId, Long workflowId, Long initiatorId);
}

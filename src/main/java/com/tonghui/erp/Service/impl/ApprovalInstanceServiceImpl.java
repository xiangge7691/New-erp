package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Approval.ApprovalInstanceWithRecordsDto;
import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.mapper.ApprovalRecordMapper;
import com.tonghui.erp.Service.ApprovalInstanceService;
import com.tonghui.erp.Service.ApprovalNodeService;
import com.tonghui.erp.Service.ApprovalRecordService;
import com.tonghui.erp.Service.RoleService;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Data.mapper.ApprovalInstanceMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批实例服务实现类
 * <p>
 * 实现ApprovalInstanceService接口，提供审批实例的CRUD操作、审批流程引擎及权限验证。
 * 核心功能包括：同意/驳回/转交审批、实例作废、处理人权限校验
 * </p>
 */
@Service
public class ApprovalInstanceServiceImpl extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance>
    implements ApprovalInstanceService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批节点服务
     */
    @Autowired
    private ApprovalNodeService approvalNodeService;

    /**
     * 角色服务
     */
    @Autowired
    private RoleService roleService;

    /**
     * 用户角色服务
     */
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 审批记录服务
     */
    @Autowired
    private ApprovalRecordService approvalRecordService;

    /**
     * 审批记录Mapper
     */
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    // endregion

    // region 基础查询方法
    // ===================================
    // 基础查询方法
    // ===================================

    /**
     * 根据关联业务获取审批实例
     *
     * @param relatedId   业务单据ID
     * @param relatedType 业务类型
     * @return 审批实例
     */
    @Override
    public ApprovalInstance getInstanceByRelated(Long relatedId, String relatedType) {
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("related_id", relatedId);
        queryWrapper.eq("related_type", relatedType);
        return getOne(queryWrapper);
    }

    /**
     * 根据流程ID获取审批实例列表
     *
     * @param workflowId 审批流程ID
     * @return 审批实例列表
     */
    @Override
    public List<ApprovalInstance> getInstancesByWorkflowId(Long workflowId) {
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_id", workflowId);
        return list(queryWrapper);
    }

    /**
     * 根据状态获取审批实例列表
     *
     * @param status 审批状态
     * @return 审批实例列表
     */
    @Override
    public List<ApprovalInstance> getInstancesByStatus(String status) {
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return list(queryWrapper);
    }

    /**
     * 获取审批实例列表（分页或全量）
     *
     * @param pageIndex 页码索引，-1为全量
     * @param pageSize  每页数量，-1为全量
     * @return 分页结果
     */
    @Override
    public PagedResult<ApprovalInstance> getInstances(int pageIndex, int pageSize) {
        Page<ApprovalInstance> page;
        boolean isAllData = (pageIndex == -1 || pageSize == -1);
        if (isAllData) {
            page = new Page<>(1, Integer.MAX_VALUE);
        } else {
            page = new Page<>(pageIndex + 1, pageSize);
        }

        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_time");

        Page<ApprovalInstance> resultPage = this.page(page, queryWrapper);

        PagedResult<ApprovalInstance> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());
        pagedResult.setPageIndex(isAllData ? 0 : pageIndex);
        pagedResult.setPageSize((int) resultPage.getSize());

        if (isAllData) {
            pagedResult.setPageSize((int) resultPage.getTotal());
        }

        return pagedResult;
    }

    // endregion

    // region 权限与状态查询方法
    // ===================================
    // 权限与状态查询方法
    // ===================================

    /**
     * 获取审批实例的当前处理角色列表
     * <p>
     * 遍历流程所有节点，构建角色信息列表，标记当前待处理节点
     * </p>
     *
     * @param id 审批实例ID
     * @return 当前处理角色列表
     */
    @Override
    public List<CurrentHandlerRoleDto> getCurrentHandlerRoles(Long id) {
        List<CurrentHandlerRoleDto> result = new ArrayList<>();

        ApprovalInstance instance = getById(id);
        if (instance == null) {
            return result;
        }

        List<ApprovalNode> nodes = approvalNodeService.getNodesByWorkflowId(instance.getWorkflowId());
        if (nodes == null || nodes.isEmpty()) {
            return result;
        }

        nodes.sort((n1, n2) -> n1.getNodeOrder().compareTo(n2.getNodeOrder()));

        boolean isPending = "PENDING".equals(instance.getStatus());
        boolean isTransferred = "TRANSFERRED".equals(instance.getStatus());

        if (!isPending && !isTransferred) {
            return result;
        }

        // 查找当前节点
        ApprovalNode currentNode = null;
        if (instance.getCurrentNodeId() != null) {
            currentNode = nodes.stream()
                .filter(node -> node.getId().equals(instance.getCurrentNodeId()))
                .findFirst()
                .orElse(null);
        }

        if (currentNode == null && !nodes.isEmpty()) {
            currentNode = nodes.get(0);
        }

        // 构建角色信息列表
        for (ApprovalNode node : nodes) {
            if (node.getRoleId() == null) continue;

            com.tonghui.erp.Data.Entity.Role role = roleService.getById(node.getRoleId());
            if (role == null) continue;

            CurrentHandlerRoleDto roleDto = new CurrentHandlerRoleDto();
            roleDto.setRoleId(role.getRoleId());
            roleDto.setRoleName(role.getRoleName());
            roleDto.setRoleDesc(role.getRoleDesc());
            roleDto.setNodeId(node.getId());
            roleDto.setNodeName(node.getNodeName());
            roleDto.setNodeOrder(node.getNodeOrder());

            boolean isCurrentNode = currentNode != null &&
                currentNode.getId().equals(node.getId());
            roleDto.setIsCurrentNode(isCurrentNode);

            if (isCurrentNode) {
                roleDto.setStatusDescription("当前待处理");
            } else if (currentNode != null && node.getNodeOrder() < currentNode.getNodeOrder()) {
                roleDto.setStatusDescription("已处理");
            } else {
                roleDto.setStatusDescription("待处理");
            }

            // 查询角色下的用户列表
            List<UserRole> userRoles = userRoleService.list(
                new QueryWrapper<UserRole>().eq("role_id", role.getRoleId())
            );
            if (userRoles != null && !userRoles.isEmpty()) {
                List<String> roleIds = userRoles.stream()
                    .map(ur -> String.valueOf(ur.getRoleId()))
                    .collect(Collectors.toList());
                roleDto.setUserList(roleIds);
            }

            result.add(roleDto);
        }

        return result;
    }

    /**
     * 检查用户是否为当前审批实例的处理人
     *
     * @param id     审批实例ID
     * @param userId 用户ID
     * @return true表示是当前处理人
     */
    @Override
    public boolean isCurrentUserHandler(Long id, Long userId) {
        List<CurrentHandlerRoleDto> handlerRoles = getCurrentHandlerRoles(id);

        for (CurrentHandlerRoleDto roleDto : handlerRoles) {
            if (Boolean.TRUE.equals(roleDto.getIsCurrentNode())) {
                List<UserRole> userRoles = userRoleService.list(
                    new QueryWrapper<UserRole>().eq("role_id", roleDto.getRoleId())
                );

                boolean userInRole = userRoles.stream()
                    .anyMatch(ur -> ur.getUserId() != null && ur.getUserId().equals(userId));

                if (userInRole) {
                    return true;
                }
            }
        }

        return false;
    }

    // endregion

    // region 作废操作
    // ===================================
    // 作废操作
    // ===================================

    /**
     * 作废审批实例
     *
     * @param instanceId   审批实例ID
     * @param userId       作废操作人ID
     * @param cancelReason 作废原因
     * @return true表示作废成功
     */
    @Override
    public boolean cancelInstance(Long instanceId, Long userId, String cancelReason) {
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            return false;
        }

        // 仅PENDING状态可作废
        if (!"PENDING".equals(instance.getStatus())) {
            return false;
        }

        instance.setStatus("CANCELLED");
        instance.setCancelReason(cancelReason);
        instance.setCancelledBy(userId);
        instance.setCancelledAt(LocalDateTime.now());
        instance.setUpdatedTime(LocalDateTime.now());

        boolean updated = updateById(instance);
        if (!updated) {
            return false;
        }

        // 记录作废操作到审批记录
        try {
            ApprovalRecord record = new ApprovalRecord();
            record.setInstanceId(instanceId);
            record.setNodeId(instance.getCurrentNodeId());
            record.setApproverId(userId);
            record.setAction("CANCEL");
            record.setComment(cancelReason);
            record.setApprovedAt(LocalDateTime.now());
            record.setCreatedTime(LocalDateTime.now());
            approvalRecordService.save(record);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    // endregion

    // region 带子表查询方法
    // ===================================
    // 带子表查询方法
    // ===================================

    /**
     * 查询审批实例（包含审批记录子表）
     *
     * @param pageIndex 页码索引
     * @param pageSize  每页数量
     * @return 包含审批记录的分页结果
     */
    @Override
    public PagedResult<ApprovalInstanceWithRecordsDto> searchWithDetails(int pageIndex, int pageSize) {
        PagedResult<ApprovalInstance> parentPage = getInstances(pageIndex, pageSize);
        List<ApprovalInstance> parents = parentPage.getItems();

        PagedResult<ApprovalInstanceWithRecordsDto> result = new PagedResult<>();
        if (parents == null || parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotalCount());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的审批记录
        List<Long> parentIds = parents.stream().map(ApprovalInstance::getId).collect(Collectors.toList());
        QueryWrapper<ApprovalRecord> wrapper = new QueryWrapper<>();
        wrapper.in("instance_id", parentIds);
        List<ApprovalRecord> allRecords = approvalRecordMapper.selectList(wrapper);
        Map<Long, List<ApprovalRecord>> recordsMap = allRecords.stream()
                .collect(Collectors.groupingBy(ApprovalRecord::getInstanceId));

        // 组装DTO
        List<ApprovalInstanceWithRecordsDto> dtos = parents.stream().map(parent -> {
            ApprovalInstanceWithRecordsDto dto = new ApprovalInstanceWithRecordsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setRecords(recordsMap.getOrDefault(parent.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotalCount());
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 审批流程引擎
    // ===================================
    // 审批流程引擎
    // ===================================

    /**
     * 同意当前节点
     * <p>
     * 验证实例状态和操作人权限，记录同意操作，流转到下一节点或完成审批
     * </p>
     *
     * @param instanceId 审批实例ID
     * @param userId     审批人ID
     * @param remark     审批意见
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long instanceId, Long userId, String remark) {
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("审批实例不存在");
        }
        if (!"PENDING".equals(instance.getStatus()) && !"TRANSFERRED".equals(instance.getStatus())) {
            throw new RuntimeException("当前状态不允许审批操作");
        }
        if (!isCurrentUserHandler(instanceId, userId)) {
            throw new RuntimeException("您不是当前节点的审批人");
        }

        ApprovalNode currentNode = getCurrentNode(instance);
        if (currentNode == null) {
            throw new RuntimeException("当前节点不存在");
        }

        // 记录审批操作
        saveRecord(instanceId, currentNode.getId(), userId, "AGREE", remark, null);

        // 查找下一个节点
        ApprovalNode nextNode = getNextNode(instance.getWorkflowId(), currentNode.getNodeOrder());

        if (nextNode != null) {
            // 有下一个节点，更新当前节点
            instance.setCurrentNodeId(nextNode.getId());
            instance.setStatus("PENDING");
        } else {
            // 没有下一个节点，审批通过
            instance.setStatus("APPROVED");
        }

        instance.setUpdatedTime(LocalDateTime.now());
        updateById(instance);
    }

    /**
     * 驳回审批
     * <p>
     * 验证实例状态和操作人权限，记录驳回操作。若配置了驳回目标节点则回退，否则审批驳回
     * </p>
     *
     * @param instanceId 审批实例ID
     * @param userId     审批人ID
     * @param remark     驳回原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long instanceId, Long userId, String remark) {
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("审批实例不存在");
        }
        if (!"PENDING".equals(instance.getStatus()) && !"TRANSFERRED".equals(instance.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回操作");
        }
        if (!isCurrentUserHandler(instanceId, userId)) {
            throw new RuntimeException("您不是当前节点的审批人");
        }

        ApprovalNode currentNode = getCurrentNode(instance);
        if (currentNode == null) {
            throw new RuntimeException("当前节点不存在");
        }

        // 记录驳回操作
        saveRecord(instanceId, currentNode.getId(), userId, "REJECT", remark, null);

        // 判断驳回到哪个节点
        Long rejectToNodeId = currentNode.getRejectToNodeId();
        if (rejectToNodeId != null) {
            // 驳回到指定节点，实例保持PENDING
            instance.setCurrentNodeId(rejectToNodeId);
            instance.setStatus("PENDING");
        } else {
            // 没有指定驳回节点，审批驳回
            instance.setStatus("REJECTED");
        }

        instance.setUpdatedTime(LocalDateTime.now());
        updateById(instance);
    }

    /**
     * 转交审批
     * <p>
     * 验证实例状态和操作人权限，记录转交操作，实例状态变为TRANSFERRED
     * </p>
     *
     * @param instanceId 审批实例ID
     * @param userId     转交人ID
     * @param remark     转交说明
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long instanceId, Long userId, String remark) {
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("审批实例不存在");
        }
        if (!"PENDING".equals(instance.getStatus())) {
            throw new RuntimeException("当前状态不允许转交操作");
        }
        if (!isCurrentUserHandler(instanceId, userId)) {
            throw new RuntimeException("您不是当前节点的审批人");
        }

        ApprovalNode currentNode = getCurrentNode(instance);
        if (currentNode == null) {
            throw new RuntimeException("当前节点不存在");
        }

        // 记录转交操作
        saveRecord(instanceId, currentNode.getId(), userId, "TRANSFER", remark, currentNode.getId());

        // 转交后实例状态变为TRANSFERRED，等待新处理人
        instance.setStatus("TRANSFERRED");
        instance.setUpdatedTime(LocalDateTime.now());
        updateById(instance);
    }

    /**
     * 创建审批实例并绑定业务
     *
     * @param relatedType 业务类型
     * @param relatedId   业务ID
     * @param workflowId  审批流程ID
     * @param initiatorId 发起人ID
     * @return 创建的审批实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance createWithBinding(String relatedType, Long relatedId, Long workflowId, Long initiatorId) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setWorkflowId(workflowId);
        instance.setRelatedId(relatedId);
        instance.setRelatedType(relatedType);
        instance.setInitiatorId(initiatorId);
        instance.setStatus("PENDING");
        instance.setIsDeleted(0);
        instance.setCreatedTime(LocalDateTime.now());
        instance.setUpdatedTime(LocalDateTime.now());

        // 获取流程的第一个节点
        List<ApprovalNode> nodes = approvalNodeService.getNodesByWorkflowId(workflowId);
        if (nodes != null && !nodes.isEmpty()) {
            nodes.sort((n1, n2) -> n1.getNodeOrder().compareTo(n2.getNodeOrder()));
            instance.setCurrentNodeId(nodes.get(0).getId());
        }

        save(instance);
        return instance;
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 获取当前审批节点
     *
     * @param instance 审批实例
     * @return 当前节点，不存在时返回null
     */
    private ApprovalNode getCurrentNode(ApprovalInstance instance) {
        if (instance.getCurrentNodeId() == null) {
            return null;
        }
        return approvalNodeService.getById(instance.getCurrentNodeId());
    }

    /**
     * 获取下一个审批节点
     *
     * @param workflowId       流程ID
     * @param currentNodeOrder 当前节点顺序
     * @return 下一个节点，不存在时返回null
     */
    private ApprovalNode getNextNode(Long workflowId, Integer currentNodeOrder) {
        List<ApprovalNode> nodes = approvalNodeService.getNodesByWorkflowId(workflowId);
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        nodes.sort((n1, n2) -> n1.getNodeOrder().compareTo(n2.getNodeOrder()));
        for (ApprovalNode node : nodes) {
            if (node.getNodeOrder() > currentNodeOrder) {
                return node;
            }
        }
        return null;
    }

    /**
     * 保存审批操作记录
     *
     * @param instanceId   审批实例ID
     * @param nodeId       节点ID
     * @param userId       操作人ID
     * @param action       操作类型（AGREE/REJECT/TRANSFER/CANCEL）
     * @param remark       操作备注
     * @param targetNodeId 转交目标节点ID（仅转交操作时使用）
     */
    private void saveRecord(Long instanceId, Long nodeId, Long userId, String action, String remark, Long targetNodeId) {
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(nodeId);
        record.setApproverId(userId);
        record.setAction(action);
        record.setComment(remark);
        record.setTargetNodeId(targetNodeId);
        record.setApprovedAt(LocalDateTime.now());
        record.setCreatedTime(LocalDateTime.now());
        approvalRecordService.save(record);
    }

    // endregion
}

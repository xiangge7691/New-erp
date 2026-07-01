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
 * 审批实例服务实现
 */
@Service
public class ApprovalInstanceServiceImpl extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance>
    implements ApprovalInstanceService {

    @Autowired
    private ApprovalNodeService approvalNodeService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private ApprovalRecordService approvalRecordService;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Override
    public ApprovalInstance getInstanceByRelated(Long relatedId, String relatedType) {
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("related_id", relatedId);
        queryWrapper.eq("related_type", relatedType);
        return getOne(queryWrapper);
    }

    @Override
    public List<ApprovalInstance> getInstancesByWorkflowId(Long workflowId) {
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_id", workflowId);
        return list(queryWrapper);
    }

    @Override
    public List<ApprovalInstance> getInstancesByStatus(String status) {
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return list(queryWrapper);
    }

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

    @Override
    public boolean cancelInstance(Long instanceId, Long userId, String cancelReason) {
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            return false;
        }

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

        List<Long> parentIds = parents.stream().map(ApprovalInstance::getId).collect(Collectors.toList());
        QueryWrapper<ApprovalRecord> wrapper = new QueryWrapper<>();
        wrapper.in("instance_id", parentIds);
        List<ApprovalRecord> allRecords = approvalRecordMapper.selectList(wrapper);
        Map<Long, List<ApprovalRecord>> recordsMap = allRecords.stream()
                .collect(Collectors.groupingBy(ApprovalRecord::getInstanceId));

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

    // ========== 审批流程引擎 ==========

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

    // ========== 私有方法 ==========

    private ApprovalNode getCurrentNode(ApprovalInstance instance) {
        if (instance.getCurrentNodeId() == null) {
            return null;
        }
        return approvalNodeService.getById(instance.getCurrentNodeId());
    }

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
}

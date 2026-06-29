package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Service.ApprovalInstanceService;
import com.tonghui.erp.Service.ApprovalNodeService;
import com.tonghui.erp.Service.ApprovalRecordService;
import com.tonghui.erp.Service.RoleService;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Data.mapper.ApprovalInstanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* @author 87954
* @description 针对表【approval_instance(审批实例)】的数据库操作Service实现
* @createDate 2025-12-18 09:50:00
*/
@Service
public class ApprovalInstanceServiceImpl extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance>
    implements ApprovalInstanceService{
    
    @Autowired
    private ApprovalNodeService approvalNodeService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private ApprovalRecordService approvalRecordService;
    
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
        // 创建Page对象，处理全量数据的情况
        Page<ApprovalInstance> page;
        boolean isAllData = (pageIndex == -1 || pageSize == -1);
        if (isAllData) {
            // 获取所有数据
            page = new Page<>(1, Integer.MAX_VALUE);
        } else {
            // 页码从0开始，但MyBatis Plus的Page页码从1开始，所以需要+1
            page = new Page<>(pageIndex + 1, pageSize);
        }

        // 构建查询条件
        QueryWrapper<ApprovalInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at"); // 按创建时间倒序排列

        Page<ApprovalInstance> resultPage = this.page(page, queryWrapper);

        PagedResult<ApprovalInstance> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());
        pagedResult.setPageIndex(isAllData ? 0 : pageIndex);
        pagedResult.setPageSize((int) resultPage.getSize());

        // 如果是全量数据，调整pageSize为实际记录数
        if (isAllData) {
            pagedResult.setPageSize((int) resultPage.getTotal());
        }

        return pagedResult;
    }
    
    @Override
    public List<CurrentHandlerRoleDto> getCurrentHandlerRoles(Long id) {
        List<CurrentHandlerRoleDto> result = new ArrayList<>();
        
        // 获取审批实例
        ApprovalInstance instance = getById(id);
        if (instance == null) {
            return result;
        }
        
        // 获取流程中的所有节点
        List<ApprovalNode> nodes = approvalNodeService.getNodesByWorkflowId(instance.getWorkflowId());
        if (nodes == null || nodes.isEmpty()) {
            return result;
        }
        
        // 按节点顺序排序
        nodes.sort((n1, n2) -> n1.getNodeOrder().compareTo(n2.getNodeOrder()));
        
        // 判断当前实例状态
        boolean isPending = "PENDING".equals(instance.getStatus());
        boolean isTransferred = "TRANSFERRED".equals(instance.getStatus());
        
        // 如果实例已完成或已驳回，则没有需要处理的角色
        if (!isPending && !isTransferred) {
            return result;
        }
        
        // 获取当前节点
        ApprovalNode currentNode = null;
        if (instance.getCurrentNodeId() != null) {
            currentNode = nodes.stream()
                .filter(node -> node.getId().equals(instance.getCurrentNodeId()))
                .findFirst()
                .orElse(null);
        }
        
        // 如果没有当前节点，取第一个节点作为当前节点
        if (currentNode == null && !nodes.isEmpty()) {
            currentNode = nodes.get(0);
        }
        
        // 构建所有涉及的角色信息
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
            
            // 判断是否为当前处理节点
            boolean isCurrentNode = currentNode != null && 
                currentNode.getId().equals(node.getId());
            roleDto.setIsCurrentNode(isCurrentNode);
            
            // 设置状态描述
            if (isCurrentNode) {
                roleDto.setStatusDescription("当前待处理");
            } else if (node.getNodeOrder() < currentNode.getNodeOrder()) {
                roleDto.setStatusDescription("已处理");
            } else {
                roleDto.setStatusDescription("待处理");
            }
            
            // 获取该角色下的用户列表
            List<UserRole> userRoles = userRoleService.list(
                new QueryWrapper<UserRole>().eq("role_id", role.getRoleId())
            );
            if (userRoles != null && !userRoles.isEmpty()) {
                List<String> roleIds = userRoles.stream()
                    .map(ur -> String.valueOf(ur.getRoleId()))
                    .collect(java.util.stream.Collectors.toList());
                roleDto.setUserList(roleIds);

            }
            
            result.add(roleDto);
        }
        
        return result;
    }
    
    @Override
    public boolean isCurrentUserHandler(Long id, Long userId) {
        // 获取当前处理角色列表
        List<CurrentHandlerRoleDto> handlerRoles = getCurrentHandlerRoles(id);
        
        // 检查用户所属的角色是否在当前处理角色列表中
        for (CurrentHandlerRoleDto roleDto : handlerRoles) {
            if (Boolean.TRUE.equals(roleDto.getIsCurrentNode())) {
                // 获取该角色下的用户
                List<UserRole> userRoles = userRoleService.list(
                    new QueryWrapper<UserRole>().eq("role_id", roleDto.getRoleId())
                );
                
                // 检查用户是否属于该角色
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
        // 获取审批实例
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            return false;
        }
        
        // 检查实例状态，只有待审批的实例才能作废
        if (!"PENDING".equals(instance.getStatus())) {
            return false;
        }
        
        // 更新实例状态为已作废
        instance.setStatus("CANCELLED");
        instance.setCancelReason(cancelReason);
        instance.setCancelledBy(userId);
        instance.setCancelledAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新
        boolean updated = updateById(instance);
        if (!updated) {
            return false;
        }
        
        // 记录作废操作到审批记录表
        try {
            ApprovalRecord record = new ApprovalRecord();
            record.setInstanceId(instanceId);
            record.setNodeId(instance.getCurrentNodeId());
            record.setApproverId(userId);
            record.setAction("CANCEL");
            record.setComment(cancelReason);
            record.setCreatedAt(LocalDateTime.now());
            approvalRecordService.save(record);
        } catch (Exception e) {
            // 记录日志但不影响主流程
            e.printStackTrace();
        }
        
        return true;
    }
}





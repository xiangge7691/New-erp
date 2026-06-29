package com.tonghui.erp.Common.Dto.Approval;

import lombok.Data;
import java.util.List;

/**
 * 当前处理角色信息DTO
 * <p>
 * 用于封装审批实例中当前需要处理的角色信息，包括角色基本信息和处理状态
 * </p>
 */
@Data
public class CurrentHandlerRoleDto {
    
    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色描述
     */
    private String roleDesc;
    
    /**
     * 是否为当前处理节点
     * true表示当前实例正在该角色对应的节点上等待处理
     */
    private Boolean isCurrentNode;
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 节点顺序
     */
    private Integer nodeOrder;
    
    /**
     * 该角色下的用户列表（可选）
     */
    private List<String> userList;
    
    /**
     * 处理状态说明
     */
    private String statusDescription;
}
package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审批节点定义
 * @TableName approval_node
 */
@TableName(value ="approval_node")
@Data
public class ApprovalNode {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程ID
     */
    @TableField(value = "workflow_id")
    private Long workflowId;

    /**
     * 节点名称
     */
    @TableField(value = "node_name")
    private String nodeName;

    /**
     * 节点顺序
     */
    @TableField(value = "node_order")
    private Integer nodeOrder;

    /**
     * 审批角色ID
     */
    @TableField(value = "role_id")
    private Long roleId;

    /**
     * 是否需要绑定业务
     */
    @TableField(value = "need_bind_business")
    private Boolean needBindBusiness;

    /**
     * 创建绑定类型
     */
    @TableField(value = "bind_type")
    private String bindType;

    /**
     * 进入节点提示
     */
    @TableField(value = "enter_node_prompt")
    private String enterNodePrompt;

    /**
     * 通过后业务状态
     */
    @TableField(value = "after_pass_status")
    private String afterPassStatus;

    /**
     * 驳回提示
     */
    @TableField(value = "reject_prompt")
    private String rejectPrompt;

    /**
     * 驳回后业务状态（用于配置驳回时业务单据的目标状态）
     */
    @TableField(value = "after_reject_status")
    private String afterRejectStatus;

    /**
     * 驳回到哪个节点（为null时驳回到第一个节点）
     */
    @TableField(value = "reject_to_node_id")
    private Long rejectToNodeId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;


// ========== 关联表显示字段（非数据库字段）==========

    /**
     * 流程名称（关联approval_workflow表）
     */
    @TableField(exist = false)
    private String workflowName;

    /**
     * 角色名称（关联role表）
     */
    @TableField(exist = false)
    private String roleName;
}

package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审批实例
 * @TableName approval_instance
 */
@TableName(value ="approval_instance")
@Data
public class ApprovalInstance {
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
     * 关联业务ID(如采购订单ID)
     */
    @TableField(value = "related_id")
    private Long relatedId;

    /**
     * 关联业务类型(PURCHASE_ORDER等)
     */
    @TableField(value = "related_type")
    private String relatedType;

    /**
     * 当前节点ID
     */
    @TableField(value = "current_node_id")
    private Long currentNodeId;

    /**
     * 审批状态 (PENDING-待审批/APPROVED-已同意/REJECTED-已驳回/TRANSFERRED-已转交/ARCHIVED-已归档/CANCELLED-已作废)
     */
    @TableField(value = "status")
    private String status;
    
    /**
     * 作废原因
     */
    @TableField(value = "cancel_reason")
    private String cancelReason;
    
    /**
     * 作废人 ID
     */
    @TableField(value = "cancelled_by")
    private Long cancelledBy;
    
    /**
     * 作废时间
     */
    @TableField(value = "cancelled_at")
    private LocalDateTime cancelledAt;

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
     * 当前节点名称（关联approval_node表）
     */
    @TableField(exist = false)
    private String currentNodeName;
}
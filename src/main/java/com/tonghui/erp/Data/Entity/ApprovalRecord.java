package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审批记录
 * @TableName approval_record
 */
@TableName(value ="approval_record")
@Data
public class ApprovalRecord {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 审批实例ID
     */
    @TableField(value = "instance_id")
    private Long instanceId;

    /**
     * 节点ID
     */
    @TableField(value = "node_id")
    private Long nodeId;

    /**
     * 审批人ID
     */
    @TableField(value = "approver_id")
    private Long approverId;

    /**
     * 审批动作 (AGREE-同意/REJECT-驳回/TRANSFER-转交/CANCEL-作废)
     */
    @TableField(value = "action")
    private String action;

    /**
     * 转交目标节点ID
     */
    @TableField(value = "target_node_id")
    private Long targetNodeId;

    /**
     * 审批意见
     */
    @TableField(value = "comment")
    private String comment;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;
}
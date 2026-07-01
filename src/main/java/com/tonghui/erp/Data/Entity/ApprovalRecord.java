package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批记录
 * @TableName approval_record
 */
@TableName(value ="approval_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalRecord extends AuditEntity {
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
     * 审批时间
     */
    @TableField(value = "approved_at")
    private LocalDateTime approvedAt;


// ========== 关联表显示字段（非数据库字段）==========

    /**
     * 节点名称（关联approval_node表）
     */
    @TableField(exist = false)
    private String nodeName;

    /**
     * 审批人姓名（关联user表）
     */
    @TableField(exist = false)
    private String approverName;

    /**
     * 转交目标节点名称（关联approval_node表）
     */
    @TableField(exist = false)
    private String targetNodeName;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;
}

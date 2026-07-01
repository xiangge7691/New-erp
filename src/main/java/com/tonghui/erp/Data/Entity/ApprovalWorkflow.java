package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程定义
 * @TableName approval_workflow
 */
@TableName(value ="approval_workflow")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalWorkflow extends AuditEntity {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程名称
     */
    @TableField(value = "workflow_name")
    private String workflowName;

    /**
     * 流程类型(PURCHASE_PREP-制剂室采购流程/PURCHASE_OUTSOURCING-委托加工采购流程等)
     */
    @TableField(value = "workflow_type")
    private String workflowType;

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "status")
    private Integer status;

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

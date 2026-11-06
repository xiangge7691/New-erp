package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检验任务步骤实体
 * <p>
 * 记录检验任务六个环节的引用记录、办理人、状态和跳过原因，
 * 避免给已有的检验计划/请检/取样/留样/检验/销毁表增加流程字段。
 * </p>
 *
 * @TableName inspection_task_step
 */
@TableName(value = "inspection_task_step")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionTaskStep extends AuditEntity {

    // region 基本信息
    // ===================================
    // 基本信息
    // ===================================

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 检验任务ID */
    @TableField(value = "task_id")
    private Long taskId;

    /** 环节：PLAN/REQUEST/SAMPLING/RETENTION/INSPECTION/DESTRUCTION */
    @TableField(value = "step_code")
    private String stepCode;

    /** 状态：待办理/进行中/已完成/已跳过 */
    @TableField(value = "step_status")
    private String stepStatus;

    /** 引用表名 */
    @TableField(value = "ref_table")
    private String refTable;

    /** 引用记录ID */
    @TableField(value = "ref_id")
    private Long refId;

    /** 办理人ID */
    @TableField(value = "operator_id")
    private Long operatorId;

    /** 办理人姓名 */
    @TableField(value = "operator_name")
    private String operatorName;

    /** 开始时间 */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /** 完成时间 */
    @TableField(value = "complete_time")
    private LocalDateTime completeTime;

    /** 跳过原因 */
    @TableField(value = "skip_reason")
    private String skipReason;

    /** 备注 */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 审计与乐观锁
    // ===================================
    // 审计与乐观锁
    // ===================================

    /** 是否删除：0否/1是 */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @TableField(value = "version")
    private Integer version;

    // endregion
}

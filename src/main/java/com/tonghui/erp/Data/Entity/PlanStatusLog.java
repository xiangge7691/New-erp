package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 计划状态流水表
 * @TableName plan_status_log
 */
@TableName(value ="plan_status_log")
@Data
public class PlanStatusLog {
    /**
     * 流水ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 外键，关联生产计划ID
     */
    @TableField(value = "plan_id")
    private Integer planId;

    /**
     * 变更前状态
     */
    @TableField(value = "from_status")
    private String fromStatus;

    /**
     * 变更后状态
     */
    @TableField(value = "to_status")
    private String toStatus;

    /**
     * 状态变更时间
     */
    @TableField(value = "change_time")
    private LocalDateTime changeTime;

    /**
     * 操作人员ID
     */
    @TableField(value = "operator")
    private Long operator;

    /**
     * 变更原因或备注
     */
    @TableField(value = "remark")
    private String remark;


// ========== 关联表显示字段（非数据库字段）==========

    /**
     * 计划编号（关联production_plan表）
     */
    @TableField(exist = false)
    private String planNumber;

    /**
     * 操作人姓名（关联user表）
     */
    @TableField(exist = false)
    private String operatorName;

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
package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工单工序实际参与人
 *
 * @TableName work_order_process_execution_participant
 */
@TableName(value = "work_order_process_execution_participant")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderProcessExecutionParticipant extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 工序执行记录ID
     */
    @TableField(value = "execution_id")
    private Long executionId;

    /**
     * 人员档案ID
     */
    @TableField(value = "personnel_id")
    private Long personnelId;

    /**
     * 个人工作时间（小时）
     */
    @TableField(value = "work_hours")
    private BigDecimal workHours;

    /**
     * 个人完成数量
     */
    @TableField(value = "completed_qty")
    private BigDecimal completedQty;

    /**
     * 是否主操作：0否/1是
     */
    @TableField(value = "is_primary")
    private Integer isPrimary;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 是否已删除：0否/1是
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion

    // region 关联显示字段（不映射数据库）
    // ===================================
    // 关联显示字段
    // ===================================

    /**
     * 人员姓名
     */
    @TableField(exist = false)
    private String personnelName;

    /**
     * 工号
     */
    @TableField(exist = false)
    private String employeeNo;

    /**
     * 班组名称
     */
    @TableField(exist = false)
    private String teamName;

    // endregion
}

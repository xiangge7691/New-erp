package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 工单工序符合性检查实体
 * <p>
 * 审核放行时记录每道工序是否符合要求、工艺查证人和日期。
 * 要求快照在检查时点保存，避免工序页配置变更后历史记录被改写。
 * </p>
 *
 * @TableName work_order_process_check
 */
@TableName(value = "work_order_process_check")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderProcessCheck extends AuditEntity {

    // region 基本信息
    // ===================================
    // 基本信息
    // ===================================

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /** 工单工序执行ID */
    @TableField(value = "execution_id")
    private Long executionId;

    /** 工序类型ID */
    @TableField(value = "process_type_id")
    private Long processTypeId;

    /** 工序名称 */
    @TableField(value = "process_name")
    private String processName;

    /** 检查时点的要求快照 */
    @TableField(value = "requirement_snapshot")
    private String requirementSnapshot;

    /** 结果：符合/不符合/待查 */
    @TableField(value = "check_result")
    private String checkResult;

    /** 工艺查证人ID */
    @TableField(value = "inspector_id")
    private Long inspectorId;

    /** 工艺查证人姓名 */
    @TableField(value = "inspector_name")
    private String inspectorName;

    /** 查证日期 */
    @TableField(value = "check_date")
    private LocalDate checkDate;

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

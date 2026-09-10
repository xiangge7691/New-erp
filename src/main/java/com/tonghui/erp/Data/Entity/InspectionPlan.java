package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验计划实体类
 * <p>
 * 记录月/周检验排程信息，包括检验类型、检验对象、计划检验时间及状态等，
 * 用于质量检验模块的检验计划手动排程管理
 * </p>
 *
 * @TableName inspection_plan
 */
@TableName(value = "inspection_plan")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionPlan extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 检验计划唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 计划编号（唯一，格式JH-YYYYMMDD-NNN，系统自动生成，可手动修改）
     */
    @TableField(value = "plan_code")
    private String planCode;

    /**
     * 计划月份/周次，如2026-07-第3周
     */
    @TableField(value = "plan_period")
    private String planPeriod;

    /**
     * 检验类型：原料检验/过程检验/成品检验/环境监测
     */
    @TableField(value = "inspection_type")
    private String inspectionType;

    /**
     * 检验对象名称（物料名称或制剂名称）
     */
    @TableField(value = "object_name")
    private String objectName;

    /**
     * 批号（检验对象的批号）
     */
    @TableField(value = "batch_no")
    private String batchNo;

    /**
     * 规格（检验对象的规格）
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 检验项目概要（计划检验的项目大类）
     */
    @TableField(value = "inspection_summary")
    private String inspectionSummary;

    /**
     * 计划检验时间
     */
    @TableField(value = "plan_time")
    private LocalDate planTime;

    /**
     * 完成时间（实际完成时间）
     */
    @TableField(value = "complete_time")
    private LocalDate completeTime;

    /**
     * 状态：待检验/检验中/已完成
     */
    @TableField(value = "status")
    private String status;

    /**
     * 备注说明
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 审计与软删除字段
    // ===================================
    // 审计与软删除字段
    // ===================================

    /**
     * 是否已删除：0否/1是
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion
}
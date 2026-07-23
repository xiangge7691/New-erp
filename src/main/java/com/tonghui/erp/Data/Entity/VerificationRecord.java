package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 验证记录表
 * <p>
 * 管理制剂室各类确认和验证活动，包括设备确认、厂房验证、工艺验证、清洁验证、设施验证、检验方法验证
 * 所有类别共享相同字段模板，通过 category 字段区分类型
 * </p>
 * @TableName verification_record
 */
@TableName(value = "verification_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class VerificationRecord extends AuditEntity {

    // region 主键字段
    // ===================================
    // 主键字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // endregion

    // region 验证基本信息字段
    // ===================================
    // 验证基本信息字段
    // ===================================

    /**
     * 验证类别（equipment/building/process/cleaning/facility/method）
     */
    @TableField(value = "category")
    private String category;

    /**
     * 验证编号
     */
    @TableField(value = "verification_no")
    private String verificationNo;

    /**
     * 验证名称
     */
    @TableField(value = "verification_name")
    private String verificationName;

    /**
     * 关联对象
     */
    @TableField(value = "related_object")
    private String relatedObject;

    // endregion

    // region 日期字段
    // ===================================
    // 日期字段
    // ===================================

    /**
     * 执行日期
     */
    @TableField(value = "execute_date")
    private Date executeDate;

    /**
     * 执行日期开始（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private Date executeDateStart;

    /**
     * 执行日期结束（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private Date executeDateEnd;

    /**
     * 下次验证日期
     */
    @TableField(value = "next_verify_date")
    private Date nextVerifyDate;

    // endregion

    // region 人员与备注字段
    // ===================================
    // 人员与备注字段
    // ===================================

    /**
     * 执行人
     */
    @TableField(value = "executor")
    private String executor;

    /**
     * 审核人
     */
    @TableField(value = "auditor")
    private String auditor;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 预警状态字段
    // ===================================
    // 预警状态字段
    // ===================================

    /**
     * 预警状态（0未处理/1已处理/2不再提醒）
     */
    @TableField(value = "reminder_status")
    private Integer reminderStatus;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

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

    // endregion
}

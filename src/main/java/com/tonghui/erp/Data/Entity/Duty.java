package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 职务信息表
 * <p>
 * 用于系统组织架构中的职务管理，记录职务编码、职务名称、职务描述等基本信息，
 * 文件上传对接统一文件模块（businessType=ORGANIZATION_POSITION，目录：机构管理/岗位）
 * </p>
 *
 * @TableName duty
 */
@TableName(value = "duty")
@Data
@EqualsAndHashCode(callSuper = true)
public class Duty extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 职务唯一标识
     */
    @TableId(value = "duty_id", type = IdType.AUTO)
    private Long dutyId;

    /**
     * 职务编码（唯一约束）
     */
    @TableField(value = "duty_code")
    private String dutyCode;

    /**
     * 职务名称
     */
    @TableField(value = "duty_name")
    private String dutyName;

    /**
     * 职务描述
     */
    @TableField(value = "duty_desc")
    private String dutyDesc;

    // endregion

    // region 状态与排序字段
    // ===================================
    // 状态与排序字段
    // ===================================

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

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
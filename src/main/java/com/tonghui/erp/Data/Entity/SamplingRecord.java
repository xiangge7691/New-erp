package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 取样记录实体类
 * <p>
 * 记录质量检验中取样的相关信息，包括被检对象、批号、取样地点、取样量、
 * 取样方法与取样人等信息，用于取样过程追溯管理
 * </p>
 *
 * @TableName sampling_record
 */
@TableName(value = "sampling_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class SamplingRecord extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 取样记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 取样编号（唯一，格式QY-YYYYMMDD-NNN，系统自动生成，可手动修改）
     */
    @TableField(value = "sampling_code")
    private String samplingCode;

    /**
     * 关联检验计划编号（可空，可选引用上一环节编号）
     */
    @TableField(value = "related_plan_code")
    private String relatedPlanCode;

    /**
     * 被检对象名称（物料/制剂名称）
     */
    @TableField(value = "object_name")
    private String objectName;

    /**
     * 批号
     */
    @TableField(value = "batch_no")
    private String batchNo;

    /**
     * 规格
     */
    @TableField(value = "spec")
    private String spec;

    // endregion

    // region 取样信息字段
    // ===================================
    // 取样信息字段
    // ===================================

    /**
     * 取样地点：仓库/车间/洁净区等
     */
    @TableField(value = "sampling_location")
    private String samplingLocation;

    /**
     * 取样量，如100g×3份
     */
    @TableField(value = "sampling_quantity")
    private String samplingQuantity;

    /**
     * 取样件数
     */
    @TableField(value = "sampling_count")
    private Integer samplingCount;

    /**
     * 取样方法：随机/分层/定点
     */
    @TableField(value = "sampling_method")
    private String samplingMethod;

    /**
     * 取样人
     */
    @TableField(value = "sampler")
    private String sampler;

    /**
     * 取样时间
     */
    @TableField(value = "sampling_time")
    private LocalDateTime samplingTime;

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
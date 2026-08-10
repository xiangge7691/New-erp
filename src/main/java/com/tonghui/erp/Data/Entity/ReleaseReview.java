package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审核放行实体类
 * <p>
 * 记录质量检验的最终放行决策信息，包括放行结论、审核意见、审核人与审核时间等，
 * 用于放行决策的过程记录与质量追溯
 * </p>
 *
 * @TableName release_review
 */
@TableName(value = "release_review")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReleaseReview extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 审核放行唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 放行编号（唯一，格式FX-YYYYMMDD-NNN，系统自动生成，可手动修改）
     */
    @TableField(value = "release_code")
    private String releaseCode;

    /**
     * 关联检验编号（可空，可选引用上一环节编号）
     */
    @TableField(value = "related_inspection_code")
    private String relatedInspectionCode;

    /**
     * 被检对象名称
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

    // region 放行决策字段
    // ===================================
    // 放行决策字段
    // ===================================

    /**
     * 放行结论：放行/拒绝放行
     */
    @TableField(value = "release_conclusion")
    private String releaseConclusion;

    /**
     * 审核意见（拒绝放行时必填原因）
     */
    @TableField(value = "review_opinion")
    private String reviewOpinion;

    /**
     * 审核人
     */
    @TableField(value = "reviewer")
    private String reviewer;

    /**
     * 审核时间
     */
    @TableField(value = "review_time")
    private LocalDateTime reviewTime;

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
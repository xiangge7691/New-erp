package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验记录实体类
 * <p>
 * 记录质量检验的核心检验数据，包括检验依据、检验项目、检验人与复核人、
 * 检验时间范围及总体结论等，作为报告书附件承载的载体记录
 * </p>
 *
 * @TableName inspection_record
 */
@TableName(value = "inspection_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionRecord extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 检验记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 检验编号（唯一，格式JY-YYYYMMDD-NNN，系统自动生成，可手动修改）
     */
    @TableField(value = "inspection_code")
    private String inspectionCode;

    /**
     * 关联取样编号（可空，可选引用上一环节编号）
     */
    @TableField(value = "related_sampling_code")
    private String relatedSamplingCode;

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

    // region 检验信息字段
    // ===================================
    // 检验信息字段
    // ===================================

    /**
     * 检验依据，如中国药典2020版
     */
    @TableField(value = "inspection_basis")
    private String inspectionBasis;

    /**
     * 检验项目内容描述
     */
    @TableField(value = "inspection_item")
    private String inspectionItem;

    /**
     * 检验人
     */
    @TableField(value = "inspector")
    private String inspector;

    /**
     * 复核人
     */
    @TableField(value = "reviewer")
    private String reviewer;

    /**
     * 检验开始时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /**
     * 检验结束时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    /**
     * 总体结论：合格/不合格
     */
    @TableField(value = "conclusion")
    private String conclusion;

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
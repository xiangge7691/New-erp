package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 培训记录表
 * @TableName training_record
 */
@TableName(value = "training_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingRecord extends AuditEntity {

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

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 培训编号（TRAIN-年-序号，如TRAIN-2026-001）
     */
    @TableField(value = "training_no")
    private String trainingNo;

    /**
     * 培训名称
     */
    @TableField(value = "training_name")
    private String trainingName;

    /**
     * 培训类别（岗前培训/在岗培训/专项培训/继续教育）
     */
    @TableField(value = "training_category")
    private String trainingCategory;

    /**
     * 培训形式（内部授课/外部培训/线上学习/实操演练）
     */
    @TableField(value = "training_form")
    private String trainingForm;

    // endregion

    // region 培训详情字段
    // ===================================
    // 培训详情字段
    // ===================================

    /**
     * 培训内容
     */
    @TableField(value = "training_content")
    private String trainingContent;

    /**
     * 培训日期
     */
    @TableField(value = "training_date")
    private Date trainingDate;

    /**
     * 培训时长（小时）
     */
    @TableField(value = "training_duration")
    private Integer trainingDuration;

    /**
     * 培训地点
     */
    @TableField(value = "training_location")
    private String trainingLocation;

    /**
     * 培训讲师
     */
    @TableField(value = "trainer")
    private String trainer;

    /**
     * 培训单位
     */
    @TableField(value = "training_unit")
    private String trainingUnit;

    // endregion

    // region 周期管理字段
    // ===================================
    // 周期管理字段
    // ===================================

    /**
     * 培训周期（月）
     */
    @TableField(value = "training_cycle")
    private Integer trainingCycle;

    /**
     * 下次培训日期
     */
    @TableField(value = "next_training_date")
    private Date nextTrainingDate;

    // endregion

    // region 备注字段
    // ===================================
    // 备注字段
    // ===================================

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

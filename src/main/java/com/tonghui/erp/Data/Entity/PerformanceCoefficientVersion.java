package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 绩效系数版本
 *
 * @TableName performance_coefficient_version
 */
@TableName(value = "performance_coefficient_version")
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformanceCoefficientVersion extends AuditEntity {

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
     * 版本号，如 V1
     */
    @TableField(value = "version_no")
    private String versionNo;

    /**
     * 生效时间
     */
    @TableField(value = "effective_time")
    private LocalDateTime effectiveTime;

    /**
     * 发布操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 调整说明
     */
    @TableField(value = "note")
    private String note;

    /**
     * 状态：生效/归档
     */
    @TableField(value = "status")
    private String status;

    /**
     * 操作人姓名（非表字段，列表展示用）
     */
    @TableField(exist = false)
    private String operatorName;

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
}

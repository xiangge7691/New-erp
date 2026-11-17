package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 版本内人员系数
 *
 * @TableName performance_person_coefficient
 */
@TableName(value = "performance_person_coefficient")
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformancePersonCoefficient extends AuditEntity {

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
     * 系数版本ID
     */
    @TableField(value = "version_id")
    private Long versionId;

    /**
     * 人员档案ID
     */
    @TableField(value = "personnel_id")
    private Long personnelId;

    /**
     * 人员系数（0.5-2.0）
     */
    @TableField(value = "coefficient")
    private BigDecimal coefficient;

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

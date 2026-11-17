package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 生产班次
 *
 * @TableName production_shift
 */
@TableName(value = "production_shift")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionShift extends AuditEntity {

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
     * 班次名称
     */
    @TableField(value = "shift_name")
    private String shiftName;

    /**
     * 开始时间
     */
    @TableField(value = "start_time")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    private LocalTime endTime;

    /**
     * 是否跨日：0否/1是
     */
    @TableField(value = "cross_day")
    private Integer crossDay;

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "status")
    private Integer status;

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

    // region 关联显示字段（不映射数据库）
    // ===================================
    // 关联显示字段
    // ===================================

    /**
     * 适用班组名称（逗号分隔）
     */
    @TableField(exist = false)
    private String teamNames;

    // endregion

    // region 请求辅助字段（不映射数据库）
    // ===================================
    // 请求辅助字段
    // ===================================

    /**
     * 适用班组ID列表
     */
    @TableField(exist = false)
    private java.util.List<Long> teamIds;

    // endregion
}

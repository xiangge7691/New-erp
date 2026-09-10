package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 能耗记录表
 * <p>
 * 按月记录院内制剂室水、电、气等能耗费用，支持按月份/类型筛选与汇总
 * </p>
 * @TableName energy_record
 */
@TableName(value = "energy_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class EnergyRecord extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 能耗记录唯一标识
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 月份，格式 YYYY-MM
     */
    @TableField(value = "month")
    private String month;

    /**
     * 能耗类型：自来水 / 电 / 燃气
     */
    @TableField(value = "energy_type")
    private String energyType;

    /**
     * 计量单位：自来水/燃气→立方米，电→度
     */
    @TableField(value = "unit")
    private String unit;

    /**
     * 上月表底读数
     */
    @TableField(value = "last_meter_reading")
    private BigDecimal lastMeterReading;

    /**
     * 本月表底读数
     */
    @TableField(value = "current_meter_reading")
    private BigDecimal currentMeterReading;

    /**
     * 本月实用量（本月表底 - 上月表底，可手动覆盖）
     */
    @TableField(value = "actual_usage")
    private BigDecimal actualUsage;

    /**
     * 单价（元）
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 总价（元），默认自动计算：实用量 × 单价
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 操作人字段
    // ===================================
    // 操作人字段
    // ===================================

    /**
     * 操作人ID（关联用户表）
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名（冗余，便于列表直接展示）
     */
    @TableField(value = "operator_name")
    private String operatorName;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 是否已删除（0-正常 1-已删除，配合全局逻辑删除）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion

    // region 非表字段
    // ===================================
    // 非表字段
    // ===================================

    /**
     * 是否有凭证附件（非表字段，列表查询时填充）
     */
    @TableField(exist = false)
    private Boolean hasAttachment;

    // endregion
}

package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备维保记录表
 * @TableName equipment_maintenance
 */
@TableName(value = "equipment_maintenance")
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentMaintenance extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 维保记录唯一标识
     */
    @TableId(value = "maintenance_id", type = IdType.AUTO)
    private Long maintenanceId;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 关联设备ID
     */
    @TableField(value = "equipment_id")
    private Long equipmentId;

    /**
     * 维保类型：日常保养/定期检修/故障维修
     */
    @TableField(value = "maintenance_type")
    private String maintenanceType;

    /**
     * 维保日期
     */
    @TableField(value = "maintenance_date")
    private LocalDate maintenanceDate;

    /**
     * 下次维保日期
     */
    @TableField(value = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    /**
     * 维保内容
     */
    @TableField(value = "maintenance_content")
    private String maintenanceContent;

    /**
     * 维保结果
     */
    @TableField(value = "maintenance_result")
    private String maintenanceResult;

    /**
     * 维保费用
     */
    @TableField(value = "cost")
    private BigDecimal cost;

    /**
     * 维保人员
     */
    @TableField(value = "maintainer")
    private String maintainer;

    /**
     * 维保公司
     */
    @TableField(value = "maintenance_company")
    private String maintenanceCompany;

    /**
     * 联系方式
     */
    @TableField(value = "contact_phone")
    private String contactPhone;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 记录人ID（关联personnel_file表）
     */
    @TableField(value = "recorder_id")
    private Long recorderId;

    // endregion

    // region 关联表显示字段
    // ===================================
    // 关联表显示字段
    // ===================================

    /**
     * 关联设备信息（关联equipment表）
     */
    @TableField(exist = false)
    private Equipment equipment;

    /**
     * 记录人姓名（关联personnel_file表，由recorderId查出）
     */
    @TableField(exist = false)
    private String recorderName;

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

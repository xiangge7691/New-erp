package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 设备维保记录表
 * @TableName equipment_maintenance
 */
@TableName(value = "equipment_maintenance")
@Data
public class EquipmentMaintenance {
    /**
     * 维保记录唯一标识
     */
    @TableId(value = "maintenance_id", type = IdType.AUTO)
    private Long maintenanceId;

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
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

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
}

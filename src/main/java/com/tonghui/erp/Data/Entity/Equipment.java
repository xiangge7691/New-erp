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
 * 设备表，记录设备详细信息。
 * @TableName equipment
 */
@TableName(value ="equipment")
@Data
@EqualsAndHashCode(callSuper = true)
public class Equipment extends AuditEntity {
    /**
     * 设备 ID，主键。唯一标识，自增长。
     */
    @TableId(value = "equipment_id", type = IdType.AUTO)
    private Integer equipmentId;

    /**
     * 设备名称。设备的具体名称，如"数控车床 -1"、"激光切割机 -1"。
     */
    @TableField(value = "equipment_name")
    private String equipmentName;

    /**
     * 设备型号。制造商提供的设备型号。
     */
    @TableField(value = "equipment_model")
    private String equipmentModel;

    /**
     * 所在房间 ID。关联 room_info 表的 room_id 字段，标识设备所在房间。
     */
    @TableField(value = "room_id")
    private Integer roomId;

    /**
     * 生产能力。描述设备的生产能力参数，如"100 件/小时"、"500 吨"。
     */
    @TableField(value = "production_capacity")
    private String productionCapacity;

    /**
     * 设备状态字典：1-启用/可用，0-停用/不可用。默认启用。
     */
    @TableField(value = "equipment_status")
    private Integer equipmentStatus;

    /**
     * 固定资产编号。公司资产管理系统中的唯一编号。
     */
    @TableField(value = "fixed_asset_code")
    private String fixedAssetCode;

    /**
     * 生产厂家。设备的生产制造商。
     */
    @TableField(value = "manufacturer")
    private String manufacturer;

    /**
     * 购置时间。设备购买的日期。
     */
    @TableField(value = "purchase_date")
    private LocalDate purchaseDate;

    /**
     * 购置金额。设备购买的价格，保留 2 位小数。
     */
    @TableField(value = "purchase_amount")
    private BigDecimal purchaseAmount;

    /**
     * 上次维保时间。最近一次维护保养的日期。
     */
    @TableField(value = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    /**
     * 设备类型：生产设备/检验设备/环境设备/其他设备
     */
    @TableField(value = "equipment_type")
    private String equipmentType;

    /**
     * 维保周期（月），默认6个月
     */
    @TableField(value = "maintenance_cycle")
    private Integer maintenanceCycle;

    /**
     * 到期提醒天数，默认15天
     */
    @TableField(value = "reminder_days")
    private Integer reminderDays;

    /**
     * 下次维保时间
     */
    @TableField(value = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    /**
     * 备注。设备的附加说明信息。
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 房间名称（非数据库字段，用于查询结果展示）。
     */
    @TableField(exist = false)
    private String roomName;

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

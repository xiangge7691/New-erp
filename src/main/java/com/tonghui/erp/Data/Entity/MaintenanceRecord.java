package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 养护记录主表实体类
 * <p>
 * 一张养护记录单对应一次库存养护检查（一张记录单包含多条库存检查明细），
 * 记录检查时间、检查类型、仓库、检查人员、环境温湿度等基本信息。
 * 系统只保存、关联、查询、展示检查过程与结果，不做质量结论自动判定，
 * 登记养护本身不产生库存增减
 * </p>
 *
 * @TableName maintenance_record
 */
@TableName(value = "maintenance_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceRecord extends AuditEntity {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 检查类型：日常检查
     */
    public static final String TYPE_DAILY = "日常检查";

    /**
     * 检查类型：定期检查
     */
    public static final String TYPE_PERIODIC = "定期检查";

    /**
     * 检查类型：特殊天气检查
     */
    public static final String TYPE_SPECIAL_WEATHER = "特殊天气检查";

    /**
     * 检查类型：复查
     */
    public static final String TYPE_REVIEW = "复查";

    // endregion

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 养护记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 养护记录编号（唯一，格式YH-YYYYMMDD-NNN，系统自动生成，不可修改）
     */
    @TableField(value = "maintenance_code")
    private String maintenanceCode;

    /**
     * 实际检查时间（人工填写，与系统登记时间分别保存，支持事后补录）
     */
    @TableField(value = "check_time")
    private LocalDateTime checkTime;

    /**
     * 检查类型：日常检查/定期检查/特殊天气检查/复查
     */
    @TableField(value = "check_type")
    private String checkType;

    // endregion

    // region 仓库与人员字段
    // ===================================
    // 仓库与人员字段
    // ===================================

    /**
     * 仓库ID（引用生产单位production_unit.prod_unit_id，一张记录单对应一个仓库）
     */
    @TableField(value = "warehouse_id")
    private Long warehouseId;

    /**
     * 仓库名称（冗余快照）
     */
    @TableField(value = "warehouse_name")
    private String warehouseName;

    /**
     * 检查人员ID（引用系统用户user.user_id，记录实际检查人员）
     */
    @TableField(value = "checker_id")
    private Long checkerId;

    /**
     * 检查人员姓名（冗余快照）
     */
    @TableField(value = "checker_name")
    private String checkerName;

    // endregion

    // region 环境数据字段
    // ===================================
    // 环境数据字段
    // ===================================

    /**
     * 环境观测时间及位置（说明温湿度数据在哪里、何时测得）
     */
    @TableField(value = "env_observe_info")
    private String envObserveInfo;

    /**
     * 温度（℃）
     */
    @TableField(value = "temperature")
    private BigDecimal temperature;

    /**
     * 相对湿度（%）
     */
    @TableField(value = "humidity")
    private BigDecimal humidity;

    /**
     * 总体说明（本次检查的补充说明）
     */
    @TableField(value = "summary")
    private String summary;

    // endregion

    // region 关联复查字段
    // ===================================
    // 关联复查字段
    // ===================================

    /**
     * 关联复查记录ID（检查类型为复查时关联原始记录，保留原始记录不覆盖）
     */
    @TableField(value = "related_maintenance_id")
    private Long relatedMaintenanceId;

    /**
     * 关联复查记录编号（冗余快照）
     */
    @TableField(value = "related_maintenance_code")
    private String relatedMaintenanceCode;

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

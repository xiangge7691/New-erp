package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存检查明细实体类
 * <p>
 * 养护记录单的每一条库存检查明细：引用库存明细（stock）的稳定标识
 * （物料编码+仓库+入库编号），快照物料信息、养护要求、批号、入库编号、
 * 检查时库存数量，并记录该批物料的检查项目结果、异常描述、养护措施、
 * 操作人员与处理结果。
 * 检查项目四态：1未检查/2未见异常/3发现异常/4不适用，默认未见异常；
 * 实测含水量为空表示未测，禁止填0；
 * 可空业务字段统一使用 updateStrategy = ALWAYS，使前端传 null 可清空历史值
 * </p>
 *
 * @TableName maintenance_record_detail
 */
@TableName(value = "maintenance_record_detail")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceRecordDetail extends AuditEntity {

    // region 检查项目四态常量
    // ===================================
    // 检查项目四态常量
    // ===================================

    /**
     * 检查项目状态：未检查
     */
    public static final int CHECK_UNCHECKED = 1;

    /**
     * 检查项目状态：未见异常
     */
    public static final int CHECK_NORMAL = 2;

    /**
     * 检查项目状态：发现异常
     */
    public static final int CHECK_ABNORMAL = 3;

    /**
     * 检查项目状态：不适用
     */
    public static final int CHECK_NA = 4;

    // endregion

    // region 关联与快照字段
    // ===================================
    // 关联与快照字段
    // ===================================

    /**
     * 库存检查明细唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属养护记录ID（maintenance_record.id）
     */
    @TableField(value = "maintenance_id")
    private Long maintenanceId;

    /**
     * 关联库存明细ID（stock.stock_id，同一物料同一批号按入库编号区分）
     */
    @TableField(value = "stock_id")
    private Long stockId;

    /**
     * 物料编码（快照）
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 物料名称（快照）
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 规格（快照）
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 物料分类（快照）
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 养护注意事项（引用物料信息快照，只读参考）
     */
    @TableField(value = "maintenance_notes")
    private String maintenanceNotes;

    /**
     * 养护周期-天（引用物料信息快照，用于计算下次养护日期）
     */
    @TableField(value = "maintenance_cycle_days")
    private Integer maintenanceCycleDays;

    /**
     * 是否重点养护物料：1是/0否（引用物料信息快照）
     */
    @TableField(value = "key_maintenance")
    private Integer keyMaintenance;

    /**
     * 批号（快照）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 入库单ID（快照）
     */
    @TableField(value = "stock_in_id")
    private Long stockInId;

    /**
     * 入库编号（stock_in.in_code快照，如IN202512010001）
     */
    @TableField(value = "in_code")
    private String inCode;

    /**
     * 仓库名称（快照）
     */
    @TableField(value = "warehouse_name")
    private String warehouseName;

    /**
     * 计量单位（快照）
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 检查时库存数量（快照，与实时库存区分）
     */
    @TableField(value = "stock_quantity")
    private BigDecimal stockQuantity;

    /**
     * 货架位置（手动填写，记录物料实际存放的货架/库位）
     */
    @TableField(value = "shelf_location", updateStrategy = FieldStrategy.ALWAYS)
    private String shelfLocation;

    /**
     * 检查范围或数量（人工选填，说明检查了哪些包装、货垛或数量）
     */
    @TableField(value = "check_scope", updateStrategy = FieldStrategy.ALWAYS)
    private String checkScope;

    // endregion

    // region 检查项目字段（四态）
    // ===================================
    // 检查项目字段（四态）
    // ===================================

    /**
     * 包装与标签：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "packaging_status")
    private Integer packagingStatus;

    /**
     * 受潮及干湿：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "moisture_status")
    private Integer moistureStatus;

    /**
     * 虫蛀情况：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "pest_status")
    private Integer pestStatus;

    /**
     * 霉变及腐败：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "mold_status")
    private Integer moldStatus;

    /**
     * 泛油情况：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "oil_status")
    private Integer oilStatus;

    /**
     * 色泽及外观：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "color_status")
    private Integer colorStatus;

    /**
     * 气味：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "odor_status")
    private Integer odorStatus;

    /**
     * 破碎及杂质：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "breakage_status")
    private Integer breakageStatus;

    /**
     * 堆放及储存条件：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "stacking_status")
    private Integer stackingStatus;

    /**
     * 其他异常：1未检查/2未见异常/3发现异常/4不适用
     */
    @TableField(value = "other_status")
    private Integer otherStatus;

    /**
     * 其他异常具体内容（勾选发现异常时手动填写）
     */
    @TableField(value = "other_description", updateStrategy = FieldStrategy.ALWAYS)
    private String otherDescription;

    // endregion

    // region 实测与异常字段
    // ===================================
    // 实测与异常字段
    // ===================================

    /**
     * 实测含水量（%），NULL表示未测（未测时不得填0）
     */
    @TableField(value = "moisture_content", updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal moistureContent;

    /**
     * 实测垛温（℃）
     */
    @TableField(value = "stack_temperature", updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal stackTemperature;

    /**
     * 异常描述（说明实际发现的情况）
     */
    @TableField(value = "abnormal_description", updateStrategy = FieldStrategy.ALWAYS)
    private String abnormalDescription;

    // endregion

    // region 养护措施与处理字段
    // ===================================
    // 养护措施与处理字段
    // ===================================

    /**
     * 养护措施（多选，逗号分隔：通风/除湿/翻垛/密封/包装整理等）
     */
    @TableField(value = "measures", updateStrategy = FieldStrategy.ALWAYS)
    private String measures;

    /**
     * 养护措施其他（补充说明）
     */
    @TableField(value = "measures_other", updateStrategy = FieldStrategy.ALWAYS)
    private String measuresOther;

    /**
     * 操作人员ID（引用系统用户，可与检查人员不同）
     */
    @TableField(value = "operator_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long operatorId;

    /**
     * 操作人员姓名（冗余快照）
     */
    @TableField(value = "operator_name", updateStrategy = FieldStrategy.ALWAYS)
    private String operatorName;

    /**
     * 操作时间（可与检查时间不同）
     */
    @TableField(value = "operate_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime operateTime;

    /**
     * 处理结果（尚未完成时允许后续补充）
     */
    @TableField(value = "handle_result", updateStrategy = FieldStrategy.ALWAYS)
    private String handleResult;

    /**
     * 备注
     */
    @TableField(value = "remark", updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    // endregion

    // region 周期提醒字段
    // ===================================
    // 周期提醒字段
    // ===================================

    /**
     * 最近养护日期（本次实际检查日期）
     */
    @TableField(value = "last_maintenance_date", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate lastMaintenanceDate;

    /**
     * 下次养护日期（=最近养护日期+养护周期快照；周期为空则为空，不纳入提醒）
     */
    @TableField(value = "next_maintenance_date", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate nextMaintenanceDate;

    // endregion

    // region 明细附件编号（不映射数据库）
    // ===================================
    // 明细附件编号（不映射数据库）
    // ===================================

    /**
     * 明细附件文件ID列表（保存时建立关联，查询时由附件接口单独获取）
     */
    @TableField(exist = false)
    private java.util.List<Long> attachmentFileIds;

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

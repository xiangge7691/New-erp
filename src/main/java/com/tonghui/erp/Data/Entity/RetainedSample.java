package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 留样记录实体类
 * <p>
 * 记录留样管理和定期观察信息，包括留样数量、留样日期、留样期限、存放位置
 * 及观察记录等，用于留样品的全生命周期管理
 * </p>
 *
 * @TableName retained_sample
 */
@TableName(value = "retained_sample")
@Data
@EqualsAndHashCode(callSuper = true)
public class RetainedSample extends AuditEntity {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 状态：留样中（可被留样销毁单引用）
     */
    public static final String STATUS_RETAINING = "留样中";

    /**
     * 状态：已销毁（由留样及稳定性考察产品销毁单反写维护）
     */
    public static final String STATUS_DESTROYED = "已销毁";

    // endregion

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 留样记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 留样编号（唯一，格式LY-YYYYMMDD-NNN，系统自动生成，可手动修改）
     */
    @TableField(value = "retained_code")
    private String retainedCode;

    /**
     * 关联检验编号（可空，可选引用上一环节编号）
     */
    @TableField(value = "related_inspection_code")
    private String relatedInspectionCode;

    /**
     * 物料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 被检品属性：生产原料/中间产品/成品
     */
    @TableField(value = "item_category")
    private String itemCategory;

    /**
     * 批号
     */
    @TableField(value = "batch_no")
    private String batchNo;

    /**
     * 规格
     */
    @TableField(value = "spec")
    private String spec;

    // endregion

    // region 留样信息字段
    // ===================================
    // 留样信息字段
    // ===================================

    /**
     * 留样数量，如100g×2份
     */
    @TableField(value = "retained_quantity")
    private String retainedQuantity;

    /**
     * 留样日期
     */
    @TableField(value = "retained_date")
    private LocalDate retainedDate;

    /**
     * 留样期限（留样截止日期）
     */
    @TableField(value = "expiry_date")
    private LocalDate expiryDate;

    /**
     * 存放位置（货架号/留样室）
     */
    @TableField(value = "storage_location")
    private String storageLocation;

    /**
     * 观察记录（外观、性状变化等）
     */
    @TableField(value = "observation_record")
    private String observationRecord;

    /**
     * 状态：留样中/已销毁
     * <p>
     * 由「留样及稳定性考察产品销毁」类型的销毁记录反写：登记销毁单时置为已销毁，
     * 销毁单移除引用或作废时回滚为留样中（仍被其他正常销毁单引用时保持已销毁）
     * </p>
     */
    @TableField(value = "status")
    private String status;

    /**
     * 销毁日期（状态=已销毁时填写，由销毁单反写：取销毁单销毁日期，未填则取登记当天）
     */
    @TableField(value = "destroy_date")
    private LocalDate destroyDate;

    /**
     * 备注说明
     */
    @TableField(value = "remark")
    private String remark;

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
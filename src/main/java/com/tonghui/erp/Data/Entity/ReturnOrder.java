package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 退库单主表
 * <p>
 * 基于出库记录进行退库的单据主表，支持生产余料退回原批次（库存回增），
 * 退库数量不超过出库数量
 * </p>
 * @TableName return_order
 */
@TableName(value = "return_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReturnOrder extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 退库单主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 退库单号（格式：TK-YYYYMMDD-NNN，唯一）
     */
    @TableField(value = "return_no")
    private String returnNo;

    /**
     * 关联出库单号
     */
    @TableField(value = "out_order_no")
    private String outOrderNo;

    /**
     * 生产计划编号
     */
    @TableField(value = "production_plan_no")
    private String productionPlanNo;

    /**
     * 物料种数
     */
    @TableField(value = "material_count")
    private Integer materialCount;

    /**
     * 退库总量
     */
    @TableField(value = "total_quantity")
    private BigDecimal totalQuantity;

    /**
     * 退库总价（元）
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 操作人与状态字段
    // ===================================
    // 操作人与状态字段
    // ===================================

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 是否已删除（软删除标记）
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
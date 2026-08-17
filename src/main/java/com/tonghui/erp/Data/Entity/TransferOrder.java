package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调拨单主表
 * <p>
 * 仓库间物料调拨（A库→B库）的单据主表，记录调拨双方仓库、物料种数、调拨总量与总价，
 * 库存总量不变，调拨同时生成出库与入库两条库存流水
 * </p>
 * @TableName transfer_order
 */
@TableName(value = "transfer_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferOrder extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 调拨单主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 调拨单号（格式：DB-YYYYMMDD-NNN，唯一）
     */
    @TableField(value = "transfer_no")
    private String transferNo;

    /**
     * 调出仓库名称
     */
    @TableField(value = "from_warehouse")
    private String fromWarehouse;

    /**
     * 调入仓库名称
     */
    @TableField(value = "to_warehouse")
    private String toWarehouse;

    /**
     * 物料种数（批次数）
     */
    @TableField(value = "material_count")
    private Integer materialCount;

    /**
     * 调拨总量
     */
    @TableField(value = "total_quantity")
    private BigDecimal totalQuantity;

    /**
     * 调拨总价（元）
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
package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 采购计划明细表
 * @TableName purchase_plan_detail
 */
@TableName(value = "purchase_plan_detail")
@Data
public class PurchasePlanDetail {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 采购计划ID
     */
    @TableField(value = "plan_id")
    private Long planId;

    /**
     * 序号
     */
    @TableField(value = "sequence_number")
    private Integer sequenceNumber;

    /**
     * 物料ID
     */
    @TableField(value = "material_id")
    private Long materialId;

    /**
     * 原料编码
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 原料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 原料分类
     */
    @TableField(value = "material_category")
    private String materialCategory;

    /**
     * 单位
     */
    @TableField(value = "unit")
    private String unit;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 标准处方量
     */
    @TableField(value = "standard_qty")
    private BigDecimal standardQty;

    /**
     * 采购数量
     */
    @TableField(value = "purchase_qty")
    private BigDecimal purchaseQty;

    /**
     * 库存数量
     */
    @TableField(value = "stock_qty")
    private BigDecimal stockQty;

    /**
     * 标准量差值
     */
    @TableField(value = "difference")
    private BigDecimal difference;

    // endregion

    // region 状态字段
    // ===================================
    // 状态字段
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

package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 生产领料单明细表实体
 * <p>
 * 记录每个物料的物料编码、物料名称、单位、请领数量、实发数量、实发批次与备注。
 * 发料时根据库存批次写入 stock_id / batch_number / unit_price，供联动出库使用。
 * </p>
 *
 * @TableName production_pick_detail
 */
@TableName(value = "production_pick_detail")
@Data
public class ProductionPickDetail {

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
     * 生产领料单ID
     */
    @TableField(value = "pick_id")
    private Long pickId;

    /**
     * 明细行序号（从1开始）
     */
    @TableField(value = "seq")
    private Integer seq;

    /**
     * 物料ID
     */
    @TableField(value = "material_id")
    private Long materialId;

    /**
     * 物料编码
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 物料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 物料分类（原料/辅料/包材）
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 计量单位
     */
    @TableField(value = "unit_name")
    private String unitName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 请领数量
     */
    @TableField(value = "apply_qty")
    private BigDecimal applyQty;

    /**
     * 实发数量
     */
    @TableField(value = "actual_qty")
    private BigDecimal actualQty;

    /**
     * 实发主库存批次ID（多批次时记录第一个批次）
     */
    @TableField(value = "stock_id")
    private Long stockId;

    /**
     * 实发批次（多批次时逗号分隔）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 发料单价（多批次时为加权平均价，展示/汇总用）
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

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

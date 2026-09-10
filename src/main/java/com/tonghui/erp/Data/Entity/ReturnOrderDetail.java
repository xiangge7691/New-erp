package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 退库单明细表
 * <p>
 * 记录每条出库明细的退库信息：出库数量、已退数量（退库前）、本次退库数量、单价与金额
 * </p>
 * @TableName return_order_detail
 */
@TableName(value = "return_order_detail")
@Data
public class ReturnOrderDetail {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 明细主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联退库单主表ID
     */
    @TableField(value = "return_order_id")
    private Long returnOrderId;

    /**
     * 出库明细ID（stock_out_detail.out_detail_id）
     */
    @TableField(value = "out_detail_id")
    private Long outDetailId;

    /**
     * 库存标识（格式：物料编码_仓库名_批号）
     */
    @TableField(value = "inventory_key")
    private String inventoryKey;

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
     * 分类（原料/辅料/包材/成品）
     */
    @TableField(value = "category")
    private String category;

    /**
     * 批号
     */
    @TableField(value = "batch_no")
    private String batchNo;

    /**
     * 仓库名称
     */
    @TableField(value = "warehouse")
    private String warehouse;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 出库数量
     */
    @TableField(value = "out_quantity")
    private BigDecimal outQuantity;

    /**
     * 已退数量（退库前）
     */
    @TableField(value = "returned_quantity")
    private BigDecimal returnedQuantity;

    /**
     * 本次退库数量
     */
    @TableField(value = "return_quantity")
    private BigDecimal returnQuantity;

    /**
     * 单价（元）
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额（元）
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 计量单位
     */
    @TableField(value = "unit")
    private String unit;

    // endregion

    // region 状态字段
    // ===================================
    // 状态字段
    // ===================================

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
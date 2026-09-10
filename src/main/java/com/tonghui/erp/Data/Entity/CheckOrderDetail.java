package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 盘点单明细表
 * <p>
 * 记录每个库存批次盘点的系统库存、实盘数量、差异与盘点结果（盘盈/盘亏/盘平）
 * </p>
 * @TableName check_order_detail
 */
@TableName(value = "check_order_detail")
@Data
public class CheckOrderDetail {

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
     * 关联盘点单主表ID
     */
    @TableField(value = "check_order_id")
    private Long checkOrderId;

    /**
     * 库存ID（stock.stock_id）
     */
    @TableField(value = "stock_id")
    private Long stockId;

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

    /**
     * 物料状态（合格/待检/不合格）
     */
    @TableField(value = "status")
    private String status;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 系统库存
     */
    @TableField(value = "system_stock")
    private BigDecimal systemStock;

    /**
     * 实盘数量
     */
    @TableField(value = "actual_stock")
    private BigDecimal actualStock;

    /**
     * 差异（实盘数量 - 系统库存）
     */
    @TableField(value = "difference")
    private BigDecimal difference;

    /**
     * 盘点结果（盘盈/盘亏/盘平）
     */
    @TableField(value = "result")
    private String result;

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
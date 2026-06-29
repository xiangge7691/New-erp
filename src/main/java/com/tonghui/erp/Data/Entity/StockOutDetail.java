package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 出库明细表
 * @TableName stock_out_detail
 */
@TableName(value ="stock_out_detail")
@Data
public class StockOutDetail {
    /**
     * 出库明细唯一标识
     */
    @TableId(value = "out_detail_id", type = IdType.AUTO)
    private Long outDetailId;

    /**
     * 关联出库单ID
     */
    @TableField(value = "out_id")
    private Long outId;

    /**
     * 关联库存记录ID
     */
    @TableField(value = "stock_id")
    private Long stockId;

    /**
     * 物品类型
     */
    @TableField(value = "item_type")
    private Object itemType;

    /**
     * 物品ID
     */
    @TableField(value = "item_id")
    private Long itemId;

    /**
     * 物品编码
     */
    @TableField(value = "item_code")
    private String itemCode;

    /**
     * 物品名称
     */
    @TableField(value = "item_name")
    private String itemName;

    /**
     * 分类
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 单位
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 批次号
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 出库数量
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
}

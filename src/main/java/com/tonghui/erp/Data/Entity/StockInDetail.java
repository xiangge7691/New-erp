package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 入库明细表
 * @TableName stock_in_detail
 */
@TableName(value ="stock_in_detail")
@Data
public class StockInDetail {
    /**
     * 入库明细唯一标识
     */
    @TableId(value = "in_detail_id", type = IdType.AUTO)
    private Long inDetailId;

    /**
     * 关联入库单ID
     */
    @TableField(value = "in_id")
    private Long inId;

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
     * 生产日期
     */
    @TableField(value = "production_date")
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    @TableField(value = "expiry_date")
    private LocalDate expiryDate;

    /**
     * 入库数量
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
     * 存放位置
     */
    @TableField(value = "storage_location")
    private String storageLocation;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
}

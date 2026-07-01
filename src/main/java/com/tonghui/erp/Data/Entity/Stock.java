package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存表（统一管理物料和制剂库存，按生产单位分配）
 * @TableName stock
 */
@TableName(value ="stock")
@Data
@EqualsAndHashCode(callSuper = true)
public class Stock extends AuditEntity {
    /**
     * 库存唯一标识符
     */
    @TableId(value = "stock_id", type = IdType.AUTO)
    private Long stockId;

    /**
     * 关联的生产单位ID（库存位置）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 物品类型：material物料 / preparation制剂
     */
    @TableField(value = "item_type")
    private Object itemType;

    /**
     * 关联的物品ID（根据item_type引用对应表ID）
     */
    @TableField(value = "item_id")
    private Long itemId;

    /**
     * 物品编码（冗余存储，便于查询）
     */
    @TableField(value = "item_code")
    private String itemCode;

    /**
     * 物品名称（冗余存储，便于查询）
     */
    @TableField(value = "item_name")
    private String itemName;

    /**
     * 分类名称（原料/辅料/包材/制剂等）
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 计量单位
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 库存数量
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 最低库存预警数量
     */
    @TableField(value = "min_quantity")
    private BigDecimal minQuantity;

    /**
     * 最高库存限制数量
     */
    @TableField(value = "max_quantity")
    private BigDecimal maxQuantity;

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
     * 库位/货架号
     */
    @TableField(value = "storage_location")
    private String storageLocation;

    /**
     * 库存状态
     */
    @TableField(value = "stock_status")
    private Object stockStatus;

    /**
     * 备注信息
     */
    @TableField(value = "remark")
    private String remark;

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
}

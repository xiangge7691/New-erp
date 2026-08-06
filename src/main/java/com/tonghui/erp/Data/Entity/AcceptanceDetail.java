package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 货物验收单明细表
 * @TableName acceptance_detail
 */
@TableName(value = "acceptance_detail")
@Data
public class AcceptanceDetail {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 验收明细唯一标识
     */
    @TableId(value = "detail_id", type = IdType.AUTO)
    private Long detailId;

    /**
     * 关联验收单ID
     */
    @TableField(value = "acceptance_id")
    private Long acceptanceId;

    /**
     * 明细序号
     */
    @TableField(value = "seq")
    private Integer seq;

    /**
     * 物品类型：material物料/preparation制剂
     */
    @TableField(value = "item_type")
    private String itemType;

    /**
     * 物品ID（引用物料或制剂表）
     */
    @TableField(value = "item_id")
    private Long itemId;

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
     * 物料分类：原料/辅料/包材/成品
     */
    @TableField(value = "material_category")
    private String materialCategory;

    /**
     * 计量单位：kg/g/L/袋/盒/瓶
     */
    @TableField(value = "unit_name")
    private String unitName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 标准处方量
     */
    @TableField(value = "standard_dosage")
    private BigDecimal standardDosage;

    /**
     * 采购数量（实际到货数量）
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 物料单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额（数量*单价）
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 物料批号（入库必填）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 有效期至
     */
    @TableField(value = "expiry_date")
    private LocalDate expiryDate;

    /**
     * 标准量差值（采购数量与标准处方量的差值）
     */
    @TableField(value = "diff_quantity")
    private BigDecimal diffQuantity;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
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

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private java.time.LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private java.time.LocalDateTime updatedTime;

    // endregion
}

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
     * 采购数量（订单/计划采购数量）
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 实际到货数量（供应商实际送达数量，金额以该数量为准计算）
     */
    @TableField(value = "actual_arrival_qty")
    private BigDecimal actualArrivalQty;

    /**
     * 入库数量（检验合格后实际入库数量）
     */
    @TableField(value = "inbound_qty")
    private BigDecimal inboundQty;

    /**
     * 物料单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额（实际到货数量*单价）
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

    // region 退货与明细状态字段
    // ===================================
    // 退货与明细状态字段
    // ===================================

    /**
     * 明细状态：待初验/待检验/待入库/待退货/已退货/已重发/已入库/已取消
     * <p>与采购订单明细（purchase_order_items.status）共享，任一页操作后双向同步</p>
     */
    @TableField(value = "status")
    private String status;

    /**
     * 退货原因：初验不合格/检验不合格
     * <p>由货物验收环节不合格时自动带出，物料退货管理页只读不可修改</p>
     */
    @TableField(value = "return_reason")
    private String returnReason;

    /**
     * 不合格原因说明
     * <p>来源于货物验收初验/检验不合格时填写的备注说明，物料退货管理页只读展示</p>
     */
    @TableField(value = "return_remark")
    private String returnRemark;

    /**
     * 供应商（从采购订单明细自动带出，退货时退回该供应商）
     */
    @TableField(value = "supplier")
    private String supplier;

    /**
     * 部分验收拆行时记录的原明细序号（用于追溯原始行）
     */
    @TableField(value = "original_seq")
    private Integer originalSeq;

    /**
     * 明细级入库仓库（生产单位ID，每条物料独立选择）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 关联采购订单明细ID（明细状态同步锚点）
     */
    @TableField(value = "purchase_item_id")
    private Long purchaseItemId;

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

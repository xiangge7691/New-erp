package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 入库主表
 * @TableName stock_in
 */
@TableName(value ="stock_in")
@Data
public class StockIn {
    /**
     * 入库单唯一标识
     */
    @TableId(value = "in_id", type = IdType.AUTO)
    private Long inId;

    /**
     * 入库单号（唯一）
     */
    @TableField(value = "in_code")
    private String inCode;

    /**
     * 入库类型：采购/生产/退货/调拨/调整
     */
    @TableField(value = "in_type")
    private String inType;

    /**
     * 入库仓库（生产单位ID）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 供应商ID（如果是采购入库）
     */
    @TableField(value = "supplier_id")
    private Long supplierId;

    /**
     * 关联单号（采购单号、生产批号等）
     */
    @TableField(value = "related_order")
    private String relatedOrder;

    /**
     * 入库日期
     */
    @TableField(value = "in_date")
    private LocalDate inDate;

    /**
     * 入库总金额
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：草稿/已确认/已完成/已取消
     */
    @TableField(value = "in_status")
    private String inStatus;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
}
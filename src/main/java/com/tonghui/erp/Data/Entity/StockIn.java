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
 * 入库主表
 * @TableName stock_in
 */
@TableName(value ="stock_in")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockIn extends AuditEntity {
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
     * 审批实例ID
     */
    @TableField(value = "approval_instance_id")
    private Long approvalInstanceId;

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

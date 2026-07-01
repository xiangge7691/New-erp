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
 * 出库主表
 * @TableName stock_out
 */
@TableName(value ="stock_out")
@Data
public class StockOut {
    /**
     * 出库单唯一标识
     */
    @TableId(value = "out_id", type = IdType.AUTO)
    private Long outId;

    /**
     * 出库单号（唯一）
     */
    @TableField(value = "out_code")
    private String outCode;

    /**
     * 出库类型：销售/生产领用/退货/调拨/调整
     */
    @TableField(value = "out_type")
    private String outType;

    /**
     * 出库仓库（生产单位ID）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 客户ID（如果是销售出库）
     */
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 关联单号（销售订单号、生产任务单等）
     */
    @TableField(value = "related_order")
    private String relatedOrder;

    /**
     * 出库日期
     */
    @TableField(value = "out_date")
    private LocalDate outDate;

    /**
     * 出库总金额
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：草稿/已确认/已完成/已取消
     */
    @TableField(value = "out_status")
    private String outStatus;

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
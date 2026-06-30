package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 生产计划主表
 * @TableName production_plan
 */
@TableName(value ="production_plan")
@Data
public class ProductionPlan {
    /**
     * 计划唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 计划编号
     */
    @TableField(value = "plan_number")
    private String planNumber;

    /**
     * 关联单号（销售订单号）
     */
    @TableField(value = "related_order")
    private String relatedOrder;

    /**
     * 制剂编码
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 制剂名称
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 计划数量（批量）
     */
    @TableField(value = "plan_quantity")
    private BigDecimal planQuantity;

    /**
     * 生产计划类型
     */
    @TableField(value = "plan_type")
    private Object planType;

    /**
     * 当前状态
     */
    @TableField(value = "current_status")
    private String currentStatus;

    /**
     * 当前状态时间
     */
    @TableField(value = "current_status_date")
    private LocalDateTime currentStatusDate;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 创建人员ID
     */
    @TableField(value = "create_user")
    private Long createUser;

    /**
     * 最后更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 最后更新人员ID
     */
    @TableField(value = "update_user")
    private Long updateUser;

    /**
     * 备注信息
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 是否已归档（0-未归档，1-已归档）
     */
    @TableField(value = "is_archived")
    private Integer isArchived;

    /**
     * 制剂所属单位
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 生产单位
     */
    @TableField(value = "production_unit")
    private String productionUnit;

    /**
     * 单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 成品数量
     */
    @TableField(value = "finished_quantity")
    private BigDecimal finishedQuantity;

    /**
     * 周期（天）
     */
    @TableField(value = "production_cycle")
    private Integer productionCycle;

    /**
     * 得率（百分比）
     */
    @TableField(value = "yield_rate")
    private BigDecimal yieldRate;

    /**
     * 总金额（成品数量*单价）
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 生产开始时间
     */
    @TableField(value = "production_start_time")
    private LocalDateTime productionStartTime;

    /**
     * 生产结束时间
     */
    @TableField(value = "production_end_time")
    private LocalDateTime productionEndTime;

    /**
     * 检验开始时间
     */
    @TableField(value = "inspection_start_time")
    private LocalDateTime inspectionStartTime;

    /**
     * 检验结束时间
     */
    @TableField(value = "inspection_end_time")
    private LocalDateTime inspectionEndTime;

    /**
     * 出库时间
     */
    @TableField(value = "outbound_time")
    private LocalDateTime outboundTime;

    /**
     * 归档时间
     */
    @TableField(value = "archive_time")
    private LocalDateTime archiveTime;
}
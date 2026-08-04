package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单表
 * @TableName work_order
 */
@TableName(value ="work_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrder extends AuditEntity {
    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 工单唯一标识
     */
    @TableId(value = "work_order_id", type = IdType.AUTO)
    private Long workOrderId;

    /**
     * 工单编号（唯一性约束）
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 工单名称
     */
    @TableField(value = "work_order_name")
    private String workOrderName;

    /**
     * 制剂ID
     */
    @TableField(value = "preparation_id")
    private Long preparationId;

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
     * 关联计划ID
     */
    @TableField(value = "plan_id")
    private Long planId;

    /**
     * 关联计划名称
     */
    @TableField(value = "plan_name")
    private String planName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 批量
     */
    @TableField(value = "batch_qty")
    private BigDecimal batchQty;

    /**
     * 生产单位
     */
    @TableField(value = "producer")
    private String producer;

    /**
     * 收货单位
     */
    @TableField(value = "receiver")
    private String receiver;

    /**
     * 交付时间
     */
    @TableField(value = "delivery_time")
    private LocalDateTime deliveryTime;

    /**
     * 开票单价
     */
    @TableField(value = "invoice_price")
    private BigDecimal invoicePrice;

    /**
     * 医保单价
     */
    @TableField(value = "insurance_price")
    private BigDecimal insurancePrice;

    /**
     * 结算单价
     */
    @TableField(value = "settlement_price")
    private BigDecimal settlementPrice;

    /**
     * 批号
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 出库量
     */
    @TableField(value = "outbound_qty")
    private BigDecimal outboundQty;

    /**
     * 收款金额
     */
    @TableField(value = "receipt_amount")
    private BigDecimal receiptAmount;

    /**
     * 实收款
     */
    @TableField(value = "actual_receipt_amount")
    private BigDecimal actualReceiptAmount;

    /**
     * 开票金额
     */
    @TableField(value = "invoice_amount")
    private BigDecimal invoiceAmount;

    /**
     * 结算金额
     */
    @TableField(value = "settlement_amount")
    private BigDecimal settlementAmount;

    /**
     * 返款金额
     */
    @TableField(value = "return_amount")
    private BigDecimal returnAmount;

    /**
     * 配置日期
     */
    @TableField(value = "config_date")
    private LocalDateTime configDate;

    /**
     * 配置完成日期
     */
    @TableField(value = "config_complete_time")
    private LocalDateTime configCompleteTime;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 加工类型（自主加工/委托加工/试生产）
     */
    @TableField(value = "production_type")
    private String productionType;

    /**
     * 销售单价
     */
    @TableField(value = "sales_price")
    private BigDecimal salesPrice;

    /**
     * 成品数量
     */
    @TableField(value = "finished_qty")
    private BigDecimal finishedQty;

    /**
     * 生产周期（天）
     */
    @TableField(value = "production_cycle")
    private Integer productionCycle;

    /**
     * 得率（百分比）
     */
    @TableField(value = "yield_rate")
    private BigDecimal yieldRate;

    /**
     * 总金额
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 当前状态（由 Service 根据日期字段自动计算并落库，无需手动赋值）
     * <p>
     * 状态流转规则：
     * <ul>
     *   <li>待生产 - 配置日期为空</li>
     *   <li>生产中 - 配置日期有值 且 配置完成日期为空</li>
     *   <li>已生产 - 配置完成日期有值</li>
     *   <li>已归档 - 归档时间有值</li>
     * </ul>
     * </p>
     */
    @TableField(value = "current_status")
    private String currentStatus;

    /**
     * 生产完成时间
     */
    @TableField(value = "production_complete_time")
    private LocalDateTime productionCompleteTime;

    /**
     * 检验开始时间
     */
    @TableField(value = "inspection_start")
    private LocalDateTime inspectionStart;

    /**
     * 检验结束时间
     */
    @TableField(value = "inspection_end")
    private LocalDateTime inspectionEnd;

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

    // endregion
}

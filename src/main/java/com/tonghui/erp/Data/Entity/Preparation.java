package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 制剂信息表
 * @TableName preparation
 */
@TableName(value ="preparation")
@Data
public class Preparation {
    /**
     * 制剂唯一标识
     */
    @TableId(value = "preparation_id", type = IdType.AUTO)
    private Long preparationId;

    /**
     * 制剂编码（唯一性约束）
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 制剂品名
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 规格描述
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 加工性质
     */
    @TableField(value = "process_attr")
    private String processAttr;

    /**
     * 包装规格
     */
    @TableField(value = "package_spec")
    private String packageSpec;

    /**
     * 制剂备案
     */
    @TableField(value = "record_info")
    private String recordInfo;

    /**
     * 功能主治
     */
    @TableField(value = "function_main")
    private String functionMain;

    /**
     * 制法
     */
    @TableField(value = "method")
    private String method;

    /**
     * 单位名称
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 剂型
     */
    @TableField(value = "dosage_form")
    private String dosageForm;

    /**
     * 生产单位
     */
    @TableField(value = "producer")
    private String producer;

    /**
     * 批量
     */
    @TableField(value = "batch_qty")
    private BigDecimal batchQty;

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
     * 状态：1启用/0禁用
     */
    @TableField(value = "status")
    private Integer status;

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
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
}
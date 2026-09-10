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
 * 制剂信息表
 * @TableName preparation
 */
@TableName(value ="preparation")
@Data
@EqualsAndHashCode(callSuper = true)
public class Preparation extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

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

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

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
     * 执行标准
     */
    @TableField(value = "executive_standard")
    private String executiveStandard;

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
     * 剂型大类（如片剂、注射剂、胶囊剂等）
     */
    @TableField(value = "dosage_category")
    private String dosageCategory;

    /**
     * 剂型名称（具体剂型名称）
     */
    @TableField(value = "dosage_name")
    private String dosageName;

    /**
     * 剂型ID（关联剂型表主键）
     */
    @TableField(value = "dosage_form_id")
    private Long dosageFormId;

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
     * 零售单价
     */
    @TableField(value = "retail_price")
    private BigDecimal retailPrice;

    /**
     * 销售单价
     */
    @TableField(value = "sales_price")
    private BigDecimal salesPrice;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 状态：1启用/0禁用
     */
    @TableField(value = "status")
    private Integer status;

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

    // region 查询辅助字段（不映射数据库）
    // ===================================
    // 查询辅助字段（不映射数据库）
    // ===================================

    /**
     * 关键字（用于模糊查询制剂编码和制剂名称，不映射数据库）
     */
    @TableField(exist = false)
    private String keyword;

    /**
     * 创建时间开始（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime createdTimeStart;

    /**
     * 创建时间结束（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime createdTimeEnd;

    /**
     * 更新时间开始（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime updatedTimeStart;

    /**
     * 更新时间结束（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime updatedTimeEnd;

    // endregion
}

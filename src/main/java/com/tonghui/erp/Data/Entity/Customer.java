package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户信息表
 * <p>
 * 维护制剂成品客户的基本信息（本院/外院），作为成品销售台账的关联对象来源
 * </p>
 *
 * @TableName customer
 */
@TableName(value = "customer")
@Data
@EqualsAndHashCode(callSuper = true)
public class Customer extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 客户唯一标识
     */
    @TableId(value = "customer_id", type = IdType.AUTO)
    private Long customerId;

    /**
     * 客户编号（4位数字递增，如0001）
     */
    @TableField(value = "customer_code")
    private String customerCode;

    /**
     * 客户名称
     */
    @TableField(value = "customer_name")
    private String customerName;

    /**
     * 类型：本院/外院（可为空）
     */
    @TableField(value = "type")
    private String type;

    // endregion

    // region 联系信息字段
    // ===================================
    // 联系信息字段
    // ===================================

    /**
     * 联系人
     */
    @TableField(value = "contact_person")
    private String contactPerson;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 收货/联系地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 开票信息（发票抬头/税号等）
     */
    @TableField(value = "invoice_info")
    private String invoiceInfo;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 状态：启用/停用
     */
    @TableField(value = "status")
    private String status;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

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

    // endregion
}

package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购供应商信息表
 * @TableName purchase_suppliers
 */
@TableName(value ="purchase_suppliers")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseSuppliers extends AuditEntity {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 供应商编号
     */
    @TableField(value = "supplier_number")
    private String supplierNumber;

    /**
     * 供应商名称
     */
    @TableField(value = "supplier_name")
    private String supplierName;

    /**
     * 类别
     */
    @TableField(value = "category")
    private String category;

    /**
     * 联系人
     */
    @TableField(value = "contact_person")
    private String contactPerson;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * 地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 银行账户
     */
    @TableField(value = "bank_account")
    private String bankAccount;

    /**
     * 开户行
     */
    @TableField(value = "bank_name")
    private String bankName;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 状态
     */
    @TableField(value = "status")
    private Object status;

    /**
     * 材料信息
     */
    @TableField(value = "material_info")
    private String materialInfo;

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
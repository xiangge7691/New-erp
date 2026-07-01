package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 药品剂型分类表
 * @TableName dosage_form
 */
@TableName(value ="dosage_form")
@Data
@EqualsAndHashCode(callSuper = true)
public class DosageForm extends AuditEntity {
    /**
     * 剂型唯一标识
     */
    @TableId(value = "dosage_id", type = IdType.AUTO)
    private Long dosageId;

    /**
     * 剂型名称（法定全称）
     */
    @TableField(value = "dosage_name")
    private String dosageName;

    /**
     * 剂型特性备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 状态：0禁用/1启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
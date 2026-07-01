package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 药品剂型分类表
 * @TableName dosage_form
 */
@TableName(value ="dosage_form")
@Data
public class DosageForm {
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
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

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
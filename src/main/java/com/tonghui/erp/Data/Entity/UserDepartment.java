package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-部门关联表
 * @TableName user_department
 */
@TableName(value ="user_department")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDepartment extends AuditEntity {
    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 部门ID
     */
    @TableField(value = "department_id")
    private Long departmentId;

    /**
     * 是否主部门
     */
    @TableField(value = "is_primary")
    private Integer isPrimary;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
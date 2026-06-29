package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 部门信息表
 * @TableName department
 */
@TableName(value ="department")
@Data
public class Department {
    /**
     * 部门唯一标识
     */
    @TableId(value = "department_id", type = IdType.AUTO)
    private Long departmentId;

    /**
     * 部门名称
     */
    @TableField(value = "department_name")
    private String departmentName;
}
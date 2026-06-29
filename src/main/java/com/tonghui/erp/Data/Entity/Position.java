package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 岗位信息表
 * @TableName position
 */
@TableName(value = "position")
@Data
public class Position {
    /**
     * 岗位唯一标识
     */
    @TableId(value = "position_id", type = IdType.AUTO)
    private Long positionId;

    /**
     * 岗位编码（唯一约束）
     */
    @TableField(value = "position_code")
    private String positionCode;

    /**
     * 岗位名称
     */
    @TableField(value = "position_name")
    private String positionName;

    /**
     * 所属部门ID
     */
    @TableField(value = "department_id")
    private Long departmentId;

    /**
     * 岗位描述
     */
    @TableField(value = "position_desc")
    private String positionDesc;

    /**
     * 岗位等级
     */
    @TableField(value = "position_level")
    private Integer positionLevel;

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

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


// ========== 关联表显示字段（非数据库字段）==========

    /**
     * 部门名称（关联department表）
     */
    @TableField(exist = false)
    private String departmentName;
}
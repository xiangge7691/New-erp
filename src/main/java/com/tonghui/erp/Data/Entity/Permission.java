package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统权限表
 * @TableName permission
 */
@TableName(value ="permission")
@Data
public class Permission {
    /**
     * 权限唯一标识
     */
    @TableId(value = "perm_id", type = IdType.AUTO)
    private Long permId;

    /**
     * 权限键（唯一标识符）
     */
    @TableField(value = "perm_key")
    private String permKey;

    /**
     * 权限名称
     */
    @TableField(value = "perm_name")
    private String permName;

    /**
     * 权限类型
     */
    @TableField(value = "perm_type")
    private Object permType;

    /**
     * 父权限ID
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 显示顺序
     */
    @TableField(value = "display_order")
    private Integer displayOrder;

    /**
     * 状态：0禁用/1启用
     */
    @TableField(value = "perm_status")
    private Integer permStatus;

    // 添加一个专门用于处理前端status字段的setter方法
    public void setStatus(Integer status) {
        this.permStatus = status;
    }

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
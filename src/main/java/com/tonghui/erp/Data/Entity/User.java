package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户信息表
 * @TableName user
 */
@TableName(value ="user")
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends AuditEntity {
    /**
     * 用户唯一标识
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名（唯一性约束）
     */
    @TableField(value = "user_account")
    private String userAccount;

    /**
     * 真实姓名
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 加密密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 性别
     */
    @TableField(value = "gender")
    private Object gender;

    /**
     * 账户状态：0禁用/1启用
     */
    @TableField(value = "user_status")
    private Integer userStatus;

    /**
     * 用户备注信息
     */
    @TableField(value = "user_notes")
    private String userNotes;

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

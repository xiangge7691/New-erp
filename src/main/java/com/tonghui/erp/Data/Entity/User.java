package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户信息表
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User {
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
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 人员档案表
 * @TableName personnel_file
 */
@TableName(value = "personnel_file")
@Data
public class PersonnelFile {
    /**
     * 人员档案唯一标识
     */
    @TableId(value = "personnel_file_id", type = IdType.AUTO)
    private Long personnelFileId;

    /**
     * 关联用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 岗位ID
     */
    @TableField(value = "position_id")
    private Long positionId;

    /**
     * 工号
     */
    @TableField(value = "employee_no")
    private String employeeNo;

    /**
     * 身份证号
     */
    @TableField(value = "id_card_no")
    private String idCardNo;

    /**
     * 健康证编号
     */
    @TableField(value = "health_cert_no")
    private String healthCertNo;

    /**
     * 健康证发证日期
     */
    @TableField(value = "health_cert_issue")
    private LocalDate healthCertIssue;

    /**
     * 健康证到期日期
     */
    @TableField(value = "health_cert_expire")
    private LocalDate healthCertExpire;

    /**
     * 学历
     */
    @TableField(value = "education")
    private String education;

    /**
     * 专业
     */
    @TableField(value = "major")
    private String major;

    /**
     * 入职日期
     */
    @TableField(value = "entry_date")
    private LocalDate entryDate;

    /**
     * 状态：0离职/1在职
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

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
     * 用户姓名（关联user表）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 岗位名称（关联position表）
     */
    @TableField(exist = false)
    private String positionName;
}

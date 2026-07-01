package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员档案表
 * @TableName personnel_file
 */
@TableName(value = "personnel_file")
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonnelFile extends AuditEntity {
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
     * 姓名
     */
    @TableField(value = "name")
    private String name;

    /**
     * 所属部门ID
     */
    @TableField(value = "department_id")
    private Long departmentId;

    /**
     * 人员资格认定
     */
    @TableField(value = "qualification")
    private String qualification;

    /**
     * 职称名
     */
    @TableField(value = "title_name")
    private String titleName;

    /**
     * 职称等级
     */
    @TableField(value = "title_level")
    private String titleLevel;

    /**
     * 身份证号
     */
    @TableField(value = "id_card_no")
    private String idCardNo;

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
     * 任职简历
     */
    @TableField(value = "work_experience")
    private String workExperience;

    /**
     * 教育-培训经历
     */
    @TableField(value = "education_training")
    private String educationTraining;

    /**
     * 注册证书名
     */
    @TableField(value = "certificate_name")
    private String certificateName;

    /**
     * 注册证书号
     */
    @TableField(value = "certificate_no")
    private String certificateNo;

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
     * 上次体检时间
     */
    @TableField(value = "last_checkup_date")
    private LocalDate lastCheckupDate;

    /**
     * 健康档案（关联file_info.file_id）
     */
    @TableField(value = "health_file")
    private Long healthFile;

    /**
     * 附件（关联file_info.file_id）
     */
    @TableField(value = "attachments")
    private Long attachments;

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

    /**
     * 部门名称（关联department表）
     */
    @TableField(exist = false)
    private String departmentName;
}

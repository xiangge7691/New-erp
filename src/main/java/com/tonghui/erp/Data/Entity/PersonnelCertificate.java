package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员证书子表
 * @TableName personnel_certificate
 */
@TableName(value = "personnel_certificate")
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonnelCertificate extends AuditEntity {
    /**
     * 证书唯一标识
     */
    @TableId(value = "certificate_id", type = IdType.AUTO)
    private Long certificateId;

    /**
     * 关联人员档案ID
     */
    @TableField(value = "personnel_file_id")
    private Long personnelFileId;

    /**
     * 证书类型（执业资格/职称证书/健康证/培训证书/其他）
     */
    @TableField(value = "certificate_type")
    private String certificateType;

    /**
     * 证书名称
     */
    @TableField(value = "certificate_name")
    private String certificateName;

    /**
     * 证书编号
     */
    @TableField(value = "certificate_no")
    private String certificateNo;

    /**
     * 发证机构
     */
    @TableField(value = "issuing_authority")
    private String issuingAuthority;

    /**
     * 发证日期
     */
    @TableField(value = "issue_date")
    private LocalDate issueDate;

    /**
     * 到期日期
     */
    @TableField(value = "expiry_date")
    private LocalDate expiryDate;

    /**
     * 证书等级/级别
     */
    @TableField(value = "certificate_level")
    private String certificateLevel;

    /**
     * 适用范围
     */
    @TableField(value = "applicable_scope")
    private String applicableScope;

    /**
     * 原件/复印件
     */
    @TableField(value = "original_status")
    private String originalStatus;

    /**
     * 复审日期
     */
    @TableField(value = "reexamine_date")
    private LocalDate reexamineDate;

    /**
     * 培训记录
     */
    @TableField(value = "training_record")
    private String trainingRecord;

    /**
     * 附件（关联file_info.file_id）
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 状态：0失效/1有效
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
}

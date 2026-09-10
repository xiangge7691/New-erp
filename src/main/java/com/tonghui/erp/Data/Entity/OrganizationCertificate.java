package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构证书子表
 * <p>
 * 管理机构级证书（如GMP证书、医疗机构执业许可证、消防验收合格证等），
 * 支持一人多证、到期预警。证书文件通过 businessId + businessType 从 file_info 表查询。
 * </p>
 * @TableName organization_certificate
 */
@TableName(value = "organization_certificate")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationCertificate extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 证书唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联机构ID
     */
    @TableField(value = "organization_id")
    private Long organizationId;

    /**
     * 证书名称（如：GMP证书、医疗机构执业许可证、消防验收合格证）
     */
    @TableField(value = "certificate_name")
    private String certificateName;

    /**
     * 证书编号
     */
    @TableField(value = "certificate_no")
    private String certificateNo;

    /**
     * 证书类型（机构资质/设备认证/消防许可/环保验收/其他）
     */
    @TableField(value = "certificate_type")
    private String certificateType;

    // endregion

    // region 发证信息
    // ===================================
    // 发证信息
    // ===================================

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
     * 有效期至
     */
    @TableField(value = "expiry_date")
    private LocalDate expiryDate;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

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

    // endregion
}

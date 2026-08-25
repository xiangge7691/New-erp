package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构信息表
 * <p>
 * 存储医疗机构制剂室的基础资质信息，包括许可证证面内容和许可/登记事项管理。
 * 单机构模式，系统中仅维护一条机构记录。
 * </p>
 * @TableName organization
 */
@TableName(value = "organization")
@Data
@EqualsAndHashCode(callSuper = true)
public class Organization extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 机构唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 许可证证号（格式：省份简称+年份+顺序号，如：湘20260001）
     */
    @TableField(value = "license_no")
    private String licenseNo;

    /**
     * 医疗机构名称（全称）
     */
    @TableField(value = "org_name")
    private String orgName;

    /**
     * 医疗机构类别（医院/卫生院/诊所/其他）
     */
    @TableField(value = "org_category")
    private String orgCategory;

    /**
     * 统一社会信用代码（三证合一，18位）
     */
    @TableField(value = "unified_social_credit_code")
    private String unifiedSocialCreditCode;

    /**
     * 医疗机构执业许可证号
     */
    @TableField(value = "practice_license_no")
    private String practiceLicenseNo;

    // endregion

    // region 负责人信息
    // ===================================
    // 负责人信息
    // ===================================

    /**
     * 法定代表人姓名
     */
    @TableField(value = "legal_representative")
    private String legalRepresentative;

    /**
     * 企业负责人（院长/分管院长）
     */
    @TableField(value = "enterprise_leader")
    private String enterpriseLeader;

    /**
     * 制剂室负责人姓名
     */
    @TableField(value = "prep_room_leader")
    private String prepRoomLeader;

    // endregion

    // region 配制信息
    // ===================================
    // 配制信息
    // ===================================

    /**
     * 制剂配制地址
     */
    @TableField(value = "preparation_address")
    private String preparationAddress;

    /**
     * 配制范围（按剂型+分类码，如：片剂(z)、颗粒剂(z)、丸剂(z)）
     */
    @TableField(value = "preparation_scope")
    private String preparationScope;

    // endregion

    // region 许可证基础信息
    // ===================================
    // 许可证基础信息
    // ===================================

    /**
     * 注册地址
     */
    @TableField(value = "registered_address")
    private String registeredAddress;

    /**
     * 联系人
     */
    @TableField(value = "contact_person")
    private String contactPerson;

    /**
     * 电话
     */
    @TableField(value = "contact_phone")
    private String contactPhone;

    /**
     * 建筑面积
     */
    @TableField(value = "building_area")
    private String buildingArea;

    /**
     * 洁净区面积
     */
    @TableField(value = "clean_area")
    private String cleanArea;

    /**
     * 制剂类别（多选，逗号分隔：化学药、中成药、其他）
     */
    @TableField(value = "preparation_category")
    private String preparationCategory;

    // endregion

    // region 许可证信息
    // ===================================
    // 许可证信息
    // ===================================

    /**
     * 发证机关名称
     */
    @TableField(value = "issuing_authority")
    private String issuingAuthority;

    /**
     * 发证日期
     */
    @TableField(value = "issue_date")
    private LocalDate issueDate;

    /**
     * 有效期至（自动计算：发证日期+5年）
     */
    @TableField(value = "expiry_date")
    private LocalDate expiryDate;

    /**
     * 许可证状态（有效/即将到期/已过期/注销/吊销）
     * <p>查询时根据 expiryDate 实时计算，此处仅存储手动设置的持久状态（注销/吊销）</p>
     */
    @TableField(value = "license_status")
    private String licenseStatus;

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
     * 状态：1启用/0停用
     */
    @TableField(value = "status")
    private Integer status;

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

    // region 关联表显示字段
    // ===================================
    // 关联表显示字段
    // ===================================

    /**
     * 证书列表（关联 organization_certificate 表）
     */
    @TableField(exist = false)
    private java.util.List<OrganizationCertificate> certificates;

    // endregion
}

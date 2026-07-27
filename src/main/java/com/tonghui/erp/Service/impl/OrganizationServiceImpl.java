package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Organization.OrganizationWithDetailsDto;
import com.tonghui.erp.Data.Entity.Organization;
import com.tonghui.erp.Data.Entity.OrganizationCertificate;
import com.tonghui.erp.Data.mapper.OrganizationMapper;
import com.tonghui.erp.Service.OrganizationCertificateService;
import com.tonghui.erp.Service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 机构信息服务实现类
 * <p>
 * 提供机构信息的CRUD操作，包括许可证有效期自动计算、状态实时计算等核心业务逻辑
 * </p>
 */
@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization>
    implements OrganizationService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 机构证书服务
     */
    @Autowired
    private OrganizationCertificateService organizationCertificateService;

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 获取当前机构信息（含证书列表+实时状态计算）
     */
    @Override
    public OrganizationWithDetailsDto getWithDetails() {
        Organization org = getCurrentOrganization();
        if (org == null) {
            return null;
        }

        // 实时计算许可证状态
        calculateLicenseStatus(org);

        // 查询关联证书
        List<OrganizationCertificate> certificates = organizationCertificateService
                .getByOrganizationId(org.getId());

        // 组装DTO
        OrganizationWithDetailsDto dto = new OrganizationWithDetailsDto();
        dto.setId(org.getId());
        dto.setLicenseNo(org.getLicenseNo());
        dto.setOrgName(org.getOrgName());
        dto.setOrgCategory(org.getOrgCategory());
        dto.setUnifiedSocialCreditCode(org.getUnifiedSocialCreditCode());
        dto.setPracticeLicenseNo(org.getPracticeLicenseNo());
        dto.setLegalRepresentative(org.getLegalRepresentative());
        dto.setEnterpriseLeader(org.getEnterpriseLeader());
        dto.setPrepRoomLeader(org.getPrepRoomLeader());
        dto.setPreparationAddress(org.getPreparationAddress());
        dto.setPreparationScope(org.getPreparationScope());
        dto.setIssuingAuthority(org.getIssuingAuthority());
        dto.setIssueDate(org.getIssueDate());
        dto.setExpiryDate(org.getExpiryDate());
        dto.setLicenseStatus(org.getLicenseStatus());
        dto.setRemark(org.getRemark());
        dto.setStatus(org.getStatus());
        dto.setCertificates(certificates);

        return dto;
    }

    /**
     * 查询即将到期和已过期的机构列表
     */
    @Override
    public List<Organization> findExpiringOrganizations(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);

        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
               .eq("status", 1)
               .isNotNull("expiry_date")
               .le("expiry_date", deadline);

        return list(wrapper);
    }

    // endregion

    // region 更新操作
    // ===================================
    // 更新操作
    // ===================================

    /**
     * 新增机构信息（单机构模式，仅允许创建一条）
     */
    @Override
    @Transactional
    public Organization createOrganization(Organization organization) {
        // 检查是否已存在机构
        Organization existing = getCurrentOrganization();
        if (existing != null) {
            throw new RuntimeException("已存在机构信息，请使用更新接口");
        }

        // 清理已软删除的机构记录（避免唯一键冲突）
        cleanSoftDeletedOrganizations();

        // 自动计算有效期至 = 发证日期 + 5年
        if (organization.getIssueDate() != null) {
            organization.setExpiryDate(organization.getIssueDate().plusYears(5));
        }

        // 设置默认状态
        organization.setLicenseStatus("有效");
        organization.setIsDeleted(0);
        organization.setVersion(0);

        save(organization);
        return organization;
    }

    /**
     * 更新机构信息（自动计算有效期至）
     */
    @Override
    @Transactional
    public Organization updateOrganization(Organization organization) {
        Organization existing = getCurrentOrganization();
        if (existing == null) {
            throw new RuntimeException("机构信息不存在");
        }

        // 自动计算有效期至 = 发证日期 + 5年
        if (organization.getIssueDate() != null) {
            organization.setExpiryDate(organization.getIssueDate().plusYears(5));
        }

        // 仅允许修改持久状态（注销/吊销），有效/即将到期/已过期由实时计算
        if (organization.getLicenseStatus() != null) {
            String status = organization.getLicenseStatus();
            if (!"注销".equals(status) && !"吊销".equals(status)) {
                organization.setLicenseStatus(null);
            }
        }

        organization.setId(existing.getId());
        updateById(organization);

        return getById(existing.getId());
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 清理已软删除的机构记录（释放唯一键约束）
     */
    private void cleanSoftDeletedOrganizations() {
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 1);
        remove(wrapper);
    }

    /**
     * 获取当前机构记录（单机构模式，始终返回唯一一条）
     */
    private Organization getCurrentOrganization() {
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 实时计算许可证状态
     * <p>
     * 根据有效期至自动计算：有效（>30天）、即将到期（<=30天）、已过期（<0天）
     * 注销/吊销状态保持不变
     * </p>
     *
     * @param org 机构信息
     */
    private void calculateLicenseStatus(Organization org) {
        if (org == null || org.getExpiryDate() == null) {
            return;
        }

        // 注销/吊销状态不自动计算
        String currentStatus = org.getLicenseStatus();
        if ("注销".equals(currentStatus) || "吊销".equals(currentStatus)) {
            return;
        }

        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(today, org.getExpiryDate());

        if (daysBetween < 0) {
            org.setLicenseStatus("已过期");
        } else if (daysBetween <= 30) {
            org.setLicenseStatus("即将到期");
        } else {
            org.setLicenseStatus("有效");
        }
    }

    // endregion
}

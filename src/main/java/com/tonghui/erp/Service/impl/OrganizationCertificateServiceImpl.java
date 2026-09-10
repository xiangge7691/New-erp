package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.OrganizationCertificate;
import com.tonghui.erp.Data.mapper.OrganizationCertificateMapper;
import com.tonghui.erp.Service.OrganizationCertificateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 机构证书服务实现类
 * <p>
 * 管理机构级证书的增删改查，包括证书列表查询、先删后插批量保存、到期预警查询
 * </p>
 */
@Service
public class OrganizationCertificateServiceImpl extends ServiceImpl<OrganizationCertificateMapper, OrganizationCertificate>
    implements OrganizationCertificateService {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据机构ID查询所有证书
     */
    @Override
    public List<OrganizationCertificate> getByOrganizationId(Long organizationId) {
        QueryWrapper<OrganizationCertificate> wrapper = new QueryWrapper<>();
        wrapper.eq("organization_id", organizationId)
               .eq("is_deleted", 0)
               .orderByDesc("created_time");
        return list(wrapper);
    }

    /**
     * 查询即将到期和已过期的证书
     */
    @Override
    public List<OrganizationCertificate> findExpiringCertificates(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);

        QueryWrapper<OrganizationCertificate> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
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
     * 保存机构证书列表（先删后插）
     */
    @Override
    @Transactional
    public void saveCertificates(Long organizationId, List<OrganizationCertificate> certificates) {
        // 先删除该机构下的所有证书
        QueryWrapper<OrganizationCertificate> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("organization_id", organizationId);
        remove(deleteWrapper);

        // 再批量插入新证书
        if (certificates != null && !certificates.isEmpty()) {
            for (OrganizationCertificate cert : certificates) {
                cert.setOrganizationId(organizationId);
                cert.setIsDeleted(0);
                cert.setVersion(0);
            }
            saveBatch(certificates);
        }
    }

    // endregion
}

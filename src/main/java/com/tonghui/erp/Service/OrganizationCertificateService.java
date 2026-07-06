package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.OrganizationCertificate;
import java.util.List;

/**
 * 机构证书服务接口
 * <p>
 * 管理机构级证书（如GMP证书、医疗机构执业许可证、消防验收合格证等）
 * </p>
 */
public interface OrganizationCertificateService extends IService<OrganizationCertificate> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据机构ID查询所有证书
     *
     * @param organizationId 机构ID
     * @return 证书列表（按创建时间倒序）
     */
    List<OrganizationCertificate> getByOrganizationId(Long organizationId);

    /**
     * 查询即将到期和已过期的证书
     *
     * @param days 提前天数（默认30天）
     * @return 到期预警证书列表
     */
    List<OrganizationCertificate> findExpiringCertificates(int days);

    // endregion

    // region 更新操作
    // ===================================
    // 更新操作
    // ===================================

    /**
     * 保存机构证书列表（先删后插）
     *
     * @param organizationId 机构ID
     * @param certificates 证书列表
     */
    void saveCertificates(Long organizationId, List<OrganizationCertificate> certificates);

    // endregion
}

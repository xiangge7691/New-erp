package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import java.util.List;

/**
 * 人员证书服务接口
 */
public interface PersonnelCertificateService extends IService<PersonnelCertificate> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据人员档案ID查询所有证书
     * @param personnelFileId 人员档案ID
     * @return 证书列表
     */
    List<PersonnelCertificate> getByPersonnelFileId(Long personnelFileId);

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 保存人员证书列表（先删后插）
     * @param personnelFileId 人员档案ID
     * @param certificates 证书列表
     */
    void saveCertificates(Long personnelFileId, List<PersonnelCertificate> certificates);

    /**
     * 查询即将到期和已过期的证书
     * @param days 提前天数
     * @return 证书列表
     */
    List<PersonnelCertificate> findExpiringCertificates(int days);

    // endregion
}

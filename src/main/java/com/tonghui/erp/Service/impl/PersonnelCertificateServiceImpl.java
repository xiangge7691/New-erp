package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import com.tonghui.erp.Data.mapper.PersonnelCertificateMapper;
import com.tonghui.erp.Service.PersonnelCertificateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 人员证书服务实现类
 * <p>
 * 实现PersonnelCertificateService接口，提供人员证书相关的业务逻辑处理，
 * 包括根据人员档案ID查询证书列表、批量保存证书等功能的具体实现
 * </p>
 *
 */
@Service
public class PersonnelCertificateServiceImpl extends ServiceImpl<PersonnelCertificateMapper, PersonnelCertificate> implements PersonnelCertificateService {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据人员档案ID查询所有证书
     * <p>按创建时间降序排列，最新的证书排在前面</p>
     *
     * @param personnelFileId 人员档案ID
     * @return 该人员档案关联的所有证书列表
     */
    @Override
    public List<PersonnelCertificate> getByPersonnelFileId(Long personnelFileId) {
        QueryWrapper<PersonnelCertificate> wrapper = new QueryWrapper<>();
        wrapper.eq("personnel_file_id", personnelFileId)
               .orderByDesc("created_time");
        return list(wrapper);
    }

    // endregion

    // region 批量操作
    // ===================================
    // 批量操作
    // ===================================

    /**
     * 保存人员证书列表（先删后插）
     * <p>使用事务保证数据一致性：先删除该人员档案原有的所有证书，再批量插入新证书</p>
     *
     * @param personnelFileId 人员档案ID
     * @param certificates   证书列表，可为null或空列表（将清空所有证书）
     */
    @Override
    @Transactional
    public void saveCertificates(Long personnelFileId, List<PersonnelCertificate> certificates) {
        // 删除原有证书
        QueryWrapper<PersonnelCertificate> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("personnel_file_id", personnelFileId);
        remove(deleteWrapper);

        // 插入新证书
        if (certificates != null && !certificates.isEmpty()) {
            for (PersonnelCertificate cert : certificates) {
                cert.setPersonnelFileId(personnelFileId);
                cert.setIsDeleted(0);
                cert.setVersion(0);
            }
            saveBatch(certificates);
        }
    }

    // endregion
}

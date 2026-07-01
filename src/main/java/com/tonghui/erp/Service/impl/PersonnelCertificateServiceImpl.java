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
 */
@Service
public class PersonnelCertificateServiceImpl extends ServiceImpl<PersonnelCertificateMapper, PersonnelCertificate> implements PersonnelCertificateService {

    /**
     * 根据人员档案ID查询所有证书
     */
    @Override
    public List<PersonnelCertificate> getByPersonnelFileId(Long personnelFileId) {
        QueryWrapper<PersonnelCertificate> wrapper = new QueryWrapper<>();
        wrapper.eq("personnel_file_id", personnelFileId)
               .orderByDesc("created_time");
        return list(wrapper);
    }

    /**
     * 保存人员证书列表（先删后插）
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
}

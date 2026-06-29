package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Service.PersonnelFileService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * 人员档案服务实现类
 */
@Service
public class PersonnelFileServiceImpl extends ServiceImpl<PersonnelFileMapper, PersonnelFile> implements PersonnelFileService {

    /**
     * 查询健康证即将到期的人员档案
     * 查询条件：在职状态 + 健康证到期日期在今天到指定天数之间
     */
    @Override
    public List<PersonnelFile> findExpiringHealthCerts(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)  // 在职状态
               .isNotNull("health_cert_expire")  // 健康证到期日期不为空
               .ge("health_cert_expire", today)  // 大于等于今天
               .le("health_cert_expire", deadline)  // 小于等于截止日期
               .orderByAsc("health_cert_expire");  // 按到期日期升序
        
        return list(wrapper);
    }

    /**
     * 根据用户ID查询人员档案
     */
    @Override
    public PersonnelFile findByUserId(Long userId) {
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return getOne(wrapper);
    }
}

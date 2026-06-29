package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import java.util.List;

/**
 * 人员档案服务接口
 */
public interface PersonnelFileService extends IService<PersonnelFile> {

    /**
     * 查询健康证即将到期的人员档案
     * @param days 提前天数
     * @return 即将到期的人员档案列表
     */
    List<PersonnelFile> findExpiringHealthCerts(int days);

    /**
     * 根据用户ID查询人员档案
     * @param userId 用户ID
     * @return 人员档案
     */
    PersonnelFile findByUserId(Long userId);
}

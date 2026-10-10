package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.PersonnelFileWithDetailsDto;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import java.util.List;

/**
 * 人员档案服务接口
 */
public interface PersonnelFileService extends IService<PersonnelFile> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

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

    /**
     * 查询人员档案（支持多条件分页）
     */
    Page<PersonnelFile> queryPersonnelFiles(PersonnelFile personnelFile, int pageNum, int pageSize);

    /**
     * 带子表查询人员档案
     */
    PagedResult<PersonnelFileWithDetailsDto> searchWithDetails(PersonnelFile personnelFile, int pageNum, int pageSize);

    // endregion

    // region 唯一性校验与软删除清理
    // ===================================
    // 唯一性校验与软删除清理
    // ===================================

    /**
     * 根据员工工号查询未删除的人员档案
     * <p>数据库唯一索引 uk_employee_no 对所有行生效（含软删除行）</p>
     *
     * @param employeeNo 员工工号
     * @return 人员档案实体，不存在返回null
     */
    PersonnelFile getByEmployeeNo(String employeeNo);

    /**
     * 清理指定员工工号下已被软删除的记录（物理删除，释放唯一索引 uk_employee_no）
     *
     * @param employeeNo 员工工号
     * @return 清理的记录数
     */
    int cleanSoftDeletedByEmployeeNo(String employeeNo);

    /**
     * 清理指定用户ID下已被软删除的记录（物理删除，释放唯一索引 uk_user_id）
     *
     * @param userId 用户ID
     * @return 清理的记录数
     */
    int cleanSoftDeletedByUserId(Long userId);

    // endregion
}

package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Service.PersonnelFileService;
import com.tonghui.erp.Service.PersonnelCertificateService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.PersonnelFileWithDetailsDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人员档案服务实现类
 * <p>
 * 实现PersonnelFileService接口，提供人员档案相关的业务逻辑处理，包括档案的高级查询、
 * 健康证到期预警查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class PersonnelFileServiceImpl extends ServiceImpl<PersonnelFileMapper, PersonnelFile> implements PersonnelFileService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 人员证书服务，用于查询人员关联的证书信息 */
    @Autowired
    private PersonnelCertificateService personnelCertificateService;

    // endregion

    // region 预警查询
    // ===================================
    // 预警查询
    // ===================================

    /**
     * 查询健康证即将到期的人员档案
     * <p>查询条件：在职状态 + 健康证到期日期在今天到指定天数之间</p>
     *
     * @param days 预警天数范围，查询从今天起days天内将过期的健康证
     * @return 健康证即将到期的人员档案列表，按到期日期升序排列
     */
    @Override
    public List<PersonnelFile> findExpiringHealthCerts(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        LocalDate pastDeadline = today.minusDays(days);
        
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)  // 在职状态
               .isNotNull("health_cert_expire")  // 健康证到期日期不为空
               .ge("health_cert_expire", pastDeadline)  // 大于等于过去N天（包含已过期）
               .le("health_cert_expire", deadline)  // 小于等于截止日期
               .orderByAsc("health_cert_expire");  // 按到期日期升序
        
        return list(wrapper);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据用户ID查询人员档案
     *
     * @param userId 用户ID
     * @return 人员档案实体，不存在则返回null
     */
    @Override
    public PersonnelFile findByUserId(Long userId) {
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return getOne(wrapper);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询人员档案（支持按ID、姓名、工号、状态、部门ID条件组合查询）
     *
     * @param personnelFile 查询条件实体，非null字段将作为等值或模糊查询条件
     * @param pageNum       页码，从0开始
     * @param pageSize      每页数量
     * @return 人员档案分页结果
     */
    @Override
    public Page<PersonnelFile> queryPersonnelFiles(PersonnelFile personnelFile, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;
        Page<PersonnelFile> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();

        if (personnelFile != null) {
            if (personnelFile.getPersonnelFileId() != null) {
                wrapper.eq("personnel_file_id", personnelFile.getPersonnelFileId());
            }
            if (StringUtils.hasText(personnelFile.getName())) {
                wrapper.like("name", personnelFile.getName());
            }
            if (StringUtils.hasText(personnelFile.getEmployeeNo())) {
                wrapper.like("employee_no", personnelFile.getEmployeeNo());
            }
            if (personnelFile.getStatus() != null) {
                wrapper.eq("status", personnelFile.getStatus());
            }
            if (personnelFile.getDepartmentId() != null) {
                wrapper.eq("department_id", personnelFile.getDepartmentId());
            }
        }
        wrapper.orderByDesc("personnel_file_id");
        return baseMapper.selectPage(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询人员档案列表并关联证书信息
     * <p>先分页查询档案主表数据，再批量查询关联的人员证书</p>
     *
     * @param personnelFile 查询条件实体
     * @param pageNum       页码，从0开始
     * @param pageSize      每页数量
     * @return 带子表关联数据的人员档案分页结果
     */
    @Override
    public PagedResult<PersonnelFileWithDetailsDto> searchWithDetails(PersonnelFile personnelFile, int pageNum, int pageSize) {
        // 查询人员档案主表分页数据
        Page<PersonnelFile> parentPage = queryPersonnelFiles(personnelFile, pageNum, pageSize);
        List<PersonnelFile> parents = parentPage.getRecords();

        PagedResult<PersonnelFileWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的人员证书
        List<Long> parentIds = parents.stream().map(PersonnelFile::getPersonnelFileId).collect(Collectors.toList());

        QueryWrapper<PersonnelCertificate> certWrapper = new QueryWrapper<>();
        certWrapper.in("personnel_file_id", parentIds);
        Map<Long, List<PersonnelCertificate>> certMap = personnelCertificateService.list(certWrapper).stream()
                .collect(Collectors.groupingBy(PersonnelCertificate::getPersonnelFileId));

        // 组装带子表数据的DTO
        List<PersonnelFileWithDetailsDto> dtos = parents.stream().map(parent -> {
            PersonnelFileWithDetailsDto dto = new PersonnelFileWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setCertificates(certMap.getOrDefault(parent.getPersonnelFileId(), Collections.emptyList()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion
}

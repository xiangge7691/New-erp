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
 */
@Service
public class PersonnelFileServiceImpl extends ServiceImpl<PersonnelFileMapper, PersonnelFile> implements PersonnelFileService {

    @Autowired
    private PersonnelCertificateService personnelCertificateService;

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

    @Override
    public PagedResult<PersonnelFileWithDetailsDto> searchWithDetails(PersonnelFile personnelFile, int pageNum, int pageSize) {
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

        List<Long> parentIds = parents.stream().map(PersonnelFile::getPersonnelFileId).collect(Collectors.toList());

        QueryWrapper<PersonnelCertificate> certWrapper = new QueryWrapper<>();
        certWrapper.in("personnel_file_id", parentIds);
        Map<Long, List<PersonnelCertificate>> certMap = personnelCertificateService.list(certWrapper).stream()
                .collect(Collectors.groupingBy(PersonnelCertificate::getPersonnelFileId));

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
}

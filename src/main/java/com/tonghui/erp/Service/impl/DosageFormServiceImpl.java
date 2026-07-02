package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.DosageForm.DosageFormWithDetailsDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.DosageForm;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.mapper.DosageFormMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Service.DosageFormService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 药品剂型服务实现类
 * <p>
 * 针对表【dosage_form(药品剂型分类表)】的数据库操作Service实现，提供药品剂型的增删改查等业务逻辑的具体实现
 * </p>
 */
@Service
public class DosageFormServiceImpl extends ServiceImpl<DosageFormMapper, DosageForm>
        implements DosageFormService{

    @Autowired
    private PreparationMapper preparationMapper;

    //#region 剂型查询实现方法
    // ===================================
    // 剂型查询实现方法
    // ===================================

    /**
     * 根据剂型名称模糊查询（分页）
     *
     * @param dosageName 剂型名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的剂型列表和分页信息
     */
    @Override
    public PagedResult<DosageForm> searchByName(String dosageName, PageRequestDto pageRequest) {
        // 创建Page对象，处理全量数据的情况
        Page<DosageForm> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 获取所有数据
            page = new Page<>(1, 10000);
        } else {
            // 页码从0开始，但MyBatis Plus的Page页码从1开始，所以需要+1
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();

        // 如果dosageName不为空，则添加模糊查询条件
        if (dosageName != null && !dosageName.isEmpty()) {
            query.like(DosageForm::getDosageName, dosageName);
        }

        Page<DosageForm> resultPage = query.page(page);

        PagedResult<DosageForm> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 全量数据情况
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            // 分页情况，页码从0开始
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }

        return pagedResult;
    }

    //#endregion

    @Override
    public Page<DosageForm> queryDosageForms(DosageForm dosageForm, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<DosageForm> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<DosageForm> wrapper = new QueryWrapper<>();

        if (dosageForm.getDosageId() != null) {
            wrapper.eq("dosage_id", dosageForm.getDosageId());
        }
        if (dosageForm.getDosageName() != null && !dosageForm.getDosageName().isEmpty()) {
            wrapper.like("dosage_name", dosageForm.getDosageName());
        }
        if (dosageForm.getStatus() != null) {
            wrapper.eq("status", dosageForm.getStatus());
        }

        return this.page(page, wrapper);
    }

    @Override
    public PagedResult<DosageFormWithDetailsDto> searchWithDetails(DosageForm dosageForm, int pageNum, int pageSize) {
        Page<DosageForm> parentPage = queryDosageForms(dosageForm, pageNum, pageSize);
        List<DosageForm> parents = parentPage.getRecords();

        PagedResult<DosageFormWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        List<Long> parentIds = parents.stream().map(DosageForm::getDosageId).collect(Collectors.toList());
        QueryWrapper<Preparation> wrapper = new QueryWrapper<>();
        wrapper.in("dosage_form_id", parentIds);
        List<Preparation> allPreparations = preparationMapper.selectList(wrapper);
        Map<Long, List<Preparation>> preparationsMap = allPreparations.stream()
                .collect(Collectors.groupingBy(Preparation::getDosageFormId));

        List<DosageFormWithDetailsDto> dtos = parents.stream().map(parent -> {
            DosageFormWithDetailsDto dto = new DosageFormWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setPreparations(preparationsMap.getOrDefault(parent.getDosageId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}

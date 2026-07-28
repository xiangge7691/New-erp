package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.DosageForm.DosageFormWithDetailsDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
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
 * 实现DosageFormService接口，提供药品剂型相关的业务逻辑处理，包括剂型的
 * 名称模糊查询、高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class DosageFormServiceImpl extends ServiceImpl<DosageFormMapper, DosageForm>
        implements DosageFormService{

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 制剂数据访问层，用于关联查询剂型关联的制剂信息 */
    @Autowired
    private PreparationMapper preparationMapper;

    /** 软删除统一清理工具 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // endregion

    // region 数据清理接口
    // ===================================
    // 数据清理接口
    // ===================================

    /**
     * 清理指定剂型大类下已被软删除的记录（释放唯一键约束）
     *
     * @param dosageCategory 剂型大类
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByDosageCategory(String dosageCategory) {
        return baseMapper.physicalDeleteByDosageCategory(dosageCategory);
    }

    /**
     * 根据剂型大类查询去重后的剂型名称列表
     *
     * @param dosageCategory 剂型大类
     * @return 去重后的剂型名称列表
     */
    @Override
    public List<String> getDistinctDosageNamesByCategory(String dosageCategory) {
        return baseMapper.selectDistinctDosageNameByCategory(dosageCategory);
    }

    // endregion

    // region 剂型查询实现方法
    // ===================================
    // 剂型查询实现方法
    // ===================================

    /**
     * 根据剂型大类模糊查询（分页）
     *
     * @param dosageCategory 剂型大类（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的剂型列表和分页信息
     */
    @Override
    public PagedResult<DosageForm> searchByName(String dosageCategory, PageRequestDto pageRequest) {
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

        // 如果dosageCategory不为空，则添加模糊查询条件
        if (dosageCategory != null && !dosageCategory.isEmpty()) {
            query.like(DosageForm::getDosageCategory, dosageCategory);
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

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询药品剂型（支持按剂型ID、名称、状态条件组合查询）
     *
     * @param dosageForm 查询条件实体，非null字段将作为等值或模糊查询条件
     * @param pageNum    页码，从0开始
     * @param pageSize   每页数量
     * @return 药品剂型分页结果
     */
    @Override
    public Page<DosageForm> queryDosageForms(DosageForm dosageForm, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<DosageForm> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<DosageForm> wrapper = new QueryWrapper<>();

        if (dosageForm.getDosageId() != null) {
            wrapper.eq("dosage_id", dosageForm.getDosageId());
        }
        if (dosageForm.getDosageCategory() != null && !dosageForm.getDosageCategory().isEmpty()) {
            wrapper.like("dosage_category", dosageForm.getDosageCategory());
        }
        if (dosageForm.getDosageName() != null && !dosageForm.getDosageName().isEmpty()) {
            wrapper.like("dosage_name", dosageForm.getDosageName());
        }
        if (dosageForm.getStatus() != null) {
            wrapper.eq("status", dosageForm.getStatus());
        }

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询药品剂型列表并关联制剂信息
     * <p>先分页查询剂型主表数据，再批量查询关联的制剂</p>
     *
     * @param dosageForm 查询条件实体
     * @param pageNum    页码，从0开始
     * @param pageSize   每页数量
     * @return 带子表关联数据的药品剂型分页结果
     */
    @Override
    public PagedResult<DosageFormWithDetailsDto> searchWithDetails(DosageForm dosageForm, int pageNum, int pageSize) {
        // 查询剂型主表分页数据
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

        // 批量查询关联的制剂
        List<Long> parentIds = parents.stream().map(DosageForm::getDosageId).collect(Collectors.toList());
        QueryWrapper<Preparation> wrapper = new QueryWrapper<>();
        wrapper.in("dosage_form_id", parentIds);
        List<Preparation> allPreparations = preparationMapper.selectList(wrapper);
        Map<Long, List<Preparation>> preparationsMap = allPreparations.stream()
                .collect(Collectors.groupingBy(Preparation::getDosageFormId));

        // 组装带子表数据的DTO
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

    // endregion
}

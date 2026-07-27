package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProcessTypeWithDetailsDto;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.ProductionProcessRecordMapper;
import com.tonghui.erp.Service.ProcessTypeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工序类型服务实现类
 * <p>
 * 实现ProcessTypeService接口，提供工序类型相关的业务逻辑处理，包括工序类型的名称模糊查询、
 * 编码查询、启用状态查询、高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class ProcessTypeServiceImpl extends ServiceImpl<ProcessTypeMapper, ProcessType>
    implements ProcessTypeService{

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 生产过程记录数据访问层，用于关联查询工序类型关联的生产过程记录 */
    @Autowired
    private ProductionProcessRecordMapper productionProcessRecordMapper;

    /** 制剂工序模板数据访问层，用于关联查询工序类型关联的制剂工序模板 */
    @Autowired
    private PreparationProcessTemplateMapper preparationProcessTemplateMapper;

    // endregion

    // region 数据清理接口
    // ===================================
    // 数据清理接口
    // ===================================

    /**
     * 清理指定工序编码下已被软删除的记录（释放唯一键约束）
     *
     * @param processCode 工序编码
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByProcessCode(String processCode) {
        return baseMapper.physicalDeleteByProcessCode(processCode);
    }

    // endregion

    // region 工序类型查询实现方法
    // ===================================
    // 工序类型查询实现方法
    // ===================================

    /**
     * 根据工序类型名称模糊查询（分页）
     *
     * @param processName 工序类型名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的工序类型列表和分页信息
     */
    @Override
    public PagedResult<ProcessType> searchByName(String processName, PageRequestDto pageRequest) {
        // 创建 Page 对象，处理全量数据的情况
        Page<ProcessType> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 获取所有数据
            page = new Page<>(1, 10000);
        } else {
            // 页码从 0 开始，但 MyBatis Plus 的 Page 页码从 1 开始，所以需要 +1
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();

        // 如果 processName 不为空，则添加模糊查询条件
        if (processName != null && !processName.isEmpty()) {
            query.like(ProcessType::getProcessName, processName);
        }

        Page<ProcessType> resultPage = query.page(page);

        PagedResult<ProcessType> pagedResult = new PagedResult<>();
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
            // 分页情况，页码从 0 开始
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }

        return pagedResult;
    }

    /**
     * 根据工序类型编码精确查询
     *
     * @param processCode 工序类型编码
     * @return 查询到的工序类型，不存在则返回 null
     */
    @Override
    public ProcessType getByCode(String processCode) {
        if (processCode == null || processCode.isEmpty()) {
            return null;
        }
        
        return this.lambdaQuery()
                .eq(ProcessType::getProcessCode, processCode)
                .one();
    }

    /**
     * 获取所有启用的工序类型
     *
     * @return 启用的工序类型列表，按名称升序排列
     */
    @Override
    public List<ProcessType> listActive() {
        return this.lambdaQuery()
                .eq(ProcessType::getProcessStatus, 1)
                .orderByAsc(ProcessType::getProcessName)
                .list();
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询工序类型（支持按工序ID、编码、名称、状态条件组合查询）
     *
     * @param processType 查询条件实体，非null字段将作为等值或模糊查询条件
     * @param pageNum     页码，从0开始
     * @param pageSize    每页数量
     * @return 工序类型分页结果
     */
    @Override
    public Page<ProcessType> queryProcessTypes(ProcessType processType, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<ProcessType> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<ProcessType> wrapper = new QueryWrapper<>();

        if (processType.getProcessId() != null) {
            wrapper.eq("process_id", processType.getProcessId());
        }
        if (StringUtils.hasText(processType.getProcessCode())) {
            wrapper.like("process_code", processType.getProcessCode());
        }
        if (StringUtils.hasText(processType.getProcessName())) {
            wrapper.like("process_name", processType.getProcessName());
        }
        if (processType.getProcessStatus() != null) {
            wrapper.eq("process_status", processType.getProcessStatus());
        }

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询工序类型列表并关联生产过程记录和制剂工序模板
     * <p>先分页查询工序类型主表数据，再批量查询关联的生产过程记录和制剂工序模板</p>
     *
     * @param processType 查询条件实体
     * @param pageNum     页码，从0开始
     * @param pageSize    每页数量
     * @return 带子表关联数据的工序类型分页结果
     */
    @Override
    public PagedResult<ProcessTypeWithDetailsDto> searchWithDetails(ProcessType processType, int pageNum, int pageSize) {
        // 查询工序类型主表分页数据
        Page<ProcessType> parentPage = queryProcessTypes(processType, pageNum, pageSize);
        List<ProcessType> parents = parentPage.getRecords();

        PagedResult<ProcessTypeWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的生产过程记录
        List<Integer> parentIds = parents.stream().map(ProcessType::getProcessId).collect(Collectors.toList());
        QueryWrapper<ProductionProcessRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.in("process_type_id", parentIds);
        List<ProductionProcessRecord> allRecords = productionProcessRecordMapper.selectList(recordWrapper);
        Map<Long, List<ProductionProcessRecord>> recordsMap = allRecords.stream()
                .filter(r -> r.getProcessTypeId() != null)
                .collect(Collectors.groupingBy(ProductionProcessRecord::getProcessTypeId));

        // 批量查询关联的制剂工序模板
        QueryWrapper<PreparationProcessTemplate> templateWrapper = new QueryWrapper<>();
        templateWrapper.in("process_type_id", parentIds);
        List<PreparationProcessTemplate> allTemplates = preparationProcessTemplateMapper.selectList(templateWrapper);
        Map<Long, List<PreparationProcessTemplate>> templatesMap = allTemplates.stream()
                .collect(Collectors.groupingBy(PreparationProcessTemplate::getProcessTypeId));

        // 组装带子表数据的DTO
        List<ProcessTypeWithDetailsDto> dtos = parents.stream().map(parent -> {
            ProcessTypeWithDetailsDto dto = new ProcessTypeWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setRecords(recordsMap.getOrDefault(parent.getProcessId().longValue(), List.of()));
            dto.setTemplates(templatesMap.getOrDefault(parent.getProcessId().longValue(), List.of()));
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

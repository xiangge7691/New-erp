package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Service.ProcessTypeService;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工序类型服务实现类
 * <p>
 * 针对表【process_type(工序类型表)】的数据库操作 Service 实现，提供工序类型的增删改查等业务逻辑的具体实现
 * </p>
 */
@Service
public class ProcessTypeServiceImpl extends ServiceImpl<ProcessTypeMapper, ProcessType>
    implements ProcessTypeService{

    //#region 工序类型查询实现方法
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
     * @return 启用的工序类型列表
     */
    @Override
    public List<ProcessType> listActive() {
        return this.lambdaQuery()
                .eq(ProcessType::getProcessStatus, 1)
                .orderByAsc(ProcessType::getProcessName)
                .list();
    }

    //#endregion
}





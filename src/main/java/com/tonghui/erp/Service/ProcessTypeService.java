package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProcessTypeWithDetailsDto;

/**
 * @author 87954
 * @description 针对表【process_type(工序类型表)】的数据库操作 Service
 * @createDate 2026-03-09 10:03:25
 */
public interface ProcessTypeService extends IService<ProcessType> {
    
    /**
     * 根据工序类型名称模糊查询（分页）
     * 
     * @param processName 工序类型名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的工序类型列表和分页信息
     */
    PagedResult<ProcessType> searchByName(String processName, PageRequestDto pageRequest);
    
    /**
     * 根据工序类型编码精确查询
     * 
     * @param processCode 工序类型编码
     * @return 查询到的工序类型，不存在则返回 null
     */
    ProcessType getByCode(String processCode);
    
    /**
     * 获取所有启用的工序类型
     * 
     * @return 启用的工序类型列表
     */
    java.util.List<ProcessType> listActive();

    Page<ProcessType> queryProcessTypes(ProcessType processType, int pageNum, int pageSize);

    PagedResult<ProcessTypeWithDetailsDto> searchWithDetails(ProcessType processType, int pageNum, int pageSize);
}

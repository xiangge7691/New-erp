package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * @author 87954
 * @description 针对表【production_process_record(生产工序记录表)】的数据库操作 Service
 * @createDate 2026-03-09 10:03:26
 */
public interface ProductionProcessRecordService extends IService<ProductionProcessRecord> {
    
    /**
     * 根据生产计划 ID 查询工序记录列表
     * 
     * @param planId 生产计划 ID
     * @return 工序记录列表，按工序顺序排序
     */
    List<ProductionProcessRecord> listByPlanId(Integer planId);
    
    /**
     * 根据生产计划 ID 分页查询工序记录
     * 
     * @param planId 生产计划 ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PagedResult<ProductionProcessRecord> listByPlanIdPaged(Integer planId, PageRequestDto pageRequest);
    
    /**
     * 根据记录状态查询工序记录（分页）
     * 
     * @param recordStatus 记录状态（1-正常，0-作废）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PagedResult<ProductionProcessRecord> listByStatus(Integer recordStatus, PageRequestDto pageRequest);
    
    /**
     * 根据工序名称模糊查询（分页）
     * 
     * @param processName 工序名称（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PagedResult<ProductionProcessRecord> searchByProcessName(String processName, PageRequestDto pageRequest);
    
    /**
     * 根据操作人姓名模糊查询（分页）
     * 
     * @param operatorName 操作人姓名（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PagedResult<ProductionProcessRecord> searchByOperatorName(String operatorName, PageRequestDto pageRequest);
    
    /**
     * 作废指定的工序记录
     * 
     * @param recordId 记录 ID
     * @param updaterId 更新人 ID
     * @return 操作是否成功
     */
    boolean cancelRecord(Long recordId, Long updaterId);
}

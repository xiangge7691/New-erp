package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* @author 87954
* @description 针对表【production_plan(生产计划主表)】的数据库操作Service
* @createDate 2025-12-08 14:02:29
*/
public interface ProductionPlanService extends IService<ProductionPlan> {

    /**
     * 高级查询生产计划（支持分页）
     *
     * @param productionPlan 查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<ProductionPlan> queryProductionPlans(ProductionPlan productionPlan, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, int pageNum, int pageSize);
    
    /**
     * 更改生产计划状态
     *
     * @param planId 生产计划ID
     * @param newStatus 新状态
     * @param operatorId 操作员ID
     * @param remark 备注
     * @param finishedQuantity 成品数量（仅在出库状态时使用）
     * @param productionCycle 生产周期（仅在出库状态时使用）
     * @param yieldRate 得率（仅在出库状态时使用）
     * @param unitPrice 单价（仅在出库状态时使用）
     * @return 是否成功
     */
    boolean changePlanStatus(Integer planId, String newStatus, Long operatorId, String remark, 
                            BigDecimal finishedQuantity, Integer productionCycle, BigDecimal yieldRate, BigDecimal unitPrice);
    
    /**
     * 恢复暂停的生产计划状态
     *
     * @param planId 生产计划ID
     * @param operatorId 操作员ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean resumePlanStatus(Integer planId, Long operatorId, String remark);
    
    /**
     * 验证状态变更是否符合业务规则
     *
     * @param oldStatus 当前状态
     * @param newStatus 新状态
     * @return 是否合法
     */
    boolean validateStatusChange(String oldStatus, String newStatus);
}

package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.Entity.PlanStatusLog;
import com.tonghui.erp.Data.mapper.ProductionProcessRecordMapper;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Service.PlanStatusLogService;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 87954
* @description 针对表【production_plan(生产计划主表)】的数据库操作Service实现
* @createDate 2025-12-08 14:02:29
*/
@Service
public class ProductionPlanServiceImpl extends ServiceImpl<ProductionPlanMapper, ProductionPlan>
    implements ProductionPlanService{
    
    private final PlanStatusLogService planStatusLogService;
    
    public ProductionPlanServiceImpl(PlanStatusLogService planStatusLogService) {
        this.planStatusLogService = planStatusLogService;
    }

    @Autowired
    private ProductionProcessRecordMapper productionProcessRecordMapper;
    
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
    @Override
    public Page<ProductionPlan> queryProductionPlans(ProductionPlan productionPlan,
                                                     LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                     LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                     LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                     LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                     LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                     LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                     LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                     LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                     int pageNum, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageNum = pageNum + 1;

        Page<ProductionPlan> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<ProductionPlan> wrapper = new QueryWrapper<>();

        if (productionPlan.getId() != null) {
            wrapper.eq("id", productionPlan.getId());
        }
        if (StringUtils.hasText(productionPlan.getPlanNumber())) {
            wrapper.like("plan_number", productionPlan.getPlanNumber());
        }
        if (StringUtils.hasText(productionPlan.getRelatedOrder())) {
            wrapper.like("related_order", productionPlan.getRelatedOrder());
        }
        if (StringUtils.hasText(productionPlan.getPreparationCode())) {
            wrapper.like("preparation_code", productionPlan.getPreparationCode());
        }
        if (StringUtils.hasText(productionPlan.getPreparationName())) {
            wrapper.like("preparation_name", productionPlan.getPreparationName());
        }
        if (StringUtils.hasText(productionPlan.getCurrentStatus())) {
            wrapper.eq("current_status", productionPlan.getCurrentStatus());
        }
        if (productionPlan.getIsArchived() != null) {
            wrapper.eq("is_archived", productionPlan.getIsArchived());
        }
        
        // Handle created time range query
        if (createdTimeStart != null) {
            wrapper.ge("create_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("create_time", createdTimeEnd);
        }
        
        // Handle updated time range query
        if (updatedTimeStart != null) {
            wrapper.ge("update_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("update_time", updatedTimeEnd);
        }

        // Handle production start time range query
        if (productionStartTimeStart != null) {
            wrapper.ge("production_start_time", productionStartTimeStart);
        }
        if (productionStartTimeEnd != null) {
            wrapper.le("production_start_time", productionStartTimeEnd);
        }

        // Handle production end time range query
        if (productionEndTimeStart != null) {
            wrapper.ge("production_end_time", productionEndTimeStart);
        }
        if (productionEndTimeEnd != null) {
            wrapper.le("production_end_time", productionEndTimeEnd);
        }

        // Handle inspection start time range query
        if (inspectionStartTimeStart != null) {
            wrapper.ge("inspection_start_time", inspectionStartTimeStart);
        }
        if (inspectionStartTimeEnd != null) {
            wrapper.le("inspection_start_time", inspectionStartTimeEnd);
        }

        // Handle inspection end time range query
        if (inspectionEndTimeStart != null) {
            wrapper.ge("inspection_end_time", inspectionEndTimeStart);
        }
        if (inspectionEndTimeEnd != null) {
            wrapper.le("inspection_end_time", inspectionEndTimeEnd);
        }

        // Handle outbound time range query
        if (outboundTimeStart != null) {
            wrapper.ge("outbound_time", outboundTimeStart);
        }
        if (outboundTimeEnd != null) {
            wrapper.le("outbound_time", outboundTimeEnd);
        }

        // Handle archive time range query
        if (archiveTimeStart != null) {
            wrapper.ge("archive_time", archiveTimeStart);
        }
        if (archiveTimeEnd != null) {
            wrapper.le("archive_time", archiveTimeEnd);
        }
        
        // 按编号倒序排列
        wrapper.orderByDesc("plan_number");

        return this.page(page, wrapper);
    }

    @Override
    public PagedResult<ProductionPlanWithRecordsDto> searchWithDetails(ProductionPlan productionPlan,
                                                                       LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                       LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                       LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                                       LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                                       LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                                       LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                                       LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                                       LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                                       int pageNum, int pageSize) {
        Page<ProductionPlan> parentPage = queryProductionPlans(productionPlan, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                inspectionStartTimeStart, inspectionStartTimeEnd, inspectionEndTimeStart, inspectionEndTimeEnd,
                outboundTimeStart, outboundTimeEnd, archiveTimeStart, archiveTimeEnd, pageNum, pageSize);
        List<ProductionPlan> parents = parentPage.getRecords();

        PagedResult<ProductionPlanWithRecordsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        List<Integer> parentIds = parents.stream().map(ProductionPlan::getId).collect(Collectors.toList());
        QueryWrapper<ProductionProcessRecord> wrapper = new QueryWrapper<>();
        wrapper.in("plan_id", parentIds);
        List<ProductionProcessRecord> allRecords = productionProcessRecordMapper.selectList(wrapper);
        Map<Integer, List<ProductionProcessRecord>> recordsMap = allRecords.stream()
                .collect(Collectors.groupingBy(ProductionProcessRecord::getPlanId));

        List<ProductionPlanWithRecordsDto> dtos = parents.stream().map(parent -> {
            ProductionPlanWithRecordsDto dto = new ProductionPlanWithRecordsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setRecords(recordsMap.getOrDefault(parent.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
    
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePlanStatus(Integer planId, String newStatus, Long operatorId, String remark, 
                                   BigDecimal finishedQuantity, Integer productionCycle, BigDecimal yieldRate, BigDecimal unitPrice) {
        // 获取当前计划
        ProductionPlan plan = this.getById(planId);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }
        
        String oldStatus = plan.getCurrentStatus();
        
        // 验证状态变更是否符合业务规则
        if (!validateStatusChange(oldStatus, newStatus)) {
            throw new RuntimeException("状态流转不符合业务规则");
        }
        
        // 根据目标状态决定如何更新主表
        if ("OUTBOUND".equals(newStatus)) {
            // 出库状态的特殊处理
            BigDecimal totalAmount = finishedQuantity.multiply(unitPrice);
            
            plan.setCurrentStatus(newStatus);
            plan.setCurrentStatusDate(LocalDateTime.now());
            plan.setUpdateUser(operatorId);
            plan.setFinishedQuantity(finishedQuantity);
            plan.setProductionCycle(productionCycle);
            plan.setYieldRate(yieldRate);
            plan.setUnitPrice(unitPrice);
            plan.setTotalAmount(totalAmount);
        } else {
            // 非出库状态的通用处理
            plan.setCurrentStatus(newStatus);
            plan.setCurrentStatusDate(LocalDateTime.now());
            plan.setUpdateUser(operatorId);
        }
        
        // 更新生产计划主表
        this.updateById(plan);
        
        // 插入状态变更流水记录
        PlanStatusLog statusLog = new PlanStatusLog();
        statusLog.setPlanId(planId);
        statusLog.setFromStatus(oldStatus);
        statusLog.setToStatus(newStatus);
        statusLog.setOperator(operatorId);
        statusLog.setRemark(remark);
        statusLog.setChangeTime(LocalDateTime.now());
        
        planStatusLogService.save(statusLog);
        
        return true;
    }
    
    /**
     * 恢复暂停的生产计划状态
     *
     * @param planId 生产计划ID
     * @param operatorId 操作员ID
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resumePlanStatus(Integer planId, Long operatorId, String remark) {
        // 获取当前计划
        ProductionPlan plan = this.getById(planId);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }
        
        String oldStatus = plan.getCurrentStatus();
        
        // 验证当前状态必须是暂停状态
        if (!"SUSPENDED".equals(oldStatus)) {
            throw new RuntimeException("只有暂停状态才能恢复");
        }
        
        // 查询暂停前的状态（从状态流水表获取最近一次暂停操作前的状态）
        QueryWrapper<PlanStatusLog> logQueryWrapper = new QueryWrapper<>();
        logQueryWrapper.eq("plan_id", planId)
                      .eq("to_status", "SUSPENDED")
                      .orderByDesc("change_time")
                      .last("LIMIT 1");
        
        PlanStatusLog lastSuspendLog = planStatusLogService.getOne(logQueryWrapper);
        if (lastSuspendLog == null) {
            throw new RuntimeException("无法找到暂停前的状态");
        }
        
        String previousStatus = lastSuspendLog.getFromStatus();
        
        // 验证状态变更的合法性
        if (!validateStatusChange("SUSPENDED", previousStatus)) {
            throw new RuntimeException("恢复状态不符合业务规则");
        }
        
        // 更新主表状态
        plan.setCurrentStatus(previousStatus);
        plan.setCurrentStatusDate(LocalDateTime.now());
        plan.setUpdateUser(operatorId);
        this.updateById(plan);
        
        // 记录状态变更流水
        PlanStatusLog statusLog = new PlanStatusLog();
        statusLog.setPlanId(planId);
        statusLog.setFromStatus("SUSPENDED");
        statusLog.setToStatus(previousStatus);
        statusLog.setOperator(operatorId);
        statusLog.setRemark("恢复生产：" + remark);
        statusLog.setChangeTime(LocalDateTime.now());
        
        planStatusLogService.save(statusLog);
        
        return true;
    }
    
    /**
     * 验证状态变更是否符合业务规则
     *
     * @param oldStatus 当前状态
     * @param newStatus 新状态
     * @return 是否合法
     */
    @Override
    public boolean validateStatusChange(String oldStatus, String newStatus) {
        // 正常生产流程
        if (("PLAN_ISSUED".equals(oldStatus) && "MATERIAL_PREP".equals(newStatus)) ||
            ("MATERIAL_PREP".equals(oldStatus) && "IN_PRODUCTION".equals(newStatus)) ||
            ("IN_PRODUCTION".equals(oldStatus) && "IN_INSPECTION".equals(newStatus)) ||
            ("IN_INSPECTION".equals(oldStatus) && "OUTBOUND".equals(newStatus)) ||
            // 返工流程
            ("IN_INSPECTION".equals(oldStatus) && "IN_PRODUCTION".equals(newStatus)) ||
            // 异常处理：暂停和取消
            "SUSPENDED".equals(newStatus) || 
            "CANCELLED".equals(newStatus) ||
            // 恢复机制：从暂停状态恢复到工作状态
            ("SUSPENDED".equals(oldStatus) && "MATERIAL_PREP".equals(newStatus)) ||
            ("SUSPENDED".equals(oldStatus) && "IN_PRODUCTION".equals(newStatus)) ||
            ("SUSPENDED".equals(oldStatus) && "IN_INSPECTION".equals(newStatus))) {
            return true;
        }
        
        return false;
    }
}





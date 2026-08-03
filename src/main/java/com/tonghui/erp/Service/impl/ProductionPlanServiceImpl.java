package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.Entity.PlanStatusLog;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.ProductionProcessRecordMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
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
 * 生产计划服务实现类
 * <p>
 * 实现ProductionPlanService接口，提供生产计划相关的业务逻辑处理，包括计划的高级查询、
 * 带子表关联查询、状态变更、暂停恢复、状态验证等功能的具体实现
 * </p>
 *
 */
@Service
public class ProductionPlanServiceImpl extends ServiceImpl<ProductionPlanMapper, ProductionPlan>
    implements ProductionPlanService{
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 计划状态日志服务，用于记录计划状态变更历史 */
    private final PlanStatusLogService planStatusLogService;

    /** 生产过程记录数据访问层，用于关联查询计划关联的生产过程记录 */
    @Autowired
    private ProductionProcessRecordMapper productionProcessRecordMapper;

    /** 工单数据访问层，用于关联查询计划关联的生产任务 */
    @Autowired
    private WorkOrderMapper workOrderMapper;
    
    /**
     * 构造函数注入依赖
     *
     * @param planStatusLogService 计划状态日志服务
     */
    public ProductionPlanServiceImpl(PlanStatusLogService planStatusLogService) {
        this.planStatusLogService = planStatusLogService;
    }

    // endregion
    
    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询生产计划（支持多条件组合查询和多种时间范围筛选）
     * <p>支持按计划编号、关联单号、制剂编码、制剂名称、当前状态、是否归档等条件筛选，
     * 同时支持创建时间、更新时间、生产时间、检验时间、出库时间、归档时间等多种时间范围查询</p>
     *
     * @param productionPlan            查询条件实体
     * @param createdTimeStart          创建时间起始值（含）
     * @param createdTimeEnd            创建时间结束值（含）
     * @param updatedTimeStart          更新时间起始值（含）
     * @param updatedTimeEnd            更新时间结束值（含）
     * @param productionStartTimeStart  生产开始时间起始值（含）
     * @param productionStartTimeEnd    生产开始时间结束值（含）
     * @param productionEndTimeStart    生产结束时间起始值（含）
     * @param productionEndTimeEnd      生产结束时间结束值（含）
     * @param inspectionStartTimeStart  检验开始时间起始值（含）
     * @param inspectionStartTimeEnd    检验开始时间结束值（含）
     * @param inspectionEndTimeStart    检验结束时间起始值（含）
     * @param inspectionEndTimeEnd      检验结束时间结束值（含）
     * @param outboundTimeStart         出库时间起始值（含）
     * @param outboundTimeEnd           出库时间结束值（含）
     * @param archiveTimeStart          归档时间起始值（含）
     * @param archiveTimeEnd            归档时间结束值（含）
     * @param timeFieldType             动态时间字段类型，可选值见枚举定义
     * @param timeStart                 动态时间字段起始值（含）
     * @param timeEnd                   动态时间字段结束值（含）
     * @param pageNum                   页码，从0开始
     * @param pageSize                  每页数量
     * @return 生产计划分页结果
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
                                                     String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd,
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
            // 使用CASE表达式根据时间字段计算当前状态
            wrapper.apply("CASE " +
                "WHEN archive_time IS NOT NULL THEN 'ARCHIVED' " +
                "WHEN outbound_time IS NOT NULL THEN 'OUTBOUND' " +
                "WHEN inspection_end_time IS NOT NULL THEN 'INSPECTED' " +
                "WHEN inspection_start_time IS NOT NULL THEN 'IN_INSPECTION' " +
                "WHEN production_end_time IS NOT NULL THEN 'PRODUCED' " +
                "WHEN production_start_time IS NOT NULL THEN 'IN_PRODUCTION' " +
                "ELSE 'PLAN_ISSUED' END = {0}", productionPlan.getCurrentStatus());
        }
        if (productionPlan.getIsArchived() != null) {
            wrapper.eq("is_archived", productionPlan.getIsArchived());
        }
        
        // 创建时间范围查询
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        
        // 更新时间范围查询
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        // 生产开始时间范围查询
        if (productionStartTimeStart != null) {
            wrapper.ge("production_start_time", productionStartTimeStart);
        }
        if (productionStartTimeEnd != null) {
            wrapper.le("production_start_time", productionStartTimeEnd);
        }

        // 生产结束时间范围查询
        if (productionEndTimeStart != null) {
            wrapper.ge("production_end_time", productionEndTimeStart);
        }
        if (productionEndTimeEnd != null) {
            wrapper.le("production_end_time", productionEndTimeEnd);
        }

        // 检验开始时间范围查询
        if (inspectionStartTimeStart != null) {
            wrapper.ge("inspection_start_time", inspectionStartTimeStart);
        }
        if (inspectionStartTimeEnd != null) {
            wrapper.le("inspection_start_time", inspectionStartTimeEnd);
        }

        // 检验结束时间范围查询
        if (inspectionEndTimeStart != null) {
            wrapper.ge("inspection_end_time", inspectionEndTimeStart);
        }
        if (inspectionEndTimeEnd != null) {
            wrapper.le("inspection_end_time", inspectionEndTimeEnd);
        }

        // 出库时间范围查询
        if (outboundTimeStart != null) {
            wrapper.ge("outbound_time", outboundTimeStart);
        }
        if (outboundTimeEnd != null) {
            wrapper.le("outbound_time", outboundTimeEnd);
        }

        // 归档时间范围查询
        if (archiveTimeStart != null) {
            wrapper.ge("archive_time", archiveTimeStart);
        }
        if (archiveTimeEnd != null) {
            wrapper.le("archive_time", archiveTimeEnd);
        }
        
        // 动态时间筛选（根据传入的时间字段类型动态选择筛选列）
        if (StringUtils.hasText(timeFieldType) && (timeStart != null || timeEnd != null)) {
            String column = switch (timeFieldType) {
                case "CREATED_TIME" -> "created_time";
                case "UPDATED_TIME" -> "updated_time";
                case "PRODUCTION_START_TIME" -> "production_start_time";
                case "PRODUCTION_END_TIME" -> "production_end_time";
                case "INSPECTION_START_TIME" -> "inspection_start_time";
                case "INSPECTION_END_TIME" -> "inspection_end_time";
                case "OUTBOUND_TIME" -> "outbound_time";
                case "ARCHIVE_TIME" -> "archive_time";
                default -> null;
            };
            if (column != null) {
                if (timeStart != null) wrapper.ge(column, timeStart);
                if (timeEnd != null) wrapper.le(column, timeEnd);
            }
        }
        
        // 按创建时间倒序排列，新创建的显示在最前
        wrapper.orderByDesc("created_time");

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询生产计划列表并关联生产过程记录
     * <p>先分页查询计划主表数据，再批量查询关联的生产过程记录</p>
     *
     * @param productionPlan            查询条件实体
     * @param createdTimeStart          创建时间起始值（含）
     * @param createdTimeEnd            创建时间结束值（含）
     * @param updatedTimeStart          更新时间起始值（含）
     * @param updatedTimeEnd            更新时间结束值（含）
     * @param productionStartTimeStart  生产开始时间起始值（含）
     * @param productionStartTimeEnd    生产开始时间结束值（含）
     * @param productionEndTimeStart    生产结束时间起始值（含）
     * @param productionEndTimeEnd      生产结束时间结束值（含）
     * @param inspectionStartTimeStart  检验开始时间起始值（含）
     * @param inspectionStartTimeEnd    检验开始时间结束值（含）
     * @param inspectionEndTimeStart    检验结束时间起始值（含）
     * @param inspectionEndTimeEnd      检验结束时间结束值（含）
     * @param outboundTimeStart         出库时间起始值（含）
     * @param outboundTimeEnd           出库时间结束值（含）
     * @param archiveTimeStart          归档时间起始值（含）
     * @param archiveTimeEnd            归档时间结束值（含）
     * @param timeFieldType             动态时间字段类型
     * @param timeStart                 动态时间字段起始值（含）
     * @param timeEnd                   动态时间字段结束值（含）
     * @param pageNum                   页码，从0开始
     * @param pageSize                  每页数量
     * @return 带子表关联数据的生产计划分页结果
     */
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
                                                                       String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd,
                                                                       int pageNum, int pageSize) {
        // 查询生产计划主表分页数据
        Page<ProductionPlan> parentPage = queryProductionPlans(productionPlan, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                inspectionStartTimeStart, inspectionStartTimeEnd, inspectionEndTimeStart, inspectionEndTimeEnd,
                outboundTimeStart, outboundTimeEnd, archiveTimeStart, archiveTimeEnd, timeFieldType, timeStart, timeEnd, pageNum, pageSize);
        List<ProductionPlan> parents = parentPage.getRecords();

        PagedResult<ProductionPlanWithRecordsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的生产过程记录
        List<Integer> parentIds = parents.stream().map(ProductionPlan::getId).collect(Collectors.toList());
        QueryWrapper<ProductionProcessRecord> wrapper = new QueryWrapper<>();
        wrapper.in("plan_id", parentIds);
        List<ProductionProcessRecord> allRecords = productionProcessRecordMapper.selectList(wrapper);
        Map<Integer, List<ProductionProcessRecord>> recordsMap = allRecords.stream()
                .collect(Collectors.groupingBy(ProductionProcessRecord::getPlanId));

        // 批量查询关联的生产任务（工单）
        QueryWrapper<WorkOrder> workOrderWrapper = new QueryWrapper<>();
        workOrderWrapper.in("plan_id", parentIds);
        workOrderWrapper.eq("is_deleted", 0);
        List<WorkOrder> allWorkOrders = workOrderMapper.selectList(workOrderWrapper);
        Map<Long, List<WorkOrder>> workOrdersMap = allWorkOrders.stream()
                .collect(Collectors.groupingBy(WorkOrder::getPlanId));

        // 组装带子表数据的DTO
        List<ProductionPlanWithRecordsDto> dtos = parents.stream().map(parent -> {
            ProductionPlanWithRecordsDto dto = new ProductionPlanWithRecordsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setRecords(recordsMap.getOrDefault(parent.getId(), List.of()));
            dto.setWorkOrders(workOrdersMap.getOrDefault(parent.getId().longValue(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion
    
    // region 状态管理
    // ===================================
    // 状态管理
    // ===================================
    
    /**
     * 根据时间字段自动计算生产计划状态
     * <p>
     * 状态判定逻辑（按优先级从高到低）：
     * ARCHIVED → OUTBOUND → INSPECTED → IN_INSPECTION → PRODUCED → IN_PRODUCTION → PLAN_ISSUED
     * </p>
     *
     * @param plan 生产计划实体
     * @return 计算出的状态字符串
     */
    private String computeStatus(ProductionPlan plan) {
        if (plan.getArchiveTime() != null) return "ARCHIVED";
        if (plan.getOutboundTime() != null) return "OUTBOUND";
        if (plan.getInspectionEndTime() != null) return "INSPECTED";
        if (plan.getInspectionStartTime() != null) return "IN_INSPECTION";
        if (plan.getProductionEndTime() != null) return "PRODUCED";
        if (plan.getProductionStartTime() != null) return "IN_PRODUCTION";
        return "PLAN_ISSUED";
    }

    /**
     * 更改生产计划状态（通过设置对应时间字段）
     * <p>
     * 状态流转规则：已下单 → 生产中 → 已生产 → 检验中 → 已检验 → 已出库 → 已归档
     * 每次状态变更会自动设置对应的时间字段，并记录状态变更日志
     * </p>
     *
     * @param planId             生产计划ID
     * @param newStatus          新状态
     * @param operatorId         操作员ID
     * @param remark             备注
     * @param finishedQuantity   成品数量（仅在出库状态时使用）
     * @param productionCycle    生产周期（仅在出库状态时使用）
     * @param yieldRate          得率（仅在出库状态时使用）
     * @param unitPrice          单价（仅在出库状态时使用）
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePlanStatus(Integer planId, String newStatus, Long operatorId, String remark, 
                                   BigDecimal finishedQuantity, Integer productionCycle, BigDecimal yieldRate, BigDecimal unitPrice) {
        ProductionPlan plan = this.getById(planId);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }
        
        // 计算当前状态
        String oldStatus = computeStatus(plan);
        
        // 验证状态变更是否合法
        if (!validateStatusChange(oldStatus, newStatus)) {
            throw new RuntimeException("状态流转不符合业务规则");
        }
        
        LocalDateTime now = LocalDateTime.now();
        plan.setUpdatedBy(operatorId);
        
        // 根据新状态设置对应的时间字段和业务数据
        switch (newStatus) {
            case "IN_PRODUCTION" -> plan.setProductionStartTime(now);
            case "PRODUCED" -> plan.setProductionEndTime(now);
            case "IN_INSPECTION" -> plan.setInspectionStartTime(now);
            case "INSPECTED" -> plan.setInspectionEndTime(now);
            case "OUTBOUND" -> {
                plan.setOutboundTime(now);
                // 计算总金额
                BigDecimal totalAmount = finishedQuantity.multiply(unitPrice);
                plan.setFinishedQuantity(finishedQuantity);
                plan.setProductionCycle(productionCycle);
                plan.setYieldRate(yieldRate);
                plan.setUnitPrice(unitPrice);
                plan.setTotalAmount(totalAmount);
            }
            case "ARCHIVED" -> {
                plan.setArchiveTime(now);
                plan.setIsArchived(1);
            }
        }
        
        // 更新计划
        this.updateById(plan);
        
        // 记录状态变更日志
        PlanStatusLog statusLog = new PlanStatusLog();
        statusLog.setPlanId(planId);
        statusLog.setFromStatus(oldStatus);
        statusLog.setToStatus(newStatus);
        statusLog.setOperator(operatorId);
        statusLog.setRemark(remark);
        statusLog.setChangeTime(now);
        planStatusLogService.save(statusLog);
        
        return true;
    }
    
    /**
     * 恢复暂停的生产计划状态
     * <p>从暂停状态恢复到暂停前的工作状态，并记录状态变更日志</p>
     *
     * @param planId     生产计划ID
     * @param operatorId 操作员ID
     * @param remark     备注
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resumePlanStatus(Integer planId, Long operatorId, String remark) {
        ProductionPlan plan = this.getById(planId);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }
        
        // 计算当前状态
        String oldStatus = computeStatus(plan);
        
        // 只有暂停状态才能恢复
        if (!"SUSPENDED".equals(oldStatus)) {
            throw new RuntimeException("只有暂停状态才能恢复");
        }
        
        // 查询暂停前的状态日志
        QueryWrapper<PlanStatusLog> logQueryWrapper = new QueryWrapper<>();
        logQueryWrapper.eq("plan_id", planId)
                      .eq("to_status", "SUSPENDED")
                      .orderByDesc("change_time")
                      .last("LIMIT 1");
        
        PlanStatusLog lastSuspendLog = planStatusLogService.getOne(logQueryWrapper);
        if (lastSuspendLog == null) {
            throw new RuntimeException("无法找到暂停前的状态");
        }
        
        // 获取暂停前的状态
        String previousStatus = lastSuspendLog.getFromStatus();
        
        // 验证恢复到的状态是否合法
        if (!validateStatusChange("SUSPENDED", previousStatus)) {
            throw new RuntimeException("恢复状态不符合业务规则");
        }
        
        plan.setUpdatedBy(operatorId);
        this.updateById(plan);
        
        // 记录恢复状态日志
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
     * <p>
     * 允许的状态流转：
     * - 正常流程：已下单 → 生产中 → 已生产 → 检验中 → 已检验 → 已出库 → 已归档
     * - 返工流程：检验中 → 生产中
     * - 异常处理：任意状态 → 暂停/取消
     * - 恢复机制：暂停 → 生产中
     * </p>
     *
     * @param oldStatus 当前状态
     * @param newStatus 新状态
     * @return 是否合法
     */
    @Override
    public boolean validateStatusChange(String oldStatus, String newStatus) {
        // 正常生产流程：已下单 → 生产中 → 已生产 → 检验中 → 已检验 → 已出库 → 已归档
        if (("PLAN_ISSUED".equals(oldStatus) && "IN_PRODUCTION".equals(newStatus)) ||
            ("IN_PRODUCTION".equals(oldStatus) && "PRODUCED".equals(newStatus)) ||
            ("PRODUCED".equals(oldStatus) && "IN_INSPECTION".equals(newStatus)) ||
            ("IN_INSPECTION".equals(oldStatus) && "INSPECTED".equals(newStatus)) ||
            ("INSPECTED".equals(oldStatus) && "OUTBOUND".equals(newStatus)) ||
            ("OUTBOUND".equals(oldStatus) && "ARCHIVED".equals(newStatus)) ||
            // 返工流程：检验中 → 生产中
            ("IN_INSPECTION".equals(oldStatus) && "IN_PRODUCTION".equals(newStatus)) ||
            // 异常处理：暂停和取消
            "SUSPENDED".equals(newStatus) || 
            "CANCELLED".equals(newStatus) ||
            // 恢复机制：从暂停状态恢复到工作状态
            ("SUSPENDED".equals(oldStatus) && "IN_PRODUCTION".equals(newStatus))) {
            return true;
        }
        
        return false;
    }

    // endregion
}

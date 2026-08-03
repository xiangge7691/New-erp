package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.ProductionProcessRecordMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生产计划服务实现类
 * <p>
 * 实现ProductionPlanService接口，提供生产计划相关的业务逻辑处理，包括计划的高级查询、
 * 带子表关联查询、状态刷新（基于关联工单状态动态计算）等功能的具体实现
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

    /** 生产过程记录数据访问层，用于关联查询计划关联的生产过程记录 */
    @Autowired
    private ProductionProcessRecordMapper productionProcessRecordMapper;

    /** 工单数据访问层，用于关联查询计划关联的生产任务及状态计算 */
    @Autowired
    private WorkOrderMapper workOrderMapper;

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
            // 状态已落库，直接按中文状态值精确匹配（待生产/生产中/已完成）
            wrapper.eq("current_status", productionPlan.getCurrentStatus());
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
     * 刷新生产计划状态（基于关联工单状态动态计算并落库）
     * <p>
     * 状态判定逻辑：
     * <ul>
     *   <li>计划未关联任何未删除工单 → 待生产</li>
     *   <li>计划关联了工单，且存在未出库的工单 → 生产中</li>
     *   <li>计划关联的所有工单均已出库或已归档 → 已完成</li>
     * </ul>
     * 在计划创建及工单新增/修改/删除后调用，保证 current_status 列实时准确
     * </p>
     *
     * @param planId 生产计划ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPlanStatus(Integer planId) {
        if (planId == null) {
            return;
        }

        // 查询计划关联的所有未删除工单
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("plan_id", planId);
        wrapper.eq("is_deleted", 0);
        List<WorkOrder> workOrders = workOrderMapper.selectList(wrapper);

        // 根据工单状态计算计划状态
        String status;
        if (workOrders.isEmpty()) {
            // 无关联工单 → 待生产
            status = "待生产";
        } else {
            // 所有工单均已出库或已归档 → 已完成，否则 → 生产中
            boolean allCompleted = workOrders.stream().allMatch(wo ->
                    "已出库".equals(wo.getCurrentStatus()) || "已归档".equals(wo.getCurrentStatus()));
            status = allCompleted ? "已完成" : "生产中";
        }

        // 状态落库
        ProductionPlan plan = this.getById(planId);
        if (plan != null) {
            plan.setCurrentStatus(status);
            plan.setCurrentStatusDate(LocalDateTime.now());
            plan.setUpdatedTime(LocalDateTime.now());
            this.updateById(plan);
        }
    }

    // endregion
}

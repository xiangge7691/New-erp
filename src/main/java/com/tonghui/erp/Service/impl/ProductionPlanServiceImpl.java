package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Production.ProductionDurationSummaryDto;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Common.utils.ProductionDurationUtils;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Data.mapper.ProductionProcessRecordMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** 制剂数据访问层，用于查询时为单价为空的计划回填制剂销售价 */
    @Autowired
    private PreparationMapper preparationMapper;

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
     * @param keyword                   关键字（对计划编号、计划名称进行模糊匹配）
     * @param createdTimeStart          创建时间起始值（含）
     * @param createdTimeEnd            创建时间结束值（含）
     * @param updatedTimeStart          更新时间起始值（含）
     * @param updatedTimeEnd            更新时间结束值（含）
     * @param productionStartTimeStart  生产开始时间起始值（含）
     * @param productionStartTimeEnd    生产开始时间结束值（含）
     * @param productionEndTimeStart    生产结束时间起始值（含）
     * @param productionEndTimeEnd      生产结束时间结束值（含）
     * @param planProductionTimeStart   计划生产时间起始值（含）
     * @param planProductionTimeEnd     计划生产时间结束值（含）
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
                                                     String keyword,
                                                     LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                     LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                     LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                     LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                     LocalDateTime planProductionTimeStart, LocalDateTime planProductionTimeEnd,
                                                     LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                     LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                     LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                     LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                     String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd,
                                                     int pageNum, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageNum = pageNum + 1;

        Page<ProductionPlan> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<ProductionPlan> wrapper = buildQueryWrapper(productionPlan, keyword,
                createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                planProductionTimeStart, planProductionTimeEnd,
                inspectionStartTimeStart, inspectionStartTimeEnd, inspectionEndTimeStart, inspectionEndTimeEnd,
                outboundTimeStart, outboundTimeEnd, archiveTimeStart, archiveTimeEnd,
                timeFieldType, timeStart, timeEnd);

        Page<ProductionPlan> result = this.page(page, wrapper);

        // 单价为空的计划按制剂销售价内存回填（不落库，仅影响查询返回）
        fillMissingUnitPrice(result.getRecords());
        // 回填计划级入库时间与生产用时（派生字段，不落库）
        fillPlanInboundAndDuration(result.getRecords());

        return result;
    }

    /**
     * 汇总当前筛选条件下的平均生产时间
     * <p>
     * 统计口径与列表查询完全一致（复用同一 QueryWrapper），不受分页影响；
     * 仅统计采购计划下达时间与计划级入库时间均有效、且生产用时非负的计划，
     * 按制剂维度拆分平均生产时间
     * </p>
     *
     * @param productionPlan           查询条件实体
     * @param keyword                  关键字（对计划编号、计划名称进行模糊匹配）
     * @param createdTimeStart         创建时间起始值（含）
     * @param createdTimeEnd           创建时间结束值（含）
     * @param updatedTimeStart         更新时间起始值（含）
     * @param updatedTimeEnd           更新时间结束值（含）
     * @param productionStartTimeStart 生产开始时间起始值（含）
     * @param productionStartTimeEnd   生产开始时间结束值（含）
     * @param productionEndTimeStart   生产结束时间起始值（含）
     * @param productionEndTimeEnd     生产结束时间结束值（含）
     * @param planProductionTimeStart  计划生产时间起始值（含）
     * @param planProductionTimeEnd    计划生产时间结束值（含）
     * @param inspectionStartTimeStart 检验开始时间起始值（含）
     * @param inspectionStartTimeEnd   检验开始时间结束值（含）
     * @param inspectionEndTimeStart   检验结束时间起始值（含）
     * @param inspectionEndTimeEnd     检验结束时间结束值（含）
     * @param outboundTimeStart        出库时间起始值（含）
     * @param outboundTimeEnd          出库时间结束值（含）
     * @param archiveTimeStart         归档时间起始值（含）
     * @param archiveTimeEnd           归档时间结束值（含）
     * @param timeFieldType            动态时间字段类型
     * @param timeStart                动态时间字段起始值（含）
     * @param timeEnd                  动态时间字段结束值（含）
     * @return 平均生产时间汇总
     */
    @Override
    public ProductionDurationSummaryDto getProductionDurationSummary(ProductionPlan productionPlan,
                                                                     String keyword,
                                                                     LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                     LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                     LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                                     LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                                     LocalDateTime planProductionTimeStart, LocalDateTime planProductionTimeEnd,
                                                                     LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                                     LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                                     LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                                     LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                                     String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd) {
        // 复用列表查询的筛选条件，仅查询统计所需字段
        QueryWrapper<ProductionPlan> wrapper = buildQueryWrapper(productionPlan, keyword,
                createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                planProductionTimeStart, planProductionTimeEnd,
                inspectionStartTimeStart, inspectionStartTimeEnd, inspectionEndTimeStart, inspectionEndTimeEnd,
                outboundTimeStart, outboundTimeEnd, archiveTimeStart, archiveTimeEnd,
                timeFieldType, timeStart, timeEnd);
        wrapper.select("id", "preparation_id", "preparation_code", "preparation_name",
                "purchase_order_time", "current_status");

        List<ProductionPlan> plans = this.list(wrapper);
        if (plans.isEmpty()) {
            return new ProductionDurationSummaryDto();
        }

        // 批量加载计划级入库时间（仅所有有效工单均已入库的计划才有值）
        List<Integer> planIds = plans.stream()
                .map(ProductionPlan::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Integer, LocalDateTime> inboundTimeMap = loadPlanInboundTimeMap(planIds);

        // 组装有效生产用时样本
        List<ProductionDurationUtils.DurationSample> samples = new ArrayList<>();
        for (ProductionPlan plan : plans) {
            // 排除已作废计划
            if ("作废".equals(plan.getCurrentStatus())) {
                continue;
            }
            LocalDateTime inboundTime = inboundTimeMap.get(plan.getId());
            BigDecimal durationDays = ProductionDurationUtils.calcDays(plan.getPurchaseOrderTime(), inboundTime);
            if (durationDays != null) {
                samples.add(new ProductionDurationUtils.DurationSample(
                        plan.getPreparationId(),
                        plan.getPreparationCode(),
                        plan.getPreparationName(),
                        durationDays));
            }
        }
        return ProductionDurationUtils.summarize(samples);
    }

    /**
     * 构建生产计划查询条件
     * <p>
     * 列表查询与平均生产时间统计共用，避免两处筛选条件不一致
     * </p>
     *
     * @return 查询条件
     */
    private QueryWrapper<ProductionPlan> buildQueryWrapper(ProductionPlan productionPlan,
                                                           String keyword,
                                                           LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                           LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                           LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                           LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                           LocalDateTime planProductionTimeStart, LocalDateTime planProductionTimeEnd,
                                                           LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                           LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                           LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                           LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                           String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd) {
        QueryWrapper<ProductionPlan> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对计划编号、计划名称进行模糊匹配
            wrapper.and(w -> w.like("plan_number", keyword).or().like("plan_name", keyword));
        }
        if (productionPlan.getId() != null) {
            wrapper.eq("id", productionPlan.getId());
        }
        if (StringUtils.hasText(productionPlan.getPlanNumber())) {
            wrapper.like("plan_number", productionPlan.getPlanNumber());
        }
        if (StringUtils.hasText(productionPlan.getPlanName())) {
            // 按计划名称（关联计划名称）模糊匹配
            wrapper.like("plan_name", productionPlan.getPlanName());
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

        // 计划生产时间范围查询
        if (planProductionTimeStart != null) {
            wrapper.ge("plan_production_time", planProductionTimeStart);
        }
        if (planProductionTimeEnd != null) {
            wrapper.le("plan_production_time", planProductionTimeEnd);
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
                case "PLAN_PRODUCTION_TIME" -> "plan_production_time";
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
        return wrapper;
    }

    /**
     * 批量加载计划级入库时间
     * <p>
     * 取每个计划下所有未删除、非作废工单的入库时间最大值；
     * 只要存在未入库的有效工单，则该计划不返回入库时间
     * </p>
     *
     * @param planIds 计划ID列表
     * @return 计划ID → 入库时间
     */
    private Map<Integer, LocalDateTime> loadPlanInboundTimeMap(Collection<Integer> planIds) {
        Map<Integer, LocalDateTime> result = new HashMap<>();
        if (planIds == null || planIds.isEmpty()) {
            return result;
        }

        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.select("plan_id", "inbound_time", "current_status");
        wrapper.in("plan_id", planIds);
        wrapper.eq("is_deleted", 0);
        // 排除已作废工单，同时保留历史数据中状态为空的工单
        wrapper.and(w -> w.isNull("current_status").or().ne("current_status", "作废"));
        List<WorkOrder> workOrders = workOrderMapper.selectList(wrapper);
        if (workOrders.isEmpty()) {
            return result;
        }

        Map<Long, List<WorkOrder>> workOrdersByPlan = workOrders.stream()
                .filter(wo -> wo.getPlanId() != null)
                .collect(Collectors.groupingBy(WorkOrder::getPlanId));

        for (Integer planId : planIds) {
            List<WorkOrder> planWorkOrders = workOrdersByPlan.get(planId.longValue());
            if (planWorkOrders == null || planWorkOrders.isEmpty()) {
                continue;
            }
            // 仅当该计划下所有有效工单均已入库时，计划才视为已入库
            boolean allInbound = planWorkOrders.stream().allMatch(wo -> wo.getInboundTime() != null);
            if (!allInbound) {
                continue;
            }
            LocalDateTime maxInboundTime = planWorkOrders.stream()
                    .map(WorkOrder::getInboundTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            if (maxInboundTime != null) {
                result.put(planId, maxInboundTime);
            }
        }
        return result;
    }

    /**
     * 回填计划级入库时间与生产用时
     *
     * @param plans 生产计划列表
     */
    private void fillPlanInboundAndDuration(List<ProductionPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        List<Integer> planIds = plans.stream()
                .map(ProductionPlan::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Integer, LocalDateTime> inboundTimeMap = loadPlanInboundTimeMap(planIds);

        for (ProductionPlan plan : plans) {
            LocalDateTime inboundTime = inboundTimeMap.get(plan.getId());
            plan.setInboundTime(inboundTime);
            plan.setProductionDurationDays(
                    ProductionDurationUtils.calcDays(plan.getPurchaseOrderTime(), inboundTime));
        }
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
     * @param keyword                   关键字（对计划编号、计划名称进行模糊匹配）
     * @param createdTimeStart          创建时间起始值（含）
     * @param createdTimeEnd            创建时间结束值（含）
     * @param updatedTimeStart          更新时间起始值（含）
     * @param updatedTimeEnd            更新时间结束值（含）
     * @param productionStartTimeStart  生产开始时间起始值（含）
     * @param productionStartTimeEnd    生产开始时间结束值（含）
     * @param productionEndTimeStart    生产结束时间起始值（含）
     * @param productionEndTimeEnd      生产结束时间结束值（含）
     * @param planProductionTimeStart   计划生产时间起始值（含）
     * @param planProductionTimeEnd     计划生产时间结束值（含）
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
                                                                       String keyword,
                                                                       LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                       LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                       LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                                       LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                                       LocalDateTime planProductionTimeStart, LocalDateTime planProductionTimeEnd,
                                                                       LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                                       LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                                       LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                                       LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                                       String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd,
                                                                       int pageNum, int pageSize) {
        // 查询生产计划主表分页数据
        Page<ProductionPlan> parentPage = queryProductionPlans(productionPlan, keyword, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                productionStartTimeStart, productionStartTimeEnd, productionEndTimeStart, productionEndTimeEnd,
                planProductionTimeStart, planProductionTimeEnd,
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

        // 制剂规格实时派生（生产计划表未存规格，按制剂主数据批量回填）
        fillPreparationSpec(dtos);

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
     *   <li>计划关联了工单，且存在未完成的工单 → 生产中</li>
     *   <li>计划关联的所有工单均为已生产或已归档 → 已完成</li>
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

        // 查询计划
        ProductionPlan plan = this.getById(planId);
        if (plan == null) {
            return;
        }

        // 根据工单状态计算计划状态
        String status;
        if (workOrders.isEmpty()) {
            // 无关联工单 → 待生产
            status = "待生产";
        } else {
            // 计算所有工单的计划数量之和
            java.math.BigDecimal totalBatchQty = workOrders.stream()
                    .map(wo -> wo.getBatchQty() != null ? wo.getBatchQty() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            // 获取计划的批次数量
            java.math.BigDecimal planQuantity = plan.getPlanQuantity() != null
                    ? plan.getPlanQuantity() : java.math.BigDecimal.ZERO;

            // 判断所有工单状态是否都为已检验或之后的状态（已检验、已放行、已入库、已归档）
            boolean allAfterInspection = workOrders.stream().allMatch(wo ->
                    "已检验".equals(wo.getCurrentStatus())
                    || "已放行".equals(wo.getCurrentStatus())
                    || "已入库".equals(wo.getCurrentStatus())
                    || "已归档".equals(wo.getCurrentStatus()));

            // 判断所有工单状态是否都为已归档
            boolean allArchived = workOrders.stream().allMatch(wo ->
                    "已归档".equals(wo.getCurrentStatus()));

            // 已完成：所有工单已检验或之后，且任务计划数量之和 >= 计划批次数量
            if (allAfterInspection && totalBatchQty.compareTo(planQuantity) >= 0) {
                status = "已完成";
            }
            // 已归档：所有工单已归档，且任务计划数量之和 >= 计划批次数量
            else if (allArchived && totalBatchQty.compareTo(planQuantity) >= 0) {
                status = "已归档";
            }
            else {
                status = "生产中";
            }
        }

        // 状态落库
        if (plan != null) {
            plan.setCurrentStatus(status);
            plan.setCurrentStatusDate(LocalDateTime.now());
            // 同步关联工单的实际发生时间到生产计划（只填不倒退，重复刷新幂等）
            syncWorkOrderTimes(plan, workOrders);
            plan.setUpdatedTime(LocalDateTime.now());
            this.updateById(plan);
        }
    }

    /**
     * 将关联工单的实际发生时间汇总同步到生产计划
     * <p>
     * 合并规则：
     * <ul>
     *   <li>生产开始/检验开始：取所有工单对应时间中最早的一个（计划始于首个工单开始）</li>
     *   <li>生产结束/检验结束/出库/归档：取所有工单对应时间中最晚的一个（计划终于末个工单完成）</li>
     * </ul>
     * 以计划当前已有值为初始值参与合并，保证幂等——重复刷新不会倒退已有时间。
     * 生产结束时间优先取工单配置完成时间（configCompleteTime），为空时回退生产完成时间（productionCompleteTime）
     * </p>
     *
     * @param plan       生产计划（会就地修改时间字段，调用方负责落库）
     * @param workOrders 计划关联的未删除工单列表
     */
    private void syncWorkOrderTimes(ProductionPlan plan, List<WorkOrder> workOrders) {
        // 生产开始时间：取最早配置日期（配置日期有值即代表进入生产）
        for (WorkOrder wo : workOrders) {
            if (wo.getConfigDate() != null
                    && (plan.getProductionStartTime() == null
                        || wo.getConfigDate().isBefore(plan.getProductionStartTime()))) {
                plan.setProductionStartTime(wo.getConfigDate());
            }
            // 生产结束时间：取最晚配置完成时间（缺省回退生产完成时间）
            LocalDateTime completeTime = wo.getConfigCompleteTime() != null
                    ? wo.getConfigCompleteTime() : wo.getProductionCompleteTime();
            if (completeTime != null
                    && (plan.getProductionEndTime() == null
                        || completeTime.isAfter(plan.getProductionEndTime()))) {
                plan.setProductionEndTime(completeTime);
            }
            // 检验开始时间：取最早
            if (wo.getInspectionStart() != null
                    && (plan.getInspectionStartTime() == null
                        || wo.getInspectionStart().isBefore(plan.getInspectionStartTime()))) {
                plan.setInspectionStartTime(wo.getInspectionStart());
            }
            // 检验结束时间：取最晚
            if (wo.getInspectionEnd() != null
                    && (plan.getInspectionEndTime() == null
                        || wo.getInspectionEnd().isAfter(plan.getInspectionEndTime()))) {
                plan.setInspectionEndTime(wo.getInspectionEnd());
            }
            // 出库时间：取最晚
            if (wo.getOutboundTime() != null
                    && (plan.getOutboundTime() == null
                        || wo.getOutboundTime().isAfter(plan.getOutboundTime()))) {
                plan.setOutboundTime(wo.getOutboundTime());
            }
            // 归档时间：取最晚
            if (wo.getArchiveTime() != null
                    && (plan.getArchiveTime() == null
                        || wo.getArchiveTime().isAfter(plan.getArchiveTime()))) {
                plan.setArchiveTime(wo.getArchiveTime());
            }
        }
    }

    // endregion

    // region 工单关联查询
    // ===================================
    // 工单关联查询
    // ===================================

    /**
     * 根据生产任务（工单）查询关联的生产计划
     * <p>
     * 按工单ID查询工单，取其关联的计划ID（planId），再查询对应的生产计划并返回。
     * 工单不存在或未关联计划时抛出异常提示
     * </p>
     *
     * @param workOrderId 生产任务（工单）ID（必填）
     * @return 关联的生产计划
     */
    @Override
    public ProductionPlan getPlanByWorkOrder(Long workOrderId) {
        // 校验工单ID不能为空
        if (workOrderId == null) {
            throw new RuntimeException("生产任务ID不能为空");
        }

        // 查询工单，获取其关联的计划ID
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new RuntimeException("生产任务不存在: " + workOrderId);
        }
        if (workOrder.getPlanId() == null) {
            throw new RuntimeException("该生产任务未关联生产计划: " + workOrderId);
        }

        // 按计划ID查询生产计划
        ProductionPlan plan = this.getById(workOrder.getPlanId().intValue());
        if (plan == null) {
            throw new RuntimeException("关联的生产计划不存在: " + workOrder.getPlanId());
        }
        return plan;
    }

    /**
     * 作废生产计划
     * <p>
     * 将生产计划状态设置为"作废"，同时作废所有关联的工单
     * 已完成或已作废的计划不能再次作废
     * </p>
     *
     * @param planId 生产计划ID
     * @return 是否作废成功
     */
    @Override
    public boolean voidProductionPlan(Integer planId) {
        ProductionPlan plan = this.getById(planId);
        if (plan == null) {
            return false;
        }
        // 已完成或已作废的计划不能再次作废
        if ("已完成".equals(plan.getCurrentStatus()) || "作废".equals(plan.getCurrentStatus())) {
            return false;
        }
        
        // 作废所有关联的工单
        QueryWrapper<WorkOrder> workOrderWrapper = new QueryWrapper<>();
        workOrderWrapper.eq("plan_id", planId);
        workOrderWrapper.ne("current_status", "作废");
        List<WorkOrder> workOrders = workOrderMapper.selectList(workOrderWrapper);
        for (WorkOrder wo : workOrders) {
            UpdateWrapper<WorkOrder> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("work_order_id", wo.getWorkOrderId());
            updateWrapper.set("current_status", "作废");
            workOrderMapper.update(null, updateWrapper);
        }
        
        // 作废生产计划
        UpdateWrapper<ProductionPlan> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", planId);
        updateWrapper.set("current_status", "作废");
        return this.update(updateWrapper);
    }

    // endregion

    // region 唯一性校验
    // ===================================
    // 唯一性校验
    // ===================================

    /**
     * 校验计划编号唯一性（业务单号拒绝复用语义）
     * <p>
     * 计数查询绕过全局软删除过滤（含 is_deleted=1 的行），与数据库唯一索引 plan_number 语义一致：
     * 已软删除的计划编号仍占用唯一索引，不可复用
     * </p>
     *
     * @param planNumber 计划编号
     * @param excludeId  排除的计划ID（新增时传null，修改时传当前计划ID）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isPlanNumberUnique(String planNumber, Integer excludeId) {
        Long count = baseMapper.countByPlanNumberIncludeDeleted(planNumber, excludeId);
        return count == null || count == 0;
    }

    /**
     * 自动生成计划编号
     * <p>
     * 规则：Plan + 当天日期(yyyyMMdd) + 4位自增序号（如 Plan202610100001）。
     * 取号查询为原生SQL（selectMaxPlanNumberByPrefix），包含已软删除的记录，
     * 避免复用已删单号导致唯一索引 plan_number 冲突
     * </p>
     *
     * @return 计划编号
     */
    @Override
    public String generatePlanNumber() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "Plan" + dateStr;

        // 原生SQL取号（绕过软删除过滤，能看到已软删除的计划编号）
        String latestNumber = baseMapper.selectMaxPlanNumberByPrefix(prefix);

        int sequence = 1;
        if (latestNumber != null && !latestNumber.isEmpty()) {
            try {
                String sequenceStr = latestNumber.substring(Math.max(0, latestNumber.length() - 4));
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }

    // endregion

    // region 单价回填
    // ===================================
    // 单价回填
    // ===================================

    /**
     * 为单价为空的生产计划回填制剂销售价（仅内存回填，不写回数据库）
     * <p>
     * 场景：部分计划创建时前端未传 unit_price，导致列表查询单价缺失。
     * 回填规则：仅对 unit_price 为 NULL 且关联了制剂（preparation_id 非空）的计划生效，
     * 按制剂档案的销售价（sales_price）回填；已有单价的计划保持原值不变；
     * 制剂已被删除或销售价未维护时保持 NULL
     * </p>
     *
     * @param plans 查询结果中的生产计划列表（可能为null或空，直接返回）
     */
    private void fillMissingUnitPrice(List<ProductionPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }

        // 收集需要回填的制剂ID（单价为空且关联了制剂）
        Set<Long> preparationIds = plans.stream()
                .filter(p -> p.getUnitPrice() == null && p.getPreparationId() != null)
                .map(ProductionPlan::getPreparationId)
                .collect(Collectors.toSet());
        if (preparationIds.isEmpty()) {
            return;
        }

        // 批量查询制剂销售价（跨页去重后最多一次查询）
        List<Preparation> preparations = preparationMapper.selectBatchIds(preparationIds);
        Map<Long, BigDecimal> salesPriceMap = preparations.stream()
                .filter(prep -> prep.getSalesPrice() != null)
                .collect(Collectors.toMap(Preparation::getPreparationId, Preparation::getSalesPrice, (a, b) -> a));

        // 仅回填单价仍为空的计划，避免覆盖查询期间被并发修改的值
        for (ProductionPlan plan : plans) {
            if (plan.getUnitPrice() == null && plan.getPreparationId() != null) {
                plan.setUnitPrice(salesPriceMap.get(plan.getPreparationId()));
            }
        }
    }

    // endregion

    // region 规格回填
    // ===================================
    // 规格回填
    // ===================================

    /**
     * 为生产计划DTO回填制剂规格（实时派生，不写回数据库）
     * <p>
     * 生产计划表未存储规格字段，规格以制剂主数据（preparation.spec）为权威来源。
     * 优先按 preparation_id 关联，未命中时回退按 preparation_code 关联
     * （历史计划可能只存了制剂编码/名称而未存ID），全批次最多两次批量查询，避免 N+1；
     * 制剂已删除或未维护规格时保持为 null。
     * </p>
     *
     * @param dtos 查询结果中的生产计划DTO列表（可能为null或空，直接返回）
     */
    private void fillPreparationSpec(List<ProductionPlanWithRecordsDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        // 收集需要查询规格的制剂ID
        Set<Long> preparationIds = dtos.stream()
                .map(ProductionPlan::getPreparationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        // 收集需要查询规格的制剂编码（ID回退匹配用）
        Set<String> preparationCodes = dtos.stream()
                .map(ProductionPlan::getPreparationCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        // 制剂ID → 规格
        Map<Long, String> specByIdMap = new java.util.HashMap<>();
        if (!preparationIds.isEmpty()) {
            specByIdMap = preparationMapper.selectBatchIds(preparationIds).stream()
                    .filter(prep -> prep.getSpec() != null)
                    .collect(Collectors.toMap(Preparation::getPreparationId, Preparation::getSpec, (a, b) -> a));
        }

        // 制剂编码 → 规格（仅在存在待回填的编码时查询一次）
        Map<String, String> specByCodeMap = new java.util.HashMap<>();
        if (!preparationCodes.isEmpty()) {
            specByCodeMap = preparationMapper.selectList(
                            new QueryWrapper<Preparation>().in("preparation_code", preparationCodes)).stream()
                    .filter(prep -> prep.getSpec() != null && StringUtils.hasText(prep.getPreparationCode()))
                    .collect(Collectors.toMap(Preparation::getPreparationCode, Preparation::getSpec, (a, b) -> a));
        }

        // 回填规格：优先按ID，未命中再按编码
        for (ProductionPlanWithRecordsDto dto : dtos) {
            String spec = dto.getPreparationId() != null ? specByIdMap.get(dto.getPreparationId()) : null;
            if (spec == null && StringUtils.hasText(dto.getPreparationCode())) {
                spec = specByCodeMap.get(dto.getPreparationCode());
            }
            dto.setSpec(spec);
        }
    }

    // endregion
}

package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.Dto.Scheduling.GanttItemDto;
import com.tonghui.erp.Common.Dto.Scheduling.PlanBatchDto;
import com.tonghui.erp.Common.Dto.Scheduling.PlanParticipantDto;
import com.tonghui.erp.Common.Dto.Scheduling.ProcessPlanSaveDto;
import com.tonghui.erp.Common.Dto.Scheduling.ProcessScheduleDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleConflictDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleDetailDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveRequest;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveResult;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleTaskDto;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionTeam;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanParticipant;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMemberMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanParticipantMapper;
import com.tonghui.erp.Service.WorkOrderScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工单排产服务实现类
 */
@Service
public class WorkOrderScheduleServiceImpl implements WorkOrderScheduleService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 工单 Mapper */
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /** 工序模板 Mapper */
    @Autowired
    private PreparationProcessTemplateMapper templateMapper;

    /** 工序类型 Mapper */
    @Autowired
    private ProcessTypeMapper processTypeMapper;

    /** 班组 Mapper */
    @Autowired
    private ProductionTeamMapper teamMapper;

    /** 生产计划 Mapper（计划编号查询） */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    /** 班组成员 Mapper */
    @Autowired
    private ProductionTeamMemberMapper teamMemberMapper;

    /** 排产计划 Mapper */
    @Autowired
    private WorkOrderProcessPlanMapper planMapper;

    /** 排产批次 Mapper */
    @Autowired
    private WorkOrderProcessPlanBatchMapper batchMapper;

    /** 计划参与人 Mapper */
    @Autowired
    private WorkOrderProcessPlanParticipantMapper participantMapper;

    /** 工序执行 Mapper */
    @Autowired
    private WorkOrderProcessExecutionMapper executionMapper;

    /** 房间 Mapper */
    @Autowired
    private RoomInfoMapper roomInfoMapper;

    /** 设备 Mapper */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /** 计量单位 Mapper */
    @Autowired
    private UnitMapper unitMapper;

    /** 冲突校验服务 */
    @Autowired
    private ScheduleConflictService conflictService;

    // endregion

    // region 任务列表
    // ===================================
    // 任务列表
    // ===================================

    /**
     * 查询未完成工单的排产任务列表
     *
     * @param keyword    工单编号/名称、制剂编码/名称、计划名称/编号、批号关键字
     * @param status     生产状态：待生产/生产中/已完工
     * @param configFrom 配置日期起
     * @param configTo   配置日期止
     * @return 任务列表
     */
    @Override
    public List<ScheduleTaskDto> listTasks(String keyword, String status,
                                           LocalDate configFrom, LocalDate configTo) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getIsDeleted, 0)
                .notIn(WorkOrder::getCurrentStatus, "已入库", "已放行", "已归档");
        if (StringUtils.hasText(keyword)) {
            // 计划编号在 production_plan 表，先按编号匹配计划ID
            final List<Integer> matchedPlanIds = productionPlanMapper.selectList(
                            new LambdaQueryWrapper<ProductionPlan>()
                                    .like(ProductionPlan::getPlanNumber, keyword))
                    .stream().map(ProductionPlan::getId).toList();
            wrapper.and(q -> {
                q.like(WorkOrder::getWorkOrderCode, keyword)
                        .or().like(WorkOrder::getWorkOrderName, keyword)
                        .or().like(WorkOrder::getPreparationCode, keyword)
                        .or().like(WorkOrder::getPreparationName, keyword)
                        .or().like(WorkOrder::getPlanName, keyword)
                        .or().like(WorkOrder::getBatchNumber, keyword);
                if (!matchedPlanIds.isEmpty()) {
                    q.or().in(WorkOrder::getPlanId, matchedPlanIds);
                }
            });
        }
        if (configFrom != null) {
            wrapper.ge(WorkOrder::getConfigDate, configFrom.atStartOfDay());
        }
        if (configTo != null) {
            wrapper.le(WorkOrder::getConfigDate, configTo.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(WorkOrder::getPlanStartTime).orderByDesc(WorkOrder::getWorkOrderId);
        List<WorkOrder> orders = workOrderMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(orders)) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(WorkOrder::getWorkOrderId).toList();
        Map<Long, String> planNumberMap = new HashMap<>();
        List<Integer> planIds = orders.stream().map(WorkOrder::getPlanId)
                .filter(Objects::nonNull).distinct().map(Long::intValue).toList();
        if (!planIds.isEmpty()) {
            productionPlanMapper.selectBatchIds(planIds)
                    .forEach(plan -> planNumberMap.put(plan.getId().longValue(), plan.getPlanNumber()));
        }
        Map<Long, List<WorkOrderProcessExecution>> execMap = executionMapper.selectList(
                        new LambdaQueryWrapper<WorkOrderProcessExecution>()
                                .in(WorkOrderProcessExecution::getWorkOrderId, orderIds)
                                .eq(WorkOrderProcessExecution::getIsDeleted, 0))
                .stream().collect(Collectors.groupingBy(WorkOrderProcessExecution::getWorkOrderId));
        List<ScheduleTaskDto> result = new ArrayList<>();
        for (WorkOrder order : orders) {
            ScheduleTaskDto dto = new ScheduleTaskDto();
            dto.setWorkOrderId(order.getWorkOrderId());
            dto.setWorkOrderCode(order.getWorkOrderCode());
            dto.setWorkOrderName(order.getWorkOrderName());
            dto.setPreparationName(order.getPreparationName());
            dto.setPlanId(order.getPlanId());
            dto.setPlanName(order.getPlanName());
            dto.setPlanNumber(order.getPlanId() == null ? null : planNumberMap.get(order.getPlanId()));
            dto.setBatchNumber(order.getBatchNumber());
            dto.setBatchQty(order.getBatchQty());
            dto.setCurrentStatus(order.getCurrentStatus());
            dto.setConfigDate(order.getConfigDate());
            dto.setPlanStartTime(order.getPlanStartTime());
            List<WorkOrderProcessPlan> plans = planMapper.selectByWorkOrderIdWithDetails(order.getWorkOrderId());
            int processCount = plans.size();
            if (processCount == 0 && order.getPreparationId() != null) {
                processCount = Math.toIntExact(templateMapper.selectCount(
                        new LambdaQueryWrapper<PreparationProcessTemplate>()
                                .eq(PreparationProcessTemplate::getPreparationId, order.getPreparationId())));
            }
            dto.setProcessCount(processCount);
            dto.setScheduledProcessCount((int) plans.stream()
                    .filter(p -> p.getScheduled() != null && p.getScheduled() == 1)
                    .count());
            dto.setConflictCount(conflictCount(order.getWorkOrderId(), plans));
            dto.setSaved(plans.stream().anyMatch(p -> "已保存".equals(p.getStatus())));
            List<WorkOrderProcessExecution> executions =
                    execMap.getOrDefault(order.getWorkOrderId(), List.of());
            String productionStatus = productionStatus(executions, processCount);
            dto.setProductionStatus(productionStatus);
            dto.setProductionDurationHours(productionHours(order, executions));
            if (!StringUtils.hasText(status) || status.equals(productionStatus)) {
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * 按执行记录推导生产状态
     */
    private String productionStatus(List<WorkOrderProcessExecution> executions, int processCount) {
        if (CollectionUtils.isEmpty(executions)) {
            return "待生产";
        }
        Set<Integer> started = new HashSet<>();
        Set<Integer> completed = new HashSet<>();
        for (WorkOrderProcessExecution execution : executions) {
            if (execution.getStepOrder() == null) {
                continue;
            }
            if (execution.getStartTime() != null) {
                started.add(execution.getStepOrder());
            }
            if (execution.getEndTime() != null
                    || ProcessExecutionStatus.COMPLETED.equals(execution.getStatus())) {
                completed.add(execution.getStepOrder());
            }
        }
        if (processCount > 0 && completed.size() >= processCount) {
            return "已完工";
        }
        return started.isEmpty() ? "待生产" : "生产中";
    }

    /**
     * 生产用时：优先取工单上已统计的时长，否则按执行记录起止累加
     */
    private BigDecimal productionHours(WorkOrder order, List<WorkOrderProcessExecution> executions) {
        if (order.getProductionDurationHours() != null) {
            return order.getProductionDurationHours();
        }
        long minutes = executions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .mapToLong(e -> Math.max(Duration.between(e.getStartTime(), e.getEndTime()).toMinutes(), 0))
                .sum();
        return minutes > 0
                ? BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                : null;
    }

    /**
     * 统计工单当前冲突数
     */
    private int conflictCount(Long workOrderId, List<WorkOrderProcessPlan> plans) {
        if (CollectionUtils.isEmpty(plans)) {
            return 0;
        }
        List<Long> ids = plans.stream().map(WorkOrderProcessPlan::getId).toList();
        List<WorkOrderProcessPlanBatch> batches = batchMapper.selectByPlanIds(ids);
        return conflictService.check(workOrderId, new ArrayList<>(plans), batches).size();
    }

    // endregion

    // region 详情
    // ===================================
    // 详情
    // ===================================

    /**
     * 查询单个工单的排产详情
     *
     * @param workOrderId 工单ID
     * @return 排产详情
     */
    @Override
    public ScheduleDetailDto getDetail(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new IllegalArgumentException("工单不存在");
        }
        ScheduleDetailDto detail = new ScheduleDetailDto();
        detail.setWorkOrderId(order.getWorkOrderId());
        detail.setWorkOrderCode(order.getWorkOrderCode());
        detail.setWorkOrderName(order.getWorkOrderName());
        detail.setPreparationName(order.getPreparationName());
        detail.setPlanId(order.getPlanId());
        detail.setPlanName(order.getPlanName());
        detail.setPlanNumber(planNumberOf(order.getPlanId()));
        detail.setBatchNumber(order.getBatchNumber());
        detail.setBatchQty(order.getBatchQty());
        detail.setCurrentStatus(order.getCurrentStatus());
        detail.setConfigDate(order.getConfigDate());
        detail.setPlanStartTime(order.getPlanStartTime());

        List<WorkOrderProcessPlan> plans = planMapper.selectByWorkOrderIdWithDetails(workOrderId);
        List<ProcessScheduleDto> processes = new ArrayList<>();
        List<ScheduleConflictDto> conflicts = new ArrayList<>();
        Map<Integer, List<WorkOrderProcessExecution>> executionMap = executionMapper
                .selectByWorkOrderIdWithDetails(workOrderId).stream()
                .filter(e -> e.getStepOrder() != null)
                .collect(Collectors.groupingBy(WorkOrderProcessExecution::getStepOrder));
        if (CollectionUtils.isEmpty(plans)) {
            List<ProcessScheduleDto> templates = buildTemplateProcesses(order);
            templates.forEach(dto -> applyActual(dto, executionMap.getOrDefault(dto.getStepOrder(), List.of())));
            processes.addAll(templates);
        } else {
            List<Long> planIds = plans.stream().map(WorkOrderProcessPlan::getId).toList();
            Map<Long, List<WorkOrderProcessPlanBatch>> batchMap = batchMapper.selectByPlanIds(planIds).stream()
                    .collect(Collectors.groupingBy(WorkOrderProcessPlanBatch::getPlanId));
            Map<Long, List<WorkOrderProcessPlanParticipant>> participantMap =
                    participantMapper.selectByPlanIdsWithPersonnel(planIds).stream()
                            .collect(Collectors.groupingBy(WorkOrderProcessPlanParticipant::getPlanId));
            Map<Long, Unit> unitMap = loadUnits(java.util.stream.Stream.concat(
                            plans.stream().map(WorkOrderProcessPlan::getUnitId),
                            batchMap.values().stream().flatMap(List::stream)
                                    .map(WorkOrderProcessPlanBatch::getUnitId))
                    .toList());
            conflicts = conflictService.check(workOrderId, new ArrayList<>(plans),
                    batchMap.values().stream().flatMap(List::stream).toList());
            Map<Long, List<ScheduleConflictDto>> conflictMap = conflicts.stream()
                    .collect(Collectors.groupingBy(ScheduleConflictDto::getPlanId));
            for (WorkOrderProcessPlan plan : plans) {
                ProcessScheduleDto dto = toProcessDto(plan, batchMap.get(plan.getId()),
                        participantMap.get(plan.getId()),
                        executionMap.getOrDefault(plan.getStepOrder(), List.of()), unitMap);
                dto.setConflicts(conflictMap.getOrDefault(plan.getId(), List.of()));
                processes.add(dto);
            }
        }
        processes.sort(Comparator.comparing(ProcessScheduleDto::getStepOrder,
                Comparator.nullsLast(Integer::compareTo)));
        List<WorkOrderProcessExecution> allExecutions = executionMap.values().stream()
                .flatMap(List::stream).toList();
        detail.setProductionStatus(productionStatus(allExecutions, processes.size()));
        detail.setProcesses(processes);
        detail.setConflicts(conflicts);
        return detail;
    }

    /**
     * 按模板生成未落库的工序计划
     */
    private List<ProcessScheduleDto> buildTemplateProcesses(WorkOrder order) {
        List<PreparationProcessTemplate> templates = loadTemplates(order.getPreparationId());
        Map<Long, Unit> unitMap = loadUnits(templates.stream()
                .map(PreparationProcessTemplate::getUnitId).toList());
        List<ProcessScheduleDto> result = new ArrayList<>();
        for (PreparationProcessTemplate template : templates) {
            ProcessScheduleDto dto = new ProcessScheduleDto();
            dto.setWorkOrderId(order.getWorkOrderId());
            dto.setProcessTypeId(template.getProcessTypeId());
            dto.setStepOrder(template.getStepOrder());
            dto.setTemplateId(template.getTemplateId());
            dto.setProcessName(processName(template.getProcessTypeId(), template.getProcessTypeName()));
            dto.setStandardDuration(template.getStandardDuration());
            dto.setStandardQty(template.getStandardQty());
            dto.setPlanQty(order.getBatchQty());
            fillUnit(dto, template.getUnitId(), unitMap);
            dto.setRoomId(template.getRoomId());
            dto.setRoomName(template.getRoomId() == null ? null : roomName(template.getRoomId()));
            dto.setEquipmentId(template.getEquipmentId());
            dto.setEquipmentName(template.getEquipmentId() == null ? null : equipmentName(template.getEquipmentId()));
            dto.setStatus("草稿");
            dto.setScheduled(false);
            result.add(dto);
        }
        return result;
    }

    /**
     * 把实际执行起止、数量与状态写入工序 DTO
     * <p>拆分批次时汇总多条实际记录：最早开始、最晚结束、数量合计</p>
     */
    private void applyActual(ProcessScheduleDto dto, List<WorkOrderProcessExecution> executions) {
        if (CollectionUtils.isEmpty(executions)) {
            return;
        }
        dto.setExecutionId(executions.size() == 1 ? executions.get(0).getId() : null);
        dto.setActualStartTime(executions.stream().map(WorkOrderProcessExecution::getStartTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null));
        dto.setActualEndTime(executions.stream().map(WorkOrderProcessExecution::getEndTime)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
        dto.setActualQty(executions.stream().map(WorkOrderProcessExecution::getProcessQty)
                .filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null));
        dto.setActualStatus(resolveActualStatus(executions));
    }

    /**
     * 汇总多条实际记录的状态
     */
    private String resolveActualStatus(List<WorkOrderProcessExecution> executions) {
        if (executions.stream().anyMatch(e -> ProcessExecutionStatus.IN_PROGRESS.equals(e.getStatus()))) {
            return ProcessExecutionStatus.IN_PROGRESS;
        }
        if (executions.stream().allMatch(e -> ProcessExecutionStatus.COMPLETED.equals(e.getStatus()))) {
            return ProcessExecutionStatus.COMPLETED;
        }
        return executions.stream().anyMatch(e -> e.getStartTime() != null)
                ? ProcessExecutionStatus.IN_PROGRESS : ProcessExecutionStatus.PENDING;
    }

    /**
     * 计划实体转 DTO
     */
    private ProcessScheduleDto toProcessDto(WorkOrderProcessPlan plan,
                                            List<WorkOrderProcessPlanBatch> batches,
                                            List<WorkOrderProcessPlanParticipant> participants,
                                            List<WorkOrderProcessExecution> executions,
                                            Map<Long, Unit> unitMap) {
        ProcessScheduleDto dto = new ProcessScheduleDto();
        dto.setPlanId(plan.getId());
        dto.setWorkOrderId(plan.getWorkOrderId());
        dto.setProcessTypeId(plan.getProcessTypeId());
        dto.setStepOrder(plan.getStepOrder());
        dto.setTemplateId(plan.getTemplateId());
        dto.setProcessName(plan.getProcessTypeName() != null ? plan.getProcessTypeName() : processName(plan.getProcessTypeId(), null));
        dto.setPlanStartTime(plan.getPlanStartTime());
        dto.setPlanEndTime(plan.getPlanEndTime());
        dto.setPlanDurationMinutes(plan.getPlanDurationMinutes());
        dto.setPlanQty(plan.getPlanQty());
        fillUnit(dto, plan.getUnitId(), unitMap);
        dto.setRoomId(plan.getRoomId());
        dto.setRoomName(plan.getRoomName());
        dto.setEquipmentId(plan.getEquipmentId());
        dto.setEquipmentName(plan.getEquipmentName());
        dto.setTeamId(plan.getTeamId());
        dto.setTeamName(plan.getTeamName());
        dto.setStatus(plan.getStatus());
        // 是否已排产以保存时前端传入的值为准，不再按计划起止时间推算
        dto.setScheduled(plan.getScheduled() != null && plan.getScheduled() == 1);
        dto.setRemark(plan.getRemark());
        dto.setBatches(toBatchDtos(batches, participants, executions, unitMap));
        dto.setParticipants(toParticipantDtos(participants, null));
        applyActual(dto, executions);
        return dto;
    }

    private List<PlanBatchDto> toBatchDtos(List<WorkOrderProcessPlanBatch> batches,
                                           List<WorkOrderProcessPlanParticipant> participants,
                                           List<WorkOrderProcessExecution> executions,
                                           Map<Long, Unit> unitMap) {
        if (CollectionUtils.isEmpty(batches)) {
            return List.of();
        }
        return batches.stream().sorted(Comparator.comparing(WorkOrderProcessPlanBatch::getBatchNo))
                .map(b -> {
                    PlanBatchDto dto = new PlanBatchDto();
                    dto.setId(b.getId());
                    dto.setBatchNo(b.getBatchNo());
                    dto.setBatchName(b.getBatchName());
                    dto.setPlanStartTime(b.getPlanStartTime());
                    dto.setPlanEndTime(b.getPlanEndTime());
                    dto.setPlanDurationMinutes(b.getPlanDurationMinutes());
                    dto.setPlanQty(b.getPlanQty());
                    fillUnit(dto, b.getUnitId(), unitMap);
                    dto.setRoomId(b.getRoomId());
                    dto.setRoomName(b.getRoomName());
                    dto.setEquipmentId(b.getEquipmentId());
                    dto.setEquipmentName(b.getEquipmentName());
                    dto.setTeamId(b.getTeamId());
                    dto.setTeamName(b.getTeamName());
                    dto.setStatus(b.getStatus());
                    // 批次是否已排产同样以保存时前端传入的值为准
                    dto.setScheduled(b.getScheduled() != null && b.getScheduled() == 1);
                    dto.setParticipants(toParticipantDtos(participants, b.getId()));
                    executions.stream()
                            .filter(e -> Objects.equals(e.getPlanBatchId(), b.getId())
                                    || (e.getPlanBatchId() == null && Objects.equals(e.getBatchNo(), b.getBatchNo())))
                            .findFirst()
                            .ifPresent(e -> {
                                dto.setExecutionId(e.getId());
                                dto.setActualStartTime(e.getStartTime());
                                dto.setActualEndTime(e.getEndTime());
                                dto.setActualStatus(e.getStatus());
                                dto.setActualQty(e.getProcessQty());
                                dto.setLocked(e.getLocked());
                            });
                    return dto;
                }).toList();
    }

    private List<PlanParticipantDto> toParticipantDtos(List<WorkOrderProcessPlanParticipant> participants,
                                                       Long batchId) {
        if (CollectionUtils.isEmpty(participants)) {
            return List.of();
        }
        return participants.stream()
                .filter(p -> batchId == null ? p.getBatchId() == null : Objects.equals(batchId, p.getBatchId()))
                .map(p -> {
                    PlanParticipantDto dto = new PlanParticipantDto();
                    dto.setPersonnelId(p.getPersonnelId());
                    dto.setPersonnelName(p.getPersonnelName());
                    dto.setIsPrimary(p.getIsPrimary());
                    return dto;
                }).toList();
    }

    // endregion

    // region 自动顺排
    // ===================================
    // 自动顺排
    // ===================================

    /**
     * 按工序模板标准工时自动顺排（保存为草稿）
     *
     * @param workOrderId 工单ID
     * @return 排产详情
     */
    @Override
    public ScheduleDetailDto autoPlan(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        if (order.getPlanStartTime() == null) {
            throw new IllegalArgumentException("工单未设置计划开始时间，无法自动顺排");
        }
        List<PreparationProcessTemplate> templates = loadTemplates(order.getPreparationId());
        if (CollectionUtils.isEmpty(templates)) {
            throw new IllegalArgumentException("该制剂未配置工序模板，无法自动顺排");
        }
        List<ProcessPlanSaveDto> processDtos = new ArrayList<>();
        LocalDateTime cursor = order.getPlanStartTime();
        for (PreparationProcessTemplate template : templates) {
            if (template.getStandardDuration() == null || template.getStandardDuration() <= 0) {
                throw new IllegalArgumentException("工序【"
                        + processName(template.getProcessTypeId(), template.getProcessTypeName())
                        + "】未维护标准工时，无法自动顺排");
            }
            ProcessPlanSaveDto dto = new ProcessPlanSaveDto();
            dto.setProcessTypeId(template.getProcessTypeId());
            dto.setStepOrder(template.getStepOrder());
            dto.setTemplateId(template.getTemplateId());
            dto.setPlanStartTime(cursor);
            dto.setPlanEndTime(cursor.plusMinutes(template.getStandardDuration()));
            dto.setPlanQty(order.getBatchQty());
            dto.setRoomId(template.getRoomId());
            dto.setEquipmentId(template.getEquipmentId());
            dto.setUnitId(template.getUnitId());
            // 自动顺排已为各工序安排计划时间，按已排产保存
            dto.setScheduled(true);
            processDtos.add(dto);
            cursor = cursor.plusMinutes(template.getStandardDuration());
        }
        ScheduleSaveRequest request = new ScheduleSaveRequest();
        request.setWorkOrderId(workOrderId);
        request.setProcesses(processDtos);
        save(workOrderId, request, false);
        return getDetail(workOrderId);
    }

    // endregion

    // region 保存
    // ===================================
    // 保存
    // ===================================

    /**
     * 保存排产
     *
     * @param workOrderId 工单ID
     * @param request     排产请求
     * @param formal      是否正式保存
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleSaveResult save(Long workOrderId, ScheduleSaveRequest request, boolean formal) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        if (request == null || CollectionUtils.isEmpty(request.getProcesses())) {
            throw new IllegalArgumentException("排产工序不能为空");
        }
        List<ProcessPlanSaveDto> dtos = request.getProcesses().stream()
                .sorted(Comparator.comparing(ProcessPlanSaveDto::getStepOrder,
                        Comparator.nullsLast(Integer::compareTo))).toList();
        Set<Integer> steps = new HashSet<>();
        long synthetic = -1;
        List<WorkOrderProcessPlan> plans = new ArrayList<>();
        Map<Long, List<WorkOrderProcessPlanBatch>> batchMap = new LinkedHashMap<>();
        Map<Long, List<WorkOrderProcessPlanParticipant>> participantMap = new LinkedHashMap<>();
        for (ProcessPlanSaveDto dto : dtos) {
            if (dto.getStepOrder() == null) {
                throw new IllegalArgumentException("工序顺序不能为空");
            }
            if (!steps.add(dto.getStepOrder())) {
                throw new IllegalArgumentException("工序顺序重复：" + dto.getStepOrder());
            }
            validateTimes(dto.getPlanStartTime(), dto.getPlanEndTime(), "工序 " + dto.getStepOrder());
            WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
            plan.setId(dto.getPlanId() != null ? dto.getPlanId() : synthetic--);
            plan.setWorkOrderId(workOrderId);
            plan.setProcessTypeId(dto.getProcessTypeId());
            plan.setTemplateId(dto.getTemplateId());
            plan.setStepOrder(dto.getStepOrder());
            // 计量单位：优先取前端传值，未传时按工序模板补齐
            plan.setUnitId(dto.getUnitId());
            if (plan.getUnitId() == null && dto.getTemplateId() != null) {
                PreparationProcessTemplate template = templateMapper.selectById(dto.getTemplateId());
                if (template != null) {
                    plan.setUnitId(template.getUnitId());
                }
            }
            plan.setPlanStartTime(dto.getPlanStartTime());
            plan.setPlanEndTime(dto.getPlanEndTime());
            plan.setPlanDurationMinutes(dto.getPlanStartTime() != null && dto.getPlanEndTime() != null
                    ? Math.toIntExact(Duration.between(dto.getPlanStartTime(), dto.getPlanEndTime()).toMinutes()) : null);
            plan.setPlanQty(dto.getPlanQty() != null ? dto.getPlanQty() : order.getBatchQty());
            plan.setRoomId(dto.getRoomId());
            plan.setEquipmentId(dto.getEquipmentId());
            plan.setTeamId(dto.getTeamId());
            plan.setRemark(dto.getRemark());
            // 是否已排产由前端传参决定，后端不按计划起止时间推算
            plan.setScheduled(dto.getScheduled() == null ? null : (dto.getScheduled() ? 1 : 0));
            plan.setStatus("草稿");
            plan.setWorkOrderCode(order.getWorkOrderCode());
            plan.setProcessTypeName(processName(dto.getProcessTypeId(), null));
            plan.setRoomName(dto.getRoomId() == null ? null : roomName(dto.getRoomId()));
            plan.setEquipmentName(dto.getEquipmentId() == null ? null : equipmentName(dto.getEquipmentId()));
            if (dto.getTeamId() != null) {
                ProductionTeam team = teamMapper.selectById(dto.getTeamId());
                plan.setTeamName(team == null ? null : team.getTeamName());
            }
            if (plan.getPlanQty() == null || plan.getPlanQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("工序 " + plan.getStepOrder() + " 计划数量必须大于 0");
            }
            plans.add(plan);
            List<WorkOrderProcessPlanBatch> batches = buildBatches(plan, dto.getBatches());
            batchMap.put(plan.getId(), batches);
            List<WorkOrderProcessPlanParticipant> participants = buildParticipants(plan, dto.getParticipants());
            participantMap.put(plan.getId(), participants);
            // 排产为可选参考：计划起止为空即未排产，正式保存同样允许，不影响任务流程
        }

        WorkOrderProcessPlan first = plans.get(0);
        if (order.getPlanStartTime() != null && first.getPlanStartTime() != null
                && first.getPlanStartTime().isBefore(order.getPlanStartTime())) {
            throw new IllegalArgumentException("首道工序计划开始不得早于任务计划开始时间");
        }
        List<WorkOrderProcessPlanBatch> allBatches = batchMap.values().stream().flatMap(List::stream).toList();
        if (formal) {
            conflictService.lockResources(plans, allBatches);
        }
        List<ScheduleConflictDto> conflicts = conflictService.check(workOrderId, plans, allBatches,
                participantMap.values().stream().flatMap(List::stream).toList());
        if (formal && !conflicts.isEmpty()) {
            ScheduleSaveResult result = new ScheduleSaveResult();
            result.setSaved(false);
            result.setConflicts(conflicts);
            result.setProcesses(getDetail(workOrderId).getProcesses());
            return result;
        }
        persist(workOrderId, plans, batchMap, participantMap, formal ? "已保存" : "草稿");
        ScheduleSaveResult result = new ScheduleSaveResult();
        result.setSaved(true);
        result.setConflicts(conflicts);
        result.setProcesses(getDetail(workOrderId).getProcesses());
        return result;
    }

    /**
     * 拆分工序批次
     *
     * @param planId  计划行ID
     * @param batches 批次
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleSaveResult split(Long planId, List<PlanBatchDto> batches) {
        WorkOrderProcessPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("排产计划不存在");
        }
        if (CollectionUtils.isEmpty(batches)) {
            throw new IllegalArgumentException("批次不能为空");
        }
        Set<Integer> batchNos = new HashSet<>();
        for (PlanBatchDto dto : batches) {
            if (dto.getBatchNo() == null || !batchNos.add(dto.getBatchNo())) {
                throw new IllegalArgumentException("批次序号不能为空且不能重复");
            }
            validateTimes(dto.getPlanStartTime(), dto.getPlanEndTime(), "批次 " + dto.getBatchNo());
            if (dto.getPlanQty() == null || dto.getPlanQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("批次计划数量必须大于 0");
            }
        }
        // 批次数量合计不强制等于工序计划数量：排产计划数量为排产时手工填写，
        // 实际拆分可按生产安排调整，允许合计大于或小于计划数量
        if (plan.getPlanQty() != null && BigDecimal.ZERO.compareTo(plan.getPlanQty()) >= 0) {
            throw new IllegalArgumentException("工序计划数量必须大于 0");
        }
        List<WorkOrderProcessPlanBatch> entities = new ArrayList<>();
        for (PlanBatchDto dto : batches) {
            WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
            batch.setPlanId(planId);
            batch.setBatchNo(dto.getBatchNo());
            batch.setBatchName(dto.getBatchName());
            batch.setPlanStartTime(dto.getPlanStartTime());
            batch.setPlanEndTime(dto.getPlanEndTime());
            batch.setPlanDurationMinutes(dto.getPlanStartTime() != null && dto.getPlanEndTime() != null
                    ? Math.toIntExact(Duration.between(dto.getPlanStartTime(), dto.getPlanEndTime()).toMinutes()) : null);
            batch.setPlanQty(dto.getPlanQty());
            // 计量单位：优先取前端传值，未传时继承所属工序计划
            batch.setUnitId(dto.getUnitId() != null ? dto.getUnitId() : plan.getUnitId());
            batch.setRoomId(dto.getRoomId() != null ? dto.getRoomId() : plan.getRoomId());
            batch.setEquipmentId(dto.getEquipmentId() != null ? dto.getEquipmentId() : plan.getEquipmentId());
            batch.setTeamId(dto.getTeamId() != null ? dto.getTeamId() : plan.getTeamId());
            // 批次排产标识由前端传参，未传时沿用所属工序计划的值
            batch.setScheduled(dto.getScheduled() != null
                    ? Integer.valueOf(dto.getScheduled() ? 1 : 0) : plan.getScheduled());
            batch.setStatus("计划中");
            if (dto.getParticipants() != null) {
                batch.setParticipants(dto.getParticipants().stream()
                        .filter(x -> x.getPersonnelId() != null)
                        .map(x -> toParticipant(x, planId, null))
                        .toList());
            }
            entities.add(batch);
        }
        plan.setWorkOrderCode(workOrderCode(plan.getWorkOrderId()));
        plan.setProcessTypeName(processName(plan.getProcessTypeId(), null));
        List<WorkOrderProcessPlan> one = List.of(plan);
        List<ScheduleConflictDto> conflicts = conflictService.check(plan.getWorkOrderId(), one, entities);
        if (!conflicts.isEmpty()) {
            ScheduleSaveResult result = new ScheduleSaveResult();
            result.setSaved(false);
            result.setConflicts(conflicts);
            return result;
        }
        batchMapper.physicalDeleteByPlanId(planId);
        participantMapper.physicalDeleteBatchParticipants(planId);
        for (WorkOrderProcessPlanBatch batch : entities) {
            batch.setId(null);
            batch.setIsDeleted(0);
            batch.setVersion(0);
            batchMapper.insert(batch);
            if (batch.getParticipants() != null) {
                for (WorkOrderProcessPlanParticipant participant : batch.getParticipants()) {
                    participant.setId(null);
                    participant.setPlanId(planId);
                    participant.setBatchId(batch.getId());
                    participant.setIsDeleted(0);
                    participant.setVersion(0);
                    participantMapper.insert(participant);
                }
            }
        }
        // 工序计划时间同步为批次的最早开始与最晚结束
        plan.setPlanStartTime(entities.stream().map(WorkOrderProcessPlanBatch::getPlanStartTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(plan.getPlanStartTime()));
        plan.setPlanEndTime(entities.stream().map(WorkOrderProcessPlanBatch::getPlanEndTime)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(plan.getPlanEndTime()));
        planMapper.updateById(plan);
        ScheduleSaveResult result = new ScheduleSaveResult();
        result.setSaved(true);
        result.setConflicts(List.of());
        result.setProcesses(getDetail(plan.getWorkOrderId()).getProcesses());
        return result;
    }

    /**
     * 持久化计划、批次与参与人
     * <p>
     * 以“工序顺序”为键合并：本次提交的工序更新或新增，未提交的工序保持原样。
     * 排产页面调整或拆分某一道工序时只回传该工序，不能据此删除其他工序的计划、批次与参与人。
     * </p>
     */
    private void persist(Long workOrderId, List<WorkOrderProcessPlan> plans,
                         Map<Long, List<WorkOrderProcessPlanBatch>> batchMap,
                         Map<Long, List<WorkOrderProcessPlanParticipant>> participantMap,
                         String status) {
        List<WorkOrderProcessPlan> existing = planMapper.selectList(
                new LambdaQueryWrapper<WorkOrderProcessPlan>()
                        .eq(WorkOrderProcessPlan::getWorkOrderId, workOrderId));
        Map<Integer, WorkOrderProcessPlan> existingByStep = existing.stream()
                .collect(Collectors.toMap(WorkOrderProcessPlan::getStepOrder, Function.identity(), (a, b) -> a));
        for (WorkOrderProcessPlan plan : plans) {
            Long childKey = plan.getId();
            WorkOrderProcessPlan old = existingByStep.get(plan.getStepOrder());
            if (old != null) {
                plan.setId(old.getId());
                // 前端未传 scheduled 时沿用原值，避免空值覆盖已保存的排产标识
                if (plan.getScheduled() == null) {
                    plan.setScheduled(old.getScheduled());
                }
                // 前端未传计量单位时沿用原值
                if (plan.getUnitId() == null) {
                    plan.setUnitId(old.getUnitId());
                }
                planMapper.updateById(plan);
                // 显式更新可能为空的字段，避免 MyBatis-Plus 忽略 null
                planMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WorkOrderProcessPlan>()
                        .eq(WorkOrderProcessPlan::getId, old.getId())
                        .set(WorkOrderProcessPlan::getPlanStartTime, plan.getPlanStartTime())
                        .set(WorkOrderProcessPlan::getPlanEndTime, plan.getPlanEndTime())
                        .set(WorkOrderProcessPlan::getPlanDurationMinutes, plan.getPlanDurationMinutes())
                        .set(WorkOrderProcessPlan::getPlanQty, plan.getPlanQty())
                        .set(WorkOrderProcessPlan::getRoomId, plan.getRoomId())
                        .set(WorkOrderProcessPlan::getEquipmentId, plan.getEquipmentId())
                        .set(WorkOrderProcessPlan::getTeamId, plan.getTeamId())
                        .set(WorkOrderProcessPlan::getRemark, plan.getRemark())
                        .set(WorkOrderProcessPlan::getStatus, status));
            } else {
                plan.setId(null);
                plan.setIsDeleted(0);
                plan.setVersion(0);
                plan.setStatus(status);
                planMapper.insert(plan);
            }
            saveChildren(plan, batchMap.get(childKey), participantMap.get(childKey));
        }
    }

    /**
     * 保存计划的批次与参与人
     */
    private void saveChildren(WorkOrderProcessPlan plan,
                              List<WorkOrderProcessPlanBatch> batches,
                              List<WorkOrderProcessPlanParticipant> participants) {
        batchMapper.physicalDeleteByPlanId(plan.getId());
        participantMapper.physicalDeleteByPlanId(plan.getId());
        if (batches != null) {
            for (WorkOrderProcessPlanBatch batch : batches) {
                batch.setId(null);
                batch.setPlanId(plan.getId());
                batch.setIsDeleted(0);
                batch.setVersion(0);
                if (batch.getTeamId() == null) {
                    batch.setTeamId(plan.getTeamId());
                }
                batchMapper.insert(batch);
                if (batch.getParticipants() != null) {
                    for (WorkOrderProcessPlanParticipant participant : batch.getParticipants()) {
                        participant.setId(null);
                        participant.setPlanId(plan.getId());
                        participant.setBatchId(batch.getId());
                        participant.setIsDeleted(0);
                        participant.setVersion(0);
                        participantMapper.insert(participant);
                    }
                }
            }
        }
        if (participants != null) {
            for (WorkOrderProcessPlanParticipant participant : participants) {
                participant.setId(null);
                participant.setPlanId(plan.getId());
                participant.setIsDeleted(0);
                participant.setVersion(0);
                participantMapper.insert(participant);
            }
        }
    }

    // endregion

    // region 甘特图
    // ===================================
    // 甘特图
    // ===================================

    /**
     * 查询甘特图数据
     *
     * @param view 视图：task/equipment/room
     * @param from 开始时间
     * @param to   结束时间
     * @return 甘特条目
     */
    @Override
    public List<GanttItemDto> gantt(String view, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<WorkOrderProcessPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderProcessPlan::getIsDeleted, 0)
                .isNotNull(WorkOrderProcessPlan::getPlanStartTime)
                .isNotNull(WorkOrderProcessPlan::getPlanEndTime)
                .lt(WorkOrderProcessPlan::getPlanStartTime, to)
                .gt(WorkOrderProcessPlan::getPlanEndTime, from)
                .orderByAsc(WorkOrderProcessPlan::getPlanStartTime);
        List<WorkOrderProcessPlan> plans = planMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(plans)) {
            return List.of();
        }
        List<Long> workOrderIds = plans.stream().map(WorkOrderProcessPlan::getWorkOrderId).distinct().toList();
        Map<Long, WorkOrder> orderMap = workOrderMapper.selectBatchIds(workOrderIds).stream()
                .collect(Collectors.toMap(WorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, String> planNumberByOrder = new HashMap<>();
        orderMap.forEach((orderId, order) -> planNumberByOrder.put(orderId, planNumberOf(order.getPlanId())));
        Map<Long, WorkOrderProcessPlan> detailMap = new HashMap<>();
        plans.stream().map(WorkOrderProcessPlan::getWorkOrderId).distinct()
                .forEach(id -> planMapper.selectByWorkOrderIdWithDetails(id).forEach(p -> detailMap.put(p.getId(), p)));
        plans.forEach(p -> {
            WorkOrderProcessPlan detail = detailMap.get(p.getId());
            if (detail != null) {
                p.setProcessTypeName(detail.getProcessTypeName());
                p.setRoomName(detail.getRoomName());
                p.setEquipmentName(detail.getEquipmentName());
                p.setTeamName(detail.getTeamName());
                p.setWorkOrderCode(detail.getWorkOrderCode());
            }
        });
        List<Long> planIds = plans.stream().map(WorkOrderProcessPlan::getId).toList();
        Map<Long, List<WorkOrderProcessPlanBatch>> batchMap = batchMapper.selectByPlanIds(planIds).stream()
                .collect(Collectors.groupingBy(WorkOrderProcessPlanBatch::getPlanId));
        // 按工单检查冲突，标记冲突计划
        Set<Long> conflictPlans = new HashSet<>();
        Map<Long, List<WorkOrderProcessPlan>> byOrder = plans.stream()
                .collect(Collectors.groupingBy(WorkOrderProcessPlan::getWorkOrderId));
        for (Map.Entry<Long, List<WorkOrderProcessPlan>> entry : byOrder.entrySet()) {
            List<WorkOrderProcessPlanBatch> batches = entry.getValue().stream()
                    .flatMap(p -> batchMap.getOrDefault(p.getId(), List.of()).stream()).toList();
            conflictService.check(entry.getKey(), entry.getValue(), batches)
                    .forEach(c -> conflictPlans.add(c.getPlanId()));
        }
        List<GanttItemDto> result = new ArrayList<>();
        for (WorkOrderProcessPlan plan : plans) {
            WorkOrder order = orderMap.get(plan.getWorkOrderId());
            List<WorkOrderProcessPlanBatch> batches = batchMap.get(plan.getId());
            if (CollectionUtils.isEmpty(batches)) {
                result.add(toGantt(plan, null, order, view, conflictPlans,
                        planNumberByOrder.get(plan.getWorkOrderId())));
            } else {
                for (WorkOrderProcessPlanBatch batch : batches) {
                    result.add(toGantt(plan, batch, order, view, conflictPlans,
                            planNumberByOrder.get(plan.getWorkOrderId())));
                }
            }
        }
        return result;
    }

    private GanttItemDto toGantt(WorkOrderProcessPlan plan, WorkOrderProcessPlanBatch batch,
                                 WorkOrder order, String view, Set<Long> conflictPlans,
                                 String planNumber) {
        GanttItemDto dto = new GanttItemDto();
        dto.setView(view);
        dto.setWorkOrderId(plan.getWorkOrderId());
        dto.setWorkOrderCode(plan.getWorkOrderCode());
        if (order != null) {
            dto.setPreparationName(order.getPreparationName());
            dto.setPlanNumber(planNumber);
            dto.setBatchNumber(order.getBatchNumber());
        }
        dto.setBatchName(batch == null ? null : batch.getBatchName());
        dto.setProcessName(plan.getProcessTypeName());
        dto.setStepOrder(plan.getStepOrder());
        dto.setStartTime(batch != null ? batch.getPlanStartTime() : plan.getPlanStartTime());
        dto.setEndTime(batch != null ? batch.getPlanEndTime() : plan.getPlanEndTime());
        dto.setEquipmentName(batch != null && batch.getEquipmentName() != null
                ? batch.getEquipmentName() : plan.getEquipmentName());
        dto.setRoomName(batch != null && batch.getRoomName() != null
                ? batch.getRoomName() : plan.getRoomName());
        dto.setBatchNo(batch == null ? null : batch.getBatchNo());
        dto.setConflict(conflictPlans.contains(plan.getId()));
        dto.setGroupKey(switch (view) {
            case "equipment" -> dto.getEquipmentName();
            case "room" -> dto.getRoomName();
            default -> plan.getWorkOrderCode();
        });
        return dto;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    private List<PreparationProcessTemplate> loadTemplates(Long preparationId) {
        if (preparationId == null) {
            return List.of();
        }
        return templateMapper.selectList(new LambdaQueryWrapper<PreparationProcessTemplate>()
                .eq(PreparationProcessTemplate::getPreparationId, preparationId)
                .orderByAsc(PreparationProcessTemplate::getStepOrder));
    }

    private List<WorkOrderProcessPlanBatch> buildBatches(WorkOrderProcessPlan plan,
                                                         List<PlanBatchDto> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return List.of();
        }
        List<WorkOrderProcessPlanBatch> result = new ArrayList<>();
        for (PlanBatchDto dto : dtos) {
            WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
            batch.setPlanId(plan.getId());
            batch.setBatchNo(dto.getBatchNo());
            batch.setBatchName(dto.getBatchName());
            batch.setPlanStartTime(dto.getPlanStartTime());
            batch.setPlanEndTime(dto.getPlanEndTime());
            batch.setPlanDurationMinutes(dto.getPlanStartTime() != null && dto.getPlanEndTime() != null
                    ? Math.toIntExact(Duration.between(dto.getPlanStartTime(), dto.getPlanEndTime()).toMinutes()) : null);
            batch.setPlanQty(dto.getPlanQty());
            // 计量单位：优先取前端传值，未传时继承所属工序计划
            batch.setUnitId(dto.getUnitId() != null ? dto.getUnitId() : plan.getUnitId());
            batch.setRoomId(dto.getRoomId() != null ? dto.getRoomId() : plan.getRoomId());
            batch.setEquipmentId(dto.getEquipmentId() != null ? dto.getEquipmentId() : plan.getEquipmentId());
            batch.setTeamId(dto.getTeamId() != null ? dto.getTeamId() : plan.getTeamId());
            // 批次排产标识由前端传参，未传时沿用所属工序计划的值
            batch.setScheduled(dto.getScheduled() != null
                    ? Integer.valueOf(dto.getScheduled() ? 1 : 0) : plan.getScheduled());
            batch.setStatus("计划中");
            batch.setTeamName(dto.getTeamName());
            batch.setRoomName(dto.getRoomName());
            batch.setEquipmentName(dto.getEquipmentName());
            if (dto.getParticipants() != null) {
                batch.setParticipants(dto.getParticipants().stream()
                        .filter(x -> x.getPersonnelId() != null)
                        .map(x -> toParticipant(x, plan.getId(), null))
                        .toList());
            }
            result.add(batch);
        }
        return result;
    }

    private List<WorkOrderProcessPlanParticipant> buildParticipants(WorkOrderProcessPlan plan,
                                                                    List<PlanParticipantDto> dtos) {
        List<WorkOrderProcessPlanParticipant> result = new ArrayList<>();
        List<PlanParticipantDto> source = dtos;
        if (CollectionUtils.isEmpty(source) && plan.getTeamId() != null) {
            source = teamMemberMapper.selectPersonnelIdsByTeamId(plan.getTeamId()).stream().map(id -> {
                PlanParticipantDto dto = new PlanParticipantDto();
                dto.setPersonnelId(id);
                return dto;
            }).toList();
        }
        if (source == null) {
            return result;
        }
        for (PlanParticipantDto dto : source) {
            if (dto.getPersonnelId() == null) {
                continue;
            }
            result.add(toParticipant(dto, plan.getId(), null));
        }
        return result;
    }

    /**
     * 计划参与人 DTO 转实体
     */
    private WorkOrderProcessPlanParticipant toParticipant(PlanParticipantDto dto, Long planId, Long batchId) {
        WorkOrderProcessPlanParticipant participant = new WorkOrderProcessPlanParticipant();
        participant.setPlanId(planId);
        participant.setBatchId(batchId);
        participant.setPersonnelId(dto.getPersonnelId());
        participant.setIsPrimary(dto.getIsPrimary() == null ? 0 : dto.getIsPrimary());
        return participant;
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end, String label) {
        if ((start == null) != (end == null)) {
            throw new IllegalArgumentException(label + "：计划开始与结束时间必须同时填写");
        }
        if (start != null && !end.isAfter(start)) {
            throw new IllegalArgumentException(label + "：计划结束时间必须晚于开始时间");
        }
    }

    /** 生产计划编号（work_order.plan_number 为非表字段，需查 production_plan） */
    private String planNumberOf(Long planId) {
        if (planId == null) {
            return null;
        }
        ProductionPlan plan = productionPlanMapper.selectById(planId.intValue());
        return plan == null ? null : plan.getPlanNumber();
    }

    private String processName(Long processTypeId, String fallback) {        if (processTypeId == null) {
            return fallback;
        }
        ProcessType type = processTypeMapper.selectById(processTypeId);
        return type == null ? fallback : type.getProcessName();
    }

    private String roomName(Integer roomId) {
        com.tonghui.erp.Data.Entity.RoomInfo room = roomInfoMapper.selectById(roomId);
        return room == null ? null : room.getRoomName();
    }

    private String equipmentName(Integer equipmentId) {
        com.tonghui.erp.Data.Entity.Equipment equipment = equipmentMapper.selectById(equipmentId);
        return equipment == null ? null : equipment.getEquipmentName();
    }

    /**
     * 批量加载计量单位
     *
     * @param unitIds 单位ID集合
     * @return 单位ID -> 单位
     */
    private Map<Long, Unit> loadUnits(java.util.Collection<Long> unitIds) {
        Set<Long> ids = unitIds == null ? Set.of()
                : unitIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return unitMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Unit::getUnitId, u -> u, (a, b) -> a));
    }

    /**
     * 填充工序行的计量单位
     */
    private void fillUnit(ProcessScheduleDto dto, Long unitId, Map<Long, Unit> unitMap) {
        dto.setUnitId(unitId);
        Unit unit = unitId == null ? null : unitMap.get(unitId);
        if (unit != null) {
            dto.setUnitName(unit.getUnitName());
            dto.setUnitSymbol(unit.getSymbol());
        }
    }

    /**
     * 填充批次行的计量单位
     */
    private void fillUnit(PlanBatchDto dto, Long unitId, Map<Long, Unit> unitMap) {
        dto.setUnitId(unitId);
        Unit unit = unitId == null ? null : unitMap.get(unitId);
        if (unit != null) {
            dto.setUnitName(unit.getUnitName());
            dto.setUnitSymbol(unit.getSymbol());
        }
    }

    private String workOrderCode(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        return order == null ? null : order.getWorkOrderCode();
    }

    // endregion
}

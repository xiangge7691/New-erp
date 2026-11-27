package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Constants.EquipmentStatus;
import com.tonghui.erp.Common.Constants.PlanBatchStatus;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.Dto.Pda.PdaExecutionSaveRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaOptionDto;
import com.tonghui.erp.Common.Dto.Pda.PdaParticipantDto;
import com.tonghui.erp.Common.Dto.Pda.PdaParticipantRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaProcessDto;
import com.tonghui.erp.Common.Dto.Pda.PdaPunchRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaRecordDto;
import com.tonghui.erp.Common.Dto.Pda.PdaTaskDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Common.utils.WorkOrderExecutionTimeValidator;
import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.mapper.BusinessFileMapper;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.PdaProductionService;
import com.tonghui.erp.Service.PerformanceGenerationService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PDA 生产工序执行服务实现
 */
@Service
public class PdaProductionServiceImpl implements PdaProductionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String BIZ_TYPE = "WORK_ORDER_PROCESS_EXECUTION";

    @Autowired
    private WorkOrderProcessExecutionService executionService;

    @Autowired
    private WorkOrderProcessExecutionMapper executionMapper;

    @Autowired
    private WorkOrderProcessExecutionParticipantMapper participantMapper;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BusinessFileMapper businessFileMapper;

    /** 排产计划 Mapper（拆分批次后按批次展开工序行） */
    @Autowired
    private WorkOrderProcessPlanMapper planMapper;

    /** 排产批次 Mapper */
    @Autowired
    private WorkOrderProcessPlanBatchMapper planBatchMapper;

    /** 计量单位 Mapper */
    @Autowired
    private UnitMapper unitMapper;

    /** 绩效生成服务（PDA 提交后生成绩效记录） */
    @Autowired
    private PerformanceGenerationService performanceGenerationService;

    /** 实际执行时间校验器（任务时间窗 + 工序先后） */
    @Autowired
    private WorkOrderExecutionTimeValidator executionTimeValidator;

    // region 查询

    @Override
    public List<PdaTaskDto> listTasks(String status, Long personId, Long processTypeId,
                                      String workshop, String keyword, boolean todoOnly) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getIsDeleted, 0)
                .notIn(WorkOrder::getCurrentStatus, "已作废", "已归档");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(WorkOrder::getWorkOrderCode, keyword)
                    .or().like(WorkOrder::getWorkOrderName, keyword)
                    .or().like(WorkOrder::getPreparationName, keyword)
                    .or().like(WorkOrder::getBatchNumber, keyword));
        }
        List<WorkOrder> orders = workOrderMapper.selectList(wrapper);
        orders.sort(Comparator
                .comparing(WorkOrder::getPlanStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkOrder::getCreatedTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkOrder::getWorkOrderId,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        List<PdaTaskDto> result = new ArrayList<>();
        for (WorkOrder order : orders) {
            PdaTaskDto dto = buildTask(order);
            if (StringUtils.hasText(status) && !status.equals(dto.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(workshop) && !workshop.equals(dto.getWorkshop())) {
                continue;
            }
            if (personId != null && dto.getProcesses().stream().noneMatch(p ->
                    p.getParticipants() != null && p.getParticipants().stream()
                            .anyMatch(x -> personId.equals(x.getPersonnelId())))) {
                continue;
            }
            if (processTypeId != null && dto.getProcesses().stream()
                    .noneMatch(p -> processTypeId.equals(p.getProcessTypeId()))) {
                continue;
            }
            // PDA 待办模式：隐藏已全部完成的工单（明确查看“已完成”时除外）
            if (todoOnly && !"已完成".equals(status)
                    && dto.getProcessCount() != null && dto.getDoneCount() != null
                    && dto.getDoneCount() >= dto.getProcessCount()) {
                continue;
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public PdaTaskDto getTask(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new IllegalArgumentException("生产任务不存在");
        }
        return buildTask(order);
    }

    /**
     * 确保排产批次的执行记录存在（不存在则创建）
     * <p>批次行拆分后按批次独立登记实际生产，PDA 开始计时或保存前先取得批次执行记录。</p>
     *
     * @param workOrderId 工单ID
     * @param planBatchId 排产批次ID
     * @return 批次执行行
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaProcessDto ensureBatchExecution(Long workOrderId, Long planBatchId) {
        WorkOrderProcessPlanBatch batch = planBatchMapper.selectById(planBatchId);
        if (batch == null) {
            throw new IllegalArgumentException("排产批次不存在");
        }
        WorkOrderProcessPlan plan = planMapper.selectById(batch.getPlanId());
        if (plan == null || !Objects.equals(plan.getWorkOrderId(), workOrderId)) {
            throw new IllegalArgumentException("排产批次不属于该生产任务");
        }
        // 已有批次执行记录直接复用，重复点击开始不会产生多行
        WorkOrderProcessExecution existing = executionService.getOne(
                new QueryWrapper<WorkOrderProcessExecution>()
                        .eq("work_order_id", workOrderId)
                        .eq("plan_batch_id", planBatchId)
                        .orderByAsc("id")
                        .last("LIMIT 1"), false);
        if (existing != null) {
            return loadProcess(existing);
        }
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setWorkOrderId(workOrderId);
        execution.setPlanId(plan.getId());
        execution.setPlanBatchId(planBatchId);
        execution.setBatchNo(batch.getBatchNo());
        execution.setProcessTypeId(plan.getProcessTypeId());
        execution.setStepOrder(plan.getStepOrder());
        execution.setUnitId(batch.getUnitId() != null ? batch.getUnitId() : plan.getUnitId());
        execution.setRoomId(batch.getRoomId() != null ? batch.getRoomId() : plan.getRoomId());
        execution.setEquipmentId(batch.getEquipmentId() != null
                ? batch.getEquipmentId() : plan.getEquipmentId());
        execution.setTeamId(batch.getTeamId() != null ? batch.getTeamId() : plan.getTeamId());
        execution.setStatus(ProcessExecutionStatus.PENDING);
        execution.setLocked(0);
        execution.setPdaSubmitted(0);
        execution.setManualTime(0);
        execution.setIsDeleted(0);
        execution.setVersion(1);
        try {
            executionService.save(execution);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            // 并发点击「开始」时另一个请求已创建同一批次执行记录（uk_wpe_plan_batch），复用已有行
            WorkOrderProcessExecution concurrent = executionService.getOne(
                    new QueryWrapper<WorkOrderProcessExecution>()
                            .eq("work_order_id", workOrderId)
                            .eq("plan_batch_id", planBatchId)
                            .orderByAsc("id")
                            .last("LIMIT 1"), false);
            if (concurrent == null) {
                throw ex;
            }
            return loadProcess(concurrent);
        }
        return loadProcess(execution);
    }

    private PdaTaskDto buildTask(WorkOrder order) {
        List<WorkOrderProcessExecution> executions =
                executionService.getByWorkOrderId(order.getWorkOrderId());
        executions.sort(Comparator.comparing(WorkOrderProcessExecution::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkOrderProcessExecution::getBatchNo,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkOrderProcessExecution::getId));
        List<PdaProcessDto> processes = buildProcesses(order, executions);

        PdaTaskDto dto = new PdaTaskDto();
        dto.setWorkOrderId(order.getWorkOrderId());
        dto.setWorkOrderCode(order.getWorkOrderCode());
        dto.setWorkOrderName(order.getWorkOrderName());
        dto.setPreparationName(order.getPreparationName());
        dto.setBatchNumber(order.getBatchNumber());
        dto.setBatchQty(order.getBatchQty());
        // 工单未填计划开始时间时取最早排产工序/批次的开始，避免任务页显示为空
        LocalDateTime planStart = order.getPlanStartTime() != null ? order.getPlanStartTime()
                : processes.stream().map(PdaProcessDto::getPlanStartTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
        dto.setPlanStartTime(planStart);
        dto.setProcessCount(processes.size());
        dto.setDoneCount((int) processes.stream()
                .filter(p -> Boolean.TRUE.equals(p.getDone())).count());
        dto.setSubmittedCount((int) processes.stream()
                .filter(p -> Boolean.TRUE.equals(p.getSubmitted())).count());
        dto.setProcesses(processes);
        dto.setStatus(resolveTaskStatus(processes));
        return dto;
    }

    /**
     * 构建 PDA 工序明细行
     * <p>
     * 排产拆分批次后，工序按批次展开为多条执行行（批次 1、批次 2…），不再展示原整道工序行；
     * 批次尚未生成执行记录时 executionId 为空，前端可调用「确保批次执行记录」接口后再开始计时。
     * </p>
     *
     * @param order      工单
     * @param executions 执行记录
     * @return 工序明细行（按工序顺序、批次序号排序）
     */
    private List<PdaProcessDto> buildProcesses(WorkOrder order, List<WorkOrderProcessExecution> executions) {
        // 草稿排产不参与实际生产，正式保存后才按批次展开
        List<WorkOrderProcessPlan> plans = planMapper.selectByWorkOrderIdWithDetails(order.getWorkOrderId())
                .stream().filter(p -> !"草稿".equals(p.getStatus())).toList();
        List<Long> planIds = plans.stream().map(WorkOrderProcessPlan::getId).toList();
        Map<Long, List<WorkOrderProcessPlanBatch>> batchesByPlan = planIds.isEmpty() ? Map.of()
                : planBatchMapper.selectByPlanIds(planIds).stream()
                .collect(Collectors.groupingBy(WorkOrderProcessPlanBatch::getPlanId));
        Map<Long, Unit> unitMap = loadUnits(plans, batchesByPlan);

        List<PdaProcessDto> rows = new ArrayList<>();
        Set<String> handled = new HashSet<>();
        for (WorkOrderProcessPlan plan : plans) {
            List<WorkOrderProcessPlanBatch> batches = batchesByPlan.getOrDefault(plan.getId(), List.of());
            if (batches.isEmpty()) {
                continue;
            }
            // 同一工序只展开一次，避免重复计划行导致批次行翻倍
            if (!handled.add(processKey(plan.getProcessTypeId(), plan.getStepOrder()))) {
                continue;
            }
            List<WorkOrderProcessExecution> stepExecutions = executions.stream()
                    .filter(e -> Objects.equals(e.getProcessTypeId(), plan.getProcessTypeId())
                            && Objects.equals(e.getStepOrder(), plan.getStepOrder()))
                    .toList();
            expandBatchRows(rows, plan, batches, stepExecutions, unitMap);
        }
        // 未拆批次的工序按执行记录逐条展示
        for (WorkOrderProcessExecution execution : executions) {
            if (handled.contains(processKey(execution.getProcessTypeId(), execution.getStepOrder()))) {
                continue;
            }
            rows.add(toProcessDto(execution));
        }
        rows.sort(Comparator.comparing(PdaProcessDto::getStepOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PdaProcessDto::getBatchNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PdaProcessDto::getExecutionId, Comparator.nullsLast(Long::compareTo)));
        return rows;
    }

    /**
     * 按批次展开工序明细行：有批次执行记录时展示实际数据，没有时生成待执行行
     *
     * @param rows       结果行
     * @param plan       排产计划
     * @param batches    批次列表
     * @param executions 该工序的执行记录
     * @param unitMap    计量单位
     */
    private void expandBatchRows(List<PdaProcessDto> rows, WorkOrderProcessPlan plan,
                                 List<WorkOrderProcessPlanBatch> batches,
                                 List<WorkOrderProcessExecution> executions,
                                 Map<Long, Unit> unitMap) {
        List<WorkOrderProcessPlanBatch> sorted = batches.stream()
                .sorted(Comparator.comparing(WorkOrderProcessPlanBatch::getBatchNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        for (WorkOrderProcessPlanBatch batch : sorted) {
            WorkOrderProcessExecution matched = executions.stream()
                    .filter(e -> PlanBatchStatus.matches(batch, e))
                    .findFirst().orElse(null);
            PdaProcessDto dto = matched != null ? toProcessDto(matched) : newBatchRow(plan, unitMap);
            // 批次口径优先：计划起止、数量、单位以批次为准
            dto.setPlanId(plan.getId());
            dto.setPlanBatchId(batch.getId());
            dto.setBatchNo(batch.getBatchNo());
            dto.setBatchName(StringUtils.hasText(batch.getBatchName())
                    ? batch.getBatchName()
                    : (batch.getBatchNo() == null ? null : "批次 " + batch.getBatchNo()));
            if (batch.getPlanStartTime() != null) {
                dto.setPlanStartTime(batch.getPlanStartTime());
            }
            if (batch.getPlanEndTime() != null) {
                dto.setPlanEndTime(batch.getPlanEndTime());
            }
            if (batch.getPlanQty() != null) {
                dto.setPlanQty(batch.getPlanQty());
            }
            Long unitId = batch.getUnitId() != null ? batch.getUnitId() : plan.getUnitId();
            if (unitId != null) {
                dto.setUnitId(unitId);
                Unit unit = unitMap.get(unitId);
                if (unit != null) {
                    dto.setUnitName(unit.getUnitName());
                    dto.setUnitSymbol(unit.getSymbol());
                }
            }
            if (dto.getEquipmentId() == null) {
                dto.setEquipmentId(batch.getEquipmentId() != null ? batch.getEquipmentId() : plan.getEquipmentId());
                dto.setEquipmentName(batch.getEquipmentName() != null
                        ? batch.getEquipmentName() : plan.getEquipmentName());
            }
            if (dto.getRoomId() == null) {
                dto.setRoomId(batch.getRoomId() != null ? batch.getRoomId() : plan.getRoomId());
                dto.setRoomName(batch.getRoomName() != null ? batch.getRoomName() : plan.getRoomName());
            }
            rows.add(dto);
        }
    }

    /**
     * 生成未登记实际生产的批次行
     *
     * @param plan    排产计划
     * @param unitMap 计量单位
     * @return 待执行的批次行
     */
    private PdaProcessDto newBatchRow(WorkOrderProcessPlan plan, Map<Long, Unit> unitMap) {
        PdaProcessDto dto = new PdaProcessDto();
        dto.setStepOrder(plan.getStepOrder());
        dto.setProcessTypeId(plan.getProcessTypeId());
        dto.setProcessName(plan.getProcessTypeName());
        dto.setPlanStartTime(plan.getPlanStartTime());
        dto.setPlanEndTime(plan.getPlanEndTime());
        dto.setPlanQty(plan.getPlanQty());
        dto.setEquipmentId(plan.getEquipmentId());
        dto.setEquipmentName(plan.getEquipmentName());
        dto.setRoomId(plan.getRoomId());
        dto.setRoomName(plan.getRoomName());
        if (plan.getUnitId() != null) {
            dto.setUnitId(plan.getUnitId());
            Unit unit = unitMap.get(plan.getUnitId());
            if (unit != null) {
                dto.setUnitName(unit.getUnitName());
                dto.setUnitSymbol(unit.getSymbol());
            }
        }
        dto.setStatus("未开始");
        dto.setDone(false);
        dto.setSubmitted(false);
        dto.setPcFilled(false);
        dto.setLocked(false);
        dto.setManualTime(false);
        dto.setParticipants(List.of());
        return dto;
    }

    /**
     * 加载计划/批次用到的计量单位
     *
     * @param plans        排产计划
     * @param batchesByPlan 计划下的批次
     * @return 单位ID → 单位
     */
    private Map<Long, Unit> loadUnits(List<WorkOrderProcessPlan> plans,
                                      Map<Long, List<WorkOrderProcessPlanBatch>> batchesByPlan) {
        Set<Long> unitIds = new HashSet<>();
        plans.stream().map(WorkOrderProcessPlan::getUnitId).filter(Objects::nonNull).forEach(unitIds::add);
        batchesByPlan.values().stream().flatMap(List::stream)
                .map(WorkOrderProcessPlanBatch::getUnitId).filter(Objects::nonNull).forEach(unitIds::add);
        if (unitIds.isEmpty()) {
            return Map.of();
        }
        return unitMapper.selectBatchIds(unitIds).stream()
                .collect(Collectors.toMap(Unit::getUnitId, Function.identity(), (a, b) -> a));
    }

    /**
     * 工序匹配键：工序类型 + 工序顺序
     *
     * @param processTypeId 工序类型ID
     * @param stepOrder     工序顺序
     * @return 匹配键
     */
    private String processKey(Long processTypeId, Integer stepOrder) {
        return processTypeId + "#" + stepOrder;
    }

    /**
     * 按明细行状态汇总任务状态
     *
     * @param processes 工序明细行
     * @return 未开始/进行中/已完成
     */
    private String resolveTaskStatus(List<PdaProcessDto> processes) {
        if (CollectionUtils.isEmpty(processes)) {
            return "未开始";
        }
        if (processes.stream().allMatch(p -> Boolean.TRUE.equals(p.getDone()))) {
            return "已完成";
        }
        boolean anyStarted = processes.stream().anyMatch(p -> "进行中".equals(p.getStatus())
                || Boolean.TRUE.equals(p.getDone()));
        return anyStarted ? "进行中" : "未开始";
    }

    /**
     * 是否已通过 PDA 提交
     */
    private boolean isPdaSubmitted(WorkOrderProcessExecution execution) {
        return Integer.valueOf(1).equals(execution.getPdaSubmitted());
    }

    /**
     * 工序是否由 PC 端填写完整起止时间（PDA 尚未提交、未完工锁定）
     * <p>
     * PC 批量保存不会写入 PDA 设备号，而 PDA 的开始、结束、暂存都会写入设备号，
     * 以此区分 PC 填写与 PDA 暂存，避免把 PDA 暂存误判为已完成。
     * </p>
     */
    private boolean isPcFilled(WorkOrderProcessExecution execution) {
        return !isPdaSubmitted(execution)
                && !Integer.valueOf(1).equals(execution.getLocked())
                && execution.getStartTime() != null
                && execution.getEndTime() != null
                && !StringUtils.hasText(execution.getPdaDeviceNo());
    }

    /**
     * 工序是否已完成（PDA提交、PC填写完整起止时间或已完工锁定）
     */
    private boolean isDone(WorkOrderProcessExecution execution) {
        return isPdaSubmitted(execution)
                || Integer.valueOf(1).equals(execution.getLocked())
                || isPcFilled(execution);
    }

    private PdaProcessDto toProcessDto(WorkOrderProcessExecution execution) {
        PdaProcessDto dto = new PdaProcessDto();
        dto.setExecutionId(execution.getId());
        dto.setPlanId(execution.getPlanId());
        dto.setPlanBatchId(execution.getPlanBatchId());
        dto.setStepOrder(execution.getStepOrder());
        dto.setProcessTypeId(execution.getProcessTypeId());
        dto.setProcessName(execution.getProcessTypeName());
        dto.setBatchNo(execution.getBatchNo());
        dto.setBatchName(execution.getPlanBatchName());
        dto.setPlanStartTime(execution.getPlanStartTime());
        dto.setPlanEndTime(execution.getPlanEndTime());
        // 计划数量只取排产计划/批次；未维护计划时返回空，不能回退成实际数量
        dto.setPlanQty(execution.getPlanQty());
        dto.setUnitId(execution.getUnitId());
        dto.setUnitName(execution.getUnitName());
        dto.setUnitSymbol(execution.getUnitSymbol());
        dto.setEquipmentId(execution.getEquipmentId());
        dto.setEquipmentName(execution.getEquipmentName());
        dto.setRoomId(execution.getRoomId());
        dto.setRoomName(execution.getRoomName());
        dto.setActualStartTime(execution.getStartTime());
        dto.setActualEndTime(execution.getEndTime());
        dto.setActualEquipmentName(execution.getEquipmentName());
        dto.setActualQty(execution.getProcessQty());
        boolean pdaSubmitted = isPdaSubmitted(execution);
        boolean locked = Integer.valueOf(1).equals(execution.getLocked());
        boolean done = isDone(execution);
        dto.setSubmitted(pdaSubmitted);
        dto.setLocked(locked);
        dto.setDone(done);
        dto.setPcFilled(isPcFilled(execution));
        dto.setManualTime(Integer.valueOf(1).equals(execution.getManualTime()));
        dto.setManualReason(execution.getManualReason());
        dto.setRemark(execution.getRemark());
        if (done) {
            dto.setStatus("已完成");
        } else if (execution.getStartTime() != null) {
            dto.setStatus("进行中");
        } else {
            dto.setStatus("未开始");
        }
        if (execution.getParticipants() != null) {
            dto.setParticipants(execution.getParticipants().stream().map(p -> {
                PdaParticipantDto x = new PdaParticipantDto();
                x.setPersonnelId(p.getPersonnelId());
                x.setPersonnelName(p.getPersonnelName());
                x.setCompletedQty(p.getCompletedQty());
                x.setIsPrimary(p.getIsPrimary());
                return x;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    @Override
    public List<PdaRecordDto> listRecords(String keyword, Long processTypeId, Long personId) {
        List<WorkOrderProcessExecution> submittedRows = executionService.list(
                new QueryWrapper<WorkOrderProcessExecution>()
                        .eq("pda_submitted", 1)
                        .eq("is_deleted", 0));
        if (submittedRows.isEmpty()) {
            return List.of();
        }
        Set<Long> workOrderIds = submittedRows.stream()
                .map(WorkOrderProcessExecution::getWorkOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 复用带工序/设备/房间/工单明细的查询，保证提交记录展示完整
        List<WorkOrderProcessExecution> executions = new ArrayList<>();
        for (Long workOrderId : workOrderIds) {
            for (WorkOrderProcessExecution execution : executionService.getByWorkOrderId(workOrderId)) {
                if (Integer.valueOf(1).equals(execution.getPdaSubmitted())) {
                    executions.add(execution);
                }
            }
        }
        if (executions.isEmpty()) {
            return List.of();
        }
        Map<Long, WorkOrder> workOrderMap = workOrderIds.isEmpty() ? Map.of()
                : workOrderMapper.selectBatchIds(workOrderIds).stream()
                .collect(Collectors.toMap(WorkOrder::getWorkOrderId, Function.identity()));

        List<Long> executionIds = executions.stream()
                .map(WorkOrderProcessExecution::getId)
                .collect(Collectors.toList());
        Map<Long, List<WorkOrderProcessExecutionParticipant>> participantMap =
                participantMapper.selectByExecutionIdsWithPersonnel(executionIds).stream()
                        .collect(Collectors.groupingBy(WorkOrderProcessExecutionParticipant::getExecutionId));

        Set<Long> userIds = executions.stream()
                .map(WorkOrderProcessExecution::getPdaSubmitBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<PdaRecordDto> records = new ArrayList<>();
        for (WorkOrderProcessExecution execution : executions) {
            List<WorkOrderProcessExecutionParticipant> participants =
                    participantMap.getOrDefault(execution.getId(), List.of());
            if (personId != null && participants.stream()
                    .noneMatch(p -> personId.equals(p.getPersonnelId()))) {
                continue;
            }
            if (processTypeId != null && !processTypeId.equals(execution.getProcessTypeId())) {
                continue;
            }
            WorkOrder workOrder = workOrderMap.get(execution.getWorkOrderId());
            List<String> names = participants.stream()
                    .map(WorkOrderProcessExecutionParticipant::getPersonnelName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            User submitter = execution.getPdaSubmitBy() == null
                    ? null : userMap.get(execution.getPdaSubmitBy());
            String submitterName = submitter != null ? submitter.getUserName() : execution.getOperatorName();
            if (StringUtils.hasText(keyword)) {
                String text = joinText(workOrder == null ? null : workOrder.getWorkOrderCode(),
                        workOrder == null ? null : workOrder.getPreparationName(),
                        workOrder == null ? null : workOrder.getBatchNumber(),
                        execution.getProcessTypeName(), submitterName,
                        String.join("、", names));
                if (!text.toLowerCase().contains(keyword.toLowerCase())) {
                    continue;
                }
            }
            PdaRecordDto dto = new PdaRecordDto();
            dto.setWorkOrderId(execution.getWorkOrderId());
            dto.setWorkOrderCode(workOrder == null ? null : workOrder.getWorkOrderCode());
            dto.setPreparationName(workOrder == null ? null : workOrder.getPreparationName());
            dto.setBatchNumber(workOrder == null ? null : workOrder.getBatchNumber());
            dto.setExecutionId(execution.getId());
            dto.setStepOrder(execution.getStepOrder());
            dto.setProcessTypeId(execution.getProcessTypeId());
            dto.setProcessName(execution.getProcessTypeName());
            dto.setActualStartTime(execution.getStartTime());
            dto.setActualEndTime(execution.getEndTime());
            dto.setEquipmentName(execution.getEquipmentName());
            dto.setProcessQty(execution.getProcessQty());
            dto.setParticipantNames(names);
            dto.setSubmitterName(submitterName);
            dto.setSubmitTime(execution.getPdaSubmitTime() != null
                    ? execution.getPdaSubmitTime() : execution.getCompletedTime());
            records.add(dto);
        }
        records.sort(Comparator
                .comparing(PdaRecordDto::getSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return records;
    }

    private String joinText(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                sb.append(value).append(' ');
            }
        }
        return sb.toString();
    }

    @Override
    public List<PdaOptionDto> equipmentOptions(String keyword, Integer roomId) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getIsDeleted, 0)
                .eq(Equipment::getEquipmentStatus, EquipmentStatus.NORMAL);
        if (roomId != null) {
            wrapper.eq(Equipment::getRoomId, roomId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Equipment::getEquipmentName, keyword)
                    .or().like(Equipment::getFixedAssetCode, keyword));
        }
        List<Equipment> equipments = equipmentMapper.selectList(wrapper);
        Map<Integer, List<WorkOrderProcessExecution>> openMap =
                executionMapper.selectOpenExecutionsWithDetails().stream()
                        .filter(e -> e.getEquipmentId() != null)
                        .collect(Collectors.groupingBy(WorkOrderProcessExecution::getEquipmentId));
        List<PdaOptionDto> options = new ArrayList<>();
        for (Equipment equipment : equipments) {
            PdaOptionDto dto = new PdaOptionDto();
            dto.setId(equipment.getEquipmentId() == null ? null : equipment.getEquipmentId().longValue());
            dto.setName(equipment.getEquipmentName());
            dto.setCode(equipment.getFixedAssetCode());
            dto.setRoomId(equipment.getRoomId());
            List<WorkOrderProcessExecution> open =
                    openMap.getOrDefault(equipment.getEquipmentId(), List.of());
            dto.setOccupied(!open.isEmpty());
            if (!open.isEmpty()) {
                WorkOrderProcessExecution e = open.get(0);
                dto.setOccupiedInfo((e.getWorkOrderCode() == null ? "" : e.getWorkOrderCode() + " ")
                        + (e.getPreparationName() == null ? "" : e.getPreparationName() + " · ")
                        + (e.getProcessTypeName() == null ? "" : e.getProcessTypeName())
                        + "（" + format(e.getStartTime()) + " 开始）");
            }
            options.add(dto);
        }
        options.sort(Comparator.comparing(PdaOptionDto::getName,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return options;
    }

    @Override
    public List<PdaOptionDto> personnelOptions(String keyword) {
        LambdaQueryWrapper<PersonnelFile> wrapper = new LambdaQueryWrapper<PersonnelFile>()
                .eq(PersonnelFile::getIsDeleted, 0)
                .eq(PersonnelFile::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(PersonnelFile::getName, keyword)
                    .or().like(PersonnelFile::getEmployeeNo, keyword));
        }
        wrapper.orderByAsc(PersonnelFile::getName);
        return personnelFileMapper.selectList(wrapper).stream().map(p -> {
            PdaOptionDto dto = new PdaOptionDto();
            dto.setId(p.getPersonnelFileId());
            dto.setName(p.getName());
            dto.setCode(p.getEmployeeNo());
            return dto;
        }).collect(Collectors.toList());
    }

    // endregion

    // region 打点与保存

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaProcessDto start(Long executionId, PdaPunchRequest request) {
        WorkOrderProcessExecution execution = requireEditable(executionId);
        if (execution.getStartTime() != null) {
            throw new IllegalArgumentException("该工序已开始计时");
        }
        Integer equipmentId = request.getEquipmentId() != null
                ? request.getEquipmentId() : execution.getEquipmentId();
        Equipment equipment = requireEquipment(equipmentId);
        LocalDateTime now = nowMinute();
        assertEquipmentFree(execution, equipment, now, now);
        execution.setEquipmentId(equipment.getEquipmentId());
        execution.setRoomId(equipment.getRoomId());
        execution.setStartTime(now);
        execution.setEndTime(null);
        execution.setManualTime(0);
        execution.setManualReason(null);
        if (StringUtils.hasText(request.getDeviceNo())) {
            execution.setPdaDeviceNo(request.getDeviceNo());
        }
        execution.setStatus("执行中");
        setOperator(execution);
        executionService.updateById(execution);
        executionService.fillPlanReference(execution);
        executionService.refreshPlanBatchStatus(execution.getPlanBatchId());
        return loadProcess(execution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaProcessDto end(Long executionId, PdaPunchRequest request) {
        WorkOrderProcessExecution execution = requireEditable(executionId);
        if (execution.getStartTime() == null) {
            throw new IllegalArgumentException("请先开始计时");
        }
        if (execution.getEndTime() != null) {
            throw new IllegalArgumentException("该工序已结束计时");
        }
        LocalDateTime now = nowMinute();
        if (!now.isAfter(execution.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        Equipment equipment = requireEquipment(execution.getEquipmentId());
        assertEquipmentFree(execution, equipment, execution.getStartTime(), now);
        execution.setEndTime(now);
        execution.setStatus("已完成");
        // 结束打点同样要满足任务时间窗与工序先后，避免 PDA 绕过 PC 的校验
        validateExecutionTime(execution);
        if (StringUtils.hasText(request.getDeviceNo())) {
            execution.setPdaDeviceNo(request.getDeviceNo());
        }
        setOperator(execution);
        executionService.updateById(execution);
        executionService.fillPlanReference(execution);
        executionService.refreshPlanBatchStatus(execution.getPlanBatchId());
        return loadProcess(execution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaProcessDto saveDraft(Long executionId, PdaExecutionSaveRequest request) {
        WorkOrderProcessExecution execution = requireEditable(executionId);
        validateCommon(execution, request, false);
        applyFields(execution, request);
        if (StringUtils.hasText(request.getDeviceNo())) {
            execution.setPdaDeviceNo(request.getDeviceNo());
        }
        executionService.updateById(execution);
        executionService.fillPlanReference(execution);
        saveParticipants(execution, request.getParticipants(), false);
        executionService.refreshPlanBatchStatus(execution.getPlanBatchId());
        return loadProcess(execution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaProcessDto submit(Long executionId, PdaExecutionSaveRequest request) {
        WorkOrderProcessExecution execution = requireEditable(executionId);
        if (StringUtils.hasText(request.getRequestId())) {
            WorkOrderProcessExecution existing = executionService.getOne(
                    new QueryWrapper<WorkOrderProcessExecution>()
                            .eq("client_request_id", request.getRequestId()), false);
            if (existing != null) {
                if (!existing.getId().equals(executionId)) {
                    throw new IllegalArgumentException("该请求已被其他工序处理");
                }
                return loadProcess(existing);
            }
        }
        validateCommon(execution, request, true);
        long photoCount = businessFileMapper.selectCount(
                new QueryWrapper<BusinessFile>()
                        .eq("biz_type", BIZ_TYPE)
                        .eq("biz_id", executionId)
                        .eq("is_deleted", 0));
        if (photoCount > 9) {
            throw new IllegalArgumentException("每道工序最多 9 张照片");
        }
        applyFields(execution, request);
        validateExecutionTime(execution);
        execution.setPdaSubmitted(1);
        execution.setPdaSubmitBy(EntityUtils.getCurrentUserId());
        execution.setPdaSubmitTime(nowMinute());
        execution.setPdaDeviceNo(request.getDeviceNo());
        execution.setClientRequestId(request.getRequestId());
        executionService.updateById(execution);
        executionService.fillPlanReference(execution);
        saveParticipants(execution, request.getParticipants(), true);
        // PDA 提交即完工口径，按参与人与工序时长生成绩效（幂等，重复提交不会重复生成）
        performanceGenerationService.generateForExecution(executionId);
        executionService.refreshPlanBatchStatus(execution.getPlanBatchId());
        return loadProcess(execution);
    }

    /**
     * 校验本次打点/提交的实际时间
     * <p>必须落在任务配置时间窗内，且不得早于前序工序的实际结束时间；校验排除本记录原值。</p>
     *
     * @param execution 执行记录（已应用本次提交的起止时间）
     */
    private void validateExecutionTime(WorkOrderProcessExecution execution) {
        if (execution.getStartTime() == null) {
            return;
        }
        WorkOrder order = workOrderMapper.selectById(execution.getWorkOrderId());
        executionTimeValidator.validate(order, List.of(execution), Set.of(execution.getId()));
    }

    private WorkOrderProcessExecution requireEditable(Long executionId) {
        WorkOrderProcessExecution execution = executionService.getById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("工序执行记录不存在");
        }
        if (isDone(execution)) {
            throw new IllegalArgumentException("该工序已在 PC 填写完成或已提交，PDA 端不可修改");
        }
        return execution;
    }

    private Equipment requireEquipment(Integer equipmentId) {
        if (equipmentId == null) {
            throw new IllegalArgumentException("请选择使用设备");
        }
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null || Integer.valueOf(1).equals(equipment.getIsDeleted())) {
            throw new IllegalArgumentException("设备不存在或已删除");
        }
        if (!EquipmentStatus.isUsable(equipment.getEquipmentStatus())) {
            throw new IllegalArgumentException("设备「" + equipment.getEquipmentName() + "」当前状态为「"
                    + equipment.getEquipmentStatus() + "」，不可使用");
        }
        return equipment;
    }

    private void validateCommon(WorkOrderProcessExecution execution,
                                PdaExecutionSaveRequest request, boolean submit) {
        LocalDateTime now = nowMinute();
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (end != null && start == null) {
            throw new IllegalArgumentException("填写结束时间时必须同时填写开始时间");
        }
        if (start != null && start.isAfter(now)) {
            throw new IllegalArgumentException("开始时间不得晚于当前时间");
        }
        if (end != null && end.isAfter(now)) {
            throw new IllegalArgumentException("结束时间不得晚于当前时间");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (Boolean.TRUE.equals(request.getManualTime())) {
            if (!StringUtils.hasText(request.getManualReason())) {
                throw new IllegalArgumentException("手填时间必须填写原因");
            }
            if (request.getManualReason().length() > 200) {
                throw new IllegalArgumentException("手填原因不能超过 200 字");
            }
        }
        if (request.getRemark() != null && request.getRemark().length() > 200) {
            throw new IllegalArgumentException("备注不能超过 200 字");
        }
        if (request.getProcessQty() != null) {
            if (request.getProcessQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("加工数量必须大于 0");
            }
            if (request.getProcessQty().stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("加工数量最多保留两位小数");
            }
        }

        Equipment equipment = null;
        if (start != null) {
            equipment = requireEquipment(request.getEquipmentId());
            if (request.getRoomId() == null) {
                request.setRoomId(equipment.getRoomId());
            } else if (equipment.getRoomId() != null
                    && !request.getRoomId().equals(equipment.getRoomId())) {
                throw new IllegalArgumentException("所选设备不属于当前配置室");
            }
            assertEquipmentFree(execution, equipment, start,
                    end == null ? now : end);
        }

        if (submit) {
            if (start == null || end == null) {
                throw new IllegalArgumentException("开始与结束时间都填写后才能提交");
            }
            if (equipment == null) {
                throw new IllegalArgumentException("请选择使用设备");
            }
            if (request.getProcessQty() == null) {
                throw new IllegalArgumentException("请填写加工数量");
            }
            if (CollectionUtils.isEmpty(request.getParticipants())) {
                throw new IllegalArgumentException("请至少选择一名操作人");
            }
            BigDecimal sum = BigDecimal.ZERO;
            Set<Long> personnelIds = new HashSet<>();
            for (PdaParticipantRequest participant : request.getParticipants()) {
                if (participant.getPersonnelId() == null
                        || !personnelIds.add(participant.getPersonnelId())) {
                    throw new IllegalArgumentException("操作人不能为空或重复");
                }
                if (participant.getCompletedQty() == null) {
                    throw new IllegalArgumentException("请填写每位操作人的完成数量");
                }
                if (participant.getCompletedQty().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("操作人完成数量不能为负");
                }
                if (participant.getCompletedQty().stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("操作人完成数量最多保留两位小数");
                }
                sum = sum.add(participant.getCompletedQty());
            }
            if (sum.compareTo(request.getProcessQty()) != 0) {
                throw new IllegalArgumentException("逐人完成数量合计 " + sum.toPlainString()
                        + " 必须等于加工数量 " + request.getProcessQty().toPlainString());
            }
        }
    }

    private void applyFields(WorkOrderProcessExecution execution, PdaExecutionSaveRequest request) {
        execution.setEquipmentId(request.getEquipmentId());
        execution.setRoomId(request.getRoomId());
        execution.setStartTime(request.getStartTime());
        execution.setEndTime(request.getEndTime());
        execution.setManualTime(Boolean.TRUE.equals(request.getManualTime()) ? 1 : 0);
        execution.setManualReason(Boolean.TRUE.equals(request.getManualTime())
                ? request.getManualReason() : null);
        execution.setProcessQty(request.getProcessQty());
        execution.setRemark(request.getRemark());
        execution.setStatus(request.getStartTime() == null
                ? "待执行" : request.getEndTime() == null ? "执行中" : "已完成");
        setOperator(execution);
    }

    private void setOperator(WorkOrderProcessExecution execution) {
        Long userId = EntityUtils.getCurrentUserId();
        execution.setOperatorId(userId);
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user != null) {
            execution.setOperatorName(user.getUserName());
        }
    }

    private void assertEquipmentFree(WorkOrderProcessExecution execution, Equipment equipment,
                                     LocalDateTime start, LocalDateTime end) {
        List<WorkOrderProcessExecution> others =
                executionMapper.selectByEquipmentIdWithDetails(equipment.getEquipmentId());
        for (WorkOrderProcessExecution other : others) {
            if (Objects.equals(other.getId(), execution.getId())) {
                continue;
            }
            if (other.getStartTime() == null) {
                continue;
            }
            if (other.getEndTime() == null) {
                throw new IllegalArgumentException("设备「" + equipment.getEquipmentName()
                        + "」正在被 " + describeExecution(other) + " 占用（"
                        + format(other.getStartTime()) + " 开始，未结束）");
            }
            if (start.isBefore(other.getEndTime()) && other.getStartTime().isBefore(end)) {
                throw new IllegalArgumentException("设备「" + equipment.getEquipmentName()
                        + "」已被 " + describeExecution(other) + " 占用（"
                        + format(other.getStartTime()) + "–" + format(other.getEndTime()) + "）");
            }
        }
    }

    private String describeExecution(WorkOrderProcessExecution execution) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(execution.getWorkOrderCode())) {
            sb.append(execution.getWorkOrderCode()).append(' ');
        }
        if (StringUtils.hasText(execution.getPreparationName())) {
            sb.append(execution.getPreparationName()).append(" · ");
        }
        sb.append(safeProcessName(execution));
        return sb.toString();
    }

    private String safeProcessName(WorkOrderProcessExecution execution) {
        if (StringUtils.hasText(execution.getProcessTypeName())) {
            return execution.getProcessTypeName();
        }
        return "未命名工序";
    }

    private void saveParticipants(WorkOrderProcessExecution execution,
                                  List<PdaParticipantRequest> participants, boolean submit) {
        if (participants == null) {
            return;
        }
        participantMapper.physicalDeleteByExecutionId(execution.getId());
        if (participants.isEmpty()) {
            return;
        }
        BigDecimal workHours = durationHours(execution.getStartTime(), execution.getEndTime());
        boolean primarySet = participants.stream()
                .anyMatch(p -> Integer.valueOf(1).equals(p.getIsPrimary()));
        for (int i = 0; i < participants.size(); i++) {
            PdaParticipantRequest request = participants.get(i);
            PersonnelFile personnel = personnelFileMapper.selectById(request.getPersonnelId());
            if (personnel == null || Integer.valueOf(1).equals(personnel.getIsDeleted())) {
                throw new IllegalArgumentException("操作人不存在或已删除");
            }
            if (!Integer.valueOf(1).equals(personnel.getStatus())) {
                throw new IllegalArgumentException("操作人「" + personnel.getName() + "」不是在职业人员");
            }
            if (submit && request.getCompletedQty() == null) {
                throw new IllegalArgumentException("请填写每位操作人的完成数量");
            }
            WorkOrderProcessExecutionParticipant participant =
                    new WorkOrderProcessExecutionParticipant();
            participant.setExecutionId(execution.getId());
            participant.setPersonnelId(request.getPersonnelId());
            participant.setCompletedQty(request.getCompletedQty());
            participant.setWorkHours(workHours);
            participant.setIsPrimary(!primarySet && i == 0 ? 1
                    : (Integer.valueOf(1).equals(request.getIsPrimary()) ? 1 : 0));
            participant.setIsDeleted(0);
            participant.setVersion(0);
            participantMapper.insert(participant);
        }
    }

    private BigDecimal durationHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Math.max(Duration.between(start, end).toMinutes(), 0);
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private PdaProcessDto loadProcess(WorkOrderProcessExecution execution) {
        List<WorkOrderProcessExecution> refreshed =
                executionService.getByWorkOrderId(execution.getWorkOrderId());
        WorkOrderProcessExecution saved = refreshed.stream()
                .filter(e -> e.getId().equals(execution.getId()))
                .findFirst()
                .orElse(execution);
        return toProcessDto(saved);
    }

    private LocalDateTime nowMinute() {
        return LocalDateTime.now().withSecond(0).withNano(0);
    }

    private String format(LocalDateTime time) {
        return time == null ? "—" : time.format(TIME_FORMATTER);
    }

    // endregion
}

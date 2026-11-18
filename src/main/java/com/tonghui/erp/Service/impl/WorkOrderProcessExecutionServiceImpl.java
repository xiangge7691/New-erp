package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.utils.EquipmentUsageConflictValidator;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.PerformanceGenerationService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单工序执行记录服务实现类
 */
@Service
public class WorkOrderProcessExecutionServiceImpl extends ServiceImpl<WorkOrderProcessExecutionMapper, WorkOrderProcessExecution>
        implements WorkOrderProcessExecutionService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 设备使用冲突校验器 */
    @Autowired
    private EquipmentUsageConflictValidator equipmentUsageConflictValidator;

    /** 实际参与人 Mapper */
    @Autowired
    private WorkOrderProcessExecutionParticipantMapper participantMapper;

    /** 排产批次 Mapper */
    @Autowired
    private WorkOrderProcessPlanBatchMapper planBatchMapper;

    /** 排产计划 Mapper */
    @Autowired
    private WorkOrderProcessPlanMapper planMapper;

    /** 绩效记录 Mapper */
    @Autowired
    private PerformanceRecordMapper performanceRecordMapper;

    /** 绩效生成服务 */
    @Autowired
    private PerformanceGenerationService performanceGenerationService;

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据工单ID查询工序执行记录（含实际参与人）
     *
     * @param workOrderId 工单ID
     * @return 工序执行记录列表
     */
    @Override
    public List<WorkOrderProcessExecution> getByWorkOrderId(Long workOrderId) {
        List<WorkOrderProcessExecution> executions = baseMapper.selectByWorkOrderIdWithDetails(workOrderId);
        executions.forEach(execution -> execution.setStatus(ProcessExecutionStatus.resolve(
                execution.getStartTime(), execution.getEndTime())));
        executions.forEach(execution -> {
            // 历史数据可能只填了结束时间、未走完工锁定：按实际结束时间视为已完工
            if (execution.getCompletedTime() == null) {
                execution.setCompletedTime(execution.getEndTime());
            }
            if (execution.getEndTime() != null) {
                execution.setStatus(ProcessExecutionStatus.COMPLETED);
            }
        });
        loadParticipants(executions);
        enrichPlanInfo(executions);
        enrichPerformanceCount(executions);
        return executions;
    }

    /**
     * 查询已完工（锁定）的工序执行记录
     *
     * @param from        完工日期起（可选）
     * @param to          完工日期止（可选）
     * @param workOrderId 工单ID（可选）
     * @return 已完工记录列表（含参与人、计划信息与生成绩效数）
     */
    @Override
    public List<WorkOrderProcessExecution> listCompleted(LocalDate from, LocalDate to, Long workOrderId) {
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        List<WorkOrderProcessExecution> executions =
                baseMapper.selectCompletedWithDetails(workOrderId, fromTime, toTime);
        if (CollectionUtils.isEmpty(executions)) {
            return executions;
        }
        loadParticipants(executions);
        enrichPlanInfo(executions);
        enrichPerformanceCount(executions);
        return executions;
    }

    /**
     * 批量加载实际参与人
     *
     * @param executions 执行记录列表
     */
    private void loadParticipants(List<WorkOrderProcessExecution> executions) {
        if (CollectionUtils.isEmpty(executions)) {
            return;
        }
        List<Long> ids = executions.stream().map(WorkOrderProcessExecution::getId).toList();
        Map<Long, List<WorkOrderProcessExecutionParticipant>> map =
                participantMapper.selectByExecutionIdsWithPersonnel(ids).stream()
                        .collect(Collectors.groupingBy(WorkOrderProcessExecutionParticipant::getExecutionId));
        executions.forEach(e -> e.setParticipants(map.getOrDefault(e.getId(), List.of())));
    }

    /**
     * 填充执行记录关联的排产计划与批次信息
     * <p>批次优先；未拆批次时回退到工序计划</p>
     *
     * @param executions 执行记录列表
     */
    private void enrichPlanInfo(List<WorkOrderProcessExecution> executions) {
        if (CollectionUtils.isEmpty(executions)) {
            return;
        }
        Set<Long> planIds = executions.stream().map(WorkOrderProcessExecution::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> batchIds = executions.stream().map(WorkOrderProcessExecution::getPlanBatchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WorkOrderProcessPlan> plans = planIds.isEmpty() ? Map.of()
                : planMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(WorkOrderProcessPlan::getId, e -> e, (a, b) -> a));
        Map<Long, WorkOrderProcessPlanBatch> batches = batchIds.isEmpty() ? Map.of()
                : planBatchMapper.selectBatchIds(batchIds).stream()
                .collect(Collectors.toMap(WorkOrderProcessPlanBatch::getId, e -> e, (a, b) -> a));
        for (WorkOrderProcessExecution execution : executions) {
            WorkOrderProcessPlan plan = execution.getPlanId() == null ? null : plans.get(execution.getPlanId());
            WorkOrderProcessPlanBatch batch = execution.getPlanBatchId() == null
                    ? null : batches.get(execution.getPlanBatchId());
            if (batch != null) {
                execution.setPlanBatchName(batch.getBatchName());
                execution.setPlanStartTime(batch.getPlanStartTime() != null
                        ? batch.getPlanStartTime() : plan == null ? null : plan.getPlanStartTime());
                execution.setPlanEndTime(batch.getPlanEndTime() != null
                        ? batch.getPlanEndTime() : plan == null ? null : plan.getPlanEndTime());
                execution.setPlanDurationMinutes(batch.getPlanDurationMinutes() != null
                        ? batch.getPlanDurationMinutes() : plan == null ? null : plan.getPlanDurationMinutes());
                execution.setPlanQty(batch.getPlanQty() != null
                        ? batch.getPlanQty() : plan == null ? null : plan.getPlanQty());
            } else if (plan != null) {
                execution.setPlanStartTime(plan.getPlanStartTime());
                execution.setPlanEndTime(plan.getPlanEndTime());
                execution.setPlanDurationMinutes(plan.getPlanDurationMinutes());
                execution.setPlanQty(plan.getPlanQty());
            }
        }
    }

    /**
     * 统计每条执行记录已生成的绩效记录数
     *
     * @param executions 执行记录列表
     */
    private void enrichPerformanceCount(List<WorkOrderProcessExecution> executions) {
        if (CollectionUtils.isEmpty(executions)) {
            return;
        }
        List<Long> ids = executions.stream().map(WorkOrderProcessExecution::getId).toList();
        List<Map<String, Object>> rows = performanceRecordMapper.selectMaps(
                new QueryWrapper<PerformanceRecord>()
                        .select("execution_id", "COUNT(*) AS cnt")
                        .in("execution_id", ids)
                        .eq("record_status", 1)
                        .groupBy("execution_id"));
        Map<Long, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object executionId = row.get("execution_id");
            Object cnt = row.get("cnt");
            if (executionId != null && cnt != null) {
                counts.put(((Number) executionId).longValue(), ((Number) cnt).intValue());
            }
        }
        executions.forEach(e -> e.setPerformanceRecordCount(counts.getOrDefault(e.getId(), 0)));
    }

    // endregion

    // region 批量保存
    // ===================================
    // 批量保存
    // ===================================

    /**
     * 批量保存工序执行记录与实际参与人
     *
     * @param workOrderId 工单ID
     * @param executions  工序执行记录列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions) {
        // 1. 设备使用冲突校验（必须在任何写库操作之前）
        equipmentUsageConflictValidator.validate(workOrderId, executions);

        // 2. 查询该工单现有记录（用于保留ID、锁定校验与计算被移除行）
        Map<Long, WorkOrderProcessExecution> existingMap = this.baseMapper.selectList(
                        new QueryWrapper<WorkOrderProcessExecution>().eq("work_order_id", workOrderId))
                .stream()
                .collect(Collectors.toMap(WorkOrderProcessExecution::getId, e -> e, (a, b) -> a));
        Set<Long> incomingIds = new HashSet<>();
        List<WorkOrderProcessExecution> toInsert = new ArrayList<>();

        if (executions != null && !executions.isEmpty()) {
            for (WorkOrderProcessExecution execution : executions) {
                execution.setWorkOrderId(workOrderId);
                execution.setIsDeleted(0);
                execution.setStatus(ProcessExecutionStatus.resolve(
                        execution.getStartTime(), execution.getEndTime()));
                if (execution.getId() != null && existingMap.containsKey(execution.getId())) {
                    WorkOrderProcessExecution old = existingMap.get(execution.getId());
                    if (Integer.valueOf(1).equals(old.getLocked())) {
                        throw new IllegalArgumentException("工序【" + old.getStepOrder() + "】已完工锁定，不能修改");
                    }
                    incomingIds.add(execution.getId());
                    this.updateById(execution);
                    // 显式清空可能为空的字段，避免 MyBatis-Plus 忽略 null
                    this.lambdaUpdate()
                            .eq(WorkOrderProcessExecution::getId, execution.getId())
                            .set(WorkOrderProcessExecution::getPlanId, execution.getPlanId())
                            .set(WorkOrderProcessExecution::getPlanBatchId, execution.getPlanBatchId())
                            .set(WorkOrderProcessExecution::getBatchNo, execution.getBatchNo())
                            .set(WorkOrderProcessExecution::getTeamId, execution.getTeamId())
                            .set(WorkOrderProcessExecution::getRoomId, execution.getRoomId())
                            .set(WorkOrderProcessExecution::getEquipmentId, execution.getEquipmentId())
                            .set(WorkOrderProcessExecution::getStartTime, execution.getStartTime())
                            .set(WorkOrderProcessExecution::getEndTime, execution.getEndTime())
                            .update();
                    saveParticipants(execution);
                } else {
                    execution.setId(null);
                    execution.setVersion(1);
                    execution.setLocked(0);
                    execution.setCompletedTime(null);
                    toInsert.add(execution);
                }
            }
            for (WorkOrderProcessExecution execution : toInsert) {
                this.save(execution);
                saveParticipants(execution);
            }
        }

        // 3. 被移除的既有行：已锁定的不允许删除，其余物理删除
        List<Long> removedIds = existingMap.keySet().stream()
                .filter(id -> !incomingIds.contains(id))
                .toList();
        for (Long removedId : removedIds) {
            WorkOrderProcessExecution old = existingMap.get(removedId);
            if (Integer.valueOf(1).equals(old.getLocked())) {
                throw new IllegalArgumentException("工序【" + old.getStepOrder() + "】已完工锁定，不能删除");
            }
            participantMapper.physicalDeleteByExecutionId(removedId);
            baseMapper.physicalDeleteById(removedId);
        }
    }

    // endregion

    // region 完工
    // ===================================
    // 完工
    // ===================================

    /**
     * 工序/批次完工：锁定执行记录并生成个人绩效
     *
     * @param executionId 工序执行记录ID
     * @return 完工后的执行记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderProcessExecution complete(Long executionId) {
        WorkOrderProcessExecution execution = this.getById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("工序执行记录不存在");
        }
        if (Integer.valueOf(1).equals(execution.getLocked())) {
            throw new IllegalArgumentException("该工序已完工锁定");
        }
        if (execution.getStartTime() == null) {
            throw new IllegalArgumentException("请先填写实际开始时间");
        }
        LocalDateTime end = execution.getEndTime() != null ? execution.getEndTime() : LocalDateTime.now();
        if (!end.isAfter(execution.getStartTime())) {
            end = execution.getStartTime().plusMinutes(1);
        }
        execution.setEndTime(end);
        execution.setStatus(ProcessExecutionStatus.COMPLETED);
        execution.setLocked(1);
        execution.setCompletedTime(end);
        this.updateById(execution);
        this.lambdaUpdate()
                .eq(WorkOrderProcessExecution::getId, executionId)
                .set(WorkOrderProcessExecution::getEndTime, end)
                .set(WorkOrderProcessExecution::getStatus, ProcessExecutionStatus.COMPLETED)
                .set(WorkOrderProcessExecution::getLocked, 1)
                .set(WorkOrderProcessExecution::getCompletedTime, end)
                .update();
        ensurePrimaryParticipant(execution);
        long participantCount = participantMapper.selectCount(
                new QueryWrapper<WorkOrderProcessExecutionParticipant>().eq("execution_id", executionId));
        if (participantCount == 0) {
            throw new IllegalArgumentException("未登记参与人，无法生成绩效；请先登记参与人");
        }
        performanceGenerationService.generateForExecution(executionId);
        if (execution.getPlanBatchId() != null) {
            WorkOrderProcessPlanBatch batch = planBatchMapper.selectById(execution.getPlanBatchId());
            if (batch != null) {
                batch.setStatus("已完工");
                planBatchMapper.updateById(batch);
            }
        }
        execution.setParticipants(participantMapper.selectByExecutionIdsWithPersonnel(List.of(executionId)));
        return execution;
    }

    /**
     * 没有显式参与人时，按主操作人补一条参与人记录
     */
    private void ensurePrimaryParticipant(WorkOrderProcessExecution execution) {
        if (execution.getOperatorId() == null) {
            return;
        }
        long count = participantMapper.selectCount(
                new QueryWrapper<WorkOrderProcessExecutionParticipant>()
                        .eq("execution_id", execution.getId()));
        if (count > 0) {
            return;
        }
        WorkOrderProcessExecutionParticipant participant = new WorkOrderProcessExecutionParticipant();
        participant.setExecutionId(execution.getId());
        participant.setPersonnelId(execution.getOperatorId());
        participant.setWorkHours(durationHours(execution.getStartTime(), execution.getEndTime()));
        participant.setCompletedQty(execution.getProcessQty());
        participant.setIsPrimary(1);
        participant.setIsDeleted(0);
        participant.setVersion(0);
        participantMapper.insert(participant);
    }

    // endregion

    // region 参与人
    // ===================================
    // 参与人
    // ===================================

    /**
     * 保存执行记录的参与人（显式传入时全量覆盖）
     *
     * @param execution 执行记录
     */
    private void saveParticipants(WorkOrderProcessExecution execution) {
        List<WorkOrderProcessExecutionParticipant> participants = execution.getParticipants();
        if (participants == null) {
            ensurePrimaryParticipant(execution);
            return;
        }
        if (participants.isEmpty()) {
            participantMapper.physicalDeleteByExecutionId(execution.getId());
            return;
        }
        Set<Long> personnelIds = new HashSet<>();
        BigDecimal qtySum = BigDecimal.ZERO;
        boolean allQtyPresent = true;
        for (WorkOrderProcessExecutionParticipant participant : participants) {
            if (participant.getPersonnelId() == null) {
                throw new IllegalArgumentException("参与人必须关联人员档案");
            }
            if (!personnelIds.add(participant.getPersonnelId())) {
                throw new IllegalArgumentException("参与人不能重复");
            }
            if (participant.getWorkHours() == null || participant.getWorkHours().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("参与人工作时间必须大于 0");
            }
            if (participant.getCompletedQty() == null) {
                allQtyPresent = false;
            } else {
                if (participant.getCompletedQty().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("参与人完成数量不能为负");
                }
                qtySum = qtySum.add(participant.getCompletedQty());
            }
        }
        if (execution.getProcessQty() != null) {
            if (!allQtyPresent) {
                throw new IllegalArgumentException("请填写每位参与人的完成数量");
            }
            if (qtySum.compareTo(execution.getProcessQty()) != 0) {
                throw new IllegalArgumentException("参与人完成数量合计 " + qtySum
                        + " 必须等于实际数量 " + execution.getProcessQty());
            }
        }
        participantMapper.physicalDeleteByExecutionId(execution.getId());
        for (WorkOrderProcessExecutionParticipant participant : participants) {
            participant.setId(null);
            participant.setExecutionId(execution.getId());
            participant.setIsDeleted(0);
            participant.setVersion(0);
            participantMapper.insert(participant);
        }
    }

    private BigDecimal durationHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(start, end).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
    }

    // endregion
}

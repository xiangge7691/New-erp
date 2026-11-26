package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Constants.BizFileType;
import com.tonghui.erp.Common.Constants.PlanBatchStatus;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.utils.EquipmentUsageConflictValidator;
import com.tonghui.erp.Common.utils.ExecutionRecordMatcher;
import com.tonghui.erp.Common.utils.WorkOrderExecutionTimeValidator;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.PerformanceGenerationService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    /** 实际执行时间校验器（任务时间窗 + 工序先后） */
    @Autowired
    private WorkOrderExecutionTimeValidator executionTimeValidator;

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

    /** 业务附件服务（删除执行行时清理照片与附件关联） */
    @Autowired
    private BusinessFileService businessFileService;

    /** 工单 Mapper（读取制剂ID，用于补齐工序关键工艺参数） */
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /** 制剂工序模板 Mapper（按制剂+工序类型+工序顺序补齐关键工艺参数） */
    @Autowired
    private PreparationProcessTemplateMapper templateMapper;

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
        // 早期 PDA 录入的执行记录可能没有 plan_id：按「工单+工序类型+工序顺序」兜底匹配计划，
        // 保证 PDA 显示的计划数量与排产接口一致（全部已关联时跳过查询）
        boolean hasUnlinked = executions.stream().anyMatch(e -> e.getPlanId() == null);
        Set<Long> workOrderIds = hasUnlinked
                ? executions.stream().map(WorkOrderProcessExecution::getWorkOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet())
                : Set.of();
        Map<String, WorkOrderProcessPlan> plansByProcess = workOrderIds.isEmpty() ? Map.of()
                : planMapper.selectList(new LambdaQueryWrapper<WorkOrderProcessPlan>()
                .in(WorkOrderProcessPlan::getWorkOrderId, workOrderIds))
                .stream().collect(Collectors.toMap(
                        p -> planProcessKey(p.getWorkOrderId(), p.getProcessTypeId(), p.getStepOrder()),
                        e -> e, (a, b) -> a));
        for (WorkOrderProcessExecution execution : executions) {
            WorkOrderProcessPlan plan = execution.getPlanId() == null ? null : plans.get(execution.getPlanId());
            if (plan == null) {
                plan = plansByProcess.get(planProcessKey(execution.getWorkOrderId(),
                        execution.getProcessTypeId(), execution.getStepOrder()));
                if (plan != null && execution.getPlanId() == null) {
                    // 仅补齐返回给前端的计划关联，落库关联由保存实际记录时回填
                    execution.setPlanId(plan.getId());
                }
            }
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
     * <p>
     * 兼容两类调用：生产任务详情页一次性回传全部工序行（带 id），以及「实际生产」按工序/批次
     * 只回传当前行（不带 id，仅带 planId/planBatchId 等定位字段）。后者的缺失字段按排产批次、
     * 工序计划与制剂工序模板补齐，并且只替换该工序/批次的既有记录，不会删除其他工序。
     * </p>
     * <p>
     * 定位规则：带 id 时按主键更新，但 id 的批次必须与本次提交的 planBatchId 一致，
     * 否则视为客户端误传同工序其它批次的 executionId，改按「计划/批次」业务键重新定位，
     * 避免多批次互相覆盖。更新时请求未携带的实际执行数据沿用原值。
     * </p>
     *
     * @param workOrderId 工单ID
     * @param executions  工序执行记录列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions) {
        // 1. 校验工单，读取制剂ID（用于补齐关键工艺参数）
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new IllegalArgumentException("工单不存在");
        }

        // 2. 查询该工单现有记录（用于保留ID、锁定校验、业务键匹配与补齐字段）
        List<WorkOrderProcessExecution> existingList = this.baseMapper.selectList(
                new QueryWrapper<WorkOrderProcessExecution>().eq("work_order_id", workOrderId));
        Map<Long, WorkOrderProcessExecution> existingMap = existingList.stream()
                .collect(Collectors.toMap(WorkOrderProcessExecution::getId, e -> e, (a, b) -> a));

        List<WorkOrderProcessExecution> rows = executions == null ? List.of() : executions;
        Set<Long> incomingIds = new HashSet<>();
        Set<Long> matchedIds = new HashSet<>();
        Set<Long> affectedBatchIds = new HashSet<>();
        List<WorkOrderProcessExecution> toInsert = new ArrayList<>();
        List<WorkOrderProcessExecution> toUpdate = new ArrayList<>();

        // 3. 逐行定位既有记录并补齐计划/批次/模板字段
        for (WorkOrderProcessExecution execution : rows) {
            execution.setWorkOrderId(workOrderId);
            execution.setIsDeleted(0);
            WorkOrderProcessPlanBatch batch = execution.getPlanBatchId() == null
                    ? null : planBatchMapper.selectById(execution.getPlanBatchId());
            if (batch != null && execution.getPlanId() == null) {
                execution.setPlanId(batch.getPlanId());
            }
            WorkOrderProcessExecution existing = ExecutionRecordMatcher.resolve(
                    execution, existingList, existingMap, matchedIds);
            boolean businessKeyMatch = existing != null && execution.getId() == null;
            if (businessKeyMatch) {
                execution.setId(existing.getId());
            }
            enrichExecution(execution, existing, batch, order.getPreparationId());
            execution.setStatus(ProcessExecutionStatus.resolve(
                    execution.getStartTime(), execution.getEndTime()));
            if (existing != null) {
                if (Integer.valueOf(1).equals(existing.getLocked())) {
                    throw new IllegalArgumentException("工序【" + existing.getStepOrder() + "】已完工锁定，不能修改");
                }
                // 记录换批次/换归属时，原批次状态也要重算，避免残留「执行中/已完工」
                if (existing.getPlanBatchId() != null) {
                    affectedBatchIds.add(existing.getPlanBatchId());
                }
                incomingIds.add(existing.getId());
                matchedIds.add(existing.getId());
                toUpdate.add(execution);
            } else {
                execution.setId(null);
                execution.setVersion(1);
                execution.setLocked(0);
                execution.setCompletedTime(null);
                toInsert.add(execution);
            }
        }
        // 本次提交涉及的目标批次同样需要刷新状态
        rows.stream().map(WorkOrderProcessExecution::getPlanBatchId)
                .filter(Objects::nonNull).forEach(affectedBatchIds::add);

        // 4. 设备使用冲突校验（必须在任何写库操作之前；此时设备与时间字段已补齐）
        equipmentUsageConflictValidator.validate(workOrderId, rows);

        // 5. 实际时间校验：任务配置时间窗 + 工序先后（同样必须在写库之前）
        Set<Long> overwrittenIds = toUpdate.stream().map(WorkOrderProcessExecution::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        executionTimeValidator.validate(order, rows, overwrittenIds);

        // 6. 更新与新增
        for (WorkOrderProcessExecution execution : toUpdate) {
            this.updateById(execution);
            // 显式更新可能为空的字段，避免 MyBatis-Plus 忽略 null
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
            // PC 批量保存把起止时间填齐同样视为完工，需与 PDA 提交、完工锁定统一生成绩效口径
            WorkOrderProcessExecution before = existingMap.get(execution.getId());
            generateForCompleted(execution,
                    before != null && ProcessExecutionStatus.COMPLETED.equals(before.getStatus()));
        }
        for (WorkOrderProcessExecution execution : toInsert) {
            this.save(execution);
            saveParticipants(execution);
            // 新增行直接以已完成状态落库时，同样要求登记参与人并生成绩效
            generateForCompleted(execution, false);
        }

        // 7. 被本次提交替换的既有行：仅删除同一工序/批次的旧记录
        Set<Long> incomingBatchIds = rows.stream().map(WorkOrderProcessExecution::getPlanBatchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> incomingBatchNos = rows.stream().map(WorkOrderProcessExecution::getBatchNo)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> incomingPlanIds = rows.stream().map(WorkOrderProcessExecution::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> incomingProcessKeys = rows.stream()
                .filter(e -> e.getProcessTypeId() != null && e.getStepOrder() != null)
                .map(e -> ExecutionRecordMatcher.processKey(e.getProcessTypeId(), e.getStepOrder()))
                .collect(Collectors.toSet());
        for (WorkOrderProcessExecution old : existingList) {
            if (incomingIds.contains(old.getId())
                    || !ExecutionRecordMatcher.isReplacedByIncoming(old, incomingBatchIds, incomingBatchNos,
                            incomingPlanIds, incomingProcessKeys)) {
                continue;
            }
            // 已锁定、已 PDA 提交或已生成绩效的旧行不允许被替换删除，避免绩效与执行数据悬挂
            assertDeletable(old);
            if (old.getPlanBatchId() != null) {
                affectedBatchIds.add(old.getPlanBatchId());
            }
            participantMapper.physicalDeleteByExecutionId(old.getId());
            baseMapper.physicalDeleteById(old.getId());
        }

        // 8. 按最新实际执行记录刷新涉及批次的状态（计划中/执行中/已完工）
        affectedBatchIds.forEach(this::refreshPlanBatchStatus);
    }

    // endregion

    // region 工序字段补齐
    // ===================================
    // 工序字段补齐
    // ===================================

    /**
     * 按工单+工序类型+工序顺序匹配排产计划
     * <p>「工单工序执行」页保存行时通常不带 planId，用该匹配把执行记录挂回计划行，
     * 使绩效记录的 planId 不为空，工序粒度视图才能显示计划起止与计划数量。</p>
     *
     * @param execution 待保存的执行记录
     * @return 匹配到的排产计划，未匹配返回 null
     */
    private WorkOrderProcessPlan findPlanByProcess(WorkOrderProcessExecution execution) {
        if (execution.getWorkOrderId() == null || execution.getProcessTypeId() == null
                || execution.getStepOrder() == null) {
            return null;
        }
        List<WorkOrderProcessPlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<WorkOrderProcessPlan>()
                        .eq(WorkOrderProcessPlan::getWorkOrderId, execution.getWorkOrderId())
                        .eq(WorkOrderProcessPlan::getProcessTypeId, execution.getProcessTypeId())
                        .eq(WorkOrderProcessPlan::getStepOrder, execution.getStepOrder())
                        .last("LIMIT 1"));
        return plans.isEmpty() ? null : plans.get(0);
    }

    /**
     * 补齐执行行的计划/批次/模板字段
     * <p>
     * 前端「实际生产」可能只传 planId/planBatchId，工序顺序、配置室、使用设备、关键工艺参数、
     * 加工数量需从排产批次、工序计划与制剂工序模板回填；更新既有行时，请求未携带的
     * 实际执行数据一律沿用原值，避免「选人」等局部保存把已登记的实际起止清空。
     * </p>
     *
     * @param execution   待保存行
     * @param existing    匹配到的既有记录（可为空）
     * @param batch       已解析的排产批次（可为空）
     * @param preparationId 制剂ID
     */
    private void enrichExecution(WorkOrderProcessExecution execution,
                                 WorkOrderProcessExecution existing,
                                 WorkOrderProcessPlanBatch batch,
                                 Long preparationId) {
        // 3.1 既有记录优先：关联字段与模板带入字段缺失时沿用原值
        if (existing != null) {
            if (execution.getPlanId() == null) {
                execution.setPlanId(existing.getPlanId());
            }
            if (execution.getPlanBatchId() == null) {
                execution.setPlanBatchId(existing.getPlanBatchId());
            }
            if (execution.getBatchNo() == null) {
                execution.setBatchNo(existing.getBatchNo());
            }
            if (execution.getProcessTypeId() == null) {
                execution.setProcessTypeId(existing.getProcessTypeId());
            }
            if (execution.getStepOrder() == null) {
                execution.setStepOrder(existing.getStepOrder());
            }
            if (execution.getRoomId() == null) {
                execution.setRoomId(existing.getRoomId());
            }
            if (execution.getEquipmentId() == null) {
                execution.setEquipmentId(existing.getEquipmentId());
            }
            if (execution.getTeamId() == null) {
                execution.setTeamId(existing.getTeamId());
            }
            if (execution.getUnitId() == null) {
                execution.setUnitId(existing.getUnitId());
            }
            if (!StringUtils.hasText(execution.getKeyProcessParams())) {
                execution.setKeyProcessParams(existing.getKeyProcessParams());
            }
            // 实际执行数据未携带时沿用原值，避免局部保存（如只改参与人）清空已登记的执行信息
            if (execution.getOperatorId() == null) {
                execution.setOperatorId(existing.getOperatorId());
            }
            if (!StringUtils.hasText(execution.getOperatorName())) {
                execution.setOperatorName(existing.getOperatorName());
            }
            if (execution.getStartTime() == null) {
                execution.setStartTime(existing.getStartTime());
            }
            if (execution.getEndTime() == null) {
                execution.setEndTime(existing.getEndTime());
            }
            if (execution.getProcessQty() == null) {
                execution.setProcessQty(existing.getProcessQty());
            }
            if (!StringUtils.hasText(execution.getRemark())) {
                execution.setRemark(existing.getRemark());
            }
        }

        // 3.2 批次兜底
        if (batch != null) {
            if (execution.getPlanId() == null) {
                execution.setPlanId(batch.getPlanId());
            }
            if (execution.getBatchNo() == null) {
                execution.setBatchNo(batch.getBatchNo());
            }
        }

        // 3.3 工序计划兜底：未携带 planId 时按工单+工序+顺序匹配排产计划，
        // 保证生成绩效后工序粒度视图能带出计划起止与计划数量
        WorkOrderProcessPlan plan = execution.getPlanId() == null
                ? findPlanByProcess(execution) : planMapper.selectById(execution.getPlanId());
        if (plan != null && execution.getPlanId() == null) {
            execution.setPlanId(plan.getId());
        }
        if (plan != null) {
            if (execution.getProcessTypeId() == null) {
                execution.setProcessTypeId(plan.getProcessTypeId());
            }
            if (execution.getStepOrder() == null) {
                execution.setStepOrder(plan.getStepOrder());
            }
        }

        // 3.4 配置室/设备/班组/数量：批次优先，其次工序计划
        if (execution.getRoomId() == null) {
            execution.setRoomId(batch != null && batch.getRoomId() != null
                    ? batch.getRoomId() : plan == null ? null : plan.getRoomId());
        }
        if (execution.getEquipmentId() == null) {
            execution.setEquipmentId(batch != null && batch.getEquipmentId() != null
                    ? batch.getEquipmentId() : plan == null ? null : plan.getEquipmentId());
        }
        if (execution.getTeamId() == null) {
            execution.setTeamId(batch != null && batch.getTeamId() != null
                    ? batch.getTeamId() : plan == null ? null : plan.getTeamId());
        }
        // 3.4.1 计量单位：批次优先，其次工序计划，最后工序模板
        if (execution.getUnitId() == null) {
            execution.setUnitId(batch != null && batch.getUnitId() != null
                    ? batch.getUnitId() : plan == null ? null : plan.getUnitId());
        }
        if (execution.getUnitId() == null) {
            execution.setUnitId(findTemplateUnitId(
                    preparationId, execution.getProcessTypeId(), execution.getStepOrder()));
        }
        if (execution.getProcessQty() == null && existing == null) {
            execution.setProcessQty(batch != null && batch.getPlanQty() != null
                    ? batch.getPlanQty() : plan == null ? null : plan.getPlanQty());
        }

        // 3.5 关键工艺参数：制剂工序模板（制剂 + 工序类型 + 工序顺序）
        if (!StringUtils.hasText(execution.getKeyProcessParams())) {
            execution.setKeyProcessParams(findTemplateKeyProcessParams(
                    preparationId, execution.getProcessTypeId(), execution.getStepOrder()));
        }
    }

    /**
     * 查询制剂工序模板的计量单位ID
     *
     * @param preparationId 制剂ID
     * @param processTypeId 工序类型ID
     * @param stepOrder     工序顺序
     * @return 计量单位ID；未配置返回 null
     */
    private Long findTemplateUnitId(Long preparationId, Long processTypeId, Integer stepOrder) {
        PreparationProcessTemplate template = findTemplate(preparationId, processTypeId, stepOrder);
        return template == null ? null : template.getUnitId();
    }

    /**
     * 按制剂 + 工序类型 + 工序顺序查询工序模板
     *
     * @param preparationId 制剂ID
     * @param processTypeId 工序类型ID
     * @param stepOrder     工序顺序
     * @return 工序模板；未配置返回 null
     */
    private PreparationProcessTemplate findTemplate(Long preparationId, Long processTypeId, Integer stepOrder) {
        if (preparationId == null || processTypeId == null) {
            return null;
        }
        LambdaQueryWrapper<PreparationProcessTemplate> wrapper =
                new LambdaQueryWrapper<PreparationProcessTemplate>()
                        .eq(PreparationProcessTemplate::getPreparationId, preparationId)
                        .eq(PreparationProcessTemplate::getProcessTypeId, processTypeId);
        if (stepOrder != null) {
            wrapper.eq(PreparationProcessTemplate::getStepOrder, stepOrder);
        }
        wrapper.orderByAsc(PreparationProcessTemplate::getStepOrder).last("LIMIT 1");
        return templateMapper.selectOne(wrapper);
    }

    /**
     * 查询制剂工序模板的关键工艺参数
     *
     * @param preparationId 制剂ID
     * @param processTypeId 工序类型ID
     * @param stepOrder     工序顺序
     * @return 关键工艺参数；未配置返回 null
     */
    private String findTemplateKeyProcessParams(Long preparationId, Long processTypeId, Integer stepOrder) {
        PreparationProcessTemplate template = findTemplate(preparationId, processTypeId, stepOrder);
        return template == null ? null : template.getKeyProcessParams();
    }

    /**
     * 计划兜底匹配键：工单 + 工序类型 + 工序顺序
     *
     * @param workOrderId   工单ID
     * @param processTypeId 工序类型ID
     * @param stepOrder     工序顺序
     * @return 匹配键
     */
    private String planProcessKey(Long workOrderId, Long processTypeId, Integer stepOrder) {
        return workOrderId + "#" + processTypeId + "#" + stepOrder;
    }

    // endregion

    // region 删除
    // ===================================
    // 删除
    // ===================================

    /**
     * 删除误拆的工序执行行
     * <p>
     * 已完工锁定、PDA 已提交或已生成绩效的执行行不允许删除；删除时一并清理参与人与照片附件。
     * </p>
     *
     * @param executionId 工序执行记录ID
     * @return 所属工单ID（供前端刷新列表）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long deleteExecution(Long executionId) {
        WorkOrderProcessExecution execution = this.getById(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("工序执行记录不存在");
        }
        assertDeletable(execution);
        // 先解绑附件（含照片），无其他业务引用时一并回收文件
        businessFileService.unbindAll(BizFileType.WORK_ORDER_PROCESS_EXECUTION.getCode(),
                executionId, null, null);
        participantMapper.physicalDeleteByExecutionId(executionId);
        baseMapper.physicalDeleteById(executionId);
        // 删除实际记录后重算批次状态，避免残留执行中/已完工
        refreshPlanBatchStatus(execution.getPlanBatchId());
        return execution.getWorkOrderId();
    }

    /**
     * 按关联的实际执行记录刷新排产批次状态
     * <p>
     * 批次状态由执行记录推导：无执行记录=计划中，已登记实际生产但未全部完成=执行中，
     * 关联执行全部完成=已完工。执行记录按批次ID关联，历史行未写批次ID时按批次序号兜底。
     * </p>
     *
     * @param planBatchId 排产批次ID（为空时忽略）
     */
    @Override
    public void refreshPlanBatchStatus(Long planBatchId) {
        if (planBatchId == null) {
            return;
        }
        WorkOrderProcessPlanBatch batch = planBatchMapper.selectById(planBatchId);
        if (batch == null || batch.getPlanId() == null) {
            return;
        }
        WorkOrderProcessPlan plan = planMapper.selectById(batch.getPlanId());
        if (plan == null || plan.getWorkOrderId() == null || plan.getStepOrder() == null) {
            return;
        }
        // 批次状态只看同一工单、同一工序下的执行记录，避免跨工序按批次序号误匹配
        List<WorkOrderProcessExecution> candidates = this.lambdaQuery()
                .eq(WorkOrderProcessExecution::getWorkOrderId, plan.getWorkOrderId())
                .eq(WorkOrderProcessExecution::getStepOrder, plan.getStepOrder())
                .list();
        String status = PlanBatchStatus.resolve(candidates.stream()
                .filter(e -> PlanBatchStatus.matches(batch, e))
                .toList());
        if (!status.equals(batch.getStatus())) {
            batch.setStatus(status);
            planBatchMapper.updateById(batch);
        }
    }

    /**
     * 执行记录未关联排产计划时，按「工单+工序类型+工序顺序」回填计划ID并落库
     * <p>
     * PDA 的开始/结束/暂存/提交直接更新执行记录，不走 PC 批量保存的字段补齐逻辑，
     * 这里补上计划关联，保证绩效记录与工序粒度视图能取到计划信息。
     * </p>
     *
     * @param execution 执行记录（需包含工单、工序类型与工序顺序）
     */
    @Override
    public void fillPlanReference(WorkOrderProcessExecution execution) {
        if (execution == null || execution.getId() == null || execution.getPlanId() != null) {
            return;
        }
        WorkOrderProcessPlan plan = findPlanByProcess(execution);
        if (plan == null) {
            return;
        }
        execution.setPlanId(plan.getId());
        this.lambdaUpdate()
                .eq(WorkOrderProcessExecution::getId, execution.getId())
                .set(WorkOrderProcessExecution::getPlanId, plan.getId())
                .update();
    }

    /**
     * 校验执行行是否可删除
     * <p>已完工锁定、已通过 PDA 提交或已生成绩效的执行行，删除后会造成绩效与执行数据悬挂，一律拒绝</p>
     *
     * @param execution 执行记录
     */
    private void assertDeletable(WorkOrderProcessExecution execution) {
        String label = execution.getStepOrder() == null
                ? "ID " + execution.getId() : "工序【" + execution.getStepOrder() + "】";
        if (Integer.valueOf(1).equals(execution.getLocked())) {
            throw new IllegalArgumentException(label + "已完工锁定，不能删除");
        }
        if (Integer.valueOf(1).equals(execution.getPdaSubmitted())) {
            throw new IllegalArgumentException(label + "已通过 PDA 提交并生成绩效，不能删除");
        }
        Long performanceCount = performanceRecordMapper.selectCount(
                new QueryWrapper<PerformanceRecord>()
                        .eq("execution_id", execution.getId())
                        .eq("record_status", 1));
        if (performanceCount != null && performanceCount > 0) {
            throw new IllegalArgumentException(label + "已生成绩效记录，不能删除");
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
        // 执行中登记的参与人工时为 0（结束时间未知），完工时按最终实际区间回填
        backfillParticipantWorkHours(executionId, execution.getStartTime(), end);
        performanceGenerationService.generateForExecution(executionId);
        // 按批次下全部执行记录的完成情况刷新批次状态，多个执行行分次完工时不会提前标记已完工
        refreshPlanBatchStatus(execution.getPlanBatchId());
        execution.setParticipants(participantMapper.selectByExecutionIdsWithPersonnel(List.of(executionId)));
        return execution;
    }

    /**
     * 回填执行中登记的参与人工时
     * <p>「实际生产」允许工序未完工时先登记参与人，此时个人工时暂记 0；完工后按实际起止补齐，
     * 避免执行记录与绩效明细中的工时长期为 0。</p>
     *
     * @param executionId 执行记录ID
     * @param start       实际开始时间
     * @param end         实际结束时间
     */
    private void backfillParticipantWorkHours(Long executionId, LocalDateTime start, LocalDateTime end) {
        BigDecimal hours = durationHours(start, end);
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        participantMapper.update(null, new LambdaUpdateWrapper<WorkOrderProcessExecutionParticipant>()
                .eq(WorkOrderProcessExecutionParticipant::getExecutionId, executionId)
                .le(WorkOrderProcessExecutionParticipant::getWorkHours, BigDecimal.ZERO)
                .set(WorkOrderProcessExecutionParticipant::getWorkHours, hours));
    }

    /**
     * 完工行生成绩效记录
     * <p>PC 批量保存把开始/结束时间填齐后同样视为完工，需要与 PDA 提交、完工锁定保持同一口径：
     * 本次由未完工变为已完成的记录必须有参与人，否则拒绝整批保存；已有参与人的完工行
     * 按「执行记录+人员」幂等生成绩效，重复保存不会重复生成。</p>
     *
     * @param execution         本次保存的执行记录（已落库并保存参与人）
     * @param existingCompleted 保存前该记录是否已完成
     */
    private void generateForCompleted(WorkOrderProcessExecution execution, boolean existingCompleted) {
        if (!ProcessExecutionStatus.COMPLETED.equals(execution.getStatus())) {
            return;
        }
        Long participantCount = participantMapper.selectCount(
                new QueryWrapper<WorkOrderProcessExecutionParticipant>()
                        .eq("execution_id", execution.getId()));
        if (participantCount == null || participantCount == 0) {
            if (!existingCompleted) {
                String label = execution.getStepOrder() == null
                        ? "ID " + execution.getId() : "工序【" + execution.getStepOrder() + "】";
                throw new IllegalArgumentException(label + "未登记参与人，无法生成绩效；请先登记参与人");
            }
            // 历史脏数据（已完成但无参与人）不阻断当前保存，待补充参与人后再次保存即可生成
            return;
        }
        performanceGenerationService.generateForExecution(execution.getId());
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
        // 前端只登记参与人时，按工序实际起止计算个人工作时间；执行中的工序结束时间未知，
        // 工时先记 0，待完工时按最终实际区间回填（绩效生成同样按执行区间兜底）
        BigDecimal defaultHours = durationHours(execution.getStartTime(), execution.getEndTime());
        boolean durationKnown = execution.getStartTime() != null && execution.getEndTime() != null;
        for (WorkOrderProcessExecutionParticipant participant : participants) {
            if (participant.getPersonnelId() == null) {
                throw new IllegalArgumentException("参与人必须关联人员档案");
            }
            if (!personnelIds.add(participant.getPersonnelId())) {
                throw new IllegalArgumentException("参与人不能重复");
            }
            if (participant.getWorkHours() == null
                    || participant.getWorkHours().compareTo(BigDecimal.ZERO) <= 0) {
                participant.setWorkHours(defaultHours);
            }
            if (participant.getWorkHours().compareTo(BigDecimal.ZERO) <= 0) {
                if (durationKnown) {
                    throw new IllegalArgumentException("参与人工作时间必须大于 0");
                }
                // 执行中：允许工时暂为 0，完工时统一回填
                participant.setWorkHours(BigDecimal.ZERO);
            }
            if (participant.getCompletedQty() != null
                    && participant.getCompletedQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("参与人完成数量不能为负");
            }
        }
        boolean anyQtyPresent = participants.stream()
                .anyMatch(p -> p.getCompletedQty() != null);
        boolean allQtyPresent = participants.stream()
                .allMatch(p -> p.getCompletedQty() != null);
        BigDecimal qtySum = participants.stream()
                .map(WorkOrderProcessExecutionParticipant::getCompletedQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (execution.getProcessQty() != null) {
            if (!anyQtyPresent) {
                // 前端未逐人填写完成数量时，按参与人均分实际数量，尾差计入最后一人
                distributeQty(participants, execution.getProcessQty());
            } else if (!allQtyPresent) {
                throw new IllegalArgumentException("请填写每位参与人的完成数量");
            } else if (qtySum.compareTo(execution.getProcessQty()) != 0) {
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

    /**
     * 按参与人均分完成数量
     * <p>除不尽时尾差计入最后一人，保证合计与实际数量一致</p>
     *
     * @param participants 参与人列表
     * @param qty          实际数量
     */
    private void distributeQty(List<WorkOrderProcessExecutionParticipant> participants, BigDecimal qty) {
        int count = participants.size();
        if (count == 0) {
            return;
        }
        BigDecimal share = qty.divide(BigDecimal.valueOf(count), 3, java.math.RoundingMode.DOWN);
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            BigDecimal value = i == count - 1 ? qty.subtract(assigned) : share;
            participants.get(i).setCompletedQty(value);
            assigned = assigned.add(value);
        }
    }

    // endregion
}

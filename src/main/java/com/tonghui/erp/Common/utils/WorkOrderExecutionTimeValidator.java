package com.tonghui.erp.Common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工序实际执行时间校验器
 * <p>
 * 在保存/提交实际执行记录前校验时间是否合理，校验项包括：
 * <ol>
 *   <li>任务时间窗：实际开始不得早于任务配置时间；已填配置完成时间时，实际起止不得晚于该时间</li>
 *   <li>工序先后：本工序实际开始不得早于前序工序的实际结束时间；前序工序尚未完工时不允许开始本工序</li>
 *   <li>时间倒挂：后序工序已登记实际开始后，前序工序的实际结束不得晚于该时间</li>
 *   <li>时长：实际结束必须晚于实际开始，避免 0 或负工时</li>
 * </ol>
 * 只校验「已填写开始或结束时间」的行；仅登记设备、数量、参与人的行放行。
 * 同工序拆分的多个批次之间允许重叠，不做先后约束。
 * </p>
 * <p>
 * 紧急开关：配置项 {@code erp.time-validation-enabled=false} 可整体关闭校验，
 * 用于历史数据批量修复等场景，默认开启。
 * </p>
 */
@Component
public class WorkOrderExecutionTimeValidator {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 工序执行记录 Mapper（读取同工单既有执行记录，用于工序先后校验） */
    private final WorkOrderProcessExecutionMapper executionMapper;

    /** 工序类型 Mapper（补齐提示文案中的工序名称） */
    private final ProcessTypeMapper processTypeMapper;

    /**
     * 时间校验开关，默认开启
     */
    @Value("${erp.time-validation-enabled:true}")
    private boolean enabled;

    /**
     * 构造方法，注入所需Mapper
     *
     * @param executionMapper   工序执行记录 Mapper
     * @param processTypeMapper 工序类型 Mapper
     */
    @Autowired
    public WorkOrderExecutionTimeValidator(WorkOrderProcessExecutionMapper executionMapper,
                                           ProcessTypeMapper processTypeMapper) {
        this.executionMapper = executionMapper;
        this.processTypeMapper = processTypeMapper;
    }

    // endregion

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 错误提示中的时间格式
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // endregion

    // region 校验入口
    // ===================================
    // 校验入口
    // ===================================

    /**
     * 校验一批执行记录的实际时间是否合法
     * <p>
     * 有任何校验不通过时抛出 {@link IllegalArgumentException}，消息为多行文本，
     * 每行描述一条具体问题，便于前端直接展示。
     * </p>
     *
     * @param order      所属工单（提供配置时间与配置完成时间）
     * @param rows       待保存的执行记录（已完成字段补齐）
     * @param excludeIds 本次将被覆盖的既有记录ID（校验前序工序时排除，避免用旧值比较）
     * @throws IllegalArgumentException 校验不通过
     */
    public void validate(WorkOrder order, List<WorkOrderProcessExecution> rows, Set<Long> excludeIds) {
        validate(order, rows, excludeIds, true);
    }

    /**
     * 完工时校验：只校验时间窗、时长与「结束不得晚于后续工序已开始」
     * <p>
     * 完工不会改变开始时间，开始时间在登记时已经校验过；这里不再重复做前序工序校验，
     * 避免历史数据或并行批次在补结束时间时被误拦。结束时间的上界与倒挂仍然校验。
     * </p>
     *
     * @param order 所属工单
     * @param row   待完工的执行记录（结束时间已填入）
     * @throws IllegalArgumentException 校验不通过
     */
    public void validateCompletion(WorkOrder order, WorkOrderProcessExecution row) {
        if (row == null) {
            return;
        }
        Set<Long> excludes = row.getId() == null ? Set.of() : Set.of(row.getId());
        validate(order, List.of(row), excludes, false);
    }

    /**
     * 校验入口（内部实现）
     *
     * @param order              所属工单
     * @param rows               待保存的执行记录
     * @param excludeIds         本次将被覆盖的既有记录ID
     * @param checkPreviousSteps 是否校验前序工序（保存时为 true，完工时为 false）
     */
    private void validate(WorkOrder order, List<WorkOrderProcessExecution> rows, Set<Long> excludeIds,
                          boolean checkPreviousSteps) {
        if (!enabled || order == null || CollectionUtils.isEmpty(rows)) {
            return;
        }
        Set<Long> excludes = excludeIds == null ? Set.of() : excludeIds;
        LocalDateTime configStart = order.getConfigDate();
        LocalDateTime configEnd = order.getConfigCompleteTime();
        Map<Integer, StepState> steps = buildStepStates(order.getWorkOrderId(), rows, excludes);
        Map<Long, String> processNames = loadProcessNames(rows, steps);

        Set<String> errors = new LinkedHashSet<>();
        for (WorkOrderProcessExecution row : rows) {
            if (row.getStartTime() == null && row.getEndTime() == null) {
                // 未填时间（仅登记设备/数量/参与人）的行不参与时间校验
                continue;
            }
            String label = describeProcess(row, processNames);
            checkTaskWindow(row, label, configStart, configEnd, errors);
            if (row.getStartTime() != null) {
                checkDuration(row, label, errors);
                if (checkPreviousSteps) {
                    checkStepOrder(row, label, steps, processNames, errors);
                }
            }
            if (row.getEndTime() != null) {
                // 反向校验：修改历史工序的结束时间时，不得晚于后续工序已登记的实际开始
                checkNextStepOrder(row, label, steps, processNames, errors);
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    // endregion

    // region 任务时间窗校验
    // ===================================
    // 任务时间窗校验
    // ===================================

    /**
     * 校验实际时间是否落在「配置时间 ~ 配置完成时间」范围内
     * <p>配置完成时间为空时只校验下界，即实际起止必须晚于配置时间。</p>
     *
     * @param row         执行记录
     * @param label       工序描述
     * @param configStart 任务配置时间（可为空）
     * @param configEnd   任务配置完成时间（可为空）
     * @param errors      错误收集容器
     */
    private void checkTaskWindow(WorkOrderProcessExecution row, String label,
                                 LocalDateTime configStart, LocalDateTime configEnd,
                                 Set<String> errors) {
        LocalDateTime start = row.getStartTime();
        LocalDateTime end = row.getEndTime();
        if (configStart != null && start != null && start.isBefore(configStart)) {
            errors.add("工序【" + label + "】实际开始 " + format(start)
                    + " 早于任务配置时间 " + format(configStart));
        }
        if (end != null && configStart != null && end.isBefore(configStart)) {
            errors.add("工序【" + label + "】实际结束 " + format(end)
                    + " 早于任务配置时间 " + format(configStart));
        }
        if (configEnd == null) {
            return;
        }
        if (end != null) {
            if (end.isAfter(configEnd)) {
                errors.add("工序【" + label + "】实际结束 " + format(end)
                        + " 晚于任务配置完成时间 " + format(configEnd));
            }
            return;
        }
        // 工序尚未结束：至少要求开始时间不晚于配置完成时间，避免登记到窗口之后再补结束时间
        if (start != null && start.isAfter(configEnd)) {
            errors.add("工序【" + label + "】实际开始 " + format(start)
                    + " 晚于任务配置完成时间 " + format(configEnd));
        }
    }

    /**
     * 校验实际结束时间晚于实际开始时间
     * <p>结束时间早于或等于开始时间会产生 0 或负工时，直接拒绝。</p>
     *
     * @param row    执行记录
     * @param label  工序描述
     * @param errors 错误收集容器
     */
    private void checkDuration(WorkOrderProcessExecution row, String label, Set<String> errors) {
        LocalDateTime start = row.getStartTime();
        LocalDateTime end = row.getEndTime();
        if (start == null || end == null) {
            return;
        }
        if (!end.isAfter(start)) {
            errors.add("工序【" + label + "】实际结束 " + format(end)
                    + " 必须晚于实际开始 " + format(start));
        }
    }

    // endregion

    // region 工序先后校验
    // ===================================
    // 工序先后校验
    // ===================================

    /**
     * 校验本工序开始时间不早于前序工序的实际结束时间
     * <p>
     * 前序工序指工序顺序更小的同一工单执行记录；存在前序工序已开始未结束（执行中）时，
     * 直接判定为尚未完工，不允许开始本工序。
     * </p>
     *
     * @param row          执行记录
     * @param label        工序描述
     * @param steps        按工序顺序汇总的执行状态
     * @param processNames 工序ID → 工序名称
     * @param errors       错误收集容器
     */
    private void checkStepOrder(WorkOrderProcessExecution row, String label,
                                Map<Integer, StepState> steps, Map<Long, String> processNames,
                                Set<String> errors) {
        Integer stepOrder = row.getStepOrder();
        if (stepOrder == null) {
            return;
        }
        List<StepState> previousSteps = steps.values().stream()
                .filter(state -> state.stepOrder != null && state.stepOrder < stepOrder)
                .sorted((a, b) -> Integer.compare(a.stepOrder, b.stepOrder))
                .toList();
        if (previousSteps.isEmpty()) {
            return;
        }
        StepState unfinished = previousSteps.stream()
                .filter(state -> state.open).findFirst().orElse(null);
        if (unfinished != null) {
            errors.add("工序【" + label + "】登记实际开始时间时，上一道工序【"
                    + stateLabel(unfinished, processNames) + "】尚未完工，请先结束上一道工序");
            return;
        }
        StepState lastFinished = previousSteps.stream()
                .filter(state -> state.latestEnd != null)
                .max((a, b) -> a.latestEnd.compareTo(b.latestEnd))
                .orElse(null);
        if (lastFinished != null && row.getStartTime().isBefore(lastFinished.latestEnd)) {
            errors.add("工序【" + label + "】实际开始 " + format(row.getStartTime())
                    + " 早于上一道工序【" + stateLabel(lastFinished, processNames)
                    + "】的实际结束 " + format(lastFinished.latestEnd));
        }
    }

    /**
     * 校验本工序结束时间不晚于后续工序的实际开始时间
     * <p>
     * 用于「反方向」修改：补录/修正前序工序的结束时间时，若已晚于后续工序已登记的实际开始，
     * 会造成时间倒挂，直接拒绝。后续工序尚未开始时不做限制。
     * 同工序拆分的多个批次之间不互相约束。
     * </p>
     *
     * @param row          执行记录
     * @param label        工序描述
     * @param steps        按工序顺序汇总的执行状态
     * @param processNames 工序ID → 工序名称
     * @param errors       错误收集容器
     */
    private void checkNextStepOrder(WorkOrderProcessExecution row, String label,
                                    Map<Integer, StepState> steps, Map<Long, String> processNames,
                                    Set<String> errors) {
        Integer stepOrder = row.getStepOrder();
        LocalDateTime end = row.getEndTime();
        if (stepOrder == null || end == null) {
            return;
        }
        StepState nextStep = steps.values().stream()
                .filter(state -> state.stepOrder != null && state.stepOrder > stepOrder)
                .filter(state -> state.earliestStart != null)
                .min((a, b) -> a.earliestStart.compareTo(b.earliestStart))
                .orElse(null);
        if (nextStep != null && end.isAfter(nextStep.earliestStart)) {
            errors.add("工序【" + label + "】实际结束 " + format(end)
                    + " 晚于后续工序【" + stateLabel(nextStep, processNames)
                    + "】的实际开始 " + format(nextStep.earliestStart));
        }
    }

    /**
     * 汇总各工序顺序的最晚实际结束时间与执行中状态
     * <p>库中既有记录按ID排除本次将被覆盖的行；本次提交的行按新值参与汇总。</p>
     *
     * @param workOrderId 工单ID
     * @param rows        本次提交的执行记录
     * @param excludeIds  本次将被覆盖的既有记录ID
     * @return 工序顺序 → 汇总状态
     */
    private Map<Integer, StepState> buildStepStates(Long workOrderId,
                                                    List<WorkOrderProcessExecution> rows,
                                                    Set<Long> excludeIds) {
        Map<Integer, StepState> steps = new HashMap<>();
        List<WorkOrderProcessExecution> persisted = workOrderId == null ? List.of()
                : executionMapper.selectList(new LambdaQueryWrapper<WorkOrderProcessExecution>()
                .eq(WorkOrderProcessExecution::getWorkOrderId, workOrderId));
        for (WorkOrderProcessExecution execution : persisted) {
            if (excludeIds.contains(execution.getId())) {
                continue;
            }
            accumulate(steps, execution);
        }
        for (WorkOrderProcessExecution row : rows) {
            accumulate(steps, row);
        }
        return steps;
    }

    /**
     * 把一条执行记录合并到工序汇总状态中
     *
     * @param steps     工序汇总状态
     * @param execution 执行记录
     */
    private void accumulate(Map<Integer, StepState> steps, WorkOrderProcessExecution execution) {
        if (execution.getStepOrder() == null) {
            return;
        }
        StepState state = steps.computeIfAbsent(execution.getStepOrder(), StepState::new);
        if (execution.getProcessTypeId() != null && state.processTypeId == null) {
            state.processTypeId = execution.getProcessTypeId();
        }
        if (execution.getStartTime() != null
                && (state.earliestStart == null || execution.getStartTime().isBefore(state.earliestStart))) {
            state.earliestStart = execution.getStartTime();
        }
        if (execution.getStartTime() != null && execution.getEndTime() == null) {
            state.open = true;
        }
        if (execution.getEndTime() != null
                && (state.latestEnd == null || execution.getEndTime().isAfter(state.latestEnd))) {
            state.latestEnd = execution.getEndTime();
        }
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 加载工序名称，用于错误提示
     *
     * @param rows  本次提交的执行记录
     * @param steps 工序汇总状态
     * @return 工序类型ID → 工序名称
     */
    private Map<Long, String> loadProcessNames(List<WorkOrderProcessExecution> rows,
                                               Map<Integer, StepState> steps) {
        Set<Long> processTypeIds = rows.stream()
                .map(WorkOrderProcessExecution::getProcessTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        steps.values().stream().map(state -> state.processTypeId)
                .filter(Objects::nonNull).forEach(processTypeIds::add);
        if (processTypeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (ProcessType processType : processTypeMapper.selectList(
                new LambdaQueryWrapper<ProcessType>().in(ProcessType::getProcessId, processTypeIds))) {
            names.put(processType.getProcessId().longValue(), processType.getProcessName());
        }
        return names;
    }

    /**
     * 描述工序（优先取记录自带的名称，其次查工序类型表，最后退化为工序顺序）
     *
     * @param execution    执行记录
     * @param processNames 工序类型ID → 工序名称
     * @return 工序描述
     */
    private String describeProcess(WorkOrderProcessExecution execution, Map<Long, String> processNames) {
        if (StringUtils.hasText(execution.getProcessTypeName())) {
            return execution.getProcessTypeName().trim();
        }
        String name = execution.getProcessTypeId() == null
                ? null : processNames.get(execution.getProcessTypeId());
        if (StringUtils.hasText(name)) {
            return name;
        }
        return execution.getStepOrder() == null ? "未命名工序" : "第" + execution.getStepOrder() + "道工序";
    }

    /**
     * 描述工序汇总状态对应的工序
     *
     * @param state        工序汇总状态
     * @param processNames 工序类型ID → 工序名称
     * @return 工序描述
     */
    private String stateLabel(StepState state, Map<Long, String> processNames) {
        String name = state.processTypeId == null ? null : processNames.get(state.processTypeId);
        if (StringUtils.hasText(name)) {
            return name;
        }
        return state.stepOrder == null ? "未命名工序" : "第" + state.stepOrder + "道工序";
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return 形如 2026-11-19 10:00 的文本
     */
    private String format(LocalDateTime time) {
        return time == null ? "未填写" : TIME_FORMATTER.format(time);
    }

    // endregion

    // region 内部状态
    // ===================================
    // 内部状态
    // ===================================

    /**
     * 单个工序顺序的执行状态汇总
     */
    private static final class StepState {

        /** 工序顺序 */
        private final Integer stepOrder;

        /** 工序类型ID（用于提示文案，取该顺序下任一记录） */
        private Long processTypeId;

        /** 最晚实际结束时间 */
        private LocalDateTime latestEnd;

        /** 最早实际开始时间（用于反向校验后续工序是否已开始） */
        private LocalDateTime earliestStart;

        /** 是否存在已开始未结束的执行记录 */
        private boolean open;

        private StepState(Integer stepOrder) {
            this.stepOrder = stepOrder;
        }
    }

    // endregion
}

package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Constants.EquipmentStatus;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentMaintenanceConflictDto;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageConflictDto;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.EquipmentMaintenanceMapper;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备使用冲突校验器
 * <p>
 * 在保存工单工序执行记录前校验设备使用是否合法，校验项包括：
 * <ol>
 *   <li>时间合法性：结束时间必须晚于开始时间；填写时间时设备必选</li>
 *   <li>设备状态：仅"正常"设备可安排生产使用（停用/报废/已删除均拦截）</li>
 *   <li>使用冲突：同一设备的时间区间不得重叠（跨工单、跨计划、同批内）</li>
 *   <li>维保冲突：使用时间区间不得与设备的维保时间区间重叠</li>
 * </ol>
 * 校验只针对"已填写开始/结束时间"的行；仅选设备未排时间（工单按模板初始化）的行放行。
 * </p>
 * <p>
 * 并发控制：按 equipment_id 升序对设备行加 {@code SELECT ... FOR UPDATE} 行锁，
 * 将同一设备的保存串行化；调用方事务隔离级别需为 READ_COMMITTED，确保加锁后能读到最新已提交数据。
 * </p>
 */
@Component
public class EquipmentUsageConflictValidator {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 设备数据访问Mapper
     */
    private final EquipmentMapper equipmentMapper;

    /**
     * 工单工序执行记录数据访问Mapper
     */
    private final WorkOrderProcessExecutionMapper executionMapper;

    /**
     * 设备维保记录数据访问Mapper
     */
    private final EquipmentMaintenanceMapper maintenanceMapper;

    /**
     * 构造方法，注入所需Mapper
     *
     * @param equipmentMapper  设备Mapper
     * @param executionMapper  工序执行记录Mapper
     * @param maintenanceMapper 设备维保记录Mapper
     */
    @Autowired
    public EquipmentUsageConflictValidator(EquipmentMapper equipmentMapper,
                                           WorkOrderProcessExecutionMapper executionMapper,
                                           EquipmentMaintenanceMapper maintenanceMapper) {
        this.equipmentMapper = equipmentMapper;
        this.executionMapper = executionMapper;
        this.maintenanceMapper = maintenanceMapper;
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
     * 校验一批工序执行记录的设备使用合法性
     * <p>
     * 有任何校验不通过时抛出 {@link IllegalArgumentException}，消息为多行文本，
     * 每行描述一条具体问题，便于前端直接展示。
     * </p>
     *
     * @param workOrderId 工单ID（用于排除本工单既有行）
     * @param executions  待保存的工序执行记录列表
     * @throws IllegalArgumentException 校验不通过
     */
    public void validate(Long workOrderId, List<WorkOrderProcessExecution> executions) {
        if (CollectionUtils.isEmpty(executions)) {
            return;
        }

        // 1. 基础校验：时间先后、设备必填、部分填写时间
        Set<String> errors = new LinkedHashSet<>();
        List<WorkOrderProcessExecution> timedRows = new ArrayList<>();
        for (WorkOrderProcessExecution execution : executions) {
            Integer equipmentId = normalizeEquipmentId(execution);
            String processLabel = describeProcess(execution);
            boolean hasStart = execution.getStartTime() != null;
            boolean hasEnd = execution.getEndTime() != null;

            if (hasStart != hasEnd) {
                errors.add("工序【" + processLabel + "】请同时填写开始时间和结束时间");
                continue;
            }
            // 未排时间的行不参与设备校验（工单按工艺模板初始化时只有设备、没有时间）
            if (!hasStart) {
                continue;
            }
            if (!execution.getStartTime().isBefore(execution.getEndTime())) {
                errors.add("工序【" + processLabel + "】结束时间必须晚于开始时间");
                continue;
            }
            if (equipmentId == null) {
                errors.add("工序【" + processLabel + "】填写使用时间时必须选择设备");
                continue;
            }
            timedRows.add(execution);
        }
        throwIfAny(errors);
        if (timedRows.isEmpty()) {
            return;
        }

        // 2. 收集本批涉及设备，按ID升序加锁并校验状态
        List<Integer> equipmentIds = timedRows.stream()
                .map(WorkOrderProcessExecution::getEquipmentId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        Map<Integer, Equipment> equipmentMap = lockAndCheckEquipments(equipmentIds, errors);
        throwIfAny(errors);

        // 3. 同一批内两两时间重叠校验
        checkInBatchConflicts(timedRows, equipmentMap, errors);

        // 4. 与其他工单既有记录的时间冲突校验
        checkDatabaseConflicts(workOrderId, timedRows, equipmentIds, equipmentMap, errors);

        // 5. 与设备维保日期的冲突校验
        checkMaintenanceConflicts(timedRows, equipmentIds, equipmentMap, errors);

        throwIfAny(errors);
    }

    // endregion

    // region 设备加锁与状态校验
    // ===================================
    // 设备加锁与状态校验
    // ===================================

    /**
     * 按设备ID升序加行锁并校验设备状态
     *
     * @param equipmentIds 设备ID列表（已升序去重）
     * @param errors       错误收集容器
     * @return 设备ID -> 设备信息
     */
    private Map<Integer, Equipment> lockAndCheckEquipments(List<Integer> equipmentIds, Set<String> errors) {
        Map<Integer, Equipment> equipmentMap = new HashMap<>(equipmentIds.size());
        for (Integer equipmentId : equipmentIds) {
            Equipment equipment = equipmentMapper.selectByIdForUpdate(equipmentId);
            if (equipment == null || Integer.valueOf(1).equals(equipment.getIsDeleted())) {
                errors.add("设备【ID:" + equipmentId + "】不存在或已删除，不可安排生产使用");
                continue;
            }
            if (!EquipmentStatus.isUsable(equipment.getEquipmentStatus())) {
                errors.add("设备【" + describeEquipment(equipment) + "】当前状态为【"
                        + safeText(equipment.getEquipmentStatus()) + "】，不可安排生产使用");
            }
            equipmentMap.put(equipmentId, equipment);
        }
        return equipmentMap;
    }

    // endregion

    // region 同批内冲突校验
    // ===================================
    // 同批内冲突校验
    // ===================================

    /**
     * 校验同一批提交记录内部的设备时间重叠
     * <p>
     * 按设备分组、开始时间升序后扫描；由于已按开始时间排序，
     * 后续行开始时间不早于当前行结束时间时即可停止比较。
     * </p>
     *
     * @param timedRows    已填写时间的记录
     * @param equipmentMap 设备信息
     * @param errors       错误收集容器
     */
    private void checkInBatchConflicts(List<WorkOrderProcessExecution> timedRows,
                                       Map<Integer, Equipment> equipmentMap,
                                       Set<String> errors) {
        Map<Integer, List<WorkOrderProcessExecution>> grouped = timedRows.stream()
                .collect(Collectors.groupingBy(WorkOrderProcessExecution::getEquipmentId));
        for (Map.Entry<Integer, List<WorkOrderProcessExecution>> entry : grouped.entrySet()) {
            List<WorkOrderProcessExecution> rows = entry.getValue();
            rows.sort(Comparator.comparing(WorkOrderProcessExecution::getStartTime));
            String equipmentLabel = describeEquipment(equipmentMap.get(entry.getKey()), entry.getKey());
            for (int i = 0; i < rows.size(); i++) {
                WorkOrderProcessExecution current = rows.get(i);
                for (int j = i + 1; j < rows.size(); j++) {
                    WorkOrderProcessExecution next = rows.get(j);
                    if (!next.getStartTime().isBefore(current.getEndTime())) {
                        break;
                    }
                    errors.add("同一工单内工序【" + describeProcess(current) + "】与工序【"
                            + describeProcess(next) + "】使用设备【" + equipmentLabel + "】的时间重叠（"
                            + formatRange(current.getStartTime(), current.getEndTime()) + " 与 "
                            + formatRange(next.getStartTime(), next.getEndTime()) + "）");
                }
            }
        }
    }

    // endregion

    // region 跨工单冲突校验
    // ===================================
    // 跨工单冲突校验
    // ===================================

    /**
     * 校验与其他工单/计划已登记的设备使用时间是否重叠
     *
     * @param workOrderId  当前工单ID
     * @param timedRows    已填写时间的记录
     * @param equipmentIds 设备ID列表
     * @param equipmentMap 设备信息
     * @param errors       错误收集容器
     */
    private void checkDatabaseConflicts(Long workOrderId,
                                        List<WorkOrderProcessExecution> timedRows,
                                        List<Integer> equipmentIds,
                                        Map<Integer, Equipment> equipmentMap,
                                        Set<String> errors) {
        LocalDateTime minStart = timedRows.stream()
                .map(WorkOrderProcessExecution::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime maxEnd = timedRows.stream()
                .map(WorkOrderProcessExecution::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (minStart == null || maxEnd == null) {
            return;
        }

        // 本工单既有行会被本次提交整体覆盖（未提交的行将被删除），不参与冲突判断
        List<Long> excludeIds = executionMapper.selectIdsByWorkOrderId(workOrderId);
        List<EquipmentUsageConflictDto> conflicts = executionMapper.selectConflictRecords(
                equipmentIds, minStart, maxEnd, excludeIds);
        if (CollectionUtils.isEmpty(conflicts)) {
            return;
        }

        for (WorkOrderProcessExecution row : timedRows) {
            for (EquipmentUsageConflictDto conflict : conflicts) {
                if (!Objects.equals(row.getEquipmentId(), conflict.getEquipmentId())) {
                    continue;
                }
                // 半开区间重叠：[start, end)
                boolean overlap = conflict.getStartTime() != null && conflict.getEndTime() != null
                        && row.getStartTime().isBefore(conflict.getEndTime())
                        && row.getEndTime().isAfter(conflict.getStartTime());
                if (!overlap) {
                    continue;
                }
                errors.add("设备【" + describeConflictEquipment(conflict, equipmentMap) + "】使用时间冲突："
                        + formatRange(row.getStartTime(), row.getEndTime()) + " 已被工单【"
                        + safeText(conflict.getWorkOrderCode()) + "】的工序【" + safeText(conflict.getProcessName())
                        + "】（批号：" + safeText(conflict.getBatchNumber()) + "）占用 "
                        + formatRange(conflict.getStartTime(), conflict.getEndTime()));
            }
        }
    }

    // endregion

    // region 维保冲突校验
    // ===================================
    // 维保冲突校验
    // ===================================

    /**
     * 校验使用时间是否与设备维保时间区间重叠
     * <p>
     * 维保时间与使用时间均按半开区间 [开始时间, 结束时间) 处理：
     * 使用结束时间恰好等于维保开始时间（或反之）视为不冲突。
     * </p>
     *
     * @param timedRows    已填写时间的记录
     * @param equipmentIds 设备ID列表
     * @param equipmentMap 设备信息
     * @param errors       错误收集容器
     */
    private void checkMaintenanceConflicts(List<WorkOrderProcessExecution> timedRows,
                                           List<Integer> equipmentIds,
                                           Map<Integer, Equipment> equipmentMap,
                                           Set<String> errors) {
        LocalDateTime minStart = timedRows.stream()
                .map(WorkOrderProcessExecution::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime maxEnd = timedRows.stream()
                .map(WorkOrderProcessExecution::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (minStart == null || maxEnd == null) {
            return;
        }

        List<EquipmentMaintenanceConflictDto> maintenances =
                maintenanceMapper.selectMaintenanceConflicts(equipmentIds, minStart, maxEnd);
        if (CollectionUtils.isEmpty(maintenances)) {
            return;
        }

        for (WorkOrderProcessExecution row : timedRows) {
            for (EquipmentMaintenanceConflictDto maintenance : maintenances) {
                if (!Objects.equals(row.getEquipmentId(), maintenance.getEquipmentId())) {
                    continue;
                }
                // 半开区间重叠：[start, end)
                boolean overlap = maintenance.getMaintenanceStartTime() != null
                        && maintenance.getMaintenanceEndTime() != null
                        && row.getStartTime().isBefore(maintenance.getMaintenanceEndTime())
                        && row.getEndTime().isAfter(maintenance.getMaintenanceStartTime());
                if (!overlap) {
                    continue;
                }
                errors.add("设备【" + describeMaintenanceEquipment(maintenance, equipmentMap)
                        + "】在 " + formatRange(maintenance.getMaintenanceStartTime(), maintenance.getMaintenanceEndTime())
                        + " 有维保记录（" + safeText(maintenance.getMaintenanceType())
                        + "），该时段不可安排生产使用（使用时间："
                        + formatRange(row.getStartTime(), row.getEndTime()) + "）");
            }
        }
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 规范化设备ID：null、0、负数统一视为未选设备，并回写实体避免脏数据入库
     *
     * @param execution 工序执行记录
     * @return 有效设备ID，未选设备时返回 null
     */
    private Integer normalizeEquipmentId(WorkOrderProcessExecution execution) {
        Integer equipmentId = execution.getEquipmentId();
        if (equipmentId == null || equipmentId <= 0) {
            execution.setEquipmentId(null);
            return null;
        }
        return equipmentId;
    }

    /**
     * 校验存在错误时抛出业务异常
     *
     * @param errors 错误集合
     */
    private void throwIfAny(Set<String> errors) {
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    /**
     * 描述工序（用于错误提示）
     *
     * @param execution 工序执行记录
     * @return 工序名称，缺失时退化为工序顺序
     */
    private String describeProcess(WorkOrderProcessExecution execution) {
        if (StringUtils.hasText(execution.getProcessTypeName())) {
            return execution.getProcessTypeName().trim();
        }
        Integer stepOrder = execution.getStepOrder();
        return stepOrder == null ? "未命名工序" : "第" + stepOrder + "道工序";
    }

    /**
     * 描述设备（设备名称 + 固定资产编号）
     *
     * @param equipment   设备信息
     * @param equipmentId 设备ID（设备信息为空时使用）
     * @return 设备描述文本
     */
    private String describeEquipment(Equipment equipment, Integer equipmentId) {
        if (equipment == null) {
            return "ID:" + equipmentId;
        }
        return describeEquipment(equipment);
    }

    /**
     * 描述设备（设备名称 + 固定资产编号）
     *
     * @param equipment 设备信息
     * @return 设备描述文本
     */
    private String describeEquipment(Equipment equipment) {
        String name = StringUtils.hasText(equipment.getEquipmentName())
                ? equipment.getEquipmentName().trim() : "ID:" + equipment.getEquipmentId();
        if (StringUtils.hasText(equipment.getFixedAssetCode())) {
            return name + "(" + equipment.getFixedAssetCode().trim() + ")";
        }
        return name;
    }

    /**
     * 描述冲突记录涉及的设备
     *
     * @param conflict     冲突记录
     * @param equipmentMap 已加锁的设备信息
     * @return 设备描述文本
     */
    private String describeConflictEquipment(EquipmentUsageConflictDto conflict,
                                             Map<Integer, Equipment> equipmentMap) {
        if (StringUtils.hasText(conflict.getEquipmentName())) {
            return StringUtils.hasText(conflict.getFixedAssetCode())
                    ? conflict.getEquipmentName() + "(" + conflict.getFixedAssetCode() + ")"
                    : conflict.getEquipmentName();
        }
        return describeEquipment(equipmentMap.get(conflict.getEquipmentId()), conflict.getEquipmentId());
    }

    /**
     * 描述维保记录涉及的设备
     *
     * @param maintenance  维保冲突记录
     * @param equipmentMap 已加锁的设备信息
     * @return 设备描述文本
     */
    private String describeMaintenanceEquipment(EquipmentMaintenanceConflictDto maintenance,
                                                Map<Integer, Equipment> equipmentMap) {
        if (StringUtils.hasText(maintenance.getEquipmentName())) {
            return StringUtils.hasText(maintenance.getFixedAssetCode())
                    ? maintenance.getEquipmentName() + "(" + maintenance.getFixedAssetCode() + ")"
                    : maintenance.getEquipmentName();
        }
        return describeEquipment(equipmentMap.get(maintenance.getEquipmentId()), maintenance.getEquipmentId());
    }

    /**
     * 格式化时间区间
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 形如 2026-10-15 08:00~12:00 的文本
     */
    private String formatRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "未填写时间";
        }
        return TIME_FORMATTER.format(start) + "~" + TIME_FORMATTER.format(end);
    }

    /**
     * 空值安全的文本输出
     *
     * @param text 原文本
     * @return 原文本，空值时返回 "未填写"
     */
    private String safeText(String text) {
        return StringUtils.hasText(text) ? text : "未填写";
    }

    // endregion
}

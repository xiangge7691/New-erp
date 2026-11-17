package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Common.Dto.Scheduling.ScheduleConflictDto;
import com.tonghui.erp.Data.Entity.ProductionShift;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.ProductionShiftMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 排产冲突校验服务
 * <p>
 * 校验设备、房间、人员班次、工序顺序与批次重叠。规则：
 * 后道工序计划开始不早于紧道工序计划开始，允许时段重叠；
 * 同一班组可同时看多台设备，班组不作为互斥资源；
 * 未保存的草稿允许存在冲突，正式保存必须无冲突。
 * </p>
 */
@Component
public class ScheduleConflictService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 排产计划 Mapper */
    @Autowired
    private WorkOrderProcessPlanMapper planMapper;

    /** 排产批次 Mapper */
    @Autowired
    private WorkOrderProcessPlanBatchMapper batchMapper;

    /** 班次 Mapper */
    @Autowired
    private ProductionShiftMapper shiftMapper;

    /** 设备 Mapper（行锁） */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /** 房间 Mapper（行锁） */
    @Autowired
    private RoomInfoMapper roomInfoMapper;

    // endregion

    // region 内部结构
    // ===================================
    // 内部结构
    // ===================================

    /**
     * 参与冲突比对的排产单元（工序或批次）
     */
    private record Item(Long planId, Long batchId, Integer stepOrder, String processName,
                        Long workOrderId, String workOrderCode,
                        LocalDateTime start, LocalDateTime end,
                        Integer equipmentId, String equipmentName,
                        Integer roomId, String roomName,
                        Long teamId, String teamName) {
    }

    // endregion

    // region 入口
    // ===================================
    // 入口
    // ===================================

    /**
     * 校验一个工单的排产计划
     *
     * @param workOrderId 工单ID
     * @param plans       待保存的计划行
     * @param batches     待保存的批次
     * @return 冲突列表，空表示无冲突
     */
    public List<ScheduleConflictDto> check(Long workOrderId, List<WorkOrderProcessPlan> plans,
                                           List<WorkOrderProcessPlanBatch> batches) {
        List<ScheduleConflictDto> conflicts = new ArrayList<>();
        if (CollectionUtils.isEmpty(plans)) {
            return conflicts;
        }
        // 调用方可能传入不可变列表，复制后再排序
        plans = new ArrayList<>(plans);
        plans.sort(Comparator.comparing(WorkOrderProcessPlan::getStepOrder,
                Comparator.nullsLast(Integer::compareTo)));
        Map<Long, List<WorkOrderProcessPlanBatch>> batchMap = batches == null ? Map.of()
                : batches.stream().collect(Collectors.groupingBy(WorkOrderProcessPlanBatch::getPlanId));

        // 1. 工序顺序：后道开始不早于紧前开始
        for (int i = 1; i < plans.size(); i++) {
            WorkOrderProcessPlan prev = plans.get(i - 1);
            WorkOrderProcessPlan cur = plans.get(i);
            LocalDateTime prevStart = effectiveStart(prev, batchMap.get(prev.getId()));
            LocalDateTime curStart = effectiveStart(cur, batchMap.get(cur.getId()));
            if (prevStart != null && curStart != null && curStart.isBefore(prevStart)) {
                conflicts.add(conflict("工序顺序", cur, null, curStart, effectiveEnd(cur, batchMap.get(cur.getId())),
                        "工序【" + safeName(cur) + "】计划开始早于紧前工序【" + safeName(prev) + "】的开始时间"));
            }
        }

        // 2. 批次数量与批次重叠
        for (WorkOrderProcessPlan plan : plans) {
            List<WorkOrderProcessPlanBatch> list = batchMap.get(plan.getId());
            if (CollectionUtils.isEmpty(list)) {
                continue;
            }
            if (plan.getPlanQty() != null) {
                java.math.BigDecimal sum = list.stream().map(WorkOrderProcessPlanBatch::getPlanQty)
                        .filter(Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                if (sum.compareTo(plan.getPlanQty()) != 0) {
                    conflicts.add(conflict("批次数量", plan, null, null, null,
                            "工序【" + safeName(plan) + "】批次计划数量合计 " + sum
                                    + " 不等于工序计划数量 " + plan.getPlanQty()));
                }
            }
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    WorkOrderProcessPlanBatch a = list.get(i);
                    WorkOrderProcessPlanBatch b = list.get(j);
                    if (overlap(a.getPlanStartTime(), a.getPlanEndTime(), b.getPlanStartTime(), b.getPlanEndTime())) {
                        conflicts.add(conflict("批次重叠", plan, a.getId(), a.getPlanStartTime(), a.getPlanEndTime(),
                                "工序【" + safeName(plan) + "】批次 " + a.getBatchNo() + " 与批次 "
                                        + b.getBatchNo() + " 时段重叠"));
                    }
                }
            }
        }

        // 3. 组装本批比对单元
        List<Item> incoming = buildItems(plans, batchMap);
        // 4. 数据库内已保存计划
        List<Item> saved = loadSavedItems(workOrderId, plans, batches);
        List<Item> all = new ArrayList<>(incoming);
        all.addAll(saved);

        // 5. 设备与房间冲突
        checkResourceConflicts(incoming, saved, all, conflicts);

        // 6. 人员班次冲突
        checkShiftConflicts(incoming, conflicts);

        return conflicts;
    }

    /**
     * 对本次涉及的设备/房间加行锁，避免并发保存同时通过校验
     *
     * @param plans   计划行
     * @param batches 批次
     */
    public void lockResources(List<WorkOrderProcessPlan> plans, List<WorkOrderProcessPlanBatch> batches) {
        Set<Integer> equipmentIds = new HashSet<>();
        Set<Integer> roomIds = new HashSet<>();
        if (plans != null) {
            plans.forEach(p -> {
                if (p.getEquipmentId() != null) equipmentIds.add(p.getEquipmentId());
                if (p.getRoomId() != null) roomIds.add(p.getRoomId());
            });
        }
        if (batches != null) {
            batches.forEach(b -> {
                if (b.getEquipmentId() != null) equipmentIds.add(b.getEquipmentId());
                if (b.getRoomId() != null) roomIds.add(b.getRoomId());
            });
        }
        equipmentIds.stream().sorted().forEach(id -> equipmentMapper.selectByIdForUpdate(id));
        roomIds.stream().sorted().forEach(id -> roomInfoMapper.selectByIdForUpdate(id));
    }

    // endregion

    // region 资源冲突
    // ===================================
    // 资源冲突
    // ===================================

    /**
     * 校验设备与房间冲突（本批内部 + 与已保存计划）
     */
    private void checkResourceConflicts(List<Item> incoming, List<Item> saved, List<Item> all,
                                        List<ScheduleConflictDto> conflicts) {
        for (int i = 0; i < incoming.size(); i++) {
            Item a = incoming.get(i);
            for (int j = i + 1; j < incoming.size(); j++) {
                Item b = incoming.get(j);
                addPairConflict(a, b, conflicts, "本工单");
            }
            for (Item b : saved) {
                addPairConflict(a, b, conflicts, "其他工单");
            }
        }
        // 为设备/房间冲突补充最近的可用时段
        for (ScheduleConflictDto conflict : conflicts) {
            if (!"设备占用".equals(conflict.getType()) && !"房间占用".equals(conflict.getType())) {
                continue;
            }
            Item item = incoming.stream()
                    .filter(x -> Objects.equals(x.planId(), conflict.getPlanId())
                            && (conflict.getBatchId() == null || Objects.equals(x.batchId(), conflict.getBatchId())))
                    .findFirst().orElse(null);
            if (item == null) {
                continue;
            }
            LocalDateTime prevStart = previousStart(item, incoming);
            LocalDateTime[] slot = findFreeSlot(item, all, prevStart);
            if (slot != null) {
                conflict.setRecommendedStartTime(slot[0]);
                conflict.setRecommendedEndTime(slot[1]);
            }
        }
    }

    /**
     * 两条排产单元之间的设备/房间冲突
     */
    private void addPairConflict(Item a, Item b, List<ScheduleConflictDto> conflicts, String scope) {
        if (a.start() == null || a.end() == null || b.start() == null || b.end() == null) {
            return;
        }
        if (!overlap(a.start(), a.end(), b.start(), b.end())) {
            return;
        }
        if (a.equipmentId() != null && Objects.equals(a.equipmentId(), b.equipmentId())) {
            ScheduleConflictDto c = conflict("设备占用", a.planId(), a.batchId(), a.stepOrder(), a.processName(),
                    a.workOrderId(), a.workOrderCode(), a.start(), a.end(),
                    "设备【" + safe(a.equipmentName()) + "】与" + b.workOrderCode() + " 工序【"
                            + b.processName() + "】时段重叠（" + b.start() + " ~ " + b.end() + "）");
            c.setOtherWorkOrderId(b.workOrderId());
            c.setOtherWorkOrderCode(b.workOrderCode());
            conflicts.add(c);
        }
        if (a.roomId() != null && Objects.equals(a.roomId(), b.roomId())) {
            ScheduleConflictDto c = conflict("房间占用", a.planId(), a.batchId(), a.stepOrder(), a.processName(),
                    a.workOrderId(), a.workOrderCode(), a.start(), a.end(),
                    "房间【" + safe(a.roomName()) + "】与" + b.workOrderCode() + " 工序【"
                            + b.processName() + "】时段重叠（" + b.start() + " ~ " + b.end() + "）");
            c.setOtherWorkOrderId(b.workOrderId());
            c.setOtherWorkOrderCode(b.workOrderCode());
            conflicts.add(c);
        }
    }

    /**
     * 查找设备与房间同时空闲的最近时段
     */
    private LocalDateTime[] findFreeSlot(Item item, List<Item> all, LocalDateTime prevStart) {
        if (item.start() == null || item.end() == null) {
            return null;
        }
        long minutes = Math.max(Duration.between(item.start(), item.end()).toMinutes(), 15);
        LocalDateTime start = item.end().withSecond(0).withNano(0);
        if (start.getMinute() % 30 != 0) {
            start = start.plusMinutes(30 - start.getMinute() % 30);
        }
        LocalDateTime limit = start.plusDays(7);
        while (start.isBefore(limit)) {
            LocalDateTime end = start.plusMinutes(minutes);
            if (prevStart != null && start.isBefore(prevStart)) {
                start = start.plusMinutes(30);
                continue;
            }
            boolean free = true;
            for (Item other : all) {
                if (Objects.equals(other.planId(), item.planId())
                        && Objects.equals(other.batchId(), item.batchId())) {
                    continue;
                }
                if (other.start() == null || other.end() == null || !overlap(start, end, other.start(), other.end())) {
                    continue;
                }
                if ((item.equipmentId() != null && Objects.equals(item.equipmentId(), other.equipmentId()))
                        || (item.roomId() != null && Objects.equals(item.roomId(), other.roomId()))) {
                    free = false;
                    break;
                }
            }
            if (free) {
                return new LocalDateTime[]{start, end};
            }
            start = start.plusMinutes(30);
        }
        return null;
    }

    private LocalDateTime previousStart(Item item, List<Item> incoming) {
        return incoming.stream()
                .filter(x -> Objects.equals(x.workOrderId(), item.workOrderId()))
                .filter(x -> x.stepOrder() != null && item.stepOrder() != null && x.stepOrder() < item.stepOrder())
                .map(Item::start).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
    }

    // endregion

    // region 班次冲突
    // ===================================
    // 班次冲突
    // ===================================

    /**
     * 校验班组班次是否覆盖计划时段
     */
    private void checkShiftConflicts(List<Item> incoming, List<ScheduleConflictDto> conflicts) {
        Map<Long, List<ProductionShift>> shiftCache = new HashMap<>();
        for (Item item : incoming) {
            if (item.teamId() == null || item.start() == null || item.end() == null) {
                continue;
            }
            List<ProductionShift> shifts = shiftCache.computeIfAbsent(item.teamId(),
                    id -> shiftMapper.selectActiveByTeamId(id));
            if (CollectionUtils.isEmpty(shifts)) {
                continue;
            }
            boolean covered = shifts.stream().anyMatch(s -> withinShift(item.start(), item.end(), s));
            if (!covered) {
                conflicts.add(conflict("人员班次", item.planId(), item.batchId(), item.stepOrder(),
                        item.processName(), item.workOrderId(), item.workOrderCode(), item.start(), item.end(),
                        "班组【" + safe(item.teamName()) + "】在 " + item.start() + " ~ " + item.end()
                                + " 不在有效班次内"));
            }
        }
    }

    /**
     * 判断计划时段是否落在班次内，支持跨日班次
     */
    private boolean withinShift(LocalDateTime start, LocalDateTime end, ProductionShift shift) {
        LocalTime shiftStart = shift.getStartTime();
        LocalTime shiftEnd = shift.getEndTime();
        if (shiftStart == null || shiftEnd == null) {
            return true;
        }
        int startMin = shiftStart.getHour() * 60 + shiftStart.getMinute();
        int endMin = shiftEnd.getHour() * 60 + shiftEnd.getMinute();
        if (endMin <= startMin) {
            endMin += 24 * 60;
        }
        LocalDate date = start.toLocalDate();
        for (int offset = -1; offset <= 1; offset++) {
            LocalDateTime windowStart = date.plusDays(offset).atStartOfDay().plusMinutes(startMin);
            LocalDateTime windowEnd = date.plusDays(offset).atStartOfDay().plusMinutes(endMin);
            if (!start.isBefore(windowStart) && !end.isAfter(windowEnd)) {
                return true;
            }
        }
        return false;
    }

    // endregion

    // region 组装与辅助
    // ===================================
    // 组装与辅助
    // ===================================

    /**
     * 加载与本次计划时间窗重叠的已保存计划，并展开为批次单元
     */
    private List<Item> loadSavedItems(Long workOrderId, List<WorkOrderProcessPlan> plans,
                                      List<WorkOrderProcessPlanBatch> batches) {
        LocalDateTime minStart = plans.stream().map(WorkOrderProcessPlan::getPlanStartTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime maxEnd = plans.stream().map(WorkOrderProcessPlan::getPlanEndTime)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        if (batches != null) {
            minStart = java.util.stream.Stream.concat(
                            minStart == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(minStart),
                            batches.stream().map(WorkOrderProcessPlanBatch::getPlanStartTime).filter(Objects::nonNull))
                    .min(LocalDateTime::compareTo).orElse(null);
            maxEnd = java.util.stream.Stream.concat(
                            maxEnd == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(maxEnd),
                            batches.stream().map(WorkOrderProcessPlanBatch::getPlanEndTime).filter(Objects::nonNull))
                    .max(LocalDateTime::compareTo).orElse(null);
        }
        if (minStart == null || maxEnd == null) {
            return List.of();
        }
        List<WorkOrderProcessPlan> savedPlans = planMapper.selectOverlappingSavedPlans(
                minStart.minusDays(1), maxEnd.plusDays(1), workOrderId);
        if (CollectionUtils.isEmpty(savedPlans)) {
            return List.of();
        }
        List<Long> savedIds = savedPlans.stream().map(WorkOrderProcessPlan::getId).toList();
        Map<Long, List<WorkOrderProcessPlanBatch>> savedBatches = batchMapper.selectByPlanIds(savedIds).stream()
                .collect(Collectors.groupingBy(WorkOrderProcessPlanBatch::getPlanId));
        List<Item> items = new ArrayList<>();
        for (WorkOrderProcessPlan plan : savedPlans) {
            List<WorkOrderProcessPlanBatch> list = savedBatches.get(plan.getId());
            if (CollectionUtils.isEmpty(list)) {
                items.add(toItem(plan, null));
            } else {
                list.forEach(b -> items.add(toItem(plan, b)));
            }
        }
        return items;
    }

    /**
     * 把待保存的计划与批次组装为比对单元
     */
    private List<Item> buildItems(List<WorkOrderProcessPlan> plans,
                                  Map<Long, List<WorkOrderProcessPlanBatch>> batchMap) {
        List<Item> items = new ArrayList<>();
        for (WorkOrderProcessPlan plan : plans) {
            List<WorkOrderProcessPlanBatch> list = batchMap.get(plan.getId());
            if (CollectionUtils.isEmpty(list)) {
                items.add(toItem(plan, null));
            } else {
                list.forEach(b -> items.add(toItem(plan, b)));
            }
        }
        return items;
    }

    private Item toItem(WorkOrderProcessPlan plan, WorkOrderProcessPlanBatch batch) {
        if (batch == null) {
            return new Item(plan.getId(), null, plan.getStepOrder(), safeName(plan),
                    plan.getWorkOrderId(), plan.getWorkOrderCode(),
                    plan.getPlanStartTime(), plan.getPlanEndTime(),
                    plan.getEquipmentId(), plan.getEquipmentName(),
                    plan.getRoomId(), plan.getRoomName(),
                    plan.getTeamId(), plan.getTeamName());
        }
        return new Item(plan.getId(), batch.getId(), plan.getStepOrder(), safeName(plan),
                plan.getWorkOrderId(), plan.getWorkOrderCode(),
                batch.getPlanStartTime(), batch.getPlanEndTime(),
                batch.getEquipmentId() != null ? batch.getEquipmentId() : plan.getEquipmentId(),
                batch.getEquipmentName() != null ? batch.getEquipmentName() : plan.getEquipmentName(),
                batch.getRoomId() != null ? batch.getRoomId() : plan.getRoomId(),
                batch.getRoomName() != null ? batch.getRoomName() : plan.getRoomName(),
                batch.getTeamId() != null ? batch.getTeamId() : plan.getTeamId(),
                batch.getTeamName() != null ? batch.getTeamName() : plan.getTeamName());
    }

    private LocalDateTime effectiveStart(WorkOrderProcessPlan plan, List<WorkOrderProcessPlanBatch> batches) {
        if (!CollectionUtils.isEmpty(batches)) {
            return batches.stream().map(WorkOrderProcessPlanBatch::getPlanStartTime).filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo).orElse(plan.getPlanStartTime());
        }
        return plan.getPlanStartTime();
    }

    private LocalDateTime effectiveEnd(WorkOrderProcessPlan plan, List<WorkOrderProcessPlanBatch> batches) {
        if (!CollectionUtils.isEmpty(batches)) {
            return batches.stream().map(WorkOrderProcessPlanBatch::getPlanEndTime).filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo).orElse(plan.getPlanEndTime());
        }
        return plan.getPlanEndTime();
    }

    private boolean overlap(LocalDateTime aStart, LocalDateTime aEnd,
                            LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart != null && aEnd != null && bStart != null && bEnd != null
                && aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private ScheduleConflictDto conflict(String type, WorkOrderProcessPlan plan, Long batchId,
                                         LocalDateTime start, LocalDateTime end, String message) {
        return conflict(type, plan.getId(), batchId, plan.getStepOrder(), safeName(plan),
                plan.getWorkOrderId(), plan.getWorkOrderCode(), start, end, message);
    }

    private ScheduleConflictDto conflict(String type, Long planId, Long batchId, Integer stepOrder,
                                         String processName, Long workOrderId, String workOrderCode,
                                         LocalDateTime start, LocalDateTime end, String message) {
        ScheduleConflictDto dto = new ScheduleConflictDto();
        dto.setType(type);
        dto.setPlanId(planId);
        dto.setBatchId(batchId);
        dto.setStepOrder(stepOrder);
        dto.setProcessName(processName);
        dto.setWorkOrderId(workOrderId);
        dto.setWorkOrderCode(workOrderCode);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setMessage(message);
        return dto;
    }

    private String safeName(WorkOrderProcessPlan plan) {
        return plan.getProcessTypeName() != null ? plan.getProcessTypeName() : ("工序" + plan.getStepOrder());
    }

    private String safe(String value) {
        return value == null ? "—" : value;
    }

    // endregion
}

package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageConflictDto;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 设备维保记录冲突校验器
 * <p>
 * 在保存设备维保记录前校验维保时间是否与设备使用时间冲突：
 * <ol>
 *   <li>基础校验：设备必选；维保开始/结束时间必须同时填写，且结束时间晚于开始时间</li>
 *   <li>设备有效性：设备必须存在且未删除（停用/故障设备仍可登记维保）</li>
 *   <li>使用冲突：维保时间区间不得与工单工序执行记录中该设备的使用时间重叠</li>
 * </ol>
 * 时间区间按半开区间 [开始时间, 结束时间) 处理，首尾相接视为不冲突。
 * </p>
 * <p>
 * 并发控制：按 equipment_id 对设备行加 {@code SELECT ... FOR UPDATE} 行锁，
 * 与工序执行记录保存的加锁顺序一致（先锁设备、再读占用记录），避免并发登记互相绕过校验；
 * 调用方事务隔离级别需为 READ_COMMITTED。
 * </p>
 */
@Component
public class EquipmentMaintenanceConflictValidator {

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
     * 构造方法，注入所需Mapper
     *
     * @param equipmentMapper 设备Mapper
     * @param executionMapper 工序执行记录Mapper
     */
    @Autowired
    public EquipmentMaintenanceConflictValidator(EquipmentMapper equipmentMapper,
                                                 WorkOrderProcessExecutionMapper executionMapper) {
        this.equipmentMapper = equipmentMapper;
        this.executionMapper = executionMapper;
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
     * 校验维保记录的时间区间是否与设备使用时间冲突
     * <p>
     * 有任何校验不通过时抛出 {@link IllegalArgumentException}，消息为多行文本，
     * 每行描述一条具体问题，便于前端直接展示。
     * </p>
     *
     * @param equipmentId 关联设备ID
     * @param startTime   维保开始时间
     * @param endTime     维保结束时间
     * @throws IllegalArgumentException 校验不通过
     */
    public void validate(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 基础校验
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("请选择维保设备");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("请同时填写维保开始时间和维保结束时间");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("维保结束时间必须晚于维保开始时间");
        }

        // 2. 设备加锁并校验有效性（与工序执行记录保存保持相同的加锁顺序）
        Integer equipmentIdValue = equipmentId.intValue();
        Equipment equipment = equipmentMapper.selectByIdForUpdate(equipmentIdValue);
        if (equipment == null || Integer.valueOf(1).equals(equipment.getIsDeleted())) {
            throw new IllegalArgumentException("设备【ID:" + equipmentId + "】不存在或已删除，不可登记维保记录");
        }

        // 3. 与既有设备使用记录做时间区间冲突校验
        List<EquipmentUsageConflictDto> conflicts = executionMapper.selectConflictRecords(
                Collections.singletonList(equipmentIdValue), startTime, endTime, null);
        if (CollectionUtils.isEmpty(conflicts)) {
            return;
        }

        String equipmentLabel = describeEquipment(equipment);
        Set<String> errors = new LinkedHashSet<>();
        for (EquipmentUsageConflictDto conflict : conflicts) {
            if (!Objects.equals(equipmentIdValue, conflict.getEquipmentId())) {
                continue;
            }
            // 半开区间重叠：[start, end)
            boolean overlap = conflict.getStartTime() != null && conflict.getEndTime() != null
                    && startTime.isBefore(conflict.getEndTime())
                    && endTime.isAfter(conflict.getStartTime());
            if (!overlap) {
                continue;
            }
            errors.add("设备【" + equipmentLabel + "】在 "
                    + formatRange(conflict.getStartTime(), conflict.getEndTime())
                    + " 已被工单【" + safeText(conflict.getWorkOrderCode()) + "】的工序【"
                    + safeText(conflict.getProcessName()) + "】（批号：" + safeText(conflict.getBatchNumber())
                    + "）占用，维保时间 " + formatRange(startTime, endTime) + " 与设备使用时间冲突");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

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

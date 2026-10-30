package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.EquipmentMaintenanceConflictValidator;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.mapper.EquipmentMaintenanceMapper;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import com.tonghui.erp.Service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备维保记录服务实现类
 * <p>
 * 实现EquipmentMaintenanceService接口，提供设备维保记录相关的业务逻辑处理，包括维保记录的
 * 查询、新增（含自动计算下次维保时间）、即将到期提醒等功能的具体实现
 * </p>
 *
 */
@Service
public class EquipmentMaintenanceServiceImpl extends ServiceImpl<EquipmentMaintenanceMapper, EquipmentMaintenance> implements EquipmentMaintenanceService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 设备服务，用于获取设备的维保周期信息 */
    @Autowired
    private EquipmentService equipmentService;

    /** 维保时间冲突校验器，用于校验维保时间与设备使用时间是否重叠 */
    @Autowired
    private EquipmentMaintenanceConflictValidator maintenanceConflictValidator;

    // endregion

    // region 预警查询
    // ===================================
    // 预警查询
    // ===================================

    /**
     * 查询即将到期的设备维保提醒
     * <p>查询条件：下次维保日期在今天到指定天数之间</p>
     *
     * @param days 预警天数范围，查询从今天起days天内将到期的维保记录
     * @return 即将到期的维保记录列表，按下次维保日期升序排列
     */
    @Override
    public List<EquipmentMaintenance> findUpcomingMaintenance(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        LocalDate pastDeadline = today.minusDays(days);
        
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
               .isNotNull("next_maintenance_date")
               .ge("next_maintenance_date", pastDeadline)
               .le("next_maintenance_date", deadline)
               .orderByAsc("next_maintenance_date");
        
        return list(wrapper);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据设备ID查询维保记录
     *
     * @param equipmentId 设备ID
     * @return 该设备的所有维保记录，按维保日期降序排列
     */
    @Override
    public List<EquipmentMaintenance> findByEquipmentId(Long equipmentId) {
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.eq("equipment_id", equipmentId)
               .orderByDesc("maintenance_start_time");  // 按维保开始时间降序
        return list(wrapper);
    }

    // endregion

    // region 业务操作
    // ===================================
    // 业务操作
    // ===================================

    /**
     * 新增维保记录（含自动计算与时间冲突校验）
     * <p>
     * 保养类型：自动按设备维保周期计算下次维保时间（以维保开始时间所在日期为基准）；
     * 维修类型：不自动计算，需手工填写。
     * 保存前校验维保时间是否与设备使用时间冲突，保存后同步设备的上次/下次维保日期。
     * </p>
     *
     * @param maintenance 维保记录实体
     * @return 保存后的维保记录实体
     * @throws IllegalArgumentException 维保时间与设备使用时间冲突或参数不合法
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public EquipmentMaintenance saveWithAutoCalc(EquipmentMaintenance maintenance) {
        // 1. 时间合法性校验 + 与设备使用时间冲突校验（同时锁定设备行，串行化同一设备的登记）
        maintenanceConflictValidator.validate(maintenance.getEquipmentId(),
                maintenance.getMaintenanceStartTime(), maintenance.getMaintenanceEndTime());

        // 2. 保养类型且未设置下次维保时间：按设备维保周期自动计算
        if ("保养".equals(maintenance.getMaintenanceType())
                && maintenance.getNextMaintenanceDate() == null) {
            Equipment equipment = equipmentService.getById(maintenance.getEquipmentId());
            int cycleMonths = 6; // 默认6个月
            if (equipment != null && equipment.getMaintenanceCycle() != null) {
                cycleMonths = equipment.getMaintenanceCycle();
            }
            // 自动计算下次维保时间 = 本次维保日期 + 维保周期（月）
            maintenance.setNextMaintenanceDate(
                    maintenance.getMaintenanceStartTime().toLocalDate().plusMonths(cycleMonths));
        }

        // 3. 保存维保记录
        save(maintenance);

        // 4. 同步设备的上次/下次维保时间
        syncEquipmentMaintenanceDates(maintenance.getEquipmentId(),
                maintenance.getMaintenanceStartTime().toLocalDate(),
                maintenance.getNextMaintenanceDate());

        return maintenance;
    }

    /**
     * 修改维保记录（含冲突校验）
     * <p>
     * 仅当入参涉及设备或维保时间时，将入参与原记录合并后校验维保时间是否与设备使用时间冲突；
     * 仅修改内容、费用等字段时不触发冲突校验，避免存量数据被误拦截。
     * </p>
     *
     * @param maintenance 维保记录实体（须包含 maintenanceId）
     * @return 修改后的维保记录实体
     * @throws IllegalArgumentException 维保记录不存在或维保时间与设备使用时间冲突
     */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public EquipmentMaintenance updateWithConflictCheck(EquipmentMaintenance maintenance) {
        EquipmentMaintenance existing = getById(maintenance.getMaintenanceId());
        if (existing == null) {
            throw new IllegalArgumentException("维保记录不存在");
        }

        boolean timeRelated = maintenance.getEquipmentId() != null
                || maintenance.getMaintenanceStartTime() != null
                || maintenance.getMaintenanceEndTime() != null;
        if (timeRelated) {
            // 入参可能只带了部分时间字段，需与原记录合并后再校验
            Long equipmentId = maintenance.getEquipmentId() != null
                    ? maintenance.getEquipmentId() : existing.getEquipmentId();
            LocalDateTime startTime = maintenance.getMaintenanceStartTime() != null
                    ? maintenance.getMaintenanceStartTime() : existing.getMaintenanceStartTime();
            LocalDateTime endTime = maintenance.getMaintenanceEndTime() != null
                    ? maintenance.getMaintenanceEndTime() : existing.getMaintenanceEndTime();
            maintenanceConflictValidator.validate(equipmentId, startTime, endTime);

            // 合并后的值显式回写，避免 MyBatis-Plus 忽略 null 字段导致"半更新"
            maintenance.setEquipmentId(equipmentId);
            maintenance.setMaintenanceStartTime(startTime);
            maintenance.setMaintenanceEndTime(endTime);
        }

        updateById(maintenance);

        // 维保开始时间变化时同步设备的上次维保时间
        if (timeRelated) {
            syncEquipmentMaintenanceDates(maintenance.getEquipmentId(),
                    maintenance.getMaintenanceStartTime().toLocalDate(),
                    maintenance.getNextMaintenanceDate());
        }

        return maintenance;
    }

    /**
     * 同步设备的维保日期字段
     * <p>
     * 下次维保日期为空时不覆盖设备上的既有值（MyBatis-Plus 默认忽略 null 字段）。
     * </p>
     *
     * @param equipmentId 设备ID
     * @param lastDate    上次维保日期
     * @param nextDate    下次维保日期
     */
    private void syncEquipmentMaintenanceDates(Long equipmentId, LocalDate lastDate, LocalDate nextDate) {
        if (equipmentId == null) {
            return;
        }
        Equipment updateEquipment = new Equipment();
        updateEquipment.setEquipmentId(equipmentId.intValue());
        updateEquipment.setLastMaintenanceDate(lastDate);
        updateEquipment.setNextMaintenanceDate(nextDate);
        equipmentService.updateById(updateEquipment);
    }

    // endregion
}

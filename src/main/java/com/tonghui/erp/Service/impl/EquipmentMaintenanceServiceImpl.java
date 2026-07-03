package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.mapper.EquipmentMaintenanceMapper;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import com.tonghui.erp.Service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
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
               .orderByDesc("maintenance_date");  // 按维保日期降序
        return list(wrapper);
    }

    // endregion

    // region 业务操作
    // ===================================
    // 业务操作
    // ===================================

    /**
     * 新增维保记录（含自动计算逻辑）
     * <p>
     * 保养类型：自动按设备维保周期计算下次维保时间
     * 维修类型：不自动计算，需手工填写
     * 保存后同时更新设备的上次维保时间和下次维保时间
     * </p>
     *
     * @param maintenance 维保记录实体
     * @return 保存后的维保记录实体
     */
    @Override
    public EquipmentMaintenance saveWithAutoCalc(EquipmentMaintenance maintenance) {
        // 如果是保养类型且未设置下次维保时间，则自动计算
        if ("保养".equals(maintenance.getMaintenanceType()) 
                && maintenance.getNextMaintenanceDate() == null
                && maintenance.getMaintenanceDate() != null) {
            
            // 获取设备信息，获取维保周期
            Equipment equipment = equipmentService.getById(maintenance.getEquipmentId());
            int cycleMonths = 6; // 默认6个月
            if (equipment != null && equipment.getMaintenanceCycle() != null) {
                cycleMonths = equipment.getMaintenanceCycle();
            }
            
            // 自动计算下次维保时间 = 本次维保时间 + 维保周期（月）
            LocalDate nextDate = maintenance.getMaintenanceDate().plusMonths(cycleMonths);
            maintenance.setNextMaintenanceDate(nextDate);
        }
        
        // 保存维保记录
        save(maintenance);
        
        // 更新设备的上次维保时间和下次维保时间
        if (maintenance.getEquipmentId() != null) {
            Equipment updateEquipment = new Equipment();
            updateEquipment.setEquipmentId(maintenance.getEquipmentId().intValue());
            updateEquipment.setLastMaintenanceDate(maintenance.getMaintenanceDate());
            updateEquipment.setNextMaintenanceDate(maintenance.getNextMaintenanceDate());
            equipmentService.updateById(updateEquipment);
        }
        
        return maintenance;
    }

    // endregion
}

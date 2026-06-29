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
 */
@Service
public class EquipmentMaintenanceServiceImpl extends ServiceImpl<EquipmentMaintenanceMapper, EquipmentMaintenance> implements EquipmentMaintenanceService {

    @Autowired
    private EquipmentService equipmentService;

    /**
     * 查询即将到期的维保提醒
     * 查询条件：下次维保日期在今天到指定天数之间
     */
    @Override
    public List<EquipmentMaintenance> findUpcomingMaintenance(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("next_maintenance_date")  // 下次维保日期不为空
               .ge("next_maintenance_date", today)  // 大于等于今天
               .le("next_maintenance_date", deadline)  // 小于等于截止日期
               .orderByAsc("next_maintenance_date");  // 按下次维保日期升序
        
        return list(wrapper);
    }

    /**
     * 根据设备ID查询维保记录
     */
    @Override
    public List<EquipmentMaintenance> findByEquipmentId(Long equipmentId) {
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.eq("equipment_id", equipmentId)
               .orderByDesc("maintenance_date");  // 按维保日期降序
        return list(wrapper);
    }

    /**
     * 新增维保记录（含自动计算逻辑）
     * 保养类型：自动按维保周期计算下次维保时间
     * 维修类型：不自动计算，需手工填写
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
}
